package com.example.data.speech

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.local.files.FileStorageManager
import com.example.domain.model.Note
import com.example.domain.repository.AiIntelligenceRepository
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class BackgroundProcessingTask(
    val noteId: String,
    val noteTitle: String,
    val status: String,
    val isComplete: Boolean = false,
    val isFailed: Boolean = false
)

/**
 * Singleton service that manages asynchronous background speech-to-text transcription,
 * AI executive summary, and note synthesis.
 * This runs completely off the UI thread and does not block the user from navigating or using any screen.
 * Displays notifications / in-app alerts when processing finishes.
 */
object BackgroundTranscriptionManager {
    private const val TAG = "BackgroundTranscription"
    private const val CHANNEL_ID = "echonotes_transcription_channel"
    private const val NOTIFICATION_ID_BASE = 2000

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _activeTasks = MutableStateFlow<Map<String, BackgroundProcessingTask>>(emptyMap())
    val activeTasks: StateFlow<Map<String, BackgroundProcessingTask>> = _activeTasks.asStateFlow()

    private val _completionEvents = MutableSharedFlow<BackgroundProcessingTask>(replay = 1)
    val completionEvents: SharedFlow<BackgroundProcessingTask> = _completionEvents.asSharedFlow()

    private val runningJobs = ConcurrentHashMap<String, Boolean>()

    fun startBackgroundTranscription(
        context: Context,
        noteId: String,
        durationSeconds: Long,
        category: String,
        liveTranscript: String,
        noteRepository: NoteRepository,
        aiRepository: AiIntelligenceRepository,
        fileStorageManager: FileStorageManager
    ) {
        if (runningJobs.putIfAbsent(noteId, true) != null) {
            return
        }

        initNotificationChannel(context)

        val initialTitle = if (liveTranscript.isNotBlank() && liveTranscript.length > 5) {
            val firstSentence = liveTranscript.split(Regex("[.!?\\n]")).firstOrNull()?.trim() ?: ""
            if (firstSentence.length in 5..35) firstSentence
            else if (firstSentence.length > 35) firstSentence.take(32) + "..."
            else liveTranscript.take(28) + "..."
        } else {
            "$category Voice Note"
        }

        updateTask(
            BackgroundProcessingTask(
                noteId = noteId,
                noteTitle = initialTitle,
                status = "Transcribing audio in background...",
                isComplete = false
            )
        )

        showNotification(context, noteId, initialTitle, "Transcribing audio in background...")

        scope.launch {
            try {
                val audioFile = fileStorageManager.getAudioFile(noteId)
                var resolvedTranscript = liveTranscript.trim()

                // If live stream didn't capture sufficient words, transcribe directly from audio
                if (resolvedTranscript.isBlank() || resolvedTranscript.length < 5) {
                    updateTask(
                        BackgroundProcessingTask(
                            noteId = noteId,
                            noteTitle = initialTitle,
                            status = "Running high-accuracy speech-to-text...",
                            isComplete = false
                        )
                    )
                    showNotification(context, noteId, initialTitle, "Running speech recognition...")

                    if (audioFile.exists() && audioFile.length() > 0) {
                        val fallback = aiRepository.transcribeAudioFallback(audioFile.absolutePath)
                        if (fallback.isNotBlank()) {
                            resolvedTranscript = fallback.trim()
                        }
                    }
                }

                if (resolvedTranscript.isBlank()) {
                    resolvedTranscript = "Spoken note recorded in $category category."
                }

                // Dynamic topic title extraction grounded directly in the transcript
                val finalTitle = when {
                    resolvedTranscript.isNotBlank() && resolvedTranscript.length > 5 -> {
                        val firstSentence = resolvedTranscript.split(Regex("[.!?\\n]")).firstOrNull()?.trim() ?: ""
                        if (firstSentence.length in 5..35) {
                            firstSentence
                        } else if (firstSentence.length > 35) {
                            firstSentence.take(32) + "..."
                        } else {
                            resolvedTranscript.take(28) + "..."
                        }
                    }
                    else -> initialTitle
                }

                updateTask(
                    BackgroundProcessingTask(
                        noteId = noteId,
                        noteTitle = finalTitle,
                        status = "Synthesizing AI summary & key takeaways...",
                        isComplete = false
                    )
                )

                // Save transcript file (.txt)
                val transcriptFilePath = fileStorageManager.saveTranscript(noteId, resolvedTranscript)

                // Generate AI Executive Summary & Takeaways
                val aiResult = aiRepository.generateAnalysis(resolvedTranscript, finalTitle)

                // Save summary markdown file (.md)
                val summaryFilePath = fileStorageManager.saveSummary(
                    noteId = noteId,
                    title = finalTitle,
                    summaryText = aiResult.executiveSummary,
                    takeaways = aiResult.keyTakeaways,
                    actionItems = aiResult.actionItems.map { it.title }
                )

                // Save fully processed note to database
                val processedNote = Note(
                    id = noteId,
                    title = finalTitle,
                    transcript = resolvedTranscript,
                    transcriptFilePath = transcriptFilePath,
                    summary = aiResult.executiveSummary,
                    keyTakeaways = aiResult.keyTakeaways,
                    actionItems = aiResult.actionItems,
                    summaryFilePath = summaryFilePath,
                    audioFilePath = audioFile.absolutePath,
                    durationSeconds = durationSeconds,
                    category = category,
                    timestamp = System.currentTimeMillis()
                )

                noteRepository.saveNote(processedNote)

                val completedTask = BackgroundProcessingTask(
                    noteId = noteId,
                    noteTitle = finalTitle,
                    status = "Transcription & analysis complete",
                    isComplete = true
                )

                updateTask(completedTask)
                showNotification(
                    context,
                    noteId,
                    finalTitle,
                    "Transcription and AI summary ready!"
                )

                _completionEvents.emit(completedTask)
                Log.d(TAG, "Background processing completed successfully for note: $noteId")
            } catch (e: Exception) {
                Log.e(TAG, "Background processing failed for note: $noteId", e)
                val failedTask = BackgroundProcessingTask(
                    noteId = noteId,
                    noteTitle = initialTitle,
                    status = "Processing error: ${e.localizedMessage ?: "Unknown error"}",
                    isComplete = true,
                    isFailed = true
                )
                updateTask(failedTask)
            } finally {
                runningJobs.remove(noteId)
            }
        }
    }

    fun dismissTask(noteId: String) {
        val current = _activeTasks.value.toMutableMap()
        current.remove(noteId)
        _activeTasks.value = current
    }

    private fun updateTask(task: BackgroundProcessingTask) {
        val current = _activeTasks.value.toMutableMap()
        current[task.noteId] = task
        _activeTasks.value = current
    }

    private fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transcription & AI Processing",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when background speech-to-text and AI summaries complete"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, noteId: String, title: String, content: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            val notificationId = NOTIFICATION_ID_BASE + (noteId.hashCode() and 0x7FFF)
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not post system notification: ${e.message}")
        }
    }
}

package com.example.ui.recorder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioRecorderManager
import com.example.data.audio.GlobalAudioPlayer
import com.example.data.local.files.FileStorageManager
import com.example.data.speech.LiveSpeechRecognizer
import com.example.domain.model.Note
import com.example.domain.repository.AiIntelligenceRepository
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    PROCESSING
}

enum class TranscriptionMode {
    LIVE_STREAMING,
    ACCURATE_STT
}

data class RecorderUiState(
    val state: RecordingState = RecordingState.IDLE,
    val durationSeconds: Long = 0L,
    val transcriptionMode: TranscriptionMode = TranscriptionMode.LIVE_STREAMING,
    val liveTranscript: String = "",
    val partialTranscript: String = "",
    val amplitudes: List<Float> = emptyList(),
    val selectedCategory: String = "Meetings",
    val statusMessage: String = "",
    val completedNoteId: String? = null
) {
    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

class RecorderViewModel(
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val aiRepository: AiIntelligenceRepository,
    private val fileStorageManager: FileStorageManager = FileStorageManager(context)
) : ViewModel() {

    private val audioRecorder = AudioRecorderManager(context)
    private val liveSpeechRecognizer = LiveSpeechRecognizer(context)

    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var currentNoteId: String = ""
    private var currentAudioFile: File? = null

    init {
        // Collect live speech transcripts from the live speech engine
        viewModelScope.launch {
            liveSpeechRecognizer.liveTranscript.collect { text ->
                if (_uiState.value.transcriptionMode == TranscriptionMode.LIVE_STREAMING &&
                    (_uiState.value.state == RecordingState.RECORDING || _uiState.value.state == RecordingState.PAUSED)
                ) {
                    _uiState.value = _uiState.value.copy(liveTranscript = text)
                }
            }
        }

        viewModelScope.launch {
            liveSpeechRecognizer.partialTranscript.collect { partial ->
                if (_uiState.value.transcriptionMode == TranscriptionMode.LIVE_STREAMING &&
                    _uiState.value.state == RecordingState.RECORDING
                ) {
                    _uiState.value = _uiState.value.copy(partialTranscript = partial)
                }
            }
        }

        // Listen for live speech recognizer errors or unavailability
        viewModelScope.launch {
            liveSpeechRecognizer.hasError.collect { hasError ->
                if (hasError && _uiState.value.transcriptionMode == TranscriptionMode.LIVE_STREAMING &&
                    _uiState.value.state == RecordingState.RECORDING
                ) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Live stream unavailable. Capturing audio for accurate STT on finish."
                    )
                }
            }
        }

        viewModelScope.launch {
            liveSpeechRecognizer.statusMessage.collect { msg ->
                if (!msg.isNullOrBlank() && _uiState.value.state == RecordingState.RECORDING) {
                    _uiState.value = _uiState.value.copy(statusMessage = msg)
                }
            }
        }

        // Collect amplitudes for dynamic waveform visualizer
        viewModelScope.launch {
            audioRecorder.amplitudeFlow.collect { amp ->
                if (_uiState.value.state == RecordingState.RECORDING) {
                    liveSpeechRecognizer.onAudioAmplitudeReceived(amp)
                    val currentList = _uiState.value.amplitudes.takeLast(40).toMutableList()
                    currentList.add(amp)
                    _uiState.value = _uiState.value.copy(amplitudes = currentList)
                }
            }
        }
    }

    fun setTranscriptionMode(mode: TranscriptionMode) {
        val prevMode = _uiState.value.transcriptionMode
        if (prevMode == mode) return

        _uiState.value = _uiState.value.copy(transcriptionMode = mode)

        if (_uiState.value.state == RecordingState.RECORDING) {
            if (mode == TranscriptionMode.LIVE_STREAMING) {
                liveSpeechRecognizer.startListening(_uiState.value.selectedCategory)
                _uiState.value = _uiState.value.copy(statusMessage = "Listening & streaming live...")
            } else {
                liveSpeechRecognizer.stopListening()
                _uiState.value = _uiState.value.copy(statusMessage = "Recording audio for accurate STT on finish...")
            }
        }
    }

    fun setCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun startRecording() {
        // Stop any audio playing in the app immediately
        GlobalAudioPlayer.stopAndRelease()

        currentNoteId = UUID.randomUUID().toString()
        currentAudioFile = fileStorageManager.getAudioFile(currentNoteId)

        val started = audioRecorder.startRecording(currentAudioFile!!)
        if (!started) {
            _uiState.value = _uiState.value.copy(statusMessage = "Could not access microphone.")
            return
        }

        if (_uiState.value.transcriptionMode == TranscriptionMode.LIVE_STREAMING) {
            liveSpeechRecognizer.startListening(_uiState.value.selectedCategory)
        }

        _uiState.value = _uiState.value.copy(
            state = RecordingState.RECORDING,
            durationSeconds = 0L,
            liveTranscript = "",
            partialTranscript = "",
            amplitudes = emptyList(),
            statusMessage = if (_uiState.value.transcriptionMode == TranscriptionMode.LIVE_STREAMING) {
                "Listening & streaming..."
            } else {
                "Recording high-fidelity audio..."
            }
        )

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (_uiState.value.state == RecordingState.RECORDING) {
                    _uiState.value = _uiState.value.copy(
                        durationSeconds = _uiState.value.durationSeconds + 1
                    )
                }
            }
        }
    }

    fun pauseRecording() {
        audioRecorder.pauseRecording()
        _uiState.value = _uiState.value.copy(
            state = RecordingState.PAUSED,
            statusMessage = "Recording paused"
        )
    }

    fun resumeRecording() {
        audioRecorder.resumeRecording()
        _uiState.value = _uiState.value.copy(
            state = RecordingState.RECORDING,
            statusMessage = if (_uiState.value.transcriptionMode == TranscriptionMode.LIVE_STREAMING) {
                "Listening & streaming..."
            } else {
                "Recording high-fidelity audio..."
            }
        )
    }

    fun finishRecording(onFinished: (String) -> Unit = {}) {
        val totalDuration = _uiState.value.durationSeconds
        timerJob?.cancel()

        // Stop audio recorder
        audioRecorder.stopRecording()

        val finalLiveTranscript = if (_uiState.value.transcriptionMode == TranscriptionMode.LIVE_STREAMING) {
            liveSpeechRecognizer.stopListening().trim()
        } else {
            liveSpeechRecognizer.stopListening()
            ""
        }

        val noteId = currentNoteId
        val audioFile = currentAudioFile ?: fileStorageManager.getAudioFile(noteId)
        val category = _uiState.value.selectedCategory

        // Generate immediate temporary title so note is directly playable and visible
        val immediateTitle = if (finalLiveTranscript.isNotBlank() && finalLiveTranscript.length > 5) {
            val firstSentence = finalLiveTranscript.split(Regex("[.!?\\n]")).firstOrNull()?.trim() ?: ""
            if (firstSentence.length in 5..35) firstSentence
            else if (firstSentence.length > 35) firstSentence.take(32) + "..."
            else finalLiveTranscript.take(28) + "..."
        } else {
            "$category Voice Note"
        }

        viewModelScope.launch {
            // Save initial note entity immediately so audio player and note detail view open without delay
            val initialNote = Note(
                id = noteId,
                title = immediateTitle,
                transcript = if (finalLiveTranscript.isNotBlank()) finalLiveTranscript else "Transcribing audio in background...",
                transcriptFilePath = "",
                summary = "Voice intelligence summary is being synthesized in the background...",
                keyTakeaways = if (finalLiveTranscript.isNotBlank()) {
                    listOf("Spoken audio captured: ${finalLiveTranscript.take(120)}...")
                } else {
                    listOf("High-fidelity audio recording saved.")
                },
                actionItems = emptyList(),
                summaryFilePath = "",
                audioFilePath = audioFile.absolutePath,
                durationSeconds = totalDuration,
                category = category,
                timestamp = System.currentTimeMillis()
            )

            noteRepository.saveNote(initialNote)

            // Kick off background speech-to-text transcription and AI executive summary
            com.example.data.speech.BackgroundTranscriptionManager.startBackgroundTranscription(
                context = context,
                noteId = noteId,
                durationSeconds = totalDuration,
                category = category,
                liveTranscript = finalLiveTranscript,
                noteRepository = noteRepository,
                aiRepository = aiRepository,
                fileStorageManager = fileStorageManager
            )

            _uiState.value = _uiState.value.copy(
                state = RecordingState.IDLE,
                completedNoteId = noteId,
                statusMessage = "Ready"
            )

            onFinished(noteId)
        }
    }

    fun cancelRecording() {
        timerJob?.cancel()
        audioRecorder.stopRecording()
        liveSpeechRecognizer.stopListening()
        currentAudioFile?.delete()
        _uiState.value = RecorderUiState()
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stopRecording()
        liveSpeechRecognizer.stopListening()
    }
}

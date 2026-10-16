package com.example.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioPlayerManager
import com.example.data.audio.AudioPlayerState
import com.example.domain.model.Note
import com.example.domain.repository.AiIntelligenceRepository
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class NoteDetailUiState(
    val note: Note? = null,
    val isLoading: Boolean = true,
    val isRegeneratingAi: Boolean = false,
    val playerState: AudioPlayerState = AudioPlayerState()
)

class NoteDetailViewModel(
    private val noteId: String,
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val aiRepository: AiIntelligenceRepository
) : ViewModel() {

    private val audioPlayerManager = AudioPlayerManager(context)

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        loadNote()
        observePlayerState()
    }

    private fun loadNote() {
        viewModelScope.launch {
            noteRepository.getNoteById(noteId).collect { note ->
                _uiState.value = _uiState.value.copy(
                    note = note,
                    isLoading = false
                )
                if (note != null && note.audioFilePath.isNotEmpty()) {
                    val file = File(note.audioFilePath)
                    if (file.exists() && _uiState.value.playerState.currentFilePath != file.absolutePath) {
                        audioPlayerManager.loadAudio(file)
                    }
                }
            }
        }
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            audioPlayerManager.playerState.collect { playerState ->
                _uiState.value = _uiState.value.copy(playerState = playerState)
            }
        }
    }

    fun togglePlayPause() {
        val currentNote = _uiState.value.note
        if (currentNote != null && currentNote.audioFilePath.isNotEmpty()) {
            val file = File(currentNote.audioFilePath)
            if (file.exists() && _uiState.value.playerState.currentFilePath != file.absolutePath) {
                audioPlayerManager.loadAudio(file)
            }
        }
        audioPlayerManager.togglePlayPause()
    }

    fun seekTo(progress: Float) {
        val duration = _uiState.value.playerState.durationMs
        if (duration > 0) {
            val targetMs = (progress * duration).toLong()
            audioPlayerManager.seekTo(targetMs)
        }
    }

    fun toggleActionItem(actionItemId: String) {
        val currentNote = _uiState.value.note ?: return
        val item = currentNote.actionItems.find { it.id == actionItemId } ?: return
        val newStatus = !item.isCompleted

        viewModelScope.launch {
            noteRepository.updateActionItem(currentNote.id, actionItemId, newStatus)
        }
    }

    fun regenerateAiAnalysis() {
        val currentNote = _uiState.value.note ?: return
        _uiState.value = _uiState.value.copy(isRegeneratingAi = true)

        viewModelScope.launch {
            try {
                val result = aiRepository.generateAnalysis(currentNote.transcript, currentNote.title)
                val updatedNote = currentNote.copy(
                    summary = result.executiveSummary,
                    keyTakeaways = result.keyTakeaways,
                    actionItems = result.actionItems
                )
                noteRepository.saveNote(updatedNote)
            } finally {
                _uiState.value = _uiState.value.copy(isRegeneratingAi = false)
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        viewModelScope.launch {
            audioPlayerManager.release()
            noteRepository.deleteNote(noteId)
            onDeleted()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.release()
    }
}

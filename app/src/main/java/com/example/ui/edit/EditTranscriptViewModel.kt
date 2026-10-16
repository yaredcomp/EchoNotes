package com.example.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Note
import com.example.domain.repository.AiIntelligenceRepository
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditTranscriptUiState(
    val note: Note? = null,
    val title: String = "",
    val transcript: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

class EditTranscriptViewModel(
    private val noteId: String,
    private val noteRepository: NoteRepository,
    private val aiRepository: AiIntelligenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditTranscriptUiState())
    val uiState: StateFlow<EditTranscriptUiState> = _uiState.asStateFlow()

    init {
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            val note = noteRepository.getNoteByIdSync(noteId)
            if (note != null) {
                _uiState.value = EditTranscriptUiState(
                    note = note,
                    title = note.title,
                    transcript = note.transcript,
                    isLoading = false
                )
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle)
    }

    fun onTranscriptChanged(newTranscript: String) {
        _uiState.value = _uiState.value.copy(transcript = newTranscript)
    }

    fun saveChanges(regenerateAi: Boolean = true, onComplete: () -> Unit) {
        val currentNote = _uiState.value.note ?: return
        val newTitle = _uiState.value.title.trim().ifEmpty { currentNote.title }
        val newTranscript = _uiState.value.transcript.trim()

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            if (regenerateAi && newTranscript != currentNote.transcript) {
                val aiResult = aiRepository.generateAnalysis(newTranscript, newTitle)
                val updatedNote = currentNote.copy(
                    title = newTitle,
                    transcript = newTranscript,
                    summary = aiResult.executiveSummary,
                    keyTakeaways = aiResult.keyTakeaways,
                    actionItems = aiResult.actionItems
                )
                noteRepository.saveNote(updatedNote)
            } else {
                val updatedNote = currentNote.copy(
                    title = newTitle,
                    transcript = newTranscript
                )
                noteRepository.saveNote(updatedNote)
            }
            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            onComplete()
        }
    }
}

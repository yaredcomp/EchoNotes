package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ChatMessage
import com.example.domain.model.Note
import com.example.domain.repository.AiIntelligenceRepository
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val note: Note? = null,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val suggestions: List<String> = listOf(
        "What were the main points?",
        "What tasks were assigned?",
        "Summarize this in one sentence",
        "What decisions were made?",
        "What deadlines were mentioned?"
    )
)

class ChatWithNoteViewModel(
    private val noteId: String,
    private val noteRepository: NoteRepository,
    private val aiRepository: AiIntelligenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadNoteAndChat()
    }

    private fun loadNoteAndChat() {
        viewModelScope.launch {
            noteRepository.getNoteById(noteId).collect { note ->
                _uiState.value = _uiState.value.copy(note = note)
            }
        }

        viewModelScope.launch {
            noteRepository.getChatMessages(noteId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage(questionText: String? = null) {
        val query = (questionText ?: _uiState.value.inputText).trim()
        if (query.isBlank() || _uiState.value.isSending) return

        val note = _uiState.value.note ?: return
        _uiState.value = _uiState.value.copy(inputText = "", isSending = true)

        viewModelScope.launch {
            // Save user message
            noteRepository.addChatMessage(note.id, isUser = true, message = query)

            // Prepare conversation history
            val history = _uiState.value.messages.map { it.isUser to it.message }

            // Ask Gemini AI grounded in the note's transcript
            val answer = aiRepository.answerQuestion(
                transcript = note.transcript,
                noteTitle = note.title,
                question = query,
                chatHistory = history
            )

            // Save AI response
            noteRepository.addChatMessage(note.id, isUser = false, message = answer)
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }
}

package com.example.domain.repository

import com.example.domain.model.ChatMessage
import com.example.domain.model.Note
import kotlinx.coroutines.flow.Flow
import java.io.File

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    suspend fun hasAnyNotes(): Boolean
    fun getNoteById(id: String): Flow<Note?>
    suspend fun getNoteByIdSync(id: String): Note?
    fun searchNotes(query: String): Flow<List<Note>>
    fun getNotesByCategory(category: String): Flow<List<Note>>
    suspend fun saveNote(note: Note): Note
    suspend fun updateNote(note: Note)
    suspend fun updateActionItem(noteId: String, actionItemId: String, isCompleted: Boolean)
    suspend fun deleteNote(id: String) // Moves note to trash

    // Trash operations
    fun getDeletedNotes(): Flow<List<Note>>
    fun getDeletedNotesCount(): Flow<Int>
    suspend fun restoreNote(id: String)
    suspend fun deletePermanently(id: String)
    suspend fun emptyTrash()

    // Chat operations
    fun getChatMessages(noteId: String): Flow<List<ChatMessage>>
    suspend fun addChatMessage(noteId: String, isUser: Boolean, message: String)

    // File operations
    fun getTranscriptFile(noteId: String): File?
    fun getSummaryFile(noteId: String): File?
    fun getAudioFile(noteId: String): File?
}

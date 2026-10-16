package com.example.data.repository

import android.content.Context
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.ChatMessageEntity
import com.example.data.local.db.NoteEntity
import com.example.data.local.files.FileStorageManager
import com.example.domain.model.ActionItem
import com.example.domain.model.ChatMessage
import com.example.domain.model.Note
import com.example.domain.repository.NoteRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class NoteRepositoryImpl(
    context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    private val fileStorageManager: FileStorageManager = FileStorageManager(context)
) : NoteRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val actionItemListType = Types.newParameterizedType(List::class.java, ActionItemDto::class.java)
    private val actionItemAdapter = moshi.adapter<List<ActionItemDto>>(actionItemListType)

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    data class ActionItemDto(
        val id: String,
        val title: String,
        val isCompleted: Boolean
    )

    private fun entityToDomain(entity: NoteEntity): Note {
        val actionItems = try {
            actionItemAdapter.fromJson(entity.actionItems)?.map {
                ActionItem(id = it.id, title = it.title, isCompleted = it.isCompleted)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val takeaways = try {
            stringListAdapter.fromJson(entity.keyTakeaways) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return Note(
            id = entity.id,
            title = entity.title,
            transcript = entity.transcript,
            transcriptFilePath = entity.transcriptFilePath,
            summary = entity.summary,
            keyTakeaways = takeaways,
            actionItems = actionItems,
            summaryFilePath = entity.summaryFilePath,
            audioFilePath = entity.audioFilePath,
            durationSeconds = entity.durationSeconds,
            category = entity.category,
            timestamp = entity.timestamp,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt
        )
    }

    private fun domainToEntity(domain: Note): NoteEntity {
        val dtos = domain.actionItems.map { ActionItemDto(it.id, it.title, it.isCompleted) }
        val actionItemsJson = actionItemAdapter.toJson(dtos)
        val takeawaysJson = stringListAdapter.toJson(domain.keyTakeaways)

        return NoteEntity(
            id = domain.id,
            title = domain.title,
            transcript = domain.transcript,
            transcriptFilePath = domain.transcriptFilePath,
            summary = domain.summary,
            keyTakeaways = takeawaysJson,
            actionItems = actionItemsJson,
            summaryFilePath = domain.summaryFilePath,
            audioFilePath = domain.audioFilePath,
            durationSeconds = domain.durationSeconds,
            category = domain.category,
            timestamp = domain.timestamp,
            isDeleted = domain.isDeleted,
            deletedAt = domain.deletedAt
        )
    }

    override fun getAllNotes(): Flow<List<Note>> {
        return database.noteDao().getAllNotes().map { list -> list.map { entityToDomain(it) } }
    }

    override suspend fun hasAnyNotes(): Boolean {
        return database.noteDao().getTotalNotesCount() > 0
    }

    override fun getNoteById(id: String): Flow<Note?> {
        return database.noteDao().getNoteById(id).map { it?.let { entityToDomain(it) } }
    }

    override suspend fun getNoteByIdSync(id: String): Note? {
        return database.noteDao().getNoteByIdSync(id)?.let { entityToDomain(it) }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return database.noteDao().searchNotes(query).map { list -> list.map { entityToDomain(it) } }
    }

    override fun getNotesByCategory(category: String): Flow<List<Note>> {
        return if (category == "All") {
            getAllNotes()
        } else {
            database.noteDao().getNotesByCategory(category).map { list -> list.map { entityToDomain(it) } }
        }
    }

    override suspend fun saveNote(note: Note): Note {
        val transcriptPath = fileStorageManager.saveTranscript(note.id, note.transcript)
        val summaryPath = fileStorageManager.saveSummary(
            noteId = note.id,
            title = note.title,
            summaryText = note.summary,
            takeaways = note.keyTakeaways,
            actionItems = note.actionItems.map { it.title }
        )

        val updatedNote = note.copy(
            transcriptFilePath = transcriptPath,
            summaryFilePath = summaryPath
        )

        database.noteDao().insertNote(domainToEntity(updatedNote))
        return updatedNote
    }

    override suspend fun updateNote(note: Note) {
        fileStorageManager.saveTranscript(note.id, note.transcript)
        fileStorageManager.saveSummary(
            noteId = note.id,
            title = note.title,
            summaryText = note.summary,
            takeaways = note.keyTakeaways,
            actionItems = note.actionItems.map { it.title }
        )
        database.noteDao().updateNote(domainToEntity(note))
    }

    override suspend fun updateActionItem(noteId: String, actionItemId: String, isCompleted: Boolean) {
        val note = getNoteByIdSync(noteId) ?: return
        val updatedItems = note.actionItems.map {
            if (it.id == actionItemId) it.copy(isCompleted = isCompleted) else it
        }
        val updatedNote = note.copy(actionItems = updatedItems)
        updateNote(updatedNote)
    }

    override suspend fun deleteNote(id: String) {
        // Soft delete: move to trash
        database.noteDao().moveToTrash(id, System.currentTimeMillis())
    }

    override fun getDeletedNotes(): Flow<List<Note>> {
        return database.noteDao().getDeletedNotes().map { list -> list.map { entityToDomain(it) } }
    }

    override fun getDeletedNotesCount(): Flow<Int> {
        return database.noteDao().getDeletedNotesCount()
    }

    override suspend fun restoreNote(id: String) {
        database.noteDao().restoreFromTrash(id)
    }

    override suspend fun deletePermanently(id: String) {
        database.noteDao().deletePermanently(id)
        database.chatMessageDao().deleteMessagesForNote(id)
        fileStorageManager.deleteNoteFiles(id)
    }

    override suspend fun emptyTrash() {
        val deletedNotes = database.noteDao().getDeletedNotes().first()
        for (note in deletedNotes) {
            database.chatMessageDao().deleteMessagesForNote(note.id)
            fileStorageManager.deleteNoteFiles(note.id)
        }
        database.noteDao().emptyTrash()
    }

    override fun getChatMessages(noteId: String): Flow<List<ChatMessage>> {
        return database.chatMessageDao().getMessagesForNote(noteId).map { list ->
            list.map { ChatMessage(it.id, it.noteId, it.isUser, it.message, it.timestamp) }
        }
    }

    override suspend fun addChatMessage(noteId: String, isUser: Boolean, message: String) {
        database.chatMessageDao().insertMessage(
            ChatMessageEntity(
                noteId = noteId,
                isUser = isUser,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override fun getTranscriptFile(noteId: String): File? {
        val file = fileStorageManager.getTranscriptFile(noteId)
        return if (file.exists()) file else null
    }

    override fun getSummaryFile(noteId: String): File? {
        val file = fileStorageManager.getSummaryFile(noteId)
        return if (file.exists()) file else null
    }

    override fun getAudioFile(noteId: String): File? {
        val file = fileStorageManager.getAudioFile(noteId)
        return if (file.exists()) file else null
    }
}

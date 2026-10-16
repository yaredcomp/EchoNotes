package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE noteId = :noteId ORDER BY timestamp ASC")
    fun getMessagesForNote(noteId: String): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE noteId = :noteId")
    suspend fun deleteMessagesForNote(noteId: String)
}

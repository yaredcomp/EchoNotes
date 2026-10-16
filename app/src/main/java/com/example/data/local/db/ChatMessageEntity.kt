package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val noteId: String,
    val isUser: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

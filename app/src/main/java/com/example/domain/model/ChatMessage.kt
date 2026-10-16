package com.example.domain.model

data class ChatMessage(
    val id: Long = 0L,
    val noteId: String,
    val isUser: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

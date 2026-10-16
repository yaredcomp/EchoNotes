package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val transcript: String,
    val transcriptFilePath: String = "",
    val summary: String = "",
    val keyTakeaways: String = "",
    val actionItems: String = "[]",
    val summaryFilePath: String = "",
    val audioFilePath: String = "",
    val durationSeconds: Long = 0L,
    val category: String = "All",
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L
)

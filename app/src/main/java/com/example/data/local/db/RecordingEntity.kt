package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val noteId: String,
    val filePath: String,
    val durationSeconds: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

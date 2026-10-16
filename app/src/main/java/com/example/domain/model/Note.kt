package com.example.domain.model

data class Note(
    val id: String,
    val title: String,
    val transcript: String,
    val transcriptFilePath: String = "",
    val summary: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val actionItems: List<ActionItem> = emptyList(),
    val summaryFilePath: String = "",
    val audioFilePath: String = "",
    val durationSeconds: Long = 0L,
    val category: String = "All",
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L
) {
    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

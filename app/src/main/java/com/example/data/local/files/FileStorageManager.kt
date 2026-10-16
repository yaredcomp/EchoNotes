package com.example.data.local.files

import android.content.Context
import java.io.File

class FileStorageManager(private val context: Context) {

    private val audioDir: File
        get() = File(context.filesDir, "audio").apply { if (!exists()) mkdirs() }

    private val transcriptDir: File
        get() = File(context.filesDir, "transcripts").apply { if (!exists()) mkdirs() }

    private val summaryDir: File
        get() = File(context.filesDir, "summaries").apply { if (!exists()) mkdirs() }

    fun getAudioFile(noteId: String): File {
        return File(audioDir, "${noteId}.m4a")
    }

    fun getTranscriptFile(noteId: String): File {
        return File(transcriptDir, "${noteId}_transcript.txt")
    }

    fun getSummaryFile(noteId: String): File {
        return File(summaryDir, "${noteId}_summary.md")
    }

    fun saveTranscript(noteId: String, transcriptText: String): String {
        val file = getTranscriptFile(noteId)
        file.writeText(transcriptText)
        return file.absolutePath
    }

    fun readTranscript(noteId: String): String {
        val file = getTranscriptFile(noteId)
        return if (file.exists()) file.readText() else ""
    }

    fun saveSummary(noteId: String, title: String, summaryText: String, takeaways: List<String>, actionItems: List<String>): String {
        val file = getSummaryFile(noteId)
        val content = buildString {
            appendLine("# $title")
            appendLine()
            appendLine("## Executive Summary")
            appendLine(summaryText)
            appendLine()
            if (takeaways.isNotEmpty()) {
                appendLine("## Key Takeaways")
                takeaways.forEach { appendLine("- $it") }
                appendLine()
            }
            if (actionItems.isNotEmpty()) {
                appendLine("## Action Items & Next Steps")
                actionItems.forEach { appendLine("- [ ] $it") }
                appendLine()
            }
        }
        file.writeText(content)
        return file.absolutePath
    }

    fun readSummary(noteId: String): String {
        val file = getSummaryFile(noteId)
        return if (file.exists()) file.readText() else ""
    }

    fun deleteNoteFiles(noteId: String) {
        getAudioFile(noteId).delete()
        getTranscriptFile(noteId).delete()
        getSummaryFile(noteId).delete()
    }
}

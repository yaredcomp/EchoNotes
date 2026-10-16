package com.example.domain.repository

import com.example.domain.model.AiAnalysisResult

interface AiIntelligenceRepository {
    suspend fun generateAnalysis(transcript: String, noteTitle: String): AiAnalysisResult
    suspend fun answerQuestion(transcript: String, noteTitle: String, question: String, chatHistory: List<Pair<Boolean, String>>): String
    suspend fun transcribeAudioFallback(audioFilePath: String): String
    suspend fun testConnection(testKey: String? = null): Result<String>
    fun getKeySourceDescription(): String
    fun isKeyConfigured(): Boolean
}

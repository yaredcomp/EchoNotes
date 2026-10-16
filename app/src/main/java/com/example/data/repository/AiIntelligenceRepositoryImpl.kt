package com.example.data.repository

import com.example.data.ai.GeminiAiClient
import com.example.domain.model.AiAnalysisResult
import com.example.domain.repository.AiIntelligenceRepository
import java.io.File

class AiIntelligenceRepositoryImpl(
    val geminiClient: GeminiAiClient = GeminiAiClient()
) : AiIntelligenceRepository {

    override suspend fun generateAnalysis(transcript: String, noteTitle: String): AiAnalysisResult {
        return geminiClient.generateAnalysis(transcript, noteTitle)
    }

    override suspend fun answerQuestion(
        transcript: String,
        noteTitle: String,
        question: String,
        chatHistory: List<Pair<Boolean, String>>
    ): String {
        return geminiClient.answerQuestion(transcript, noteTitle, question, chatHistory)
    }

    override suspend fun transcribeAudioFallback(audioFilePath: String): String {
        val file = File(audioFilePath)
        return geminiClient.transcribeAudioFallback(file)
    }

    override suspend fun testConnection(testKey: String?): Result<String> {
        return geminiClient.testConnection(testKey)
    }

    override fun getKeySourceDescription(): String {
        return geminiClient.getActiveKeySourceDescription()
    }

    override fun isKeyConfigured(): Boolean {
        return geminiClient.isApiKeyConfigured()
    }
}

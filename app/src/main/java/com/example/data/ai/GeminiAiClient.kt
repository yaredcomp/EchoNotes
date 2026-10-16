package com.example.data.ai

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.preferences.UserPreferencesManager
import com.example.domain.model.ActionItem
import com.example.domain.model.AiAnalysisResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiAiClient(private val context: Context? = null) {

    private val apiService: GeminiApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        apiService = retrofit.create(GeminiApiService::class.java)
    }

    fun getApiKey(): String {
        val ctx = context ?: applicationContext
        if (ctx != null) {
            val custom = UserPreferencesManager.getInstance(ctx).getCustomGeminiApiKey()
            if (custom.isNotBlank() && !isPlaceholderKey(custom)) {
                return custom
            }
        }

        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim().trim('"', '\'')
        } catch (_: Throwable) {
            ""
        }

        if (buildKey.isNotBlank() && !isPlaceholderKey(buildKey)) {
            return buildKey
        }

        return ""
    }

    fun isApiKeyConfigured(): Boolean {
        return getApiKey().isNotBlank()
    }

    fun getActiveKeySourceDescription(): String {
        val ctx = context ?: applicationContext
        if (ctx != null) {
            val custom = UserPreferencesManager.getInstance(ctx).getCustomGeminiApiKey()
            if (custom.isNotBlank() && !isPlaceholderKey(custom)) {
                val masked = if (custom.length > 8) "${custom.take(4)}...${custom.takeLast(4)}" else "****"
                return "Custom In-App Key ($masked)"
            }
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim().trim('"', '\'')
        } catch (_: Throwable) {
            ""
        }
        if (buildKey.isNotBlank() && !isPlaceholderKey(buildKey)) {
            val masked = if (buildKey.length > 8) "${buildKey.take(4)}...${buildKey.takeLast(4)}" else "****"
            return "Configured from .env / Build ($masked)"
        }
        return "No API Key configured"
    }

    fun getActiveModel(): String {
        val ctx = context ?: applicationContext
        if (ctx != null) {
            val savedModel = UserPreferencesManager.getInstance(ctx).getActiveGeminiModel()
            if (savedModel.isNotBlank()) {
                return savedModel
            }
        }
        return DEFAULT_FALLBACK_MODEL
    }

    private fun persistActiveModel(modelName: String) {
        val ctx = context ?: applicationContext
        if (ctx != null) {
            UserPreferencesManager.getInstance(ctx).setActiveGeminiModel(modelName)
        }
    }

    suspend fun discoverAvailableModels(apiKey: String): List<GeminiModelInfo> {
        val key = apiKey.trim().trim('"', '\'')
        if (key.isBlank() || isPlaceholderKey(key)) return emptyList()
        return try {
            val response = apiService.listModels(key)
            response.models?.filter { model ->
                val methods = model.supportedGenerationMethods ?: emptyList()
                methods.contains("generateContent")
            } ?: emptyList()
        } catch (e: Exception) {
            Log.w("GeminiAiClient", "Model discovery failed: ${e.message}")
            emptyList()
        }
    }

    fun getCandidateModelList(availableModels: List<GeminiModelInfo>, preferredFirst: String? = null): List<String> {
        val candidates = mutableListOf<String>()

        if (!preferredFirst.isNullOrBlank() && preferredFirst != "auto") {
            candidates.add(preferredFirst.removePrefix("models/").trim())
        } else {
            val saved = getActiveModel()
            if (saved.isNotBlank() && saved != "auto") {
                candidates.add(saved.removePrefix("models/").trim())
            }
        }

        val cleanDiscovered = availableModels.mapNotNull { it.name?.removePrefix("models/") }

        for (preferred in PREFERRED_MODELS) {
            val match = cleanDiscovered.firstOrNull { it.equals(preferred, ignoreCase = true) || it.startsWith("$preferred-") }
            if (match != null && !candidates.contains(match)) {
                candidates.add(match)
            }
        }

        for (disc in cleanDiscovered) {
            if (!candidates.contains(disc)) {
                candidates.add(disc)
            }
        }

        for (fallback in listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-flash-latest", DEFAULT_FALLBACK_MODEL)) {
            if (!candidates.contains(fallback)) {
                candidates.add(fallback)
            }
        }

        return candidates
    }

    fun selectBestModel(availableModels: List<GeminiModelInfo>): String {
        return getCandidateModelList(availableModels).firstOrNull() ?: DEFAULT_FALLBACK_MODEL
    }

    private suspend fun executeWithModelFallback(
        apiKey: String,
        request: GeminiRequest
    ): GeminiResponse {
        val candidateModels = mutableListOf<String>()
        val currentActive = getActiveModel()
        if (currentActive.isNotBlank() && currentActive != "auto") {
            candidateModels.add(currentActive)
        }
        for (pref in PREFERRED_MODELS) {
            if (!candidateModels.contains(pref)) {
                candidateModels.add(pref)
            }
        }

        var lastException: Exception? = null

        for (model in candidateModels) {
            try {
                val response = apiService.generateContent(model, apiKey, request)
                persistActiveModel(model)
                return response
            } catch (e: HttpException) {
                lastException = e
                val code = e.code()
                if (code == 503 || code == 404 || code == 429) {
                    Log.w("GeminiAiClient", "Model '$model' returned HTTP $code, attempting next candidate model...")
                    kotlinx.coroutines.delay(200)
                    continue
                } else {
                    throw e
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        throw lastException ?: Exception("All Gemini models temporarily unavailable")
    }

    suspend fun testConnection(testKeyCandidate: String? = null, modelPreference: String? = null): Result<String> {
        val key = (testKeyCandidate ?: getApiKey()).trim().trim('"', '\'')
        if (key.isBlank() || isPlaceholderKey(key)) {
            return Result.failure(
                IllegalArgumentException("Please enter a valid Gemini API key (starts with AIza...).")
            )
        }

        return try {
            val discovered = discoverAvailableModels(key)
            val candidates = getCandidateModelList(discovered, modelPreference)

            val pingRequest = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = "Respond with 'Connected'")),
                        role = "user"
                    )
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.0f)
            )

            var workingModel: String? = null
            var lastHttpException: HttpException? = null
            val overloadedModels = mutableListOf<String>()

            for (candidate in candidates) {
                try {
                    apiService.generateContent(candidate, key, pingRequest)
                    workingModel = candidate
                    break
                } catch (e: HttpException) {
                    lastHttpException = e
                    val code = e.code()
                    if (code == 503) {
                        overloadedModels.add(candidate)
                        Log.w("GeminiAiClient", "Model $candidate is overloaded (HTTP 503), trying alternate candidate...")
                        kotlinx.coroutines.delay(200)
                        continue
                    } else if (code == 404 || code == 429) {
                        Log.w("GeminiAiClient", "Model $candidate returned HTTP $code, trying alternate candidate...")
                        kotlinx.coroutines.delay(150)
                        continue
                    } else {
                        throw e
                    }
                }
            }

            if (workingModel != null) {
                persistActiveModel(workingModel)
                val activeModelName = getActiveModel()
                val modelCountStr = if (discovered.isNotEmpty()) {
                    "(${discovered.size} models auto-detected)"
                } else {
                    "(Direct fallback)"
                }

                val overloadNote = if (overloadedModels.isNotEmpty()) {
                    "\n\n⚡ ${overloadedModels.joinToString(", ")} is temporarily overloaded on Google's servers (HTTP 503). EchoNotes seamlessly connected to resilient $activeModelName."
                } else {
                    ""
                }

                Result.success("Success! Connected to $activeModelName $modelCountStr.$overloadNote")
            } else {
                throw lastHttpException ?: Exception("Could not connect to any Gemini models.")
            }
        } catch (e: HttpException) {
            val code = e.code()
            val msg = when (code) {
                400 -> "Invalid API Key or Bad Request (HTTP 400). Please check your key from Google AI Studio."
                403 -> "API Key permission denied (HTTP 403). Make sure the Generative Language API is enabled."
                404 -> "Gemini model endpoint not found (HTTP 404). Tried fallback models."
                429 -> "Gemini API quota exceeded (HTTP 429). Please wait a moment."
                503 -> "Google Gemini servers are temporarily overloaded (HTTP 503). We attempted fallbacks, but the service is currently experiencing heavy global load. Please wait a minute and tap Test Connection again."
                else -> "Gemini API error (HTTP $code): ${e.message()}"
            }
            Result.failure(Exception(msg))
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Unable to reach Google AI servers. Check internet connection."))
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message}"))
        }
    }

    suspend fun generateAnalysis(transcript: String, title: String): AiAnalysisResult {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            Log.w("GeminiAiClient", "API key missing or placeholder. Using resilient local intelligence fallback.")
            return generateLocalFallbackAnalysis(transcript, title)
        }

        val systemInstruction = "You are EchoNotes AI, an elite voice intelligence assistant. " +
                "You analyze audio transcripts to extract an Executive Summary, Key Takeaways, and actionable Tasks. " +
                "CRITICAL: Base your output STRICTLY on the provided transcript context. Never invent facts not spoken in the transcript. " +
                "Respond in valid JSON format matching this exact schema:\n" +
                "{\n" +
                "  \"executive_summary\": \"1-3 concise sentences highlighting core discussion points without markdown asterisks or brackets\",\n" +
                "  \"key_takeaways\": [\"Takeaway point 1\", \"Takeaway point 2\", \"Takeaway point 3\", \"Takeaway point 4\"],\n" +
                "  \"action_items\": [\"Task item 1\", \"Task item 2\", \"Task item 3\"]\n" +
                "}"

        val prompt = "Note Title: $title\n\nTRANSCRIPT CONTEXT:\n$transcript\n\nPlease extract the executive summary, key takeaways, and action items in JSON."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt)),
                    role = "user"
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemInstruction))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.2f,
                responseMimeType = "application/json"
            )
        )

        return try {
            val response = executeWithModelFallback(apiKey, request)
            val outputText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!outputText.isNullOrBlank()) {
                parseAnalysisResponse(outputText, transcript)
            } else {
                generateLocalFallbackAnalysis(transcript, title)
            }
        } catch (e: Exception) {
            Log.e("GeminiAiClient", "Gemini API error: ${e.message}", e)
            generateLocalFallbackAnalysis(transcript, title)
        }
    }

    private fun cleanMarkdown(text: String): String {
        return text
            .replace(HEADER_REGEX, "")
            .replace(LIST_PREFIX_REGEX, "")
            .replace(CHECKBOX_PREFIX_REGEX, "")
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .replace(QUOTE_BOUNDS_REGEX, "")
            .trim()
    }

    private fun parseAnalysisResponse(response: String, fallbackTranscript: String): AiAnalysisResult {
        return parseJsonResponse(response, fallbackTranscript)
            ?: parseMarkdownSections(response, fallbackTranscript)
    }

    private fun parseJsonResponse(response: String, fallbackTranscript: String): AiAnalysisResult? {
        val jsonStr = extractJsonSubstring(response) ?: return null
        return try {
            val jsonObj = org.json.JSONObject(jsonStr)
            val summary = cleanMarkdown(
                jsonObj.optString("executive_summary", jsonObj.optString("summary", ""))
            )
            if (summary.isBlank()) return null

            val takeaways = parseJsonArray(jsonObj, "key_takeaways", "takeaways")
                .map { cleanMarkdown(it) }
                .filter { it.isNotBlank() }

            val actionItems = parseJsonArray(jsonObj, "action_items", "actions", "tasks")
                .mapIndexedNotNull { index, raw ->
                    val task = cleanMarkdown(raw)
                    if (task.isNotBlank()) ActionItem(title = task, isCompleted = index == 0) else null
                }

            AiAnalysisResult(
                executiveSummary = summary,
                keyTakeaways = takeaways.ifEmpty { extractDynamicTakeaways(fallbackTranscript) },
                actionItems = actionItems.ifEmpty { getDefaultActionItems(fallbackTranscript) }
            )
        } catch (jsonEx: Exception) {
            Log.w("GeminiAiClient", "JSON parse attempt failed, trying markdown section parser: ${jsonEx.message}")
            null
        }
    }

    private fun extractJsonSubstring(response: String): String? {
        val jsonStartIndex = response.indexOf('{')
        val jsonEndIndex = response.lastIndexOf('}')
        if (jsonStartIndex != -1 && jsonEndIndex > jsonStartIndex) {
            return response.substring(jsonStartIndex, jsonEndIndex + 1)
        }
        return null
    }

    private fun parseJsonArray(jsonObj: org.json.JSONObject, vararg keys: String): List<String> {
        for (key in keys) {
            val arr = jsonObj.optJSONArray(key) ?: continue
            val result = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                result.add(arr.optString(i))
            }
            return result
        }
        return emptyList()
    }

    private fun parseMarkdownSections(response: String, fallbackTranscript: String): AiAnalysisResult {
        val parsedSections = processMarkdownLines(response.lines())
        val finalSummary = buildFinalSummary(parsedSections.summary, fallbackTranscript)

        return AiAnalysisResult(
            executiveSummary = finalSummary,
            keyTakeaways = parsedSections.takeaways.ifEmpty { extractDynamicTakeaways(fallbackTranscript) },
            actionItems = parsedSections.actionItems.ifEmpty { getDefaultActionItems(fallbackTranscript) }
        )
    }

    private data class ParsedMarkdownData(
        var summary: String = "",
        val takeaways: MutableList<String> = mutableListOf(),
        val actionItems: MutableList<ActionItem> = mutableListOf()
    )

    private fun processMarkdownLines(lines: List<String>): ParsedMarkdownData {
        val data = ParsedMarkdownData()
        var currentSection = ""

        for (line in lines) {
            val raw = line.trim()
            if (raw.isEmpty()) continue

            val detectedSection = detectSectionHeader(raw)
            if (detectedSection != null) {
                currentSection = detectedSection
                data.summary = extractInlineSummary(data.summary, currentSection, raw)
            } else {
                processNonHeaderLine(data, currentSection, raw)
            }
        }
        return data
    }

    private fun processNonHeaderLine(data: ParsedMarkdownData, currentSection: String, rawLine: String) {
        val cleaned = cleanMarkdown(rawLine)
        if (!isIgnorableMarkdownLine(cleaned)) {
            appendSectionContent(data, currentSection, cleaned)
        }
    }

    private fun extractInlineSummary(existingSummary: String, currentSection: String, rawLine: String): String {
        if (currentSection != "SUMMARY" || existingSummary.isNotBlank()) return existingSummary
        val inline = rawLine.substringAfter(":", "").trim()
        return if (inline.isNotEmpty() && !inline.startsWith("#") && !inline.startsWith("{")) {
            cleanMarkdown(inline)
        } else {
            existingSummary
        }
    }

    private fun appendSectionContent(data: ParsedMarkdownData, section: String, cleanedContent: String) {
        when (section) {
            "SUMMARY" -> data.summary = if (data.summary.isEmpty()) cleanedContent else "${data.summary} $cleanedContent"
            "TAKEAWAYS" -> data.takeaways.add(cleanedContent)
            "ACTIONS" -> data.actionItems.add(ActionItem(title = cleanedContent, isCompleted = data.actionItems.isEmpty()))
        }
    }

    private fun detectSectionHeader(rawLine: String): String? {
        val upper = rawLine.uppercase(Locale.ROOT)
        return when {
            upper.contains("EXECUTIVE_SUMMARY") || upper.contains("EXECUTIVE SUMMARY") -> "SUMMARY"
            upper.contains("KEY_TAKEAWAYS") || upper.contains("KEY TAKEAWAYS") -> "TAKEAWAYS"
            upper.contains("ACTION_ITEMS") || upper.contains("ACTION ITEMS") || upper.contains("NEXT STEPS") || upper.contains("TASKS") -> "ACTIONS"
            else -> null
        }
    }

    private fun isIgnorableMarkdownLine(cleaned: String): Boolean {
        return cleaned.isEmpty() || cleaned == "{" || cleaned == "}" || cleaned == "[]"
    }

    private fun buildFinalSummary(extractedSummary: String, fallbackTranscript: String): String {
        return when {
            extractedSummary.isNotBlank() -> cleanMarkdown(extractedSummary)
            fallbackTranscript.isNotBlank() -> {
                val truncated = fallbackTranscript.take(220)
                cleanMarkdown(truncated) + if (fallbackTranscript.length > 220) "..." else ""
            }
            else -> "Voice audio session recorded and transcribed."
        }
    }

    private fun extractDynamicTakeaways(transcript: String): List<String> {
        val clean = transcript.trim()
        if (clean.isBlank()) {
            return listOf("Spoken audio note captured and stored securely.")
        }

        val sentences = extractSentenceTakeaways(clean)
        if (sentences.isNotEmpty()) {
            return sentences
        }

        val chunked = chunkTextIntoTakeaways(clean)
        if (chunked.isNotEmpty()) {
            return chunked
        }

        return listOf(clean.take(150))
    }

    private fun extractSentenceTakeaways(cleanTranscript: String): List<String> {
        val rawSentences = cleanTranscript.split(SENTENCE_SPLIT_REGEX)
            .map { cleanMarkdown(it).trim() }
            .filter { it.length > 8 }

        return rawSentences.take(4).map { formatSentence(it) }
    }

    private fun formatSentence(sentence: String): String {
        val formatted = sentence.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        return if (formatted.endsWith('.') || formatted.endsWith('!') || formatted.endsWith('?')) {
            formatted
        } else {
            "$formatted."
        }
    }

    private fun chunkTextIntoTakeaways(cleanTranscript: String): List<String> {
        val words = cleanTranscript.split(WORD_SPLIT_REGEX)
        if (words.size <= 6) return emptyList()

        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            if (current.length + word.length > 60 && current.isNotEmpty()) {
                chunks.add("${current.toString().trim()}.")
                current = StringBuilder()
                if (chunks.size == 4) break
            }
            if (current.isNotEmpty()) current.append(" ")
            current.append(word)
        }
        if (current.isNotEmpty() && chunks.size < 4) {
            chunks.add("${current.toString().trim()}.")
        }
        return chunks
    }

    private fun getDefaultActionItems(transcript: String): List<ActionItem> {
        val clean = transcript.trim()
        val topic = if (clean.length > 20) clean.take(25) + "..." else "recorded discussion"
        return listOf(
            ActionItem(title = "Review $topic", isCompleted = true),
            ActionItem(title = "Follow up on discussed notes and decisions", isCompleted = false)
        )
    }

    suspend fun answerQuestion(
        transcript: String,
        noteTitle: String,
        question: String,
        chatHistory: List<Pair<Boolean, String>>
    ): String {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return "⚠️ Gemini API key is missing or not configured. Please tap the Key icon in the top bar or open Settings to enter your Gemini API key (or configure GEMINI_API_KEY in your root .env file), then try asking again."
        }

        val systemInstruction = "You are EchoNotes AI. You answer user questions about a specific recorded voice note. " +
                "CRITICAL RULES:\n" +
                "1. Answer ONLY based on the facts explicitly mentioned in the TRANSCRIPT CONTEXT below.\n" +
                "2. If the user asks something that is NOT in the transcript, state: 'That detail was not mentioned in this recording.'\n" +
                "3. Structure your response into clear topic points when explaining takeaways or tasks.\n" +
                "4. Avoid raw markdown hashtags."

        val contents = mutableListOf<GeminiContent>()

        val contextPrompt = "NOTE TITLE: $noteTitle\n\nTRANSCRIPT CONTEXT:\n$transcript\n\nPlease answer questions based solely on this note."
        contents.add(GeminiContent(parts = listOf(GeminiPart(text = contextPrompt)), role = "user"))
        contents.add(GeminiContent(parts = listOf(GeminiPart(text = "Understood. I will answer all questions strictly based on this transcript context.")), role = "model"))

        chatHistory.takeLast(6).forEach { (isUser, msg) ->
            contents.add(
                GeminiContent(
                    parts = listOf(GeminiPart(text = msg)),
                    role = if (isUser) "user" else "model"
                )
            )
        }

        contents.add(GeminiContent(parts = listOf(GeminiPart(text = question)), role = "user"))

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstruction))),
            generationConfig = GeminiGenerationConfig(temperature = 0.2f)
        )

        return try {
            val response = executeWithModelFallback(apiKey, request)
            val answer = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!answer.isNullOrBlank()) {
                answer.trim()
            } else {
                generateLocalFallbackAnswer(transcript, question)
            }
        } catch (e: Exception) {
            Log.e("GeminiAiClient", "Gemini chat error: ${e.message}", e)
            generateLocalFallbackAnswer(transcript, question)
        }
    }

    suspend fun transcribeAudioFallback(audioFile: File): String {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || !audioFile.exists() || audioFile.length() == 0L) {
            return ""
        }

        if (audioFile.length() > 8 * 1024 * 1024) {
            return ""
        }

        return try {
            val bytes = audioFile.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = "Please transcribe all spoken words in this audio recording clearly. Output only the transcription text."),
                            GeminiPart(inlineData = GeminiInlineData(mimeType = "audio/mp4", data = base64))
                        ),
                        role = "user"
                    )
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.1f)
            )

            val response = executeWithModelFallback(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
        } catch (e: Exception) {
            Log.e("GeminiAiClient", "Audio fallback transcription error: ${e.message}")
            ""
        }
    }

    private fun generateLocalFallbackAnalysis(transcript: String, title: String): AiAnalysisResult {
        val clean = transcript.trim()
        val summary = if (clean.isNotBlank()) {
            cleanMarkdown(clean)
        } else {
            "Voice note session recorded for $title."
        }

        val takeaways = extractDynamicTakeaways(clean)
        val actionItems = getDefaultActionItems(clean)

        return AiAnalysisResult(
            executiveSummary = summary,
            keyTakeaways = takeaways,
            actionItems = actionItems
        )
    }

    private fun generateLocalFallbackAnswer(transcript: String, question: String): String {
        val cleanTranscript = transcript.trim()
        val cleanQuestion = question.trim().lowercase(Locale.ROOT)

        if (cleanTranscript.isBlank()) {
            return "No spoken transcript is recorded for this audio note yet."
        }

        val sentences = cleanTranscript.split(SENTENCE_SPLIT_REGEX)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val questionKeywords = extractQuestionKeywords(cleanQuestion)
        val matchedSentences = findMatchingSentences(sentences, questionKeywords)

        return buildAnswerString(matchedSentences, cleanQuestion, sentences, cleanTranscript)
    }

    private fun extractQuestionKeywords(cleanQuestion: String): List<String> {
        val ignoreWords = setOf("what", "where", "when", "which", "about", "this", "were", "there", "have", "with")
        return cleanQuestion.split(KEYWORD_SPLIT_REGEX)
            .filter { it.length > 3 && it !in ignoreWords }
    }

    private fun findMatchingSentences(sentences: List<String>, keywords: List<String>): List<String> {
        if (keywords.isEmpty()) return emptyList()
        return sentences.filter { sentence ->
            val lower = sentence.lowercase(Locale.ROOT)
            keywords.any { kw -> lower.contains(kw) }
        }
    }

    private fun buildAnswerString(
        matchedSentences: List<String>,
        cleanQuestion: String,
        sentences: List<String>,
        cleanTranscript: String
    ): String = buildString {
        when {
            matchedSentences.isNotEmpty() -> {
                appendLine("Here is what was mentioned regarding your question:")
                appendLine()
                matchedSentences.take(3).forEach { s -> appendLine("● $s") }
            }
            isSummaryOrTakeawayQuestion(cleanQuestion) -> {
                appendLine("Here are the main points discussed in this recording:")
                appendLine()
                sentences.take(4).forEach { s -> appendLine("● $s") }
            }
            else -> {
                appendLine("Based on this note's transcript:")
                appendLine()
                appendFallbackSentenceList(sentences, cleanTranscript)
            }
        }
    }

    private fun isSummaryOrTakeawayQuestion(question: String): Boolean {
        return question.contains("summary") || question.contains("main") ||
                question.contains("point") || question.contains("takeaway")
    }

    private fun StringBuilder.appendFallbackSentenceList(sentences: List<String>, cleanTranscript: String) {
        if (sentences.isNotEmpty()) {
            appendLine("● ${sentences.first()}")
            if (sentences.size > 1) {
                appendLine("● ${sentences[1]}")
            }
        } else {
            appendLine("● $cleanTranscript")
        }
    }

    companion object {
        const val DEFAULT_FALLBACK_MODEL = "gemini-1.5-flash"

        val PREFERRED_MODELS = listOf(
            "gemini-1.5-flash",
            "gemini-2.0-flash",
            "gemini-flash-latest",
            "gemini-1.5-flash-8b",
            "gemini-2.0-flash-lite",
            "gemini-1.5-pro"
        )

        @Volatile
        private var applicationContext: Context? = null

        fun init(context: Context) {
            applicationContext = context.applicationContext
        }

        fun isPlaceholderKey(key: String): Boolean {
            val clean = key.trim().trim('"', '\'')
            return clean.isBlank() ||
                    clean.equals("MY_GEMINI_API_KEY", ignoreCase = true) ||
                    clean.equals("DEFAULT_API_KEY", ignoreCase = true) ||
                    clean.equals("your_gemini_api_key_here", ignoreCase = true) ||
                    clean.equals("YOUR_API_KEY", ignoreCase = true) ||
                    clean.length < 15
        }

        private val HEADER_REGEX = Regex("^#+\\s*")
        private val LIST_PREFIX_REGEX = Regex("^[-*•\\d.]+\\s*")
        private val CHECKBOX_PREFIX_REGEX = Regex("^\\[[ xX]]\\s*")
        private val QUOTE_BOUNDS_REGEX = Regex("(^\")|(\"$)")
        private val SENTENCE_SPLIT_REGEX = Regex("(?<=[.!?\\n])\\s+")
        private val WORD_SPLIT_REGEX = Regex("\\s+")
        private val KEYWORD_SPLIT_REGEX = Regex("[^a-zA-Z0-9]+")
    }
}

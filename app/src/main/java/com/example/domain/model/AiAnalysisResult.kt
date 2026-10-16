package com.example.domain.model

data class AiAnalysisResult(
    val executiveSummary: String,
    val keyTakeaways: List<String>,
    val actionItems: List<ActionItem>
)

package com.example.ai.model

data class LlmRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val contextChunks: List<RetrievedChunk> = emptyList(),
    val maxTokens: Int = 1024,
    val temperature: Float = 0.2f, // Low temperature for high factual grounding
    val stopSequences: List<String> = emptyList()
)

data class LlmToken(
    val token: String,
    val isFinished: Boolean = false
)

data class GroundedCitation(
    val documentId: Long,
    val documentTitle: String,
    val chapterId: Long? = null,
    val chapterTitle: String? = null,
    val pageIndex: Int,
    val physicalPage: Int,
    val printedPage: String? = null,
    val quoteSnippet: String,
    val relevanceScore: Float = 1.0f
)

data class GroundedAnswer(
    val answer: String,
    val citations: List<GroundedCitation> = emptyList(),
    val sourceQuality: String = "GOOD", // GOOD, LIMITED, POOR
    val isGrounded: Boolean = true,
    val rawRetrievedChunks: List<RetrievedChunk> = emptyList(),
    val confidenceLevel: String = "SUPPORTED" // SUPPORTED, PARTIAL, UNSUPPORTED
)

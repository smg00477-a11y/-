package com.example.ai.engine

import com.example.ai.model.GroundedAnswer
import com.example.ai.model.LlmRequest
import com.example.ai.model.LlmToken
import kotlinx.coroutines.flow.Flow

interface LocalLlmEngine {
    val modelName: String
    val modelVersion: String
    val isReady: Boolean

    /**
     * Streams tokens for a conversational or Q&A prompt.
     */
    fun generateStream(request: LlmRequest): Flow<LlmToken>

    /**
     * Synthesizes a grounded answer from retrieved chunks, enforcing strict citations
     * and anti-hallucination rules.
     */
    suspend fun generateGrounded(request: LlmRequest): GroundedAnswer

    /**
     * Releases or unloads any heavy native memory resources.
     */
    fun unload()
}

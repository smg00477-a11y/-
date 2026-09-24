package com.example.ai.engine

interface EmbeddingEngine {
    val modelId: String
    val modelVersion: String
    val dimensions: Int

    /**
     * Computes a dense semantic vector for the provided text.
     * The resulting vector must be unit-normalized (L2 norm = 1.0).
     */
    suspend fun embed(text: String): FloatArray

    /**
     * Computes cosine similarity between two unit-normalized vectors.
     */
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float
}

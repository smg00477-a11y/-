package com.example.ai.model

data class RetrievedChunk(
    val chunkId: Long,
    val documentId: Long,
    val documentTitle: String,
    val pageId: Long,
    val pageIndex: Int,
    val physicalPageNumber: Int,
    val printedPageNumber: String? = null,
    val chapterId: Long? = null,
    val chapterTitle: String? = null,
    val text: String,
    val normalizedText: String,
    val score: Float,
    val matchType: String = "HYBRID", // HYBRID, SEMANTIC, LEXICAL
    val ocrQuality: String = "GOOD" // GOOD, LIMITED, POOR
)

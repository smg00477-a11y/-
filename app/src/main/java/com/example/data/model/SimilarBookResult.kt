package com.example.data.model

import com.example.data.database.entity.DocumentWithPages

data class SimilarBookResult(
    val documentWithPages: DocumentWithPages,
    val similarityScore: Float, // 0.0 to 1.0
    val sharedCategories: List<String> = emptyList(),
    val sharedAuthor: Boolean = false,
    val sharedSeries: String? = null,
    val sharedTopics: List<String> = emptyList(),
    val sharedKeywords: List<String> = emptyList(),
    val primaryReason: String = ""
)

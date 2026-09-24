package com.example.data.model

data class LibraryStats(
    val totalDocuments: Int = 0,
    val totalPages: Int = 0,
    val totalAuthors: Int = 0,
    val totalCategories: Int = 0,
    val totalSeries: Int = 0,
    val documentsWithOcr: Int = 0,
    val pagesWithOcr: Int = 0,
    val ocrCoveragePercent: Int = 0,
    val totalFavorites: Int = 0,
    val totalTextChunks: Int = 0,
    val totalEmbeddings: Int = 0,
    val totalChapters: Int = 0,
    val totalBookmarks: Int = 0,
    val totalNotes: Int = 0,
    val calculatedAt: Long = System.currentTimeMillis()
)

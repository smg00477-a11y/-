package com.example.data.model

data class LibraryFilterOptions(
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val authorId: Long? = null,
    val authorName: String? = null,
    val seriesId: Long? = null,
    val seriesName: String? = null,
    val onlyFavorites: Boolean = false,
    val onlyWithOcr: Boolean = false,
    val onlyWithoutOcr: Boolean = false,
    val documentType: DocumentType? = null
) {
    val isActive: Boolean
        get() = categoryId != null ||
                categoryName != null ||
                authorId != null ||
                authorName != null ||
                seriesId != null ||
                seriesName != null ||
                onlyFavorites ||
                onlyWithOcr ||
                onlyWithoutOcr ||
                documentType != null

    val activeFilterCount: Int
        get() {
            var count = 0
            if (categoryId != null || categoryName != null) count++
            if (authorId != null || authorName != null) count++
            if (seriesId != null || seriesName != null) count++
            if (onlyFavorites) count++
            if (onlyWithOcr || onlyWithoutOcr) count++
            if (documentType != null) count++
            return count
        }
}

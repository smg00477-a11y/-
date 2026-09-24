package com.example.data.model

import com.example.data.database.entity.DocumentWithPages

data class DocumentSearchResult(
    val documentWithPages: DocumentWithPages,
    val matchedInTitle: Boolean,
    val matchedPageNumber: Int? = null,
    val matchedPageId: Long? = null,
    val matchedSnippet: String? = null,
    val matchedInAuthor: Boolean = false,
    val matchedInChapter: String? = null,
    val matchedInCategory: Boolean = false
)

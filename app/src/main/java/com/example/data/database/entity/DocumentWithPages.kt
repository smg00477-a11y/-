package com.example.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DocumentWithPages(
    @Embedded
    val document: DocumentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val pages: List<PageEntity>
) {
    val sortedPages: List<PageEntity>
        get() = pages.sortedBy { it.pageIndex }

    val pageCount: Int
        get() = pages.size

    val coverFilePath: String?
        get() = sortedPages.firstOrNull()?.displayThumbnailPath
}

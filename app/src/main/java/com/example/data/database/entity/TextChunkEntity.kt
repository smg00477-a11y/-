package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "text_chunks",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["pageId"]),
        Index(value = ["chapterId"]),
        Index(value = ["documentId", "pageIndex"])
    ]
)
data class TextChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val pageId: Long,
    val pageIndex: Int,
    val chapterId: Long? = null,
    val sectionTitle: String? = null,
    val physicalPageNumber: Int = 1,
    val printedPageNumber: String? = null,
    val chunkIndex: Int = 0,
    val text: String,
    val normalizedText: String = "",
    val tokenCount: Int = 0,
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

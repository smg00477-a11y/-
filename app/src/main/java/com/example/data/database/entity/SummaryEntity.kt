package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["documentId", "targetType", "targetId"], unique = true)
    ]
)
data class SummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val targetType: String = "BOOK", // BOOK, CHAPTER, PAGE, SECTION
    val targetId: Long = 0L, // 0 for BOOK, chapterId for CHAPTER, pageIndex/pageId for PAGE
    val title: String = "",
    val content: String,
    val keyIdeasJson: String = "[]", // JSON array of bullet strings
    val sourcePagesJson: String = "[]", // JSON array of Int pages
    val sourceChunksJson: String = "[]", // JSON array of Long chunkIds
    val modelUsed: String = "raqeem-arabic-rag-v1",
    val modelVersion: String = "1.0",
    val isUserEdited: Boolean = false,
    val status: String = "COMPLETED", // COMPLETED, PROCESSING, STALE, FAILED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

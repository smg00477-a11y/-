package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
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
        Index(value = ["documentId", "readingOrder"]),
        Index(value = ["documentId", "startPageIndex"])
    ]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val parentChapterId: Long? = null,
    val title: String,
    val normalizedTitle: String = "",
    val startPageIndex: Int, // 0-indexed physical page
    val endPageIndex: Int,   // 0-indexed physical page
    val readingOrder: Int = 0,
    val level: Int = 1,      // 1 = Part/Chapter, 2 = Section, 3 = Subsection
    val detectionSource: String = "AUTO_HEADING", // AUTO_HEADING, AUTO_TOC, MANUAL
    val confidence: Float = 1.0f,
    val manuallyConfirmed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

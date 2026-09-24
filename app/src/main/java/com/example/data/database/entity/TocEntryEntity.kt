package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "toc_entries",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedChapterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["documentId", "readingOrder"]),
        Index(value = ["linkedChapterId"])
    ]
)
data class TocEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val title: String,
    val targetPhysicalPageIndex: Int = 0,
    val targetPrintedPage: String = "",
    val linkedChapterId: Long? = null,
    val level: Int = 1,
    val readingOrder: Int = 0,
    val confidence: Float = 1.0f,
    val detectionStatus: String = "DETECTED" // DETECTED, USER_CONFIRMED, USER_EDITED
)

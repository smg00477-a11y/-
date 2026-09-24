package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_insights",
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
        Index(value = ["insightType"])
    ]
)
data class AiInsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val insightType: String, // QUOTATION, CHAPTER_RELATION, KEY_CONCEPT
    val primaryId: Long = 0L, // e.g. chapterId or pageIndex
    val secondaryId: Long? = null, // e.g. relatedChapterId
    val title: String,
    val content: String,
    val score: Float = 0f,
    val isSaved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

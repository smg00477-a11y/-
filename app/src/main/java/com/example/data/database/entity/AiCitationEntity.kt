package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_citations",
    foreignKeys = [
        ForeignKey(
            entity = AiMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["messageId"]),
        Index(value = ["documentId", "pageIndex"])
    ]
)
data class AiCitationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long,
    val documentId: Long,
    val documentTitle: String,
    val chapterId: Long? = null,
    val chapterTitle: String? = null,
    val pageIndex: Int,
    val physicalPage: Int,
    val printedPage: String? = null,
    val quoteSnippet: String,
    val relevanceScore: Float = 1.0f
)

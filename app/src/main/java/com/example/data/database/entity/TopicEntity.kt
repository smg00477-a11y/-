package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
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
        Index(value = ["documentId", "normalizedTopic"])
    ]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val topic: String,
    val normalizedTopic: String,
    val confidence: Float = 0.85f,
    val sourceDescription: String = "",
    val relatedChapterIdsJson: String = "[]",
    val relatedPagesJson: String = "[]",
    val status: String = "AUTO_DETECTED", // AUTO_DETECTED, SUGGESTED, USER_CONFIRMED, USER_EDITED, USER_REJECTED
    val createdAt: Long = System.currentTimeMillis()
)

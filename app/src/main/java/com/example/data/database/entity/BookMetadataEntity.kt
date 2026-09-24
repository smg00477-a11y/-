package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_metadata",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AuthorEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["documentId"], unique = true),
        Index(value = ["authorId"])
    ]
)
data class BookMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val title: String,
    val subtitle: String = "",
    val author: String = "",
    val authorId: Long? = null,
    val coAuthors: String = "",
    val translator: String = "",
    val editor: String = "",
    val publisher: String = "",
    val publicationYear: String = "",
    val edition: String = "",
    val isbn: String = "",
    val language: String = "ar",
    val category: String = "عام",
    val subject: String = "",
    val tags: String = "",
    val description: String = "",
    val notes: String = "",
    val originalPageCount: Int = 0,
    val detectionStatus: String = "UNKNOWN", // DETECTED, USER_CONFIRMED, USER_EDITED, UNKNOWN
    val archiveDate: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

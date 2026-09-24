package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "book_authors",
    primaryKeys = ["documentId", "authorId", "role"],
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
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["authorId"])
    ]
)
data class BookAuthorCrossRef(
    val documentId: Long,
    val authorId: Long,
    val role: String = "AUTHOR", // AUTHOR, TRANSLATOR, EDITOR, CO_AUTHOR
    val assignedAt: Long = System.currentTimeMillis()
)

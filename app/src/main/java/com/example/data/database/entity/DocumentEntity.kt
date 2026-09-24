package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DocumentType

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: DocumentType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val authorId: Long? = null,
    val authorName: String = "غير محدد",
    val category: String = "عام",
    val originalPageCount: Int = 0
)

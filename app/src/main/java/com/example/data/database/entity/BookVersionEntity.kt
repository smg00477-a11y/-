package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_versions",
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
        Index(value = ["documentId", "versionNumber"])
    ]
)
data class BookVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val versionNumber: Int,
    val versionTag: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val checksumSha256: String = "",
    val metadataSnapshotJson: String = "",
    val pagesSnapshotJson: String = "",
    val changeSummary: String = ""
)

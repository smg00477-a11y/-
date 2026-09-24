package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "book_series",
    primaryKeys = ["documentId", "seriesId"],
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["seriesId"]),
        Index(value = ["seriesId", "volumeNumber"])
    ]
)
data class BookSeriesCrossRef(
    val documentId: Long,
    val seriesId: Long,
    val volumeNumber: Int = 1,
    val volumeTitle: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

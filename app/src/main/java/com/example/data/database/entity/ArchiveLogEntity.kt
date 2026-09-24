package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "archive_logs")
data class ArchiveLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String, // EXPORT, IMPORT, COMPRESSION, HEALTH_CHECK, RESTORATION, D2D_TRANSFER
    val documentId: Long? = null,
    val status: String, // SUCCESS, WARNING, FAILED
    val details: String = "",
    val checksumSha256: String = "",
    val fileSize: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

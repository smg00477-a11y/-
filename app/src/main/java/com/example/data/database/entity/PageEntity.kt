package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
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
        Index(value = ["documentId", "pageIndex"])
    ]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val localFilePath: String, // Original image file path (PROTECTED - NEVER OVERWRITTEN)
    val processedFilePath: String? = null, // Processed image file path
    val thumbnailPath: String? = null, // Lightweight thumbnail file path
    val processingState: String = "NOT_PROCESSED", // NOT_PROCESSED, PROCESSING, COMPLETED, FAILED
    val processingMode: String = "ORIGINAL", // ORIGINAL, AUTO, DOCUMENT, ENHANCED, GRAYSCALE, BLACK_AND_WHITE
    val rotationDegrees: Int = 0,
    val ocrStatus: String = "NOT_PROCESSED", // NOT_PROCESSED, PROCESSING, COMPLETED, FAILED
    val ocrRawText: String = "",
    val ocrCleanedText: String = "",
    val ocrUserEditedText: String? = null,
    val ocrLanguage: String = "ara+eng",
    val ocrConfidence: Float = 0f,
    val ocrErrorMessage: String? = null,
    val pageType: String = "UNKNOWN", // PRINTED, HANDWRITTEN, MIXED, IMAGE_ONLY, TABLE_HEAVY, COMPLEX_LAYOUT, UNKNOWN
    val ocrEngineUsed: String = "Tesseract", // Tesseract, Handwriting, Neural, LayoutAnalysis
    val ocrUpdatedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayFilePath: String
        get() = processedFilePath ?: localFilePath

    val displayThumbnailPath: String
        get() = thumbnailPath ?: processedFilePath ?: localFilePath

    val effectiveOcrText: String
        get() = ocrUserEditedText?.takeIf { it.isNotBlank() }
            ?: ocrCleanedText.takeIf { it.isNotBlank() }
            ?: ocrRawText

    val isOcrCompleted: Boolean
        get() = ocrStatus == "COMPLETED" && effectiveOcrText.isNotBlank()
}

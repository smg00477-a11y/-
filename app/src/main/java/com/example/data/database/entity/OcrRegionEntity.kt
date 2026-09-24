package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ocr.model.OcrBoundingBox
import com.example.ocr.model.OcrRegion
import com.example.ocr.model.PageType
import com.example.ocr.model.RegionType

@Entity(
    tableName = "ocr_regions",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["pageId"]),
        Index(value = ["pageId", "readingOrder"])
    ]
)
data class OcrRegionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pageId: Long,
    val regionType: String = "MAIN_TEXT", // TITLE, MAIN_TEXT, FOOTNOTE, HEADER, FOOTER, MARGINAL_NOTE, etc.
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val readingOrder: Int = 0,
    val confidence: Float = 0f,
    val rawText: String = "",
    val cleanedText: String = "",
    val userEditedText: String? = null,
    val engine: String = "Tesseract",
    val pageType: String = "PRINTED",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val effectiveText: String
        get() = userEditedText?.takeIf { it.isNotBlank() }
            ?: cleanedText.takeIf { it.isNotBlank() }
            ?: rawText

    fun toOcrRegion(): OcrRegion {
        return OcrRegion(
            id = id,
            pageId = pageId,
            regionType = RegionType.fromName(regionType),
            box = OcrBoundingBox(left, top, right, bottom),
            readingOrder = readingOrder,
            confidence = confidence,
            rawText = rawText,
            cleanedText = cleanedText,
            userEditedText = userEditedText,
            engine = engine,
            pageType = PageType.fromName(pageType)
        )
    }

    companion object {
        fun fromOcrRegion(region: OcrRegion, pageId: Long): OcrRegionEntity {
            return OcrRegionEntity(
                id = region.id,
                pageId = pageId,
                regionType = region.regionType.name,
                left = region.box.left,
                top = region.box.top,
                right = region.box.right,
                bottom = region.box.bottom,
                readingOrder = region.readingOrder,
                confidence = region.confidence,
                rawText = region.rawText,
                cleanedText = region.cleanedText,
                userEditedText = region.userEditedText,
                engine = region.engine,
                pageType = region.pageType.name
            )
        }
    }
}

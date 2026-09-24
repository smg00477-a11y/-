package com.example.ocr.analysis

import android.graphics.Bitmap
import com.example.ocr.model.OcrBlock
import com.example.ocr.model.OcrBoundingBox
import com.example.ocr.model.OcrLine
import com.example.ocr.model.OcrRegion
import com.example.ocr.model.PageType
import com.example.ocr.model.RegionType
import kotlin.math.abs

object LayoutAnalysisEngine {

    /**
     * Performs Arabic document layout analysis and segments blocks into structured regions,
     * assigning strict Arabic Right-To-Left (RTL) reading order:
     * 1. Header (ترويسة علوية)
     * 2. Title (عنوان رئيسي)
     * 3. Multi-column body: Right Column FIRST (العمود الأيمن أولاً), then Left Column
     * 4. Marginal notes (هوامش جانبية)
     * 5. Footnotes (حواشي سفلية)
     * 6. Footer & Page Number (تذييل ورقم الصفحة)
     */
    fun segmentAndOrderRegions(
        pageId: Long,
        bitmapWidth: Int,
        bitmapHeight: Int,
        blocks: List<OcrBlock>,
        pageType: PageType,
        engineName: String = "Tesseract"
    ): List<OcrRegion> {
        if (blocks.isEmpty()) {
            return emptyList()
        }

        val headerThresholdY = (bitmapHeight * 0.10f).toInt()
        val footerThresholdY = (bitmapHeight * 0.88f).toInt()
        val footnoteThresholdY = (bitmapHeight * 0.76f).toInt()
        val leftMarginThresholdX = (bitmapWidth * 0.16f).toInt()
        val rightMarginThresholdX = (bitmapWidth * 0.84f).toInt()
        val midX = bitmapWidth / 2

        // Detect if page exhibits two-column behavior
        var rightColCount = 0
        var leftColCount = 0
        for (b in blocks) {
            val box = b.box ?: continue
            val centerY = (box.top + box.bottom) / 2
            if (centerY in headerThresholdY..footnoteThresholdY) {
                if (box.left >= midX) rightColCount++
                if (box.right <= midX) leftColCount++
            }
        }
        val isTwoColumns = rightColCount >= 2 && leftColCount >= 2

        // Categorize each block into a candidate region
        val classifiedRegions = mutableListOf<OcrRegion>()

        for (block in blocks) {
            val box = block.box ?: OcrBoundingBox(0, 0, bitmapWidth, bitmapHeight)
            val centerY = (box.top + box.bottom) / 2
            val centerX = (box.left + box.right) / 2

            val regionType = when {
                // Top 10%
                centerY < headerThresholdY -> {
                    if (box.width < bitmapWidth * 0.15f) RegionType.PAGE_NUMBER else RegionType.HEADER
                }
                // Bottom 12%
                centerY > footerThresholdY -> {
                    if (box.width < bitmapWidth * 0.15f) RegionType.PAGE_NUMBER else RegionType.FOOTER
                }
                // Marginal notes on extreme left or right outer bands
                (box.right < leftMarginThresholdX || box.left > rightMarginThresholdX) && box.width < bitmapWidth * 0.25f -> {
                    RegionType.MARGINAL_NOTE
                }
                // Footnote area (between 76% and 88%)
                centerY in footnoteThresholdY..footerThresholdY && (block.lines.size <= 4 || block.confidence < 70) -> {
                    RegionType.FOOTNOTE
                }
                // Check if top block of main body is Title (centered, short, high in page)
                centerY < (bitmapHeight * 0.25f) && block.lines.size <= 2 && abs(centerX - midX) < (bitmapWidth * 0.20f) -> {
                    RegionType.TITLE
                }
                else -> {
                    RegionType.MAIN_TEXT
                }
            }

            classifiedRegions.add(
                OcrRegion(
                    pageId = pageId,
                    regionType = regionType,
                    box = box,
                    confidence = block.confidence,
                    rawText = block.text.trim(),
                    cleanedText = block.text.trim(),
                    engine = engineName,
                    pageType = pageType
                )
            )
        }

        // Apply Arabic Right-to-Left (RTL) reading order sorting
        val sorted = classifiedRegions.sortedWith { r1, r2 ->
            val priority1 = getRegionPriority(r1.regionType)
            val priority2 = getRegionPriority(r2.regionType)

            if (priority1 != priority2) {
                priority1.compareTo(priority2)
            } else {
                when (r1.regionType) {
                    RegionType.MAIN_TEXT -> {
                        if (isTwoColumns) {
                            // Column check: In Arabic RTL, RIGHT column (x >= midX) is read FIRST!
                            val isR1Right = r1.box.left >= (midX - bitmapWidth * 0.05f)
                            val isR2Right = r2.box.left >= (midX - bitmapWidth * 0.05f)

                            if (isR1Right && !isR2Right) {
                                -1 // r1 is in right column -> precedes left column
                            } else if (!isR1Right && isR2Right) {
                                1  // r2 is in right column -> precedes left column
                            } else {
                                // Same column: top-to-bottom
                                r1.box.top.compareTo(r2.box.top)
                            }
                        } else {
                            // Single column: top-to-bottom
                            r1.box.top.compareTo(r2.box.top)
                        }
                    }
                    RegionType.MARGINAL_NOTE -> {
                        // Right margin first (closer in RTL flow), then left margin
                        val isR1Right = r1.box.left >= midX
                        val isR2Right = r2.box.left >= midX
                        if (isR1Right && !isR2Right) -1
                        else if (!isR1Right && isR2Right) 1
                        else r1.box.top.compareTo(r2.box.top)
                    }
                    else -> {
                        r1.box.top.compareTo(r2.box.top)
                    }
                }
            }
        }

        // Assign incremental reading order (1, 2, 3, ...)
        return sorted.mapIndexed { index, region ->
            region.copy(readingOrder = index + 1)
        }
    }

    private fun getRegionPriority(type: RegionType): Int {
        return when (type) {
            RegionType.HEADER -> 1
            RegionType.TITLE -> 2
            RegionType.MAIN_TEXT -> 3
            RegionType.TABLE -> 4
            RegionType.LIST -> 5
            RegionType.CAPTION -> 6
            RegionType.MARGINAL_NOTE -> 7
            RegionType.FOOTNOTE -> 8
            RegionType.FOOTER -> 9
            RegionType.PAGE_NUMBER -> 10
            RegionType.IMAGE -> 11
            RegionType.UNKNOWN -> 12
        }
    }
}

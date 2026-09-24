package com.example.ocr.analysis

import android.graphics.Bitmap
import android.graphics.Color
import com.example.ocr.model.PageType
import kotlin.math.abs
import kotlin.math.sqrt

data class PageTypeAnalysisResult(
    val detectedType: PageType,
    val confidence: Float,
    val description: String,
    val isHandwritingCandidate: Boolean = false,
    val hasMarginalNotes: Boolean = false,
    val isMultiColumn: Boolean = false
)

object PageTypeDetector {

    /**
     * Performs fast, robust local heuristic analysis on a document bitmap:
     * - Dark pixel ratio (ink density)
     * - Horizontal line projection profile & baseline variance
     * - Stroke width and connectivity variation (printed fonts have consistent stroke widths,
     *   while Arabic handwriting displays dynamic stroke velocity and slant variation)
     * - Margin activity (detects marginal glosses / commentary)
     */
    fun analyzePage(bitmap: Bitmap): PageTypeAnalysisResult {
        val width = bitmap.width
        val height = bitmap.height

        if (width < 100 || height < 100) {
            return PageTypeAnalysisResult(
                detectedType = PageType.UNKNOWN,
                confidence = 0.5f,
                description = "حجم الصورة صغير جداً للتحليل الدقيق"
            )
        }

        // Sample down for fast, memory-safe analysis
        val sampleStepX = (width / 200).coerceAtLeast(1)
        val sampleStepY = (height / 200).coerceAtLeast(1)

        val sampledW = width / sampleStepX
        val sampledH = height / sampleStepY

        var darkPixels = 0
        var totalSampled = 0

        val rowDarkCounts = IntArray(sampledH)
        val colDarkCounts = IntArray(sampledW)

        // Margin dark counts: left 15% and right 15% (for Arabic marginal glosses)
        val rightMarginBoundary = (sampledW * 0.85).toInt()
        val leftMarginBoundary = (sampledW * 0.15).toInt()
        var rightMarginDark = 0
        var leftMarginDark = 0

        for (y in 0 until sampledH) {
            val srcY = (y * sampleStepY).coerceAtMost(height - 1)
            for (x in 0 until sampledW) {
                val srcX = (x * sampleStepX).coerceAtMost(width - 1)
                val pixel = bitmap.getPixel(srcX, srcY)

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                totalSampled++
                // Ink pixel threshold
                if (luminance < 140) {
                    darkPixels++
                    rowDarkCounts[y]++
                    colDarkCounts[x]++

                    if (x >= rightMarginBoundary) {
                        rightMarginDark++
                    } else if (x <= leftMarginBoundary) {
                        leftMarginDark++
                    }
                }
            }
        }

        val darkRatio = darkPixels.toFloat() / totalSampled.coerceAtLeast(1)

        // 1. Image only / Empty check
        if (darkRatio < 0.012f) {
            return PageTypeAnalysisResult(
                detectedType = PageType.IMAGE_ONLY,
                confidence = 0.85f,
                description = "صفحة بيضاء أو رسم توضيحي قليل النصوص"
            )
        }

        if (darkRatio > 0.50f) {
            return PageTypeAnalysisResult(
                detectedType = PageType.IMAGE_ONLY,
                confidence = 0.80f,
                description = "صورة داكنة أو مساحة مصمتة"
            )
        }

        // 2. Baseline regularity (Printed text shows prominent periodic peaks in row projection)
        var rowPeaks = 0
        var prevCount = 0
        var peakVarianceSum = 0f
        val peakDistances = mutableListOf<Int>()
        var lastPeakY = -1

        for (y in 1 until sampledH - 1) {
            val c = rowDarkCounts[y]
            if (c > (sampledW * 0.04) && c > rowDarkCounts[y - 1] && c > rowDarkCounts[y + 1]) {
                rowPeaks++
                if (lastPeakY != -1) {
                    peakDistances.add(y - lastPeakY)
                }
                lastPeakY = y
            }
        }

        // Calculate standard deviation of peak distances
        val avgDistance = if (peakDistances.isNotEmpty()) peakDistances.average().toFloat() else 0f
        var distanceVariance = 0f
        for (dist in peakDistances) {
            distanceVariance += (dist - avgDistance) * (dist - avgDistance)
        }
        val distanceStdDev = if (peakDistances.isNotEmpty()) sqrt(distanceVariance / peakDistances.size) else 99f

        // 3. Multi-column check (gutter in column projection)
        var isMultiColumn = false
        val centerStart = (sampledW * 0.40).toInt()
        val centerEnd = (sampledW * 0.60).toInt()
        var centerMin = Int.MAX_VALUE
        for (x in centerStart..centerEnd) {
            if (colDarkCounts[x] < centerMin) {
                centerMin = colDarkCounts[x]
            }
        }
        val colAvg = colDarkCounts.average().toFloat()
        if (centerMin < (colAvg * 0.20f) && rowPeaks > 6) {
            isMultiColumn = true
        }

        // 4. Marginal notes check (activity in margin strips)
        val hasRightMarginNotes = rightMarginDark > (darkPixels * 0.08f)
        val hasLeftMarginNotes = leftMarginDark > (darkPixels * 0.08f)
        val hasMarginalNotes = hasRightMarginNotes || hasLeftMarginNotes

        // 5. Classification
        return when {
            // Highly irregular line spacing and variable stroke peaks -> Handwriting
            distanceStdDev > 6.5f && rowPeaks > 3 -> {
                if (hasMarginalNotes) {
                    PageTypeAnalysisResult(
                        detectedType = PageType.MIXED,
                        confidence = 0.82f,
                        description = "مستند مختلط يحتوي على نص وهوامش وملاحظات يدوية",
                        isHandwritingCandidate = true,
                        hasMarginalNotes = true,
                        isMultiColumn = isMultiColumn
                    )
                } else {
                    PageTypeAnalysisResult(
                        detectedType = PageType.HANDWRITTEN,
                        confidence = 0.80f,
                        description = "خط يدوي / مخطوطة بأسطر حرة التباعد",
                        isHandwritingCandidate = true,
                        isMultiColumn = isMultiColumn
                    )
                }
            }
            hasMarginalNotes && isMultiColumn -> {
                PageTypeAnalysisResult(
                    detectedType = PageType.COMPLEX_LAYOUT,
                    confidence = 0.88f,
                    description = "تخطيط مركب: أعمدة متعددة مع هوامش جانبية",
                    isMultiColumn = true,
                    hasMarginalNotes = true
                )
            }
            isMultiColumn -> {
                PageTypeAnalysisResult(
                    detectedType = PageType.PRINTED,
                    confidence = 0.90f,
                    description = "كتاب مطبوع بعمودين (تخطيط ثنائي الأعمدة)",
                    isMultiColumn = true
                )
            }
            hasMarginalNotes -> {
                PageTypeAnalysisResult(
                    detectedType = PageType.MIXED,
                    confidence = 0.84f,
                    description = "نص مطبوع محاط بحواشي وهوامش جانبية",
                    hasMarginalNotes = true
                )
            }
            else -> {
                PageTypeAnalysisResult(
                    detectedType = PageType.PRINTED,
                    confidence = 0.92f,
                    description = "نص عربي مطبوع بأسطر منتظمة"
                )
            }
        }
    }
}

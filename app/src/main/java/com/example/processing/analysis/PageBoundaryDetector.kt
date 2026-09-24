package com.example.processing.analysis

import android.graphics.Bitmap
import android.graphics.PointF
import com.example.processing.model.DocumentQuad
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object PageBoundaryDetector {

    data class DetectionResult(
        val quad: DocumentQuad,
        val isAutoDetected: Boolean,
        val statusMessageAr: String
    )

    /**
     * Attempts to detect document boundaries in the given bitmap locally.
     * Operates on a downscaled representation to ensure fast execution and low memory consumption.
     */
    fun detectPageBoundary(bitmap: Bitmap): DetectionResult {
        try {
            val scaleWidth = 320
            val scaleHeight = (scaleWidth * (bitmap.height.toFloat() / bitmap.width)).toInt().coerceIn(200, 480)
            val smallBitmap = Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, true)

            val width = smallBitmap.width
            val height = smallBitmap.height
            val pixels = IntArray(width * height)
            smallBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            if (smallBitmap != bitmap) {
                smallBitmap.recycle()
            }

            // 1. Convert to Luminance array
            val lum = IntArray(width * height)
            var sumLum = 0L
            for (i in pixels.indices) {
                val c = pixels[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val l = (r * 299 + g * 587 + b * 114) / 1000
                lum[i] = l
                sumLum += l
            }
            val avgLum = (sumLum / lum.size).toInt()

            // 2. Compute Horizontal & Vertical Gradients (Sobel-like difference)
            val edgeMap = ByteArray(width * height)
            var edgeSum = 0L
            for (y in 1 until height - 1) {
                val rowOffset = y * width
                for (x in 1 until width - 1) {
                    val idx = rowOffset + x
                    val dx = abs(lum[idx + 1] - lum[idx - 1])
                    val dy = abs(lum[idx + width] - lum[idx - width])
                    val grad = (dx + dy).coerceAtMost(255)
                    edgeMap[idx] = grad.toByte()
                    edgeSum += grad
                }
            }
            val edgeThreshold = (edgeSum / (width * height) * 1.5).toInt().coerceIn(20, 80)

            // 3. Scan for outer boundaries where page begins
            // Top scan
            var topBoundary = (height * 0.05f).toInt()
            for (y in (height * 0.04f).toInt() until (height * 0.4f).toInt()) {
                var rowEdges = 0
                val rowOffset = y * width
                for (x in (width * 0.1f).toInt() until (width * 0.9f).toInt()) {
                    if ((edgeMap[rowOffset + x].toInt() and 0xFF) > edgeThreshold) {
                        rowEdges++
                    }
                }
                if (rowEdges > width * 0.15f) {
                    topBoundary = y
                    break
                }
            }

            // Bottom scan
            var bottomBoundary = (height * 0.95f).toInt()
            for (y in (height * 0.96f).toInt() downTo (height * 0.6f).toInt()) {
                var rowEdges = 0
                val rowOffset = y * width
                for (x in (width * 0.1f).toInt() until (width * 0.9f).toInt()) {
                    if ((edgeMap[rowOffset + x].toInt() and 0xFF) > edgeThreshold) {
                        rowEdges++
                    }
                }
                if (rowEdges > width * 0.15f) {
                    bottomBoundary = y
                    break
                }
            }

            // Left scan
            var leftBoundary = (width * 0.05f).toInt()
            for (x in (width * 0.04f).toInt() until (width * 0.4f).toInt()) {
                var colEdges = 0
                for (y in topBoundary until bottomBoundary) {
                    if ((edgeMap[y * width + x].toInt() and 0xFF) > edgeThreshold) {
                        colEdges++
                    }
                }
                if (colEdges > (bottomBoundary - topBoundary) * 0.15f) {
                    leftBoundary = x
                    break
                }
            }

            // Right scan
            var rightBoundary = (width * 0.95f).toInt()
            for (x in (width * 0.96f).toInt() downTo (width * 0.6f).toInt()) {
                var colEdges = 0
                for (y in topBoundary until bottomBoundary) {
                    if ((edgeMap[y * width + x].toInt() and 0xFF) > edgeThreshold) {
                        colEdges++
                    }
                }
                if (colEdges > (bottomBoundary - topBoundary) * 0.15f) {
                    rightBoundary = x
                    break
                }
            }

            // Normalize coordinates
            val normLeft = (leftBoundary.toFloat() / width).coerceIn(0.02f, 0.35f)
            val normRight = (rightBoundary.toFloat() / width).coerceIn(0.65f, 0.98f)
            val normTop = (topBoundary.toFloat() / height).coerceIn(0.02f, 0.35f)
            val normBottom = (bottomBoundary.toFloat() / height).coerceIn(0.65f, 0.98f)

            // Plausibility check: Page width and height must be at least 40% of image
            val detectedW = normRight - normLeft
            val detectedH = normBottom - normTop
            if (detectedW >= 0.40f && detectedH >= 0.40f) {
                // Introduce subtle corner refinement if available
                val quad = DocumentQuad(
                    topLeft = PointF(normLeft, normTop),
                    topRight = PointF(normRight, normTop),
                    bottomRight = PointF(normRight, normBottom),
                    bottomLeft = PointF(normLeft, normBottom)
                )

                if (quad.isConvex()) {
                    return DetectionResult(
                        quad = quad,
                        isAutoDetected = true,
                        statusMessageAr = "تم رصد حدود الصفحة تلقائياً - يمكنك تعديل الزوايا يدوياً"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Graceful fallback to default quad
        return DetectionResult(
            quad = DocumentQuad.defaultQuad(inset = 0.05f),
            isAutoDetected = false,
            statusMessageAr = "تعذر تحديد الحدود بدقة - تم وضع إطار افتراضي قابل للتعديل"
        )
    }
}

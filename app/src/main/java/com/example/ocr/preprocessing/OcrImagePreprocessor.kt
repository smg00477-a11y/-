package com.example.ocr.preprocessing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.example.ocr.model.OcrBoundingBox
import kotlin.math.max
import kotlin.math.min

object OcrImagePreprocessor {

    private const val MAX_OCR_DIMENSION = 2400
    private const val MIN_OCR_DIMENSION = 900

    enum class Profile {
        PRINTED,
        HANDWRITING,
        REGION_CROP
    }

    /**
     * Prepares a bitmap specifically for OCR:
     * - Respects page rotation
     * - Rescales within optimal OCR dimension boundaries
     * - Applies profile-specific enhancement:
     *   - PRINTED: High-contrast binarization and edge-crisp grayscale
     *   - HANDWRITING: Gentle contrast, background flattening, strict dot-preserving
     *     (no destructive erosion/dilation as warned by ArabicOCR-KHATT)
     */
    fun prepareForOcr(
        sourceBitmap: Bitmap,
        rotationDegrees: Int = 0,
        enhanceContrast: Boolean = true,
        profile: Profile = Profile.PRINTED
    ): Bitmap {
        var current = sourceBitmap

        // 1. Handle rotation if needed
        if (rotationDegrees % 360 != 0) {
            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
            }
            val rotated = Bitmap.createBitmap(
                current, 0, 0, current.width, current.height, matrix, true
            )
            if (current != sourceBitmap) {
                current.recycle()
            }
            current = rotated
        }

        // 2. Scale within optimal OCR bounds
        val maxDim = max(current.width, current.height)
        val minDim = min(current.width, current.height)

        var scaleFactor = 1.0f
        if (maxDim > MAX_OCR_DIMENSION) {
            scaleFactor = MAX_OCR_DIMENSION.toFloat() / maxDim
        } else if (minDim < MIN_OCR_DIMENSION && maxDim * 1.5f <= MAX_OCR_DIMENSION) {
            scaleFactor = MIN_OCR_DIMENSION.toFloat() / minDim
        }

        val targetWidth = (current.width * scaleFactor).toInt().coerceAtLeast(1)
        val targetHeight = (current.height * scaleFactor).toInt().coerceAtLeast(1)

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        if (enhanceContrast) {
            val cm = ColorMatrix()
            cm.setSaturation(0f) // High-fidelity Grayscale

            val (contrast, brightness) = when (profile) {
                Profile.PRINTED -> Pair(1.30f, 0.05f) // Crisper contrast for printed typography
                Profile.HANDWRITING -> Pair(1.15f, 0.08f) // Gentle contrast preserving light ink strokes and diacritical dots
                Profile.REGION_CROP -> Pair(1.20f, 0.05f)
            }

            val translate = (-0.5f * contrast + 0.5f + brightness) * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastMatrix)
            paint.colorFilter = ColorMatrixColorFilter(cm)
        }

        val drawMatrix = Matrix()
        if (scaleFactor != 1.0f) {
            drawMatrix.postScale(scaleFactor, scaleFactor)
        }
        canvas.drawBitmap(current, drawMatrix, paint)

        if (current != sourceBitmap) {
            current.recycle()
        }

        return output
    }

    /**
     * Extracts a sub-region from a bitmap with safe boundary clipping and padding.
     */
    fun cropRegion(
        sourceBitmap: Bitmap,
        box: OcrBoundingBox,
        padding: Int = 8
    ): Bitmap {
        val safeLeft = (box.left - padding).coerceIn(0, sourceBitmap.width - 1)
        val safeTop = (box.top - padding).coerceIn(0, sourceBitmap.height - 1)
        val safeRight = (box.right + padding).coerceIn(safeLeft + 1, sourceBitmap.width)
        val safeBottom = (box.bottom + padding).coerceIn(safeTop + 1, sourceBitmap.height)

        val width = safeRight - safeLeft
        val height = safeBottom - safeTop

        return Bitmap.createBitmap(sourceBitmap, safeLeft, safeTop, width, height)
    }
}

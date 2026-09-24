package com.example.processing.enhancement

import android.graphics.Bitmap
import com.example.processing.model.ProcessingMode
import com.example.processing.model.ProcessingOptions
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ImageEnhancer {

    /**
     * Applies the configured enhancement mode and parameter adjustments (brightness, contrast, noise reduction, sharpening).
     */
    fun enhance(sourceBitmap: Bitmap, options: ProcessingOptions): Bitmap {
        var result = when (options.mode) {
            ProcessingMode.ORIGINAL -> sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
            ProcessingMode.AUTO -> applyAutoEnhance(sourceBitmap)
            ProcessingMode.DOCUMENT -> applyDocumentFilter(sourceBitmap)
            ProcessingMode.ENHANCED -> applyHighClarityFilter(sourceBitmap)
            ProcessingMode.GRAYSCALE -> applyGrayscale(sourceBitmap)
            ProcessingMode.BLACK_AND_WHITE -> applyDocumentThreshold(sourceBitmap)
        }

        // Apply Denoising if requested
        if (options.noiseReduction) {
            val denoised = applyLightweightDenoise(result)
            if (denoised != result) {
                if (result != sourceBitmap) result.recycle()
                result = denoised
            }
        }

        // Apply Sharpening if requested
        if (options.sharpening) {
            val sharpened = applySubtleSharpen(result)
            if (sharpened != result) {
                if (result != sourceBitmap) result.recycle()
                result = sharpened
            }
        }

        // Apply Brightness & Contrast adjustment if modified
        if (options.brightness != 0f || options.contrast != 1.0f) {
            val adjusted = applyBrightnessContrast(result, options.brightness, options.contrast)
            if (adjusted != result) {
                if (result != sourceBitmap) result.recycle()
                result = adjusted
            }
        }

        return result
    }

    /**
     * Converts to high-fidelity Grayscale using standard ITU-R luminance weights.
     */
    fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val gray = (r * 299 + g * 587 + b * 114) / 1000
            pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Auto: Analyzes luminance histogram, stretches contrast to optimize dynamic range.
     */
    private fun applyAutoEnhance(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Find min and max luminance with 1% percentile clipping to avoid outliers
        val hist = IntArray(256)
        for (c in pixels) {
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val l = (r * 299 + g * 587 + b * 114) / 1000
            hist[l]++
        }

        val total = pixels.size
        val clipCount = (total * 0.015f).toInt()
        var lowCount = 0
        var minLum = 0
        for (i in 0..255) {
            lowCount += hist[i]
            if (lowCount >= clipCount) {
                minLum = i
                break
            }
        }

        var highCount = 0
        var maxLum = 255
        for (i in 255 downTo 0) {
            highCount += hist[i]
            if (highCount >= clipCount) {
                maxLum = i
                break
            }
        }

        val range = (maxLum - minLum).coerceAtLeast(20)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            val r = (((c shr 16) and 0xFF) - minLum) * 255 / range
            val g = (((c shr 8) and 0xFF) - minLum) * 255 / range
            val b = ((c and 0xFF) - minLum) * 255 / range

            val cr = r.coerceIn(0, 255)
            val cg = g.coerceIn(0, 255)
            val cb = b.coerceIn(0, 255)
            pixels[i] = (a shl 24) or (cr shl 16) or (cg shl 8) or cb
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Document: Normalizes page background lighting (shadow elimination) and maximizes text contrast.
     */
    private fun applyDocumentFilter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val blockSize = (max(width, height) / 24).coerceIn(16, 64)
        val gridW = (width + blockSize - 1) / blockSize
        val gridH = (height + blockSize - 1) / blockSize
        val bgMax = IntArray(gridW * gridH)

        // Block-based estimation of paper background brightness
        for (gy in 0 until gridH) {
            val startY = gy * blockSize
            val endY = min(startY + blockSize, height)
            for (gx in 0 until gridW) {
                val startX = gx * blockSize
                val endX = min(startX + blockSize, width)

                var maxVal = 0
                for (y in startY until endY) {
                    val row = y * width
                    for (x in startX until endX) {
                        val c = pixels[row + x]
                        val r = (c shr 16) and 0xFF
                        val g = (c shr 8) and 0xFF
                        val b = c and 0xFF
                        val l = (r * 299 + g * 587 + b * 114) / 1000
                        if (l > maxVal) maxVal = l
                    }
                }
                bgMax[gy * gridW + gx] = maxVal.coerceAtLeast(140)
            }
        }

        // Normalize each pixel by local background estimate
        for (y in 0 until height) {
            val gy = (y / blockSize).coerceIn(0, gridH - 1)
            val row = y * width
            for (x in 0 until width) {
                val gx = (x / blockSize).coerceIn(0, gridW - 1)
                val bg = bgMax[gy * gridW + gx].toFloat()

                val c = pixels[row + x]
                val a = (c ushr 24) and 0xFF
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                // Divide by local background and boost contrast curve
                val nr = ((r / bg) * 255f).coerceIn(0f, 255f)
                val ng = ((g / bg) * 255f).coerceIn(0f, 255f)
                val nb = ((b / bg) * 255f).coerceIn(0f, 255f)

                // High contrast curve: stretch paper to clean white, darken ink
                val cr = (if (nr > 200f) 255f else nr * 0.9f).toInt().coerceIn(0, 255)
                val cg = (if (ng > 200f) 255f else ng * 0.9f).toInt().coerceIn(0, 255)
                val cb = (if (nb > 200f) 255f else nb * 0.9f).toInt().coerceIn(0, 255)

                pixels[row + x] = (a shl 24) or (cr shl 16) or (cg shl 8) or cb
            }
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Enhanced: High-clarity mode with contrast curve and unsharp masking.
     */
    private fun applyHighClarityFilter(bitmap: Bitmap): Bitmap {
        val docBitmap = applyDocumentFilter(bitmap)
        val sharpened = applySubtleSharpen(docBitmap)
        if (sharpened != docBitmap) {
            docBitmap.recycle()
        }
        return sharpened
    }

    /**
     * Black & White: Local document-oriented thresholding that isolates ink from paper cleanly.
     */
    fun applyDocumentThreshold(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

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

        // Global threshold with gentle adaptive sensitivity
        val avg = (sumLum / lum.size).toInt()
        val threshold = (avg * 0.88f).toInt().coerceIn(70, 185)

        for (i in pixels.indices) {
            val binary = if (lum[i] < threshold) 0x00 else 0xFF
            pixels[i] = (0xFF shl 24) or (binary shl 16) or (binary shl 8) or binary
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Controlled Brightness & Contrast adjustment
     */
    fun applyBrightnessContrast(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            val nr = (((r - 128) * contrast) + 128 + brightness).toInt().coerceIn(0, 255)
            val ng = (((g - 128) * contrast) + 128 + brightness).toInt().coerceIn(0, 255)
            val nb = (((b - 128) * contrast) + 128 + brightness).toInt().coerceIn(0, 255)

            pixels[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Denoising: Selective local smoothing on flat areas that preserves fine text strokes.
     */
    private fun applyLightweightDenoise(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val src = IntArray(width * height)
        val dst = IntArray(width * height)
        bitmap.getPixels(src, 0, width, 0, 0, width, height)

        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val center = src[row + x]
                val cr = (center shr 16) and 0xFF
                val cg = (center shr 8) and 0xFF
                val cb = center and 0xFF

                // Check neighbors: if gradient is small, smooth it; if high contrast edge, keep intact!
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var maxDiff = 0

                for (dy in -1..1) {
                    val nRow = (y + dy) * width
                    for (dx in -1..1) {
                        val neighbor = src[nRow + (x + dx)]
                        val nr = (neighbor shr 16) and 0xFF
                        val ng = (neighbor shr 8) and 0xFF
                        val nb = neighbor and 0xFF
                        rSum += nr
                        gSum += ng
                        bSum += nb
                        val diff = abs(cr - nr) + abs(cg - ng) + abs(cb - nb)
                        if (diff > maxDiff) maxDiff = diff
                    }
                }

                // If edge is sharp (e.g. text edge), preserve original pixel
                if (maxDiff > 70) {
                    dst[row + x] = center
                } else {
                    val avgR = rSum / 9
                    val avgG = gSum / 9
                    val avgB = bSum / 9
                    dst[row + x] = ((center ushr 24) shl 24) or (avgR shl 16) or (avgG shl 8) or avgB
                }
            }
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(dst, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Restrained Sharpening: Unsharp mask kernel [0, -1, 0; -1, 5, -1; 0, -1, 0] without halos.
     */
    private fun applySubtleSharpen(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val src = IntArray(width * height)
        val dst = IntArray(width * height)
        bitmap.getPixels(src, 0, width, 0, 0, width, height)

        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val center = src[row + x]
                val top = src[(y - 1) * width + x]
                val bottom = src[(y + 1) * width + x]
                val left = src[row + x - 1]
                val right = src[row + x + 1]

                val a = (center ushr 24) and 0xFF

                fun sharpChan(cCenter: Int, cTop: Int, cBottom: Int, cLeft: Int, cRight: Int): Int {
                    val v = (5 * cCenter - cTop - cBottom - cLeft - cRight)
                    // Blend 60% sharpened with 40% original to avoid harsh artifacts
                    val blended = (v * 0.6f + cCenter * 0.4f).toInt()
                    return blended.coerceIn(0, 255)
                }

                val nr = sharpChan(
                    (center shr 16) and 0xFF,
                    (top shr 16) and 0xFF,
                    (bottom shr 16) and 0xFF,
                    (left shr 16) and 0xFF,
                    (right shr 16) and 0xFF
                )
                val ng = sharpChan(
                    (center shr 8) and 0xFF,
                    (top shr 8) and 0xFF,
                    (bottom shr 8) and 0xFF,
                    (left shr 8) and 0xFF,
                    (right shr 8) and 0xFF
                )
                val nb = sharpChan(
                    center and 0xFF,
                    top and 0xFF,
                    bottom and 0xFF,
                    left and 0xFF,
                    right and 0xFF
                )

                dst[row + x] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(dst, 0, width, 0, 0, width, height)
        return out
    }
}

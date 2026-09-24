package com.example.processing.transformation

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import com.example.processing.model.DocumentQuad
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object GeometryTransformer {

    /**
     * Applies rotation to a bitmap clockwise by degrees (0, 90, 180, 270).
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val normalizedDegrees = ((degrees % 360) + 360) % 360
        if (normalizedDegrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(normalizedDegrees.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Applies true homography / perspective transformation and crop from 4 corners to a rectangular bitmap.
     * Preserves document aspect ratio and uses bilinear interpolation for crisp text clarity.
     */
    fun perspectiveTransform(
        sourceBitmap: Bitmap,
        quad: DocumentQuad,
        applyDeskew: Boolean = false
    ): Bitmap {
        val clampedQuad = quad.clamped()
        val srcW = sourceBitmap.width
        val srcH = sourceBitmap.height

        // Source 4 points in absolute pixels
        val p0 = PointF(clampedQuad.topLeft.x * srcW, clampedQuad.topLeft.y * srcH)
        val p1 = PointF(clampedQuad.topRight.x * srcW, clampedQuad.topRight.y * srcH)
        val p2 = PointF(clampedQuad.bottomRight.x * srcW, clampedQuad.bottomRight.y * srcH)
        val p3 = PointF(clampedQuad.bottomLeft.x * srcW, clampedQuad.bottomLeft.y * srcH)

        // Calculate target dimensions based on edge lengths
        val topW = hypot(p1.x - p0.x, p1.y - p0.y)
        val bottomW = hypot(p2.x - p3.x, p2.y - p3.y)
        val leftH = hypot(p3.x - p0.x, p3.y - p0.y)
        val rightH = hypot(p2.x - p1.x, p2.y - p1.y)

        val targetWidth = max(topW, bottomW).roundToInt().coerceIn(120, 3600)
        val targetHeight = max(leftH, rightH).roundToInt().coerceIn(120, 3600)

        // Compute 3x3 homography matrix mapping [0,0, targetW,0, targetW,targetH, 0,targetH] -> [p0, p1, p2, p3]
        val matrix = Matrix()
        val rectPts = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )
        val quadPts = floatArrayOf(
            p0.x, p0.y,
            p1.x, p1.y,
            p2.x, p2.y,
            p3.x, p3.y
        )
        val success = matrix.setPolyToPoly(rectPts, 0, quadPts, 0, 4)

        if (!success) {
            // Fallback to bounding box crop if quad is degenerate
            val minX = min(min(p0.x, p1.x), min(p2.x, p3.x)).toInt().coerceIn(0, srcW - 1)
            val maxX = max(max(p0.x, p1.x), max(p2.x, p3.x)).toInt().coerceIn(minX + 1, srcW)
            val minY = min(min(p0.y, p1.y), min(p2.y, p3.y)).toInt().coerceIn(0, srcH - 1)
            val maxY = max(max(p0.y, p1.y), max(p2.y, p3.y)).toInt().coerceIn(minY + 1, srcH)
            return Bitmap.createBitmap(sourceBitmap, minX, minY, maxX - minX, maxY - minY)
        }

        val values = FloatArray(9)
        matrix.getValues(values)
        val m0 = values[Matrix.MSCALE_X]
        val m1 = values[Matrix.MSKEW_X]
        val m2 = values[Matrix.MTRANS_X]
        val m3 = values[Matrix.MSKEW_Y]
        val m4 = values[Matrix.MSCALE_Y]
        val m5 = values[Matrix.MTRANS_Y]
        val m6 = values[Matrix.MPERSP_0]
        val m7 = values[Matrix.MPERSP_1]
        val m8 = values[Matrix.MPERSP_2]

        // Fetch source pixels into IntArray for fast bilinear sampling
        val srcPixels = IntArray(srcW * srcH)
        sourceBitmap.getPixels(srcPixels, 0, srcW, 0, 0, srcW, srcH)

        val dstPixels = IntArray(targetWidth * targetHeight)

        for (y in 0 until targetHeight) {
            val yFloat = y.toFloat()
            val rowOffset = y * targetWidth
            for (x in 0 until targetWidth) {
                val xFloat = x.toFloat()
                val denom = m6 * xFloat + m7 * yFloat + m8
                if (denom != 0f) {
                    val u = (m0 * xFloat + m1 * yFloat + m2) / denom
                    val v = (m3 * xFloat + m4 * yFloat + m5) / denom

                    if (u >= 0f && u < srcW - 1 && v >= 0f && v < srcH - 1) {
                        val u0 = u.toInt()
                        val v0 = v.toInt()
                        val u1 = u0 + 1
                        val v1 = v0 + 1

                        val du = u - u0
                        val dv = v - v0
                        val w00 = (1f - du) * (1f - dv)
                        val w10 = du * (1f - dv)
                        val w01 = (1f - du) * dv
                        val w11 = du * dv

                        val p00 = srcPixels[v0 * srcW + u0]
                        val p10 = srcPixels[v0 * srcW + u1]
                        val p01 = srcPixels[v1 * srcW + u0]
                        val p11 = srcPixels[v1 * srcW + u1]

                        val a = (w00 * ((p00 ushr 24) and 0xFF) + w10 * ((p10 ushr 24) and 0xFF) + w01 * ((p01 ushr 24) and 0xFF) + w11 * ((p11 ushr 24) and 0xFF)).toInt()
                        val r = (w00 * ((p00 ushr 16) and 0xFF) + w10 * ((p10 ushr 16) and 0xFF) + w01 * ((p01 ushr 16) and 0xFF) + w11 * ((p11 ushr 16) and 0xFF)).toInt()
                        val g = (w00 * ((p00 ushr 8) and 0xFF) + w10 * ((p10 ushr 8) and 0xFF) + w01 * ((p01 ushr 8) and 0xFF) + w11 * ((p11 ushr 8) and 0xFF)).toInt()
                        val b = (w00 * (p00 and 0xFF) + w10 * (p10 and 0xFF) + w01 * (p01 and 0xFF) + w11 * (p11 and 0xFF)).toInt()

                        dstPixels[rowOffset + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    } else if (u >= 0f && u < srcW && v >= 0f && v < srcH) {
                        dstPixels[rowOffset + x] = srcPixels[v.toInt() * srcW + u.toInt()]
                    } else {
                        dstPixels[rowOffset + x] = -0x1 // 0xFFFFFFFF (White background)
                    }
                }
            }
        }

        var warpedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        warpedBitmap.setPixels(dstPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        // Deskew adjustment if requested and small deviation detected
        if (applyDeskew) {
            val angleRad = atan2(p1.y - p0.y, p1.x - p0.x)
            val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
            if (abs(angleDeg) in 0.5f..12.0f) {
                val deskewMatrix = Matrix().apply {
                    postRotate(-angleDeg, warpedBitmap.width / 2f, warpedBitmap.height / 2f)
                }
                val deskewed = Bitmap.createBitmap(warpedBitmap, 0, 0, warpedBitmap.width, warpedBitmap.height, deskewMatrix, true)
                if (deskewed != warpedBitmap) {
                    warpedBitmap.recycle()
                    warpedBitmap = deskewed
                }
            }
        }

        return warpedBitmap
    }
}

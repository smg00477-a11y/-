package com.example.processing.model

import android.graphics.PointF
import kotlin.math.hypot
import kotlin.math.max

data class DocumentQuad(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
) {
    /**
     * Clamps all coordinates to [0f, 1f]
     */
    fun clamped(): DocumentQuad {
        fun clamp(p: PointF) = PointF(p.x.coerceIn(0f, 1f), p.y.coerceIn(0f, 1f))
        return DocumentQuad(
            topLeft = clamp(topLeft),
            topRight = clamp(topRight),
            bottomRight = clamp(bottomRight),
            bottomLeft = clamp(bottomLeft)
        )
    }

    /**
     * Checks if the 4 points form a valid convex quadrilateral in clockwise order
     */
    fun isConvex(): Boolean {
        val pts = arrayOf(topLeft, topRight, bottomRight, bottomLeft)
        var sign = 0
        for (i in 0 until 4) {
            val p1 = pts[i]
            val p2 = pts[(i + 1) % 4]
            val p3 = pts[(i + 2) % 4]
            val crossProduct = (p2.x - p1.x) * (p3.y - p2.y) - (p2.y - p1.y) * (p3.x - p2.x)
            if (crossProduct != 0f) {
                val currentSign = if (crossProduct > 0f) 1 else -1
                if (sign == 0) {
                    sign = currentSign
                } else if (sign != currentSign) {
                    return false
                }
            }
        }
        return sign != 0
    }

    /**
     * Converts normalized [0..1] points to absolute pixel coordinates
     */
    fun toAbsolutePoints(width: Float, height: Float): FloatArray {
        return floatArrayOf(
            topLeft.x * width, topLeft.y * height,
            topRight.x * width, topRight.y * height,
            bottomRight.x * width, bottomRight.y * height,
            bottomLeft.x * width, bottomLeft.y * height
        )
    }

    /**
     * Estimated width and height in pixels when mapped to rectangular shape
     */
    fun calculateDimensions(imageWidth: Float, imageHeight: Float): Pair<Int, Int> {
        val p0x = topLeft.x * imageWidth
        val p0y = topLeft.y * imageHeight
        val p1x = topRight.x * imageWidth
        val p1y = topRight.y * imageHeight
        val p2x = bottomRight.x * imageWidth
        val p2y = bottomRight.y * imageHeight
        val p3x = bottomLeft.x * imageWidth
        val p3y = bottomLeft.y * imageHeight

        val topWidth = hypot(p1x - p0x, p1y - p0y)
        val bottomWidth = hypot(p2x - p3x, p2y - p3y)
        val leftHeight = hypot(p3x - p0x, p3y - p0y)
        val rightHeight = hypot(p2x - p1x, p2y - p1y)

        val targetWidth = max(topWidth, bottomWidth).toInt().coerceIn(100, 4500)
        val targetHeight = max(leftHeight, rightHeight).toInt().coerceIn(100, 4500)

        return Pair(targetWidth, targetHeight)
    }

    companion object {
        fun defaultQuad(inset: Float = 0.05f): DocumentQuad {
            return DocumentQuad(
                topLeft = PointF(inset, inset),
                topRight = PointF(1f - inset, inset),
                bottomRight = PointF(1f - inset, 1f - inset),
                bottomLeft = PointF(inset, 1f - inset)
            )
        }

        fun fullImage(): DocumentQuad {
            return DocumentQuad(
                topLeft = PointF(0f, 0f),
                topRight = PointF(1f, 0f),
                bottomRight = PointF(1f, 1f),
                bottomLeft = PointF(0f, 1f)
            )
        }
    }
}

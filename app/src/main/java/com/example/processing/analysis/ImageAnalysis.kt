package com.example.processing.analysis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import kotlin.math.max

object ImageAnalysis {

    data class ImageProperties(
        val width: Int,
        val height: Int,
        val exifRotationDegrees: Int
    )

    /**
     * Reads image properties (width, height, EXIF orientation) without allocating bitmap pixels in memory.
     */
    fun readImageProperties(filePath: String): ImageProperties {
        val file = File(filePath)
        if (!file.exists()) {
            return ImageProperties(0, 0, 0)
        }

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        val exifRotation = try {
            val exif = ExifInterface(filePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }

        // If rotated 90 or 270, swap dimensions
        val (w, h) = if (exifRotation == 90 || exifRotation == 270) {
            Pair(options.outHeight, options.outWidth)
        } else {
            Pair(options.outWidth, options.outHeight)
        }

        return ImageProperties(
            width = max(w, 1),
            height = max(h, 1),
            exifRotationDegrees = exifRotation
        )
    }

    /**
     * Decodes a bitmap oriented correctly according to its EXIF data.
     * Optionally limits maximum dimension to maxDimension to protect memory.
     */
    fun decodeOrientedBitmap(filePath: String, maxDimension: Int? = null): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null

        val props = readImageProperties(filePath)
        if (props.width <= 0 || props.height <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = if (maxDimension != null) {
                calculateInSampleSize(props.width, props.height, maxDimension, maxDimension)
            } else {
                1
            }
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = BitmapFactory.decodeFile(filePath, options) ?: return null

        // Apply EXIF rotation if needed so it is correctly upright
        return if (props.exifRotationDegrees != 0) {
            val matrix = Matrix().apply {
                postRotate(props.exifRotationDegrees.toFloat())
            }
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated != decoded) {
                decoded.recycle()
            }
            rotated
        } else {
            decoded
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

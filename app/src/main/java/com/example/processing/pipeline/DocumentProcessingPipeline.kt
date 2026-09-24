package com.example.processing.pipeline

import android.content.Context
import android.graphics.Bitmap
import com.example.data.storage.FileStorageManager
import com.example.processing.analysis.ImageAnalysis
import com.example.processing.analysis.PageBoundaryDetector
import com.example.processing.enhancement.ImageEnhancer
import com.example.processing.model.DocumentQuad
import com.example.processing.model.ProcessingOptions
import com.example.processing.model.ProcessingResult
import com.example.processing.model.ProcessingState
import com.example.processing.transformation.GeometryTransformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DocumentProcessingPipeline(
    private val context: Context,
    private val storageManager: FileStorageManager
) {

    /**
     * Executes the complete processing pipeline on an image file.
     * The original file is strictly protected and never modified.
     */
    suspend fun processImage(
        originalFilePath: String,
        options: ProcessingOptions,
        pageIdTag: String = "P${System.currentTimeMillis()}"
    ): ProcessingResult = withContext(Dispatchers.Default) {
        val originalFile = File(originalFilePath)
        if (!originalFile.exists() || originalFile.length() == 0L) {
            return@withContext ProcessingResult(
                success = false,
                state = ProcessingState.FAILED,
                errorMessage = "الملف الأصلي غير موجود أو فارغ"
            )
        }

        var sourceBitmap: Bitmap? = null
        var transformedBitmap: Bitmap? = null
        var enhancedBitmap: Bitmap? = null
        var thumbnailBitmap: Bitmap? = null

        try {
            // Step 1: Decode oriented original image
            sourceBitmap = ImageAnalysis.decodeOrientedBitmap(originalFilePath, maxDimension = 2400)
                ?: return@withContext ProcessingResult(
                    success = false,
                    state = ProcessingState.FAILED,
                    errorMessage = "تعذر قراءة ملف الصورة الأصلي"
                )

            // Step 2: Apply manual or EXIF rotation if specified
            var currentBitmap = if (options.rotationDegrees != 0) {
                GeometryTransformer.rotateBitmap(sourceBitmap, options.rotationDegrees)
            } else {
                sourceBitmap
            }

            // Step 3: Geometry Transformation (Perspective & Crop)
            val quadToApply = options.cropQuad
            transformedBitmap = if (quadToApply != null && !isFullImage(quadToApply)) {
                GeometryTransformer.perspectiveTransform(
                    sourceBitmap = currentBitmap,
                    quad = quadToApply,
                    applyDeskew = options.deskew
                )
            } else {
                if (currentBitmap != sourceBitmap) currentBitmap else currentBitmap.copy(Bitmap.Config.ARGB_8888, true)
            }

            // Step 4: Enhancement Filters (Auto, Document, Enhanced, Grayscale, B&W, Brightness/Contrast)
            enhancedBitmap = ImageEnhancer.enhance(transformedBitmap, options)

            // Step 5: Save Processed File into private storage (documents/processed/)
            val processedPath = storageManager.saveProcessedBitmap(enhancedBitmap, pageIdTag)
                ?: return@withContext ProcessingResult(
                    success = false,
                    state = ProcessingState.FAILED,
                    errorMessage = "تعذر كتابة ملف الصورة المعالجة على القرص"
                )

            // Step 6: Generate High-Performance Thumbnail
            val thumbWidth = 280
            val thumbHeight = ((thumbWidth.toFloat() / enhancedBitmap.width) * enhancedBitmap.height).toInt().coerceIn(160, 420)
            thumbnailBitmap = Bitmap.createScaledBitmap(enhancedBitmap, thumbWidth, thumbHeight, true)
            val thumbnailPath = storageManager.saveThumbnailBitmap(thumbnailBitmap, pageIdTag)

            ProcessingResult(
                success = true,
                processedFilePath = processedPath,
                thumbnailPath = thumbnailPath,
                state = ProcessingState.COMPLETED
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ProcessingResult(
                success = false,
                state = ProcessingState.FAILED,
                errorMessage = "خطأ أثناء معالجة الصورة: ${e.localizedMessage}"
            )
        } finally {
            // Clean up and recycle intermediate memory allocations safely
            try {
                if (sourceBitmap != null && !sourceBitmap.isRecycled) sourceBitmap.recycle()
                if (transformedBitmap != null && transformedBitmap != sourceBitmap && !transformedBitmap.isRecycled) transformedBitmap.recycle()
                if (enhancedBitmap != null && enhancedBitmap != transformedBitmap && !enhancedBitmap.isRecycled) enhancedBitmap.recycle()
                if (thumbnailBitmap != null && !thumbnailBitmap.isRecycled) thumbnailBitmap.recycle()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Fast local preview generator for live interaction before final save.
     */
    suspend fun generatePreview(
        originalFilePath: String,
        options: ProcessingOptions,
        maxPreviewDimension: Int = 800
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val previewSource = ImageAnalysis.decodeOrientedBitmap(originalFilePath, maxDimension = maxPreviewDimension)
                ?: return@withContext null

            val rotated = if (options.rotationDegrees != 0) {
                val r = GeometryTransformer.rotateBitmap(previewSource, options.rotationDegrees)
                if (r != previewSource) previewSource.recycle()
                r
            } else {
                previewSource
            }

            val quadToApply = options.cropQuad
            val transformed = if (quadToApply != null && !isFullImage(quadToApply)) {
                val t = GeometryTransformer.perspectiveTransform(
                    sourceBitmap = rotated,
                    quad = quadToApply,
                    applyDeskew = options.deskew
                )
                if (t != rotated) rotated.recycle()
                t
            } else {
                rotated
            }

            val enhanced = ImageEnhancer.enhance(transformed, options)
            if (enhanced != transformed) {
                transformed.recycle()
            }
            enhanced
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isFullImage(quad: DocumentQuad): Boolean {
        val eps = 0.015f
        return quad.topLeft.x <= eps && quad.topLeft.y <= eps &&
                quad.topRight.x >= 1f - eps && quad.topRight.y <= eps &&
                quad.bottomRight.x >= 1f - eps && quad.bottomRight.y >= 1f - eps &&
                quad.bottomLeft.x <= eps && quad.bottomLeft.y >= 1f - eps
    }
}

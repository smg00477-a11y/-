package com.example.data.storage.archive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.database.AppDatabase
import com.example.data.database.entity.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class CompressionStrategy(
    val titleAr: String,
    val descriptionAr: String,
    val quality: Int,
    val maxDimension: Int
) {
    ORIGINAL("الجودة الأصلية", "الحفاظ على الصورة بجودتها وأبعادها الأصلية (100%)", 100, 4096),
    BALANCED("أرشفة متوازنة", "توازن مثالي بين توفير مساحة التخزين وجودة النص والوضوح (85%)", 85, 1920),
    SPACE_SAVING("توفير الأقصى للمساحة", "تقليل الحجم إلى أقصى حد مع بقاء النص قابلاً للقرائية والـ OCR (70%)", 70, 1280)
}

data class StorageStats(
    val documentCount: Int,
    val pageCount: Int,
    val originalFilesBytes: Long,
    val processedFilesBytes: Long,
    val thumbnailsBytes: Long,
    val tempCacheBytes: Long,
    val totalLibraryBytes: Long,
    val freeSpaceBytes: Long
)

data class CompressionResult(
    val processedPagesCount: Int,
    val initialBytes: Long,
    val finalBytes: Long,
    val savedBytes: Long,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class StorageOptimizationManager(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val documents = database.documentDao().getAllDocumentsDirect()
        val pages = database.pageDao().getAllPagesDirect()

        var origBytes = 0L
        var procBytes = 0L
        var thumbBytes = 0L

        pages.forEach { page ->
            File(page.localFilePath).takeIf { it.exists() }?.let { origBytes += it.length() }
            page.processedFilePath?.let { path ->
                File(path).takeIf { it.exists() }?.let { procBytes += it.length() }
            }
            page.thumbnailPath?.let { path ->
                File(path).takeIf { it.exists() }?.let { thumbBytes += it.length() }
            }
        }

        val cacheDir = context.cacheDir
        val tempDir = File(context.cacheDir, "scan_temp")
        var tempBytes = getDirectorySize(cacheDir) + getDirectorySize(tempDir)

        val filesDir = context.filesDir
        val freeBytes = filesDir.freeSpace

        StorageStats(
            documentCount = documents.size,
            pageCount = pages.size,
            originalFilesBytes = origBytes,
            processedFilesBytes = procBytes,
            thumbnailsBytes = thumbBytes,
            tempCacheBytes = tempBytes,
            totalLibraryBytes = origBytes + procBytes + thumbBytes,
            freeSpaceBytes = freeBytes
        )
    }

    suspend fun compressDocument(
        documentId: Long,
        strategy: CompressionStrategy,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): CompressionResult = withContext(Dispatchers.IO) {
        val pages = database.pageDao().getPagesForDocumentDirect(documentId)
        if (pages.isEmpty()) {
            return@withContext CompressionResult(0, 0, 0, 0, true)
        }

        var totalInitialBytes = 0L
        var totalFinalBytes = 0L
        var successCount = 0

        pages.forEachIndexed { index, page ->
            onProgress((index + 1).toFloat() / pages.size, "ضغط الصفحة ${index + 1} من ${pages.size}")
            
            val targetPath = page.processedFilePath ?: page.localFilePath
            val sourceFile = File(targetPath)
            if (sourceFile.exists()) {
                val initialSize = sourceFile.length()
                totalInitialBytes += initialSize

                val compressedFile = compressImageFile(sourceFile, strategy)
                if (compressedFile != null && verifyImageIntegrity(compressedFile)) {
                    // Safe replacement
                    val newSha256 = ChecksumUtility.calculateSha256(compressedFile)
                    val finalSize = compressedFile.length()
                    
                    val destFile = File(context.filesDir, "documents/processed/COMPRESSED_${page.id}_${UUID.randomUUID().toString().take(6)}.jpg")
                    destFile.parentFile?.mkdirs()
                    compressedFile.copyTo(destFile, overwrite = true)
                    compressedFile.delete()

                    val updatedPage = page.copy(
                        processedFilePath = destFile.absolutePath,
                        processingState = "COMPLETED",
                        processingMode = strategy.name,
                        updatedAt = System.currentTimeMillis()
                    )
                    database.pageDao().updatePage(updatedPage)

                    totalFinalBytes += finalSize
                    successCount++
                } else {
                    totalFinalBytes += initialSize
                }
            }
        }

        database.documentDao().touchDocument(documentId, System.currentTimeMillis())

        CompressionResult(
            processedPagesCount = successCount,
            initialBytes = totalInitialBytes,
            finalBytes = totalFinalBytes,
            savedBytes = (totalInitialBytes - totalFinalBytes).coerceAtLeast(0),
            isSuccess = true
        )
    }

    suspend fun compressAllDocuments(
        strategy: CompressionStrategy,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): CompressionResult = withContext(Dispatchers.IO) {
        val documents = database.documentDao().getAllDocumentsDirect()
        var grandInitial = 0L
        var grandFinal = 0L
        var grandProcessed = 0

        documents.forEachIndexed { idx, doc ->
            onProgress((idx + 1).toFloat() / documents.size, "ضغط الكتاب: ${doc.title}")
            val res = compressDocument(doc.id, strategy)
            grandInitial += res.initialBytes
            grandFinal += res.finalBytes
            grandProcessed += res.processedPagesCount
        }

        CompressionResult(
            processedPagesCount = grandProcessed,
            initialBytes = grandInitial,
            finalBytes = grandFinal,
            savedBytes = (grandInitial - grandFinal).coerceAtLeast(0),
            isSuccess = true
        )
    }

    private fun compressImageFile(inputFile: File, strategy: CompressionStrategy): File? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(inputFile.absolutePath, options)

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) return null

            var sampleSize = 1
            var maxDim = Math.max(originalWidth, originalHeight)
            while (maxDim / sampleSize > strategy.maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath, decodeOptions) ?: return null

            val tempOutFile = File.createTempFile("compress_temp_", ".jpg", context.cacheDir)
            FileOutputStream(tempOutFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, strategy.quality, out)
            }
            bitmap.recycle()

            if (tempOutFile.exists() && tempOutFile.length() > 0) tempOutFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun verifyImageIntegrity(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth > 0 && options.outHeight > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearTempAndCache(): Long = withContext(Dispatchers.IO) {
        var bytesFreed = 0L
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    bytesFreed += getDirectorySize(file)
                    file.deleteRecursively()
                } else {
                    bytesFreed += file.length()
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bytesFreed
    }

    suspend fun cleanOrphanedFiles(): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        try {
            val allPages = database.pageDao().getAllPagesDirect()
            val validPaths = HashSet<String>()
            allPages.forEach { page ->
                validPaths.add(page.localFilePath)
                page.processedFilePath?.let { validPaths.add(it) }
                page.thumbnailPath?.let { validPaths.add(it) }
            }

            val documentsDir = File(context.filesDir, "documents")
            if (documentsDir.exists()) {
                documentsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    if (!validPaths.contains(file.absolutePath) && !file.name.contains("database")) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        deletedCount
    }

    private fun getDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { child ->
            size += if (child.isDirectory) getDirectorySize(child) else child.length()
        }
        return size
    }
}

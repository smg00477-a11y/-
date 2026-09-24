package com.example.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FileStorageManager(private val context: Context) {

    private val documentsDir: File by lazy {
        File(context.filesDir, "documents").apply {
            if (!exists()) mkdirs()
        }
    }

    private val processedDir: File by lazy {
        File(documentsDir, "processed").apply {
            if (!exists()) mkdirs()
        }
    }

    private val thumbnailsDir: File by lazy {
        File(documentsDir, "thumbnails").apply {
            if (!exists()) mkdirs()
        }
    }

    private val tempDir: File by lazy {
        File(context.cacheDir, "scan_temp").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun saveProcessedBitmap(bitmap: android.graphics.Bitmap, pageIdTag: String): String? = withContext(Dispatchers.IO) {
        try {
            val filename = "PROCESSED_${pageIdTag}_${UUID.randomUUID().toString().take(6)}.jpg"
            val targetFile = File(processedDir, filename)
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (targetFile.exists() && targetFile.length() > 0) {
                targetFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveThumbnailBitmap(bitmap: android.graphics.Bitmap, pageIdTag: String): String? = withContext(Dispatchers.IO) {
        try {
            val filename = "THUMB_${pageIdTag}_${UUID.randomUUID().toString().take(6)}.jpg"
            val targetFile = File(thumbnailsDir, filename)
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (targetFile.exists() && targetFile.length() > 0) {
                targetFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun cleanTempFiles() = withContext(Dispatchers.IO) {
        try {
            tempDir.listFiles()?.forEach { file ->
                try {
                    file.delete()
                } catch (ignored: Exception) {}
            }
        } catch (ignored: Exception) {}
    }

    suspend fun createNewCaptureFile(): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "CAPTURE_${timestamp}_${UUID.randomUUID().toString().take(8)}.jpg"
        File(tempDir, filename)
    }

    suspend fun copyUriToManagedStorage(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val extension = getExtensionFromUri(sourceUri)
            val filename = "PAGE_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"
            val targetFile = File(documentsDir, filename)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) {
                targetFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun commitTempFileToStorage(tempFile: File): String? = withContext(Dispatchers.IO) {
        try {
            if (!tempFile.exists()) return@withContext null
            val filename = "PAGE_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val targetFile = File(documentsDir, filename)
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteFiles(filePaths: List<String>) = withContext(Dispatchers.IO) {
        filePaths.forEach { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getShareableUri(filePath: String): Uri? {
        val file = File(filePath)
        if (!file.exists()) return null
        return try {
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getExtensionFromUri(uri: Uri): String {
        val type = context.contentResolver.getType(uri)
        return when {
            type?.contains("png", ignoreCase = true) == true -> "png"
            type?.contains("webp", ignoreCase = true) == true -> "webp"
            type?.contains("pdf", ignoreCase = true) == true -> "pdf"
            else -> "jpg"
        }
    }
}

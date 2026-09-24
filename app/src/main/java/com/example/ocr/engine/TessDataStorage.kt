package com.example.ocr.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object TessDataStorage {
    private const val TAG = "TessDataStorage"
    private const val TESSDATA_FOLDER = "tessdata"
    val REQUIRED_FILES = listOf("ara.traineddata", "eng.traineddata", "osd.traineddata")

    fun getTessDataDirectory(context: Context): File {
        val dir = File(context.filesDir, TESSDATA_FOLDER)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Tesseract API requires the path to the parent directory containing the "tessdata" folder.
     */
    fun getTessParentDirectoryPath(context: Context): String {
        return context.filesDir.absolutePath
    }

    /**
     * Synchronously or asynchronously unpacks local bundled assets to internal app storage.
     * ZERO NETWORK CALLS. Completely offline.
     */
    suspend fun ensureTessDataReady(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDir = getTessDataDirectory(context)

            for (fileName in REQUIRED_FILES) {
                val targetFile = File(targetDir, fileName)

                // If file does not exist or is empty, copy from bundled APK assets
                if (!targetFile.exists() || targetFile.length() == 0L) {
                    Log.d(TAG, "Unpacking local asset: $fileName to ${targetFile.absolutePath}")
                    try {
                        context.assets.open("$TESSDATA_FOLDER/$fileName").use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error copying asset $fileName: ${e.message}", e)
                        // If osd.traineddata fails but ara.traineddata succeeds, we can still proceed
                        if (fileName == "ara.traineddata") {
                            return@withContext false
                        }
                    }
                }
            }

            // Verify Arabic model is present and has non-zero size
            val arabicFile = File(targetDir, "ara.traineddata")
            val isReady = arabicFile.exists() && arabicFile.length() > 1000L
            Log.d(TAG, "TessData validation: ara.traineddata ready = $isReady (${arabicFile.length()} bytes)")
            isReady
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize local TessData", e)
            false
        }
    }

    /**
     * Checks if local traineddata files already exist without attempting to copy.
     */
    fun isArabicModelAvailable(context: Context): Boolean {
        val arabicFile = File(getTessDataDirectory(context), "ara.traineddata")
        return arabicFile.exists() && arabicFile.length() > 1000L
    }
}

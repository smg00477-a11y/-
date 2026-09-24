package com.example.ocr.engine

import android.graphics.Bitmap
import com.example.ocr.model.OcrBoundingBox
import com.example.ocr.model.OcrEngineType
import com.example.ocr.model.OcrLanguage
import com.example.ocr.model.OcrPageResult
import com.example.ocr.model.PageType

interface OcrEngine {
    val engineName: String
    val engineType: OcrEngineType

    /**
     * Performs optical character recognition locally on the provided bitmap or sub-region.
     * ZERO NETWORK CALLS. Completely offline.
     */
    suspend fun recognize(
        bitmap: Bitmap,
        pageId: Long = 0,
        language: OcrLanguage = OcrLanguage.ARABIC_AND_ENGLISH,
        cropBox: OcrBoundingBox? = null,
        overridePageType: PageType? = null
    ): OcrPageResult

    /**
     * Checks if the required local model files are ready.
     */
    suspend fun isModelReady(): Boolean
}

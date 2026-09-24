package com.example.ocr.engine

import android.content.Context
import android.graphics.Bitmap
import com.example.ocr.model.OcrBoundingBox
import com.example.ocr.model.OcrEngineType
import com.example.ocr.model.OcrLanguage
import com.example.ocr.model.OcrPageResult
import com.example.ocr.model.PageType

/**
 * Backwards-compatible facade for TesseractArabicEngine.
 */
class TesseractOcrEngine(
    private val context: Context
) : OcrEngine {

    private val delegate = TesseractArabicEngine(context)

    override val engineName: String get() = delegate.engineName
    override val engineType: OcrEngineType get() = delegate.engineType

    override suspend fun isModelReady(): Boolean = delegate.isModelReady()

    override suspend fun recognize(
        bitmap: Bitmap,
        pageId: Long,
        language: OcrLanguage,
        cropBox: OcrBoundingBox?,
        overridePageType: PageType?
    ): OcrPageResult {
        return delegate.recognize(bitmap, pageId, language, cropBox, overridePageType)
    }
}

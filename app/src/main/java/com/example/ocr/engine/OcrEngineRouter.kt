package com.example.ocr.engine

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.ocr.analysis.PageTypeDetector
import com.example.ocr.model.OcrBoundingBox
import com.example.ocr.model.OcrEngineType
import com.example.ocr.model.OcrLanguage
import com.example.ocr.model.OcrPageResult
import com.example.ocr.model.PageType
import com.example.ocr.model.RegionType

/**
 * Central offline OCR routing coordinator.
 * Directs pages and user-selected regions to the appropriate specialized engine:
 * - Printed Arabic -> TesseractArabicEngine
 * - Handwriting / Manuscripts -> ArabicHandwritingEngine
 * - Neural Printed -> ArabicPrintedModelEngine
 * - Auto Mode -> Local PageTypeDetector + Smart Dispatch
 */
class OcrEngineRouter(
    private val context: Context
) {
    companion object {
        private const val TAG = "OcrEngineRouter"
    }

    val printedEngine = TesseractArabicEngine(context)
    val handwritingEngine = ArabicHandwritingEngine(context)
    val neuralPrintedEngine = ArabicPrintedModelEngine(context)

    suspend fun isModelReady(): Boolean {
        return printedEngine.isModelReady()
    }

    /**
     * Executes OCR on a full page bitmap according to requested engine and page type.
     */
    suspend fun processPage(
        bitmap: Bitmap,
        pageId: Long,
        language: OcrLanguage = OcrLanguage.ARABIC_AND_ENGLISH,
        preferredEngine: OcrEngineType = OcrEngineType.AUTO,
        manualPageType: PageType? = null
    ): OcrPageResult {
        // 1. Analyze page type locally if not explicitly provided
        val pageAnalysis = PageTypeDetector.analyzePage(bitmap)
        val effectivePageType = manualPageType ?: pageAnalysis.detectedType

        Log.d(TAG, "Processing page $pageId: preferredEngine=$preferredEngine, detectedPageType=${pageAnalysis.detectedType}, effective=$effectivePageType")

        // 2. Route to specialized engine
        return when (preferredEngine) {
            OcrEngineType.ARABIC_HANDWRITING -> {
                handwritingEngine.recognize(
                    bitmap = bitmap,
                    pageId = pageId,
                    language = language,
                    overridePageType = effectivePageType
                )
            }
            OcrEngineType.ARABIC_PRINT_NEURAL -> {
                neuralPrintedEngine.recognize(
                    bitmap = bitmap,
                    pageId = pageId,
                    language = language,
                    overridePageType = effectivePageType
                )
            }
            OcrEngineType.TESSERACT_PRINTED -> {
                printedEngine.recognize(
                    bitmap = bitmap,
                    pageId = pageId,
                    language = language,
                    overridePageType = effectivePageType
                )
            }
            OcrEngineType.LAYOUT_ANALYSIS, OcrEngineType.AUTO -> {
                when (effectivePageType) {
                    PageType.HANDWRITTEN -> {
                        handwritingEngine.recognize(
                            bitmap = bitmap,
                            pageId = pageId,
                            language = language,
                            overridePageType = PageType.HANDWRITTEN
                        )
                    }
                    PageType.MIXED -> {
                        // For mixed pages, perform printed recognition with layout analysis
                        // and classify handwriting-flagged regions
                        val result = printedEngine.recognize(
                            bitmap = bitmap,
                            pageId = pageId,
                            language = language,
                            overridePageType = PageType.MIXED
                        )
                        result.copy(pageType = PageType.MIXED)
                    }
                    else -> {
                        printedEngine.recognize(
                            bitmap = bitmap,
                            pageId = pageId,
                            language = language,
                            overridePageType = effectivePageType
                        )
                    }
                }
            }
        }
    }

    /**
     * Executes OCR specifically on a user-selected bounding box region.
     */
    suspend fun processRegion(
        bitmap: Bitmap,
        pageId: Long,
        cropBox: OcrBoundingBox,
        targetType: PageType = PageType.PRINTED,
        language: OcrLanguage = OcrLanguage.ARABIC_AND_ENGLISH
    ): OcrPageResult {
        Log.d(TAG, "Processing manual region on page $pageId: box=$cropBox, targetType=$targetType")
        val engine = when (targetType) {
            PageType.HANDWRITTEN -> handwritingEngine
            else -> printedEngine
        }
        return engine.recognize(
            bitmap = bitmap,
            pageId = pageId,
            language = language,
            cropBox = cropBox,
            overridePageType = targetType
        )
    }
}

package com.example.ocr.engine

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.ocr.analysis.LayoutAnalysisEngine
import com.example.ocr.analysis.PageTypeDetector
import com.example.ocr.model.OcrBlock
import com.example.ocr.model.OcrBoundingBox
import com.example.ocr.model.OcrEngineType
import com.example.ocr.model.OcrLanguage
import com.example.ocr.model.OcrLine
import com.example.ocr.model.OcrPageResult
import com.example.ocr.model.OcrStatus
import com.example.ocr.model.OcrWord
import com.example.ocr.model.PageType
import com.example.ocr.postprocessing.ArabicOcrPostProcessor
import com.example.ocr.preprocessing.OcrImagePreprocessor
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Baseline Tesseract OCR engine for printed Arabic and mixed documents.
 * Operates 100% offline with bundled ara.traineddata and eng.traineddata.
 */
class TesseractArabicEngine(
    private val context: Context
) : OcrEngine {

    companion object {
        private const val TAG = "TesseractArabicEngine"
    }

    override val engineName: String = "Tesseract (مطبوع)"
    override val engineType: OcrEngineType = OcrEngineType.TESSERACT_PRINTED

    override suspend fun isModelReady(): Boolean = withContext(Dispatchers.IO) {
        TessDataStorage.ensureTessDataReady(context)
    }

    override suspend fun recognize(
        bitmap: Bitmap,
        pageId: Long,
        language: OcrLanguage,
        cropBox: OcrBoundingBox?,
        overridePageType: PageType?
    ): OcrPageResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. Verify local model files
        val ready = TessDataStorage.ensureTessDataReady(context)
        if (!ready) {
            return@withContext OcrPageResult(
                pageId = pageId,
                status = OcrStatus.FAILED,
                errorMessage = "ملفات النماذج المحلية (tessdata) غير متوفرة داخل التطبيق."
            )
        }

        // 2. Crop region if requested
        val workingBitmap = if (cropBox != null) {
            try {
                OcrImagePreprocessor.cropRegion(bitmap, cropBox)
            } catch (e: Exception) {
                Log.w(TAG, "Crop failed, falling back to full bitmap: ${e.message}")
                bitmap
            }
        } else {
            bitmap
        }

        // 3. Local Page Type Detection if not overridden
        val detectedPageType = overridePageType ?: run {
            if (cropBox != null) {
                PageType.PRINTED
            } else {
                PageTypeDetector.analyzePage(workingBitmap).detectedType
            }
        }

        // 4. Preprocessing using PRINTED profile
        val preprocessed = try {
            OcrImagePreprocessor.prepareForOcr(
                sourceBitmap = workingBitmap,
                rotationDegrees = 0,
                enhanceContrast = true,
                profile = OcrImagePreprocessor.Profile.PRINTED
            )
        } catch (e: Exception) {
            Log.w(TAG, "Preprocessing failed: ${e.message}")
            workingBitmap
        }

        var tessApi: TessBaseAPI? = null
        try {
            tessApi = TessBaseAPI()
            val dataPath = TessDataStorage.getTessParentDirectoryPath(context)

            val initSuccess = tessApi.init(dataPath, language.code, TessBaseAPI.OEM_LSTM_ONLY)
            if (!initSuccess) {
                val fallbackSuccess = tessApi.init(dataPath, "ara", TessBaseAPI.OEM_LSTM_ONLY)
                if (!fallbackSuccess) {
                    return@withContext OcrPageResult(
                        pageId = pageId,
                        status = OcrStatus.FAILED,
                        errorMessage = "تعذر تهيئة محرك Tesseract باللغة المطلوبة: ${language.code}"
                    )
                }
            }

            tessApi.pageSegMode = if (cropBox != null) {
                TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
            } else {
                TessBaseAPI.PageSegMode.PSM_AUTO
            }

            tessApi.setImage(preprocessed)

            val rawText = tessApi.utF8Text ?: ""
            val meanConfidence = tessApi.meanConfidence().toFloat()

            val blocks = extractStructuredHierarchy(tessApi)

            // Segment into functional layout regions with Arabic RTL ordering
            val regions = if (cropBox == null && blocks.isNotEmpty()) {
                LayoutAnalysisEngine.segmentAndOrderRegions(
                    pageId = pageId,
                    bitmapWidth = workingBitmap.width,
                    bitmapHeight = workingBitmap.height,
                    blocks = blocks,
                    pageType = detectedPageType,
                    engineName = engineName
                )
            } else {
                emptyList()
            }

            // Post-processing
            val cleanedText = ArabicOcrPostProcessor.clean(rawText)
            val duration = System.currentTimeMillis() - startTime

            OcrPageResult(
                pageId = pageId,
                status = OcrStatus.COMPLETED,
                rawText = rawText,
                cleanedText = cleanedText,
                blocks = blocks,
                regions = regions,
                pageType = detectedPageType,
                engineUsed = engineName,
                confidence = meanConfidence,
                language = language.code,
                processingTimeMs = duration
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error executing Tesseract OCR: ${e.message}", e)
            OcrPageResult(
                pageId = pageId,
                status = OcrStatus.FAILED,
                errorMessage = "خطأ في التعرف الضوئي: ${e.localizedMessage ?: e.message}"
            )
        } finally {
            try {
                tessApi?.recycle()
            } catch (ignored: Exception) {}

            if (preprocessed != workingBitmap && preprocessed != bitmap) {
                try {
                    preprocessed.recycle()
                } catch (ignored: Exception) {}
            }
            if (workingBitmap != bitmap) {
                try {
                    workingBitmap.recycle()
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun extractStructuredHierarchy(tessApi: TessBaseAPI): List<OcrBlock> {
        val blocks = mutableListOf<OcrBlock>()
        val iterator = tessApi.resultIterator ?: return blocks

        try {
            iterator.begin()
            val currentLineWords = mutableListOf<OcrWord>()
            val currentBlockLines = mutableListOf<OcrLine>()

            do {
                // Word
                val wordText = iterator.getUTF8Text(PageIteratorLevel.RIL_WORD)
                if (!wordText.isNullOrBlank()) {
                    val boxArray = iterator.getBoundingBox(PageIteratorLevel.RIL_WORD)
                    val box = boxArray?.let { OcrBoundingBox(it[0], it[1], it[2], it[3]) }
                    val wordConf = iterator.confidence(PageIteratorLevel.RIL_WORD)
                    currentLineWords.add(OcrWord(wordText, wordConf, box))
                }

                // Line
                if (iterator.isAtFinalElement(PageIteratorLevel.RIL_TEXTLINE, PageIteratorLevel.RIL_WORD)) {
                    val lineText = iterator.getUTF8Text(PageIteratorLevel.RIL_TEXTLINE) ?: ""
                    val lineBoxArray = iterator.getBoundingBox(PageIteratorLevel.RIL_TEXTLINE)
                    val lineBox = lineBoxArray?.let { OcrBoundingBox(it[0], it[1], it[2], it[3]) }
                    val lineConf = iterator.confidence(PageIteratorLevel.RIL_TEXTLINE)

                    currentBlockLines.add(
                        OcrLine(
                            text = lineText,
                            words = currentLineWords.toList(),
                            confidence = lineConf,
                            box = lineBox
                        )
                    )
                    currentLineWords.clear()
                }

                // Block
                if (iterator.isAtFinalElement(PageIteratorLevel.RIL_BLOCK, PageIteratorLevel.RIL_WORD)) {
                    val blockText = iterator.getUTF8Text(PageIteratorLevel.RIL_BLOCK) ?: ""
                    val blockBoxArray = iterator.getBoundingBox(PageIteratorLevel.RIL_BLOCK)
                    val blockBox = blockBoxArray?.let { OcrBoundingBox(it[0], it[1], it[2], it[3]) }
                    val blockConf = iterator.confidence(PageIteratorLevel.RIL_BLOCK)

                    blocks.add(
                        OcrBlock(
                            text = blockText,
                            lines = currentBlockLines.toList(),
                            confidence = blockConf,
                            box = blockBox
                        )
                    )
                    currentBlockLines.clear()
                }
            } while (iterator.next(PageIteratorLevel.RIL_WORD))
        } catch (e: Exception) {
            Log.w(TAG, "Structured OCR iteration encountered an issue: ${e.message}")
        } finally {
            try {
                iterator.delete()
            } catch (ignored: Exception) {}
        }

        return blocks
    }
}

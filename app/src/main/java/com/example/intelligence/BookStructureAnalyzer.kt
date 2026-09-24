package com.example.intelligence

import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.entity.AuthorEntity
import com.example.data.database.entity.BookMetadataEntity
import com.example.data.database.entity.PrintedPageNumberEntity
import com.example.ocr.cleaner.ArabicTextCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookStructureAnalyzer(
    private val database: AppDatabase
) {
    companion object {
        private const val TAG = "BookStructureAnalyzer"
    }

    /**
     * Executes full local book structure analysis:
     * 1. Printed Page Numbers detection
     * 2. Front matter Metadata & Author detection
     * 3. Chapter detection
     * 4. Table of Contents detection
     * 5. Table detection
     *
     * Respects and preserves any data with USER_CONFIRMED or USER_EDITED status!
     */
    suspend fun analyzeBookStructure(
        documentId: Long,
        onProgress: (step: String, progress: Float) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val docWithPages = database.documentDao().getDocumentWithPagesByIdDirect(documentId)
                ?: return@withContext false

            val document = docWithPages.document
            val pages = docWithPages.sortedPages
            if (pages.isEmpty()) return@withContext false

            onProgress("كشف أرقام الصفحات المطبوعة...", 0.15f)
            // 1. Detect Printed Page Numbers
            val detectedPageNumbers = mutableListOf<PrintedPageNumberEntity>()
            for (page in pages) {
                val numEntity = PrintedPageDetector.detectPrintedPage(documentId, page)
                if (numEntity != null) {
                    detectedPageNumbers.add(numEntity)
                    database.printedPageNumberDao().insertOrUpdate(numEntity)
                }
            }

            onProgress("استخراج بيانات الكتاب وهوية المؤلف...", 0.35f)
            // 2. Metadata & Author
            val existingMeta = database.bookMetadataDao().getMetadataByDocumentIdDirect(documentId)
            val isUserConfirmedMeta = existingMeta?.detectionStatus in listOf("USER_CONFIRMED", "USER_EDITED")

            if (!isUserConfirmedMeta) {
                val detected = MetadataDetector.detectFromFrontMatter(pages)

                var authorId: Long? = existingMeta?.authorId
                val authorName = detected.author ?: existingMeta?.author ?: document.authorName

                // Author matching & profile creation
                if (authorName.isNotBlank() && authorName != "غير محدد") {
                    val normalizedAuthor = AuthorMatcher.normalizeAuthorName(authorName)
                    val existingAuthor = database.authorDao().findAuthorByNormalizedName(normalizedAuthor)

                    authorId = if (existingAuthor != null) {
                        existingAuthor.id
                    } else {
                        val newAuthor = AuthorEntity(
                            name = authorName,
                            normalizedName = normalizedAuthor
                        )
                        database.authorDao().insertAuthor(newAuthor)
                    }
                }

                val title = detected.title ?: existingMeta?.title ?: document.title
                val category = detected.category ?: existingMeta?.category ?: document.category

                val newMeta = BookMetadataEntity(
                    id = existingMeta?.id ?: 0L,
                    documentId = documentId,
                    title = title,
                    subtitle = detected.subtitle ?: existingMeta?.subtitle ?: "",
                    author = authorName,
                    authorId = authorId,
                    coAuthors = existingMeta?.coAuthors ?: "",
                    translator = detected.translator ?: existingMeta?.translator ?: "",
                    editor = detected.editor ?: existingMeta?.editor ?: "",
                    publisher = detected.publisher ?: existingMeta?.publisher ?: "",
                    publicationYear = detected.publicationYear ?: existingMeta?.publicationYear ?: "",
                    edition = detected.edition ?: existingMeta?.edition ?: "",
                    isbn = detected.isbn ?: existingMeta?.isbn ?: "",
                    language = existingMeta?.language ?: "ar",
                    category = category,
                    tags = detected.tags.joinToString(", "),
                    description = existingMeta?.description ?: "",
                    notes = existingMeta?.notes ?: "",
                    originalPageCount = pages.size,
                    detectionStatus = "DETECTED",
                    updatedAt = System.currentTimeMillis()
                )
                database.bookMetadataDao().insertOrUpdate(newMeta)

                // Update document record for quick library queries
                database.documentDao().updateDocument(
                    document.copy(
                        title = title,
                        authorId = authorId,
                        authorName = authorName,
                        category = category,
                        originalPageCount = pages.size,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            onProgress("تحليل فصول الكتاب وأقسامه...", 0.60f)
            // 3. Chapters
            val existingChapters = database.chapterDao().getChaptersByDocumentIdDirect(documentId)
            val hasConfirmedChapters = existingChapters.any { it.manuallyConfirmed }

            if (!hasConfirmedChapters) {
                val detectedChapters = ChapterDetector.detectChapters(documentId, pages)
                if (detectedChapters.isNotEmpty()) {
                    database.chapterDao().deleteUnconfirmedChaptersByDocumentId(documentId)
                    database.chapterDao().insertChapters(detectedChapters)
                }
            }

            onProgress("بناء الفهرس وقائمة المحتويات...", 0.80f)
            // 4. Table of Contents
            val existingToc = database.tocDao().getTocByDocumentIdDirect(documentId)
            val hasConfirmedToc = existingToc.any { it.detectionStatus in listOf("USER_CONFIRMED", "USER_EDITED") }

            if (!hasConfirmedToc) {
                val detectedToc = TocDetector.detectTocEntries(documentId, pages, detectedPageNumbers)
                if (detectedToc.isNotEmpty()) {
                    database.tocDao().deleteTocByDocumentId(documentId)
                    database.tocDao().insertTocEntries(detectedToc)
                }
            }

            onProgress("رصد الجداول واستخراج بنيتها...", 0.95f)
            // 5. Tables
            database.tableDao().deleteTablesByDocumentId(documentId)
            for (page in pages) {
                val regions = database.ocrRegionDao().getRegionsForPage(page.id)
                val detectedTables = TableDetector.detectTables(documentId, page, regions)

                for (dTable in detectedTables) {
                    val tableId = database.tableDao().insertTable(dTable.table)
                    val updatedCells = dTable.cells.map { it.copy(tableId = tableId) }
                    database.tableDao().insertCells(updatedCells)
                }
            }

            onProgress("اكتمل التحليل الهيكلي للكتاب بنجاح!", 1.0f)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during book structure analysis: ${e.message}", e)
            false
        }
    }
}

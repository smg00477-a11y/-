package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.data.database.entity.BookCategoryCrossRef
import com.example.data.database.entity.BookSeriesCrossRef
import com.example.data.database.entity.CategoryEntity
import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.OcrRegionEntity
import com.example.data.database.entity.PageEntity
import com.example.data.database.entity.SeriesEntity
import com.example.data.model.DocumentSearchResult
import com.example.data.model.DocumentType
import com.example.data.model.LibraryFilterOptions
import com.example.data.model.LibrarySortField
import com.example.data.model.LibrarySortOption
import com.example.data.model.LibraryStats
import com.example.data.model.SimilarBookResult
import com.example.data.model.SortDirection
import com.example.data.storage.FileStorageManager
import com.example.ocr.engine.OcrEngine
import com.example.ocr.engine.OcrEngineRouter
import com.example.ocr.export.PdfExportManager
import com.example.ocr.model.OcrBoundingBox
import com.example.ocr.model.OcrEngineType
import com.example.ocr.model.OcrLanguage
import com.example.ocr.model.OcrPageResult
import com.example.ocr.model.OcrStatus
import com.example.ocr.model.PageType
import com.example.ocr.model.RegionType
import com.example.ocr.postprocessing.ArabicOcrPostProcessor
import com.example.processing.analysis.ImageAnalysis
import com.example.processing.model.ProcessingOptions
import com.example.processing.model.ProcessingResult
import com.example.processing.model.ProcessingState
import com.example.processing.pipeline.DocumentProcessingPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class DocumentRepository(
    private val database: AppDatabase,
    private val storageManager: FileStorageManager,
    val processingPipeline: DocumentProcessingPipeline,
    val ocrEngine: OcrEngine,
    val ocrRouter: OcrEngineRouter? = null
) {
    private val documentDao = database.documentDao()
    private val pageDao = database.pageDao()
    private val ocrRegionDao = database.ocrRegionDao()
    val authorDao = database.authorDao()
    val bookMetadataDao = database.bookMetadataDao()
    val chapterDao = database.chapterDao()
    val tocDao = database.tocDao()
    val tableDao = database.tableDao()
    val printedPageNumberDao = database.printedPageNumberDao()
    val bookmarkDao = database.bookmarkDao()
    val bookNoteDao = database.bookNoteDao()
    val categoryDao = database.categoryDao()
    val seriesDao = database.seriesDao()

    val bookStructureAnalyzer = com.example.intelligence.BookStructureAnalyzer(database)

    val allDocuments: Flow<List<DocumentWithPages>> =
        documentDao.getAllDocumentsWithPages()

    val favoriteDocuments: Flow<List<DocumentWithPages>> =
        documentDao.getFavoriteDocumentsWithPages()

    val allAuthors: Flow<List<com.example.data.database.entity.AuthorEntity>> =
        authorDao.getAllAuthors()

    val allCategories: Flow<List<com.example.data.database.entity.CategoryEntity>> =
        categoryDao.getAllCategories()

    val allSeries: Flow<List<com.example.data.database.entity.SeriesEntity>> =
        seriesDao.getAllSeries()

    fun getAuthorById(authorId: Long): Flow<com.example.data.database.entity.AuthorEntity?> =
        authorDao.getAuthorById(authorId)

    fun getMetadataForDocument(documentId: Long): Flow<com.example.data.database.entity.BookMetadataEntity?> =
        bookMetadataDao.getMetadataByDocumentId(documentId)

    fun getChaptersForDocument(documentId: Long): Flow<List<com.example.data.database.entity.ChapterEntity>> =
        chapterDao.getChaptersByDocumentId(documentId)

    fun getTocForDocument(documentId: Long): Flow<List<com.example.data.database.entity.TocEntryEntity>> =
        tocDao.getTocByDocumentId(documentId)

    fun getTablesForDocument(documentId: Long): Flow<List<com.example.data.database.entity.TableEntity>> =
        tableDao.getTablesByDocumentId(documentId)

    fun getCellsForTable(tableId: Long): Flow<List<com.example.data.database.entity.TableCellEntity>> =
        tableDao.getCellsByTableId(tableId)

    fun getBookmarksForDocument(documentId: Long): Flow<List<com.example.data.database.entity.BookmarkEntity>> =
        bookmarkDao.getBookmarksByDocumentId(documentId)

    fun isBookmarked(documentId: Long, pageIndex: Int): Flow<Boolean> =
        bookmarkDao.isBookmarked(documentId, pageIndex)

    fun getNotesForDocument(documentId: Long): Flow<List<com.example.data.database.entity.BookNoteEntity>> =
        bookNoteDao.getNotesByDocumentId(documentId)

    fun getNotesForPage(documentId: Long, pageIndex: Int): Flow<List<com.example.data.database.entity.BookNoteEntity>> =
        bookNoteDao.getNotesForPage(documentId, pageIndex)

    fun getDocumentById(id: Long): Flow<DocumentWithPages?> =
        documentDao.getDocumentWithPagesById(id)

    suspend fun getDocumentByIdDirect(id: Long): DocumentWithPages? =
        documentDao.getDocumentWithPagesByIdDirect(id)

    fun searchDocuments(query: String): Flow<List<DocumentWithPages>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            allDocuments
        } else {
            documentDao.searchDocumentsByTitle(trimmed)
        }
    }

    /**
     * Local Full-Text Search across Document Titles AND OCR text with Arabic normalization.
     * ZERO NETWORK CALLS. Completely offline.
     */
    fun searchDocumentsWithOcr(query: String): Flow<List<DocumentSearchResult>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return allDocuments.map { list ->
                list.map { DocumentSearchResult(it, matchedInTitle = false) }
            }
        }

        val normalizedQuery = ArabicOcrPostProcessor.normalizeForSearch(trimmed)

        return allDocuments.map { docs ->
            val results = mutableListOf<DocumentSearchResult>()

            for (docWithPages in docs) {
                val titleNormalized = ArabicOcrPostProcessor.normalizeForSearch(docWithPages.document.title)
                val matchesTitle = titleNormalized.contains(normalizedQuery)

                val authorNormalized = ArabicOcrPostProcessor.normalizeForSearch(docWithPages.document.authorName)
                val matchesAuthor = authorNormalized.contains(normalizedQuery)

                val categoryNormalized = ArabicOcrPostProcessor.normalizeForSearch(docWithPages.document.category)
                val matchesCategory = categoryNormalized.contains(normalizedQuery)

                var matchedPageNumber: Int? = null
                var matchedPageId: Long? = null
                var snippet: String? = null

                for (page in docWithPages.pages.sortedBy { it.pageIndex }) {
                    val textToSearch = page.ocrUserEditedText?.takeIf { it.isNotBlank() }
                        ?: page.ocrCleanedText.takeIf { it.isNotBlank() }
                        ?: page.ocrRawText

                    if (textToSearch.isNotBlank()) {
                        val pageNormalized = ArabicOcrPostProcessor.normalizeForSearch(textToSearch)
                        val matchIndex = pageNormalized.indexOf(normalizedQuery)

                        if (matchIndex != -1) {
                            matchedPageNumber = page.pageIndex + 1
                            matchedPageId = page.id
                            val snippetStart = (matchIndex - 30).coerceAtLeast(0)
                            val snippetEnd = (matchIndex + normalizedQuery.length + 30).coerceAtMost(textToSearch.length)
                            val rawSnippet = textToSearch.substring(snippetStart, snippetEnd).trim()
                            snippet = "...$rawSnippet..."
                            break
                        }
                    }
                }

                if (matchesTitle || matchesAuthor || matchesCategory || matchedPageNumber != null) {
                    results.add(
                        DocumentSearchResult(
                            documentWithPages = docWithPages,
                            matchedInTitle = matchesTitle,
                            matchedPageNumber = matchedPageNumber,
                            matchedPageId = matchedPageId,
                            matchedSnippet = snippet,
                            matchedInAuthor = matchesAuthor,
                            matchedInCategory = matchesCategory
                        )
                    )
                }
            }

            results
        }
    }

    /**
     * Executes real local OCR on an individual page using the specialized offline engine.
     * Supports routing: AUTO, PRINTED, HANDWRITING, NEURAL, or LAYOUT ANALYSIS.
     */
    suspend fun runOcrOnPage(
        pageId: Long,
        language: OcrLanguage = OcrLanguage.ARABIC_AND_ENGLISH,
        preferredEngine: OcrEngineType = OcrEngineType.AUTO,
        manualPageType: PageType? = null
    ): OcrPageResult = withContext(Dispatchers.IO) {
        val page = pageDao.getPageById(pageId)
            ?: return@withContext OcrPageResult(
                pageId = pageId,
                status = OcrStatus.FAILED,
                errorMessage = "الصفحة غير موجودة في قاعدة البيانات"
            )

        pageDao.updatePageOcrStatus(pageId, OcrStatus.PROCESSING.name)

        val imagePath = page.processedFilePath ?: page.localFilePath
        val bitmap = ImageAnalysis.decodeOrientedBitmap(imagePath, maxDimension = 2400)

        if (bitmap == null) {
            val error = "تعذر تحميل صورة الصفحة لمعالجتها ضوئياً"
            pageDao.updatePageOcrResult(
                pageId = pageId,
                status = OcrStatus.FAILED.name,
                rawText = page.ocrRawText,
                cleanedText = page.ocrCleanedText,
                userEditedText = page.ocrUserEditedText,
                language = language.code,
                confidence = 0f,
                errorMessage = error,
                pageType = page.pageType,
                ocrEngineUsed = page.ocrEngineUsed
            )
            return@withContext OcrPageResult(pageId = pageId, status = OcrStatus.FAILED, errorMessage = error)
        }

        try {
            val result = if (ocrRouter != null) {
                ocrRouter.processPage(
                    bitmap = bitmap,
                    pageId = pageId,
                    language = language,
                    preferredEngine = preferredEngine,
                    manualPageType = manualPageType
                )
            } else {
                ocrEngine.recognize(
                    bitmap = bitmap,
                    pageId = pageId,
                    language = language,
                    overridePageType = manualPageType
                )
            }

            if (result.status == OcrStatus.COMPLETED) {
                pageDao.updatePageOcrResult(
                    pageId = pageId,
                    status = OcrStatus.COMPLETED.name,
                    rawText = result.rawText,
                    cleanedText = result.cleanedText,
                    userEditedText = page.ocrUserEditedText,
                    language = result.language,
                    confidence = result.confidence,
                    errorMessage = null,
                    pageType = result.pageType.name,
                    ocrEngineUsed = result.engineUsed
                )

                // Save layout analysis regions
                if (result.regions.isNotEmpty()) {
                    ocrRegionDao.deleteRegionsForPage(pageId)
                    val entities = result.regions.map { OcrRegionEntity.fromOcrRegion(it, pageId) }
                    ocrRegionDao.insertRegions(entities)
                }
            } else {
                pageDao.updatePageOcrResult(
                    pageId = pageId,
                    status = OcrStatus.FAILED.name,
                    rawText = page.ocrRawText,
                    cleanedText = page.ocrCleanedText,
                    userEditedText = page.ocrUserEditedText,
                    language = language.code,
                    confidence = 0f,
                    errorMessage = result.errorMessage,
                    pageType = result.pageType.name,
                    ocrEngineUsed = result.engineUsed
                )
            }
            result
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Executes real local OCR on all pages of a document sequentially.
     */
    suspend fun runOcrOnDocument(
        documentId: Long,
        language: OcrLanguage = OcrLanguage.ARABIC_AND_ENGLISH,
        preferredEngine: OcrEngineType = OcrEngineType.AUTO,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): List<OcrPageResult> = withContext(Dispatchers.IO) {
        val pages = pageDao.getPagesListForDocument(documentId)
        val results = mutableListOf<OcrPageResult>()
        val total = pages.size

        pages.forEachIndexed { index, page ->
            val result = runOcrOnPage(page.id, language, preferredEngine)
            results.add(result)
            onProgress?.invoke(index + 1, total)
        }

        results
    }

    /**
     * Retries OCR only on pages that previously failed or were not processed.
     */
    suspend fun retryFailedOcrPages(
        documentId: Long,
        language: OcrLanguage = OcrLanguage.ARABIC_AND_ENGLISH,
        preferredEngine: OcrEngineType = OcrEngineType.AUTO,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): List<OcrPageResult> = withContext(Dispatchers.IO) {
        val pages = pageDao.getPagesListForDocument(documentId)
        val failedPages = pages.filter { it.ocrStatus == OcrStatus.FAILED.name || it.ocrStatus == OcrStatus.NOT_PROCESSED.name }
        val results = mutableListOf<OcrPageResult>()
        val total = failedPages.size

        failedPages.forEachIndexed { index, page ->
            val result = runOcrOnPage(page.id, language, preferredEngine)
            results.add(result)
            onProgress?.invoke(index + 1, total)
        }

        results
    }

    /**
     * Updates user-edited text for an OCR page without overwriting raw or cleaned OCR.
     */
    suspend fun updateUserEditedText(pageId: Long, userEditedText: String) = withContext(Dispatchers.IO) {
        pageDao.updatePageUserEditedText(
            pageId = pageId,
            userEditedText = userEditedText,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun updatePageType(pageId: Long, pageType: PageType) = withContext(Dispatchers.IO) {
        pageDao.updatePageType(pageId, pageType.name)
    }

    // --- Region Management ---

    fun getRegionsForPageFlow(pageId: Long): Flow<List<OcrRegionEntity>> =
        ocrRegionDao.getRegionsForPageFlow(pageId)

    suspend fun getRegionsForPage(pageId: Long): List<OcrRegionEntity> = withContext(Dispatchers.IO) {
        ocrRegionDao.getRegionsForPage(pageId)
    }

    suspend fun updateRegionUserText(regionId: Long, userEditedText: String) = withContext(Dispatchers.IO) {
        ocrRegionDao.updateRegionUserText(regionId, userEditedText)
    }

    suspend fun updateRegionType(regionId: Long, regionType: RegionType) = withContext(Dispatchers.IO) {
        ocrRegionDao.updateRegionType(regionId, regionType.name)
    }

    suspend fun deleteRegion(regionId: Long) = withContext(Dispatchers.IO) {
        ocrRegionDao.deleteRegionById(regionId)
    }

    /**
     * Recognizes a manually selected bounding box region on a page.
     * The original image is untouched. Adds the recognized region to the page hierarchy.
     */
    suspend fun recognizeManualRegion(
        pageId: Long,
        cropBox: OcrBoundingBox,
        targetType: PageType = PageType.PRINTED,
        regionType: RegionType = RegionType.MAIN_TEXT,
        language: OcrLanguage = OcrLanguage.ARABIC_AND_ENGLISH
    ): OcrRegionEntity? = withContext(Dispatchers.IO) {
        val page = pageDao.getPageById(pageId) ?: return@withContext null
        val imagePath = page.processedFilePath ?: page.localFilePath
        val bitmap = ImageAnalysis.decodeOrientedBitmap(imagePath, maxDimension = 2400) ?: return@withContext null

        try {
            val result = if (ocrRouter != null) {
                ocrRouter.processRegion(
                    bitmap = bitmap,
                    pageId = pageId,
                    cropBox = cropBox,
                    targetType = targetType,
                    language = language
                )
            } else {
                ocrEngine.recognize(
                    bitmap = bitmap,
                    pageId = pageId,
                    language = language,
                    cropBox = cropBox,
                    overridePageType = targetType
                )
            }

            val existingRegions = ocrRegionDao.getRegionsForPage(pageId)
            val readingOrder = existingRegions.size + 1

            val entity = OcrRegionEntity(
                pageId = pageId,
                regionType = regionType.name,
                left = cropBox.left,
                top = cropBox.top,
                right = cropBox.right,
                bottom = cropBox.bottom,
                readingOrder = readingOrder,
                confidence = result.confidence,
                rawText = result.rawText,
                cleanedText = result.cleanedText,
                userEditedText = null,
                engine = result.engineUsed,
                pageType = targetType.name
            )

            val newId = ocrRegionDao.insertRegion(entity)
            entity.copy(id = newId)
        } finally {
            bitmap.recycle()
        }
    }

    // --- PDF Export with Rights Page ---

    suspend fun exportPdf(
        context: Context,
        documentId: Long,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): File? = withContext(Dispatchers.IO) {
        val docWithPages = documentDao.getDocumentWithPagesByIdDirect(documentId) ?: return@withContext null
        PdfExportManager.generateDocumentPdf(context, docWithPages, onProgress)
    }

    suspend fun getPageById(pageId: Long): PageEntity? = withContext(Dispatchers.IO) {
        pageDao.getPageById(pageId)
    }

    fun getPageByIdFlow(pageId: Long): Flow<PageEntity?> =
        pageDao.getPageByIdFlow(pageId)

    suspend fun createDocument(
        title: String,
        type: DocumentType,
        pageFilePaths: List<String>,
        processedPageMap: Map<Int, Pair<String, String>> = emptyMap()
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val docEntity = DocumentEntity(
            title = title.trim().ifEmpty { "مستند بدون عنوان" },
            type = type,
            createdAt = now,
            updatedAt = now,
            isFavorite = false
        )
        val documentId = documentDao.insertDocument(docEntity)

        val pages = pageFilePaths.mapIndexed { index, path ->
            val processedPair = processedPageMap[index]
            var thumb = processedPair?.second

            if (thumb == null) {
                try {
                    val decoded = ImageAnalysis.decodeOrientedBitmap(path, maxDimension = 320)
                    if (decoded != null) {
                        thumb = storageManager.saveThumbnailBitmap(decoded, "D${documentId}_P$index")
                        decoded.recycle()
                    }
                } catch (ignored: Exception) {}
            }

            PageEntity(
                documentId = documentId,
                pageIndex = index,
                localFilePath = path,
                processedFilePath = processedPair?.first,
                thumbnailPath = thumb,
                processingState = if (processedPair != null) ProcessingState.COMPLETED.name else ProcessingState.NOT_PROCESSED.name,
                processingMode = if (processedPair != null) "PROCESSED" else "ORIGINAL",
                createdAt = now,
                updatedAt = now
            )
        }
        if (pages.isNotEmpty()) {
            pageDao.insertPages(pages)
        }
        documentId
    }

    suspend fun processExistingPage(
        pageId: Long,
        options: ProcessingOptions
    ): ProcessingResult = withContext(Dispatchers.IO) {
        val page = pageDao.getPageById(pageId)
            ?: return@withContext ProcessingResult(
                success = false,
                state = ProcessingState.FAILED,
                errorMessage = "الصفحة غير موجودة في قاعدة البيانات"
            )

        pageDao.updatePageProcessingState(pageId, ProcessingState.PROCESSING.name)

        val result = processingPipeline.processImage(
            originalFilePath = page.localFilePath,
            options = options,
            pageIdTag = "P${page.id}"
        )

        if (result.success && result.processedFilePath != null) {
            pageDao.updatePageProcessing(
                pageId = pageId,
                processedPath = result.processedFilePath,
                thumbnailPath = result.thumbnailPath,
                state = ProcessingState.COMPLETED.name,
                mode = options.mode.name,
                rotation = options.rotationDegrees,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            pageDao.updatePageProcessingState(pageId, ProcessingState.FAILED.name)
        }

        result
    }

    suspend fun resetPageToOriginal(pageId: Long) = withContext(Dispatchers.IO) {
        val page = pageDao.getPageById(pageId) ?: return@withContext
        page.processedFilePath?.let { storageManager.deleteFile(it) }
        pageDao.updatePageProcessing(
            pageId = pageId,
            processedPath = null,
            thumbnailPath = null,
            state = ProcessingState.NOT_PROCESSED.name,
            mode = "ORIGINAL",
            rotation = 0,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun toggleFavorite(documentId: Long, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        documentDao.updateFavorite(
            id = documentId,
            isFavorite = !currentStatus,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteDocument(documentId: Long) = withContext(Dispatchers.IO) {
        val pages = pageDao.getPagesListForDocument(documentId)
        val filesToDelete = mutableListOf<String>()
        for (page in pages) {
            filesToDelete.add(page.localFilePath)
            page.processedFilePath?.let { filesToDelete.add(it) }
            page.thumbnailPath?.let { filesToDelete.add(it) }
        }

        documentDao.deleteDocumentById(documentId)
        storageManager.deleteFiles(filesToDelete)
    }

    suspend fun createNewCaptureFile(): File = storageManager.createNewCaptureFile()

    suspend fun commitTempCaptureFile(tempFile: File): String? =
        storageManager.commitTempFileToStorage(tempFile)

    suspend fun importUriToLocal(uri: Uri): String? =
        storageManager.copyUriToManagedStorage(uri)

    fun getShareableUri(filePath: String): Uri? =
        storageManager.getShareableUri(filePath)

    // --- Raqeem V0.5 Book Intelligence Helpers ---

    suspend fun analyzeBookStructure(
        documentId: Long,
        onProgress: (step: String, progress: Float) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        bookStructureAnalyzer.analyzeBookStructure(documentId, onProgress)
    }

    suspend fun saveBookMetadata(metadata: com.example.data.database.entity.BookMetadataEntity) = withContext(Dispatchers.IO) {
        bookMetadataDao.insertOrUpdate(metadata)

        // Also update DocumentEntity title, author, category, originalPageCount
        val docWithPages = documentDao.getDocumentWithPagesByIdDirect(metadata.documentId)
        if (docWithPages != null) {
            documentDao.updateDocument(
                docWithPages.document.copy(
                    title = metadata.title,
                    authorId = metadata.authorId,
                    authorName = metadata.author,
                    category = metadata.category,
                    originalPageCount = metadata.originalPageCount,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun saveAuthor(author: com.example.data.database.entity.AuthorEntity): Long = withContext(Dispatchers.IO) {
        if (author.id == 0L) {
            authorDao.insertAuthor(author)
        } else {
            authorDao.updateAuthor(author)
            author.id
        }
    }

    suspend fun deleteAuthor(authorId: Long) = withContext(Dispatchers.IO) {
        authorDao.deleteAuthorById(authorId)
    }

    suspend fun saveChapter(chapter: com.example.data.database.entity.ChapterEntity): Long = withContext(Dispatchers.IO) {
        if (chapter.id == 0L) {
            chapterDao.insertChapter(chapter)
        } else {
            chapterDao.updateChapter(chapter)
            chapter.id
        }
    }

    suspend fun deleteChapter(chapterId: Long) = withContext(Dispatchers.IO) {
        chapterDao.deleteChapterById(chapterId)
    }

    suspend fun saveTocEntry(entry: com.example.data.database.entity.TocEntryEntity): Long = withContext(Dispatchers.IO) {
        if (entry.id == 0L) {
            tocDao.insertTocEntry(entry)
        } else {
            tocDao.updateTocEntry(entry)
            entry.id
        }
    }

    suspend fun deleteTocEntry(entryId: Long) = withContext(Dispatchers.IO) {
        tocDao.deleteTocEntryById(entryId)
    }

    suspend fun toggleBookmark(
        documentId: Long,
        pageId: Long,
        pageIndex: Int,
        chapterTitle: String = "",
        title: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getBookmarkForPage(documentId, pageIndex)
        if (existing != null) {
            bookmarkDao.deleteBookmark(existing)
            false
        } else {
            val bookmark = com.example.data.database.entity.BookmarkEntity(
                documentId = documentId,
                pageId = pageId,
                pageIndex = pageIndex,
                chapterTitle = chapterTitle,
                title = if (title.isNotBlank()) title else "علامة صفحة ${pageIndex + 1}"
            )
            bookmarkDao.insertBookmark(bookmark)
            true
        }
    }

    suspend fun addBookNote(
        documentId: Long,
        pageId: Long? = null,
        pageIndex: Int? = null,
        chapterId: Long? = null,
        content: String
    ): Long = withContext(Dispatchers.IO) {
        val note = com.example.data.database.entity.BookNoteEntity(
            documentId = documentId,
            pageId = pageId,
            pageIndex = pageIndex,
            chapterId = chapterId,
            content = content
        )
        bookNoteDao.insertNote(note)
    }

    suspend fun deleteBookNote(noteId: Long) = withContext(Dispatchers.IO) {
        bookNoteDao.deleteNoteById(noteId)
    }

    // =========================================================================
    // V0.7: ADVANCED CATEGORY SYSTEM
    // =========================================================================

    fun getCategoryById(categoryId: Long): Flow<CategoryEntity?> =
        categoryDao.getCategoryById(categoryId)

    suspend fun getCategoryByIdDirect(categoryId: Long): CategoryEntity? =
        categoryDao.getCategoryByIdDirect(categoryId)

    fun getCategoriesForDocument(documentId: Long): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesForDocument(documentId)

    suspend fun getCategoriesForDocumentDirect(documentId: Long): List<CategoryEntity> =
        categoryDao.getCategoriesForDocumentDirect(documentId)

    fun getDocumentsForCategory(categoryId: Long): Flow<List<DocumentWithPages>> =
        categoryDao.getDocumentsWithPagesForCategory(categoryId)

    fun getDocumentCountForCategory(categoryId: Long): Flow<Int> =
        categoryDao.getDocumentCountForCategory(categoryId)

    suspend fun saveCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        val normalized = ArabicOcrPostProcessor.normalizeForSearch(category.name)
        val entityToSave = category.copy(
            normalizedName = normalized,
            updatedAt = System.currentTimeMillis()
        )
        if (entityToSave.id == 0L) {
            categoryDao.insertCategory(entityToSave)
        } else {
            categoryDao.updateCategory(entityToSave)
            entityToSave.id
        }
    }

    suspend fun deleteCategory(categoryId: Long) = withContext(Dispatchers.IO) {
        // Safe deletion: Removes category record and cross-references, but leaves all books intact!
        categoryDao.deleteCategoryById(categoryId)
    }

    suspend fun assignBookToCategory(documentId: Long, categoryId: Long) = withContext(Dispatchers.IO) {
        categoryDao.assignBookToCategory(BookCategoryCrossRef(documentId, categoryId))
        // Sync primary category string on document for fast single-column index
        val category = categoryDao.getCategoryByIdDirect(categoryId)
        if (category != null) {
            val doc = documentDao.getDocumentByIdDirect(documentId)
            if (doc != null && (doc.category == "عام" || doc.category.isBlank())) {
                documentDao.updateDocument(doc.copy(category = category.name, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun removeBookFromCategory(documentId: Long, categoryId: Long) = withContext(Dispatchers.IO) {
        categoryDao.removeBookFromCategory(documentId, categoryId)
    }

    suspend fun setCategoriesForDocument(documentId: Long, categoryIds: List<Long>) = withContext(Dispatchers.IO) {
        categoryDao.removeAllCategoriesFromBook(documentId)
        val crossRefs = categoryIds.map { BookCategoryCrossRef(documentId, it) }
        categoryDao.assignBookToCategories(crossRefs)
        if (categoryIds.isNotEmpty()) {
            val primaryCat = categoryDao.getCategoryByIdDirect(categoryIds.first())
            if (primaryCat != null) {
                val doc = documentDao.getDocumentByIdDirect(documentId)
                if (doc != null) {
                    documentDao.updateDocument(doc.copy(category = primaryCat.name, updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    // =========================================================================
    // V0.7: BOOK SERIES MANAGEMENT
    // =========================================================================

    fun getSeriesById(seriesId: Long): Flow<SeriesEntity?> =
        seriesDao.getSeriesById(seriesId)

    suspend fun getSeriesByIdDirect(seriesId: Long): SeriesEntity? =
        seriesDao.getSeriesByIdDirect(seriesId)

    fun getSeriesForDocument(documentId: Long): Flow<SeriesEntity?> =
        seriesDao.getSeriesForDocument(documentId)

    suspend fun getSeriesForDocumentDirect(documentId: Long): SeriesEntity? =
        seriesDao.getSeriesForDocumentDirect(documentId)

    fun getDocumentsForSeries(seriesId: Long): Flow<List<DocumentWithPages>> =
        seriesDao.getDocumentsWithPagesForSeries(seriesId)

    suspend fun getDocumentsForSeriesDirect(seriesId: Long): List<DocumentWithPages> =
        seriesDao.getDocumentsWithPagesForSeriesDirect(seriesId)

    fun getBookCountForSeries(seriesId: Long): Flow<Int> =
        seriesDao.getBookCountForSeries(seriesId)

    suspend fun saveSeries(series: SeriesEntity): Long = withContext(Dispatchers.IO) {
        val normalized = ArabicOcrPostProcessor.normalizeForSearch(series.title)
        val entityToSave = series.copy(
            normalizedTitle = normalized,
            updatedAt = System.currentTimeMillis()
        )
        if (entityToSave.id == 0L) {
            seriesDao.insertSeries(entityToSave)
        } else {
            seriesDao.updateSeries(entityToSave)
            entityToSave.id
        }
    }

    suspend fun deleteSeries(seriesId: Long) = withContext(Dispatchers.IO) {
        // Safe deletion: Removes series and cross-references, keeps all books intact!
        seriesDao.deleteSeriesById(seriesId)
    }

    suspend fun assignBookToSeries(
        documentId: Long,
        seriesId: Long,
        volumeNumber: Int = 1,
        volumeTitle: String = ""
    ) = withContext(Dispatchers.IO) {
        seriesDao.removeBookFromAllSeries(documentId)
        seriesDao.assignBookToSeries(
            BookSeriesCrossRef(
                documentId = documentId,
                seriesId = seriesId,
                volumeNumber = volumeNumber,
                volumeTitle = volumeTitle
            )
        )
    }

    suspend fun removeBookFromSeries(documentId: Long, seriesId: Long) = withContext(Dispatchers.IO) {
        seriesDao.removeBookFromSeries(documentId, seriesId)
    }

    suspend fun getBookSeriesInfo(documentId: Long, seriesId: Long): BookSeriesCrossRef? = withContext(Dispatchers.IO) {
        seriesDao.getBookSeriesCrossRef(documentId, seriesId)
    }

    // =========================================================================
    // V0.7: ADVANCED MULTI-CRITERIA SEARCH, FILTERING & SORTING
    // =========================================================================

    /**
     * Executes real multi-criteria search, filtering, and sorting over the library.
     * Works 100% offline at the local level.
     */
    fun searchAndFilterDocuments(
        query: String = "",
        filterOptions: LibraryFilterOptions = LibraryFilterOptions(),
        sortOption: LibrarySortOption = LibrarySortOption()
    ): Flow<List<DocumentSearchResult>> {
        val trimmedQuery = query.trim()
        val normalizedQuery = if (trimmedQuery.isNotEmpty()) ArabicOcrPostProcessor.normalizeForSearch(trimmedQuery) else ""

        return allDocuments.map { docs ->
            val filteredResults = mutableListOf<DocumentSearchResult>()

            for (docWithPages in docs) {
                val doc = docWithPages.document

                // 1. Filter: Favorites
                if (filterOptions.onlyFavorites && !doc.isFavorite) {
                    continue
                }

                // 2. Filter: DocumentType
                if (filterOptions.documentType != null && doc.type != filterOptions.documentType) {
                    continue
                }

                // 3. Filter: Category
                if (filterOptions.categoryName != null && !doc.category.equals(filterOptions.categoryName, ignoreCase = true)) {
                    continue
                }

                // 4. Filter: Author
                if (filterOptions.authorId != null && doc.authorId != filterOptions.authorId) {
                    continue
                }
                if (filterOptions.authorName != null && !doc.authorName.contains(filterOptions.authorName, ignoreCase = true)) {
                    continue
                }

                // 5. Filter: OCR status
                val hasOcr = docWithPages.pages.any { it.effectiveOcrText.isNotBlank() }
                if (filterOptions.onlyWithOcr && !hasOcr) {
                    continue
                }
                if (filterOptions.onlyWithoutOcr && hasOcr) {
                    continue
                }

                // 6. Search matching: Title, Author, Category, or Page OCR
                if (normalizedQuery.isEmpty()) {
                    filteredResults.add(DocumentSearchResult(docWithPages, matchedInTitle = false))
                } else {
                    val titleNormalized = ArabicOcrPostProcessor.normalizeForSearch(doc.title)
                    val matchesTitle = titleNormalized.contains(normalizedQuery)

                    val authorNormalized = ArabicOcrPostProcessor.normalizeForSearch(doc.authorName)
                    val matchesAuthor = authorNormalized.contains(normalizedQuery)

                    val categoryNormalized = ArabicOcrPostProcessor.normalizeForSearch(doc.category)
                    val matchesCategory = categoryNormalized.contains(normalizedQuery)

                    var matchedPageNumber: Int? = null
                    var matchedPageId: Long? = null
                    var snippet: String? = null

                    for (page in docWithPages.pages.sortedBy { it.pageIndex }) {
                        val textToSearch = page.effectiveOcrText
                        if (textToSearch.isNotBlank()) {
                            val pageNormalized = ArabicOcrPostProcessor.normalizeForSearch(textToSearch)
                            val matchIndex = pageNormalized.indexOf(normalizedQuery)
                            if (matchIndex != -1) {
                                matchedPageNumber = page.pageIndex + 1
                                matchedPageId = page.id
                                val snippetStart = (matchIndex - 30).coerceAtLeast(0)
                                val snippetEnd = (matchIndex + normalizedQuery.length + 30).coerceAtMost(textToSearch.length)
                                val rawSnippet = textToSearch.substring(snippetStart, snippetEnd).trim()
                                snippet = "...$rawSnippet..."
                                break
                            }
                        }
                    }

                    if (matchesTitle || matchesAuthor || matchesCategory || matchedPageNumber != null) {
                        filteredResults.add(
                            DocumentSearchResult(
                                documentWithPages = docWithPages,
                                matchedInTitle = matchesTitle,
                                matchedPageNumber = matchedPageNumber,
                                matchedPageId = matchedPageId,
                                matchedSnippet = snippet,
                                matchedInAuthor = matchesAuthor,
                                matchedInCategory = matchesCategory
                            )
                        )
                    }
                }
            }

            // 7. Apply Sorting
            val sortedList = when (sortOption.field) {
                LibrarySortField.DATE_ADDED -> {
                    if (sortOption.direction == SortDirection.ASCENDING) {
                        filteredResults.sortedBy { it.documentWithPages.document.createdAt }
                    } else {
                        filteredResults.sortedByDescending { it.documentWithPages.document.createdAt }
                    }
                }
                LibrarySortField.TITLE -> {
                    if (sortOption.direction == SortDirection.ASCENDING) {
                        filteredResults.sortedBy { it.documentWithPages.document.title }
                    } else {
                        filteredResults.sortedByDescending { it.documentWithPages.document.title }
                    }
                }
                LibrarySortField.AUTHOR -> {
                    if (sortOption.direction == SortDirection.ASCENDING) {
                        filteredResults.sortedBy { it.documentWithPages.document.authorName }
                    } else {
                        filteredResults.sortedByDescending { it.documentWithPages.document.authorName }
                    }
                }
                LibrarySortField.PAGE_COUNT -> {
                    if (sortOption.direction == SortDirection.ASCENDING) {
                        filteredResults.sortedBy { it.documentWithPages.pages.size }
                    } else {
                        filteredResults.sortedByDescending { it.documentWithPages.pages.size }
                    }
                }
                LibrarySortField.CATEGORY -> {
                    if (sortOption.direction == SortDirection.ASCENDING) {
                        filteredResults.sortedBy { it.documentWithPages.document.category }
                    } else {
                        filteredResults.sortedByDescending { it.documentWithPages.document.category }
                    }
                }
                LibrarySortField.LAST_UPDATED -> {
                    if (sortOption.direction == SortDirection.ASCENDING) {
                        filteredResults.sortedBy { it.documentWithPages.document.updatedAt }
                    } else {
                        filteredResults.sortedByDescending { it.documentWithPages.document.updatedAt }
                    }
                }
            }

            sortedList
        }
    }

    // =========================================================================
    // V0.7: BATCH OPERATIONS
    // =========================================================================

    suspend fun deleteDocumentsBatch(documentIds: List<Long>) = withContext(Dispatchers.IO) {
        if (documentIds.isEmpty()) return@withContext
        for (id in documentIds) {
            deleteDocument(id)
        }
    }

    suspend fun assignCategoryBatch(documentIds: List<Long>, categoryId: Long) = withContext(Dispatchers.IO) {
        val category = categoryDao.getCategoryByIdDirect(categoryId) ?: return@withContext
        for (id in documentIds) {
            assignBookToCategory(id, categoryId)
        }
        documentDao.updateCategoryForDocuments(documentIds, category.name)
    }

    suspend fun assignAuthorBatch(documentIds: List<Long>, authorId: Long?, authorName: String) = withContext(Dispatchers.IO) {
        documentDao.updateAuthorForDocuments(documentIds, authorId, authorName)
    }

    // =========================================================================
    // V0.7: REAL DATABASE LIBRARY STATISTICS
    // =========================================================================

    suspend fun getLibraryStatistics(): LibraryStats = withContext(Dispatchers.IO) {
        val totalDocs = documentDao.getTotalDocumentsCount()
        val totalFavorites = documentDao.getTotalFavoritesCount()
        val totalPages = pageDao.getTotalPagesCount()
        val pagesWithOcr = pageDao.getPagesWithOcrCount()
        val totalAuthors = authorDao.getTotalAuthorsCount()
        val totalCategories = categoryDao.getTotalCategoriesCount()
        val totalSeries = seriesDao.getTotalSeriesCount()

        val allDocs = documentDao.getAllDocumentsDirect()
        var docsWithOcr = 0
        for (doc in allDocs) {
            val pages = pageDao.getPagesListForDocument(doc.id)
            if (pages.any { it.effectiveOcrText.isNotBlank() }) {
                docsWithOcr++
            }
        }

        val totalChunks = database.textChunkDao().getTotalChunksCount()
        val totalEmbeddings = database.embeddingDao().getTotalEmbeddingsCount()
        val totalChapters = database.chapterDao().getTotalChaptersCount()
        val totalBookmarks = database.bookmarkDao().getTotalBookmarksCount()
        val totalNotes = database.bookNoteDao().getTotalNotesCount()

        val coveragePercent = if (totalPages > 0) ((pagesWithOcr.toFloat() / totalPages) * 100).toInt() else 0

        LibraryStats(
            totalDocuments = totalDocs,
            totalPages = totalPages,
            totalAuthors = totalAuthors,
            totalCategories = totalCategories,
            totalSeries = totalSeries,
            documentsWithOcr = docsWithOcr,
            pagesWithOcr = pagesWithOcr,
            ocrCoveragePercent = coveragePercent,
            totalFavorites = totalFavorites,
            totalTextChunks = totalChunks,
            totalEmbeddings = totalEmbeddings,
            totalChapters = totalChapters,
            totalBookmarks = totalBookmarks,
            totalNotes = totalNotes,
            calculatedAt = System.currentTimeMillis()
        )
    }

    // =========================================================================
    // V0.7: LOCAL SIMILAR-BOOK DETECTION ("كتب مشابهة")
    // =========================================================================

    /**
     * Calculates local book similarities based on shared categories, shared authors,
     * series, keywords, topics, and vector embedding representations.
     * ZERO NETWORK CALLS. 100% offline.
     */
    suspend fun getSimilarBooks(documentId: Long, limit: Int = 5): List<SimilarBookResult> = withContext(Dispatchers.IO) {
        val targetDoc = documentDao.getDocumentWithPagesByIdDirect(documentId) ?: return@withContext emptyList()
        val allOtherDocs = documentDao.getAllDocumentsDirect().filter { it.id != documentId }
        if (allOtherDocs.isEmpty()) return@withContext emptyList()

        val targetCategories = categoryDao.getCategoriesForDocumentDirect(documentId).map { it.name }.toSet()
        val targetSeries = seriesDao.getSeriesForDocumentDirect(documentId)
        val targetKeywords = database.keywordDao().getKeywordsByDocumentIdDirect(documentId).map { it.normalizedTerm }.toSet()
        val targetTopics = database.topicDao().getTopicsByDocumentIdDirect(documentId).map { it.normalizedTopic }.toSet()

        val results = mutableListOf<SimilarBookResult>()

        for (otherDocEntity in allOtherDocs) {
            val otherDocWithPages = documentDao.getDocumentWithPagesByIdDirect(otherDocEntity.id) ?: continue
            val otherCategories = categoryDao.getCategoriesForDocumentDirect(otherDocEntity.id).map { it.name }.toSet()
            val otherSeries = seriesDao.getSeriesForDocumentDirect(otherDocEntity.id)
            val otherKeywords = database.keywordDao().getKeywordsByDocumentIdDirect(otherDocEntity.id).map { it.normalizedTerm }.toSet()
            val otherTopics = database.topicDao().getTopicsByDocumentIdDirect(otherDocEntity.id).map { it.normalizedTopic }.toSet()

            var score = 0.0f
            val reasons = mutableListOf<String>()

            // 1. Shared Series match (High weight: +0.40)
            val sharedSeries = if (targetSeries != null && otherSeries != null && targetSeries.id == otherSeries.id) {
                score += 0.40f
                reasons.add("جزء من نفس السلسلة: ${targetSeries.title}")
                targetSeries.title
            } else null

            // 2. Shared Author match (Weight: +0.25)
            val sharedAuthor = if (targetDoc.document.authorName.isNotBlank() &&
                targetDoc.document.authorName != "غير محدد" &&
                targetDoc.document.authorName.equals(otherDocEntity.authorName, ignoreCase = true)
            ) {
                score += 0.25f
                reasons.add("نفس المؤلف: ${targetDoc.document.authorName}")
                true
            } else false

            // 3. Shared Categories match (Jaccard similarity, up to +0.20)
            val sharedCats = (targetCategories.intersect(otherCategories)).toList()
            if (sharedCats.isNotEmpty() || (targetDoc.document.category.isNotBlank() && targetDoc.document.category != "عام" && targetDoc.document.category == otherDocEntity.category)) {
                score += 0.20f
                val catNames = if (sharedCats.isNotEmpty()) sharedCats else listOf(targetDoc.document.category)
                reasons.add("تصنيف مشترك: ${catNames.joinToString(", ")}")
            }

            // 4. Shared Topics & Keywords (up to +0.25)
            val sharedTops = (targetTopics.intersect(otherTopics)).toList()
            val sharedKeys = (targetKeywords.intersect(otherKeywords)).toList()
            if (sharedTops.isNotEmpty()) {
                score += (sharedTops.size * 0.08f).coerceAtMost(0.15f)
                reasons.add("موضوعات متشابهة (${sharedTops.take(2).joinToString("، ")})")
            }
            if (sharedKeys.isNotEmpty()) {
                score += (sharedKeys.size * 0.04f).coerceAtMost(0.10f)
                reasons.add("مصطلحات مشتركة (${sharedKeys.take(3).joinToString("، ")})")
            }

            val finalScore = score.coerceIn(0.10f, 0.99f)
            if (finalScore >= 0.20f || reasons.isNotEmpty()) {
                results.add(
                    SimilarBookResult(
                        documentWithPages = otherDocWithPages,
                        similarityScore = finalScore,
                        sharedCategories = sharedCats,
                        sharedAuthor = sharedAuthor,
                        sharedSeries = sharedSeries,
                        sharedTopics = sharedTops,
                        sharedKeywords = sharedKeys,
                        primaryReason = reasons.firstOrNull() ?: "تقارب في المحتوى والأرشفة"
                    )
                )
            }
        }

        results.sortedByDescending { it.similarityScore }.take(limit)
    }
}

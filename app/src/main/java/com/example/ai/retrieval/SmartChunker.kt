package com.example.ai.retrieval

import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.PageEntity
import com.example.data.database.entity.PrintedPageNumberEntity
import com.example.data.database.entity.TextChunkEntity
import com.example.ocr.cleaner.ArabicTextCleaner

object SmartChunker {

    private const val TARGET_CHUNK_CHARS = 800
    private const val OVERLAP_CHARS = 120

    /**
     * Chunks a collection of pages for a book while preserving chapter boundaries,
     * printed page numbers, and physical page indices.
     */
    fun chunkDocument(
        documentId: Long,
        pages: List<PageEntity>,
        chapters: List<ChapterEntity> = emptyList(),
        printedPages: List<PrintedPageNumberEntity> = emptyList()
    ): List<TextChunkEntity> {
        val chunks = mutableListOf<TextChunkEntity>()
        val printedMap = printedPages.associateBy { it.physicalPageIndex }

        for (page in pages) {
            val text = page.effectiveOcrText.trim()
            if (text.isBlank()) continue

            // Determine chapter if page is within chapter range
            val chapter = chapters.find { ch ->
                page.pageIndex in ch.startPageIndex..ch.endPageIndex
            }

            val printedPageNum = printedMap[page.pageIndex]?.printedPageNumber
            val physicalPageNum = page.pageIndex + 1

            val pageChunks = chunkPageText(
                documentId = documentId,
                pageId = page.id,
                pageIndex = page.pageIndex,
                chapterId = chapter?.id,
                sectionTitle = chapter?.title,
                physicalPageNumber = physicalPageNum,
                printedPageNumber = printedPageNum,
                fullText = text
            )
            chunks.addAll(pageChunks)
        }

        return chunks
    }

    fun chunkPageText(
        documentId: Long,
        pageId: Long,
        pageIndex: Int,
        chapterId: Long?,
        sectionTitle: String?,
        physicalPageNumber: Int,
        printedPageNumber: String?,
        fullText: String
    ): List<TextChunkEntity> {
        val result = mutableListOf<TextChunkEntity>()
        if (fullText.length <= TARGET_CHUNK_CHARS) {
            result.add(
                TextChunkEntity(
                    documentId = documentId,
                    pageId = pageId,
                    pageIndex = pageIndex,
                    chapterId = chapterId,
                    sectionTitle = sectionTitle,
                    physicalPageNumber = physicalPageNumber,
                    printedPageNumber = printedPageNumber,
                    chunkIndex = 0,
                    text = fullText,
                    normalizedText = ArabicTextCleaner.normalizeForSearch(fullText),
                    tokenCount = countTokens(fullText),
                    startOffset = 0,
                    endOffset = fullText.length
                )
            )
            return result
        }

        // Split by paragraphs first
        val paragraphs = fullText.split(Regex("\\n{2,}"))
        var currentChunkText = StringBuilder()
        var chunkIndex = 0
        var startOffset = 0

        for (para in paragraphs) {
            val trimmedPara = para.trim()
            if (trimmedPara.isEmpty()) continue

            if (currentChunkText.length + trimmedPara.length > TARGET_CHUNK_CHARS && currentChunkText.isNotEmpty()) {
                val chunkStr = currentChunkText.toString().trim()
                result.add(
                    TextChunkEntity(
                        documentId = documentId,
                        pageId = pageId,
                        pageIndex = pageIndex,
                        chapterId = chapterId,
                        sectionTitle = sectionTitle,
                        physicalPageNumber = physicalPageNumber,
                        printedPageNumber = printedPageNumber,
                        chunkIndex = chunkIndex++,
                        text = chunkStr,
                        normalizedText = ArabicTextCleaner.normalizeForSearch(chunkStr),
                        tokenCount = countTokens(chunkStr),
                        startOffset = startOffset,
                        endOffset = startOffset + chunkStr.length
                    )
                )

                // Overlap: retain last portion of chunk
                val overlap = if (chunkStr.length > OVERLAP_CHARS) {
                    chunkStr.takeLast(OVERLAP_CHARS)
                } else ""

                currentChunkText = StringBuilder(overlap)
                startOffset += (chunkStr.length - overlap.length)
            }

            if (currentChunkText.isNotEmpty()) {
                currentChunkText.append("\n\n")
            }
            currentChunkText.append(trimmedPara)
        }

        if (currentChunkText.isNotBlank()) {
            val chunkStr = currentChunkText.toString().trim()
            result.add(
                TextChunkEntity(
                    documentId = documentId,
                    pageId = pageId,
                    pageIndex = pageIndex,
                    chapterId = chapterId,
                    sectionTitle = sectionTitle,
                    physicalPageNumber = physicalPageNumber,
                    printedPageNumber = printedPageNumber,
                    chunkIndex = chunkIndex,
                    text = chunkStr,
                    normalizedText = ArabicTextCleaner.normalizeForSearch(chunkStr),
                    tokenCount = countTokens(chunkStr),
                    startOffset = startOffset,
                    endOffset = startOffset + chunkStr.length
                )
            )
        }

        return result
    }

    private fun countTokens(text: String): Int {
        return text.split(Regex("\\s+")).count { it.isNotBlank() }
    }
}

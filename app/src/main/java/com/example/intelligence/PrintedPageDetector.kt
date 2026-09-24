package com.example.intelligence

import com.example.data.database.entity.PageEntity
import com.example.data.database.entity.PrintedPageNumberEntity

object PrintedPageDetector {

    private val DIGIT_PAGE_REGEX = Regex("""\b([0-9٠-٩]{1,4})\b""")
    private val ROMAN_PAGE_REGEX = Regex("""\b([ivxlcdm]{1,6})\b""", RegexOption.IGNORE_CASE)

    /**
     * Extracts printed page number from header (first 2 lines) or footer (last 2 lines).
     */
    fun detectPrintedPage(documentId: Long, page: PageEntity): PrintedPageNumberEntity? {
        val text = page.effectiveOcrText
        if (text.isBlank()) return null

        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        // Check footer lines first (standard in Arabic books: bottom center or bottom margin)
        val candidateLines = listOfNotNull(
            lines.lastOrNull(),
            if (lines.size > 1) lines[lines.size - 2] else null,
            lines.firstOrNull() // header
        )

        for (line in candidateLines) {
            // If the line is short (typical of standalone page numbers e.g. "— 45 —" or "٢٥")
            if (line.length <= 15) {
                val match = DIGIT_PAGE_REGEX.find(line)
                if (match != null) {
                    val num = match.groupValues[1]
                    return PrintedPageNumberEntity(
                        documentId = documentId,
                        pageId = page.id,
                        physicalPageIndex = page.pageIndex,
                        printedPageNumber = num,
                        confidence = 0.90f
                    )
                }

                val romanMatch = ROMAN_PAGE_REGEX.find(line)
                if (romanMatch != null) {
                    return PrintedPageNumberEntity(
                        documentId = documentId,
                        pageId = page.id,
                        physicalPageIndex = page.pageIndex,
                        printedPageNumber = romanMatch.groupValues[1],
                        confidence = 0.85f
                    )
                }
            }
        }

        return null
    }
}

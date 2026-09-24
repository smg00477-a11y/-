package com.example.intelligence

import com.example.data.database.entity.PageEntity
import com.example.data.database.entity.PrintedPageNumberEntity
import com.example.data.database.entity.TocEntryEntity
import com.example.ocr.cleaner.ArabicTextCleaner

object TocDetector {

    // Regex matching dotted leaders or multiple dots/dashes connecting a title to a page number
    // e.g.: "الفصل الأول ................. 15" or "الباب الأول ------------------ ٢٥"
    private val TOC_LINE_REGEX = Regex(
        """^(.+?)(?:[\.·•…\-_—–]{2,}|\s{3,})\s*([0-9٠-٩]+|[ivxlcdm]+)\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val TOC_HEADER_REGEX = Regex(
        """(?:فهرس|الفهرس|فهرست|محتويات|المحتويات|جدول\s*المحتويات|Table\s*of\s*Contents|Contents)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Inspects pages (front or back matter) for Table of Contents pages and extracts entries.
     */
    fun detectTocEntries(
        documentId: Long,
        pages: List<PageEntity>,
        printedPageNumbers: List<PrintedPageNumberEntity>
    ): List<TocEntryEntity> {
        val sortedPages = pages.sortedBy { it.pageIndex }
        val entries = mutableListOf<TocEntryEntity>()

        // Look in both front matter (first 10 pages) and back matter (last 10 pages)
        val candidatePages = (sortedPages.take(10) + sortedPages.takeLast(10)).distinctBy { it.id }

        var readingOrder = 1

        for (page in candidatePages) {
            val text = page.effectiveOcrText
            if (text.isBlank()) continue

            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

            val hasTocHeader = lines.take(3).any { line -> TOC_HEADER_REGEX.containsMatchIn(line) }
            val matchingLines = lines.mapNotNull { line -> TOC_LINE_REGEX.find(line) }

            // If page has a TOC header OR at least 3 dotted-leader entries, treat as TOC page
            if (hasTocHeader || matchingLines.size >= 3) {
                for (match in matchingLines) {
                    val rawTitle = match.groupValues[1].trim()
                    val pageStr = match.groupValues[2].trim()

                    if (rawTitle.length < 2 || rawTitle.length > 100) continue

                    // Parse printed page number (convert Arabic-Indic digits if present)
                    val westernDigits = parseArabicDigits(pageStr)
                    val targetPhysical = resolvePhysicalPage(westernDigits, sortedPages, printedPageNumbers)

                    val cleanTitle = ArabicTextCleaner.cleanArabicText(rawTitle)

                    entries.add(
                        TocEntryEntity(
                            documentId = documentId,
                            title = cleanTitle,
                            targetPhysicalPageIndex = targetPhysical,
                            targetPrintedPage = pageStr,
                            level = determineLevel(cleanTitle),
                            readingOrder = readingOrder++,
                            confidence = 0.85f,
                            detectionStatus = "DETECTED"
                        )
                    )
                }
            }
        }

        return entries
    }

    private fun determineLevel(title: String): Int {
        return when {
            title.startsWith("الباب") || title.startsWith("الجزء") -> 1
            title.startsWith("الفصل") -> 1
            title.startsWith("المبحث") || title.startsWith("القسم") -> 2
            title.startsWith("المطلب") -> 3
            else -> 1
        }
    }

    private fun parseArabicDigits(input: String): Int? {
        val converted = input.map { char ->
            when (char) {
                '٠' -> '0'
                '١' -> '1'
                '٢' -> '2'
                '٣' -> '3'
                '٤' -> '4'
                '٥' -> '5'
                '٦' -> '6'
                '٧' -> '7'
                '٨' -> '8'
                '٩' -> '9'
                else -> char
            }
        }.joinToString("")

        return converted.toIntOrNull()
    }

    private fun resolvePhysicalPage(
        targetPrintedNum: Int?,
        pages: List<PageEntity>,
        printedNumbers: List<PrintedPageNumberEntity>
    ): Int {
        if (targetPrintedNum == null) return 0

        // 1. Look up in detected printed page numbers
        val match = printedNumbers.firstOrNull {
            parseArabicDigits(it.printedPageNumber) == targetPrintedNum
        }
        if (match != null) {
            return match.physicalPageIndex
        }

        // 2. Fallback heuristic: 0-indexed guess within bounds
        return (targetPrintedNum - 1).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    }
}

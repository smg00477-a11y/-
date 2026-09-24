package com.example.ocr.export

import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.TableEntity
import com.example.data.database.entity.TocEntryEntity

object OcrTextFormatter {

    /**
     * Post-processes raw OCR or user-edited text into clean, structured publication text.
     */
    fun formatTextForExport(
        rawText: String,
        cleanLineBreaks: Boolean = true,
        fixRepeatedSpaces: Boolean = true,
        filterOcrNoise: Boolean = true
    ): String {
        if (rawText.isBlank()) return ""

        var processed = rawText

        // 1. Fix repeated whitespace and trailing/leading spaces per line
        if (fixRepeatedSpaces) {
            processed = processed.lines().joinToString("\n") { line ->
                line.replace(Regex("[ \\t]+"), " ").trim()
            }
        }

        // 2. Clean common OCR noise artifacts
        if (filterOcrNoise) {
            processed = processed
                .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "") // Control chars
                .replace(Regex("~+"), "")
                .replace(Regex("_+"), " ")
        }

        // 3. Format line breaks into natural paragraphs
        if (cleanLineBreaks) {
            val lines = processed.split("\n")
            val formattedSb = StringBuilder()
            var currentParagraph = StringBuilder()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    if (currentParagraph.isNotEmpty()) {
                        formattedSb.append(currentParagraph.toString().trim()).append("\n\n")
                        currentParagraph.clear()
                    }
                } else {
                    if (currentParagraph.isNotEmpty()) {
                        val lastChar = currentParagraph.last()
                        // If sentence ended with dot, question mark, colon, or exclamation mark, or line looks like heading, split
                        if (lastChar == '.' || lastChar == '؟' || lastChar == '!' || lastChar == ':' || lastChar == '؛' || isHeadingLine(trimmed)) {
                            formattedSb.append(currentParagraph.toString().trim()).append("\n\n")
                            currentParagraph.clear()
                            currentParagraph.append(trimmed)
                        } else {
                            // Join lines in same paragraph with a space
                            currentParagraph.append(" ").append(trimmed)
                        }
                    } else {
                        currentParagraph.append(trimmed)
                    }
                }
            }

            if (currentParagraph.isNotEmpty()) {
                formattedSb.append(currentParagraph.toString().trim()).append("\n\n")
            }

            processed = formattedSb.toString().trim()
        }

        return processed
    }

    /**
     * Determines if a line represents a heading or section title.
     */
    fun isHeadingLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.length in 3..60 && !trimmed.endsWith(".") && !trimmed.endsWith("،")) {
            if (trimmed.startsWith("الباب") || trimmed.startsWith("الفصل") ||
                trimmed.startsWith("المبحث") || trimmed.startsWith("المدخل") ||
                trimmed.startsWith("مقدمة") || trimmed.startsWith("خاتمة") ||
                trimmed.startsWith("Chapter") || trimmed.startsWith("Section")
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Formats database TableEntity list into a plain text or Markdown grid representation.
     */
    fun formatTableToGridText(table: TableEntity, cellsTextMap: Map<Pair<Int, Int>, String>): String {
        if (table.rowCount <= 0 || table.columnCount <= 0) return ""

        val sb = StringBuilder()
        sb.append("=== جدول: ").append(table.title.ifBlank { "بيانات جدولية" }).append(" ===\n")

        val colWidths = IntArray(table.columnCount) { 10 }
        for (r in 0 until table.rowCount) {
            for (c in 0 until table.columnCount) {
                val content = cellsTextMap[Pair(r, c)] ?: ""
                colWidths[c] = maxOf(colWidths[c], content.length + 2)
            }
        }

        // Draw top border
        sb.append("+")
        colWidths.forEach { w -> sb.append("-".repeat(w)).append("+") }
        sb.append("\n")

        for (r in 0 until table.rowCount) {
            sb.append("|")
            for (c in 0 until table.columnCount) {
                val content = cellsTextMap[Pair(r, c)] ?: ""
                val padded = " " + content.padEnd(colWidths[c] - 1)
                sb.append(padded).append("|")
            }
            sb.append("\n")

            // Header separator after row 0
            if (r == 0) {
                sb.append("+")
                colWidths.forEach { w -> sb.append("=").append("=".repeat(w - 1)).append("+") }
                sb.append("\n")
            }
        }

        // Draw bottom border
        sb.append("+")
        colWidths.forEach { w -> sb.append("-".repeat(w)).append("+") }
        sb.append("\n")

        return sb.toString()
    }

    /**
     * Constructs a structured Table of Contents (TOC) string.
     */
    fun buildTocString(tocList: List<TocEntryEntity>, chaptersList: List<ChapterEntity>): String {
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("           فهرس المحتويات (TOC)          \n")
        sb.append("=========================================\n\n")

        if (chaptersList.isNotEmpty()) {
            chaptersList.sortedBy { it.readingOrder }.forEach { ch ->
                val title = ch.title
                val pStart = ch.startPageIndex + 1
                val pEnd = ch.endPageIndex + 1
                sb.append("• ").append(title.padEnd(45, '.'))
                    .append(" ص ").append(pStart)
                    .append(if (pStart != pEnd) "-$pEnd" else "")
                    .append("\n")
            }
        } else if (tocList.isNotEmpty()) {
            tocList.sortedBy { it.readingOrder }.forEach { entry ->
                val indent = "  ".repeat(entry.level.coerceAtLeast(0))
                val title = entry.title
                val pNum = entry.targetPhysicalPageIndex + 1
                sb.append(indent).append("• ").append(title.padEnd(40 - indent.length, '.'))
                    .append(" ص ").append(pNum).append("\n")
            }
        } else {
            sb.append("(لا يتوفر فهرس أبواب مسجل لهذا الكتاب)\n")
        }

        sb.append("\n")
        return sb.toString()
    }
}

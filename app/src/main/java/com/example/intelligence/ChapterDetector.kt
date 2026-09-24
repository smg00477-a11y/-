package com.example.intelligence

import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.PageEntity
import com.example.ocr.cleaner.ArabicTextCleaner

object ChapterDetector {

    private val CHAPTER_PATTERNS = listOf(
        // Arabic Chapter: الفصل الأول، الفصل 1
        Regex("""^\s*(الفصل\s+(?:الأول|الثاني|الثالث|الرابع|الخامس|السادس|السابع|الثامن|التاسع|العاشر|الحادي\s*عشر|الثاني\s*عشر|[0-9٠-٩]+))(?:\s*[:：\-–.]\s*(.*))?"""),
        // Arabic Part: الباب الأول، الباب 2
        Regex("""^\s*(الباب\s+(?:الأول|الثاني|الثالث|الرابع|الخامس|السادس|السابع|الثامن|التاسع|العاشر|[0-9٠-٩]+))(?:\s*[:：\-–.]\s*(.*))?"""),
        // Arabic Section: القسم الأول، الجزء الأول
        Regex("""^\s*((?:القسم|الجزء)\s+(?:الأول|الثاني|الثالث|الرابع|الخامس|[0-9٠-٩]+))(?:\s*[:：\-–.]\s*(.*))?"""),
        // Subsections: المبحث الأول، المطلب الأول
        Regex("""^\s*((?:المبحث|المطلب)\s+(?:الأول|الثاني|الثالث|الرابع|الخامس|[0-9٠-٩]+))(?:\s*[:：\-–.]\s*(.*))?"""),
        // Special structural sections
        Regex("""^\s*(المقدمة|التمهيد|المدخل|توطئة|كلمة\s*الناشر|تصدير|شكر\s*وتقدير)(?:\s*[:：\-–.]\s*(.*))?"""),
        Regex("""^\s*(الخاتمة|نتائج\s*البحث|التوصيات|ملحق\s*(?:[0-9٠-٩]+)?|الملاحق|ثبت\s*المصادر\s*والمراجع|قائمة\s*المراجع|الفهرس\s*العام)(?:\s*[:：\-–.]\s*(.*))?"""),
        // English Chapter / Part
        Regex("""^\s*(Chapter\s+[0-9IVXLCDM]+)(?:\s*[:：\-–.]\s*(.*))?""", RegexOption.IGNORE_CASE),
        Regex("""^\s*(Part\s+[0-9IVXLCDM]+)(?:\s*[:：\-–.]\s*(.*))?""", RegexOption.IGNORE_CASE)
    )

    data class RawChapterMarker(
        val title: String,
        val pageIndex: Int,
        val level: Int,
        val confidence: Float
    )

    /**
     * Scans pages sequentially to detect chapter start boundaries.
     */
    fun detectChapters(documentId: Long, pages: List<PageEntity>): List<ChapterEntity> {
        val sortedPages = pages.sortedBy { it.pageIndex }
        if (sortedPages.isEmpty()) return emptyList()

        val markers = mutableListOf<RawChapterMarker>()

        for (page in sortedPages) {
            val text = page.effectiveOcrText
            if (text.isBlank()) continue

            // Inspect the first 5 lines of the page for prominent headings
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }.take(6)

            for (line in lines) {
                // Must be a heading-sized line
                if (line.length in 4..90) {
                    for (pattern in CHAPTER_PATTERNS) {
                        val match = pattern.find(line)
                        if (match != null) {
                            val mainMarker = match.groupValues[1].trim()
                            val subtitle = match.groupValues.getOrNull(2)?.trim()
                            val fullTitle = if (!subtitle.isNullOrBlank()) {
                                "$mainMarker: $subtitle"
                            } else {
                                mainMarker
                            }

                            val level = when {
                                mainMarker.startsWith("الباب") || mainMarker.startsWith("الجزء") || mainMarker.startsWith("Part") -> 1
                                mainMarker.startsWith("الفصل") || mainMarker.startsWith("Chapter") -> 1
                                mainMarker.startsWith("القسم") -> 2
                                mainMarker.startsWith("المبحث") -> 2
                                mainMarker.startsWith("المطلب") -> 3
                                else -> 1
                            }

                            // Don't add duplicate markers on the same page
                            if (markers.none { it.pageIndex == page.pageIndex }) {
                                markers.add(
                                    RawChapterMarker(
                                        title = ArabicTextCleaner.cleanArabicText(fullTitle),
                                        pageIndex = page.pageIndex,
                                        level = level,
                                        confidence = 0.90f
                                    )
                                )
                            }
                            break
                        }
                    }
                }
            }
        }

        if (markers.isEmpty()) {
            return emptyList()
        }

        // Construct ChapterEntity instances with proper start and end page indices
        val totalPages = sortedPages.size
        val result = mutableListOf<ChapterEntity>()

        for (i in markers.indices) {
            val current = markers[i]
            val nextStart = if (i + 1 < markers.size) markers[i + 1].pageIndex else totalPages
            val endPage = (nextStart - 1).coerceAtLeast(current.pageIndex)

            result.add(
                ChapterEntity(
                    documentId = documentId,
                    title = current.title,
                    normalizedTitle = ArabicTextCleaner.normalizeForSearch(current.title),
                    startPageIndex = current.pageIndex,
                    endPageIndex = endPage,
                    readingOrder = i + 1,
                    level = current.level,
                    detectionSource = "AUTO_HEADING",
                    confidence = current.confidence,
                    manuallyConfirmed = false
                )
            )
        }

        return result
    }
}

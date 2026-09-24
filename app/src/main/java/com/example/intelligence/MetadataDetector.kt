package com.example.intelligence

import com.example.data.database.entity.PageEntity
import com.example.ocr.cleaner.ArabicTextCleaner

data class DetectedMetadata(
    val title: String? = null,
    val subtitle: String? = null,
    val author: String? = null,
    val translator: String? = null,
    val editor: String? = null,
    val publisher: String? = null,
    val publicationYear: String? = null,
    val edition: String? = null,
    val isbn: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val confidence: Float = 0.0f
)

object MetadataDetector {

    private val AUTHOR_PATTERNS = listOf(
        Regex("""(?:تأليف|المؤلف|الكاتب|بقلم|جمع\s*وتحقيق|تصنيف)\s*[:：\-]?\s*([^\n\r,،]+)"""),
        Regex("""(?:للدكتور|للشيخ|للأستاذ|للكاتب)\s+([^\n\r,،]+)"""),
        Regex("""(?:Author|By)\s*[:：\-]?\s*([^\n\r,،]+)""", RegexOption.IGNORE_CASE)
    )

    private val TRANSLATOR_PATTERNS = listOf(
        Regex("""(?:ترجمة|المترجم|نقلها\s*إلى\s*العربية|عربه)\s*[:：\-]?\s*([^\n\r,،]+)"""),
        Regex("""(?:Translated\s*by|Translator)\s*[:：\-]?\s*([^\n\r,،]+)""", RegexOption.IGNORE_CASE)
    )

    private val EDITOR_PATTERNS = listOf(
        Regex("""(?:تحقيق|المحقق|تقديم|إعداد|إشراف|مراجعة)\s*[:：\-]?\s*([^\n\r,،]+)""")
    )

    private val PUBLISHER_PATTERNS = listOf(
        Regex("""(?:دار|منشورات|مكتبة|مؤسسة|مطبعة|مركز)\s+([^\n\r,،]{3,35})"""),
        Regex("""(?:الناشر|Publisher)\s*[:：\-]?\s*([^\n\r,،]+)""", RegexOption.IGNORE_CASE)
    )

    private val YEAR_PATTERNS = listOf(
        Regex("""(?:سنة|عام|تاريخ\s*النشر|طبع\s*سنة|طبعة\s*عام)\s*[:：\-]?\s*([0-9٠-٩]{4})"""),
        Regex("""\b(19\d\d|20\d\d)\b"""),
        Regex("""\b(13\d\d|14\d\d)\s*(?:هـ|هـ\.|هجري|هجرية)\b""")
    )

    private val EDITION_PATTERNS = listOf(
        Regex("""(?:الطبعة|طبعة)\s*[:：\-]?\s*(الأولى|الثانية|الثالثة|الرابعة|الخامسة|المزيدة|المنقحة|[0-9٠-٩]+)"""),
        Regex("""([0-9٠-٩]+)\s*(?:st|nd|rd|th)?\s*Edition""", RegexOption.IGNORE_CASE)
    )

    private val ISBN_PATTERNS = listOf(
        Regex("""(?:ISBN|ردمك)\s*[:：\-]?\s*([0-9\-–]{10,17})""", RegexOption.IGNORE_CASE)
    )

    /**
     * Inspects the first front-matter pages (up to 5 pages) to detect book metadata.
     */
    fun detectFromFrontMatter(pages: List<PageEntity>): DetectedMetadata {
        val candidatePages = pages.sortedBy { it.pageIndex }.take(5)
        if (candidatePages.isEmpty()) return DetectedMetadata()

        var detectedTitle: String? = null
        var detectedAuthor: String? = null
        var detectedTranslator: String? = null
        var detectedEditor: String? = null
        var detectedPublisher: String? = null
        var detectedYear: String? = null
        var detectedEdition: String? = null
        var detectedIsbn: String? = null

        for ((index, page) in candidatePages.withIndex()) {
            val text = page.effectiveOcrText
            if (text.isBlank()) continue

            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

            // Title extraction from page 0 or 1
            if (detectedTitle == null && (index == 0 || index == 1)) {
                // The first prominent line that isn't a basmala or publisher
                val titleCandidate = lines.firstOrNull { line ->
                    line.length in 3..60 &&
                    !line.contains("بسم الله") &&
                    !line.contains("الناشر") &&
                    !line.contains("مطبعة") &&
                    !line.contains("دار")
                }
                if (titleCandidate != null) {
                    detectedTitle = ArabicTextCleaner.cleanArabicText(titleCandidate)
                }
            }

            // Author extraction
            if (detectedAuthor == null) {
                for (pattern in AUTHOR_PATTERNS) {
                    val match = pattern.find(text)
                    if (match != null) {
                        detectedAuthor = ArabicTextCleaner.cleanArabicText(match.groupValues[1]).trim()
                        break
                    }
                }
            }

            // Translator
            if (detectedTranslator == null) {
                for (pattern in TRANSLATOR_PATTERNS) {
                    val match = pattern.find(text)
                    if (match != null) {
                        detectedTranslator = ArabicTextCleaner.cleanArabicText(match.groupValues[1]).trim()
                        break
                    }
                }
            }

            // Editor
            if (detectedEditor == null) {
                for (pattern in EDITOR_PATTERNS) {
                    val match = pattern.find(text)
                    if (match != null) {
                        detectedEditor = ArabicTextCleaner.cleanArabicText(match.groupValues[1]).trim()
                        break
                    }
                }
            }

            // Publisher
            if (detectedPublisher == null) {
                for (pattern in PUBLISHER_PATTERNS) {
                    val match = pattern.find(text)
                    if (match != null) {
                        val pub = match.value.trim()
                        if (pub.length in 4..40) {
                            detectedPublisher = ArabicTextCleaner.cleanArabicText(pub)
                            break
                        }
                    }
                }
            }

            // Year
            if (detectedYear == null) {
                for (pattern in YEAR_PATTERNS) {
                    val match = pattern.find(text)
                    if (match != null) {
                        detectedYear = match.groupValues.getOrNull(1) ?: match.value
                        break
                    }
                }
            }

            // Edition
            if (detectedEdition == null) {
                for (pattern in EDITION_PATTERNS) {
                    val match = pattern.find(text)
                    if (match != null) {
                        detectedEdition = match.groupValues.getOrNull(1) ?: match.value
                        break
                    }
                }
            }

            // ISBN
            if (detectedIsbn == null) {
                for (pattern in ISBN_PATTERNS) {
                    val match = pattern.find(text)
                    if (match != null) {
                        detectedIsbn = match.groupValues[1].trim()
                        break
                    }
                }
            }
        }

        // Categorize and suggest tags based on overall text
        val combinedText = candidatePages.joinToString(" ") { it.effectiveOcrText }
        val category = BookClassifier.classifyText(combinedText)
        val tags = BookClassifier.suggestTags(detectedTitle ?: "", combinedText, category)

        val confidence = listOfNotNull(
            detectedTitle, detectedAuthor, detectedPublisher, detectedYear, detectedIsbn
        ).size * 0.2f

        return DetectedMetadata(
            title = detectedTitle,
            author = detectedAuthor,
            translator = detectedTranslator,
            editor = detectedEditor,
            publisher = detectedPublisher,
            publicationYear = detectedYear,
            edition = detectedEdition,
            isbn = detectedIsbn,
            category = category,
            tags = tags,
            confidence = confidence.coerceIn(0.1f, 1.0f)
        )
    }
}

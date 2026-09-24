package com.example.ocr.model

enum class OcrStatus {
    NOT_PROCESSED,
    PROCESSING,
    COMPLETED,
    FAILED
}

enum class OcrLanguage(val code: String, val displayNameArabic: String, val displayNameEnglish: String) {
    ARABIC_AND_ENGLISH("ara+eng", "العربية والإنجليزية (شامل)", "Arabic + English"),
    ARABIC("ara", "العربية فقط", "Arabic Only"),
    ENGLISH("eng", "الإنجليزية فقط", "English Only");

    companion object {
        fun fromCode(code: String): OcrLanguage {
            return values().firstOrNull { it.code == code } ?: ARABIC_AND_ENGLISH
        }
    }
}

/**
 * Local classification of the page script / content type.
 */
enum class PageType(val displayNameArabic: String, val displayNameEnglish: String) {
    PRINTED("نص مطبوع", "Printed Text"),
    HANDWRITTEN("مخطوط / خط يدوي", "Handwritten / Manuscript"),
    MIXED("مختلط (مطبوع + يدوي)", "Mixed (Printed + Handwritten)"),
    IMAGE_ONLY("رسوم / صور فقط", "Image Only"),
    TABLE_HEAVY("جداول كثيفة", "Table Heavy"),
    COMPLEX_LAYOUT("تخطيط مركب وهوامش", "Complex Layout"),
    UNKNOWN("غير محدد", "Unknown");

    val titleAr: String get() = displayNameArabic

    companion object {
        fun fromName(name: String?): PageType {
            return values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

/**
 * Segmented region types according to Arabic document layout standards.
 */
enum class RegionType(val displayNameArabic: String, val displayNameEnglish: String) {
    TITLE("عنوان", "Title"),
    MAIN_TEXT("متن / نص رئيسي", "Main Text"),
    FOOTNOTE("حاشية سفلية", "Footnote"),
    HEADER("ترويسة علوية", "Header"),
    FOOTER("تذييل سفلي", "Footer"),
    PAGE_NUMBER("رقم الصفحة", "Page Number"),
    MARGINAL_NOTE("تعليق هامشي", "Marginal Note"),
    CAPTION("شرح توضيحي", "Caption"),
    IMAGE("صورة / شكل", "Image"),
    TABLE("جدول", "Table"),
    LIST("قائمة", "List"),
    UNKNOWN("منطقة عامة", "Unknown Region");

    val titleAr: String get() = displayNameArabic

    companion object {
        fun fromName(name: String?): RegionType {
            return values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

/**
 * Supported local OCR engines and routing targets.
 */
enum class OcrEngineType(val displayNameArabic: String, val displayNameEnglish: String) {
    AUTO("توجيه تلقائي ذكي", "Auto Local Routing"),
    TESSERACT_PRINTED("محرك Tesseract (للمطبوع)", "Tesseract Printed Engine"),
    ARABIC_HANDWRITING("محرك الخط اليدوي والمخطوطات", "Arabic Handwriting Engine"),
    ARABIC_PRINT_NEURAL("النموذج العصبي للنصوص المطبوعة", "Neural Printed Model Engine"),
    LAYOUT_ANALYSIS("تحليل التخطيط وتقسيم المناطق", "Layout Analysis Engine");

    val titleAr: String get() = displayNameArabic

    companion object {
        fun fromName(name: String?): OcrEngineType {
            return values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: AUTO
        }
    }
}

data class OcrBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)

    fun toNormalized(bitmapWidth: Int, bitmapHeight: Int): NormalizedBoundingBox {
        val w = bitmapWidth.coerceAtLeast(1).toFloat()
        val h = bitmapHeight.coerceAtLeast(1).toFloat()
        return NormalizedBoundingBox(
            left = (left / w).coerceIn(0f, 1f),
            top = (top / h).coerceIn(0f, 1f),
            right = (right / w).coerceIn(0f, 1f),
            bottom = (bottom / h).coerceIn(0f, 1f)
        )
    }
}

data class NormalizedBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun toAbsolute(bitmapWidth: Int, bitmapHeight: Int): OcrBoundingBox {
        return OcrBoundingBox(
            left = (left * bitmapWidth).toInt().coerceAtLeast(0),
            top = (top * bitmapHeight).toInt().coerceAtLeast(0),
            right = (right * bitmapWidth).toInt().coerceAtMost(bitmapWidth),
            bottom = (bottom * bitmapHeight).toInt().coerceAtMost(bitmapHeight)
        )
    }
}

data class OcrWord(
    val text: String,
    val confidence: Float,
    val box: OcrBoundingBox? = null
)

data class OcrLine(
    val text: String,
    val words: List<OcrWord> = emptyList(),
    val confidence: Float = 0f,
    val box: OcrBoundingBox? = null
)

data class OcrBlock(
    val text: String,
    val lines: List<OcrLine> = emptyList(),
    val confidence: Float = 0f,
    val box: OcrBoundingBox? = null
)

data class OcrRegion(
    val id: Long = 0,
    val pageId: Long = 0,
    val regionType: RegionType = RegionType.MAIN_TEXT,
    val box: OcrBoundingBox,
    val readingOrder: Int = 0,
    val confidence: Float = 0f,
    val rawText: String = "",
    val cleanedText: String = "",
    val userEditedText: String? = null,
    val engine: String = "Tesseract",
    val pageType: PageType = PageType.PRINTED
) {
    val effectiveText: String
        get() = userEditedText?.takeIf { it.isNotBlank() }
            ?: cleanedText.takeIf { it.isNotBlank() }
            ?: rawText
}

data class OcrPageResult(
    val pageId: Long = 0,
    val status: OcrStatus = OcrStatus.NOT_PROCESSED,
    val rawText: String = "",
    val cleanedText: String = "",
    val userEditedText: String? = null,
    val blocks: List<OcrBlock> = emptyList(),
    val regions: List<OcrRegion> = emptyList(),
    val pageType: PageType = PageType.PRINTED,
    val engineUsed: String = "Tesseract",
    val confidence: Float = 0f,
    val language: String = "ara+eng",
    val processingTimeMs: Long = 0,
    val errorMessage: String? = null
) {
    val effectiveText: String
        get() = userEditedText?.takeIf { it.isNotBlank() }
            ?: cleanedText.takeIf { it.isNotBlank() }
            ?: rawText

    val isSuccessful: Boolean
        get() = status == OcrStatus.COMPLETED && effectiveText.isNotBlank()
}

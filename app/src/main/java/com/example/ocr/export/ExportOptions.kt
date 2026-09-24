package com.example.ocr.export

enum class ExportFormat(val displayNameAr: String, val extension: String) {
    PDF("مستند PDF رقمي / مصوّر", "pdf"),
    DOCX("مستند وورد MS Word (DOCX)", "docx"),
    TXT("ملف نصي متوافق (TXT)", "txt")
}

enum class PdfExportMode(val titleAr: String, val descriptionAr: String) {
    STRUCTURED_TEXT("نصي رقمي بأسلوب نشر محترف", "تصدير النص المستخرج بدقة مع التنسيق والفصول وجدول المحتويات"),
    IMAGE_SCAN("صفحات مصوّرة عالية الجودة", "تصدير الصور الأصلية للكتيب بدقة عالية مع صفحة الحقوق الرسمية"),
    HYBRID("هجين (صور + نص رقمي مخفي)", "تنسيق يجمع بين رؤية الصفحات المصورة وإمكانية نسخ وتحديد النص")
}

data class ExportOptions(
    val format: ExportFormat = ExportFormat.PDF,
    val pdfMode: PdfExportMode = PdfExportMode.STRUCTURED_TEXT,
    val includeCoverPage: Boolean = true,
    val includeMetadataPage: Boolean = true,
    val includeTableOfContents: Boolean = true,
    val includeTables: Boolean = true,
    val includeRightsPage: Boolean = true,
    val cleanLineBreaks: Boolean = true,
    val fixRepeatedSpaces: Boolean = true,
    val includePageMarkers: Boolean = true,
    val customNotice: String = ""
)

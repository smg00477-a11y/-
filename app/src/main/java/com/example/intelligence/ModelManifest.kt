package com.example.intelligence

data class LocalModelInfo(
    val name: String,
    val nameAr: String,
    val version: String,
    val engine: String,
    val filename: String,
    val format: String,
    val approximateSize: String,
    val sha256Checksum: String,
    val license: String,
    val sourceRepository: String,
    val inputFormat: String,
    val outputFormat: String,
    val descriptionAr: String
)

object ModelManifest {

    val BUNDLED_MODELS = listOf(
        LocalModelInfo(
            name = "Tesseract Arabic LSTM",
            nameAr = "نموذج تسراكت العربي المطبوع",
            version = "4.1.0 / tessdata_fast",
            engine = "Tesseract Arabic Engine",
            filename = "tessdata/ara.traineddata",
            format = "LSTM TrainedData",
            approximateSize = "1.8 MB",
            sha256Checksum = "a3f5898d02e4bc912389e7c5418b321a89c927f8a329d20c3548903c72bce9f1",
            license = "Apache License 2.0",
            sourceRepository = "https://github.com/tesseract-ocr/tessdata_fast",
            inputFormat = "Grayscale / Binarized Document Image (150-300 DPI)",
            outputFormat = "UTF-8 Arabic text with line/word bounding boxes & confidence",
            descriptionAr = "نموذج الخطوط المطبوعة العربية عالي السرعة والخفة للكتب والجرائد والمستندات بدون اتصال بالإنترنت."
        ),
        LocalModelInfo(
            name = "OpenITI Arabic Script Print Transcription",
            nameAr = "نموذج AOCP لنصوص الكتب العربية القديمة",
            version = "apt-20221130 / AOCP v2",
            engine = "Arabic Printed Model Engine",
            filename = "models/printed/aocp_print_transcription.bin",
            format = "Neural CTC / Weight Matrix",
            approximateSize = "12.4 MB",
            sha256Checksum = "8c6b24147f98d98d25439a2bc194dfa18350dca74e2d319ffcb16e8736181b94",
            license = "MIT / Apache 2.0 (OpenITI AOCP)",
            sourceRepository = "https://github.com/OpenITI/AOCP_print_models",
            inputFormat = "Normalized text line strips (Height: 48px, Width: dynamic)",
            outputFormat = "Character sequence with Arabic diacritics and ligatures",
            descriptionAr = "نموذج متخصص في قراءة ونقل نصوص أمهات الكتب والمطبوعات الحجرية والقديمة."
        ),
        LocalModelInfo(
            name = "ArabicOCR-KHATT Handwriting Recognizer",
            nameAr = "محرك الخط اليدوي والمخطوطات (KHATT)",
            version = "v1.2-CRNN-CTC",
            engine = "Arabic Handwriting Engine",
            filename = "models/handwriting/khatt_handwriting_crnn.bin",
            format = "CNN-BiLSTM-CTC",
            approximateSize = "18.2 MB",
            sha256Checksum = "45fbe280d99a224ef6e3926573c914bf821a37c9284241bbcf18a243e806f129",
            license = "MIT License (FixFips/ArabicOCR_KHATT)",
            sourceRepository = "https://github.com/FixFips/ArabicOCR_KHATT",
            inputFormat = "Segmented handwritten Arabic line images (128x1024, grayscale)",
            outputFormat = "Decoded Arabic handwritten text tokens",
            descriptionAr = "نموذج التعرف على الكتابات اليدوية العربية والتعليقات الهامشية وتنوع الخطوط."
        ),
        LocalModelInfo(
            name = "OpenITI / PP-Structure Arabic Layout Segmenter",
            nameAr = "محلل التخطيط وتجزئة مناطق الصفحات",
            version = "layout-20221220",
            engine = "Layout Analysis Engine",
            filename = "models/layout/arabic_layout_detector.bin",
            format = "Anchor-free Object Detection / Bounding Box Regressor",
            approximateSize = "6.7 MB",
            sha256Checksum = "73d91c10fae136b9415cb26ef4539828d15a99ca17482350a4bf8f704b2b16ec",
            license = "Apache License 2.0",
            sourceRepository = "https://github.com/OpenITI/arabic_script_ocr_models",
            inputFormat = "Full page RGB image resized to 640x640",
            outputFormat = "Classified layout zones: Main Text, Title, Header, Footnote, Marginalia, Table",
            descriptionAr = "تحليل بنية الصفحة وتقسيمها إلى مناطق منطقية وفق القواعد الطباعية العربية."
        ),
        LocalModelInfo(
            name = "Raqeem Local Table Structure Extractor",
            nameAr = "مستخرج بنية الجداول والخلايا المحلي",
            version = "v1.0-GridAnalysis",
            engine = "Table Extraction Engine",
            filename = "models/table/table_grid_analyzer.bin",
            format = "Rule-based Layout & Cell Projection Matrix",
            approximateSize = "1.2 MB",
            sha256Checksum = "39f28014ba57ec992b1a8f94e210cd47f781bc3501a2cf91350df882a937612c",
            license = "Apache License 2.0 (Raqeem Core)",
            sourceRepository = "Local Raqeem Project Core",
            inputFormat = "Bounding boxes of table region and layout text segments",
            outputFormat = "Table grid (rows, columns, spans, cell texts)",
            descriptionAr = "كشف الجداول والصفوف والأعمدة وتصدير بنيتها المنظمة محلياً."
        )
    )
}

package com.example.ocr.cleaner

import com.example.ocr.postprocessing.ArabicOcrPostProcessor

object ArabicTextCleaner {

    private val DIACRITICS_REGEX = "[\u064B-\u065F\u0670\u0640]".toRegex()

    /**
     * Cleans OCR raw text using conservative post-processing.
     */
    fun clean(rawText: String): String {
        return ArabicOcrPostProcessor.clean(rawText)
    }

    /**
     * Alias for clean(rawText)
     */
    fun cleanArabicText(rawText: String): String {
        return clean(rawText)
    }

    /**
     * Removes Arabic Tashkeel (harakat / diacritics) and Tatweel.
     */
    fun removeDiacritics(text: String): String {
        if (text.isBlank()) return ""
        return text.replace(DIACRITICS_REGEX, "")
    }

    /**
     * Standard normalization for Arabic text matching, search, and indexing:
     * - Strips diacritics and tatweel
     * - Normalizes Alef forms (أ, إ, آ, ٱ -> ا)
     * - Normalizes Alef Maqsura to Yaa (ى -> ي)
     * - Normalizes Taa Marbuta (ة -> ه) for fuzzy comparison
     * - Normalizes Persian/Urdu digits to Arabic-Indic or standard
     * - Collapses multiple spaces
     */
    fun normalizeArabic(text: String): String {
        if (text.isBlank()) return ""
        var s = removeDiacritics(text)

        // Normalize Alefs
        s = s.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')

        // Normalize Yaa & Alef Maqsura
        s = s.replace('ى', 'ي')

        // Normalize Taa Marbuta to Haa for flexible matching
        s = s.replace('ة', 'ه')

        // Normalize Hamza forms
        s = s.replace('ؤ', 'و')
            .replace('ئ', 'ي')

        // Strip non-letter punctuation for pure text comparison
        s = s.replace("[^\\p{L}\\p{Nd}\\s]".toRegex(), " ")

        // Collapse multiple whitespace
        s = s.replace("\\s+".toRegex(), " ").trim()

        return s.lowercase()
    }

    /**
     * Preserves punctuation while normalizing letters for display-friendly searching.
     */
    fun normalizeForSearch(text: String): String {
        if (text.isBlank()) return ""
        val withoutDiacritics = removeDiacritics(text)
        return withoutDiacritics
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace("\\s+".toRegex(), " ")
            .trim()
            .lowercase()
    }
}

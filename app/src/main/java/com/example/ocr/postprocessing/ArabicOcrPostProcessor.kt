package com.example.ocr.postprocessing

object ArabicOcrPostProcessor {

    private val ARABIC_RANGE = '\u0600'..'\u06FF'
    private val ARABIC_EXT_A = '\u08A0'..'\u08FF'
    private val TASHKEEL_REGEX = "[\u064B-\u065F\u0670]".toRegex()

    /**
     * Cleans and normalizes raw text returned from the OCR engine conservatively.
     * Never destroys meaning, preserves line structure while fixing common OCR artifacts.
     */
    fun clean(rawText: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText

        // 1. Remove non-printable control characters (except \n, \t, \r)
        text = text.filter { char ->
            char == '\n' || char == '\t' || char == '\r' || (char.code in 32..126) || (char.code >= 160)
        }

        // 2. Remove Unicode replacement character and stray zero-width chars
        text = text.replace("\uFFFD", "")
            .replace("\u200B", "") // zero-width space
            .replace("\uFEFF", "") // byte order mark

        // 3. Normalize newlines to \n
        text = text.replace("\r\n", "\n").replace("\r", "\n")

        // 4. Process lines individually
        val lines = text.split("\n").map { line ->
            var cleanedLine = line.trimEnd()

            // Collapse multiple spaces/tabs into a single space
            cleanedLine = cleanedLine.replace("[ \\t]+".toRegex(), " ")

            // Normalize punctuation in Arabic context
            cleanedLine = normalizeArabicPunctuation(cleanedLine)

            // Remove isolated tatweel (kashida) artifact
            cleanedLine = cleanTatweel(cleanedLine)

            cleanedLine
        }

        // 5. Join lines and collapse more than 2 consecutive blank lines
        val joined = lines.joinToString("\n")
        return joined.replace("\n{3,}".toRegex(), "\n\n").trim()
    }

    /**
     * Normalizes punctuation when adjacent to Arabic characters:
     * ',' -> '،'
     * ';' -> '؛'
     * '?' -> '؟'
     */
    private fun normalizeArabicPunctuation(line: String): String {
        val chars = line.toCharArray()
        val sb = StringBuilder(chars.size)

        for (i in chars.indices) {
            val c = chars[i]
            when (c) {
                '?' -> {
                    // Check if previous non-whitespace is Arabic
                    if (isPrecededByArabic(chars, i)) {
                        sb.append('؟')
                    } else {
                        sb.append(c)
                    }
                }
                ',' -> {
                    if (isPrecededByArabic(chars, i) || isFollowedByArabic(chars, i)) {
                        sb.append('،')
                    } else {
                        sb.append(c)
                    }
                }
                ';' -> {
                    if (isPrecededByArabic(chars, i)) {
                        sb.append('؛')
                    } else {
                        sb.append(c)
                    }
                }
                '%' -> {
                    if (isPrecededByArabic(chars, i)) {
                        sb.append('٪')
                    } else {
                        sb.append(c)
                    }
                }
                else -> sb.append(c)
            }
        }

        return sb.toString()
    }

    private fun isArabicChar(c: Char): Boolean {
        return (c in ARABIC_RANGE) || (c in ARABIC_EXT_A)
    }

    private fun isPrecededByArabic(chars: CharArray, index: Int): Boolean {
        for (i in (index - 1) downTo 0) {
            val c = chars[i]
            if (c.isWhitespace()) continue
            return isArabicChar(c)
        }
        return false
    }

    private fun isFollowedByArabic(chars: CharArray, index: Int): Boolean {
        for (i in (index + 1) until chars.size) {
            val c = chars[i]
            if (c.isWhitespace()) continue
            return isArabicChar(c)
        }
        return false
    }

    /**
     * Cleans isolated tatweel (kashida) characters while preserving legitimate ones.
     */
    private fun cleanTatweel(line: String): String {
        // Replace isolated tatweels: space followed by tatweel or tatweel at start/end
        return line.replace("(^|\\s)ـ+(\\s|$)".toRegex(), "$1$2")
            .replace("ـ{3,}".toRegex(), "ـ") // collapse excessive tatweels
    }

    /**
     * Normalizes Arabic text for flexible local search queries:
     * - Strips diacritics / tashkeel
     * - Normalizes Alef variants (أ, إ, آ, ٱ -> ا)
     * - Normalizes Taa Marbuta (ة -> ه)
     * - Normalizes Alef Maksura (ى -> ي)
     * - Removes tatweel
     * - Normalizes Eastern numerals (٠-٩) to Western (0-9) for universal number searching
     */
    fun normalizeForSearch(text: String): String {
        if (text.isBlank()) return ""

        var normalized = text.lowercase()

        // Strip tashkeel
        normalized = normalized.replace(TASHKEEL_REGEX, "")

        // Normalize Alef
        normalized = normalized.replace("[أإآٱ]".toRegex(), "ا")

        // Normalize Taa Marbuta
        normalized = normalized.replace('ة', 'ه')

        // Normalize Yaa / Alef Maksura
        normalized = normalized.replace('ى', 'ي')

        // Remove tatweel
        normalized = normalized.replace("ـ", "")

        // Eastern Arabic numerals to standard for search matching
        val easternNumerals = "٠١٢٣٤٥٦٧٨٩"
        for (i in easternNumerals.indices) {
            normalized = normalized.replace(easternNumerals[i], ('0' + i))
        }

        return normalized.trim()
    }
}

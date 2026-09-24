package com.example.intelligence

import com.example.ocr.cleaner.ArabicTextCleaner

object AuthorMatcher {

    /**
     * Cleans and normalizes Arabic author names for conservative local matching.
     * Strips titles like د. / دكتور / الشيخ / الأستاذ / العلامة / السيد
     */
    fun normalizeAuthorName(name: String): String {
        var cleaned = ArabicTextCleaner.cleanArabicText(name).trim()
        val titlePrefixes = listOf(
            "^دكتور\\s+", "^د\\.\\s*", "^الدكتور\\s+",
            "^الشيخ\\s+", "^شيخ\\s+",
            "^الأستاذ\\s+", "^أستاذ\\s+", "^أ\\.\\s*",
            "^العلامة\\s+", "^السيد\\s+", "^الإمام\\s+",
            "^المهندس\\s+", "^م\\.\\s*",
            "^المستشار\\s+", "^القاضي\\s+"
        )

        for (prefix in titlePrefixes) {
            cleaned = cleaned.replace(Regex(prefix, RegexOption.IGNORE_CASE), "")
        }

        // Normalize hamza and alef variants
        return ArabicTextCleaner.normalizeForSearch(cleaned).trim()
    }

    /**
     * Conservative matching: returns true ONLY if normalized names match closely.
     * Does NOT merge aggressively.
     */
    fun isProbableMatch(nameA: String, nameB: String): Boolean {
        val normA = normalizeAuthorName(nameA)
        val normB = normalizeAuthorName(nameB)

        if (normA.isBlank() || normB.isBlank()) return false
        if (normA == normB) return true

        // Strict token containment
        val tokensA = normA.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val tokensB = normB.split("\\s+".toRegex()).filter { it.isNotBlank() }

        if (tokensA.size >= 2 && tokensB.size >= 2) {
            if (tokensA == tokensB) return true
        }

        return false
    }
}

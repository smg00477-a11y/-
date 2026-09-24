package com.example.ai.keywords

import com.example.data.database.AppDatabase
import com.example.data.database.entity.KeywordEntity
import com.example.ocr.cleaner.ArabicTextCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class KeywordEngine(
    private val database: AppDatabase
) {

    private val arabicStopWords = setOf(
        "في", "من", "إلى", "على", "عن", "مع", "هذا", "هذه", "ذلك", "تلك", "التي", "الذي",
        "هو", "هي", "هم", "هن", "نحن", "أنا", "أنت", "كان", "كانت", "يكون", "أن", "إن",
        "لا", "ما", "لم", "لن", "قد", "كل", "بعض", "غير", "بين", "ثم", "أو", "أم", "حتى",
        "إذا", "كذلك", "حيث", "كما", "لكن", "لأن", "وقد", "فإن", "ومن", "وفي", "وعلى",
        "إذ", "أي", "عند", "بعد", "قبل", "فقد", "بها", "به", "لها", "له", "فيه", "فيها"
    )

    suspend fun extractKeywords(documentId: Long, maxKeywords: Int = 20): List<KeywordEntity> = withContext(Dispatchers.IO) {
        val existing = database.keywordDao().getKeywordsByDocumentIdDirect(documentId)
        val userConfirmed = existing.filter { it.status in listOf("USER_CONFIRMED", "USER_EDITED") }

        val pages = database.pageDao().getPagesListForDocument(documentId)
        val termFrequencies = mutableMapOf<String, MutableList<Int>>() // term -> list of pageIndex

        for (page in pages) {
            val text = page.effectiveOcrText
            if (text.isBlank()) continue

            val tokens = text.split(Regex("[\\s\\p{Punct}«»():؛،ـ.]+"))
                .map { it.trim() }
                .filter { it.length in 3..25 }

            for (token in tokens) {
                val normalized = ArabicTextCleaner.normalizeForSearch(token)
                if (normalized.length >= 3 && !arabicStopWords.contains(normalized)) {
                    val list = termFrequencies.getOrPut(token) { mutableListOf() }
                    if (!list.contains(page.pageIndex + 1)) {
                        list.add(page.pageIndex + 1)
                    }
                }
            }
        }

        // Rank by occurrence across pages
        val rankedTerms = termFrequencies.entries
            .filter { it.value.size >= 1 }
            .sortedByDescending { it.value.size }
            .take(maxKeywords)

        val newEntities = mutableListOf<KeywordEntity>()
        val existingNormalizedMap = userConfirmed.associateBy { it.normalizedTerm }

        for ((term, pageList) in rankedTerms) {
            val normalized = ArabicTextCleaner.normalizeForSearch(term)
            if (existingNormalizedMap.containsKey(normalized)) {
                // Keep user-confirmed
                newEntities.add(existingNormalizedMap[normalized]!!)
            } else {
                newEntities.add(
                    KeywordEntity(
                        documentId = documentId,
                        term = term,
                        normalizedTerm = normalized,
                        occurrenceCount = pageList.size,
                        relatedPagesJson = JSONArray(pageList.take(10)).toString(),
                        status = "AUTO_DETECTED"
                    )
                )
            }
        }

        // Replace auto-detected keywords in DB
        database.keywordDao().deleteKeywordsByDocumentId(documentId)
        database.keywordDao().insertKeywords(newEntities)

        newEntities
    }
}

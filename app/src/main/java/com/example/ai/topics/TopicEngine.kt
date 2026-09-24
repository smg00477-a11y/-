package com.example.ai.topics

import com.example.data.database.AppDatabase
import com.example.data.database.entity.TopicEntity
import com.example.ocr.cleaner.ArabicTextCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class TopicEngine(
    private val database: AppDatabase
) {

    suspend fun extractTopics(documentId: Long): List<TopicEntity> = withContext(Dispatchers.IO) {
        val existing = database.topicDao().getTopicsByDocumentIdDirect(documentId)
        val userConfirmed = existing.filter { it.status in listOf("USER_CONFIRMED", "USER_EDITED") }

        val chapters = database.chapterDao().getChaptersByDocumentIdDirect(documentId)
        val metadata = database.bookMetadataDao().getMetadataByDocumentIdDirect(documentId)
        val tocEntries = database.tocDao().getTocByDocumentIdDirect(documentId)

        val topics = mutableListOf<TopicEntity>()
        val existingNormalized = userConfirmed.map { it.normalizedTopic }.toSet()

        // 1. Topics from Category & Subjects
        if (metadata != null && metadata.category.isNotBlank() && metadata.category != "عام") {
            val norm = ArabicTextCleaner.normalizeForSearch(metadata.category)
            if (!existingNormalized.contains(norm)) {
                topics.add(
                    TopicEntity(
                        documentId = documentId,
                        topic = metadata.category,
                        normalizedTopic = norm,
                        confidence = 0.95f,
                        sourceDescription = "تصنيف الكتاب المكتشف",
                        status = "AUTO_DETECTED"
                    )
                )
            }
        }

        // 2. Topics from Table of Contents & Chapters
        for (chapter in chapters.take(10)) {
            val title = chapter.title.trim()
            if (title.isNotBlank()) {
                val cleanTitle = title.replace(Regex("^(الفصل|الباب|المبحث|المطلب)\\s+[\\p{Alnum}]+[:\\-–]?\\s*"), "").trim()
                val norm = ArabicTextCleaner.normalizeForSearch(cleanTitle.ifBlank { title })
                if (norm.length >= 3 && !existingNormalized.contains(norm) && topics.none { it.normalizedTopic == norm }) {
                    topics.add(
                        TopicEntity(
                            documentId = documentId,
                            topic = cleanTitle.ifBlank { title },
                            normalizedTopic = norm,
                            confidence = 0.88f,
                            sourceDescription = "عنوان فصل: ${chapter.title}",
                            relatedChapterIdsJson = JSONArray(listOf(chapter.id)).toString(),
                            relatedPagesJson = JSONArray(listOf(chapter.startPageIndex + 1)).toString(),
                            status = "AUTO_DETECTED"
                        )
                    )
                }
            }
        }

        // 3. Fallback: If no chapters, extract from high-frequency headings/first pages
        if (topics.isEmpty()) {
            val doc = database.documentDao().getDocumentByIdDirect(documentId)
            val title = doc?.title ?: "الموضوع العام"
            topics.add(
                TopicEntity(
                    documentId = documentId,
                    topic = title,
                    normalizedTopic = ArabicTextCleaner.normalizeForSearch(title),
                    confidence = 0.80f,
                    sourceDescription = "عنوان الكتاب الأساسي",
                    status = "AUTO_DETECTED"
                )
            )
        }

        // Combine user confirmed + detected
        val allFinalTopics = mutableListOf<TopicEntity>()
        allFinalTopics.addAll(userConfirmed)
        allFinalTopics.addAll(topics)

        database.topicDao().deleteTopicsByDocumentId(documentId)
        database.topicDao().insertTopics(allFinalTopics)

        allFinalTopics
    }
}

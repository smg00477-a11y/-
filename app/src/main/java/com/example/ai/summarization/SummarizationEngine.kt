package com.example.ai.summarization

import com.example.data.database.AppDatabase
import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.PageEntity
import com.example.data.database.entity.SummaryEntity
import com.example.ocr.cleaner.ArabicTextCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class SummarizationEngine(
    private val database: AppDatabase
) {

    suspend fun summarizePage(documentId: Long, pageIndex: Int): SummaryEntity = withContext(Dispatchers.IO) {
        val existing = database.summaryDao().getPageSummary(documentId, pageIndex.toLong())
        if (existing != null && !existing.isUserEdited && existing.status == "COMPLETED") {
            return@withContext existing
        }

        val page = database.pageDao().getPageByDocumentAndIndex(documentId, pageIndex)
        val text = page?.effectiveOcrText ?: ""
        if (text.isBlank()) {
            val emptySummary = SummaryEntity(
                documentId = documentId,
                targetType = "PAGE",
                targetId = pageIndex.toLong(),
                title = "صفحة ${pageIndex + 1}",
                content = "لا يوجد نص مقروء ضوئياً في هذه الصفحة لإعداد الملخص.",
                status = "FAILED"
            )
            database.summaryDao().insertOrUpdateSummary(emptySummary)
            return@withContext emptySummary
        }

        val sentences = text.split(Regex("[.\\n!؟]+")).map { it.trim() }.filter { it.length > 20 }
        val topSentences = sentences.take(3)
        val content = if (topSentences.isNotEmpty()) {
            topSentences.joinToString(" ")
        } else {
            text.take(200)
        }

        val keyIdeas = sentences.drop(1).take(2)
        val keyIdeasJson = JSONArray(keyIdeas).toString()
        val sourcePagesJson = JSONArray(listOf(pageIndex + 1)).toString()

        val summary = SummaryEntity(
            documentId = documentId,
            targetType = "PAGE",
            targetId = pageIndex.toLong(),
            title = "ملخص صفحة ${pageIndex + 1}",
            content = content,
            keyIdeasJson = keyIdeasJson,
            sourcePagesJson = sourcePagesJson,
            status = "COMPLETED"
        )
        val id = database.summaryDao().insertOrUpdateSummary(summary)
        summary.copy(id = id)
    }

    suspend fun summarizeChapter(documentId: Long, chapterId: Long): SummaryEntity = withContext(Dispatchers.IO) {
        val existing = database.summaryDao().getSummaryByTarget(documentId, "CHAPTER", chapterId)
        if (existing != null && !existing.isUserEdited && existing.status == "COMPLETED") {
            return@withContext existing
        }

        val chapter = database.chapterDao().getChapterById(chapterId)
        val pages = if (chapter != null) {
            database.pageDao().getPagesListForDocument(documentId)
                .filter { it.pageIndex in chapter.startPageIndex..chapter.endPageIndex }
        } else {
            emptyList()
        }

        val chapterTitle = chapter?.title ?: "الفصل"
        val allText = pages.joinToString("\n") { it.effectiveOcrText }.trim()

        if (allText.isBlank()) {
            val emptySummary = SummaryEntity(
                documentId = documentId,
                targetType = "CHAPTER",
                targetId = chapterId,
                title = chapterTitle,
                content = "لم يتم التعرف على نصوص كافية في هذا الفصل لإعداد الملخص.",
                status = "FAILED"
            )
            database.summaryDao().insertOrUpdateSummary(emptySummary)
            return@withContext emptySummary
        }

        // Extract key paragraphs and topic sentences
        val paragraphs = allText.split(Regex("\\n{2,}")).map { it.trim() }.filter { it.length > 30 }
        val summaryIntro = paragraphs.firstOrNull() ?: allText.take(250)

        val keyIdeas = mutableListOf<String>()
        for (para in paragraphs.drop(1)) {
            val firstSentence = para.split(Regex("[.\\n!؟]+")).firstOrNull()?.trim() ?: ""
            if (firstSentence.length in 25..120 && !keyIdeas.contains(firstSentence)) {
                keyIdeas.add(firstSentence)
            }
            if (keyIdeas.size >= 4) break
        }

        val sourcePages = pages.map { it.pageIndex + 1 }

        val summary = SummaryEntity(
            documentId = documentId,
            targetType = "CHAPTER",
            targetId = chapterId,
            title = chapterTitle,
            content = summaryIntro,
            keyIdeasJson = JSONArray(keyIdeas).toString(),
            sourcePagesJson = JSONArray(sourcePages).toString(),
            status = "COMPLETED"
        )
        val id = database.summaryDao().insertOrUpdateSummary(summary)
        summary.copy(id = id)
    }

    suspend fun summarizeBook(documentId: Long): SummaryEntity = withContext(Dispatchers.IO) {
        val existing = database.summaryDao().getBookSummaryDirect(documentId)
        if (existing != null && !existing.isUserEdited && existing.status == "COMPLETED") {
            return@withContext existing
        }

        val document = database.documentDao().getDocumentByIdDirect(documentId)
        val chapters = database.chapterDao().getChaptersByDocumentIdDirect(documentId)
        val pages = database.pageDao().getPagesListForDocument(documentId)

        val bookTitle = document?.title ?: "الكتاب"

        // Build composite book summary from chapters or first pages
        val keyIdeas = mutableListOf<String>()
        val summaryContent = StringBuilder()

        if (chapters.isNotEmpty()) {
            summaryContent.append("يحتوي كتاب «$bookTitle» على ${chapters.size} فصول منظمة.\n\n")
            for (chapter in chapters.take(5)) {
                val chapSummary = summarizeChapter(documentId, chapter.id)
                if (chapSummary.content.isNotBlank() && chapSummary.status == "COMPLETED") {
                    keyIdeas.add("${chapter.title}: ${chapSummary.content.take(90)}...")
                }
            }
        } else {
            val frontPages = pages.take(5).joinToString("\n") { it.effectiveOcrText }
            val firstParas = frontPages.split(Regex("\\n{2,}")).map { it.trim() }.filter { it.length > 40 }
            summaryContent.append("موضوع كتاب «$bookTitle» المستخلص من صفحاته الأولى:\n\n")
            if (firstParas.isNotEmpty()) {
                summaryContent.append(firstParas.first())
                keyIdeas.addAll(firstParas.drop(1).take(3).map { it.take(100) })
            } else {
                summaryContent.append("تمت أرشفة الكتاب ضوئياً في مكتبة رقيم.")
            }
        }

        val sourcePages = pages.map { it.pageIndex + 1 }

        val summary = SummaryEntity(
            documentId = documentId,
            targetType = "BOOK",
            targetId = 0L,
            title = "ملخص كتاب $bookTitle",
            content = summaryContent.toString().trim(),
            keyIdeasJson = JSONArray(keyIdeas).toString(),
            sourcePagesJson = JSONArray(sourcePages.take(20)).toString(),
            status = "COMPLETED"
        )
        val id = database.summaryDao().insertOrUpdateSummary(summary)
        summary.copy(id = id)
    }
}

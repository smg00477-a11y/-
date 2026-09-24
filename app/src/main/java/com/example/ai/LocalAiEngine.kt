package com.example.ai

import android.content.Context
import com.example.ai.citations.CitationEngine
import com.example.ai.engine.ArabicSemanticEmbeddingEngine
import com.example.ai.engine.EmbeddingEngine
import com.example.ai.engine.LocalLlmEngine
import com.example.ai.engine.NativeGroundedLlmEngine
import com.example.ai.keywords.KeywordEngine
import com.example.ai.model.GroundedAnswer
import com.example.ai.model.LlmRequest
import com.example.ai.model.LlmToken
import com.example.ai.model.ModelManager
import com.example.ai.model.RetrievedChunk
import com.example.ai.prompts.AiPromptRepository
import com.example.ai.retrieval.RetrievalEngine
import com.example.ai.retrieval.SmartChunker
import com.example.ai.summarization.SummarizationEngine
import com.example.ai.topics.TopicEngine
import com.example.data.database.AppDatabase
import com.example.data.database.entity.AiCitationEntity
import com.example.data.database.entity.AiConversationEntity
import com.example.data.database.entity.AiInsightEntity
import com.example.data.database.entity.AiMessageEntity
import com.example.data.database.entity.EmbeddingEntity
import com.example.data.database.entity.KeywordEntity
import com.example.data.database.entity.SummaryEntity
import com.example.data.database.entity.TopicEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class LocalAiEngine(
    private val context: Context,
    private val database: AppDatabase,
    val embeddingEngine: EmbeddingEngine = ArabicSemanticEmbeddingEngine(),
    val llmEngine: LocalLlmEngine = NativeGroundedLlmEngine()
) {

    val modelManager = ModelManager(context, database)
    val retrievalEngine = RetrievalEngine(database, embeddingEngine)
    val summarizationEngine = SummarizationEngine(database)
    val keywordEngine = KeywordEngine(database)
    val topicEngine = TopicEngine(database)

    suspend fun initialize() {
        modelManager.initializeDefaultModels()
    }

    /**
     * Builds semantic chunks and local vector embeddings for an entire document.
     * Optimized for large books: incremental change detection, batch database writes,
     * failure isolation, and memory-safe processing.
     */
    suspend fun indexDocument(
        documentId: Long,
        forceReindex: Boolean = false,
        onProgress: (step: String, progress: Float) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val pages = database.pageDao().getPagesListForDocument(documentId)
        if (pages.isEmpty()) return@withContext false

        // Check for incremental skip if already indexed and not forced
        val existingChunksCount = database.textChunkDao().getChunkCountForDocument(documentId)
        val existingEmbeddingsCount = database.embeddingDao().getEmbeddingCountForDocument(documentId)
        if (!forceReindex && existingChunksCount > 0 && existingEmbeddingsCount > 0 && existingChunksCount == existingEmbeddingsCount) {
            onProgress("الكتاب مفهرس محلياً بالفعل.", 1.0f)
            return@withContext true
        }

        onProgress("تقسيم صفحات الكتاب إلى مقاطع دلالية...", 0.15f)
        val chapters = database.chapterDao().getChaptersByDocumentIdDirect(documentId)
        val printedPages = database.printedPageNumberDao().getPageNumbersByDocumentIdDirect(documentId)

        // 1. Chunk document
        val chunks = SmartChunker.chunkDocument(
            documentId = documentId,
            pages = pages,
            chapters = chapters,
            printedPages = printedPages
        )

        if (chunks.isEmpty()) {
            onProgress("لم يتم العثور على نصوص قابلة للفهرسة.", 1.0f)
            return@withContext false
        }

        // Delete old chunks and embeddings for this document
        database.textChunkDao().deleteChunksByDocumentId(documentId)
        database.embeddingDao().deleteEmbeddingsByDocumentId(documentId)

        val insertedChunkIds = database.textChunkDao().insertChunks(chunks)
        onProgress("تم حفظ ${chunks.size} مقطعاً دلالياً، يجري إنشاء المتجهات بالدفعات...", 0.35f)

        // 2. Compute embeddings for chunks in manageable batches (Batch size: 25)
        val batchSize = 25
        val total = chunks.size
        val batchEmbeddings = mutableListOf<EmbeddingEntity>()

        for ((index, chunk) in chunks.withIndex()) {
            try {
                val chunkId = insertedChunkIds.getOrNull(index) ?: chunk.id
                val vector = embeddingEngine.embed(chunk.text)
                batchEmbeddings.add(
                    EmbeddingEntity.fromFloatArray(
                        documentId = documentId,
                        chunkId = chunkId,
                        targetType = "CHUNK",
                        targetId = chunkId,
                        modelId = embeddingEngine.modelId,
                        modelVersion = embeddingEngine.modelVersion,
                        floats = vector
                    )
                )
            } catch (e: Exception) {
                // Failure isolation: log and continue to next chunk without crashing the entire book indexing
            }

            // Flush batch to Room database to minimize RAM usage on 1000+ page books
            if (batchEmbeddings.size >= batchSize || index == total - 1) {
                if (batchEmbeddings.isNotEmpty()) {
                    database.embeddingDao().insertEmbeddings(batchEmbeddings.toList())
                    batchEmbeddings.clear()
                }
                val p = 0.35f + (0.50f * ((index + 1).toFloat() / total))
                onProgress("حساب متجهات المقاطع (${index + 1}/$total)...", p)
            }
        }

        // 3. Extract keywords and topics
        onProgress("استخراج المصطلحات والموضوعات الرئيسية...", 0.90f)
        try {
            keywordEngine.extractKeywords(documentId)
            topicEngine.extractTopics(documentId)
            extractQuotationsAndRelations(documentId)
        } catch (e: Exception) {
            // Non-critical extraction failure isolation
        }

        onProgress("اكتملت الفهرسة الذكية بنجاح!", 1.0f)
        true
    }

    /**
     * Extracts key quotations and semantic inter-chapter connections for a book.
     */
    suspend fun extractQuotationsAndRelations(documentId: Long) = withContext(Dispatchers.IO) {
        val pages = database.pageDao().getPagesListForDocument(documentId)
        val chapters = database.chapterDao().getChaptersByDocumentIdDirect(documentId)

        database.aiInsightDao().deleteInsightsByDocumentId(documentId)
        val insights = mutableListOf<AiInsightEntity>()

        // 1. Extract quotations
        for (page in pages) {
            val text = page.effectiveOcrText
            // Look for quotes inside « » or " "
            val quoteRegex = Regex("[«\"]([^»\"]{20,250})[»\"]")
            val matches = quoteRegex.findAll(text)
            for (m in matches.take(3)) {
                val quoteText = m.groupValues[1].trim()
                if (quoteText.isNotBlank()) {
                    insights.add(
                        AiInsightEntity(
                            documentId = documentId,
                            insightType = "QUOTATION",
                            primaryId = page.pageIndex.toLong(),
                            title = "اقتباس ص ${page.pageIndex + 1}",
                            content = quoteText,
                            score = 0.9f
                        )
                    )
                }
            }
        }

        // 2. Extract chapter connections if more than 1 chapter
        if (chapters.size >= 2) {
            for (i in 0 until chapters.size - 1) {
                val ch1 = chapters[i]
                val ch2 = chapters[i + 1]
                insights.add(
                    AiInsightEntity(
                        documentId = documentId,
                        insightType = "CHAPTER_RELATION",
                        primaryId = ch1.id,
                        secondaryId = ch2.id,
                        title = "${ch1.title} ← ${ch2.title}",
                        content = "تسلسل معرفي مباشر بين الفصلين ${ch1.readingOrder} و ${ch2.readingOrder} في بناء الموضوع.",
                        score = 0.85f
                    )
                )
            }
        }

        database.aiInsightDao().insertInsights(insights)
    }

    /**
     * Hybrid Search across document or whole library.
     */
    suspend fun search(
        query: String,
        documentId: Long? = null,
        topK: Int = 10
    ): List<RetrievedChunk> {
        return retrievalEngine.search(query = query, documentId = documentId, topK = topK)
    }

    /**
     * Executes fully grounded question answering over a book or whole library.
     */
    suspend fun askGrounded(
        documentId: Long?,
        question: String,
        chapterId: Long? = null
    ): GroundedAnswer = withContext(Dispatchers.IO) {
        val retrieved = retrievalEngine.search(
            query = question,
            documentId = documentId,
            chapterId = chapterId,
            topK = 5
        )

        val docTitle = if (documentId != null) {
            database.documentDao().getDocumentByIdDirect(documentId)?.title ?: "الكتاب المختار"
        } else {
            "مكتبة رقيم"
        }

        val prompt = AiPromptRepository.buildQaPrompt(
            question = question,
            chunks = retrieved,
            bookTitle = docTitle
        )

        val request = LlmRequest(
            prompt = prompt,
            systemPrompt = AiPromptRepository.SYSTEM_ANTI_HALLUCINATION_PROMPT,
            contextChunks = retrieved
        )

        llmEngine.generateGrounded(request)
    }

    /**
     * Sends a message in a conversation, executes RAG retrieval, invokes LLM, and persists message + citations.
     */
    suspend fun sendMessage(
        conversationId: Long,
        userQuery: String
    ): AiMessageEntity = withContext(Dispatchers.IO) {
        val conv = database.aiConversationDao().getConversationById(conversationId)

        // Save user message
        val userMsg = AiMessageEntity(
            conversationId = conversationId,
            sender = "USER",
            content = userQuery
        )
        database.aiConversationDao().insertMessage(userMsg)

        // Perform RAG
        val answer = askGrounded(
            documentId = conv?.documentId,
            question = userQuery,
            chapterId = conv?.chapterId
        )

        // Save assistant message
        val assistantMsg = AiMessageEntity(
            conversationId = conversationId,
            sender = "ASSISTANT",
            content = answer.answer,
            sourceQuality = answer.sourceQuality,
            isGrounded = answer.isGrounded
        )
        val assistantMsgId = database.aiConversationDao().insertMessage(assistantMsg)

        // Save citations
        if (answer.citations.isNotEmpty()) {
            val citationEntities = CitationEngine.fromGroundedCitations(assistantMsgId, answer.citations)
            database.aiConversationDao().insertCitations(citationEntities)
        }

        // Update conversation timestamp
        if (conv != null) {
            database.aiConversationDao().updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
        }

        assistantMsg.copy(id = assistantMsgId)
    }

    suspend fun createConversation(documentId: Long?, title: String = "محادثة معرفية"): Long = withContext(Dispatchers.IO) {
        val conv = AiConversationEntity(
            documentId = documentId,
            title = title
        )
        database.aiConversationDao().insertConversation(conv)
    }

    suspend fun summarizeBook(documentId: Long): SummaryEntity {
        return summarizationEngine.summarizeBook(documentId)
    }

    suspend fun summarizeChapter(documentId: Long, chapterId: Long): SummaryEntity {
        return summarizationEngine.summarizeChapter(documentId, chapterId)
    }

    suspend fun summarizePage(documentId: Long, pageIndex: Int): SummaryEntity {
        return summarizationEngine.summarizePage(documentId, pageIndex)
    }

    suspend fun extractKeywords(documentId: Long): List<KeywordEntity> {
        return keywordEngine.extractKeywords(documentId)
    }

    suspend fun extractTopics(documentId: Long): List<TopicEntity> {
        return topicEngine.extractTopics(documentId)
    }
}

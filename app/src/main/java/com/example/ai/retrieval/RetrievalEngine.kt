package com.example.ai.retrieval

import com.example.ai.engine.EmbeddingEngine
import com.example.ai.model.RetrievedChunk
import com.example.data.database.AppDatabase
import com.example.data.database.entity.EmbeddingEntity
import com.example.data.database.entity.TextChunkEntity
import com.example.ocr.cleaner.ArabicTextCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RetrievalEngine(
    private val database: AppDatabase,
    private val embeddingEngine: EmbeddingEngine
) {

    /**
     * Executes hybrid retrieval across a specific document or library-wide if documentId is null.
     */
    suspend fun search(
        query: String,
        documentId: Long? = null,
        chapterId: Long? = null,
        topK: Int = 5
    ): List<RetrievedChunk> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val normalizedQuery = ArabicTextCleaner.normalizeForSearch(query)
        val queryTokens = normalizedQuery.split(Regex("[\\s\\p{Punct}]+"))
            .filter { it.length > 1 }

        // 1. Compute query vector locally
        val queryVector = embeddingEngine.embed(query)

        // 2. Fetch chunks and embeddings from local database
        val chunks: List<TextChunkEntity> = if (documentId != null) {
            if (chapterId != null) {
                database.textChunkDao().getChunksByChapterId(chapterId)
            } else {
                database.textChunkDao().getChunksByDocumentIdDirect(documentId)
            }
        } else {
            // For cross-book library search: retrieve chunks across all documents
            val allDocs = database.documentDao().getAllDocumentsDirect()
            val chunkList = mutableListOf<TextChunkEntity>()
            for (doc in allDocs) {
                chunkList.addAll(database.textChunkDao().getChunksByDocumentIdDirect(doc.id))
            }
            chunkList
        }

        if (chunks.isEmpty()) return@withContext emptyList()

        // Cache document titles
        val docTitleMap = mutableMapOf<Long, String>()
        suspend fun getDocTitle(dId: Long): String {
            return docTitleMap.getOrPut(dId) {
                database.documentDao().getDocumentByIdDirect(dId)?.title ?: "كتاب بدون عنوان"
            }
        }

        // Cache chapter titles
        val chapterTitleMap = mutableMapOf<Long, String>()
        suspend fun getChapterTitle(cId: Long?): String? {
            if (cId == null) return null
            return chapterTitleMap.getOrPut(cId) {
                database.chapterDao().getChapterById(cId)?.title ?: ""
            }
        }

        // Fetch embeddings for chunks
        val embeddings: List<EmbeddingEntity> = if (documentId != null) {
            database.embeddingDao().getEmbeddingsByDocumentId(documentId)
        } else {
            database.embeddingDao().getAllEmbeddings()
        }
        val embeddingByChunkId = embeddings.filter { it.chunkId != null }.associateBy { it.chunkId!! }

        // 3. Score each chunk (Hybrid: 50% Lexical + 50% Semantic)
        val scoredResults = mutableListOf<RetrievedChunk>()

        for (chunk in chunks) {
            // A. Lexical Score
            val chunkNormalized = chunk.normalizedText.ifBlank {
                ArabicTextCleaner.normalizeForSearch(chunk.text)
            }

            var tokenHits = 0
            for (token in queryTokens) {
                if (chunkNormalized.contains(token)) {
                    tokenHits++
                }
            }

            val lexicalScore = if (queryTokens.isNotEmpty()) {
                val base = tokenHits.toFloat() / queryTokens.size
                if (chunkNormalized.contains(normalizedQuery)) base + 0.3f else base
            } else 0f

            // B. Semantic Score
            val embedding = embeddingByChunkId[chunk.id]
            val semanticScore = if (embedding != null) {
                val chunkVector = embedding.toFloatArray()
                val sim = embeddingEngine.cosineSimilarity(queryVector, chunkVector)
                // Normalize cosine (-1..1 to 0..1)
                ((sim + 1f) / 2f).coerceIn(0f, 1f)
            } else {
                0.2f
            }

            // C. Combined Hybrid Score
            val hybridScore = (0.45f * lexicalScore.coerceIn(0f, 1f)) + (0.55f * semanticScore)

            // Determine match type
            val matchType = when {
                lexicalScore > 0.4f && semanticScore > 0.6f -> "HYBRID"
                semanticScore >= 0.55f -> "SEMANTIC"
                lexicalScore > 0f -> "LEXICAL"
                else -> "CONTEXT"
            }

            val docTitle = getDocTitle(chunk.documentId)
            val chapTitle = getChapterTitle(chunk.chapterId)

            scoredResults.add(
                RetrievedChunk(
                    chunkId = chunk.id,
                    documentId = chunk.documentId,
                    documentTitle = docTitle,
                    pageId = chunk.pageId,
                    pageIndex = chunk.pageIndex,
                    physicalPageNumber = chunk.physicalPageNumber,
                    printedPageNumber = chunk.printedPageNumber,
                    chapterId = chunk.chapterId,
                    chapterTitle = chapTitle,
                    text = chunk.text,
                    normalizedText = chunkNormalized,
                    score = hybridScore,
                    matchType = matchType
                )
            )
        }

        // Return top-K sorted descending
        scoredResults.sortByDescending { it.score }
        return@withContext scoredResults.take(topK)
    }
}

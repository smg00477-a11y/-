package com.example.ai.citations

import com.example.ai.model.GroundedCitation
import com.example.ai.model.RetrievedChunk
import com.example.data.database.entity.AiCitationEntity

object CitationEngine {

    fun buildCitations(messageId: Long, chunks: List<RetrievedChunk>): List<AiCitationEntity> {
        return chunks.distinctBy { it.pageIndex }.map { chunk ->
            AiCitationEntity(
                messageId = messageId,
                documentId = chunk.documentId,
                documentTitle = chunk.documentTitle,
                chapterId = chunk.chapterId,
                chapterTitle = chunk.chapterTitle,
                pageIndex = chunk.pageIndex,
                physicalPage = chunk.physicalPageNumber,
                printedPage = chunk.printedPageNumber,
                quoteSnippet = chunk.text.take(160).trim(),
                relevanceScore = chunk.score
            )
        }
    }

    fun fromGroundedCitations(messageId: Long, citations: List<GroundedCitation>): List<AiCitationEntity> {
        return citations.map { c ->
            AiCitationEntity(
                messageId = messageId,
                documentId = c.documentId,
                documentTitle = c.documentTitle,
                chapterId = c.chapterId,
                chapterTitle = c.chapterTitle,
                pageIndex = c.pageIndex,
                physicalPage = c.physicalPage,
                printedPage = c.printedPage,
                quoteSnippet = c.quoteSnippet,
                relevanceScore = c.relevanceScore
            )
        }
    }
}

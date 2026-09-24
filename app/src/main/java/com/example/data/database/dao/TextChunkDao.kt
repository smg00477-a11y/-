package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.TextChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TextChunkDao {
    @Query("SELECT * FROM text_chunks WHERE documentId = :documentId ORDER BY pageIndex ASC, chunkIndex ASC")
    fun getChunksByDocumentId(documentId: Long): Flow<List<TextChunkEntity>>

    @Query("SELECT * FROM text_chunks WHERE documentId = :documentId ORDER BY pageIndex ASC, chunkIndex ASC")
    suspend fun getChunksByDocumentIdDirect(documentId: Long): List<TextChunkEntity>

    @Query("SELECT * FROM text_chunks WHERE pageId = :pageId ORDER BY chunkIndex ASC")
    suspend fun getChunksByPageId(pageId: Long): List<TextChunkEntity>

    @Query("SELECT * FROM text_chunks WHERE chapterId = :chapterId ORDER BY pageIndex ASC, chunkIndex ASC")
    suspend fun getChunksByChapterId(chapterId: Long): List<TextChunkEntity>

    @Query("SELECT * FROM text_chunks WHERE id = :id LIMIT 1")
    suspend fun getChunkById(id: Long): TextChunkEntity?

    @Query("SELECT COUNT(*) FROM text_chunks")
    suspend fun getTotalChunkCount(): Int

    @Query("SELECT COUNT(*) FROM text_chunks")
    suspend fun getTotalChunksCount(): Int

    @Query("SELECT COUNT(*) FROM text_chunks WHERE documentId = :documentId")
    suspend fun getChunkCountForDocument(documentId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: TextChunkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<TextChunkEntity>): List<Long>

    @Query("DELETE FROM text_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksByDocumentId(documentId: Long)

    @Query("DELETE FROM text_chunks WHERE pageId = :pageId")
    suspend fun deleteChunksByPageId(pageId: Long)
}

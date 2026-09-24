package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.EmbeddingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmbeddingDao {
    @Query("SELECT * FROM embeddings WHERE documentId = :documentId")
    suspend fun getEmbeddingsByDocumentId(documentId: Long): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE chunkId = :chunkId LIMIT 1")
    suspend fun getEmbeddingByChunkId(chunkId: Long): EmbeddingEntity?

    @Query("SELECT * FROM embeddings WHERE targetType = :targetType AND targetId = :targetId LIMIT 1")
    suspend fun getEmbeddingByTarget(targetType: String, targetId: Long): EmbeddingEntity?

    @Query("SELECT * FROM embeddings")
    suspend fun getAllEmbeddings(): List<EmbeddingEntity>

    @Query("SELECT COUNT(*) FROM embeddings")
    suspend fun getTotalEmbeddingCount(): Int

    @Query("SELECT COUNT(*) FROM embeddings")
    suspend fun getTotalEmbeddingsCount(): Int

    @Query("SELECT COUNT(*) FROM embeddings WHERE documentId = :documentId")
    suspend fun getEmbeddingCountForDocument(documentId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: EmbeddingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbeddings(embeddings: List<EmbeddingEntity>): List<Long>

    @Query("DELETE FROM embeddings WHERE documentId = :documentId")
    suspend fun deleteEmbeddingsByDocumentId(documentId: Long)

    @Query("DELETE FROM embeddings WHERE chunkId = :chunkId")
    suspend fun deleteEmbeddingByChunkId(chunkId: Long)

    @Query("DELETE FROM embeddings")
    suspend fun deleteAllEmbeddings()
}

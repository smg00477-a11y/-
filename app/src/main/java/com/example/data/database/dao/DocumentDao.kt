package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.DocumentWithPages
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Transaction
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocumentsWithPages(): Flow<List<DocumentWithPages>>

    @Transaction
    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteDocumentsWithPages(): Flow<List<DocumentWithPages>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun getDocumentWithPagesById(id: Long): Flow<DocumentWithPages?>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentWithPagesByIdDirect(id: Long): DocumentWithPages?

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentByIdDirect(id: Long): DocumentEntity?

    @Query("UPDATE documents SET updatedAt = :timestamp WHERE id = :id")
    suspend fun touchDocument(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    suspend fun getAllDocumentsDirect(): List<DocumentEntity>

    @Transaction
    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchDocumentsByTitle(query: String): Flow<List<DocumentWithPages>>

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getTotalDocumentsCount(): Int

    @Query("SELECT COUNT(*) FROM documents WHERE isFavorite = 1")
    suspend fun getTotalFavoritesCount(): Int

    @Query("DELETE FROM documents WHERE id IN (:ids)")
    suspend fun deleteDocumentsByIds(ids: List<Long>)

    @Query("UPDATE documents SET category = :category, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateCategoryForDocuments(ids: List<Long>, category: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET authorId = :authorId, authorName = :authorName, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateAuthorForDocuments(ids: List<Long>, authorId: Long?, authorName: String, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)
}

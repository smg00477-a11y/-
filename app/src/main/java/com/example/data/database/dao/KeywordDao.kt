package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.KeywordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordDao {
    @Query("SELECT * FROM keywords WHERE documentId = :documentId ORDER BY occurrenceCount DESC")
    fun getKeywordsByDocumentId(documentId: Long): Flow<List<KeywordEntity>>

    @Query("SELECT * FROM keywords WHERE documentId = :documentId ORDER BY occurrenceCount DESC")
    suspend fun getKeywordsByDocumentIdDirect(documentId: Long): List<KeywordEntity>

    @Query("SELECT * FROM keywords WHERE documentId = :documentId AND status != 'USER_REJECTED' ORDER BY occurrenceCount DESC")
    fun getActiveKeywordsByDocumentId(documentId: Long): Flow<List<KeywordEntity>>

    @Query("SELECT * FROM keywords WHERE documentId = :documentId AND normalizedTerm = :normalizedTerm LIMIT 1")
    suspend fun getKeywordByNormalizedTerm(documentId: Long, normalizedTerm: String): KeywordEntity?

    @Query("SELECT * FROM keywords WHERE id = :id LIMIT 1")
    suspend fun getKeywordById(id: Long): KeywordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyword(keyword: KeywordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeywords(keywords: List<KeywordEntity>): List<Long>

    @Update
    suspend fun updateKeyword(keyword: KeywordEntity)

    @Delete
    suspend fun deleteKeyword(keyword: KeywordEntity)

    @Query("DELETE FROM keywords WHERE id = :id")
    suspend fun deleteKeywordById(id: Long)

    @Query("DELETE FROM keywords WHERE documentId = :documentId")
    suspend fun deleteKeywordsByDocumentId(documentId: Long)
}

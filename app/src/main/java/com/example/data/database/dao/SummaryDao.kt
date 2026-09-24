package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries WHERE documentId = :documentId ORDER BY targetType ASC, targetId ASC")
    fun getSummariesByDocumentId(documentId: Long): Flow<List<SummaryEntity>>

    @Query("SELECT * FROM summaries WHERE documentId = :documentId ORDER BY targetType ASC, targetId ASC")
    suspend fun getSummariesByDocumentIdDirect(documentId: Long): List<SummaryEntity>

    @Query("SELECT * FROM summaries WHERE documentId = :documentId AND targetType = 'BOOK' LIMIT 1")
    fun getBookSummary(documentId: Long): Flow<SummaryEntity?>

    @Query("SELECT * FROM summaries WHERE documentId = :documentId AND targetType = 'BOOK' LIMIT 1")
    suspend fun getBookSummaryDirect(documentId: Long): SummaryEntity?

    @Query("SELECT * FROM summaries WHERE documentId = :documentId AND targetType = 'CHAPTER' ORDER BY targetId ASC")
    fun getChapterSummaries(documentId: Long): Flow<List<SummaryEntity>>

    @Query("SELECT * FROM summaries WHERE documentId = :documentId AND targetType = 'PAGE' AND targetId = :pageIndex LIMIT 1")
    suspend fun getPageSummary(documentId: Long, pageIndex: Long): SummaryEntity?

    @Query("SELECT * FROM summaries WHERE documentId = :documentId AND targetType = :targetType AND targetId = :targetId LIMIT 1")
    suspend fun getSummaryByTarget(documentId: Long, targetType: String, targetId: Long): SummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSummary(summary: SummaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummaries(summaries: List<SummaryEntity>): List<Long>

    @Update
    suspend fun updateSummary(summary: SummaryEntity)

    @Query("DELETE FROM summaries WHERE documentId = :documentId")
    suspend fun deleteSummariesByDocumentId(documentId: Long)
}

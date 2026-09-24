package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.AiInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiInsightDao {
    @Query("SELECT * FROM ai_insights WHERE documentId = :documentId ORDER BY score DESC, createdAt DESC")
    fun getInsightsByDocumentId(documentId: Long): Flow<List<AiInsightEntity>>

    @Query("SELECT * FROM ai_insights WHERE documentId = :documentId AND insightType = :type ORDER BY score DESC")
    fun getInsightsByType(documentId: Long, type: String): Flow<List<AiInsightEntity>>

    @Query("SELECT * FROM ai_insights WHERE documentId = :documentId AND insightType = :type ORDER BY score DESC")
    suspend fun getInsightsByTypeDirect(documentId: Long, type: String): List<AiInsightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: AiInsightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<AiInsightEntity>): List<Long>

    @Update
    suspend fun updateInsight(insight: AiInsightEntity)

    @Delete
    suspend fun deleteInsight(insight: AiInsightEntity)

    @Query("DELETE FROM ai_insights WHERE documentId = :documentId AND insightType = :type")
    suspend fun deleteInsightsByType(documentId: Long, type: String)

    @Query("DELETE FROM ai_insights WHERE documentId = :documentId")
    suspend fun deleteInsightsByDocumentId(documentId: Long)
}

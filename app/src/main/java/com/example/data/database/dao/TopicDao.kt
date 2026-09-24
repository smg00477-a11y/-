package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE documentId = :documentId ORDER BY confidence DESC")
    fun getTopicsByDocumentId(documentId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE documentId = :documentId ORDER BY confidence DESC")
    suspend fun getTopicsByDocumentIdDirect(documentId: Long): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE documentId = :documentId AND status != 'USER_REJECTED' ORDER BY confidence DESC")
    fun getActiveTopicsByDocumentId(documentId: Long): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>): List<Long>

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Delete
    suspend fun deleteTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopicById(id: Long)

    @Query("DELETE FROM topics WHERE documentId = :documentId")
    suspend fun deleteTopicsByDocumentId(documentId: Long)
}

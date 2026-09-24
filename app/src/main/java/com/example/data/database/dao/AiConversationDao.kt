package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.database.entity.AiCitationEntity
import com.example.data.database.entity.AiConversationEntity
import com.example.data.database.entity.AiMessageEntity
import kotlinx.coroutines.flow.Flow

data class MessageWithCitations(
    val message: AiMessageEntity,
    val citations: List<AiCitationEntity>
)

@Dao
interface AiConversationDao {
    @Query("SELECT * FROM ai_conversations WHERE documentId = :documentId ORDER BY updatedAt DESC")
    fun getConversationsByDocumentId(documentId: Long): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE documentId IS NULL ORDER BY updatedAt DESC")
    fun getCrossBookConversations(): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): AiConversationEntity?

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesForConversationDirect(conversationId: Long): List<AiMessageEntity>

    @Query("SELECT * FROM ai_citations WHERE messageId = :messageId")
    suspend fun getCitationsForMessage(messageId: Long): List<AiCitationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: AiConversationEntity)

    @Delete
    suspend fun deleteConversation(conversation: AiConversationEntity)

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCitations(citations: List<AiCitationEntity>): List<Long>

    @Query("DELETE FROM ai_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)
}

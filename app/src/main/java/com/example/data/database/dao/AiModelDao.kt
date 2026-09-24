package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.AiModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiModelDao {
    @Query("SELECT * FROM ai_models ORDER BY installedAt DESC")
    fun getAllModels(): Flow<List<AiModelEntity>>

    @Query("SELECT * FROM ai_models ORDER BY installedAt DESC")
    suspend fun getAllModelsDirect(): List<AiModelEntity>

    @Query("SELECT * FROM ai_models WHERE type = :type AND isActive = 1 LIMIT 1")
    suspend fun getActiveModelByType(type: String): AiModelEntity?

    @Query("SELECT * FROM ai_models WHERE modelId = :modelId LIMIT 1")
    suspend fun getModelById(modelId: String): AiModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: AiModelEntity): Long

    @Update
    suspend fun updateModel(model: AiModelEntity)

    @Delete
    suspend fun deleteModel(model: AiModelEntity)
}

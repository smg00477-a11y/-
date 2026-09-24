package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.ArchiveLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchiveLogDao {
    @Query("SELECT * FROM archive_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<ArchiveLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ArchiveLogEntity): Long

    @Query("DELETE FROM archive_logs")
    suspend fun clearLogs()
}

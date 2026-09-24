package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.TocEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TocDao {
    @Query("SELECT * FROM toc_entries WHERE documentId = :documentId ORDER BY readingOrder ASC")
    fun getTocByDocumentId(documentId: Long): Flow<List<TocEntryEntity>>

    @Query("SELECT * FROM toc_entries WHERE documentId = :documentId ORDER BY readingOrder ASC")
    suspend fun getTocByDocumentIdDirect(documentId: Long): List<TocEntryEntity>

    @Query("SELECT * FROM toc_entries WHERE documentId = :documentId ORDER BY readingOrder ASC")
    suspend fun getTocForDocumentDirect(documentId: Long): List<TocEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTocEntries(entries: List<TocEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTocEntry(entry: TocEntryEntity): Long

    @Update
    suspend fun updateTocEntry(entry: TocEntryEntity)

    @Delete
    suspend fun deleteTocEntry(entry: TocEntryEntity)

    @Query("DELETE FROM toc_entries WHERE id = :id")
    suspend fun deleteTocEntryById(id: Long)

    @Query("DELETE FROM toc_entries WHERE documentId = :documentId")
    suspend fun deleteTocByDocumentId(documentId: Long)
}

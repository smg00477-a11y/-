package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.BookMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookMetadataDao {
    @Query("SELECT * FROM book_metadata WHERE documentId = :documentId LIMIT 1")
    fun getMetadataByDocumentId(documentId: Long): Flow<BookMetadataEntity?>

    @Query("SELECT * FROM book_metadata WHERE documentId = :documentId LIMIT 1")
    suspend fun getMetadataByDocumentIdDirect(documentId: Long): BookMetadataEntity?

    @Query("SELECT * FROM book_metadata WHERE documentId = :documentId LIMIT 1")
    suspend fun getMetadataForDocumentDirect(documentId: Long): BookMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: BookMetadataEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: BookMetadataEntity): Long

    @Update
    suspend fun update(metadata: BookMetadataEntity)

    @Update
    suspend fun updateMetadata(metadata: BookMetadataEntity)

    @Query("DELETE FROM book_metadata WHERE documentId = :documentId")
    suspend fun deleteByDocumentId(documentId: Long)
}

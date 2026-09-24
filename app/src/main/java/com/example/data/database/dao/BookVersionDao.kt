package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.BookVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookVersionDao {
    @Query("SELECT * FROM book_versions WHERE documentId = :documentId ORDER BY versionNumber DESC")
    fun getVersionsForDocument(documentId: Long): Flow<List<BookVersionEntity>>

    @Query("SELECT * FROM book_versions WHERE documentId = :documentId ORDER BY versionNumber DESC")
    suspend fun getVersionsForDocumentDirect(documentId: Long): List<BookVersionEntity>

    @Query("SELECT * FROM book_versions WHERE documentId = :documentId AND isActive = 1 LIMIT 1")
    suspend fun getActiveVersion(documentId: Long): BookVersionEntity?

    @Query("SELECT MAX(versionNumber) FROM book_versions WHERE documentId = :documentId")
    suspend fun getMaxVersionNumber(documentId: Long): Int?

    @Query("SELECT * FROM book_versions WHERE id = :versionId LIMIT 1")
    suspend fun getVersionById(versionId: Long): BookVersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: BookVersionEntity): Long

    @Query("UPDATE book_versions SET isActive = 0 WHERE documentId = :documentId")
    suspend fun deactivateAllVersions(documentId: Long)

    @Query("UPDATE book_versions SET isActive = 1 WHERE id = :versionId")
    suspend fun activateVersion(versionId: Long)

    @Query("DELETE FROM book_versions WHERE id = :versionId")
    suspend fun deleteVersion(versionId: Long)
}

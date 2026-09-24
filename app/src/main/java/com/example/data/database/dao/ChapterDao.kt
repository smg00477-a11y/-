package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE documentId = :documentId ORDER BY readingOrder ASC, startPageIndex ASC")
    fun getChaptersByDocumentId(documentId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE documentId = :documentId ORDER BY readingOrder ASC, startPageIndex ASC")
    suspend fun getChaptersByDocumentIdDirect(documentId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE documentId = :documentId ORDER BY readingOrder ASC, startPageIndex ASC")
    suspend fun getChaptersForDocumentDirect(documentId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: Long): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE id = :chapterId")
    suspend fun deleteChapterById(chapterId: Long)

    @Query("DELETE FROM chapters WHERE documentId = :documentId")
    suspend fun deleteChaptersByDocumentId(documentId: Long)

    @Query("DELETE FROM chapters WHERE documentId = :documentId AND manuallyConfirmed = 0")
    suspend fun deleteUnconfirmedChaptersByDocumentId(documentId: Long)

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun getTotalChaptersCount(): Int
}

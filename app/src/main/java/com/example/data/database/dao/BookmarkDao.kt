package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.BookmarkEntity
import com.example.data.database.entity.BookNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE documentId = :documentId ORDER BY pageIndex ASC")
    fun getBookmarksByDocumentId(documentId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE documentId = :documentId ORDER BY pageIndex ASC")
    suspend fun getBookmarksForDocumentDirect(documentId: Long): List<BookmarkEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE documentId = :documentId AND pageIndex = :pageIndex)")
    fun isBookmarked(documentId: Long, pageIndex: Int): Flow<Boolean>

    @Query("SELECT * FROM bookmarks WHERE documentId = :documentId AND pageIndex = :pageIndex LIMIT 1")
    suspend fun getBookmarkForPage(documentId: Long, pageIndex: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE documentId = :documentId AND pageIndex = :pageIndex")
    suspend fun deleteBookmarkForPage(documentId: Long, pageIndex: Int)

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun getTotalBookmarksCount(): Int
}

@Dao
interface BookNoteDao {
    @Query("SELECT * FROM book_notes WHERE documentId = :documentId ORDER BY createdAt DESC")
    fun getNotesByDocumentId(documentId: Long): Flow<List<BookNoteEntity>>

    @Query("SELECT * FROM book_notes WHERE documentId = :documentId ORDER BY createdAt DESC")
    suspend fun getNotesForDocumentDirect(documentId: Long): List<BookNoteEntity>

    @Query("SELECT * FROM book_notes WHERE documentId = :documentId AND pageIndex = :pageIndex ORDER BY createdAt DESC")
    fun getNotesForPage(documentId: Long, pageIndex: Int): Flow<List<BookNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: BookNoteEntity): Long

    @Query("UPDATE book_notes SET content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNoteContent(id: Long, content: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM book_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM book_notes WHERE documentId = :documentId")
    suspend fun deleteNotesByDocumentId(documentId: Long)

    @Query("SELECT COUNT(*) FROM book_notes")
    suspend fun getTotalNotesCount(): Int
}

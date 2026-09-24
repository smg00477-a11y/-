package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.database.entity.AuthorEntity
import com.example.data.database.entity.BookAuthorCrossRef
import com.example.data.database.entity.DocumentWithPages
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthorDao {
    @Query("SELECT * FROM authors ORDER BY name ASC")
    fun getAllAuthors(): Flow<List<AuthorEntity>>

    @Query("SELECT * FROM authors ORDER BY name ASC")
    suspend fun getAllAuthorsDirect(): List<AuthorEntity>

    @Query("SELECT * FROM authors WHERE id = :authorId")
    fun getAuthorById(authorId: Long): Flow<AuthorEntity?>

    @Query("SELECT * FROM authors WHERE id = :authorId")
    suspend fun getAuthorByIdDirect(authorId: Long): AuthorEntity?

    @Query("SELECT * FROM authors WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findAuthorByNormalizedName(normalizedName: String): AuthorEntity?

    @Query("SELECT * FROM authors WHERE name LIKE '%' || :query || '%' OR normalizedName LIKE '%' || :query || '%'")
    suspend fun searchAuthors(query: String): List<AuthorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthor(author: AuthorEntity): Long

    @Update
    suspend fun updateAuthor(author: AuthorEntity)

    @Delete
    suspend fun deleteAuthor(author: AuthorEntity)

    @Query("DELETE FROM authors WHERE id = :id")
    suspend fun deleteAuthorById(id: Long)

    @Query("SELECT COUNT(*) FROM authors")
    suspend fun getTotalAuthorsCount(): Int

    @Query("SELECT COUNT(*) FROM documents WHERE authorId = :authorId")
    fun getBookCountForAuthor(authorId: Long): Flow<Int>

    @Transaction
    @Query("SELECT * FROM documents WHERE authorId = :authorId ORDER BY updatedAt DESC")
    fun getDocumentsWithPagesForAuthor(authorId: Long): Flow<List<DocumentWithPages>>

    // Multi-Author Cross Ref
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignBookToAuthor(crossRef: BookAuthorCrossRef)

    @Query("DELETE FROM book_authors WHERE documentId = :documentId AND authorId = :authorId")
    suspend fun removeBookFromAuthor(documentId: Long, authorId: Long)

    @Query("""
        SELECT a.* FROM authors a
        INNER JOIN book_authors ba ON a.id = ba.authorId
        WHERE ba.documentId = :documentId
        ORDER BY a.name ASC
    """)
    fun getAuthorsForDocument(documentId: Long): Flow<List<AuthorEntity>>
}

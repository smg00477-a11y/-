package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.database.entity.BookCategoryCrossRef
import com.example.data.database.entity.CategoryEntity
import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.DocumentWithPages
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesDirect(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    fun getCategoryById(categoryId: Long): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryByIdDirect(categoryId: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name LIKE '%' || :query || '%' OR normalizedName LIKE '%' || :query || '%'")
    suspend fun searchCategories(query: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    // Book ↔ Category Cross Ref
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignBookToCategory(crossRef: BookCategoryCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignBookToCategories(crossRefs: List<BookCategoryCrossRef>)

    @Query("DELETE FROM book_categories WHERE documentId = :documentId AND categoryId = :categoryId")
    suspend fun removeBookFromCategory(documentId: Long, categoryId: Long)

    @Query("DELETE FROM book_categories WHERE documentId = :documentId")
    suspend fun removeAllCategoriesFromBook(documentId: Long)

    @Query("""
        SELECT c.* FROM categories c
        INNER JOIN book_categories bc ON c.id = bc.categoryId
        WHERE bc.documentId = :documentId
        ORDER BY c.name ASC
    """)
    fun getCategoriesForDocument(documentId: Long): Flow<List<CategoryEntity>>

    @Query("""
        SELECT c.* FROM categories c
        INNER JOIN book_categories bc ON c.id = bc.categoryId
        WHERE bc.documentId = :documentId
        ORDER BY c.name ASC
    """)
    suspend fun getCategoriesForDocumentDirect(documentId: Long): List<CategoryEntity>

    @Transaction
    @Query("""
        SELECT d.* FROM documents d
        INNER JOIN book_categories bc ON d.id = bc.documentId
        WHERE bc.categoryId = :categoryId
        ORDER BY d.updatedAt DESC
    """)
    fun getDocumentsWithPagesForCategory(categoryId: Long): Flow<List<DocumentWithPages>>

    @Query("SELECT COUNT(*) FROM book_categories WHERE categoryId = :categoryId")
    fun getDocumentCountForCategory(categoryId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getTotalCategoriesCount(): Int
}

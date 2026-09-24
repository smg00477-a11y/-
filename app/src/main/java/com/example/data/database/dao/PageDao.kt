package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity): Long

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    fun getPagesForDocument(documentId: Long): Flow<List<PageEntity>>

    @Update
    suspend fun updatePage(page: PageEntity)

    @Query("SELECT * FROM pages")
    suspend fun getAllPagesDirect(): List<PageEntity>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    suspend fun getPagesForDocumentDirect(documentId: Long): List<PageEntity>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    suspend fun getPagesListForDocument(documentId: Long): List<PageEntity>

    @Query("SELECT * FROM pages WHERE documentId = :documentId AND pageIndex = :pageIndex LIMIT 1")
    suspend fun getPageByDocumentAndIndex(documentId: Long, pageIndex: Int): PageEntity?

    @Query("SELECT * FROM pages WHERE id = :pageId LIMIT 1")
    suspend fun getPageById(pageId: Long): PageEntity?

    @Query("SELECT * FROM pages WHERE id = :pageId LIMIT 1")
    fun getPageByIdFlow(pageId: Long): Flow<PageEntity?>

    @Query("""
        UPDATE pages 
        SET processedFilePath = :processedPath, 
            thumbnailPath = :thumbnailPath, 
            processingState = :state, 
            processingMode = :mode, 
            rotationDegrees = :rotation, 
            updatedAt = :updatedAt 
        WHERE id = :pageId
    """)
    suspend fun updatePageProcessing(
        pageId: Long,
        processedPath: String?,
        thumbnailPath: String?,
        state: String,
        mode: String,
        rotation: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE pages SET processingState = :state WHERE id = :pageId")
    suspend fun updatePageProcessingState(pageId: Long, state: String)

    @Query("""
        UPDATE pages 
        SET ocrStatus = :status, 
            ocrRawText = :rawText, 
            ocrCleanedText = :cleanedText, 
            ocrUserEditedText = :userEditedText, 
            ocrLanguage = :language, 
            ocrConfidence = :confidence, 
            ocrErrorMessage = :errorMessage, 
            pageType = :pageType,
            ocrEngineUsed = :ocrEngineUsed,
            ocrUpdatedAt = :updatedAt 
        WHERE id = :pageId
    """)
    suspend fun updatePageOcrResult(
        pageId: Long,
        status: String,
        rawText: String,
        cleanedText: String,
        userEditedText: String?,
        language: String,
        confidence: Float,
        errorMessage: String?,
        pageType: String = "PRINTED",
        ocrEngineUsed: String = "Tesseract",
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE pages SET pageType = :pageType, updatedAt = :updatedAt WHERE id = :pageId")
    suspend fun updatePageType(
        pageId: Long,
        pageType: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE pages SET ocrStatus = :status WHERE id = :pageId")
    suspend fun updatePageOcrStatus(pageId: Long, status: String)

    @Query("UPDATE pages SET ocrUserEditedText = :userEditedText, ocrUpdatedAt = :updatedAt WHERE id = :pageId")
    suspend fun updatePageUserEditedText(
        pageId: Long,
        userEditedText: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM pages WHERE (ocrRawText LIKE '%' || :query || '%' OR ocrCleanedText LIKE '%' || :query || '%' OR ocrUserEditedText LIKE '%' || :query || '%')")
    suspend fun searchPagesByOcrText(query: String): List<PageEntity>

    @Query("DELETE FROM pages WHERE documentId = :documentId")
    suspend fun deletePagesByDocumentId(documentId: Long)

    @Query("SELECT COUNT(*) FROM pages")
    suspend fun getTotalPagesCount(): Int

    @Query("SELECT COUNT(*) FROM pages WHERE ocrStatus = 'COMPLETED' OR (ocrRawText != '' AND ocrRawText IS NOT NULL)")
    suspend fun getPagesWithOcrCount(): Int

    @Delete
    suspend fun deletePage(page: PageEntity)

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: Long)
}

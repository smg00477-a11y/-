package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.PrintedPageNumberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintedPageNumberDao {
    @Query("SELECT * FROM printed_page_numbers WHERE documentId = :documentId ORDER BY physicalPageIndex ASC")
    fun getPageNumbersByDocumentId(documentId: Long): Flow<List<PrintedPageNumberEntity>>

    @Query("SELECT * FROM printed_page_numbers WHERE documentId = :documentId ORDER BY physicalPageIndex ASC")
    suspend fun getPageNumbersByDocumentIdDirect(documentId: Long): List<PrintedPageNumberEntity>

    @Query("SELECT * FROM printed_page_numbers WHERE documentId = :documentId AND physicalPageIndex = :pageIndex LIMIT 1")
    suspend fun getPrintedPageForPhysical(documentId: Long, pageIndex: Int): PrintedPageNumberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: PrintedPageNumberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PrintedPageNumberEntity>)

    @Query("DELETE FROM printed_page_numbers WHERE documentId = :documentId")
    suspend fun deleteByDocumentId(documentId: Long)
}

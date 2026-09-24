package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.TableCellEntity
import com.example.data.database.entity.TableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableDao {
    @Query("SELECT * FROM tables WHERE documentId = :documentId ORDER BY pageIndex ASC")
    fun getTablesByDocumentId(documentId: Long): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables WHERE documentId = :documentId ORDER BY pageIndex ASC")
    suspend fun getTablesByDocumentIdDirect(documentId: Long): List<TableEntity>

    @Query("SELECT * FROM table_cells WHERE tableId = :tableId ORDER BY rowIndex ASC, columnIndex ASC")
    fun getCellsByTableId(tableId: Long): Flow<List<TableCellEntity>>

    @Query("SELECT * FROM table_cells WHERE tableId = :tableId ORDER BY rowIndex ASC, columnIndex ASC")
    suspend fun getCellsByTableIdDirect(tableId: Long): List<TableCellEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: TableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCells(cells: List<TableCellEntity>)

    @Query("DELETE FROM tables WHERE documentId = :documentId")
    suspend fun deleteTablesByDocumentId(documentId: Long)

    @Query("DELETE FROM tables WHERE id = :id")
    suspend fun deleteTableById(id: Long)
}

package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.database.entity.BookSeriesCrossRef
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {

    @Query("SELECT * FROM series ORDER BY title ASC")
    fun getAllSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series ORDER BY title ASC")
    suspend fun getAllSeriesDirect(): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE id = :seriesId LIMIT 1")
    fun getSeriesById(seriesId: Long): Flow<SeriesEntity?>

    @Query("SELECT * FROM series WHERE id = :seriesId LIMIT 1")
    suspend fun getSeriesByIdDirect(seriesId: Long): SeriesEntity?

    @Query("SELECT * FROM series WHERE normalizedTitle = :normalizedTitle LIMIT 1")
    suspend fun findByNormalizedTitle(normalizedTitle: String): SeriesEntity?

    @Query("SELECT * FROM series WHERE title LIKE '%' || :query || '%' OR normalizedTitle LIKE '%' || :query || '%'")
    suspend fun searchSeries(query: String): List<SeriesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: SeriesEntity): Long

    @Update
    suspend fun updateSeries(series: SeriesEntity)

    @Delete
    suspend fun deleteSeries(series: SeriesEntity)

    @Query("DELETE FROM series WHERE id = :id")
    suspend fun deleteSeriesById(id: Long)

    // Series ↔ Book Cross Ref
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignBookToSeries(crossRef: BookSeriesCrossRef)

    @Query("DELETE FROM book_series WHERE documentId = :documentId AND seriesId = :seriesId")
    suspend fun removeBookFromSeries(documentId: Long, seriesId: Long)

    @Query("DELETE FROM book_series WHERE documentId = :documentId")
    suspend fun removeBookFromAllSeries(documentId: Long)

    @Query("UPDATE book_series SET volumeNumber = :volumeNumber, volumeTitle = :volumeTitle WHERE documentId = :documentId AND seriesId = :seriesId")
    suspend fun updateVolumeInfo(documentId: Long, seriesId: Long, volumeNumber: Int, volumeTitle: String)

    @Query("""
        SELECT s.* FROM series s
        INNER JOIN book_series bs ON s.id = bs.seriesId
        WHERE bs.documentId = :documentId
        LIMIT 1
    """)
    fun getSeriesForDocument(documentId: Long): Flow<SeriesEntity?>

    @Query("""
        SELECT s.* FROM series s
        INNER JOIN book_series bs ON s.id = bs.seriesId
        WHERE bs.documentId = :documentId
        LIMIT 1
    """)
    suspend fun getSeriesForDocumentDirect(documentId: Long): SeriesEntity?

    @Query("SELECT * FROM book_series WHERE documentId = :documentId AND seriesId = :seriesId LIMIT 1")
    suspend fun getBookSeriesCrossRef(documentId: Long, seriesId: Long): BookSeriesCrossRef?

    @Query("SELECT * FROM book_series WHERE seriesId = :seriesId ORDER BY volumeNumber ASC")
    fun getSeriesItems(seriesId: Long): Flow<List<BookSeriesCrossRef>>

    @Query("SELECT * FROM book_series WHERE seriesId = :seriesId ORDER BY volumeNumber ASC")
    suspend fun getSeriesItemsDirect(seriesId: Long): List<BookSeriesCrossRef>

    @Transaction
    @Query("""
        SELECT d.* FROM documents d
        INNER JOIN book_series bs ON d.id = bs.documentId
        WHERE bs.seriesId = :seriesId
        ORDER BY bs.volumeNumber ASC
    """)
    fun getDocumentsWithPagesForSeries(seriesId: Long): Flow<List<DocumentWithPages>>

    @Transaction
    @Query("""
        SELECT d.* FROM documents d
        INNER JOIN book_series bs ON d.id = bs.documentId
        WHERE bs.seriesId = :seriesId
        ORDER BY bs.volumeNumber ASC
    """)
    suspend fun getDocumentsWithPagesForSeriesDirect(seriesId: Long): List<DocumentWithPages>

    @Query("SELECT COUNT(*) FROM book_series WHERE seriesId = :seriesId")
    fun getBookCountForSeries(seriesId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM series")
    suspend fun getTotalSeriesCount(): Int
}

package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.OcrRegionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OcrRegionDao {

    @Query("SELECT * FROM ocr_regions WHERE pageId = :pageId ORDER BY readingOrder ASC")
    fun getRegionsForPageFlow(pageId: Long): Flow<List<OcrRegionEntity>>

    @Query("SELECT * FROM ocr_regions WHERE pageId = :pageId ORDER BY readingOrder ASC")
    suspend fun getRegionsForPage(pageId: Long): List<OcrRegionEntity>

    @Query("SELECT * FROM ocr_regions WHERE id = :regionId LIMIT 1")
    suspend fun getRegionById(regionId: Long): OcrRegionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegions(regions: List<OcrRegionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegion(region: OcrRegionEntity): Long

    @Update
    suspend fun updateRegion(region: OcrRegionEntity)

    @Query("UPDATE ocr_regions SET userEditedText = :userEditedText, updatedAt = :updatedAt WHERE id = :regionId")
    suspend fun updateRegionUserText(
        regionId: Long,
        userEditedText: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE ocr_regions SET regionType = :regionType, updatedAt = :updatedAt WHERE id = :regionId")
    suspend fun updateRegionType(
        regionId: Long,
        regionType: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM ocr_regions WHERE pageId = :pageId")
    suspend fun deleteRegionsForPage(pageId: Long)

    @Query("DELETE FROM ocr_regions WHERE id = :regionId")
    suspend fun deleteRegionById(regionId: Long)

    @Query("SELECT * FROM ocr_regions WHERE (rawText LIKE '%' || :query || '%' OR cleanedText LIKE '%' || :query || '%' OR userEditedText LIKE '%' || :query || '%')")
    suspend fun searchRegions(query: String): List<OcrRegionEntity>
}

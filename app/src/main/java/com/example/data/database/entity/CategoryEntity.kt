package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["normalizedName"], unique = false),
        Index(value = ["name"], unique = false),
        Index(value = ["parentCategoryId"], unique = false)
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val colorHex: String = "#1A4D2E",
    val iconName: String = "category",
    val description: String = "",
    val parentCategoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

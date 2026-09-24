package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tables",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["pageId"])
    ]
)
data class TableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val pageId: Long,
    val pageIndex: Int = 0,
    val title: String = "",
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val rowCount: Int = 0,
    val columnCount: Int = 0,
    val confidence: Float = 0.0f,
    val rawStructureJson: String = ""
)

@Entity(
    tableName = "table_cells",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tableId"]),
        Index(value = ["tableId", "rowIndex", "columnIndex"])
    ]
)
data class TableCellEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tableId: Long,
    val rowIndex: Int = 0,
    val columnIndex: Int = 0,
    val rowSpan: Int = 1,
    val colSpan: Int = 1,
    val text: String = "",
    val confidence: Float = 0.0f
)

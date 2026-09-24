package com.example.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Entity(
    tableName = "embeddings",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["chunkId"]),
        Index(value = ["targetType", "targetId"])
    ]
)
data class EmbeddingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val chunkId: Long? = null,
    val targetType: String = "CHUNK", // CHUNK, CHAPTER, BOOK
    val targetId: Long = 0L,
    val modelId: String = "raqeem-embed-ar-v1",
    val modelVersion: String = "1.0",
    val dimensions: Int = 128,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val vectorBlob: ByteArray,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toFloatArray(): FloatArray {
        val buffer = ByteBuffer.wrap(vectorBlob).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(dimensions)
        for (i in 0 until dimensions) {
            floats[i] = buffer.getFloat()
        }
        return floats
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EmbeddingEntity
        if (id != other.id) return false
        if (documentId != other.documentId) return false
        if (chunkId != other.chunkId) return false
        if (targetType != other.targetType) return false
        if (targetId != other.targetId) return false
        if (!vectorBlob.contentEquals(other.vectorBlob)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + (chunkId?.hashCode() ?: 0)
        result = 31 * result + targetType.hashCode()
        result = 31 * result + targetId.hashCode()
        result = 31 * result + vectorBlob.contentHashCode()
        return result
    }

    companion object {
        fun fromFloatArray(
            documentId: Long,
            chunkId: Long?,
            targetType: String,
            targetId: Long,
            modelId: String,
            modelVersion: String,
            floats: FloatArray
        ): EmbeddingEntity {
            val buffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (f in floats) {
                buffer.putFloat(f)
            }
            return EmbeddingEntity(
                documentId = documentId,
                chunkId = chunkId,
                targetType = targetType,
                targetId = targetId,
                modelId = modelId,
                modelVersion = modelVersion,
                dimensions = floats.size,
                vectorBlob = buffer.array()
            )
        }
    }
}

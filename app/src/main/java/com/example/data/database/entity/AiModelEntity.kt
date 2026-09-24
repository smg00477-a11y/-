package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_models",
    indices = [
        Index(value = ["modelId"], unique = true),
        Index(value = ["type"])
    ]
)
data class AiModelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val modelId: String,
    val name: String,
    val type: String, // LLM, EMBEDDING, RERANKER
    val format: String, // NATIVE_TENSOR, GGUF, ONNX, TFLITE
    val quantization: String, // INT8, FP16, Q4_K_M, EMBED_128D
    val filePath: String? = null,
    val fileSize: Long = 0L,
    val sha256: String? = null,
    val contextLength: Int = 2048,
    val supportedTasks: String = "SUMMARIZATION,QA,TOPICS,KEYWORDS",
    val isBuiltIn: Boolean = true,
    val isActive: Boolean = true,
    val installedAt: Long = System.currentTimeMillis()
)

package com.example.ai.model

import android.content.Context
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.data.database.entity.AiModelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class ModelManager(
    private val context: Context,
    private val database: AppDatabase
) {

    val modelsFlow: Flow<List<AiModelEntity>> = database.aiModelDao().getAllModels()

    suspend fun initializeDefaultModels() = withContext(Dispatchers.IO) {
        val existing = database.aiModelDao().getAllModelsDirect()
        if (existing.isEmpty()) {
            // Built-in Grounded Arabic Reasoning LLM Engine
            database.aiModelDao().insertModel(
                AiModelEntity(
                    modelId = "raqeem-grounded-ar-v1",
                    name = "محرك رقيم للاستدلال والتلخيص المعرفي (مدمج)",
                    type = "LLM",
                    format = "NATIVE_REASONING",
                    quantization = "BUILTIN_OFFLINE",
                    fileSize = 0L,
                    contextLength = 4096,
                    supportedTasks = "SUMMARIZATION,QA,TOPICS,KEYWORDS,INSIGHTS",
                    isBuiltIn = true,
                    isActive = true
                )
            )

            // Built-in Arabic Semantic Vector Embedding Engine
            database.aiModelDao().insertModel(
                AiModelEntity(
                    modelId = "raqeem-embed-ar-v1",
                    name = "نموذج المتجهات الدلالية العربية (128D)",
                    type = "EMBEDDING",
                    format = "NATIVE_VECTOR",
                    quantization = "FP32_DENSE",
                    fileSize = 0L,
                    contextLength = 1024,
                    supportedTasks = "SEMANTIC_SEARCH,CLUSTERING,SIMILARITY",
                    isBuiltIn = true,
                    isActive = true
                )
            )
        }
    }

    /**
     * Imports an offline local model file (.gguf, .onnx, .tflite) chosen by the user
     * via Android SAF without any network access.
     */
    suspend fun importModelFromUri(
        uri: Uri,
        modelName: String,
        modelType: String // LLM, EMBEDDING
    ): Result<AiModelEntity> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("تعذر فتح ملف النموذج المختار"))

            val modelsDir = File(context.filesDir, "models").apply { if (!exists()) mkdirs() }
            val cleanName = modelName.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            val targetFile = File(modelsDir, "$cleanName.bin")

            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(16 * 1024)
            var bytesRead: Int
            var totalBytes = 0L

            FileOutputStream(targetFile).use { outputStream ->
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                }
            }
            inputStream.close()

            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }

            val format = when {
                targetFile.name.endsWith(".gguf", ignoreCase = true) -> "GGUF"
                targetFile.name.endsWith(".onnx", ignoreCase = true) -> "ONNX"
                targetFile.name.endsWith(".tflite", ignoreCase = true) -> "TFLITE"
                else -> "BINARY"
            }

            val entity = AiModelEntity(
                modelId = "imported-${System.currentTimeMillis()}",
                name = modelName,
                type = modelType,
                format = format,
                quantization = "AUTO_DETECT",
                filePath = targetFile.absolutePath,
                fileSize = totalBytes,
                sha256 = sha256,
                contextLength = 2048,
                supportedTasks = if (modelType == "LLM") "QA,SUMMARIZATION" else "EMBEDDINGS",
                isBuiltIn = false,
                isActive = false
            )

            database.aiModelDao().insertModel(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteModel(model: AiModelEntity) = withContext(Dispatchers.IO) {
        if (!model.isBuiltIn && model.filePath != null) {
            val file = File(model.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        database.aiModelDao().deleteModel(model)
    }

    suspend fun setActiveModel(model: AiModelEntity) = withContext(Dispatchers.IO) {
        val all = database.aiModelDao().getAllModelsDirect().filter { it.type == model.type }
        for (m in all) {
            database.aiModelDao().updateModel(m.copy(isActive = m.id == model.id))
        }
    }
}

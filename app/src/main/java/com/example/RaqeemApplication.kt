package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.preferences.ThemePreferences
import com.example.data.repository.DocumentRepository
import com.example.data.storage.FileStorageManager
import com.example.ocr.engine.OcrEngine
import com.example.ocr.engine.OcrEngineFactory
import com.example.ocr.engine.TessDataStorage
import com.example.processing.pipeline.DocumentProcessingPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RaqeemApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Pre-unpack local OCR traineddata assets to app storage in background (100% offline, zero network)
        CoroutineScope(Dispatchers.IO).launch {
            TessDataStorage.ensureTessDataReady(this@RaqeemApplication)
            container.localAiEngine.initialize()
        }
    }
}

class AppContainer(application: Application) {
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(application)
    }

    val storageManager: FileStorageManager by lazy {
        FileStorageManager(application)
    }

    val processingPipeline: DocumentProcessingPipeline by lazy {
        DocumentProcessingPipeline(application, storageManager)
    }

    val ocrEngine: OcrEngine by lazy {
        OcrEngineFactory.getEngine(application)
    }

    val ocrRouter: com.example.ocr.engine.OcrEngineRouter by lazy {
        OcrEngineFactory.getRouter(application)
    }

    val themePreferences: ThemePreferences by lazy {
        ThemePreferences(application)
    }

    val documentRepository: DocumentRepository by lazy {
        DocumentRepository(database, storageManager, processingPipeline, ocrEngine, ocrRouter)
    }

    val localAiEngine: com.example.ai.LocalAiEngine by lazy {
        com.example.ai.LocalAiEngine(application, database)
    }

    val storageOptimizationManager: com.example.data.storage.archive.StorageOptimizationManager by lazy {
        com.example.data.storage.archive.StorageOptimizationManager(application, database)
    }

    val bookVersionManager: com.example.data.storage.archive.BookVersionManager by lazy {
        com.example.data.storage.archive.BookVersionManager(database)
    }

    val archiveHealthChecker: com.example.data.storage.archive.ArchiveHealthChecker by lazy {
        com.example.data.storage.archive.ArchiveHealthChecker(application, database)
    }

    val raqeemArchiveManager: com.example.data.storage.archive.RaqeemArchiveManager by lazy {
        com.example.data.storage.archive.RaqeemArchiveManager(application, database)
    }

    val localDeviceTransferManager: com.example.data.storage.archive.LocalDeviceTransferManager by lazy {
        com.example.data.storage.archive.LocalDeviceTransferManager(application, database, raqeemArchiveManager)
    }
}

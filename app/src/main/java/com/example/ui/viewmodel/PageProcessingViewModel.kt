package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.DocumentRepository
import com.example.processing.analysis.ImageAnalysis
import com.example.processing.analysis.PageBoundaryDetector
import com.example.processing.model.DocumentQuad
import com.example.processing.model.ProcessingMode
import com.example.processing.model.ProcessingOptions
import com.example.processing.model.ProcessingResult
import com.example.processing.model.ProcessingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PageProcessingUiState(
    val originalFilePath: String = "",
    val pageId: Long = -1L,
    val pageIndex: Int = 0,
    val documentId: Long = -1L,
    val activeTab: ProcessingTab = ProcessingTab.GEOMETRY,
    val quad: DocumentQuad = DocumentQuad.defaultQuad(),
    val options: ProcessingOptions = ProcessingOptions(),
    val isAutoDetected: Boolean = false,
    val statusMessage: String? = null,
    val previewBitmap: Bitmap? = null,
    val originalBitmap: Bitmap? = null,
    val isLoading: Boolean = true,
    val isProcessingSaving: Boolean = false,
    val showOriginal: Boolean = false,
    val error: String? = null
)

enum class ProcessingTab(val titleAr: String) {
    GEOMETRY("الحدود والمنظور"),
    ENHANCEMENT("الفلاتر والتحسين")
}

class PageProcessingViewModel(
    val pageId: Long,
    val initialFilePath: String,
    val pageIndex: Int,
    val documentId: Long,
    private val repository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PageProcessingUiState(
            originalFilePath = initialFilePath,
            pageId = pageId,
            pageIndex = pageIndex,
            documentId = documentId
        )
    )
    val uiState: StateFlow<PageProcessingUiState> = _uiState.asStateFlow()

    init {
        loadPage()
    }

    private fun loadPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // If pageId was provided, resolve the real file path from the database
            val targetPath = if (pageId > 0) {
                val entity = repository.getPageById(pageId)
                entity?.localFilePath ?: initialFilePath
            } else {
                initialFilePath
            }

            if (targetPath.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, error = "مسار الصفحة غير متوفر") }
                return@launch
            }

            _uiState.update { it.copy(originalFilePath = targetPath) }

            withContext(Dispatchers.Default) {
                val orig = ImageAnalysis.decodeOrientedBitmap(targetPath, maxDimension = 1000)
                if (orig == null) {
                    _uiState.update { it.copy(isLoading = false, error = "تعذر تحميل صورة الصفحة") }
                    return@withContext
                }

                // Run automatic page boundary detection
                val detection = PageBoundaryDetector.detectPageBoundary(orig)

                _uiState.update { state ->
                    state.copy(
                        originalBitmap = orig,
                        previewBitmap = orig,
                        quad = detection.quad,
                        isAutoDetected = detection.isAutoDetected,
                        statusMessage = detection.statusMessageAr,
                        options = state.options.copy(cropQuad = detection.quad),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectTab(tab: ProcessingTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == ProcessingTab.ENHANCEMENT) {
            refreshEnhancementPreview()
        }
    }

    fun updateCorner(cornerIndex: Int, normalizedPoint: PointF) {
        val currentQuad = _uiState.value.quad
        val clamped = PointF(normalizedPoint.x.coerceIn(0f, 1f), normalizedPoint.y.coerceIn(0f, 1f))

        val newQuad = when (cornerIndex) {
            0 -> currentQuad.copy(topLeft = clamped)
            1 -> currentQuad.copy(topRight = clamped)
            2 -> currentQuad.copy(bottomRight = clamped)
            3 -> currentQuad.copy(bottomLeft = clamped)
            else -> currentQuad
        }

        if (newQuad.isConvex()) {
            _uiState.update { state ->
                val newOptions = state.options.copy(cropQuad = newQuad)
                state.copy(
                    quad = newQuad,
                    options = newOptions
                )
            }
        }
    }

    fun rotateClockwise() {
        val currentDegrees = _uiState.value.options.rotationDegrees
        val newDegrees = (currentDegrees + 90) % 360
        _uiState.update { state ->
            state.copy(
                options = state.options.copy(rotationDegrees = newDegrees)
            )
        }
        refreshEnhancementPreview()
    }

    fun resetToFullImage() {
        val fullQuad = DocumentQuad.fullImage()
        _uiState.update { state ->
            state.copy(
                quad = fullQuad,
                options = state.options.copy(cropQuad = fullQuad),
                statusMessage = "تم توسيع الحدود لكامل إطار الصورة"
            )
        }
    }

    fun autoDetectBoundaries() {
        val orig = _uiState.value.originalBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val detection = PageBoundaryDetector.detectPageBoundary(orig)
            _uiState.update { state ->
                state.copy(
                    quad = detection.quad,
                    isAutoDetected = detection.isAutoDetected,
                    statusMessage = detection.statusMessageAr,
                    options = state.options.copy(cropQuad = detection.quad)
                )
            }
        }
    }

    fun setProcessingMode(mode: ProcessingMode) {
        _uiState.update { state ->
            state.copy(options = state.options.copy(mode = mode))
        }
        refreshEnhancementPreview()
    }

    fun setBrightness(brightness: Float) {
        _uiState.update { state ->
            state.copy(options = state.options.copy(brightness = brightness))
        }
        refreshEnhancementPreview()
    }

    fun setContrast(contrast: Float) {
        _uiState.update { state ->
            state.copy(options = state.options.copy(contrast = contrast))
        }
        refreshEnhancementPreview()
    }

    fun setNoiseReduction(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(options = state.options.copy(noiseReduction = enabled))
        }
        refreshEnhancementPreview()
    }

    fun setSharpening(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(options = state.options.copy(sharpening = enabled))
        }
        refreshEnhancementPreview()
    }

    fun setDeskew(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(options = state.options.copy(deskew = enabled))
        }
        refreshEnhancementPreview()
    }

    fun setShowOriginal(show: Boolean) {
        _uiState.update { it.copy(showOriginal = show) }
    }

    private fun refreshEnhancementPreview() {
        val path = _uiState.value.originalFilePath
        val currentOptions = _uiState.value.options.copy(cropQuad = _uiState.value.quad)

        viewModelScope.launch(Dispatchers.Default) {
            val preview = repository.processingPipeline.generatePreview(
                originalFilePath = path,
                options = currentOptions,
                maxPreviewDimension = 800
            )
            if (preview != null) {
                _uiState.update { it.copy(previewBitmap = preview) }
            }
        }
    }

    fun applyAndSave(
        onSuccess: (processedPath: String?, thumbPath: String?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val state = _uiState.value
        val finalOptions = state.options.copy(cropQuad = state.quad)

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingSaving = true) }

            val result: ProcessingResult = if (state.pageId > 0) {
                repository.processExistingPage(state.pageId, finalOptions)
            } else {
                repository.processingPipeline.processImage(
                    originalFilePath = state.originalFilePath,
                    options = finalOptions
                )
            }

            _uiState.update { it.copy(isProcessingSaving = false) }

            if (result.success && result.processedFilePath != null) {
                onSuccess(result.processedFilePath, result.thumbnailPath)
            } else {
                onError(result.errorMessage ?: "حدث خطأ أثناء معالجة الصفحة")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            _uiState.value.originalBitmap?.recycle()
            _uiState.value.previewBitmap?.recycle()
        } catch (ignored: Exception) {}
    }

    class Factory(
        private val pageId: Long,
        private val initialFilePath: String,
        private val pageIndex: Int,
        private val documentId: Long,
        private val repository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PageProcessingViewModel(
                pageId = pageId,
                initialFilePath = initialFilePath,
                pageIndex = pageIndex,
                documentId = documentId,
                repository = repository
            ) as T
        }
    }
}

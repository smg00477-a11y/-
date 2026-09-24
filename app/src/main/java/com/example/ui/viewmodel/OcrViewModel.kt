package com.example.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.OcrRegionEntity
import com.example.data.database.entity.PageEntity
import com.example.data.repository.DocumentRepository
import com.example.ocr.model.NormalizedBoundingBox
import com.example.ocr.model.OcrBoundingBox
import com.example.ocr.model.OcrEngineType
import com.example.ocr.model.OcrLanguage
import com.example.ocr.model.OcrStatus
import com.example.ocr.model.PageType
import com.example.ocr.model.RegionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OcrUiState(
    val isLoading: Boolean = true,
    val isRunningOcr: Boolean = false,
    val page: PageEntity? = null,
    val documentWithPages: DocumentWithPages? = null,
    val selectedLanguage: OcrLanguage = OcrLanguage.ARABIC_AND_ENGLISH,
    val selectedEngine: OcrEngineType = OcrEngineType.AUTO,
    val pageType: PageType = PageType.UNKNOWN,
    val regions: List<OcrRegionEntity> = emptyList(),
    val isManualSelectionMode: Boolean = false,
    val selectionBox: NormalizedBoundingBox? = null,
    val editedText: String = "",
    val hasUnsavedEdits: Boolean = false,
    val activeTab: Int = 0, // 0: Text, 1: Regions, 2: Cleaned, 3: Raw
    val status: OcrStatus = OcrStatus.NOT_PROCESSED,
    val confidence: Float = 0f,
    val engineUsed: String = "Tesseract",
    val errorMessage: String? = null,
    val userNotification: String? = null
)

class OcrViewModel(
    private val repository: DocumentRepository,
    private val initialPageId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    private var currentPageId: Long = initialPageId

    init {
        loadPage(initialPageId)
    }

    fun loadPage(pageId: Long) {
        currentPageId = pageId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val page = repository.getPageById(pageId)
            if (page != null) {
                val currentText = page.effectiveOcrText
                val parsedPageType = PageType.fromName(page.pageType)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        page = page,
                        selectedLanguage = OcrLanguage.fromCode(page.ocrLanguage),
                        pageType = parsedPageType,
                        engineUsed = page.ocrEngineUsed,
                        editedText = currentText,
                        hasUnsavedEdits = false,
                        status = try { OcrStatus.valueOf(page.ocrStatus) } catch (e: Exception) { OcrStatus.NOT_PROCESSED },
                        confidence = page.ocrConfidence,
                        errorMessage = page.ocrErrorMessage
                    )
                }

                // Collect structured layout regions
                viewModelScope.launch {
                    repository.getRegionsForPageFlow(pageId).collect { regionList ->
                        _uiState.update { it.copy(regions = regionList) }
                    }
                }

                // If never processed before, auto-trigger first recognition
                if (page.ocrStatus == OcrStatus.NOT_PROCESSED.name && page.effectiveOcrText.isBlank()) {
                    runOcr()
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "الصفحة غير موجودة")
                }
            }
        }
    }

    fun onEditedTextChanged(newText: String) {
        val currentEffective = _uiState.value.page?.effectiveOcrText ?: ""
        _uiState.update {
            it.copy(
                editedText = newText,
                hasUnsavedEdits = (newText != currentEffective)
            )
        }
    }

    fun setActiveTab(index: Int) {
        _uiState.update { it.copy(activeTab = index) }
    }

    fun setSelectedLanguage(language: OcrLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun setSelectedEngine(engine: OcrEngineType) {
        _uiState.update { it.copy(selectedEngine = engine) }
    }

    fun setPageType(pageType: PageType) {
        val page = _uiState.value.page ?: return
        viewModelScope.launch {
            repository.updatePageType(page.id, pageType)
            _uiState.update { it.copy(pageType = pageType) }
        }
    }

    fun toggleManualSelectionMode() {
        _uiState.update {
            it.copy(
                isManualSelectionMode = !it.isManualSelectionMode,
                selectionBox = if (it.isManualSelectionMode) null else it.selectionBox
            )
        }
    }

    fun setSelectionBox(box: NormalizedBoundingBox?) {
        _uiState.update { it.copy(selectionBox = box) }
    }

    fun saveEdits() {
        val page = _uiState.value.page ?: return
        val textToSave = _uiState.value.editedText
        viewModelScope.launch {
            repository.updateUserEditedText(page.id, textToSave)
            val updated = repository.getPageById(page.id)
            _uiState.update {
                it.copy(
                    page = updated,
                    hasUnsavedEdits = false,
                    userNotification = "تم حفظ النص بنجاح"
                )
            }
        }
    }

    fun runOcr(
        targetLanguage: OcrLanguage = _uiState.value.selectedLanguage,
        targetEngine: OcrEngineType = _uiState.value.selectedEngine
    ) {
        val page = _uiState.value.page ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningOcr = true,
                    status = OcrStatus.PROCESSING,
                    errorMessage = null
                )
            }

            val result = repository.runOcrOnPage(
                pageId = page.id,
                language = targetLanguage,
                preferredEngine = targetEngine,
                manualPageType = if (_uiState.value.pageType != PageType.UNKNOWN) _uiState.value.pageType else null
            )
            val updatedPage = repository.getPageById(page.id)

            _uiState.update {
                it.copy(
                    isRunningOcr = false,
                    page = updatedPage,
                    status = result.status,
                    confidence = result.confidence,
                    engineUsed = result.engineUsed,
                    pageType = result.pageType,
                    editedText = updatedPage?.effectiveOcrText ?: result.effectiveText,
                    hasUnsavedEdits = false,
                    errorMessage = result.errorMessage,
                    userNotification = if (result.status == OcrStatus.COMPLETED) {
                        "تم التعرف بواسطة ${result.engineUsed} (${result.rawText.length} حرف، ${result.regions.size} منطقة)"
                    } else {
                        null
                    }
                )
            }
        }
    }

    /**
     * Executes recognition on user-selected manual region bounding box.
     */
    fun recognizeSelectedRegion(
        bitmapWidth: Int,
        bitmapHeight: Int,
        targetType: PageType = PageType.PRINTED,
        regionType: RegionType = RegionType.MAIN_TEXT
    ) {
        val page = _uiState.value.page ?: return
        val normBox = _uiState.value.selectionBox ?: return

        val absBox = normBox.toAbsolute(bitmapWidth, bitmapHeight)
        if (absBox.width < 10 || absBox.height < 10) {
            _uiState.update { it.copy(userNotification = "المنطقة المحددة صغيرة جداً") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRunningOcr = true) }
            val regionEntity = repository.recognizeManualRegion(
                pageId = page.id,
                cropBox = absBox,
                targetType = targetType,
                regionType = regionType,
                language = _uiState.value.selectedLanguage
            )

            if (regionEntity != null) {
                // If recognized region text is meaningful, append or offer to append to main text
                val newText = regionEntity.effectiveText
                if (newText.isNotBlank()) {
                    val currentEdits = _uiState.value.editedText
                    val updatedEdits = if (currentEdits.isBlank()) newText else "$currentEdits\n\n$newText"
                    onEditedTextChanged(updatedEdits)
                }

                _uiState.update {
                    it.copy(
                        isRunningOcr = false,
                        selectionBox = null,
                        isManualSelectionMode = false,
                        userNotification = "تم التعرف على المنطقة (${regionEntity.rawText.take(25)}...)"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isRunningOcr = false,
                        userNotification = "تعذر التعرف على المنطقة المحددة"
                    )
                }
            }
        }
    }

    fun updateRegionUserText(regionId: Long, newText: String) {
        viewModelScope.launch {
            repository.updateRegionUserText(regionId, newText)
        }
    }

    fun updateRegionType(regionId: Long, regionType: RegionType) {
        viewModelScope.launch {
            repository.updateRegionType(regionId, regionType)
        }
    }

    fun deleteRegion(regionId: Long) {
        viewModelScope.launch {
            repository.deleteRegion(regionId)
        }
    }

    fun copyTextToClipboard(context: Context) {
        val textToCopy = _uiState.value.editedText.ifBlank {
            _uiState.value.page?.effectiveOcrText ?: ""
        }
        if (textToCopy.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Raqeem OCR Text", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "تم نسخ النص إلى الحافظة", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "لا يوجد نص لنسخه", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    class Factory(
        private val repository: DocumentRepository,
        private val pageId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OcrViewModel(repository, pageId) as T
        }
    }
}

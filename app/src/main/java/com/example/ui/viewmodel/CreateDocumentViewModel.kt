package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DocumentType
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateDocumentViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    private val defaultTitle: String
        get() {
            val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
            return "مستند $dateStr"
        }

    private val _title = MutableStateFlow(defaultTitle)
    val title: StateFlow<String> = _title.asStateFlow()

    private val _selectedType = MutableStateFlow(DocumentType.DOCUMENT)
    val selectedType: StateFlow<DocumentType> = _selectedType.asStateFlow()

    private val _pages = MutableStateFlow<List<String>>(emptyList())
    val pages: StateFlow<List<String>> = _pages.asStateFlow()

    // Map of pageIndex -> Pair(processedFilePath, thumbnailPath)
    private val _processedPages = MutableStateFlow<Map<Int, Pair<String, String>>>(emptyMap())
    val processedPages: StateFlow<Map<Int, Pair<String, String>>> = _processedPages.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun updateType(newType: DocumentType) {
        _selectedType.value = newType
    }

    fun addPage(filePath: String) {
        val current = _pages.value.toMutableList()
        current.add(filePath)
        _pages.value = current
    }

    fun setPageProcessed(pageIndex: Int, processedPath: String, thumbPath: String?) {
        val currentMap = _processedPages.value.toMutableMap()
        currentMap[pageIndex] = Pair(processedPath, thumbPath ?: processedPath)
        _processedPages.value = currentMap
    }

    fun getDisplayPathForPage(index: Int): String {
        return _processedPages.value[index]?.first ?: _pages.value.getOrNull(index) ?: ""
    }

    fun importImageUri(uri: Uri, onCompleted: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val storedPath = repository.importUriToLocal(uri)
                if (storedPath != null) {
                    addPage(storedPath)
                    onCompleted(true)
                } else {
                    _errorMessage.value = "تعذر نسخ الصورة المحددة"
                    onCompleted(false)
                }
            } catch (e: Exception) {
                _errorMessage.value = "حدث خطأ أثناء استيراد الصورة: ${e.localizedMessage}"
                onCompleted(false)
            }
        }
    }

    fun addCapturedFile(tempFile: File, onCompleted: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val savedPath = repository.commitTempCaptureFile(tempFile)
                if (savedPath != null) {
                    addPage(savedPath)
                    onCompleted(true)
                } else {
                    _errorMessage.value = "تعذر حفظ الصورة الملتقطة"
                    onCompleted(false)
                }
            } catch (e: Exception) {
                _errorMessage.value = "خطأ أثناء حفظ الصورة: ${e.localizedMessage}"
                onCompleted(false)
            }
        }
    }

    fun removePage(index: Int) {
        val current = _pages.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _pages.value = current

            // Re-index processed pages map
            val newProcessedMap = mutableMapOf<Int, Pair<String, String>>()
            _processedPages.value.forEach { (idx, pair) ->
                if (idx < index) {
                    newProcessedMap[idx] = pair
                } else if (idx > index) {
                    newProcessedMap[idx - 1] = pair
                }
            }
            _processedPages.value = newProcessedMap
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val current = _pages.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _pages.value = current
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun saveDocument(onSuccess: (Long) -> Unit) {
        if (_pages.value.isEmpty()) {
            _errorMessage.value = "يرجى إضافة صفحة واحدة على الأقل قبل الحفظ"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val docId = repository.createDocument(
                    title = _title.value,
                    type = _selectedType.value,
                    pageFilePaths = _pages.value,
                    processedPageMap = _processedPages.value
                )
                _isSaving.value = false
                reset()
                onSuccess(docId)
            } catch (e: Exception) {
                _isSaving.value = false
                _errorMessage.value = "فشل في حفظ المستند: ${e.localizedMessage}"
            }
        }
    }

    fun reset() {
        _title.value = defaultTitle
        _selectedType.value = DocumentType.DOCUMENT
        _pages.value = emptyList()
        _processedPages.value = emptyMap()
        _isSaving.value = false
        _errorMessage.value = null
    }

    class Factory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateDocumentViewModel(repository) as T
        }
    }
}

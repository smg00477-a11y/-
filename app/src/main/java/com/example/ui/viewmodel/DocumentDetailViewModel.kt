package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.CategoryEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.SeriesEntity
import com.example.data.model.SimilarBookResult
import com.example.data.repository.DocumentRepository
import com.example.ocr.export.TxtExportManager
import com.example.ocr.model.OcrLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DocumentDetailViewModel(
    private val documentId: Long,
    private val repository: DocumentRepository
) : ViewModel() {

    val documentWithPages: StateFlow<DocumentWithPages?> =
        repository.getDocumentById(documentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    val bookCategories: StateFlow<List<CategoryEntity>> =
        repository.getCategoriesForDocument(documentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val bookSeries: StateFlow<SeriesEntity?> =
        repository.getSeriesForDocument(documentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    private val _similarBooks = MutableStateFlow<List<SimilarBookResult>>(emptyList())
    val similarBooks: StateFlow<List<SimilarBookResult>> = _similarBooks.asStateFlow()

    init {
        loadSimilarBooks()
    }

    fun loadSimilarBooks() {
        viewModelScope.launch {
            try {
                val results = repository.getSimilarBooks(documentId, limit = 4)
                _similarBooks.value = results
            } catch (e: Exception) {
                _similarBooks.value = emptyList()
            }
        }
    }

    private val _isOcrBatchRunning = MutableStateFlow(false)
    val isOcrBatchRunning: StateFlow<Boolean> = _isOcrBatchRunning.asStateFlow()

    private val _ocrProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val ocrProgress: StateFlow<Pair<Int, Int>?> = _ocrProgress.asStateFlow()

    fun runOcrOnAllPages(language: OcrLanguage = OcrLanguage.ARABIC_AND_ENGLISH) {
        val doc = documentWithPages.value ?: return
        if (doc.pages.isEmpty() || _isOcrBatchRunning.value) return

        viewModelScope.launch {
            _isOcrBatchRunning.value = true
            _ocrProgress.value = 0 to doc.pages.size

            repository.runOcrOnDocument(documentId, language) { completed, total ->
                _ocrProgress.value = completed to total
            }

            _isOcrBatchRunning.value = false
            _ocrProgress.value = null
        }
    }

    fun runOcrOnPage(pageId: Long, language: OcrLanguage = OcrLanguage.ARABIC_AND_ENGLISH) {
        viewModelScope.launch {
            repository.runOcrOnPage(pageId, language)
        }
    }

    fun retryFailedPages(language: OcrLanguage = OcrLanguage.ARABIC_AND_ENGLISH) {
        viewModelScope.launch {
            _isOcrBatchRunning.value = true
            repository.retryFailedOcrPages(documentId, language) { completed, total ->
                _ocrProgress.value = completed to total
            }
            _isOcrBatchRunning.value = false
            _ocrProgress.value = null
        }
    }

    private val _isPdfExporting = MutableStateFlow(false)
    val isPdfExporting: StateFlow<Boolean> = _isPdfExporting.asStateFlow()

    private val _pdfProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val pdfProgress: StateFlow<Pair<Int, Int>?> = _pdfProgress.asStateFlow()

    fun exportToPdf(context: Context, onReady: (Uri) -> Unit) {
        val doc = documentWithPages.value ?: return
        if (_isPdfExporting.value) return

        viewModelScope.launch {
            _isPdfExporting.value = true
            _pdfProgress.value = 0 to doc.pages.size
            val file = repository.exportPdf(context, documentId) { curr, total ->
                _pdfProgress.value = curr to total
            }
            _isPdfExporting.value = false
            _pdfProgress.value = null

            if (file != null) {
                val uri = com.example.ocr.export.PdfExportManager.getShareableUri(context, file)
                onReady(uri)
            }
        }
    }

    fun exportToTxt(context: Context, onReady: (Uri) -> Unit) {
        val doc = documentWithPages.value ?: return
        viewModelScope.launch {
            val file = TxtExportManager.exportDocumentToTxt(context, doc)
            val uri = TxtExportManager.getShareableUri(context, file)
            onReady(uri)
        }
    }

    fun toggleFavorite() {
        val current = documentWithPages.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(documentId, current.document.isFavorite)
        }
    }

    fun deleteDocument(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDocument(documentId)
            onDeleted()
        }
    }

    fun getShareableUri(pageIndex: Int = 0): Uri? {
        val doc = documentWithPages.value ?: return null
        val page = doc.sortedPages.getOrNull(pageIndex) ?: doc.sortedPages.firstOrNull() ?: return null
        return repository.getShareableUri(page.displayFilePath)
    }

    class Factory(
        private val documentId: Long,
        private val repository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DocumentDetailViewModel(documentId, repository) as T
        }
    }
}

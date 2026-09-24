package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.AuthorEntity
import com.example.data.database.entity.BookMetadataEntity
import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.TableEntity
import com.example.data.database.entity.TocEntryEntity
import com.example.data.repository.DocumentRepository
import com.example.intelligence.AuthorMatcher
import com.example.ocr.export.PdfExportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookIdentityViewModel(
    val documentId: Long,
    private val repository: DocumentRepository
) : ViewModel() {

    val documentWithPages: StateFlow<DocumentWithPages?> =
        repository.getDocumentById(documentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    val metadata: StateFlow<BookMetadataEntity?> =
        repository.getMetadataForDocument(documentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    val chapters: StateFlow<List<ChapterEntity>> =
        repository.getChaptersForDocument(documentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val tocEntries: StateFlow<List<TocEntryEntity>> =
        repository.getTocForDocument(documentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val tables: StateFlow<List<TableEntity>> =
        repository.getTablesForDocument(documentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val author: StateFlow<AuthorEntity?> = metadata.flatMapLatest { meta ->
        val authorId = meta?.authorId
        if (authorId != null) {
            repository.getAuthorById(authorId)
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisStep = MutableStateFlow("")
    val analysisStep: StateFlow<String> = _analysisStep.asStateFlow()

    private val _analysisProgress = MutableStateFlow(0f)
    val analysisProgress: StateFlow<Float> = _analysisProgress.asStateFlow()

    private val _exportingPdf = MutableStateFlow(false)
    val exportingPdf: StateFlow<Boolean> = _exportingPdf.asStateFlow()

    private val _exportedPdfUri = MutableStateFlow<Uri?>(null)
    val exportedPdfUri: StateFlow<Uri?> = _exportedPdfUri.asStateFlow()

    fun runStructureAnalysis() {
        if (_isAnalyzing.value) return
        _isAnalyzing.value = true
        _analysisProgress.value = 0.05f
        _analysisStep.value = "بدء التحليل الهيكلي للكتاب..."

        viewModelScope.launch {
            repository.analyzeBookStructure(documentId) { step, progress ->
                _analysisStep.value = step
                _analysisProgress.value = progress
            }
            _isAnalyzing.value = false
        }
    }

    fun saveMetadata(
        title: String,
        subtitle: String,
        author: String,
        publisher: String,
        publicationYear: String,
        edition: String,
        isbn: String,
        category: String,
        tags: String,
        description: String,
        notes: String
    ) {
        val current = metadata.value
        val doc = documentWithPages.value?.document

        viewModelScope.launch {
            var authorId = current?.authorId
            val cleanAuthor = author.trim()

            if (cleanAuthor.isNotBlank() && cleanAuthor != "غير محدد") {
                val normalized = AuthorMatcher.normalizeAuthorName(cleanAuthor)
                val existing = repository.authorDao.findAuthorByNormalizedName(normalized)
                authorId = if (existing != null) {
                    existing.id
                } else {
                    repository.authorDao.insertAuthor(
                        AuthorEntity(
                            name = cleanAuthor,
                            normalizedName = normalized
                        )
                    )
                }
            }

            val newMeta = BookMetadataEntity(
                id = current?.id ?: 0L,
                documentId = documentId,
                title = title.trim().ifBlank { doc?.title ?: "كتاب بدون عنوان" },
                subtitle = subtitle.trim(),
                author = cleanAuthor.ifBlank { "غير محدد" },
                authorId = authorId,
                coAuthors = current?.coAuthors ?: "",
                translator = current?.translator ?: "",
                editor = current?.editor ?: "",
                publisher = publisher.trim(),
                publicationYear = publicationYear.trim(),
                edition = edition.trim(),
                isbn = isbn.trim(),
                language = current?.language ?: "ar",
                category = category.trim().ifBlank { "عام ومراجع متنوعة" },
                subject = current?.subject ?: "",
                tags = tags.trim(),
                description = description.trim(),
                notes = notes.trim(),
                originalPageCount = doc?.originalPageCount ?: (documentWithPages.value?.pages?.size ?: 0),
                detectionStatus = "USER_EDITED",
                updatedAt = System.currentTimeMillis()
            )

            repository.saveBookMetadata(newMeta)
        }
    }

    fun exportPdf(context: Context, onReady: (Uri) -> Unit) {
        val docWithPages = documentWithPages.value ?: return
        if (_exportingPdf.value) return
        _exportingPdf.value = true

        viewModelScope.launch {
            val file = repository.exportPdf(context, documentId)
            _exportingPdf.value = false
            if (file != null) {
                val uri = com.example.ocr.export.PdfExportManager.getShareableUri(context, file)
                _exportedPdfUri.value = uri
                onReady(uri)
            }
        }
    }

    fun clearExportedPdfUri() {
        _exportedPdfUri.value = null
    }

    class Factory(
        private val documentId: Long,
        private val repository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BookIdentityViewModel(documentId, repository) as T
        }
    }
}

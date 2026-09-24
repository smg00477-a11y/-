package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.LocalAiEngine
import com.example.ai.model.GroundedAnswer
import com.example.ai.model.RetrievedChunk
import com.example.data.database.entity.BookMetadataEntity
import com.example.data.database.entity.BookNoteEntity
import com.example.data.database.entity.BookmarkEntity
import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.SummaryEntity
import com.example.data.database.entity.TableEntity
import com.example.data.database.entity.TocEntryEntity
import com.example.data.repository.DocumentRepository
import com.example.ocr.cleaner.ArabicTextCleaner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InBookSearchResult(
    val pageIndex: Int,
    val snippet: String
)

class ReaderViewModel(
    val documentId: Long,
    initialPage: Int,
    private val repository: DocumentRepository,
    private val localAiEngine: LocalAiEngine? = null
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

    val bookmarks: StateFlow<List<BookmarkEntity>> =
        repository.getBookmarksForDocument(documentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val notes: StateFlow<List<BookNoteEntity>> =
        repository.getNotesForDocument(documentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _currentPage = MutableStateFlow(initialPage)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<InBookSearchResult>>(emptyList())
    val searchResults: StateFlow<List<InBookSearchResult>> = _searchResults.asStateFlow()

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    fun toggleBookmarkForCurrentPage() {
        val doc = documentWithPages.value ?: return
        val pages = doc.sortedPages
        val currIdx = _currentPage.value.coerceIn(0, pages.lastIndex)
        val page = pages.getOrNull(currIdx) ?: return

        val chTitle = chapters.value.firstOrNull { currIdx in it.startPageIndex..it.endPageIndex }?.title ?: ""

        viewModelScope.launch {
            repository.toggleBookmark(
                documentId = documentId,
                pageId = page.id,
                pageIndex = currIdx,
                chapterTitle = chTitle,
                title = "صفحة ${currIdx + 1}"
            )
        }
    }

    fun isCurrentPageBookmarked(): Boolean {
        val curr = _currentPage.value
        return bookmarks.value.any { it.pageIndex == curr }
    }

    fun addNote(content: String) {
        if (content.isBlank()) return
        val curr = _currentPage.value
        val doc = documentWithPages.value
        val page = doc?.sortedPages?.getOrNull(curr)
        val ch = chapters.value.firstOrNull { curr in it.startPageIndex..it.endPageIndex }

        viewModelScope.launch {
            repository.addBookNote(
                documentId = documentId,
                pageId = page?.id,
                pageIndex = curr,
                chapterId = ch?.id,
                content = content
            )
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteBookNote(noteId)
        }
    }

    fun searchInBook(query: String) {
        _searchQuery.value = query
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        val doc = documentWithPages.value ?: return
        val normalizedQuery = ArabicTextCleaner.normalizeForSearch(trimmed)
        val results = mutableListOf<InBookSearchResult>()

        for (page in doc.sortedPages) {
            val text = page.effectiveOcrText
            if (text.isNotBlank()) {
                val normalizedText = ArabicTextCleaner.normalizeForSearch(text)
                val idx = normalizedText.indexOf(normalizedQuery)
                if (idx != -1) {
                    val start = (idx - 35).coerceAtLeast(0)
                    val end = (idx + normalizedQuery.length + 35).coerceAtMost(text.length)
                    val snippet = text.substring(start, end).replace("\n", " ").trim()
                    results.add(InBookSearchResult(page.pageIndex, "...$snippet..."))
                }
            }
        }

        _searchResults.value = results
    }

    // --- V0.6 Local AI Reader Assistant ---

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _pageAiResult = MutableStateFlow<String?>(null)
    val pageAiResult: StateFlow<String?> = _pageAiResult.asStateFlow()

    private val _similarChunks = MutableStateFlow<List<RetrievedChunk>>(emptyList())
    val similarChunks: StateFlow<List<RetrievedChunk>> = _similarChunks.asStateFlow()

    fun summarizeCurrentPage() {
        val engine = localAiEngine ?: return
        val doc = documentWithPages.value ?: return
        val page = doc.sortedPages.getOrNull(_currentPage.value) ?: return

        _isAiProcessing.value = true
        _pageAiResult.value = null
        viewModelScope.launch {
            try {
                val summary = engine.summarizePage(documentId, page.pageIndex)
                _pageAiResult.value = "ملخص الصفحة ${page.pageIndex + 1}:\n\n${summary.content}"
            } catch (e: Exception) {
                _pageAiResult.value = "تعذر إعداد الملخص: ${e.localizedMessage}"
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun explainCurrentPage() {
        val engine = localAiEngine ?: return
        val doc = documentWithPages.value ?: return
        val page = doc.sortedPages.getOrNull(_currentPage.value) ?: return
        val text = page.effectiveOcrText
        if (text.isBlank()) {
            _pageAiResult.value = "لا يوجد نص مقروء ضوئياً في هذه الصفحة لشرحه."
            return
        }

        _isAiProcessing.value = true
        _pageAiResult.value = null
        viewModelScope.launch {
            try {
                val answer = engine.askGrounded(
                    documentId = documentId,
                    question = "اشرح وبسط الأفكار الواردة في هذه الصفحة: $text"
                )
                _pageAiResult.value = answer.answer
            } catch (e: Exception) {
                _pageAiResult.value = "تعذر إكمال الشرح: ${e.localizedMessage}"
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun findSimilarPassages() {
        val engine = localAiEngine ?: return
        val doc = documentWithPages.value ?: return
        val page = doc.sortedPages.getOrNull(_currentPage.value) ?: return
        val text = page.effectiveOcrText
        if (text.isBlank()) {
            _similarChunks.value = emptyList()
            return
        }

        _isAiProcessing.value = true
        viewModelScope.launch {
            try {
                val results = engine.search(
                    query = text.take(120),
                    documentId = documentId,
                    topK = 5
                ).filter { it.pageIndex != page.pageIndex } // Exclude current page
                _similarChunks.value = results
            } catch (e: Exception) {
                _similarChunks.value = emptyList()
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun askAboutCurrentPage(question: String) {
        val engine = localAiEngine ?: return
        if (question.isBlank()) return

        _isAiProcessing.value = true
        _pageAiResult.value = null
        viewModelScope.launch {
            try {
                val answer = engine.askGrounded(
                    documentId = documentId,
                    question = question
                )
                _pageAiResult.value = answer.answer
            } catch (e: Exception) {
                _pageAiResult.value = "تعذر الإجابة: ${e.localizedMessage}"
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    class Factory(
        private val documentId: Long,
        private val initialPage: Int,
        private val repository: DocumentRepository,
        private val localAiEngine: LocalAiEngine? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReaderViewModel(documentId, initialPage, repository, localAiEngine) as T
        }
    }
}

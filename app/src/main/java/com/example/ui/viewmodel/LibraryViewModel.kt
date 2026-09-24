package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.LocalAiEngine
import com.example.ai.model.RetrievedChunk
import com.example.data.database.entity.CategoryEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.SeriesEntity
import com.example.data.model.DocumentSearchResult
import com.example.data.model.LibraryFilterOptions
import com.example.data.model.LibrarySortField
import com.example.data.model.LibrarySortOption
import com.example.data.model.SortDirection
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: DocumentRepository,
    private val localAiEngine: LocalAiEngine? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSemanticMode = MutableStateFlow(false)
    val isSemanticMode: StateFlow<Boolean> = _isSemanticMode.asStateFlow()

    private val _semanticResults = MutableStateFlow<List<RetrievedChunk>>(emptyList())
    val semanticResults: StateFlow<List<RetrievedChunk>> = _semanticResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // V0.7: Filter Options
    private val _filterOptions = MutableStateFlow(LibraryFilterOptions())
    val filterOptions: StateFlow<LibraryFilterOptions> = _filterOptions.asStateFlow()

    // V0.7: Sort Options
    private val _sortOption = MutableStateFlow(LibrarySortOption())
    val sortOption: StateFlow<LibrarySortOption> = _sortOption.asStateFlow()

    // V0.7: Batch Selection Mode
    private val _selectedDocumentIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDocumentIds: StateFlow<Set<Long>> = _selectedDocumentIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedDocumentIds
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allSeries: StateFlow<List<SeriesEntity>> = repository.allSeries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive Combined Search, Filter & Sort results
    val searchResults: StateFlow<List<DocumentSearchResult>> = combine(
        _searchQuery,
        _filterOptions,
        _sortOption
    ) { query, filter, sort ->
        Triple(query, filter, sort)
    }.flatMapLatest { (query, filter, sort) ->
        repository.searchAndFilterDocuments(query, filter, sort)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val documents: StateFlow<List<DocumentWithPages>> = searchResults
        .map { results -> results.map { it.documentWithPages } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (_isSemanticMode.value && localAiEngine != null) {
            executeSemanticSearch(query)
        }
    }

    fun toggleSearchMode(semantic: Boolean) {
        _isSemanticMode.value = semantic
        if (semantic && _searchQuery.value.isNotBlank()) {
            executeSemanticSearch(_searchQuery.value)
        }
    }

    fun setFilterCategory(categoryName: String?) {
        _filterOptions.value = _filterOptions.value.copy(
            categoryName = if (_filterOptions.value.categoryName == categoryName) null else categoryName
        )
    }

    fun updateFilters(newFilters: LibraryFilterOptions) {
        _filterOptions.value = newFilters
    }

    fun clearAllFilters() {
        _filterOptions.value = LibraryFilterOptions()
    }

    fun updateSorting(field: LibrarySortField, direction: SortDirection) {
        _sortOption.value = LibrarySortOption(field, direction)
    }

    // Batch Selection Operations
    fun toggleDocumentSelection(documentId: Long) {
        val current = _selectedDocumentIds.value.toMutableSet()
        if (current.contains(documentId)) {
            current.remove(documentId)
        } else {
            current.add(documentId)
        }
        _selectedDocumentIds.value = current
    }

    fun selectAll() {
        val allIds = searchResults.value.map { it.documentWithPages.document.id }.toSet()
        _selectedDocumentIds.value = allIds
    }

    fun clearSelection() {
        _selectedDocumentIds.value = emptySet()
    }

    fun deleteSelectedDocuments() {
        val idsToDelete = _selectedDocumentIds.value.toList()
        if (idsToDelete.isEmpty()) return
        viewModelScope.launch {
            repository.deleteDocumentsBatch(idsToDelete)
            clearSelection()
        }
    }

    fun assignCategoryToSelected(categoryId: Long) {
        val idsToAssign = _selectedDocumentIds.value.toList()
        if (idsToAssign.isEmpty()) return
        viewModelScope.launch {
            repository.assignCategoryBatch(idsToAssign, categoryId)
            clearSelection()
        }
    }

    private fun executeSemanticSearch(query: String) {
        val engine = localAiEngine ?: return
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _semanticResults.value = emptyList()
            return
        }

        _isSearching.value = true
        viewModelScope.launch {
            try {
                val results = engine.search(query = trimmed, topK = 10)
                _semanticResults.value = results
            } catch (e: Exception) {
                _semanticResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _semanticResults.value = emptyList()
    }

    fun toggleFavorite(documentId: Long, currentIsFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(documentId, currentIsFavorite)
        }
    }

    fun deleteDocument(documentId: Long) {
        viewModelScope.launch {
            repository.deleteDocument(documentId)
        }
    }

    class Factory(
        private val repository: DocumentRepository,
        private val localAiEngine: LocalAiEngine? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository, localAiEngine) as T
        }
    }
}



package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.SeriesEntity
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SeriesViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    val seriesList: StateFlow<List<SeriesEntity>> = repository.allSeries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedSeries = MutableStateFlow<SeriesEntity?>(null)
    val selectedSeries: StateFlow<SeriesEntity?> = _selectedSeries.asStateFlow()

    private val _seriesBooks = MutableStateFlow<List<DocumentWithPages>>(emptyList())
    val seriesBooks: StateFlow<List<DocumentWithPages>> = _seriesBooks.asStateFlow()

    fun selectSeries(series: SeriesEntity?) {
        _selectedSeries.value = series
        if (series != null) {
            viewModelScope.launch {
                repository.getDocumentsForSeries(series.id).collect {
                    _seriesBooks.value = it
                }
            }
        } else {
            _seriesBooks.value = emptyList()
        }
    }

    fun saveSeries(
        id: Long = 0L,
        title: String,
        authorName: String = "",
        description: String = "",
        totalVolumes: Int = 0
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.saveSeries(
                SeriesEntity(
                    id = id,
                    title = title.trim(),
                    normalizedTitle = title.trim(),
                    authorName = authorName.trim(),
                    description = description.trim(),
                    totalVolumes = totalVolumes
                )
            )
        }
    }

    fun deleteSeries(seriesId: Long) {
        viewModelScope.launch {
            if (_selectedSeries.value?.id == seriesId) {
                _selectedSeries.value = null
                _seriesBooks.value = emptyList()
            }
            repository.deleteSeries(seriesId)
        }
    }

    fun assignBookToSeries(documentId: Long, seriesId: Long, volumeNumber: Int, volumeTitle: String = "") {
        viewModelScope.launch {
            repository.assignBookToSeries(documentId, seriesId, volumeNumber, volumeTitle)
            if (_selectedSeries.value?.id == seriesId) {
                _seriesBooks.value = repository.getDocumentsForSeriesDirect(seriesId)
            }
        }
    }

    fun removeBookFromSeries(documentId: Long, seriesId: Long) {
        viewModelScope.launch {
            repository.removeBookFromSeries(documentId, seriesId)
            if (_selectedSeries.value?.id == seriesId) {
                _seriesBooks.value = repository.getDocumentsForSeriesDirect(seriesId)
            }
        }
    }

    class Factory(
        private val repository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SeriesViewModel(repository) as T
        }
    }
}

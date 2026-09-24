package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.BookVersionEntity
import com.example.data.storage.archive.BookVersionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookVersionViewModel(
    private val documentId: Long,
    private val versionManager: BookVersionManager
) : ViewModel() {

    val versions: StateFlow<List<BookVersionEntity>> = versionManager.getVersions(documentId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createVersionSnapshot(tag: String, description: String, changeSummary: String) {
        viewModelScope.launch {
            versionManager.createVersionSnapshot(documentId, tag, description, changeSummary)
        }
    }

    fun restoreVersion(versionId: Long) {
        viewModelScope.launch {
            versionManager.restoreVersion(versionId)
        }
    }

    fun deleteVersion(versionId: Long) {
        viewModelScope.launch {
            versionManager.deleteVersion(versionId)
        }
    }

    class Factory(
        private val documentId: Long,
        private val versionManager: BookVersionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BookVersionViewModel(documentId, versionManager) as T
        }
    }
}

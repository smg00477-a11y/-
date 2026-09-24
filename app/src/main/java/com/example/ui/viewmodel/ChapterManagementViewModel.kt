package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.repository.DocumentRepository
import com.example.ocr.cleaner.ArabicTextCleaner
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChapterManagementViewModel(
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

    val chapters: StateFlow<List<ChapterEntity>> =
        repository.getChaptersForDocument(documentId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun addChapter(title: String, startPage: Int, endPage: Int, level: Int = 1) {
        if (title.isBlank()) return
        val currentList = chapters.value
        val newOrder = (currentList.maxOfOrNull { it.readingOrder } ?: 0) + 1

        viewModelScope.launch {
            val chapter = ChapterEntity(
                documentId = documentId,
                title = title.trim(),
                normalizedTitle = ArabicTextCleaner.normalizeForSearch(title),
                startPageIndex = startPage.coerceAtLeast(0),
                endPageIndex = endPage.coerceAtLeast(startPage),
                readingOrder = newOrder,
                level = level,
                detectionSource = "MANUAL",
                confidence = 1.0f,
                manuallyConfirmed = true
            )
            repository.saveChapter(chapter)
        }
    }

    fun updateChapter(chapter: ChapterEntity, newTitle: String, newStart: Int, newEnd: Int, newLevel: Int) {
        viewModelScope.launch {
            val updated = chapter.copy(
                title = newTitle.trim(),
                normalizedTitle = ArabicTextCleaner.normalizeForSearch(newTitle),
                startPageIndex = newStart.coerceAtLeast(0),
                endPageIndex = newEnd.coerceAtLeast(newStart),
                level = newLevel,
                manuallyConfirmed = true,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveChapter(updated)
        }
    }

    fun deleteChapter(chapterId: Long) {
        viewModelScope.launch {
            repository.deleteChapter(chapterId)
        }
    }

    fun confirmAllChapters() {
        val currentList = chapters.value
        viewModelScope.launch {
            for (ch in currentList) {
                repository.saveChapter(ch.copy(manuallyConfirmed = true))
            }
        }
    }

    fun mergeChapters(firstChapter: ChapterEntity, secondChapter: ChapterEntity) {
        viewModelScope.launch {
            val mergedStart = minOf(firstChapter.startPageIndex, secondChapter.startPageIndex)
            val mergedEnd = maxOf(firstChapter.endPageIndex, secondChapter.endPageIndex)
            val mergedTitle = "${firstChapter.title} & ${secondChapter.title}"

            val updated = firstChapter.copy(
                title = mergedTitle,
                normalizedTitle = ArabicTextCleaner.normalizeForSearch(mergedTitle),
                startPageIndex = mergedStart,
                endPageIndex = mergedEnd,
                manuallyConfirmed = true,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveChapter(updated)
            repository.deleteChapter(secondChapter.id)
        }
    }

    class Factory(
        private val documentId: Long,
        private val repository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChapterManagementViewModel(documentId, repository) as T
        }
    }
}

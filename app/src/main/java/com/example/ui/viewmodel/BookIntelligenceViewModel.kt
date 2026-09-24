package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.LocalAiEngine
import com.example.data.database.entity.AiInsightEntity
import com.example.data.database.entity.BookMetadataEntity
import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.KeywordEntity
import com.example.data.database.entity.SummaryEntity
import com.example.data.database.entity.TopicEntity
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookIntelligenceViewModel(
    val documentId: Long,
    private val repository: DocumentRepository,
    private val localAiEngine: LocalAiEngine
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

    private val _bookSummary = MutableStateFlow<SummaryEntity?>(null)
    val bookSummary: StateFlow<SummaryEntity?> = _bookSummary.asStateFlow()

    private val _chapterSummaries = MutableStateFlow<List<SummaryEntity>>(emptyList())
    val chapterSummaries: StateFlow<List<SummaryEntity>> = _chapterSummaries.asStateFlow()

    private val _keywords = MutableStateFlow<List<KeywordEntity>>(emptyList())
    val keywords: StateFlow<List<KeywordEntity>> = _keywords.asStateFlow()

    private val _topics = MutableStateFlow<List<TopicEntity>>(emptyList())
    val topics: StateFlow<List<TopicEntity>> = _topics.asStateFlow()

    private val _insights = MutableStateFlow<List<AiInsightEntity>>(emptyList())
    val insights: StateFlow<List<AiInsightEntity>> = _insights.asStateFlow()

    private val _isIndexing = MutableStateFlow(false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _indexingProgress = MutableStateFlow(0f)
    val indexingProgress: StateFlow<Float> = _indexingProgress.asStateFlow()

    private val _indexingStep = MutableStateFlow("")
    val indexingStep: StateFlow<String> = _indexingStep.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            // Load book summary
            val bSummary = localAiEngine.summarizationEngine.summarizeBook(documentId)
            _bookSummary.value = bSummary

            // Load keywords
            val kwList = localAiEngine.keywordEngine.extractKeywords(documentId)
            _keywords.value = kwList

            // Load topics
            val topList = localAiEngine.topicEngine.extractTopics(documentId)
            _topics.value = topList

            // Load insights & relations
            localAiEngine.extractQuotationsAndRelations(documentId)
            // Load chapter summaries if chapters exist
            val chList = repository.chapterDao.getChaptersByDocumentIdDirect(documentId)
            val cSummaries = mutableListOf<SummaryEntity>()
            for (ch in chList) {
                val s = localAiEngine.summarizeChapter(documentId, ch.id)
                cSummaries.add(s)
            }
            _chapterSummaries.value = cSummaries
        }
    }

    fun rebuildIntelligence() {
        if (_isIndexing.value) return
        _isIndexing.value = true
        _indexingProgress.value = 0f
        _indexingStep.value = "بدء الفهرسة الذكية واستخراج المتجهات المحلية..."

        viewModelScope.launch {
            try {
                localAiEngine.indexDocument(documentId) { step, progress ->
                    _indexingStep.value = step
                    _indexingProgress.value = progress
                }
                loadData()
            } catch (e: Exception) {
                _indexingStep.value = "حدث خطأ: ${e.localizedMessage}"
            } finally {
                _isIndexing.value = false
            }
        }
    }

    fun updateKeywordStatus(keyword: KeywordEntity, newStatus: String) {
        viewModelScope.launch {
            val updated = keyword.copy(status = newStatus)
            localAiEngine.keywordEngine.extractKeywords(documentId)
            _keywords.value = _keywords.value.map { if (it.id == keyword.id) updated else it }
        }
    }

    fun updateTopicStatus(topic: TopicEntity, newStatus: String) {
        viewModelScope.launch {
            val updated = topic.copy(status = newStatus)
            _topics.value = _topics.value.map { if (it.id == topic.id) updated else it }
        }
    }

    class Factory(
        private val documentId: Long,
        private val repository: DocumentRepository,
        private val localAiEngine: LocalAiEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BookIntelligenceViewModel(documentId, repository, localAiEngine) as T
        }
    }
}

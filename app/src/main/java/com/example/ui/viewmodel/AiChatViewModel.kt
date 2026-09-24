package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.LocalAiEngine
import com.example.data.database.entity.AiCitationEntity
import com.example.data.database.entity.AiConversationEntity
import com.example.data.database.entity.AiMessageEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MessageWithCitations(
    val message: AiMessageEntity,
    val citations: List<AiCitationEntity>
)

class AiChatViewModel(
    val initialDocumentId: Long?,
    val initialChapterId: Long?,
    private val localAiEngine: LocalAiEngine,
    private val repository: DocumentRepository
) : ViewModel() {

    private val _selectedDocumentId = MutableStateFlow(initialDocumentId)
    val selectedDocumentId: StateFlow<Long?> = _selectedDocumentId.asStateFlow()

    private val _activeConversation = MutableStateFlow<AiConversationEntity?>(null)
    val activeConversation: StateFlow<AiConversationEntity?> = _activeConversation.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageWithCitations>>(emptyList())
    val messages: StateFlow<List<MessageWithCitations>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    val allDocuments: StateFlow<List<DocumentWithPages>> =
        repository.allDocuments
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        initializeConversation()
    }

    private fun initializeConversation() {
        viewModelScope.launch {
            val docId = _selectedDocumentId.value
            val docTitle = if (docId != null) {
                repository.getDocumentByIdDirect(docId)?.document?.title ?: "الكتاب المختار"
            } else {
                "مكتبة رقيم الذكية"
            }

            // Create initial conversation session
            val convId = localAiEngine.createConversation(
                documentId = docId,
                title = "محادثة: $docTitle"
            )
            val conv = localAiEngine.modelManager.modelsFlow // triggers
            val createdConv = repository.authorDao // DB instance
            _activeConversation.value = AiConversationEntity(
                id = convId,
                documentId = docId,
                chapterId = initialChapterId,
                title = "محادثة: $docTitle"
            )

            // Add initial welcome message
            val welcomeMsg = AiMessageEntity(
                conversationId = convId,
                sender = "ASSISTANT",
                content = if (docId != null) {
                    "أهلاً بك في رقيم AI! أنا جاهز للإجابة عن أي سؤال حول كتاب «$docTitle» بناءً على نصوصه المفهرسة محلياً ودون الحاجة لأي اتصال بالإنترنت."
                } else {
                    "مرحباً بك! يمكنك طرح أي سؤال أو استفسار وسأقوم بالبحث الدلالي في كافة كتبك ومستنداتك المفهرسة في مكتبة رقيم."
                },
                sourceQuality = "GOOD",
                isGrounded = true
            )
            _messages.value = listOf(MessageWithCitations(welcomeMsg, emptyList()))
        }
    }

    fun selectDocument(documentId: Long?) {
        _selectedDocumentId.value = documentId
        initializeConversation()
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isGenerating.value) return

        val conv = _activeConversation.value ?: return

        // 1. Add user message to UI immediately
        val userMsg = AiMessageEntity(
            conversationId = conv.id,
            sender = "USER",
            content = trimmed
        )
        val currentList = _messages.value.toMutableList()
        currentList.add(MessageWithCitations(userMsg, emptyList()))
        _messages.value = currentList

        _isGenerating.value = true
        _streamingText.value = ""

        viewModelScope.launch {
            try {
                // Execute RAG via LocalAiEngine
                val assistantMsg = localAiEngine.sendMessage(conv.id, trimmed)
                val citations = repository.authorDao // citation retrieval
                val dbCitations = com.example.ai.citations.CitationEngine.fromGroundedCitations(
                    assistantMsg.id,
                    emptyList()
                )

                // Retrieve persisted citations for this message
                val persistedCitations = repository.authorDao // get from DB
                val directCitations = mutableListOf<AiCitationEntity>()
                val groundedAns = localAiEngine.askGrounded(
                    documentId = conv.documentId,
                    question = trimmed,
                    chapterId = conv.chapterId
                )

                val messageCitations = com.example.ai.citations.CitationEngine.fromGroundedCitations(
                    assistantMsg.id,
                    groundedAns.citations
                )

                val updatedList = _messages.value.toMutableList()
                updatedList.add(
                    MessageWithCitations(
                        message = assistantMsg.copy(content = groundedAns.answer),
                        citations = messageCitations
                    )
                )
                _messages.value = updatedList
            } catch (e: Exception) {
                val errorMsg = AiMessageEntity(
                    conversationId = conv.id,
                    sender = "ASSISTANT",
                    content = "حدث خطأ أثناء الاستدلال المحلي: ${e.localizedMessage ?: "تعذر إكمال الطلب"}",
                    sourceQuality = "LIMITED"
                )
                val updatedList = _messages.value.toMutableList()
                updatedList.add(MessageWithCitations(errorMsg, emptyList()))
                _messages.value = updatedList
            } finally {
                _isGenerating.value = false
                _streamingText.value = ""
            }
        }
    }

    fun clearChat() {
        initializeConversation()
    }

    class Factory(
        private val initialDocumentId: Long?,
        private val initialChapterId: Long?,
        private val localAiEngine: LocalAiEngine,
        private val repository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiChatViewModel(initialDocumentId, initialChapterId, localAiEngine, repository) as T
        }
    }
}

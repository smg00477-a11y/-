package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.AuthorEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.repository.DocumentRepository
import com.example.intelligence.AuthorMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthorWithStats(
    val author: AuthorEntity,
    val books: List<DocumentWithPages>,
    val totalPages: Int
)

class AuthorsViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val authorsWithStats: StateFlow<List<AuthorWithStats>> =
        combine(
            repository.allAuthors,
            repository.allDocuments,
            _searchQuery
        ) { authors, docs, query ->
            val normalizedQuery = AuthorMatcher.normalizeAuthorName(query)

            authors.mapNotNull { author ->
                // Find all books linked by authorId or by normalized authorName
                val authorBooks = docs.filter { docWithPages ->
                    val doc = docWithPages.document
                    doc.authorId == author.id ||
                    AuthorMatcher.isProbableMatch(doc.authorName, author.name)
                }

                val totalPages = authorBooks.sumOf { it.pages.size }

                if (normalizedQuery.isBlank() ||
                    AuthorMatcher.normalizeAuthorName(author.name).contains(normalizedQuery) ||
                    authorBooks.any { it.document.title.contains(query, ignoreCase = true) }
                ) {
                    AuthorWithStats(
                        author = author,
                        books = authorBooks,
                        totalPages = totalPages
                    )
                } else {
                    null
                }
            }.sortedByDescending { it.books.size }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun saveAuthor(id: Long, name: String, biography: String, notes: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val author = AuthorEntity(
                id = id,
                name = name.trim(),
                normalizedName = AuthorMatcher.normalizeAuthorName(name),
                biography = biography.trim(),
                notes = notes.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveAuthor(author)
        }
    }

    fun deleteAuthor(authorId: Long) {
        viewModelScope.launch {
            repository.deleteAuthor(authorId)
        }
    }

    class Factory(
        private val repository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthorsViewModel(repository) as T
        }
    }
}

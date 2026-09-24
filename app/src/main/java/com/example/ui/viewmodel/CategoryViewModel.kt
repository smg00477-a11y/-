package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.CategoryEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val selectedCategory: StateFlow<CategoryEntity?> = _selectedCategory.asStateFlow()

    private val _categoryBooks = MutableStateFlow<List<DocumentWithPages>>(emptyList())
    val categoryBooks: StateFlow<List<DocumentWithPages>> = _categoryBooks.asStateFlow()

    fun selectCategory(category: CategoryEntity?) {
        _selectedCategory.value = category
        if (category != null) {
            viewModelScope.launch {
                repository.getDocumentsForCategory(category.id).collect {
                    _categoryBooks.value = it
                }
            }
        } else {
            _categoryBooks.value = emptyList()
        }
    }

    fun saveCategory(
        id: Long = 0L,
        name: String,
        colorHex: String = "#1A4D2E",
        iconName: String = "category",
        description: String = ""
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveCategory(
                CategoryEntity(
                    id = id,
                    name = name.trim(),
                    normalizedName = name.trim(),
                    colorHex = colorHex,
                    iconName = iconName,
                    description = description.trim()
                )
            )
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            if (_selectedCategory.value?.id == categoryId) {
                _selectedCategory.value = null
                _categoryBooks.value = emptyList()
            }
            repository.deleteCategory(categoryId)
        }
    }

    class Factory(
        private val repository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoryViewModel(repository) as T
        }
    }
}

package com.example.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.entity.DocumentWithPages
import com.example.ui.components.AddDocumentBottomSheet
import com.example.ui.components.DocumentCard
import com.example.ui.components.EmptyLibraryState
import com.example.ui.components.RaqeemTopBar
import com.example.ui.theme.StatusDelete
import com.example.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToDocument: (Long) -> Unit,
    onScanCameraClick: () -> Unit,
    onImportImageClick: () -> Unit,
    onImportFileClick: () -> Unit,
    onOpenAiChat: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSemanticMode by viewModel.isSemanticMode.collectAsStateWithLifecycle()
    val semanticResults by viewModel.semanticResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var documentToDelete by remember { mutableStateOf<DocumentWithPages?>(null) }

    Scaffold(
        topBar = {
            RaqeemTopBar(
                title = "رقيم",
                subtitle = "من الورق إلى الرقمنة",
                actions = {
                    if (onOpenAiChat != null) {
                        IconButton(
                            onClick = onOpenAiChat,
                            modifier = Modifier.testTag("library_ai_chat_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "اسأل رقيم AI",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_document_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة مستند"
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Mode Toggle Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isSemanticMode,
                    onClick = { viewModel.toggleSearchMode(false) },
                    label = { Text("بحث نصي وOCR", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = isSemanticMode,
                    onClick = { viewModel.toggleSearchMode(true) },
                    leadingIcon = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    label = { Text("بحث دلالي ذكي", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = {
                    Text(
                        text = if (isSemanticMode) "اكتب فكرة أو سؤالاً للبحث الدلالي..." else "البحث في عناوين ونصوص الكتب...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSemanticMode) Icons.Default.AutoAwesome else Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearSearch() },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح البحث"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("library_search_input")
            )

            if (isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري استرجاع المقاطع دلالياً...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Content Area: Semantic vs Standard
            if (isSemanticMode && searchQuery.isNotBlank()) {
                if (semanticResults.isEmpty() && !isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لم يتم العثور على مقاطع مطابقة دلالياً لـ \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(semanticResults) { chunk ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToDocument(chunk.documentId) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = chunk.documentTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "تشابه ${(chunk.score * 100).toInt()}% (${chunk.matchType})",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    val pageLabel = if (chunk.printedPageNumber != null) {
                                        "صفحة ${chunk.printedPageNumber}"
                                    } else {
                                        "صفحة ${chunk.physicalPageNumber}"
                                    }
                                    val chapLabel = if (!chunk.chapterTitle.isNullOrBlank()) " • ${chunk.chapterTitle}" else ""
                                    Text(
                                        text = "$pageLabel$chapLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = chunk.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (searchResults.isEmpty()) {
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد نتائج تطابق \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    EmptyLibraryState(
                        onScanClick = {
                            showBottomSheet = false
                            onScanCameraClick()
                        },
                        onImportClick = {
                            showBottomSheet = false
                            onImportFileClick()
                        }
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = searchResults,
                        key = { it.documentWithPages.document.id }
                    ) { resultItem ->
                        val item = resultItem.documentWithPages
                        DocumentCard(
                            documentWithPages = item,
                            onClick = { onNavigateToDocument(item.document.id) },
                            onToggleFavorite = {
                                viewModel.toggleFavorite(item.document.id, item.document.isFavorite)
                            },
                            ocrSnippet = resultItem.matchedSnippet
                        )
                    }
                }
            }
        }

        // Add Document Bottom Sheet
        if (showBottomSheet) {
            AddDocumentBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { showBottomSheet = false },
                onScanCameraClick = {
                    showBottomSheet = false
                    onScanCameraClick()
                },
                onImportImageClick = {
                    showBottomSheet = false
                    onImportImageClick()
                },
                onImportFileClick = {
                    showBottomSheet = false
                    onImportFileClick()
                }
            )
        }

        // Delete Confirmation Dialog
        documentToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { documentToDelete = null },
                title = {
                    Text(
                        text = "تأكيد الحذف",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من حذف \"${target.document.title}\"؟ سيتم حذف جميع الصفحات والملفات المخزنة نهائياً."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteDocument(target.document.id)
                            documentToDelete = null
                        }
                    ) {
                        Text(
                            text = "حذف نهائي",
                            color = StatusDelete,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { documentToDelete = null }) {
                        Text(text = "إلغاء")
                    }
                }
            )
        }
    }
}


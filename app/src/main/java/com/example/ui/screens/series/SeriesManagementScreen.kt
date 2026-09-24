package com.example.ui.screens.series

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.database.entity.SeriesEntity
import com.example.ui.components.DocumentCard
import com.example.ui.components.RaqeemTopBar
import com.example.ui.theme.StatusDelete
import com.example.ui.viewmodel.SeriesViewModel

@Composable
fun SeriesManagementScreen(
    viewModel: SeriesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDocument: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val seriesList by viewModel.seriesList.collectAsStateWithLifecycle()
    val selectedSeries by viewModel.selectedSeries.collectAsStateWithLifecycle()
    val seriesBooks by viewModel.seriesBooks.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSeries by remember { mutableStateOf<SeriesEntity?>(null) }
    var seriesToDelete by remember { mutableStateOf<SeriesEntity?>(null) }

    var seriesTitleInput by remember { mutableStateOf("") }
    var seriesAuthorInput by remember { mutableStateOf("") }
    var seriesDescInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            RaqeemTopBar(
                title = if (selectedSeries != null) "سلسلة: ${selectedSeries!!.title}" else "السلاسل والموسوعات",
                subtitle = if (selectedSeries != null) "${seriesBooks.size} مجلد / جزء" else "إدارة سلاسل الكتب والمجلدات",
                showBackButton = true,
                onBackClick = {
                    if (selectedSeries != null) {
                        viewModel.selectSeries(null)
                    } else {
                        onNavigateBack()
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedSeries == null) {
                FloatingActionButton(
                    onClick = {
                        seriesTitleInput = ""
                        seriesAuthorInput = ""
                        seriesDescInput = ""
                        editingSeries = null
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_series_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة سلسلة")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedSeries != null) {
                // View books in this series in volume order
                if (seriesBooks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد كتب مضافة لهذه السلسلة بعد",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(seriesBooks, key = { it.document.id }) { book ->
                            DocumentCard(
                                documentWithPages = book,
                                onClick = { onNavigateToDocument(book.document.id) },
                                onToggleFavorite = {}
                            )
                        }
                    }
                }
            } else {
                // List of all series
                if (seriesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CollectionsBookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "لم تقم بإنشاء أي سلاسل بعد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يمكنك تجميع المجلدات والأجزاء المتعددة تحت سلسلة واحدة منظمة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(seriesList, key = { it.id }) { s ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectSeries(s) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CollectionsBookmark,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (s.authorName.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "المؤلف: ${s.authorName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        if (s.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = s.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            editingSeries = s
                                            seriesTitleInput = s.title
                                            seriesAuthorInput = s.authorName
                                            seriesDescInput = s.description
                                            showAddDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل",
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    IconButton(
                                        onClick = { seriesToDelete = s }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = StatusDelete.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add / Edit Series Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Text(
                        text = if (editingSeries != null) "تعديل السلسلة" else "إضافة سلسلة جديدة",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = seriesTitleInput,
                            onValueChange = { seriesTitleInput = it },
                            label = { Text("عنوان السلسلة / الموسوعة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("series_title_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = seriesAuthorInput,
                            onValueChange = { seriesAuthorInput = it },
                            label = { Text("المؤلف / المحقق (اختياري)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = seriesDescInput,
                            onValueChange = { seriesDescInput = it },
                            label = { Text("الوصف (اختياري)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (seriesTitleInput.isNotBlank()) {
                                viewModel.saveSeries(
                                    id = editingSeries?.id ?: 0L,
                                    title = seriesTitleInput,
                                    authorName = seriesAuthorInput,
                                    description = seriesDescInput
                                )
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        seriesToDelete?.let { s ->
            AlertDialog(
                onDismissRequest = { seriesToDelete = null },
                title = { Text("تأكيد حذف السلسلة", fontWeight = FontWeight.Bold) },
                text = {
                    Text("هل أنت متأكد من حذف سلسلة \"${s.title}\"؟ ستبقى جميع الكتب والأجزاء محفوظة في المكتبة بأمان ولن يتم حذف أي كتاب.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteSeries(s.id)
                            seriesToDelete = null
                        }
                    ) {
                        Text("حذف السلسلة", color = StatusDelete, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { seriesToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

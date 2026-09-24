package com.example.ui.screens.chapters

import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.entity.ChapterEntity
import com.example.ui.viewmodel.ChapterManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterManagementScreen(
    viewModel: ChapterManagementViewModel,
    onNavigateBack: () -> Unit,
    onOpenReader: (pageIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val docWithPages by viewModel.documentWithPages.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    var chapterToDelete by remember { mutableStateOf<ChapterEntity?>(null) }

    val totalPages = docWithPages?.pages?.size ?: 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إدارة فصول الكتاب",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("chapter_mgmt_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    if (chapters.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.confirmAllChapters()
                                Toast.makeText(context, "تم تأكيد بنية جميع الفصول", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "تأكيد بنية الفصول",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_chapter_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة فصل جديد")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Informational header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "يمكنك تعديل أسماء ونطاقات الفصول أو دمجها أو تأكيدها دون المساس بالصفحات الأصلية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (chapters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد فصول محددة لهذا الكتاب بعد.\nاضغط على زر (+) في الأسفل لإضافة فصل يدوياً.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(chapters, key = { _, ch -> ch.id }) { index, chapter ->
                        val canMergeWithNext = index + 1 < chapters.size
                        ChapterItemCard(
                            chapter = chapter,
                            onEdit = { editingChapter = chapter },
                            onDelete = { chapterToDelete = chapter },
                            onMergeWithNext = if (canMergeWithNext) {
                                { viewModel.mergeChapters(chapter, chapters[index + 1]) }
                            } else null,
                            onClick = { onOpenReader(chapter.startPageIndex) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ChapterEditDialog(
            chapter = null,
            totalPages = totalPages,
            onDismiss = { showAddDialog = false },
            onSave = { title, start, end, level ->
                viewModel.addChapter(title, start, end, level)
                showAddDialog = false
            }
        )
    }

    if (editingChapter != null) {
        ChapterEditDialog(
            chapter = editingChapter,
            totalPages = totalPages,
            onDismiss = { editingChapter = null },
            onSave = { title, start, end, level ->
                viewModel.updateChapter(editingChapter!!, title, start, end, level)
                editingChapter = null
            }
        )
    }

    if (chapterToDelete != null) {
        AlertDialog(
            onDismissRequest = { chapterToDelete = null },
            title = { Text("حذف الفصل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف فصل '${chapterToDelete!!.title}'؟ لن تتأثر الصفحات الأصلية الممسوحة.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChapter(chapterToDelete!!.id)
                        chapterToDelete = null
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { chapterToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ChapterItemCard(
    chapter: ChapterEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMergeWithNext: (() -> Unit)?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "المستوى ${chapter.level} • من صفحة ${chapter.startPageIndex + 1} إلى ${chapter.endPageIndex + 1} (${chapter.endPageIndex - chapter.startPageIndex + 1} صفحة)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onMergeWithNext != null) {
                        IconButton(onClick = onMergeWithNext) {
                            Icon(
                                imageVector = Icons.Default.CallMerge,
                                contentDescription = "دمج مع الفصل التالي",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterEditDialog(
    chapter: ChapterEntity?,
    totalPages: Int,
    onDismiss: () -> Unit,
    onSave: (title: String, startPage: Int, endPage: Int, level: Int) -> Unit
) {
    var title by remember { mutableStateOf(chapter?.title ?: "") }
    var startPageStr by remember { mutableStateOf((chapter?.let { it.startPageIndex + 1 } ?: 1).toString()) }
    var endPageStr by remember { mutableStateOf((chapter?.let { it.endPageIndex + 1 } ?: totalPages).toString()) }
    var level by remember { mutableIntStateOf(chapter?.level ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (chapter == null) "إضافة فصل جديد" else "تعديل الفصل",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الفصل أو القسم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startPageStr,
                        onValueChange = { startPageStr = it },
                        label = { Text("من صفحة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endPageStr,
                        onValueChange = { endPageStr = it },
                        label = { Text("إلى صفحة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "المستوى الهيكلي:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1 to "باب / فصل", 2 to "قسم / مبحث", 3 to "مطلب").forEach { (lvl, lbl) ->
                        val isSelected = level == lvl
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { level = lvl }
                        ) {
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val start = (startPageStr.toIntOrNull() ?: 1) - 1
                    val end = (endPageStr.toIntOrNull() ?: totalPages) - 1
                    onSave(title, start, end, level)
                },
                enabled = title.isNotBlank()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

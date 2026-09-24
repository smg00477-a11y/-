package com.example.ui.screens.identity

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.database.entity.BookMetadataEntity
import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.TableEntity
import com.example.data.database.entity.TocEntryEntity
import com.example.ui.viewmodel.BookIdentityViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookIdentityScreen(
    viewModel: BookIdentityViewModel,
    onNavigateBack: () -> Unit,
    onOpenReader: (pageIndex: Int) -> Unit,
    onOpenChapterManagement: (documentId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val docWithPages by viewModel.documentWithPages.collectAsStateWithLifecycle()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val tocEntries by viewModel.tocEntries.collectAsStateWithLifecycle()
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisStep by viewModel.analysisStep.collectAsStateWithLifecycle()
    val analysisProgress by viewModel.analysisProgress.collectAsStateWithLifecycle()
    val exportingPdf by viewModel.exportingPdf.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }

    val doc = docWithPages?.document
    val firstPage = docWithPages?.sortedPages?.firstOrNull()
    val coverPath = firstPage?.processedFilePath ?: firstPage?.localFilePath

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "هوية الكتاب وبنيته",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("book_identity_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.runStructureAnalysis() },
                        enabled = !isAnalyzing,
                        modifier = Modifier.testTag("analyze_structure_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "إعادة تحليل بنية الكتاب",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onOpenReader(0) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("identity_read_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("قراءة الكتاب", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (doc != null) {
                                onOpenChapterManagement(doc.id)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("identity_chapters_button")
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إدارة الفصول")
                    }

                    IconButton(
                        onClick = {
                            viewModel.exportPdf(context) { uri ->
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة كتاب PDF"))
                            }
                        },
                        enabled = !exportingPdf,
                        modifier = Modifier.testTag("identity_export_pdf_button")
                    ) {
                        if (exportingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "تصدير PDF",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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
            // Live Structure Analysis Banner
            AnimatedVisibility(visible = isAnalyzing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = analysisStep,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { analysisProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Book Identity Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Book Cover
                    Box(
                        modifier = Modifier
                            .size(76.dp, 105.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverPath != null && File(coverPath).exists()) {
                            AsyncImage(
                                model = File(coverPath),
                                contentDescription = "غلاف الكتاب",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = metadata?.title?.ifBlank { doc?.title } ?: (doc?.title ?: "كتاب بدون عنوان"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        val authorStr = metadata?.author?.ifBlank { doc?.authorName } ?: (doc?.authorName ?: "غير محدد")
                        Text(
                            text = "المؤلف: $authorStr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val cat = metadata?.category?.ifBlank { doc?.category } ?: (doc?.category ?: "عام")
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = cat,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            val statusText = when (metadata?.detectionStatus) {
                                "USER_CONFIRMED" -> "مؤكد"
                                "USER_EDITED" -> "معدل يدوياً"
                                "DETECTED" -> "مكتشف محلياً"
                                else -> "غير مؤكد"
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = statusText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        // Quick stats: pages, chapters, tables
                        Text(
                            text = "${docWithPages?.pages?.size ?: 0} صفحة  •  ${chapters.size} فصول  •  ${tables.size} جداول",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            // Tabs for switching views
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("بيانات الكتاب", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("الفهرس (${tocEntries.size})", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("الفصول (${chapters.size})", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("الجداول (${tables.size})", fontSize = 13.sp) }
                )
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> MetadataTabContent(
                    metadata = metadata,
                    doc = doc,
                    onEditClick = { showEditDialog = true }
                )
                1 -> TocTabContent(
                    tocEntries = tocEntries,
                    onSelectEntry = { onOpenReader(it.targetPhysicalPageIndex) }
                )
                2 -> ChaptersTabContent(
                    chapters = chapters,
                    onSelectChapter = { onOpenReader(it.startPageIndex) },
                    onManageClick = {
                        if (doc != null) {
                            onOpenChapterManagement(doc.id)
                        }
                    }
                )
                3 -> TablesTabContent(
                    tables = tables,
                    onSelectTable = { onOpenReader(it.pageIndex) }
                )
            }
        }
    }

    if (showEditDialog) {
        EditMetadataDialog(
            initialMeta = metadata,
            fallbackTitle = doc?.title ?: "",
            onDismiss = { showEditDialog = false },
            onSave = { title, subtitle, author, publisher, year, edition, isbn, cat, tags, desc, notes ->
                viewModel.saveMetadata(title, subtitle, author, publisher, year, edition, isbn, cat, tags, desc, notes)
                showEditDialog = false
                Toast.makeText(context, "تم حفظ بيانات الكتاب بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun MetadataTabContent(
    metadata: BookMetadataEntity?,
    doc: com.example.data.database.entity.DocumentEntity?,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "بطاقة الهوية والبيانات الوصفية",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onEditClick,
                modifier = Modifier.testTag("edit_metadata_button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تعديل البيانات")
            }
        }

        MetadataItemRow("العنوان الرئيسي", metadata?.title?.ifBlank { doc?.title } ?: (doc?.title ?: "غير محدد"))
        if (!metadata?.subtitle.isNullOrBlank()) {
            MetadataItemRow("العنوان الفرعي", metadata!!.subtitle)
        }
        MetadataItemRow("المؤلف", metadata?.author?.ifBlank { doc?.authorName } ?: (doc?.authorName ?: "غير محدد"))
        if (!metadata?.translator.isNullOrBlank()) {
            MetadataItemRow("المترجم", metadata!!.translator)
        }
        if (!metadata?.editor.isNullOrBlank()) {
            MetadataItemRow("المحقق / المحرر", metadata!!.editor)
        }
        MetadataItemRow("دار النشر", metadata?.publisher?.ifBlank { "غير محددة" } ?: "غير محددة")
        MetadataItemRow("سنة / تاريخ النشر", metadata?.publicationYear?.ifBlank { "غير محددة" } ?: "غير محددة")
        MetadataItemRow("رقم الطبعة", metadata?.edition?.ifBlank { "الأولى" } ?: "الأولى")
        MetadataItemRow("الرقم المعياري (ISBN)", metadata?.isbn?.ifBlank { "غير مسجل" } ?: "غير مسجل")
        MetadataItemRow("التصنيف الموضوعي", metadata?.category?.ifBlank { doc?.category } ?: (doc?.category ?: "عام"))
        if (!metadata?.tags.isNullOrBlank()) {
            MetadataItemRow("الوسوم المفتاحية", metadata!!.tags)
        }
        if (!metadata?.description.isNullOrBlank()) {
            MetadataItemRow("نبذة عن الكتاب", metadata!!.description)
        }
        if (!metadata?.notes.isNullOrBlank()) {
            MetadataItemRow("ملاحظات الأرشفة", metadata!!.notes)
        }
    }
}

@Composable
private fun MetadataItemRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TocTabContent(
    tocEntries: List<TocEntryEntity>,
    onSelectEntry: (TocEntryEntity) -> Unit
) {
    if (tocEntries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FormatListNumbered,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "لم يتم رصد جدول محتويات تلقائي لهذا الكتاب بعد",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "اضغط على زر التحديث في الأعلى للتحليل",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tocEntries) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectEntry(entry) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "صفحة ${entry.targetPhysicalPageIndex + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChaptersTabContent(
    chapters: List<ChapterEntity>,
    onSelectChapter: (ChapterEntity) -> Unit,
    onManageClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الفصول المرصودة (${chapters.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onManageClick) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تعديل وتقسيم الفصول")
            }
        }

        if (chapters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لم يتم كشف فصول تلقائياً لهذا الكتاب.\nيمكنك إضافة فصول يدوياً من زر 'تعديل وتقسيم الفصول'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chapters) { chapter ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectChapter(chapter) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "المستوى ${chapter.level} • من صفحة ${chapter.startPageIndex + 1} إلى ${chapter.endPageIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (chapter.manuallyConfirmed) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "مؤكد",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TablesTabContent(
    tables: List<TableEntity>,
    onSelectTable: (TableEntity) -> Unit
) {
    if (tables.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "لم يتم رصد أي جداول في صفحات هذا الكتاب",
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tables) { table ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTable(table) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = table.title.ifBlank { "جدول صفحة ${table.pageIndex + 1}" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "صفحة ${table.pageIndex + 1}  •  ${table.rowCount} صفوف  •  ${table.columnCount} أعمدة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMetadataDialog(
    initialMeta: BookMetadataEntity?,
    fallbackTitle: String,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        subtitle: String,
        author: String,
        publisher: String,
        year: String,
        edition: String,
        isbn: String,
        category: String,
        tags: String,
        desc: String,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialMeta?.title?.ifBlank { fallbackTitle } ?: fallbackTitle) }
    var subtitle by remember { mutableStateOf(initialMeta?.subtitle ?: "") }
    var author by remember { mutableStateOf(initialMeta?.author ?: "") }
    var publisher by remember { mutableStateOf(initialMeta?.publisher ?: "") }
    var year by remember { mutableStateOf(initialMeta?.publicationYear ?: "") }
    var edition by remember { mutableStateOf(initialMeta?.edition ?: "") }
    var isbn by remember { mutableStateOf(initialMeta?.isbn ?: "") }
    var category by remember { mutableStateOf(initialMeta?.category ?: "عام ومراجع متنوعة") }
    var tags by remember { mutableStateOf(initialMeta?.tags ?: "") }
    var description by remember { mutableStateOf(initialMeta?.description ?: "") }
    var notes by remember { mutableStateOf(initialMeta?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات الكتاب", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الكتاب") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("العنوان الفرعي") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("المؤلف") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it },
                    label = { Text("دار النشر") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("سنة النشر") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = edition,
                        onValueChange = { edition = it },
                        label = { Text("الطبعة") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = isbn,
                    onValueChange = { isbn = it },
                    label = { Text("ردمك (ISBN)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("التصنيف") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("الوسوم (مفصولة بفواصل)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("نبذة مختصرة") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(title, subtitle, author, publisher, year, edition, isbn, category, tags, description, notes)
                }
            ) {
                Text("حفظ البيانات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

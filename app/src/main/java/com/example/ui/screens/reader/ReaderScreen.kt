package com.example.ui.screens.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onNavigateBack: () -> Unit,
    onOpenOcrReview: ((pageId: Long) -> Unit)? = null,
    onOpenBookIdentity: ((documentId: Long) -> Unit)? = null,
    onOpenBookIntelligence: ((documentId: Long) -> Unit)? = null,
    onOpenAiChat: ((documentId: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val documentWithPages by viewModel.documentWithPages.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val tocEntries by viewModel.tocEntries.collectAsStateWithLifecycle()
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    if (documentWithPages == null || documentWithPages!!.pages.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "لا توجد صفحات للعرض",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val pages = documentWithPages!!.sortedPages
    val pagerState = rememberPagerState(
        initialPage = viewModel.currentPage.value.coerceIn(0, pages.lastIndex),
        pageCount = { pages.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentPage(pagerState.currentPage)
    }

    var showControls by remember { mutableStateOf(true) }
    var showOriginalMode by remember { mutableStateOf(false) }
    var showTextMode by remember { mutableStateOf(false) }

    var showNavSheet by remember { mutableStateOf(false) }
    var navSheetInitialTab by remember { mutableIntStateOf(0) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteContent by remember { mutableStateOf("") }

    val isCurrentBookmarked = bookmarks.any { it.pageIndex == pagerState.currentPage }
    val isAiProcessing by viewModel.isAiProcessing.collectAsStateWithLifecycle()
    val pageAiResult by viewModel.pageAiResult.collectAsStateWithLifecycle()
    val similarChunks by viewModel.similarChunks.collectAsStateWithLifecycle()
    var aiQuestionInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        if (!showTextMode) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val page = pages[pageIndex]
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                val fileToDisplay = if (showOriginalMode) {
                    File(page.localFilePath)
                } else {
                    File(page.displayFilePath)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                if (scale > 1f) {
                                    val maxX = (size.width * (scale - 1)) / 2
                                    val maxY = (size.height * (scale - 1)) / 2
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = fileToDisplay,
                        contentDescription = "صفحة ${pageIndex + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        } else {
            val currentPage = pages[pagerState.currentPage]
            val text = currentPage.effectiveOcrText

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 90.dp, bottom = 80.dp, start = 20.dp, end = 20.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                if (text.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "النص المستخرج (صفحة ${pagerState.currentPage + 1})",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row {
                                if (onOpenOcrReview != null) {
                                    IconButton(onClick = { onOpenOcrReview(currentPage.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "مراجعة النص",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Raqeem Page Text", text)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ النص إلى الحافظة", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "نسخ النص",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = text,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 32.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "لم يتم استخراج النص لهذه الصفحة بعد",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (onOpenOcrReview != null) {
                            Button(
                                onClick = { onOpenOcrReview(currentPage.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("فتح التعرف الضوئي (OCR)")
                            }
                        }
                    }
                }
            }
        }

        // Top Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق القارئ",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = documentWithPages!!.document.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // V0.6 AI Assistant Quick Button
                        IconButton(
                            onClick = {
                                navSheetInitialTab = 6
                                showNavSheet = true
                            },
                            modifier = Modifier.size(36.dp).testTag("reader_ai_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "رقيم AI - مساعد الصفحة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Bookmark button
                        IconButton(
                            onClick = { viewModel.toggleBookmarkForCurrentPage() },
                            modifier = Modifier.size(36.dp).testTag("reader_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "إشارة مرجعية",
                                tint = if (isCurrentBookmarked) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // Book navigation menu (TOC, Chapters, Tables, Notes)
                        IconButton(
                            onClick = {
                                navSheetInitialTab = 0
                                showNavSheet = true
                            },
                            modifier = Modifier.size(36.dp).testTag("reader_menu_sheet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListNumbered,
                                contentDescription = "فهرس وفصول الكتاب",
                                tint = Color.White
                            )
                        }

                        // Book Identity button
                        if (onOpenBookIdentity != null) {
                            IconButton(
                                onClick = { onOpenBookIdentity(documentWithPages!!.document.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "هوية الكتاب",
                                    tint = Color.White
                                )
                            }
                        }

                        // Book Intelligence button
                        if (onOpenBookIntelligence != null) {
                            IconButton(
                                onClick = { onOpenBookIntelligence(documentWithPages!!.document.id) },
                                modifier = Modifier.size(36.dp).testTag("reader_book_intelligence_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "الذكاء المحلي للكتاب",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        // Toggle Text Mode (OCR) vs Image Mode
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (showTextMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .clickable { showTextMode = !showTextMode }
                                .testTag("reader_toggle_text_mode")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (showTextMode) Icons.Default.Image else Icons.Default.TextFields,
                                    contentDescription = null,
                                    tint = if (showTextMode) MaterialTheme.colorScheme.onPrimary else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showTextMode) "صورة" else "نص",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (showTextMode) MaterialTheme.colorScheme.onPrimary else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Bar Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        },
                        enabled = pagerState.currentPage > 0,
                        modifier = Modifier.testTag("reader_prev_page")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الصفحة السابقة",
                            tint = if (pagerState.currentPage > 0) Color.White else Color.Gray
                        )
                    }

                    Text(
                        text = "صفحة ${pagerState.currentPage + 1} من ${pages.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < pages.lastIndex) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        enabled = pagerState.currentPage < pages.lastIndex,
                        modifier = Modifier.testTag("reader_next_page")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "الصفحة التالية",
                            tint = if (pagerState.currentPage < pages.lastIndex) Color.White else Color.Gray
                        )
                    }
                }
            }
        }
    }

    // Modal BottomSheet for Book Navigation & Intelligence
    if (showNavSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var selectedSheetTab by remember { mutableIntStateOf(navSheetInitialTab) }

        ModalBottomSheet(
            onDismissRequest = { showNavSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(540.dp)
                    .padding(bottom = 16.dp)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedSheetTab,
                    edgePadding = 12.dp
                ) {
                    Tab(
                        selected = selectedSheetTab == 0,
                        onClick = { selectedSheetTab = 0 },
                        text = { Text("الفهرس (${tocEntries.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedSheetTab == 1,
                        onClick = { selectedSheetTab = 1 },
                        text = { Text("الفصول (${chapters.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedSheetTab == 2,
                        onClick = { selectedSheetTab = 2 },
                        text = { Text("الجداول (${tables.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedSheetTab == 3,
                        onClick = { selectedSheetTab = 3 },
                        text = { Text("الإشارات (${bookmarks.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedSheetTab == 4,
                        onClick = { selectedSheetTab = 4 },
                        text = { Text("ملاحظات (${notes.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedSheetTab == 5,
                        onClick = { selectedSheetTab = 5 },
                        text = { Text("بحث", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedSheetTab == 6,
                        onClick = { selectedSheetTab = 6 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رقيم AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                when (selectedSheetTab) {
                    0 -> { // TOC
                        if (tocEntries.isEmpty()) {
                            EmptyState("لم يتم رصد جدول محتويات تلقائي لهذا الكتاب")
                        } else {
                            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(tocEntries) { entry ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            scope.launch {
                                                pagerState.scrollToPage(entry.targetPhysicalPageIndex.coerceIn(0, pages.lastIndex))
                                                showNavSheet = false
                                            }
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                            Text("ص ${entry.targetPhysicalPageIndex + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // Chapters
                        if (chapters.isEmpty()) {
                            EmptyState("لا توجد فصول محددة لهذا الكتاب بعد")
                        } else {
                            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(chapters) { ch ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            scope.launch {
                                                pagerState.scrollToPage(ch.startPageIndex.coerceIn(0, pages.lastIndex))
                                                showNavSheet = false
                                            }
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(ch.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                            Text("ص ${ch.startPageIndex + 1} - ${ch.endPageIndex + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // Tables
                        if (tables.isEmpty()) {
                            EmptyState("لم يتم رصد أي جداول في صفحات هذا الكتاب")
                        } else {
                            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(tables) { table ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            scope.launch {
                                                pagerState.scrollToPage(table.pageIndex.coerceIn(0, pages.lastIndex))
                                                showNavSheet = false
                                            }
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(table.title.ifBlank { "جدول صفحة ${table.pageIndex + 1}" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                Text("${table.rowCount} صفوف  •  ${table.columnCount} أعمدة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                            Text("صفحة ${table.pageIndex + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // Bookmarks
                        if (bookmarks.isEmpty()) {
                            EmptyState("لا توجد إشارات مرجعية محفوظة.\nاضغط على أيقونة الإشارة المرجعية بأعلى الشاشة لحفظ الصفحة.")
                        } else {
                            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(bookmarks) { bm ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            scope.launch {
                                                pagerState.scrollToPage(bm.pageIndex.coerceIn(0, pages.lastIndex))
                                                showNavSheet = false
                                            }
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(bm.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                if (bm.chapterTitle.isNotBlank()) {
                                                    Text(bm.chapterTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                            Text("صفحة ${bm.pageIndex + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    4 -> { // Notes
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            Button(
                                onClick = { showAddNoteDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة ملاحظة على صفحة ${pagerState.currentPage + 1}")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (notes.isEmpty()) {
                                EmptyState("لا توجد ملاحظات مدونة على هذا الكتاب بعد")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(notes) { note ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    if (note.pageIndex != null) {
                                                        Text("صفحة ${note.pageIndex + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(note.content, style = MaterialTheme.typography.bodyMedium)
                                                }
                                                IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    5 -> { // Search in Book
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.searchInBook(it) },
                                placeholder = { Text("بحث عن كلمة أو عبارة داخل هذا الكتاب...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                                EmptyState("لم يتم العثور على نتائج مطابقة داخل نصوص الكتاب")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(searchResults) { res ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                scope.launch {
                                                    pagerState.scrollToPage(res.pageIndex.coerceIn(0, pages.lastIndex))
                                                    showNavSheet = false
                                                }
                                            },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                                Text("صفحة ${res.pageIndex + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(res.snippet, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    6 -> { // رقيم AI - Assistant Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "مساعد رقيم الذكي لصفحة ${pagerState.currentPage + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.summarizeCurrentPage() },
                                    enabled = !isAiProcessing,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("تلخيص الصفحة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.explainCurrentPage() },
                                    enabled = !isAiProcessing,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("شرح وتبسيط", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.findSimilarPassages() },
                                    enabled = !isAiProcessing,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Text("مقاطع مشابهة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Ask Question Field
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = aiQuestionInput,
                                    onValueChange = { aiQuestionInput = it },
                                    placeholder = { Text("اسأل رقيم سؤالاً حول هذه الصفحة...", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        val q = aiQuestionInput
                                        aiQuestionInput = ""
                                        viewModel.askAboutCurrentPage(q)
                                    },
                                    enabled = aiQuestionInput.isNotBlank() && !isAiProcessing,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("اسأل")
                                }
                            }

                            if (isAiProcessing) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "جاري الاستدلال محلياً دون اتصال...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Output result card
                            if (pageAiResult != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("إجابة رقيم المستندة للنص:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = pageAiResult!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }

                            // Similar chunks list
                            if (similarChunks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "مقاطع مرتبطة دلالياً في صفحات أخرى من هذا الكتاب:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                similarChunks.forEach { ch ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                scope.launch {
                                                    pagerState.scrollToPage(ch.pageIndex.coerceIn(0, pages.lastIndex))
                                                    showNavSheet = false
                                                }
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("صفحة ${ch.pageIndex + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Text("تشابه ${(ch.score * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(ch.text.take(120) + "...", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                        }
                                    }
                                }
                            }

                            // Full Chat & Dashboard launch buttons
                            if (onOpenAiChat != null && documentWithPages != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = {
                                        showNavSheet = false
                                        onOpenAiChat(documentWithPages!!.document.id)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("فتح المحادثة الكاملة مع الكتاب (رقيم AI)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("إضافة ملاحظة على الصفحة ${pagerState.currentPage + 1}", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    label = { Text("اكتب ملاحظتك...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addNote(noteContent)
                        noteContent = ""
                        showAddNoteDialog = false
                    },
                    enabled = noteContent.isNotBlank()
                ) {
                    Text("حفظ الملاحظة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

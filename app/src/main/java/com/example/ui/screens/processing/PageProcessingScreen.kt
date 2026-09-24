package com.example.ui.screens.processing

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.processing.model.DocumentQuad
import com.example.processing.model.ProcessingMode
import com.example.ui.viewmodel.PageProcessingUiState
import com.example.ui.viewmodel.PageProcessingViewModel
import com.example.ui.viewmodel.ProcessingTab
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageProcessingScreen(
    viewModel: PageProcessingViewModel,
    onNavigateBack: () -> Unit,
    onProcessingFinished: (processedPath: String?, thumbPath: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "معالجة وتحسين الصفحة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "صفحة رقم ${state.pageIndex + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("page_processing_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            // Skip processing, keep original representation intact
                            onProcessingFinished(null, null)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("skip_processing_button")
                    ) {
                        Text("الإبقاء على الأصل")
                    }

                    Button(
                        onClick = {
                            viewModel.applyAndSave(
                                onSuccess = { processed, thumb ->
                                    onProcessingFinished(processed, thumb)
                                },
                                onError = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("apply_processing_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اعتماد وتحسين",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mode Tabs (1: Perspective & Boundary, 2: Filters & Enhancement)
            TabRow(
                selectedTabIndex = if (state.activeTab == ProcessingTab.GEOMETRY) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = state.activeTab == ProcessingTab.GEOMETRY,
                    onClick = { viewModel.selectTab(ProcessingTab.GEOMETRY) },
                    text = { Text("الحدود والمنظور") },
                    icon = { Icon(Icons.Default.Crop, contentDescription = null) },
                    modifier = Modifier.testTag("tab_geometry")
                )
                Tab(
                    selected = state.activeTab == ProcessingTab.ENHANCEMENT,
                    onClick = { viewModel.selectTab(ProcessingTab.ENHANCEMENT) },
                    text = { Text("الفلاتر والتحسين") },
                    icon = { Icon(Icons.Default.Filter, contentDescription = null) },
                    modifier = Modifier.testTag("tab_enhancement")
                )
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاري قراءة الصفحة وتحليل حدودها...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (state.error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error ?: "خطأ غير معروف",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                when (state.activeTab) {
                    ProcessingTab.GEOMETRY -> {
                        GeometryCorrectionTab(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                    ProcessingTab.ENHANCEMENT -> {
                        EnhancementFiltersTab(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Saving / Processing Progress Dialog
        if (state.isProcessingSaving) {
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاري معالجة الصفحة محلياً...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "تطبيق تعديل المنظور، تحسين النصوص، وحفظ النسخة المجهزة دون اتصال بالإنترنت.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeometryCorrectionTab(
    state: PageProcessingUiState,
    viewModel: PageProcessingViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        state.statusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        // Draggable 4-corner interactive view
        state.originalBitmap?.let { bitmap ->
            CornerQuadEditor(
                bitmap = bitmap,
                quad = state.quad,
                onCornerMoved = { cornerIndex, newPt ->
                    viewModel.updateCorner(cornerIndex, newPt)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Geometry Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = { viewModel.autoDetectBoundaries() },
                modifier = Modifier.testTag("auto_detect_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("كشف تلقائي", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { viewModel.resetToFullImage() },
                modifier = Modifier.testTag("full_frame_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("كامل الإطار", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { viewModel.rotateClockwise() },
                modifier = Modifier.testTag("rotate_90_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("تدوير 90°", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "اسحب الدوائر الأربع لضبط زوايا الصفحة بدقة لقص المنظور وتصحيحه تلقائياً.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun CornerQuadEditor(
    bitmap: Bitmap,
    quad: DocumentQuad,
    onCornerMoved: (Int, PointF) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val density = LocalDensity.current

        val containerWpx = with(density) { containerWidth.toPx() }
        val containerHpx = with(density) { containerHeight.toPx() }

        // Fit bitmap inside container preserving aspect ratio
        val imgRatio = bitmap.width.toFloat() / bitmap.height
        val containerRatio = containerWpx / containerHpx

        val displayedW: Float
        val displayedH: Float
        val offsetX: Float
        val offsetY: Float

        if (imgRatio > containerRatio) {
            displayedW = containerWpx
            displayedH = containerWpx / imgRatio
            offsetX = 0f
            offsetY = (containerHpx - displayedH) / 2f
        } else {
            displayedH = containerHpx
            displayedW = containerHpx * imgRatio
            offsetX = (containerWpx - displayedW) / 2f
            offsetY = 0f
        }

        var activeDragIndex by remember { mutableIntStateOf(-1) }

        // Image background
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(with(density) { displayedW.toDp() })
                .height(with(density) { displayedH.toDp() })
        )

        // Interactive Overlay Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(displayedW, displayedH, offsetX, offsetY, quad) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            // Find nearest corner
                            val touchNormX = ((startOffset.x - offsetX) / displayedW).coerceIn(0f, 1f)
                            val touchNormY = ((startOffset.y - offsetY) / displayedH).coerceIn(0f, 1f)

                            val corners = listOf(
                                quad.topLeft,
                                quad.topRight,
                                quad.bottomRight,
                                quad.bottomLeft
                            )

                            var nearestIdx = -1
                            var minDist = Float.MAX_VALUE
                            // 50dp touch radius in pixels
                            val thresholdPx = 54.dp.toPx()

                            corners.forEachIndexed { index, pt ->
                                val px = offsetX + pt.x * displayedW
                                val py = offsetY + pt.y * displayedH
                                val dist = hypot(startOffset.x - px, startOffset.y - py)
                                if (dist < thresholdPx && dist < minDist) {
                                    minDist = dist
                                    nearestIdx = index
                                }
                            }
                            activeDragIndex = nearestIdx
                        },
                        onDrag = { change, dragAmount ->
                            if (activeDragIndex != -1) {
                                change.consume()
                                val currentPos = change.position
                                val normX = ((currentPos.x - offsetX) / displayedW).coerceIn(0f, 1f)
                                val normY = ((currentPos.y - offsetY) / displayedH).coerceIn(0f, 1f)
                                onCornerMoved(activeDragIndex, PointF(normX, normY))
                            }
                        },
                        onDragEnd = {
                            activeDragIndex = -1
                        },
                        onDragCancel = {
                            activeDragIndex = -1
                        }
                    )
                }
        ) {
            val p0 = Offset(offsetX + quad.topLeft.x * displayedW, offsetY + quad.topLeft.y * displayedH)
            val p1 = Offset(offsetX + quad.topRight.x * displayedW, offsetY + quad.topRight.y * displayedH)
            val p2 = Offset(offsetX + quad.bottomRight.x * displayedW, offsetY + quad.bottomRight.y * displayedH)
            val p3 = Offset(offsetX + quad.bottomLeft.x * displayedW, offsetY + quad.bottomLeft.y * displayedH)

            // Draw bounding polygon path
            val path = Path().apply {
                moveTo(p0.x, p0.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }

            // Quadrilateral outline
            drawPath(
                path = path,
                color = Color(0xFFD4AF37), // Raqeem Gold Accent
                style = Stroke(width = 3.dp.toPx())
            )

            // 4 Corner Handles
            val handleRadius = 14.dp.toPx()
            val innerRadius = 7.dp.toPx()

            val points = listOf(p0, p1, p2, p3)
            points.forEachIndexed { index, pt ->
                val isSelected = (index == activeDragIndex)
                // Outer glow ring
                drawCircle(
                    color = if (isSelected) Color(0xFFE5C158).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.4f),
                    radius = if (isSelected) handleRadius * 1.4f else handleRadius,
                    center = pt
                )
                // White border
                drawCircle(
                    color = Color.White,
                    radius = handleRadius,
                    center = pt,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                // Core dot
                drawCircle(
                    color = if (isSelected) Color(0xFFE5C158) else Color(0xFFD4AF37),
                    radius = innerRadius,
                    center = pt
                )
            }
        }
    }
}

@Composable
private fun EnhancementFiltersTab(
    state: PageProcessingUiState,
    viewModel: PageProcessingViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Live Filter Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val bitmapToDisplay = if (state.showOriginal) state.originalBitmap else state.previewBitmap
            if (bitmapToDisplay != null) {
                Image(
                    bitmap = bitmapToDisplay.asImageBitmap(),
                    contentDescription = "معاينة الصفحة",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            // Before / After Comparison Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clickable { viewModel.setShowOriginal(!state.showOriginal) }
                    .testTag("toggle_compare_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.showOriginal) "الأصل (المصدر)" else "المعالج",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Enhancement Modes Selector
        Text(
            text = "نوع المعالجة والتحسين",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProcessingMode.values().forEach { mode ->
                val isSelected = state.options.mode == mode
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setProcessingMode(mode) },
                    label = { Text(mode.titleAr) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_mode_${mode.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fine Tuning Card
        var isTuneExpanded by remember { mutableStateOf(false) }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isTuneExpanded = !isTuneExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ضبط دقيق (السطوع، التباين، والضوضاء)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (isTuneExpanded) "إخفاء" else "تعديل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isTuneExpanded) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Brightness
                    Text(
                        text = "السطوع: ${state.options.brightness.roundToInt()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = state.options.brightness,
                        onValueChange = { viewModel.setBrightness(it) },
                        valueRange = -50f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Contrast
                    Text(
                        text = "التباين: ${String.format("%.1fx", state.options.contrast)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = state.options.contrast,
                        onValueChange = { viewModel.setContrast(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Noise Reduction
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("إزالة الضوضاء", style = MaterialTheme.typography.bodyMedium)
                            Text("تنعيم الحبيبات مع الحفاظ على الخط", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = state.options.noiseReduction,
                            onCheckedChange = { viewModel.setNoiseReduction(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sharpening
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("شحذ الحواف", style = MaterialTheme.typography.bodyMedium)
                            Text("إبراز تفاصيل الكلمات والحروف بدقة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = state.options.sharpening,
                            onCheckedChange = { viewModel.setSharpening(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Deskew
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("تصحيح الميلان الطفيف", style = MaterialTheme.typography.bodyMedium)
                            Text("موازنة السطور أفقياً في حدود درجات طفيفة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = state.options.deskew,
                            onCheckedChange = { viewModel.setDeskew(it) }
                        )
                    }
                }
            }
        }
    }
}

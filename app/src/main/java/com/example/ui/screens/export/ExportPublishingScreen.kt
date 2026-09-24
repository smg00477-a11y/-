package com.example.ui.screens.export

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ocr.export.ExportFormat
import com.example.ocr.export.ExportOptions
import com.example.ocr.export.OcrTextFormatter
import com.example.ocr.export.PdfExportMode
import com.example.ui.components.RaqeemTopBar
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.ExportPublishingViewModel
import com.example.ui.viewmodel.ExportState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPublishingScreen(
    viewModel: ExportPublishingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val docWithPages by viewModel.documentWithPages.collectAsStateWithLifecycle()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val tocList by viewModel.tocList.collectAsStateWithLifecycle()
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val options by viewModel.options.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Export Settings, 1: Book Structure Preview

    Scaffold(
        topBar = {
            RaqeemTopBar(
                title = "تصدير ونشر الكتاب الرقمي",
                subtitle = docWithPages?.document?.title ?: "إعدادات التصدير",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        },
        bottomBar = {
            if (docWithPages != null) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.executeExport(context) },
                            enabled = exportState !is ExportState.Generating,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_export_button")
                        ) {
                            if (exportState is ExportState.Generating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("جاري معالجة وتصدير الكتاب...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تصدير الكتاب الآن بصيغة ${options.format.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (docWithPages == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val doc = docWithPages!!.document

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tab Selection Bar
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text("إعدادات الصيغ والتنسيق", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        },
                        icon = { Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("export_settings_tab")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text("معاينة هيكل الكتاب", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("book_preview_tab")
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (selectedTab == 0) {
                        // SECTION 1: EXPORT FORMAT SELECTION
                        Text(
                            text = "اختيار صيغة النشر والتصدير",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        ExportFormatCard(
                            format = ExportFormat.PDF,
                            title = "مستند PDF (رقمي أو مصوّر)",
                            description = "تصدير عالي الجودة متوافق مع المطبوعات مع دعم النص العربي وهيكل الفصول وسطح الحقوق",
                            icon = Icons.Default.PictureAsPdf,
                            isSelected = options.format == ExportFormat.PDF,
                            onClick = { viewModel.updateFormat(ExportFormat.PDF) }
                        )

                        if (options.format == ExportFormat.PDF) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "نمط تصدير PDF:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    PdfExportMode.entries.forEach { mode ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.updatePdfMode(mode) }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            RadioButton(
                                                selected = options.pdfMode == mode,
                                                onClick = { viewModel.updatePdfMode(mode) }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(mode.titleAr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Text(mode.descriptionAr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        ExportFormatCard(
                            format = ExportFormat.DOCX,
                            title = "مستند وورد MS Word (DOCX)",
                            description = "تنسيق OpenXML وورد مع المحافظة على اتجاه النص العربي (RTL) والجداول وعناوين الفصول",
                            icon = Icons.Default.Description,
                            isSelected = options.format == ExportFormat.DOCX,
                            onClick = { viewModel.updateFormat(ExportFormat.DOCX) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ExportFormatCard(
                            format = ExportFormat.TXT,
                            title = "ملف نصي متوافق (TXT)",
                            description = "نص ترميز UTF-8 نظيف وخالٍ من شوائب المعالجة الضوئية مع علامات الصفحات وحقوق رقيم",
                            icon = Icons.Default.TextFields,
                            isSelected = options.format == ExportFormat.TXT,
                            onClick = { viewModel.updateFormat(ExportFormat.TXT) }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 2: EXPORT STRUCTURE & PAGES OPTIONS
                        Text(
                            text = "خيارات بنية ومحتوى المستند",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OptionSwitchRow(
                                    title = "تضمين صفحة الغلاف والعنوان الرئيسي",
                                    subtitle = "غلاف مصمم يحتوي اسم الكتاب والمؤلف ومنصة رقيم",
                                    checked = options.includeCoverPage,
                                    onCheckedChange = { viewModel.updateOption(includeCoverPage = it) }
                                )
                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                OptionSwitchRow(
                                    title = "تضمين بطاقة البيانات الببليوجرافية",
                                    subtitle = "صفحة البيانات التوثيقية (الناشر، السنة، ISBN، الطبعة)",
                                    checked = options.includeMetadataPage,
                                    onCheckedChange = { viewModel.updateOption(includeMetadataPage = it) }
                                )
                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                OptionSwitchRow(
                                    title = "تضمين فهرس المحتويات (TOC)",
                                    subtitle = "توليد قائمة مرتبة بالفصول والأبواب المسجلة للكتاب",
                                    checked = options.includeTableOfContents,
                                    onCheckedChange = { viewModel.updateOption(includeTableOfContents = it) }
                                )
                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                OptionSwitchRow(
                                    title = "تضمين صفحة الحقوق الرسمية لمنصة رقيم",
                                    subtitle = "تضمن نسبة الأرشفة للمكتبة المركزية والعقول القائمة على التطوير",
                                    checked = options.includeRightsPage,
                                    onCheckedChange = { viewModel.updateOption(includeRightsPage = it) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 3: TEXT FORMATTING & CLEANING
                        Text(
                            text = "تنقية وتنسيق النصوص (OCR Post-Processing)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OptionCheckboxRow(
                                    title = "إعادة بناء الفقرات والأسطر المقصوصة",
                                    subtitle = "دمج الأسطر المكسورة داخل الفقرة الواحدة تلقائياً",
                                    checked = options.cleanLineBreaks,
                                    onCheckedChange = { viewModel.updateOption(cleanLineBreaks = it) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OptionCheckboxRow(
                                    title = "معالجة المسافات المتكررة والرموز الزائدة",
                                    subtitle = "تنظيف المسافات المزدوجة ومخلفات الـ OCR",
                                    checked = options.fixRepeatedSpaces,
                                    onCheckedChange = { viewModel.updateOption(fixRepeatedSpaces = it) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OptionCheckboxRow(
                                    title = "تضمين الفواصل وأرقام الصفحات الأصلية",
                                    subtitle = "إضافة فاصل نصوص صريح بين صفحات الكتاب (--- [صفحة X] ---)",
                                    checked = options.includePageMarkers,
                                    onCheckedChange = { viewModel.updateOption(includePageMarkers = it) }
                                )
                            }
                        }
                    } else {
                        // TAB 2: BOOK STRUCTURE PREVIEW
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "المعاينة الرقمية لمكونات الكتاب",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "معاينة سريعة للهيكل والبيانات التي سيتم تصديرها استناداً إلى قاعدة بيانات رقيم المحلية.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Book Summary Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("• العنوان الرئيسي: ${doc.title}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("• المؤلف: ${doc.authorName.ifBlank { "غير محدد" }}", style = MaterialTheme.typography.bodyMedium)
                                Text("• التصنيف: ${doc.category}", style = MaterialTheme.typography.bodyMedium)
                                Text("• إجمالي الصفحات: ${docWithPages!!.pageCount} صفحة", style = MaterialTheme.typography.bodyMedium)
                                Text("• الفصول والأبواب: ${chapters.size} فصول مسجلة", style = MaterialTheme.typography.bodyMedium)
                                Text("• الجداول المستخرجة: ${tables.size} جدول بيانات", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // TOC Preview
                        Text("معاينة فهرس المحتويات (TOC):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E1E1E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val tocText = OcrTextFormatter.buildTocString(tocList, chapters)
                            Text(
                                text = tocText,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = Color(0xFF00FFCC),
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Text Sample Preview
                        Text("عينة من أولى صفحات الكتاب بعد التنسيق:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sampleText = docWithPages!!.pages.firstOrNull()?.effectiveOcrText?.let {
                                OcrTextFormatter.formatTextForExport(it, options.cleanLineBreaks, options.fixRepeatedSpaces)
                            } ?: "(لا يوجد نص مستخرج في الصفحة الأولى بعد)"

                            Text(
                                text = sampleText.take(400) + if (sampleText.length > 400) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Export Progress / Success / Error Dialogs
            when (val state = exportState) {
                is ExportState.Generating -> {
                    AlertDialog(
                        onDismissRequest = {},
                        title = {
                            Text("جاري تصدير ونشر الكتاب", fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(state.currentStep, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            }
                        },
                        confirmButton = {}
                    )
                }

                is ExportState.Success -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.resetState() },
                        icon = {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(48.dp))
                        },
                        title = {
                            Text("تم التصدير والنشر بنجاح!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "تم إنشاء ملف التصدير بنجاح وحفظه في ذاكرة الجهاز المحلية.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "الملف: ${state.file.name}\nالحجم: ${state.file.length() / 1024} كيلو بايت",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.resetState()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = when (state.format) {
                                            ExportFormat.PDF -> "application/pdf"
                                            ExportFormat.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                            ExportFormat.TXT -> "text/plain"
                                        }
                                        putExtra(Intent.EXTRA_STREAM, state.shareableUri)
                                        putExtra(Intent.EXTRA_SUBJECT, docWithPages?.document?.title ?: "مستند رقيم")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الملف المصدر"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة الملف")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.resetState() }) {
                                Text("إغلاق")
                            }
                        }
                    )
                }

                is ExportState.Error -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.resetState() },
                        title = { Text("فشل في عملية التصدير", fontWeight = FontWeight.Bold) },
                        text = { Text(state.message) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.resetState() }) {
                                Text("موافق")
                            }
                        }
                    )
                }

                ExportState.Idle -> {}
            }
        }
    }
}

@Composable
private fun ExportFormatCard(
    format: ExportFormat,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, EmeraldPrimary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("export_format_${format.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "محدد",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun OptionSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun OptionCheckboxRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

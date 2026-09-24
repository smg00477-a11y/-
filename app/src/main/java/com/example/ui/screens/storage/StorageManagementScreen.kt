package com.example.ui.screens.storage

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.storage.archive.*
import com.example.ui.viewmodel.StorageManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagementScreen(
    viewModel: StorageManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val storageStats by viewModel.storageStats.collectAsState()
    val healthReport by viewModel.healthReport.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val progressMsg by viewModel.progressMessage.collectAsState()
    val progressFrac by viewModel.progressFraction.collectAsState()
    val userMsg by viewModel.userMessage.collectAsState()
    val senderSession by viewModel.senderSession.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }

    val createArchiveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.exportLibraryToUri(it) }
    }

    val openArchiveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importArchiveFromUri(it, DuplicateStrategy.KEEP_BOTH_CREATE_COPY) }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMsg) {
        userMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "التخزين والأرشفة الرقمية (V0.8)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("storage_management_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshStorageStats()
                        viewModel.runHealthCheck()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "تحديث البيانات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Header
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("ضغط التخزين", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Filled.Storage, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_storage_compression")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("صحة الأرشيف", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Filled.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_archive_health")
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("تصدير واستيراد", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_archive_export_import")
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("نقل بين الأجهزة", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_device_transfer")
                )
            }

            if (isLoading) {
                LinearProgressIndicator(
                    progress = { progressFrac },
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = progressMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> StorageCompressionTab(
                        stats = storageStats,
                        onCompress = { strategy -> viewModel.compressLibrary(strategy) },
                        onClearCache = { viewModel.clearCacheAndTemp() },
                        onCleanOrphaned = { viewModel.cleanOrphanedFiles() }
                    )
                    1 -> ArchiveHealthTab(
                        report = healthReport,
                        onRunHealthCheck = { viewModel.runHealthCheck() },
                        onRepair = { viewModel.repairArchiveIssues() }
                    )
                    2 -> ExportImportTab(
                        onExportClick = { createArchiveFileLauncher.launch("raqeem_library_archive_${System.currentTimeMillis()}.rqm") },
                        onImportClick = { openArchiveFileLauncher.launch(arrayOf("*/*")) }
                    )
                    3 -> DeviceTransferTab(
                        senderSession = senderSession,
                        onStartSender = { viewModel.startSenderSession() },
                        onStopSender = { viewModel.stopSenderSession() },
                        onReceive = { ip, pin -> viewModel.receiveFromSender(ip, pin) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageCompressionTab(
    stats: StorageStats?,
    onCompress: (CompressionStrategy) -> Unit,
    onClearCache: () -> Unit,
    onCleanOrphaned: () -> Unit
) {
    var selectedStrategy by remember { mutableStateOf(CompressionStrategy.BALANCED) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "نظرة عامة على التخزين والمساحة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (stats != null) {
                        val totalMb = stats.totalLibraryBytes / (1024.0 * 1024.0)
                        val freeGb = stats.freeSpaceBytes / (1024.0 * 1024.0 * 1024.0)

                        Text(
                            text = "إجمالي حجم المكتبة المحلي: ${String.format("%.2f", totalMb)} ميجابايت",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "المساحة المتاحة في الجهاز: ${String.format("%.2f", freeGb)} جيجابايت",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StorageStatBadge("الكتب", "${stats.documentCount} كتاب")
                            StorageStatBadge("الصفحات", "${stats.pageCount} صفحة")
                            StorageStatBadge("المؤقتات", "${String.format("%.1f", stats.tempCacheBytes / (1024.0 * 1024.0))} MB")
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ضغط صور الكتب والأرشفة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "اختر إستراتيجية الضغط المناسبة للحفاظ على جودة النص وتوفير مساحة التخزين:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CompressionStrategy.values().forEach { strategy ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedStrategy == strategy,
                                onClick = { selectedStrategy = strategy }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = strategy.titleAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strategy.descriptionAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onCompress(selectedStrategy) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_library_compression_button")
                    ) {
                        Icon(Icons.Filled.Compress, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بدء ضغط كامل المكتبة")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "صيانة وتنظيف التخزين",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حذف الملفات المؤقتة والذاكرة المخبأة")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onCleanOrphaned,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مسح الصور الميتة/اليتيمة غير المرتبطة بكتاب")
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageStatBadge(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun ArchiveHealthTab(
    report: ArchiveHealthReport?,
    onRunHealthCheck: () -> Unit,
    onRepair: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (report?.status) {
                        HealthStatus.HEALTHY -> MaterialTheme.colorScheme.primaryContainer
                        HealthStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                        HealthStatus.CORRUPTED -> MaterialTheme.colorScheme.errorContainer
                        null -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    if (report != null) {
                        Text(
                            text = when (report.status) {
                                HealthStatus.HEALTHY -> "الأرشيف سليماً 100%"
                                HealthStatus.WARNING -> "توجد تنبيهات طفيفة بالملفات"
                                HealthStatus.CORRUPTED -> "تلف في بعض صور أو بيانات الكتب"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "مؤشر صحة الأرشيف المحلي: ${report.healthScore}%",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("الكتب: ${report.totalDocumentsChecked}")
                            Text("الملفات المفقودة: ${report.missingFilesCount}")
                            Text("المرجعيات المكسورة: ${report.brokenReferencesCount}")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onRunHealthCheck,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.FindInPage, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إعادة الفحص")
                            }

                            if (report.issues.any { it.isAutoRepairable }) {
                                Button(
                                    onClick = onRepair,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Build, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إصلاح تلقائي")
                                }
                            }
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        item {
            Text(
                text = "سجل المشكلات والتنبيهات المكتشفة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (report?.issues.isNullOrEmpty()) {
            item {
                Text(
                    text = "لم يتم العثور على أي مشاكل أو ملفات تالفة بالأرشيف.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(report!!.issues, key = { it.id }) { issue ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = issue.documentTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Badge(
                                containerColor = when (issue.severity) {
                                    IssueSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                                    IssueSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
                                    IssueSeverity.INFO -> MaterialTheme.colorScheme.secondary
                                }
                            ) {
                                Text(issue.issueType, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = issue.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportImportTab(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Icon(
                    imageVector = Icons.Filled.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "تصدير حزمة الأرشيف الكاملة (.rqm)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "قم بتصدير جميع كتبك، الصور، نصوص OCR، الفهارس، الفصول وملاحظاتك في حزمة أرشيف رقيم موحدة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onExportClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_archive_button")
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ وتصدير الأرشيف (.rqm)")
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Icon(
                    imageVector = Icons.Filled.DownloadForOffline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "استيراد حزمة أرشيف رقيم (.rqm)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "استرجاع أو استيراد مكتبة كتب من ملف أرشيف (.rqm) مع التحقق التلقائي من عدم التكرار وصحة التشفير.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onImportClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_archive_button")
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("فتح واستيراد حزمة الأرشيف (.rqm)")
                }
            }
        }
    }
}

@Composable
private fun DeviceTransferTab(
    senderSession: SenderSession?,
    onStartSender: () -> Unit,
    onStopSender: () -> Unit,
    onReceive: (ip: String, pin: String) -> Unit
) {
    var receiverIp by remember { mutableStateOf("") }
    var receiverPin by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.WifiTethering,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "إرسال المكتبة للجهاز الآخر (Sender Mode)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (senderSession != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(text = "خادم النقل المباشر يعمل حالياً 🟢", fontWeight = FontWeight.Bold)
                                Text(text = "عنوان IP للجهاز: ${senderSession.serverIp}")
                                Text(text = "المنفذ: ${senderSession.serverPort}")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "رمز الاقتران (PIN): ${senderSession.pairingPin}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onStopSender,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إيقاف جلسة الإرسال")
                        }
                    } else {
                        Button(
                            onClick = onStartSender,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_sender_session_button")
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("بدء جلسة الإرسال على الشبكة المحلية")
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MoveToInbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "استقبال المكتبة من جهاز آخر (Receiver Mode)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = receiverIp,
                        onValueChange = { receiverIp = it },
                        label = { Text("عنوان IP للجهاز المرسل (مثال: 192.168.1.50)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = receiverPin,
                        onValueChange = { receiverPin = it },
                        label = { Text("رمز الاقتران (PIN المكون من 4 أرقام)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onReceive(receiverIp, receiverPin) },
                        enabled = receiverIp.isNotBlank() && receiverPin.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_receive_transfer_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الاتصال واستقبال المكتبة مباشرة")
                    }
                }
            }
        }
    }
}

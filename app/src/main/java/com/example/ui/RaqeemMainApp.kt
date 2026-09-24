package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.ui.navigation.Screen
import com.example.ui.screens.camera.CameraCaptureScreen
import com.example.ui.screens.authors.AuthorsScreen
import com.example.ui.screens.chapters.ChapterManagementScreen
import com.example.ui.screens.create.CreateDocumentScreen
import com.example.ui.screens.detail.DocumentDetailScreen
import com.example.ui.screens.favorites.FavoritesScreen
import com.example.ui.screens.identity.BookIdentityScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.manifest.ModelManifestScreen
import com.example.ui.screens.ocr.OcrReviewScreen
import com.example.ui.screens.processing.PageProcessingScreen
import com.example.ui.screens.reader.ReaderScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.intelligence.BookIntelligenceScreen
import com.example.ui.screens.chat.AiChatScreen
import com.example.ui.screens.diagnostics.AiDiagnosticsScreen
import com.example.ui.screens.storage.StorageManagementScreen
import com.example.ui.viewmodel.StorageManagementViewModel
import com.example.ui.screens.export.ExportPublishingScreen
import com.example.ui.viewmodel.ExportPublishingViewModel
import com.example.ui.screens.categories.CategoryManagementScreen
import com.example.ui.screens.series.SeriesManagementScreen
import com.example.ui.screens.stats.LibraryStatsScreen
import com.example.ui.viewmodel.AuthorsViewModel
import com.example.ui.viewmodel.BookIdentityViewModel
import com.example.ui.viewmodel.ChapterManagementViewModel
import com.example.ui.viewmodel.CreateDocumentViewModel
import com.example.ui.viewmodel.DocumentDetailViewModel
import com.example.ui.viewmodel.FavoritesViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.OcrViewModel
import com.example.ui.viewmodel.PageProcessingViewModel
import com.example.ui.viewmodel.ReaderViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.BookIntelligenceViewModel
import com.example.ui.viewmodel.AiChatViewModel
import com.example.ui.viewmodel.AiDiagnosticsViewModel
import com.example.ui.viewmodel.CategoryViewModel
import com.example.ui.viewmodel.SeriesViewModel
import com.example.ui.viewmodel.LibraryStatsViewModel
import java.net.URLDecoder

data class BottomNavItem(
    val screen: Screen,
    val titleAr: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)

@Composable
fun RaqeemMainApp(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Shared ViewModel for creating documents across Library -> Camera -> CreateDocument
    val createDocViewModel: CreateDocumentViewModel = viewModel(
        factory = CreateDocumentViewModel.Factory(container.documentRepository)
    )

    val bottomNavItems = listOf(
        BottomNavItem(
            screen = Screen.Library,
            titleAr = "المكتبة",
            selectedIcon = Icons.Filled.AutoStories,
            unselectedIcon = Icons.Outlined.AutoStories,
            testTag = "nav_tab_library"
        ),
        BottomNavItem(
            screen = Screen.Authors,
            titleAr = "المؤلفون",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            testTag = "nav_tab_authors"
        ),
        BottomNavItem(
            screen = Screen.Favorites,
            titleAr = "المفضلة",
            selectedIcon = Icons.Filled.Star,
            unselectedIcon = Icons.Outlined.StarBorder,
            testTag = "nav_tab_favorites"
        ),
        BottomNavItem(
            screen = Screen.Settings,
            titleAr = "الإعدادات",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_tab_settings"
        )
    )

    // Launchers for Image and File picker from Library quick action
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            createDocViewModel.reset()
            createDocViewModel.importImageUri(it) { success ->
                if (success) {
                    navController.navigate(Screen.CreateDocument.route)
                }
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            createDocViewModel.reset()
            createDocViewModel.importImageUri(it) { success ->
                if (success) {
                    navController.navigate(Screen.CreateDocument.route)
                }
            }
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Library.route,
        Screen.Authors.route,
        Screen.Favorites.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.titleAr
                                )
                            },
                            label = { Text(text = item.titleAr) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(
                bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp
            )
        ) {
            // 1. Library Screen
            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(container.documentRepository, container.localAiEngine)
                )
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onNavigateToDocument = { docId ->
                        navController.navigate(Screen.DocumentDetail.createRoute(docId))
                    },
                    onScanCameraClick = {
                        createDocViewModel.reset()
                        navController.navigate(Screen.CameraCapture.route)
                    },
                    onImportImageClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onImportFileClick = {
                        documentPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                    },
                    onOpenAiChat = {
                        navController.navigate(Screen.AiChat.createRoute())
                    }
                )
            }

            // 2. Favorites Screen
            composable(Screen.Favorites.route) {
                val favoritesViewModel: FavoritesViewModel = viewModel(
                    factory = FavoritesViewModel.Factory(container.documentRepository)
                )
                FavoritesScreen(
                    viewModel = favoritesViewModel,
                    onNavigateToDocument = { docId ->
                        navController.navigate(Screen.DocumentDetail.createRoute(docId))
                    }
                )
            }

            // 3. Settings Screen
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(container.themePreferences)
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onOpenModelManifest = {
                        navController.navigate(Screen.ModelManifest.route)
                    },
                    onOpenAiDiagnostics = {
                        navController.navigate(Screen.AiDiagnostics.route)
                    },
                    onOpenStorageManagement = {
                        navController.navigate(Screen.StorageManagement.route)
                    }
                )
            }

            // 4. Create Document Screen
            composable(Screen.CreateDocument.route) {
                CreateDocumentScreen(
                    viewModel = createDocViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToCamera = {
                        navController.navigate(Screen.CameraCapture.route)
                    },
                    onNavigateToProcessPage = { pageIndex, filePath ->
                        navController.navigate(
                            Screen.PageProcessing.createRoute(
                                pageId = -1L,
                                filePath = filePath,
                                pageIndex = pageIndex,
                                documentId = -1L
                            )
                        )
                    },
                    onDocumentSaved = { docId ->
                        navController.navigate(Screen.DocumentDetail.createRoute(docId)) {
                            popUpTo(Screen.Library.route)
                        }
                    }
                )
            }

            // 5. Camera Capture Screen
            composable(Screen.CameraCapture.route) {
                CameraCaptureScreen(
                    createDocumentViewModel = createDocViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onFinishToReview = {
                        navController.navigate(Screen.CreateDocument.route) {
                            popUpTo(Screen.CameraCapture.route) { inclusive = true }
                        }
                    },
                    onProcessCapturedPage = { filePath ->
                        val lastIndex = createDocViewModel.pages.value.lastIndex.coerceAtLeast(0)
                        navController.navigate(
                            Screen.PageProcessing.createRoute(
                                pageId = -1L,
                                filePath = filePath,
                                pageIndex = lastIndex,
                                documentId = -1L
                            )
                        ) {
                            popUpTo(Screen.CameraCapture.route) { inclusive = true }
                        }
                    }
                )
            }

            // 6. Document Detail Screen
            composable(
                route = Screen.DocumentDetail.route,
                arguments = listOf(navArgument("documentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                val detailViewModel: DocumentDetailViewModel = viewModel(
                    key = "detail_$documentId",
                    factory = DocumentDetailViewModel.Factory(documentId, container.documentRepository)
                )
                DocumentDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onOpenReader = { pageIndex ->
                        navController.navigate(Screen.Reader.createRoute(documentId, pageIndex))
                    },
                    onProcessPage = { pageId, filePath, pageIndex, docId ->
                        navController.navigate(
                            Screen.PageProcessing.createRoute(
                                pageId = pageId,
                                filePath = filePath,
                                pageIndex = pageIndex,
                                documentId = docId
                            )
                        )
                    },
                    onOpenOcrReview = { pageId ->
                        navController.navigate(Screen.OcrReview.createRoute(pageId))
                    },
                    onOpenBookIdentity = { docId ->
                        navController.navigate(Screen.BookIdentity.createRoute(docId))
                    },
                    onOpenBookIntelligence = { docId ->
                        navController.navigate(Screen.BookIntelligence.createRoute(docId))
                    },
                    onOpenAiChat = { docId ->
                        navController.navigate(Screen.AiChat.createRoute(docId))
                    },
                    onOpenExportPublishing = { docId ->
                        navController.navigate(Screen.ExportPublishing.createRoute(docId))
                    }
                )
            }

            // 7. Reader Screen
            composable(
                route = Screen.Reader.route,
                arguments = listOf(
                    navArgument("documentId") { type = NavType.LongType },
                    navArgument("page") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                val initialPage = backStackEntry.arguments?.getInt("page") ?: 0
                val readerViewModel: ReaderViewModel = viewModel(
                    key = "reader_$documentId",
                    factory = ReaderViewModel.Factory(documentId, initialPage, container.documentRepository, container.localAiEngine)
                )
                ReaderScreen(
                    viewModel = readerViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onOpenOcrReview = { pageId ->
                        navController.navigate(Screen.OcrReview.createRoute(pageId))
                    },
                    onOpenBookIdentity = { docId ->
                        navController.navigate(Screen.BookIdentity.createRoute(docId))
                    },
                    onOpenBookIntelligence = { docId ->
                        navController.navigate(Screen.BookIntelligence.createRoute(docId))
                    },
                    onOpenAiChat = { docId ->
                        navController.navigate(Screen.AiChat.createRoute(docId))
                    }
                )
            }

            // 8. Page Processing Screen (V0.2 Pipeline)
            composable(
                route = Screen.PageProcessing.route,
                arguments = listOf(
                    navArgument("pageId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("filePath") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("pageIndex") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                    navArgument("documentId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val pageId = backStackEntry.arguments?.getLong("pageId") ?: -1L
                val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                val decodedPath = try {
                    URLDecoder.decode(encodedPath, "UTF-8")
                } catch (e: Exception) {
                    encodedPath
                }
                val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0
                val documentId = backStackEntry.arguments?.getLong("documentId") ?: -1L

                val processingViewModel: PageProcessingViewModel = viewModel(
                    key = "processing_${pageId}_${pageIndex}_${decodedPath.hashCode()}",
                    factory = PageProcessingViewModel.Factory(
                        pageId = pageId,
                        initialFilePath = decodedPath,
                        pageIndex = pageIndex,
                        documentId = documentId,
                        repository = container.documentRepository
                    )
                )

                PageProcessingScreen(
                    viewModel = processingViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onProcessingFinished = { processedPath, thumbPath ->
                        if (pageId <= 0 && processedPath != null) {
                            createDocViewModel.setPageProcessed(pageIndex, processedPath, thumbPath)
                        }
                        navController.popBackStack()
                    }
                )
            }

            // 9. OCR Review Screen (V0.3 Offline OCR)
            composable(
                route = Screen.OcrReview.route,
                arguments = listOf(navArgument("pageId") { type = NavType.LongType })
            ) { backStackEntry ->
                val pageId = backStackEntry.arguments?.getLong("pageId") ?: 0L
                val ocrViewModel: OcrViewModel = viewModel(
                    key = "ocr_$pageId",
                    factory = OcrViewModel.Factory(container.documentRepository, pageId)
                )
                OcrReviewScreen(
                    viewModel = ocrViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // 10. Authors Screen (V0.5 Author Profiles)
            composable(Screen.Authors.route) {
                val authorsViewModel: AuthorsViewModel = viewModel(
                    factory = AuthorsViewModel.Factory(container.documentRepository)
                )
                AuthorsScreen(
                    viewModel = authorsViewModel,
                    onOpenBookIdentity = { docId ->
                        navController.navigate(Screen.BookIdentity.createRoute(docId))
                    }
                )
            }

            // 11. Book Identity Screen (V0.5 Metadata, Chapters, TOC, Tables)
            composable(
                route = Screen.BookIdentity.route,
                arguments = listOf(navArgument("documentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                val identityViewModel: BookIdentityViewModel = viewModel(
                    key = "identity_$documentId",
                    factory = BookIdentityViewModel.Factory(documentId, container.documentRepository)
                )
                BookIdentityScreen(
                    viewModel = identityViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onOpenReader = { pageIndex ->
                        navController.navigate(Screen.Reader.createRoute(documentId, pageIndex))
                    },
                    onOpenChapterManagement = { docId ->
                        navController.navigate(Screen.ChapterManagement.createRoute(docId))
                    }
                )
            }

            // 12. Chapter Management Screen (V0.5 Chapters Editor)
            composable(
                route = Screen.ChapterManagement.route,
                arguments = listOf(navArgument("documentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                val chapterViewModel: ChapterManagementViewModel = viewModel(
                    key = "chapter_mgmt_$documentId",
                    factory = ChapterManagementViewModel.Factory(documentId, container.documentRepository)
                )
                ChapterManagementScreen(
                    viewModel = chapterViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onOpenReader = { pageIndex ->
                        navController.navigate(Screen.Reader.createRoute(documentId, pageIndex))
                    }
                )
            }

            // 13. Model Manifest Screen (V0.5 Offline Model Inventory)
            composable(Screen.ModelManifest.route) {
                ModelManifestScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // 14. Book Intelligence Screen (V0.6 Offline AI Dashboard)
            composable(
                route = Screen.BookIntelligence.route,
                arguments = listOf(navArgument("documentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                val intelligenceViewModel: BookIntelligenceViewModel = viewModel(
                    key = "intelligence_$documentId",
                    factory = BookIntelligenceViewModel.Factory(
                        documentId = documentId,
                        repository = container.documentRepository,
                        localAiEngine = container.localAiEngine
                    )
                )
                BookIntelligenceScreen(
                    viewModel = intelligenceViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onOpenReader = { pageIndex ->
                        navController.navigate(Screen.Reader.createRoute(documentId, pageIndex))
                    },
                    onOpenAiChat = { docId ->
                        navController.navigate(Screen.AiChat.createRoute(docId))
                    }
                )
            }

            // 15. Local AI Chat Screen (V0.6 Grounded RAG Chat)
            composable(
                route = Screen.AiChat.route,
                arguments = listOf(
                    navArgument("documentId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val documentIdArg = backStackEntry.arguments?.getLong("documentId") ?: -1L
                val docId = if (documentIdArg > 0) documentIdArg else null
                val chatViewModel: AiChatViewModel = viewModel(
                    key = "ai_chat_${docId ?: 0L}",
                    factory = AiChatViewModel.Factory(
                        initialDocumentId = docId,
                        initialChapterId = null,
                        localAiEngine = container.localAiEngine,
                        repository = container.documentRepository
                    )
                )
                AiChatScreen(
                    viewModel = chatViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onOpenReader = { docIdParam, pageIndex ->
                        navController.navigate(Screen.Reader.createRoute(docIdParam, pageIndex))
                    }
                )
            }

            // 16. AI Diagnostics & Model Management Screen (V0.6 Local Model Manager)
            composable(Screen.AiDiagnostics.route) {
                val diagnosticsViewModel: AiDiagnosticsViewModel = viewModel(
                    factory = AiDiagnosticsViewModel.Factory(
                        localAiEngine = container.localAiEngine,
                        repository = container.documentRepository
                    )
                )
                AiDiagnosticsScreen(
                    viewModel = diagnosticsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // 17. Professional Storage & Archiving Screen (V0.8)
            composable(Screen.StorageManagement.route) {
                val storageViewModel: StorageManagementViewModel = viewModel(
                    factory = StorageManagementViewModel.Factory(
                        context = container.database.openHelper.writableDatabase.let { navController.context },
                        optimizationManager = container.storageOptimizationManager,
                        healthChecker = container.archiveHealthChecker,
                        archiveManager = container.raqeemArchiveManager,
                        transferManager = container.localDeviceTransferManager
                    )
                )
                StorageManagementScreen(
                    viewModel = storageViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // 18. Export & Publishing Screen (V0.9)
            composable(
                route = Screen.ExportPublishing.route,
                arguments = listOf(navArgument("documentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                val exportViewModel: ExportPublishingViewModel = viewModel(
                    key = "export_$documentId",
                    factory = ExportPublishingViewModel.Factory(documentId, container.documentRepository)
                )
                ExportPublishingScreen(
                    viewModel = exportViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

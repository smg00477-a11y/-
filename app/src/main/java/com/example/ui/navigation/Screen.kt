package com.example.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object CreateDocument : Screen("create_document")
    object CameraCapture : Screen("camera_capture")
    
    object DocumentDetail : Screen("document_detail/{documentId}") {
        fun createRoute(documentId: Long): String = "document_detail/$documentId"
    }

    object Reader : Screen("reader/{documentId}?page={page}") {
        fun createRoute(documentId: Long, page: Int = 0): String = "reader/$documentId?page=$page"
    }

    object PageProcessing : Screen("page_processing?pageId={pageId}&filePath={filePath}&pageIndex={pageIndex}&documentId={documentId}") {
        fun createRoute(
            pageId: Long = -1L,
            filePath: String = "",
            pageIndex: Int = 0,
            documentId: Long = -1L
        ): String {
            val encodedPath = URLEncoder.encode(filePath, "UTF-8")
            return "page_processing?pageId=$pageId&filePath=$encodedPath&pageIndex=$pageIndex&documentId=$documentId"
        }
    }

    object OcrReview : Screen("ocr_review/{pageId}") {
        fun createRoute(pageId: Long): String = "ocr_review/$pageId"
    }

    object BookIdentity : Screen("book_identity/{documentId}") {
        fun createRoute(documentId: Long): String = "book_identity/$documentId"
    }

    object Authors : Screen("authors")

    object ChapterManagement : Screen("chapter_management/{documentId}") {
        fun createRoute(documentId: Long): String = "chapter_management/$documentId"
    }

    object ModelManifest : Screen("model_manifest")

    object BookIntelligence : Screen("book_intelligence/{documentId}") {
        fun createRoute(documentId: Long): String = "book_intelligence/$documentId"
    }

    object AiChat : Screen("ai_chat?documentId={documentId}&chapterId={chapterId}") {
        fun createRoute(documentId: Long? = null, chapterId: Long? = null): String {
            val docParam = documentId?.toString() ?: ""
            val chParam = chapterId?.toString() ?: ""
            return "ai_chat?documentId=$docParam&chapterId=$chParam"
        }
    }

    object AiDiagnostics : Screen("ai_diagnostics")

    object Categories : Screen("categories")

    object Series : Screen("series")

    object LibraryStats : Screen("library_stats")

    object StorageManagement : Screen("storage_management")

    object ExportPublishing : Screen("export_publishing/{documentId}") {
        fun createRoute(documentId: Long): String = "export_publishing/$documentId"
    }
}

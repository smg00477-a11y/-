package com.example.data.storage.archive

import android.content.Context
import android.graphics.BitmapFactory
import com.example.data.database.AppDatabase
import com.example.data.database.entity.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class HealthStatus {
    HEALTHY,
    WARNING,
    CORRUPTED
}

enum class IssueSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class ArchiveIssue(
    val id: String,
    val documentId: Long?,
    val documentTitle: String,
    val pageIndex: Int?,
    val filePath: String?,
    val issueType: String,
    val description: String,
    val severity: IssueSeverity,
    val isAutoRepairable: Boolean
)

data class ArchiveHealthReport(
    val status: HealthStatus,
    val healthScore: Int,
    val totalDocumentsChecked: Int,
    val totalPagesChecked: Int,
    val missingFilesCount: Int,
    val corruptedFilesCount: Int,
    val brokenReferencesCount: Int,
    val issues: List<ArchiveIssue>,
    val timestamp: Long = System.currentTimeMillis()
)

class ArchiveHealthChecker(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun runHealthCheck(
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): ArchiveHealthReport = withContext(Dispatchers.IO) {
        val documents = database.documentDao().getAllDocumentsDirect()
        val allPages = database.pageDao().getAllPagesDirect()

        val issues = mutableListOf<ArchiveIssue>()
        var missingFiles = 0
        var corruptedFiles = 0
        var brokenRefs = 0

        documents.forEachIndexed { docIdx, doc ->
            onProgress((docIdx + 1).toFloat() / (documents.size + 1), "فحص الكتاب: ${doc.title}")
            val pages = allPages.filter { it.documentId == doc.id }

            if (pages.isEmpty()) {
                issues.add(
                    ArchiveIssue(
                        id = "no_pages_${doc.id}",
                        documentId = doc.id,
                        documentTitle = doc.title,
                        pageIndex = null,
                        filePath = null,
                        issueType = "EMPTY_BOOK",
                        description = "الكتاب لا يحتوي على أي صفحات مسجلة في قاعدة البيانات.",
                        severity = IssueSeverity.WARNING,
                        isAutoRepairable = false
                    )
                )
            }

            pages.forEach { page ->
                val origFile = File(page.localFilePath)
                if (!origFile.exists()) {
                    missingFiles++
                    issues.add(
                        ArchiveIssue(
                            id = "missing_orig_${page.id}",
                            documentId = doc.id,
                            documentTitle = doc.title,
                            pageIndex = page.pageIndex,
                            filePath = page.localFilePath,
                            issueType = "MISSING_ORIGINAL_FILE",
                            description = "صورة الصفحة الأصلية غير موجودة على وحدات التخزين.",
                            severity = IssueSeverity.CRITICAL,
                            isAutoRepairable = false
                        )
                    )
                } else if (!isImageReadable(origFile)) {
                    corruptedFiles++
                    issues.add(
                        ArchiveIssue(
                            id = "corrupt_orig_${page.id}",
                            documentId = doc.id,
                            documentTitle = doc.title,
                            pageIndex = page.pageIndex,
                            filePath = page.localFilePath,
                            issueType = "CORRUPTED_FILE",
                            description = "صورة الصفحة غير قابلة للقراءة أو تالفة.",
                            severity = IssueSeverity.CRITICAL,
                            isAutoRepairable = false
                        )
                    )
                }

                page.processedFilePath?.let { procPath ->
                    val procFile = File(procPath)
                    if (!procFile.exists()) {
                        brokenRefs++
                        issues.add(
                            ArchiveIssue(
                                id = "broken_proc_${page.id}",
                                documentId = doc.id,
                                documentTitle = doc.title,
                                pageIndex = page.pageIndex,
                                filePath = procPath,
                                issueType = "BROKEN_PROCESSED_REFERENCE",
                                description = "مسار المعالجة غير موجود. يمكن استعادته من الصورة الأصلية.",
                                severity = IssueSeverity.WARNING,
                                isAutoRepairable = true
                            )
                        )
                    }
                }

                page.thumbnailPath?.let { thumbPath ->
                    val thumbFile = File(thumbPath)
                    if (!thumbFile.exists()) {
                        brokenRefs++
                        issues.add(
                            ArchiveIssue(
                                id = "broken_thumb_${page.id}",
                                documentId = doc.id,
                                documentTitle = doc.title,
                                pageIndex = page.pageIndex,
                                filePath = thumbPath,
                                issueType = "MISSING_THUMBNAIL",
                                description = "صورة المعاينة المصغرة مفقودة. يمكن إعادة إنشائها تلقائياً.",
                                severity = IssueSeverity.INFO,
                                isAutoRepairable = true
                            )
                        )
                    }
                }
            }
        }

        onProgress(1.0f, "اكتمل الفحص التشخيصي بالأرشيف")

        val totalChecks = (allPages.size * 2).coerceAtLeast(1)
        val errorPoints = (missingFiles * 10) + (corruptedFiles * 10) + (brokenRefs * 2)
        val rawScore = 100 - ((errorPoints * 100) / totalChecks)
        val healthScore = rawScore.coerceIn(0, 100)

        val status = when {
            healthScore >= 90 && missingFiles == 0 -> HealthStatus.HEALTHY
            healthScore >= 60 && missingFiles == 0 -> HealthStatus.WARNING
            else -> HealthStatus.CORRUPTED
        }

        ArchiveHealthReport(
            status = status,
            healthScore = healthScore,
            totalDocumentsChecked = documents.size,
            totalPagesChecked = allPages.size,
            missingFilesCount = missingFiles,
            corruptedFilesCount = corruptedFiles,
            brokenReferencesCount = brokenRefs,
            issues = issues
        )
    }

    suspend fun repairArchiveIssues(report: ArchiveHealthReport): Int = withContext(Dispatchers.IO) {
        var repairedCount = 0
        report.issues.filter { it.isAutoRepairable }.forEach { issue ->
            when (issue.issueType) {
                "BROKEN_PROCESSED_REFERENCE" -> {
                    issue.pageIndex?.let { pageIdx ->
                        issue.documentId?.let { docId ->
                            val pages = database.pageDao().getPagesForDocumentDirect(docId)
                            val page = pages.find { it.pageIndex == pageIdx }
                            if (page != null) {
                                val updated = page.copy(processedFilePath = null, processingState = "NOT_PROCESSED")
                                database.pageDao().updatePage(updated)
                                repairedCount++
                            }
                        }
                    }
                }
                "MISSING_THUMBNAIL" -> {
                    issue.pageIndex?.let { pageIdx ->
                        issue.documentId?.let { docId ->
                            val pages = database.pageDao().getPagesForDocumentDirect(docId)
                            val page = pages.find { it.pageIndex == pageIdx }
                            if (page != null) {
                                val updated = page.copy(thumbnailPath = null)
                                database.pageDao().updatePage(updated)
                                repairedCount++
                            }
                        }
                    }
                }
            }
        }
        repairedCount
    }

    private fun isImageReadable(file: File): Boolean {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth > 0 && options.outHeight > 0
        } catch (e: Exception) {
            false
        }
    }
}

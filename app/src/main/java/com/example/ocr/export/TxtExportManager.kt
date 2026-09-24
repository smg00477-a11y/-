package com.example.ocr.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.database.entity.BookMetadataEntity
import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.TableEntity
import com.example.data.database.entity.TocEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TxtExportManager {

    private const val EXPORT_DIR = "exports"

    suspend fun exportDocumentToTxtWithOptions(
        context: Context,
        documentWithPages: DocumentWithPages,
        metadata: BookMetadataEntity? = null,
        chapters: List<ChapterEntity> = emptyList(),
        tocList: List<TocEntryEntity> = emptyList(),
        tables: List<TableEntity> = emptyList(),
        options: ExportOptions = ExportOptions()
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, EXPORT_DIR).apply { mkdirs() }

        val safeTitle = documentWithPages.document.title
            .replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            .take(35)
            .ifEmpty { "document" }

        val fileName = "Raqeem_${safeTitle}_${System.currentTimeMillis()}.txt"
        val targetFile = File(exportDir, fileName)

        val fullText = buildFullDocumentTextWithOptions(context, documentWithPages, metadata, chapters, tocList, tables, options)

        FileOutputStream(targetFile).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.write(fullText)
            }
        }

        targetFile
    }

    suspend fun exportDocumentToTxt(
        context: Context,
        documentWithPages: DocumentWithPages
    ): File = exportDocumentToTxtWithOptions(context, documentWithPages)

    fun buildFullDocumentTextWithOptions(
        context: Context? = null,
        documentWithPages: DocumentWithPages,
        metadata: BookMetadataEntity? = null,
        chapters: List<ChapterEntity> = emptyList(),
        tocList: List<TocEntryEntity> = emptyList(),
        tables: List<TableEntity> = emptyList(),
        options: ExportOptions = ExportOptions()
    ): String {
        val doc = documentWithPages.document
        val pages = documentWithPages.pages.sortedBy { it.pageIndex }

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(Date(doc.updatedAt.takeIf { it > 0 } ?: doc.createdAt))
        val attributionText = context?.getString(com.example.R.string.library_attribution)
            ?: "تم تصميمه للمكتبة المركزية العامة خصيصًا"

        val sb = StringBuilder()

        // 1. Header & Metadata
        if (options.includeCoverPage || options.includeMetadataPage) {
            sb.append("==================================================\n")
            sb.append("رَقِيم — Raqeem | من الورق إلى الرقمنة\n")
            sb.append("عنوان المستند: ").append(doc.title).append("\n")
            sb.append("المؤلف: ").append(doc.authorName.ifBlank { "غير محدد" }).append("\n")
            sb.append("التصنيف: ").append(doc.category).append("\n")
            if (metadata != null) {
                if (metadata.publisher.isNotBlank()) sb.append("الناشر: ").append(metadata.publisher).append("\n")
                if (metadata.isbn.isNotBlank()) sb.append("ISBN: ").append(metadata.isbn).append("\n")
            }
            sb.append("تاريخ التحديث: ").append(dateString).append("\n")
            sb.append("إجمالي الصفحات: ").append(pages.size).append("\n")
            sb.append("==================================================\n\n")
        }

        // 2. Table of Contents
        if (options.includeTableOfContents && (chapters.isNotEmpty() || tocList.isNotEmpty())) {
            sb.append(OcrTextFormatter.buildTocString(tocList, chapters))
        }

        // 3. Pages Content
        pages.forEachIndexed { idx, page ->
            if (options.includePageMarkers) {
                sb.append("--- [الصفحة ").append(idx + 1).append("] ---\n")
            }
            val text = page.effectiveOcrText
            if (text.isNotBlank()) {
                val formatted = OcrTextFormatter.formatTextForExport(
                    rawText = text,
                    cleanLineBreaks = options.cleanLineBreaks,
                    fixRepeatedSpaces = options.fixRepeatedSpaces
                )
                sb.append(formatted).append("\n\n")
            } else {
                sb.append("(لا يوجد نص مستخرج لهذه الصفحة)\n\n")
            }
        }

        // 4. Rights Footer
        if (options.includeRightsPage) {
            sb.append("==================================================\n")
            sb.append("تمت أرشفة هذا الكتاب بواسطة منصة رقيم\n")
            sb.append("الحقوق: ").append(attributionText).append("\n")
            sb.append("برمجة: همام محمد\n")
            sb.append("تطبيق رقيم V1.0 — محلي 100% دون اتصال بالإنترنت\n")
            sb.append("==================================================\n")
        }

        return sb.toString()
    }

    fun getShareableUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }
}

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxExportManager {

    private const val EXPORT_DIR = "exports"

    suspend fun exportDocumentToDocx(
        context: Context,
        documentWithPages: DocumentWithPages,
        metadata: BookMetadataEntity? = null,
        chapters: List<ChapterEntity> = emptyList(),
        tocList: List<TocEntryEntity> = emptyList(),
        tables: List<TableEntity> = emptyList(),
        options: ExportOptions = ExportOptions(),
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, EXPORT_DIR).apply { mkdirs() }
        val safeTitle = documentWithPages.document.title
            .replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            .take(35)
            .ifEmpty { "document" }

        val fileName = "Raqeem_${safeTitle}_${System.currentTimeMillis()}.docx"
        val outputFile = File(exportDir, fileName)

        val docXmlContent = buildDocumentXml(context, documentWithPages, metadata, chapters, tocList, tables, options, onProgress)
        val stylesXmlContent = buildStylesXml()
        val contentTypesXml = buildContentTypesXml()
        val relsXml = buildRelsXml()
        val docRelsXml = buildDocumentRelsXml()

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(contentTypesXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(relsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. word/_rels/document.xml.rels
            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zos.write(docRelsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. word/styles.xml
            zos.putNextEntry(ZipEntry("word/styles.xml"))
            zos.write(stylesXmlContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 5. word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(docXmlContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        outputFile
    }

    private fun buildDocumentXml(
        context: Context,
        documentWithPages: DocumentWithPages,
        metadata: BookMetadataEntity?,
        chapters: List<ChapterEntity>,
        tocList: List<TocEntryEntity>,
        tables: List<TableEntity>,
        options: ExportOptions,
        onProgress: (current: Int, total: Int) -> Unit
    ): String {
        val doc = documentWithPages.document
        val pages = documentWithPages.pages.sortedBy { it.pageIndex }
        val totalPages = pages.size

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" ")
        sb.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
        sb.append("  <w:body>\n")

        // 1. Cover / Title Page
        if (options.includeCoverPage) {
            sb.append("    <w:p><w:pPr><w:pStyle w:val=\"Title\"/><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr>")
            sb.append("<w:r><w:rPr><w:b/><w:sz w:val=\"56\"/></w:rPr><w:t>").append(escapeXml(doc.title)).append("</w:t></w:r></w:p>\n")

            sb.append("    <w:p><w:pPr><w:pStyle w:val=\"Subtitle\"/><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr>")
            sb.append("<w:r><w:rPr><w:i/><w:sz w:val=\"32\"/></w:rPr><w:t>منصة رقيم للنشر الرقمي والأرشفة</w:t></w:r></w:p>\n")

            sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr>")
            sb.append("<w:r><w:rPr><w:sz w:val=\"28\"/></w:rPr><w:t>المؤلف: ").append(escapeXml(doc.authorName.ifBlank { "غير محدد" })).append("</w:t></w:r></w:p>\n")

            sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr>")
            sb.append("<w:r><w:rPr><w:sz w:val=\"24\"/></w:rPr><w:t>التصنيف: ").append(escapeXml(doc.category)).append("</w:t></w:r></w:p>\n")

            sb.append("    <w:p><w:pPr><w:pageBreakBefore/></w:pPr></w:p>\n")
        }

        // 2. Metadata Block
        if (options.includeMetadataPage && metadata != null) {
            sb.append("    <w:p><w:pPr><w:pStyle w:val=\"Heading1\"/><w:bdr/><w:rtl/><w:jc w:val=\"right\"/></w:pPr>")
            sb.append("<w:r><w:rPr><w:b/><w:sz w:val=\"36\"/></w:rPr><w:t>بطاقة الكتاب والبيانات الببليوجرافية</w:t></w:r></w:p>\n")

            val metaFields = listOf(
                "عنوان الكتاب" to metadata.title,
                "المؤلف" to metadata.author,
                "الناشر" to metadata.publisher,
                "سنة النشر" to (metadata.publicationYear?.toString() ?: ""),
                "رقم الطبعة" to metadata.edition,
                "الرقم الدولي ISBN" to metadata.isbn,
                "التصنيف" to metadata.category,
                "عدد الصفحات الأصلية" to metadata.originalPageCount.toString()
            ).filter { it.second.isNotBlank() }

            metaFields.forEach { (key, valStr) ->
                sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"right\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:b/></w:rPr><w:t>• ").append(escapeXml(key)).append(": </w:t></w:r>")
                sb.append("<w:r><w:t>").append(escapeXml(valStr)).append("</w:t></w:r></w:p>\n")
            }

            sb.append("    <w:p><w:pPr><w:pageBreakBefore/></w:pPr></w:p>\n")
        }

        // 3. Table of Contents
        if (options.includeTableOfContents && (tocList.isNotEmpty() || chapters.isNotEmpty())) {
            sb.append("    <w:p><w:pPr><w:pStyle w:val=\"Heading1\"/><w:bdr/><w:rtl/><w:jc w:val=\"right\"/></w:pPr>")
            sb.append("<w:r><w:rPr><w:b/><w:sz w:val=\"36\"/></w:rPr><w:t>فهرس المحتويات (TOC)</w:t></w:r></w:p>\n")

            if (chapters.isNotEmpty()) {
                chapters.sortedBy { it.readingOrder }.forEach { ch ->
                    sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"right\"/></w:pPr>")
                    sb.append("<w:r><w:rPr><w:b/></w:rPr><w:t>• ").append(escapeXml(ch.title)).append(" ........ ص ").append(ch.startPageIndex + 1).append("</w:t></w:r></w:p>\n")
                }
            } else if (tocList.isNotEmpty()) {
                tocList.sortedBy { it.readingOrder }.forEach { entry ->
                    sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"right\"/></w:pPr>")
                    sb.append("<w:r><w:t>").append("  ".repeat(entry.level)).append("• ").append(escapeXml(entry.title)).append(" ........ ص ").append(entry.targetPhysicalPageIndex + 1).append("</w:t></w:r></w:p>\n")
                }
            }

            sb.append("    <w:p><w:pPr><w:pageBreakBefore/></w:pPr></w:p>\n")
        }

        // 4. Main Body Content Pages
        pages.forEachIndexed { idx, page ->
            onProgress(idx + 1, totalPages)

            if (options.includePageMarkers) {
                sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:b/><w:color w:val=\"555555\"/><w:sz w:val=\"20\"/></w:rPr><w:t>--- [الصفحة ").append(idx + 1).append("] ---</w:t></w:r></w:p>\n")
            }

            val pageText = page.effectiveOcrText
            if (pageText.isNotBlank()) {
                val formatted = OcrTextFormatter.formatTextForExport(
                    rawText = pageText,
                    cleanLineBreaks = options.cleanLineBreaks,
                    fixRepeatedSpaces = options.fixRepeatedSpaces
                )

                val paragraphs = formatted.split("\n\n")
                paragraphs.forEach { p ->
                    val trimmed = p.trim()
                    if (trimmed.isNotBlank()) {
                        val isHeading = OcrTextFormatter.isHeadingLine(trimmed)
                        sb.append("    <w:p><w:pPr>")
                        if (isHeading) {
                            sb.append("<w:pStyle w:val=\"Heading2\"/>")
                        }
                        sb.append("<w:bdr/><w:rtl/><w:jc w:val=\"").append(if (isHeading) "center" else "both").append("\"/></w:pPr>")
                        sb.append("<w:r><w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\" w:cs=\"Traditional Arabic\"/><w:sz w:val=\"28\"/></w:rPr><w:t>")
                        sb.append(escapeXml(trimmed))
                        sb.append("</w:t></w:r></w:p>\n")
                    }
                }
            } else {
                sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:i/><w:color w:val=\"888888\"/></w:rPr><w:t>(لا يوجد نص مستخرج لهذه الصفحة)</w:t></w:r></w:p>\n")
            }
        }

        // 5. Official Raqeem Rights Section
        if (options.includeRightsPage) {
            sb.append("    <w:p><w:pPr><w:pageBreakBefore/></w:pPr></w:p>\n")
            sb.append("    <w:p><w:pPr><w:pStyle w:val=\"Title\"/><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr>")
            sb.append("<w:r><w:rPr><w:b/><w:color w:val=\"1A4D2E\"/><w:sz w:val=\"48\"/></w:rPr><w:t>رَقِيم — RAQEEM</w:t></w:r></w:p>\n")

            val line1 = "تمت أرشفة هذا الكتاب بواسطة منصة رقيم"
            val line2 = "من الورق إلى الرقمنة"
            val attr = context.getString(com.example.R.string.library_attribution)
            val dev = "برمجة: همام محمد"
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"32\"/></w:rPr><w:t>").append(escapeXml(line1)).append("</w:t></w:r></w:p>\n")
            sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:i/><w:sz w:val=\"26\"/></w:rPr><w:t>").append(escapeXml(line2)).append("</w:t></w:r></w:p>\n")
            sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:b/><w:color w:val=\"8C3C0A\"/><w:sz w:val=\"28\"/></w:rPr><w:t>").append(escapeXml(attr)).append("</w:t></w:r></w:p>\n")
            sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"26\"/></w:rPr><w:t>").append(escapeXml(dev)).append("</w:t></w:r></w:p>\n")
            sb.append("    <w:p><w:pPr><w:bdr/><w:rtl/><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:color w:val=\"666666\"/><w:sz w:val=\"22\"/></w:rPr><w:t>تاريخ التصدير الرقمي: ").append(dateStr).append(" • رقيم V1.0</w:t></w:r></w:p>\n")
        }

        sb.append("  </w:body>\n")
        sb.append("</w:document>")
        return sb.toString()
    }

    private fun buildStylesXml(): String {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n" +
                "  <w:style w:type=\"paragraph\" w:styleId=\"Title\">\n" +
                "    <w:name w:val=\"Title\"/>\n" +
                "    <w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\" w:cs=\"Traditional Arabic\"/><w:b/><w:sz w:val=\"52\"/></w:rPr>\n" +
                "  </w:style>\n" +
                "  <w:style w:type=\"paragraph\" w:styleId=\"Subtitle\">\n" +
                "    <w:name w:val=\"Subtitle\"/>\n" +
                "    <w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\" w:cs=\"Traditional Arabic\"/><w:i/><w:sz w:val=\"32\"/></w:rPr>\n" +
                "  </w:style>\n" +
                "  <w:style w:type=\"paragraph\" w:styleId=\"Heading1\">\n" +
                "    <w:name w:val=\"heading 1\"/>\n" +
                "    <w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\" w:cs=\"Traditional Arabic\"/><w:b/><w:sz w:val=\"36\"/><w:color w:val=\"1A4D2E\"/></w:rPr>\n" +
                "  </w:style>\n" +
                "  <w:style w:type=\"paragraph\" w:styleId=\"Heading2\">\n" +
                "    <w:name w:val=\"heading 2\"/>\n" +
                "    <w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\" w:cs=\"Traditional Arabic\"/><w:b/><w:sz w:val=\"30\"/><w:color w:val=\"222222\"/></w:rPr>\n" +
                "  </w:style>\n" +
                "</w:styles>"
    }

    private fun buildContentTypesXml(): String {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                "  <Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n" +
                "  <Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/>\n" +
                "</Types>"
    }

    private fun buildRelsXml(): String {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n" +
                "</Relationships>"
    }

    private fun buildDocumentRelsXml(): String {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n" +
                "</Relationships>"
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun getShareableUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}

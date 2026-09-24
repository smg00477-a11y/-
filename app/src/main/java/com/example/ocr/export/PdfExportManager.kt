package com.example.ocr.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
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

object PdfExportManager {

    private const val TAG = "PdfExportManager"

    // High resolution print A4 dimensions at 150 DPI
    private const val PAGE_WIDTH = 1240
    private const val PAGE_HEIGHT = 1754
    private const val MARGIN_X = 90f
    private const val MARGIN_TOP = 110f
    private const val MARGIN_BOTTOM = 110f
    private const val CONTENT_WIDTH = (PAGE_WIDTH - 2 * MARGIN_X).toInt()

    suspend fun generateDocumentPdfWithOptions(
        context: Context,
        documentWithPages: DocumentWithPages,
        metadata: BookMetadataEntity? = null,
        chapters: List<ChapterEntity> = emptyList(),
        tocList: List<TocEntryEntity> = emptyList(),
        tables: List<TableEntity> = emptyList(),
        options: ExportOptions = ExportOptions(),
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val document = documentWithPages.document
        val pdfDocument = PdfDocument()

        try {
            var currentPageNumber = 1

            // Choice of PDF Mode: Structured Text vs Image Scan vs Hybrid
            when (options.pdfMode) {
                PdfExportMode.STRUCTURED_TEXT -> {
                    // 1. Cover Page
                    if (options.includeCoverPage) {
                        val coverInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                        val coverPage = pdfDocument.startPage(coverInfo)
                        drawCoverPage(context, coverPage.canvas, document.title, document.authorName, document.category)
                        pdfDocument.finishPage(coverPage)
                        currentPageNumber++
                    }

                    // 2. Metadata Page
                    if (options.includeMetadataPage && metadata != null) {
                        val metaInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                        val metaPage = pdfDocument.startPage(metaInfo)
                        drawMetadataPage(coverCanvas = metaPage.canvas, metadata = metadata)
                        pdfDocument.finishPage(metaPage)
                        currentPageNumber++
                    }

                    // 3. Table of Contents
                    if (options.includeTableOfContents && (chapters.isNotEmpty() || tocList.isNotEmpty())) {
                        val tocInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                        val tocPage = pdfDocument.startPage(tocInfo)
                        drawTocPage(tocPage.canvas, chapters, tocList)
                        pdfDocument.finishPage(tocPage)
                        currentPageNumber++
                    }

                    // 4. Content Pages with Structured Text Layout
                    val pages = documentWithPages.pages.sortedBy { it.pageIndex }
                    val totalPages = pages.size

                    pages.forEachIndexed { index, pageEntity ->
                        onProgress(index + 1, totalPages)

                        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                        val pdfPage = pdfDocument.startPage(pageInfo)
                        val canvas = pdfPage.canvas

                        canvas.drawColor(Color.WHITE)
                        drawHeaderFooter(canvas, document.title, index + 1, totalPages)

                        val rawText = pageEntity.effectiveOcrText
                        if (rawText.isNotBlank()) {
                            val formatted = OcrTextFormatter.formatTextForExport(
                                rawText = rawText,
                                cleanLineBreaks = options.cleanLineBreaks,
                                fixRepeatedSpaces = options.fixRepeatedSpaces
                            )
                            drawStructuredTextOnCanvas(canvas, formatted, index + 1)
                        } else {
                            drawMissingTextNotice(canvas, index + 1)
                        }

                        pdfDocument.finishPage(pdfPage)
                        currentPageNumber++
                    }
                }

                PdfExportMode.IMAGE_SCAN -> {
                    // Render high quality scanned page images
                    val pages = documentWithPages.pages.sortedBy { it.pageIndex }
                    val totalPages = pages.size

                    pages.forEachIndexed { index, pageEntity ->
                        onProgress(index + 1, totalPages)

                        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                        val pdfPage = pdfDocument.startPage(pageInfo)
                        val canvas = pdfPage.canvas

                        canvas.drawColor(Color.WHITE)

                        val imagePath = pageEntity.displayFilePath
                        val imageFile = File(imagePath)
                        if (imageFile.exists()) {
                            val bitmap = decodeSampledBitmap(imagePath, PAGE_WIDTH, PAGE_HEIGHT)
                            if (bitmap != null) {
                                drawFitCenter(canvas, bitmap, PAGE_WIDTH, PAGE_HEIGHT)
                                bitmap.recycle()
                            }
                        } else {
                            drawMissingPageNotice(canvas, index + 1)
                        }

                        pdfDocument.finishPage(pdfPage)
                        currentPageNumber++
                    }
                }

                PdfExportMode.HYBRID -> {
                    val pages = documentWithPages.pages.sortedBy { it.pageIndex }
                    val totalPages = pages.size

                    pages.forEachIndexed { index, pageEntity ->
                        onProgress(index + 1, totalPages)

                        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                        val pdfPage = pdfDocument.startPage(pageInfo)
                        val canvas = pdfPage.canvas

                        canvas.drawColor(Color.WHITE)

                        val imagePath = pageEntity.displayFilePath
                        if (File(imagePath).exists()) {
                            val bitmap = decodeSampledBitmap(imagePath, PAGE_WIDTH, PAGE_HEIGHT)
                            if (bitmap != null) {
                                drawFitCenter(canvas, bitmap, PAGE_WIDTH, PAGE_HEIGHT)
                                bitmap.recycle()
                            }
                        }

                        pdfDocument.finishPage(pdfPage)
                        currentPageNumber++
                    }
                }
            }

            // 5. Official Raqeem Rights Page
            if (options.includeRightsPage) {
                val rightsPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                val rightsPage = pdfDocument.startPage(rightsPageInfo)
                drawOfficialRightsPage(context, rightsPage.canvas, document.title, documentWithPages.pageCount, document.createdAt)
                pdfDocument.finishPage(rightsPage)
            }

            // Save to exports directory
            val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
            val sanitizedTitle = document.title
                .replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
                .take(35)
                .ifEmpty { "book" }
            val fileName = "Raqeem_${sanitizedTitle}_${System.currentTimeMillis()}.pdf"
            val outputFile = File(exportsDir, fileName)

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            Log.d(TAG, "PDF generated successfully at ${outputFile.absolutePath}")
            outputFile
        } finally {
            pdfDocument.close()
        }
    }

    suspend fun generateDocumentPdf(
        context: Context,
        documentWithPages: DocumentWithPages,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): File {
        return generateDocumentPdfWithOptions(
            context = context,
            documentWithPages = documentWithPages,
            options = ExportOptions(format = ExportFormat.PDF, pdfMode = PdfExportMode.IMAGE_SCAN),
            onProgress = onProgress
        )
    }

    private fun drawStructuredTextOnCanvas(canvas: Canvas, text: String, pageNumber: Int) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 20, 20)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        canvas.save()
        canvas.translate(MARGIN_X, MARGIN_TOP)

        val builder = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, CONTENT_WIDTH)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(12f, 1.25f)
            .setIncludePad(true)

        val staticLayout = builder.build()
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawHeaderFooter(canvas: Canvas, bookTitle: String, pageIndex: Int, totalPages: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 120, 120)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Top Header
        canvas.drawText("رَقِيم — $bookTitle", PAGE_WIDTH - MARGIN_X, 60f, paint.apply { textAlign = Paint.Align.RIGHT })
        canvas.drawLine(MARGIN_X, 75f, PAGE_WIDTH - MARGIN_X, 75f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(220, 220, 220); strokeWidth = 1.5f })

        // Bottom Footer (Page number)
        val footerY = PAGE_HEIGHT - 50f
        canvas.drawLine(MARGIN_X, footerY - 20f, PAGE_WIDTH - MARGIN_X, footerY - 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(220, 220, 220); strokeWidth = 1.5f })
        canvas.drawText("صفحة $pageIndex من $totalPages", PAGE_WIDTH / 2f, footerY, paint.apply { textAlign = Paint.Align.CENTER })
    }

    private fun drawCoverPage(context: Context, canvas: Canvas, title: String, author: String, category: String) {
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 77, 46) // Deep Emerald
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(212, 175, 55) // Gold
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        canvas.drawRect(60f, 60f, PAGE_WIDTH - 60f, PAGE_HEIGHT - 60f, borderPaint)
        canvas.drawRect(72f, 72f, PAGE_WIDTH - 72f, PAGE_HEIGHT - 72f, innerBorder)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 20, 20)
            textSize = 54f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(80, 80, 80)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 77, 46)
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("رَقِيم", PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.25f, brandPaint)
        canvas.drawText(title, PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.45f, titlePaint)
        canvas.drawText("المؤلف: ${author.ifBlank { "غير محدد" }}", PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.55f, authorPaint)
        canvas.drawText("التصنيف: $category", PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.60f, authorPaint)
    }

    private fun drawMetadataPage(coverCanvas: Canvas, metadata: BookMetadataEntity) {
        coverCanvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 77, 46)
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 40, 40)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        var currentY = 180f
        coverCanvas.drawText("بطاقة الكتاب والبيانات الببليوجرافية", PAGE_WIDTH - MARGIN_X, currentY, titlePaint)
        currentY += 80f

        val items = listOf(
            "العنوان الكامل: ${metadata.title}",
            "المؤلف: ${metadata.author}",
            "الناشر: ${metadata.publisher}",
            "سنة النشر: ${metadata.publicationYear ?: "غير محدد"}",
            "الطبعة: ${metadata.edition}",
            "الرقم الدولي (ISBN): ${metadata.isbn}",
            "التصنيف: ${metadata.category}",
            "عدد الصفحات: ${metadata.originalPageCount}"
        )

        items.forEach { item ->
            coverCanvas.drawText("• $item", PAGE_WIDTH - MARGIN_X, currentY, textPaint)
            currentY += 55f
        }
    }

    private fun drawTocPage(canvas: Canvas, chapters: List<ChapterEntity>, tocList: List<TocEntryEntity>) {
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 77, 46)
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 30, 30)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        var currentY = 180f
        canvas.drawText("فهرس المحتويات", PAGE_WIDTH - MARGIN_X, currentY, titlePaint)
        currentY += 80f

        if (chapters.isNotEmpty()) {
            chapters.sortedBy { it.readingOrder }.forEach { ch ->
                val line = "• ${ch.title} ................................ ص ${ch.startPageIndex + 1}"
                canvas.drawText(line, PAGE_WIDTH - MARGIN_X, currentY, itemPaint)
                currentY += 50f
            }
        } else if (tocList.isNotEmpty()) {
            tocList.sortedBy { it.readingOrder }.forEach { entry ->
                val line = "• ${entry.title} ................................ ص ${entry.targetPhysicalPageIndex + 1}"
                canvas.drawText(line, PAGE_WIDTH - MARGIN_X, currentY, itemPaint)
                currentY += 50f
            }
        }
    }

    private fun drawOfficialRightsPage(
        context: Context,
        canvas: Canvas,
        bookTitle: String,
        originalPageCount: Int,
        archivedTimestamp: Long
    ) {
        val width = PAGE_WIDTH.toFloat()
        val height = PAGE_HEIGHT.toFloat()

        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(212, 175, 55)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(230, 230, 230)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val margin = 80f
        canvas.drawRect(margin, margin, width - margin, height - margin, borderPaint)
        canvas.drawRect(margin + 16f, margin + 16f, width - margin - 16f, height - margin - 16f, innerBorderPaint)

        val centerX = width / 2f
        var currentY = height * 0.26f

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 77, 46)
            textSize = 58f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("رَقِيم", centerX, currentY, brandPaint)
        currentY += 75f

        val line1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 30, 30)
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("تمت أرشفة هذا الكتاب بواسطة منصة رقيم", centerX, currentY, line1Paint)
        currentY += 65f

        val line2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 90, 90)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("من الورق إلى الرقمنة", centerX, currentY, line2Paint)
        currentY += 80f

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 200, 200)
            strokeWidth = 2f
        }
        canvas.drawLine(centerX - 180f, currentY, centerX + 180f, currentY, dividerPaint)
        currentY += 75f

        val attributionText = context.getString(com.example.R.string.library_attribution)
        val attrBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(245, 248, 245)
            style = Paint.Style.FILL
        }
        val attrBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 140, 40)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val boxWidth = 820f
        val boxRect = RectF(centerX - boxWidth / 2f, currentY - 45f, centerX + boxWidth / 2f, currentY + 25f)
        canvas.drawRoundRect(boxRect, 12f, 12f, attrBoxPaint)
        canvas.drawRoundRect(boxRect, 12f, 12f, attrBorderPaint)

        val attrTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(140, 60, 10)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(attributionText, centerX, currentY, attrTextPaint)
        currentY += 90f

        val line3Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 40, 40)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("برمجة: همام محمد", centerX, currentY, line3Paint)

        val metaY = height * 0.77f
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 120, 120)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(archivedTimestamp))
        canvas.drawText("عنوان الوثيقة: $bookTitle", centerX, metaY, metaPaint)
        canvas.drawText("عدد صفحات الكتاب الأصلي: $originalPageCount صفحة", centerX, metaY + 40f, metaPaint)
        canvas.drawText("تاريخ الأرشفة الرقمية: $dateStr • نظام رقيم V1.0", centerX, metaY + 80f, metaPaint)
        canvas.drawText("وثيقة رقمية محلية محفوظة بنسبة 100% دون اتصال بالإنترنت", centerX, metaY + 120f, metaPaint)
    }

    private fun drawFitCenter(canvas: Canvas, bitmap: Bitmap, targetW: Int, targetH: Int) {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()

        val scale = Math.min(targetW / srcW, targetH / srcH)
        val dstW = srcW * scale
        val dstH = srcH * scale

        val left = (targetW - dstW) / 2f
        val top = (targetH - dstH) / 2f

        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = RectF(left, top, left + dstW, top + dstH)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
    }

    private fun drawMissingTextNotice(canvas: Canvas, pageNumber: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("صفحة $pageNumber — لم يتم استخراج النص بعد", PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f, paint)
    }

    private fun drawMissingPageNotice(canvas: Canvas, pageNumber: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("الصفحة $pageNumber غير متوفرة محلياً", PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f, paint)
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun getShareableUri(context: Context, pdfFile: File): android.net.Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val uri = getShareableUri(context, pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "مشاركة كتاب رقيم بصيغة PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

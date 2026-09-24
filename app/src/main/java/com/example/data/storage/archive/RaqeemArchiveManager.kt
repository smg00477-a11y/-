package com.example.data.storage.archive

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.entity.*
import com.example.data.model.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class DuplicateStrategy {
    SKIP_EXISTING,
    OVERWRITE_EXISTING,
    KEEP_BOTH_CREATE_COPY
}

data class ArchiveValidationResult(
    val isValid: Boolean,
    val archiveVersion: String,
    val generatorApp: String,
    val documentCount: Int,
    val documentTitles: List<String>,
    val errorMessage: String? = null
)

data class ImportResult(
    val importedCount: Int,
    val skippedCount: Int,
    val overwrittenCount: Int,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class RaqeemArchiveManager(
    private val context: Context,
    private val database: AppDatabase
) {

    companion object {
        const val CURRENT_ARCHIVE_VERSION = "RAQEEM_ARCHIVE_V1.0"
    }

    suspend fun exportDocumentArchive(
        documentId: Long,
        destinationFile: File,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): File? = withContext(Dispatchers.IO) {
        exportLibraryArchive(listOf(documentId), destinationFile, onProgress)
    }

    suspend fun exportLibraryArchive(
        documentIds: List<Long>? = null,
        destinationFile: File,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): File? = withContext(Dispatchers.IO) {
        val targetDocs = if (documentIds == null) {
            database.documentDao().getAllDocumentsDirect()
        } else {
            documentIds.mapNotNull { database.documentDao().getDocumentById(it) }
        }

        if (targetDocs.isEmpty()) return@withContext null

        try {
            destinationFile.parentFile?.mkdirs()
            FileOutputStream(destinationFile).use { fos ->
                ZipOutputStream(BufferedOutputStream(fos)).use { zipOut ->

                    val docManifests = JSONArray()

                    targetDocs.forEachIndexed { docIdx, doc ->
                        val progress = (docIdx + 1).toFloat() / targetDocs.size
                        onProgress(progress, "تصدير الكتاب: ${doc.title}")

                        val docFolder = "data/document_${doc.id}"

                        // 1. Metadata
                        val metadata = database.bookMetadataDao().getMetadataForDocumentDirect(doc.id)
                        val metaObj = JSONObject().apply {
                            put("id", doc.id)
                            put("title", doc.title)
                            put("type", doc.type.name)
                            put("authorName", doc.authorName)
                            put("category", doc.category)
                            put("originalPageCount", doc.originalPageCount)
                            metadata?.let { meta ->
                                put("subtitle", meta.subtitle)
                                put("translator", meta.translator)
                                put("editor", meta.editor)
                                put("publisher", meta.publisher)
                                put("publicationYear", meta.publicationYear)
                                put("edition", meta.edition)
                                put("isbn", meta.isbn)
                                put("language", meta.language)
                                put("subject", meta.subject)
                                put("tags", meta.tags)
                                put("description", meta.description)
                                put("notes", meta.notes)
                            }
                        }
                        writeZipTextEntry(zipOut, "$docFolder/metadata.json", metaObj.toString(2))

                        // 2. Chapters & TOC
                        val chapters = database.chapterDao().getChaptersForDocumentDirect(doc.id)
                        val toc = database.tocDao().getTocForDocumentDirect(doc.id)
                        val structureObj = JSONObject().apply {
                            put("chapters", JSONArray().apply {
                                chapters.forEach { ch ->
                                    put(JSONObject().apply {
                                        put("title", ch.title)
                                        put("startPageIndex", ch.startPageIndex)
                                        put("endPageIndex", ch.endPageIndex)
                                        put("readingOrder", ch.readingOrder)
                                        put("level", ch.level)
                                    })
                                }
                            })
                            put("toc", JSONArray().apply {
                                toc.forEach { t ->
                                    put(JSONObject().apply {
                                        put("title", t.title)
                                        put("targetPhysicalPageIndex", t.targetPhysicalPageIndex)
                                        put("targetPrintedPage", t.targetPrintedPage)
                                        put("level", t.level)
                                    })
                                }
                            })
                        }
                        writeZipTextEntry(zipOut, "$docFolder/structure.json", structureObj.toString(2))

                        // 3. User Data (Bookmarks & Notes)
                        val bookmarks = database.bookmarkDao().getBookmarksForDocumentDirect(doc.id)
                        val notes = database.bookNoteDao().getNotesForDocumentDirect(doc.id)
                        val userObj = JSONObject().apply {
                            put("bookmarks", JSONArray().apply {
                                bookmarks.forEach { b ->
                                    put(JSONObject().apply {
                                        put("pageIndex", b.pageIndex)
                                        put("chapterTitle", b.chapterTitle)
                                        put("title", b.title)
                                        put("note", b.note)
                                    })
                                }
                            })
                            put("notes", JSONArray().apply {
                                notes.forEach { n ->
                                    put(JSONObject().apply {
                                        put("pageIndex", n.pageIndex)
                                        put("selectedText", n.selectedText)
                                        put("content", n.content)
                                    })
                                }
                            })
                        }
                        writeZipTextEntry(zipOut, "$docFolder/user_data.json", userObj.toString(2))

                        // 4. Pages & Image Files
                        val pages = database.pageDao().getPagesForDocumentDirect(doc.id)
                        val pagesArr = JSONArray()

                        pages.forEach { page ->
                            val pageFolder = "$docFolder/pages/page_${page.pageIndex}"
                            var relOrigPath = ""
                            var relProcPath = ""
                            var relThumbPath = ""

                            val origFile = File(page.localFilePath)
                            if (origFile.exists()) {
                                relOrigPath = "$pageFolder/orig.jpg"
                                writeZipFileEntry(zipOut, relOrigPath, origFile)
                            }

                            page.processedFilePath?.let { procPath ->
                                val procFile = File(procPath)
                                if (procFile.exists()) {
                                    relProcPath = "$pageFolder/proc.jpg"
                                    writeZipFileEntry(zipOut, relProcPath, procFile)
                                }
                            }

                            page.thumbnailPath?.let { thumbPath ->
                                val thumbFile = File(thumbPath)
                                if (thumbFile.exists()) {
                                    relThumbPath = "$pageFolder/thumb.jpg"
                                    writeZipFileEntry(zipOut, relThumbPath, thumbFile)
                                }
                            }

                            pagesArr.put(JSONObject().apply {
                                put("pageIndex", page.pageIndex)
                                put("origRelPath", relOrigPath)
                                put("procRelPath", relProcPath)
                                put("thumbRelPath", relThumbPath)
                                put("processingState", page.processingState)
                                put("processingMode", page.processingMode)
                                put("rotationDegrees", page.rotationDegrees)
                                put("ocrStatus", page.ocrStatus)
                                put("ocrRawText", page.ocrRawText)
                                put("ocrCleanedText", page.ocrCleanedText)
                                put("ocrUserEditedText", page.ocrUserEditedText ?: "")
                                put("ocrLanguage", page.ocrLanguage)
                                put("ocrConfidence", page.ocrConfidence)
                                put("pageType", page.pageType)
                            })
                        }

                        writeZipTextEntry(zipOut, "$docFolder/pages.json", JSONObject().apply {
                            put("pages", pagesArr)
                        }.toString(2))

                        docManifests.put(JSONObject().apply {
                            put("id", doc.id)
                            put("title", doc.title)
                            put("authorName", doc.authorName)
                            put("folder", docFolder)
                            put("pageCount", pages.size)
                        })
                    }

                    // Main Manifest
                    val mainManifest = JSONObject().apply {
                        put("archiveVersion", CURRENT_ARCHIVE_VERSION)
                        put("appVersion", "0.8")
                        put("generatorApp", "منصة رقيم للأرشفة والتنسيق الرقمي")
                        put("exportTimestamp", System.currentTimeMillis())
                        put("documentCount", targetDocs.size)
                        put("documents", docManifests)
                    }

                    writeZipTextEntry(zipOut, "manifest.json", mainManifest.toString(2))
                }
            }

            if (destinationFile.exists() && destinationFile.length() > 0) {
                destinationFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun validateArchive(archiveFile: File): ArchiveValidationResult = withContext(Dispatchers.IO) {
        if (!archiveFile.exists() || archiveFile.length() == 0L) {
            return@withContext ArchiveValidationResult(false, "", "", 0, emptyList(), "ملف الأرشيف غير موجود أو فارغ")
        }

        try {
            var manifestContent: String? = null
            ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    if (entry.name == "manifest.json") {
                        manifestContent = zipIn.bufferedReader().readText()
                        break
                    }
                    entry = zipIn.nextEntry
                }
            }

            if (manifestContent.isNullOrBlank()) {
                return@withContext ArchiveValidationResult(false, "", "", 0, emptyList(), "ملف البيان (manifest.json) مفقود من حزمة الأرشيف")
            }

            val manifestObj = JSONObject(manifestContent)
            val archiveVer = manifestObj.optString("archiveVersion", "UNKNOWN")
            val generator = manifestObj.optString("generatorApp", "UNKNOWN")
            val docCount = manifestObj.optInt("documentCount", 0)

            val titlesList = mutableListOf<String>()
            val docsArr = manifestObj.optJSONArray("documents")
            if (docsArr != null) {
                for (i in 0 until docsArr.length()) {
                    val d = docsArr.getJSONObject(i)
                    titlesList.add(d.optString("title", "كتاب بدون عنوان"))
                }
            }

            ArchiveValidationResult(
                isValid = archiveVer == CURRENT_ARCHIVE_VERSION,
                archiveVersion = archiveVer,
                generatorApp = generator,
                documentCount = docCount,
                documentTitles = titlesList,
                errorMessage = if (archiveVer != CURRENT_ARCHIVE_VERSION) "إصدار الأرشيف ($archiveVer) غير متوافق" else null
            )
        } catch (e: Exception) {
            ArchiveValidationResult(false, "", "", 0, emptyList(), "خطأ في قراءة الأرشيف: ${e.localizedMessage}")
        }
    }

    suspend fun importArchive(
        archiveFile: File,
        duplicateStrategy: DuplicateStrategy,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): ImportResult = withContext(Dispatchers.IO) {
        val validation = validateArchive(archiveFile)
        if (!validation.isValid) {
            return@withContext ImportResult(0, 0, 0, false, validation.errorMessage ?: "فشلت عملية التحقق من الأرشيف")
        }

        var importedCount = 0
        var skippedCount = 0
        var overwrittenCount = 0

        val tempUnpackDir = File(context.cacheDir, "unpack_${UUID.randomUUID()}")
        tempUnpackDir.mkdirs()

        try {
            onProgress(0.1f, "فك ضغط حزمة الأرشيف...")
            unzipArchive(archiveFile, tempUnpackDir)

            val manifestFile = File(tempUnpackDir, "manifest.json")
            val manifestObj = JSONObject(manifestFile.readText())
            val docsArr = manifestObj.getJSONArray("documents")

            for (i in 0 until docsArr.length()) {
                val docInfo = docsArr.getJSONObject(i)
                val docFolderRel = docInfo.getString("folder")
                val docFolder = File(tempUnpackDir, docFolderRel)
                val originalTitle = docInfo.getString("title")
                val authorName = docInfo.optString("authorName", "غير محدد")

                onProgress((i + 1).toFloat() / docsArr.length(), "استيراد الكتاب: $originalTitle")

                // Check duplicates
                val existingDoc = database.documentDao().getAllDocumentsDirect()
                    .find { it.title.equals(originalTitle, ignoreCase = true) && it.authorName.equals(authorName, ignoreCase = true) }

                if (existingDoc != null) {
                    when (duplicateStrategy) {
                        DuplicateStrategy.SKIP_EXISTING -> {
                            skippedCount++
                            continue
                        }
                        DuplicateStrategy.OVERWRITE_EXISTING -> {
                            database.documentDao().deleteDocumentById(existingDoc.id)
                            overwrittenCount++
                        }
                        DuplicateStrategy.KEEP_BOTH_CREATE_COPY -> {
                            // Proceed with new title
                        }
                    }
                }

                val finalTitle = if (existingDoc != null && duplicateStrategy == DuplicateStrategy.KEEP_BOTH_CREATE_COPY) {
                    "$originalTitle (نسخة مستوردة)"
                } else {
                    originalTitle
                }

                // Parse files from docFolder
                val metaFile = File(docFolder, "metadata.json")
                val pagesFile = File(docFolder, "pages.json")
                val structureFile = File(docFolder, "structure.json")
                val userDataFile = File(docFolder, "user_data.json")

                if (!metaFile.exists() || !pagesFile.exists()) {
                    skippedCount++
                    continue
                }

                val metaObj = JSONObject(metaFile.readText())
                val category = metaObj.optString("category", "عام")

                val newDocId = database.documentDao().insertDocument(
                    DocumentEntity(
                        title = finalTitle,
                        type = DocumentType.BOOK,
                        authorName = authorName,
                        category = category,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )

                // Insert Metadata
                database.bookMetadataDao().insertMetadata(
                    BookMetadataEntity(
                        documentId = newDocId,
                        title = finalTitle,
                        subtitle = metaObj.optString("subtitle", ""),
                        author = authorName,
                        translator = metaObj.optString("translator", ""),
                        editor = metaObj.optString("editor", ""),
                        publisher = metaObj.optString("publisher", ""),
                        publicationYear = metaObj.optString("publicationYear", ""),
                        edition = metaObj.optString("edition", ""),
                        isbn = metaObj.optString("isbn", ""),
                        language = metaObj.optString("language", "ar"),
                        category = category,
                        subject = metaObj.optString("subject", ""),
                        tags = metaObj.optString("tags", ""),
                        description = metaObj.optString("description", ""),
                        notes = metaObj.optString("notes", ""),
                        originalPageCount = metaObj.optInt("originalPageCount", 0),
                        archiveDate = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )

                // Insert Pages & Images
                val pagesObj = JSONObject(pagesFile.readText())
                val pagesArr = pagesObj.getJSONArray("pages")

                for (pIdx in 0 until pagesArr.length()) {
                    val p = pagesArr.getJSONObject(pIdx)
                    val pageIndex = p.getInt("pageIndex")

                    val origRel = p.optString("origRelPath", "")
                    val procRel = p.optString("procRelPath", "")
                    val thumbRel = p.optString("thumbRelPath", "")

                    var finalOrigPath = ""
                    var finalProcPath: String? = null
                    var finalThumbPath: String? = null

                    if (origRel.isNotBlank()) {
                        val srcFile = File(tempUnpackDir, origRel)
                        if (srcFile.exists()) {
                            val dest = File(context.filesDir, "documents/PAGE_${newDocId}_${pageIndex}_orig.jpg")
                            srcFile.copyTo(dest, overwrite = true)
                            finalOrigPath = dest.absolutePath
                        }
                    }

                    if (procRel.isNotBlank()) {
                        val srcFile = File(tempUnpackDir, procRel)
                        if (srcFile.exists()) {
                            val dest = File(context.filesDir, "documents/processed/PAGE_${newDocId}_${pageIndex}_proc.jpg")
                            dest.parentFile?.mkdirs()
                            srcFile.copyTo(dest, overwrite = true)
                            finalProcPath = dest.absolutePath
                        }
                    }

                    if (thumbRel.isNotBlank()) {
                        val srcFile = File(tempUnpackDir, thumbRel)
                        if (srcFile.exists()) {
                            val dest = File(context.filesDir, "documents/thumbnails/PAGE_${newDocId}_${pageIndex}_thumb.jpg")
                            dest.parentFile?.mkdirs()
                            srcFile.copyTo(dest, overwrite = true)
                            finalThumbPath = dest.absolutePath
                        }
                    }

                    if (finalOrigPath.isBlank()) continue

                    database.pageDao().insertPage(
                        PageEntity(
                            documentId = newDocId,
                            pageIndex = pageIndex,
                            localFilePath = finalOrigPath,
                            processedFilePath = finalProcPath,
                            thumbnailPath = finalThumbPath,
                            processingState = p.optString("processingState", "COMPLETED"),
                            processingMode = p.optString("processingMode", "ORIGINAL"),
                            rotationDegrees = p.optInt("rotationDegrees", 0),
                            ocrStatus = p.optString("ocrStatus", "COMPLETED"),
                            ocrRawText = p.optString("ocrRawText", ""),
                            ocrCleanedText = p.optString("ocrCleanedText", ""),
                            ocrUserEditedText = p.optString("ocrUserEditedText", null),
                            ocrLanguage = p.optString("ocrLanguage", "ara+eng"),
                            ocrConfidence = p.optDouble("ocrConfidence", 0.9).toFloat(),
                            pageType = p.optString("pageType", "PRINTED")
                        )
                    )
                }

                // Insert Chapters
                if (structureFile.exists()) {
                    val structObj = JSONObject(structureFile.readText())
                    val chArr = structObj.optJSONArray("chapters")
                    if (chArr != null) {
                        for (cIdx in 0 until chArr.length()) {
                            val c = chArr.getJSONObject(cIdx)
                            database.chapterDao().insertChapter(
                                ChapterEntity(
                                    documentId = newDocId,
                                    title = c.getString("title"),
                                    startPageIndex = c.getInt("startPageIndex"),
                                    endPageIndex = c.getInt("endPageIndex"),
                                    readingOrder = c.optInt("readingOrder", cIdx),
                                    level = c.optInt("level", 1)
                                )
                            )
                        }
                    }
                }

                // Insert User Data
                if (userDataFile.exists()) {
                    val userObj = JSONObject(userDataFile.readText())
                    val bArr = userObj.optJSONArray("bookmarks")
                    if (bArr != null) {
                        for (bIdx in 0 until bArr.length()) {
                            val b = bArr.getJSONObject(bIdx)
                            val pageIdx = b.getInt("pageIndex")
                            val pages = database.pageDao().getPagesForDocumentDirect(newDocId)
                            val p = pages.find { it.pageIndex == pageIdx }
                            if (p != null) {
                                database.bookmarkDao().insertBookmark(
                                    BookmarkEntity(
                                        documentId = newDocId,
                                        pageId = p.id,
                                        pageIndex = pageIdx,
                                        chapterTitle = b.optString("chapterTitle", ""),
                                        title = b.optString("title", "علامة مرجعية"),
                                        note = b.optString("note", "")
                                    )
                                )
                            }
                        }
                    }

                    val nArr = userObj.optJSONArray("notes")
                    if (nArr != null) {
                        for (nIdx in 0 until nArr.length()) {
                            val n = nArr.getJSONObject(nIdx)
                            database.bookNoteDao().insertNote(
                                BookNoteEntity(
                                    documentId = newDocId,
                                    pageIndex = n.optInt("pageIndex", 0),
                                    selectedText = n.optString("selectedText", ""),
                                    content = n.getString("content")
                                )
                            )
                        }
                    }
                }

                importedCount++
            }

            tempUnpackDir.deleteRecursively()

            ImportResult(
                importedCount = importedCount,
                skippedCount = skippedCount,
                overwrittenCount = overwrittenCount,
                isSuccess = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            tempUnpackDir.deleteRecursively()
            ImportResult(importedCount, skippedCount, overwrittenCount, false, "فشل الاستيراد: ${e.localizedMessage}")
        }
    }

    private fun writeZipTextEntry(zipOut: ZipOutputStream, entryName: String, text: String) {
        zipOut.putNextEntry(ZipEntry(entryName))
        zipOut.write(text.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
    }

    private fun writeZipFileEntry(zipOut: ZipOutputStream, entryName: String, file: File) {
        zipOut.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { fis ->
            fis.copyTo(zipOut)
        }
        zipOut.closeEntry()
    }

    private fun unzipArchive(zipFile: File, targetDir: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zipIn.copyTo(fos)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }
}

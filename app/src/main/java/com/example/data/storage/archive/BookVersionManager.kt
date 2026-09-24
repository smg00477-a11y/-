package com.example.data.storage.archive

import com.example.data.database.AppDatabase
import com.example.data.database.entity.BookVersionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BookVersionManager(private val database: AppDatabase) {

    fun getVersions(documentId: Long): Flow<List<BookVersionEntity>> {
        return database.bookVersionDao().getVersionsForDocument(documentId)
    }

    suspend fun createVersionSnapshot(
        documentId: Long,
        versionTag: String,
        description: String,
        changeSummary: String
    ): BookVersionEntity? = withContext(Dispatchers.IO) {
        val document = database.documentDao().getDocumentById(documentId) ?: return@withContext null
        val metadata = database.bookMetadataDao().getMetadataForDocumentDirect(documentId)
        val pages = database.pageDao().getPagesForDocumentDirect(documentId)
        val chapters = database.chapterDao().getChaptersForDocumentDirect(documentId)
        val tocEntries = database.tocDao().getTocForDocumentDirect(documentId)
        val bookmarks = database.bookmarkDao().getBookmarksForDocumentDirect(documentId)
        val notes = database.bookNoteDao().getNotesForDocumentDirect(documentId)

        val metadataObj = JSONObject().apply {
            put("title", document.title)
            put("type", document.type.name)
            put("authorName", document.authorName)
            put("category", document.category)
            put("originalPageCount", document.originalPageCount)
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

        val pagesArr = JSONArray().apply {
            pages.forEach { page ->
                put(JSONObject().apply {
                    put("id", page.id)
                    put("pageIndex", page.pageIndex)
                    put("localFilePath", page.localFilePath)
                    put("processedFilePath", page.processedFilePath ?: "")
                    put("thumbnailPath", page.thumbnailPath ?: "")
                    put("processingState", page.processingState)
                    put("processingMode", page.processingMode)
                    put("rotationDegrees", page.rotationDegrees)
                    put("ocrStatus", page.ocrStatus)
                    put("ocrCleanedText", page.ocrCleanedText)
                    put("ocrUserEditedText", page.ocrUserEditedText ?: "")
                    put("pageType", page.pageType)
                })
            }
        }

        val chaptersArr = JSONArray().apply {
            chapters.forEach { ch ->
                put(JSONObject().apply {
                    put("title", ch.title)
                    put("startPageIndex", ch.startPageIndex)
                    put("endPageIndex", ch.endPageIndex)
                    put("readingOrder", ch.readingOrder)
                    put("level", ch.level)
                })
            }
        }

        val fullSnapshotText = metadataObj.toString() + pagesArr.toString() + chaptersArr.toString()
        val snapshotHash = ChecksumUtility.calculateSha256(fullSnapshotText)

        val maxVersion = database.bookVersionDao().getMaxVersionNumber(documentId) ?: 0
        val newVersionNumber = maxVersion + 1

        database.bookVersionDao().deactivateAllVersions(documentId)

        val versionEntity = BookVersionEntity(
            documentId = documentId,
            versionNumber = newVersionNumber,
            versionTag = versionTag.ifBlank { "v$newVersionNumber.0" },
            description = description.ifBlank { "نسخة جديدة محفوظة تلقائياً" },
            isActive = true,
            checksumSha256 = snapshotHash,
            metadataSnapshotJson = metadataObj.toString(),
            pagesSnapshotJson = JSONObject().apply {
                put("pages", pagesArr)
                put("chapters", chaptersArr)
                put("bookmarksCount", bookmarks.size)
                put("notesCount", notes.size)
            }.toString(),
            changeSummary = changeSummary.ifBlank { "تغييرات في التنسيق والمحتوى" }
        )

        val versionId = database.bookVersionDao().insertVersion(versionEntity)
        versionEntity.copy(id = versionId)
    }

    suspend fun restoreVersion(versionId: Long): Boolean = withContext(Dispatchers.IO) {
        val version = database.bookVersionDao().getVersionById(versionId) ?: return@withContext false
        val documentId = version.documentId
        val document = database.documentDao().getDocumentById(documentId) ?: return@withContext false

        try {
            val metaObj = JSONObject(version.metadataSnapshotJson)
            val title = metaObj.optString("title", document.title)
            val authorName = metaObj.optString("authorName", document.authorName)
            val category = metaObj.optString("category", document.category)

            val updatedDoc = document.copy(
                title = title,
                authorName = authorName,
                category = category,
                updatedAt = System.currentTimeMillis()
            )
            database.documentDao().updateDocument(updatedDoc)

            val metadata = database.bookMetadataDao().getMetadataForDocumentDirect(documentId)
            if (metadata != null) {
                val updatedMeta = metadata.copy(
                    title = title,
                    subtitle = metaObj.optString("subtitle", metadata.subtitle),
                    translator = metaObj.optString("translator", metadata.translator),
                    editor = metaObj.optString("editor", metadata.editor),
                    publisher = metaObj.optString("publisher", metadata.publisher),
                    publicationYear = metaObj.optString("publicationYear", metadata.publicationYear),
                    edition = metaObj.optString("edition", metadata.edition),
                    isbn = metaObj.optString("isbn", metadata.isbn),
                    language = metaObj.optString("language", metadata.language),
                    subject = metaObj.optString("subject", metadata.subject),
                    tags = metaObj.optString("tags", metadata.tags),
                    description = metaObj.optString("description", metadata.description),
                    notes = metaObj.optString("notes", metadata.notes),
                    updatedAt = System.currentTimeMillis()
                )
                database.bookMetadataDao().updateMetadata(updatedMeta)
            }

            database.bookVersionDao().deactivateAllVersions(documentId)
            database.bookVersionDao().activateVersion(versionId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteVersion(versionId: Long) = withContext(Dispatchers.IO) {
        database.bookVersionDao().deleteVersion(versionId)
    }
}

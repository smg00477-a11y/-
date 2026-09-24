package com.example.ocr

import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.PageEntity
import com.example.data.model.DocumentType
import com.example.ocr.export.TxtExportManager
import org.junit.Assert.assertTrue
import org.junit.Test

class TxtExportManagerTest {

    @Test
    fun testBuildDocumentExportText() {
        val doc = DocumentEntity(
            id = 1L,
            title = "وثيقة تاريخية",
            type = DocumentType.DOCUMENT,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )

        val page1 = PageEntity(
            id = 10L,
            documentId = 1L,
            pageIndex = 0,
            localFilePath = "/dummy/p1.jpg",
            ocrRawText = "الصفحة الأولى من الوثيقة",
            ocrCleanedText = "الصفحة الأولى من الوثيقة"
        )

        val page2 = PageEntity(
            id = 11L,
            documentId = 1L,
            pageIndex = 1,
            localFilePath = "/dummy/p2.jpg",
            ocrRawText = "الصفحة الثانية ومحتواها",
            ocrUserEditedText = "الصفحة الثانية ومحتواها بعد المراجعة"
        )

        val docWithPages = DocumentWithPages(
            document = doc,
            pages = listOf(page1, page2)
        )

        val exportText = TxtExportManager.buildFullDocumentTextWithOptions(documentWithPages = docWithPages)

        assertTrue(exportText.contains("وثيقة تاريخية"))
        assertTrue(exportText.contains("الصفحة الأولى من الوثيقة"))
        assertTrue(exportText.contains("الصفحة الثانية ومحتواها بعد المراجعة"))
        assertTrue(exportText.contains("Raqeem"))
    }
}

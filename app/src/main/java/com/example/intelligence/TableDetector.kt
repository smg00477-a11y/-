package com.example.intelligence

import com.example.data.database.entity.OcrRegionEntity
import com.example.data.database.entity.PageEntity
import com.example.data.database.entity.TableCellEntity
import com.example.data.database.entity.TableEntity
import com.example.ocr.cleaner.ArabicTextCleaner
import com.example.ocr.model.RegionType

data class DetectedTable(
    val table: TableEntity,
    val cells: List<TableCellEntity>
)

object TableDetector {

    /**
     * Inspects page OCR regions or tabular line formats to detect tables.
     */
    fun detectTables(
        documentId: Long,
        page: PageEntity,
        regions: List<OcrRegionEntity>
    ): List<DetectedTable> {
        val detected = mutableListOf<DetectedTable>()

        // 1. Check layout regions classified as TABLE
        val tableRegions = regions.filter {
            RegionType.fromName(it.regionType) == RegionType.TABLE
        }

        for (tRegion in tableRegions) {
            val text = tRegion.effectiveText
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

            val rowCount = lines.size.coerceAtLeast(1)
            var colCount = 1

            val cells = mutableListOf<TableCellEntity>()

            // Approximate column splitting using pipe (|), tab (\t), or wide spaces (3+ spaces)
            lines.forEachIndexed { rIndex, line ->
                val tokens = line.split(Regex("""[|\t]|\s{3,}""")).map { it.trim() }.filter { it.isNotBlank() }
                if (tokens.size > colCount) colCount = tokens.size

                tokens.forEachIndexed { cIndex, cellText ->
                    cells.add(
                        TableCellEntity(
                            tableId = 0, // will be assigned after table insertion
                            rowIndex = rIndex,
                            columnIndex = cIndex,
                            rowSpan = 1,
                            colSpan = 1,
                            text = ArabicTextCleaner.cleanArabicText(cellText),
                            confidence = tRegion.confidence
                        )
                    )
                }
            }

            val tableEntity = TableEntity(
                documentId = documentId,
                pageId = page.id,
                pageIndex = page.pageIndex,
                title = "جدول صفحة ${page.pageIndex + 1}",
                left = tRegion.left,
                top = tRegion.top,
                right = tRegion.right,
                bottom = tRegion.bottom,
                rowCount = rowCount,
                columnCount = colCount,
                confidence = tRegion.confidence,
                rawStructureJson = ""
            )

            detected.add(DetectedTable(tableEntity, cells))
        }

        // 2. Also check if the page raw text contains ASCII or pipe table patterns:
        if (detected.isEmpty()) {
            val lines = page.effectiveOcrText.lines().map { it.trim() }.filter { it.isNotBlank() }
            val pipeLines = lines.filter { it.contains("|") }

            if (pipeLines.size >= 3) {
                val cells = mutableListOf<TableCellEntity>()
                var colCount = 1

                pipeLines.forEachIndexed { rIndex, line ->
                    val tokens = line.split("|").map { it.trim() }.filter { it.isNotBlank() }
                    if (tokens.size > colCount) colCount = tokens.size

                    tokens.forEachIndexed { cIndex, cellText ->
                        cells.add(
                            TableCellEntity(
                                tableId = 0,
                                rowIndex = rIndex,
                                columnIndex = cIndex,
                                rowSpan = 1,
                                colSpan = 1,
                                text = ArabicTextCleaner.cleanArabicText(cellText),
                                confidence = 0.80f
                            )
                        )
                    }
                }

                val tableEntity = TableEntity(
                    documentId = documentId,
                    pageId = page.id,
                    pageIndex = page.pageIndex,
                    title = "جدول صفحة ${page.pageIndex + 1}",
                    left = 0,
                    top = 0,
                    right = 1000,
                    bottom = 1000,
                    rowCount = pipeLines.size,
                    columnCount = colCount,
                    confidence = 0.80f,
                    rawStructureJson = ""
                )

                detected.add(DetectedTable(tableEntity, cells))
            }
        }

        return detected
    }
}

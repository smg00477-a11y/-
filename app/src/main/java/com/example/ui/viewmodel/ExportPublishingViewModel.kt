package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.BookMetadataEntity
import com.example.data.database.entity.ChapterEntity
import com.example.data.database.entity.DocumentWithPages
import com.example.data.database.entity.TableEntity
import com.example.data.database.entity.TocEntryEntity
import com.example.data.repository.DocumentRepository
import com.example.ocr.export.DocxExportManager
import com.example.ocr.export.ExportFormat
import com.example.ocr.export.ExportOptions
import com.example.ocr.export.PdfExportManager
import com.example.ocr.export.PdfExportMode
import com.example.ocr.export.TxtExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ExportState {
    object Idle : ExportState
    data class Generating(val currentStep: String, val progress: Pair<Int, Int>? = null) : ExportState
    data class Success(val file: File, val shareableUri: Uri, val format: ExportFormat) : ExportState
    data class Error(val message: String) : ExportState
}

class ExportPublishingViewModel(
    private val documentRepository: DocumentRepository,
    val documentId: Long
) : ViewModel() {

    private val _documentWithPages = MutableStateFlow<DocumentWithPages?>(null)
    val documentWithPages: StateFlow<DocumentWithPages?> = _documentWithPages.asStateFlow()

    private val _metadata = MutableStateFlow<BookMetadataEntity?>(null)
    val metadata: StateFlow<BookMetadataEntity?> = _metadata.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val chapters: StateFlow<List<ChapterEntity>> = _chapters.asStateFlow()

    private val _tocList = MutableStateFlow<List<TocEntryEntity>>(emptyList())
    val tocList: StateFlow<List<TocEntryEntity>> = _tocList.asStateFlow()

    private val _tables = MutableStateFlow<List<TableEntity>>(emptyList())
    val tables: StateFlow<List<TableEntity>> = _tables.asStateFlow()

    private val _options = MutableStateFlow(ExportOptions())
    val options: StateFlow<ExportOptions> = _options.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val doc = documentRepository.getDocumentByIdDirect(documentId)
            _documentWithPages.value = doc

            launch {
                documentRepository.getMetadataForDocument(documentId).collect {
                    _metadata.value = it
                }
            }

            launch {
                documentRepository.getChaptersForDocument(documentId).collect {
                    _chapters.value = it
                }
            }

            launch {
                documentRepository.getTocForDocument(documentId).collect {
                    _tocList.value = it
                }
            }

            launch {
                documentRepository.getTablesForDocument(documentId).collect {
                    _tables.value = it
                }
            }
        }
    }

    fun updateFormat(format: ExportFormat) {
        _options.value = _options.value.copy(format = format)
    }

    fun updatePdfMode(pdfMode: PdfExportMode) {
        _options.value = _options.value.copy(pdfMode = pdfMode)
    }

    fun updateOption(
        includeCoverPage: Boolean? = null,
        includeMetadataPage: Boolean? = null,
        includeTableOfContents: Boolean? = null,
        includeTables: Boolean? = null,
        includeRightsPage: Boolean? = null,
        cleanLineBreaks: Boolean? = null,
        fixRepeatedSpaces: Boolean? = null,
        includePageMarkers: Boolean? = null
    ) {
        _options.value = _options.value.copy(
            includeCoverPage = includeCoverPage ?: _options.value.includeCoverPage,
            includeMetadataPage = includeMetadataPage ?: _options.value.includeMetadataPage,
            includeTableOfContents = includeTableOfContents ?: _options.value.includeTableOfContents,
            includeTables = includeTables ?: _options.value.includeTables,
            includeRightsPage = includeRightsPage ?: _options.value.includeRightsPage,
            cleanLineBreaks = cleanLineBreaks ?: _options.value.cleanLineBreaks,
            fixRepeatedSpaces = fixRepeatedSpaces ?: _options.value.fixRepeatedSpaces,
            includePageMarkers = includePageMarkers ?: _options.value.includePageMarkers
        )
    }

    fun executeExport(context: Context) {
        val doc = _documentWithPages.value ?: run {
            _exportState.value = ExportState.Error("لم يتم العثور على المستند المراد تصديره")
            return
        }

        val opt = _options.value
        _exportState.value = ExportState.Generating("جاري إعداد وهيكلة بيانات الكتاب...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (opt.format) {
                    ExportFormat.PDF -> {
                        val pdfFile = PdfExportManager.generateDocumentPdfWithOptions(
                            context = context,
                            documentWithPages = doc,
                            metadata = _metadata.value,
                            chapters = _chapters.value,
                            tocList = _tocList.value,
                            tables = _tables.value,
                            options = opt,
                            onProgress = { cur, total ->
                                _exportState.value = ExportState.Generating("جاري بناء صفحات PDF رقمية ($cur من $total)...", Pair(cur, total))
                            }
                        )
                        val uri = PdfExportManager.getShareableUri(context, pdfFile)
                        _exportState.value = ExportState.Success(pdfFile, uri, ExportFormat.PDF)
                    }

                    ExportFormat.DOCX -> {
                        _exportState.value = ExportState.Generating("جاري صياغة وبناء ملف وورد MS Word (DOCX)...")
                        val docxFile = DocxExportManager.exportDocumentToDocx(
                            context = context,
                            documentWithPages = doc,
                            metadata = _metadata.value,
                            chapters = _chapters.value,
                            tocList = _tocList.value,
                            tables = _tables.value,
                            options = opt,
                            onProgress = { cur, total ->
                                _exportState.value = ExportState.Generating("جاري تنسيق فصول ونصوص الوورد ($cur من $total)...", Pair(cur, total))
                            }
                        )
                        val uri = DocxExportManager.getShareableUri(context, docxFile)
                        _exportState.value = ExportState.Success(docxFile, uri, ExportFormat.DOCX)
                    }

                    ExportFormat.TXT -> {
                        _exportState.value = ExportState.Generating("جاري تنظيف وتشكيل الملف النصي UTF-8...")
                        val txtFile = TxtExportManager.exportDocumentToTxtWithOptions(
                            context = context,
                            documentWithPages = doc,
                            metadata = _metadata.value,
                            chapters = _chapters.value,
                            tocList = _tocList.value,
                            tables = _tables.value,
                            options = opt
                        )
                        val uri = TxtExportManager.getShareableUri(context, txtFile)
                        _exportState.value = ExportState.Success(txtFile, uri, ExportFormat.TXT)
                    }
                }
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "حدث خطأ أثناء التصدير والنشر")
            }
        }
    }

    fun resetState() {
        _exportState.value = ExportState.Idle
    }

    class Factory(
        private val documentId: Long,
        private val repository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExportPublishingViewModel(repository, documentId) as T
        }
    }
}

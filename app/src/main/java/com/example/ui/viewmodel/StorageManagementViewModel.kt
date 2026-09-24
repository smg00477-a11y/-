package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.storage.archive.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class StorageManagementViewModel(
    private val context: Context,
    private val optimizationManager: StorageOptimizationManager,
    private val healthChecker: ArchiveHealthChecker,
    private val archiveManager: RaqeemArchiveManager,
    private val transferManager: LocalDeviceTransferManager
) : ViewModel() {

    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()

    private val _healthReport = MutableStateFlow<ArchiveHealthReport?>(null)
    val healthReport: StateFlow<ArchiveHealthReport?> = _healthReport.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage.asStateFlow()

    private val _progressFraction = MutableStateFlow(0f)
    val progressFraction: StateFlow<Float> = _progressFraction.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _senderSession = MutableStateFlow<SenderSession?>(null)
    val senderSession: StateFlow<SenderSession?> = _senderSession.asStateFlow()

    init {
        refreshStorageStats()
        runHealthCheck()
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = optimizationManager.getStorageStats()
        }
    }

    fun runHealthCheck() {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "جاري إجراء فحص صحة الأرشيف والملفات..."
            _healthReport.value = healthChecker.runHealthCheck { frac, msg ->
                _progressFraction.value = frac
                _progressMessage.value = msg
            }
            _isLoading.value = false
        }
    }

    fun repairArchiveIssues() {
        viewModelScope.launch {
            val currentReport = _healthReport.value ?: return@launch
            _isLoading.value = true
            _progressMessage.value = "جاري إصلاح مشكلات الأرشيف المكتشفة..."
            val repaired = healthChecker.repairArchiveIssues(currentReport)
            _userMessage.value = "تمت معالجة وإصلاح $repaired من العناصر التالفة تلقائياً."
            runHealthCheck()
            refreshStorageStats()
            _isLoading.value = false
        }
    }

    fun compressLibrary(strategy: CompressionStrategy) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = optimizationManager.compressAllDocuments(strategy) { frac, msg ->
                _progressFraction.value = frac
                _progressMessage.value = msg
            }
            val savedMb = String.format("%.2f", res.savedBytes / (1024.0 * 1024.0))
            _userMessage.value = "تم اكتمل ضغط المكتبة بنجاح! تم توفير $savedMb ميجابايت من المساحة."
            refreshStorageStats()
            _isLoading.value = false
        }
    }

    fun clearCacheAndTemp() {
        viewModelScope.launch {
            _isLoading.value = true
            val freed = optimizationManager.clearTempAndCache()
            val freedMb = String.format("%.2f", freed / (1024.0 * 1024.0))
            _userMessage.value = "تم تنظيف الملفات المؤقتة والمؤقتية: تم تحرير $freedMb ميجابايت."
            refreshStorageStats()
            _isLoading.value = false
        }
    }

    fun cleanOrphanedFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            val count = optimizationManager.cleanOrphanedFiles()
            _userMessage.value = "تم حذف $count ملفاً يتيماً غير مرتبط بالكتب."
            refreshStorageStats()
            _isLoading.value = false
        }
    }

    fun exportLibraryToUri(targetUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val tempExport = File(context.cacheDir, "export_${System.currentTimeMillis()}.rqm")
            val resFile = archiveManager.exportLibraryArchive(null, tempExport) { frac, msg ->
                _progressFraction.value = frac
                _progressMessage.value = msg
            }

            if (resFile != null && resFile.exists()) {
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                        tempExport.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    _userMessage.value = "تم تصدير أرشيف رقيم (.rqm) بنجاح إلى المجلد المحدد!"
                } catch (e: Exception) {
                    _userMessage.value = "فشل حفظ ملف الأرشيف: ${e.localizedMessage}"
                }
                tempExport.delete()
            } else {
                _userMessage.value = "فشلت عملية تصدير الأرشيف."
            }
            _isLoading.value = false
        }
    }

    fun importArchiveFromUri(sourceUri: Uri, duplicateStrategy: DuplicateStrategy) {
        viewModelScope.launch {
            _isLoading.value = true
            val tempImport = File(context.cacheDir, "import_${System.currentTimeMillis()}.rqm")

            try {
                context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                    FileOutputStream(tempImport).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                val res = archiveManager.importArchive(tempImport, duplicateStrategy) { frac, msg ->
                    _progressFraction.value = frac
                    _progressMessage.value = msg
                }

                if (res.isSuccess) {
                    _userMessage.value = "تم استيراد ${res.importedCount} كتاباً بنجاح! (تم تجاوز: ${res.skippedCount}، تم استبدال: ${res.overwrittenCount})"
                } else {
                    _userMessage.value = res.errorMessage ?: "فشلت عملية استيراد الأرشيف"
                }

            } catch (e: Exception) {
                _userMessage.value = "خطأ في قراءة حزمة الأرشيف: ${e.localizedMessage}"
            } finally {
                tempImport.delete()
                refreshStorageStats()
                runHealthCheck()
                _isLoading.value = false
            }
        }
    }

    fun startSenderSession() {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "جاري تحضير حزمة النقل وبدء الخادم المحلي..."
            val session = transferManager.startSenderSession { frac, msg ->
                _progressFraction.value = frac
                _progressMessage.value = msg
            }
            _senderSession.value = session
            _isLoading.value = false
            if (session == null) {
                _userMessage.value = "فشل بدء جلسة النقل المحلية."
            }
        }
    }

    fun stopSenderSession() {
        transferManager.stopSenderSession()
        _senderSession.value = null
    }

    fun receiveFromSender(senderIp: String, pin: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = transferManager.receiveFromDevice(senderIp, 8888, pin) { frac, msg ->
                _progressFraction.value = frac
                _progressMessage.value = msg
            }

            if (res.isSuccess) {
                _userMessage.value = "اكتمل النقل المباشر عبر الشبكة المحلية! تم استيراد ${res.importedCount} كتاب."
                refreshStorageStats()
                runHealthCheck()
            } else {
                _userMessage.value = res.errorMessage ?: "فشلت عملية النقل المباشر."
            }
            _isLoading.value = false
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    class Factory(
        private val context: Context,
        private val optimizationManager: StorageOptimizationManager,
        private val healthChecker: ArchiveHealthChecker,
        private val archiveManager: RaqeemArchiveManager,
        private val transferManager: LocalDeviceTransferManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StorageManagementViewModel(
                context,
                optimizationManager,
                healthChecker,
                archiveManager,
                transferManager
            ) as T
        }
    }
}

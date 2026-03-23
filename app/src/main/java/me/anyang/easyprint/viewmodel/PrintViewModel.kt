package me.anyang.easyprint.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintJob
import android.print.PrintManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.anyang.easyprint.data.*
import me.anyang.easyprint.print.NetworkPrinterScanner
import me.anyang.easyprint.print.PdfGenerator

class PrintViewModel(application: Application) : AndroidViewModel(application) {

    private val printerDataStore = PrinterDataStore(application)

    private val _selectedFile = MutableStateFlow<PrintFile?>(null)
    val selectedFile: StateFlow<PrintFile?> = _selectedFile.asStateFlow()

    private val _printSettings = MutableStateFlow(PrintSettings())
    val printSettings: StateFlow<PrintSettings> = _printSettings.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    private val _printJobs = MutableStateFlow<List<PrintJobInfo>>(emptyList())
    val printJobs: StateFlow<List<PrintJobInfo>> = _printJobs.asStateFlow()

    private val _availablePrinters = MutableStateFlow<List<PrinterInfo>>(emptyList())
    val availablePrinters: StateFlow<List<PrinterInfo>> = _availablePrinters.asStateFlow()

    private val _selectedPrinter = MutableStateFlow<PrinterInfo?>(null)
    val selectedPrinter: StateFlow<PrinterInfo?> = _selectedPrinter.asStateFlow()

    private val _showPageSelection = MutableStateFlow(false)
    val showPageSelection: StateFlow<Boolean> = _showPageSelection.asStateFlow()

    private val _customPages = MutableStateFlow("")
    val customPages: StateFlow<String> = _customPages.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow("")
    val scanMessage: StateFlow<String> = _scanMessage.asStateFlow()

    init {
        // 加载保存的打印机
        viewModelScope.launch {
            printerDataStore.savedPrinters.collect { savedPrinters ->
                _availablePrinters.value = savedPrinters.map { sp ->
                    PrinterInfo(
                        id = sp.id,
                        name = sp.name,
                        description = sp.type,
                        isAvailable = true
                    )
                }
            }
        }
    }

    fun selectFile(uri: Uri, name: String, size: Long, mimeType: String?, pageCount: Int = 1) {
        val fileType = when {
            mimeType == "application/pdf" ||
            name.endsWith(".pdf", ignoreCase = true) -> FileType.PDF
            mimeType?.startsWith("image/") == true ||
            name.endsWith(".jpg", ignoreCase = true) ||
            name.endsWith(".jpeg", ignoreCase = true) ||
            name.endsWith(".png", ignoreCase = true) ||
            name.endsWith(".gif", ignoreCase = true) ||
            name.endsWith(".webp", ignoreCase = true) ||
            name.endsWith(".bmp", ignoreCase = true) ||
            name.endsWith(".tiff", ignoreCase = true) -> FileType.IMAGE
            mimeType?.startsWith("text/") == true ||
            name.endsWith(".txt", ignoreCase = true) ||
            name.endsWith(".csv", ignoreCase = true) ||
            name.endsWith(".json", ignoreCase = true) ||
            name.endsWith(".xml", ignoreCase = true) ||
            name.endsWith(".md", ignoreCase = true) ||
            name.endsWith(".log", ignoreCase = true) ||
            name.endsWith(".java", ignoreCase = true) ||
            name.endsWith(".kt", ignoreCase = true) ||
            name.endsWith(".py", ignoreCase = true) ||
            name.endsWith(".js", ignoreCase = true) ||
            name.endsWith(".html", ignoreCase = true) ||
            name.endsWith(".css", ignoreCase = true) -> FileType.TEXT
            else -> FileType.UNKNOWN
        }

        _selectedFile.value = PrintFile(
            uri = uri.toString(),
            name = name,
            type = fileType,
            size = size,
            pageCount = pageCount
        )
    }

    fun clearSelectedFile() {
        _selectedFile.value = null
    }

    fun updateScale(scale: Float) {
        _printSettings.value = _printSettings.value.copy(scale = scale)
    }

    fun toggleOrientation() {
        _printSettings.value = _printSettings.value.copy(isLandscape = !_printSettings.value.isLandscape)
    }

    fun setOrientation(isLandscape: Boolean) {
        _printSettings.value = _printSettings.value.copy(isLandscape = isLandscape)
    }

    fun setPageRange(pageRange: PageRange) {
        _printSettings.value = _printSettings.value.copy(pageRange = pageRange)
    }

    fun setCustomPages(pages: String) {
        _customPages.value = pages
        val pageList = parsePageRange(pages)
        if (pageList.isNotEmpty()) {
            _printSettings.value = _printSettings.value.copy(
                pageRange = PageRange.Custom(pageList, pageList.toSet())
            )
        }
    }

    private fun parsePageRange(range: String): List<Int> {
        val result = mutableListOf<Int>()
        val parts = range.split(",", "，")

        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val rangeParts = trimmed.split("-")
                if (rangeParts.size == 2) {
                    val start = rangeParts[0].trim().toIntOrNull()
                    val end = rangeParts[1].trim().toIntOrNull()
                    if (start != null && end != null && start <= end) {
                        for (i in start..end) {
                            result.add(i)
                        }
                    }
                }
            } else {
                trimmed.toIntOrNull()?.let { result.add(it) }
            }
        }

        return result.distinct().sorted()
    }

    fun setPaperSize(paperSize: PaperSize) {
        _printSettings.value = _printSettings.value.copy(paperSize = paperSize)
    }

    fun setCopies(copies: Int) {
        _printSettings.value = _printSettings.value.copy(copies = maxOf(1, copies))
    }

    fun setColorMode(colorMode: ColorMode) {
        _printSettings.value = _printSettings.value.copy(colorMode = colorMode)
    }

    // 获取选中的页面列表
    fun getSelectedPages(totalPages: Int): List<Int> {
        return when (val range = _printSettings.value.pageRange) {
            is PageRange.All -> (1..totalPages).toList()
            is PageRange.Odd -> (1..totalPages).filter { it % 2 == 1 }
            is PageRange.Even -> (1..totalPages).filter { it % 2 == 0 }
            is PageRange.Custom -> range.selectedPages.sorted()
        }
    }

    // 生成打印用的PDF
    suspend fun generatePrintPdf(file: PrintFile): java.io.File? {
        val pdfGenerator = PdfGenerator(getApplication())
        val selectedPages = getSelectedPages(file.pageCount)
        return pdfGenerator.generatePrintPdf(
            Uri.parse(file.uri),
            file.type,
            _printSettings.value,
            selectedPages
        )
    }

    fun setDuplexMode(duplexMode: DuplexMode) {
        _printSettings.value = _printSettings.value.copy(duplexMode = duplexMode)
    }

    fun setHorizontalAlignment(alignment: HorizontalAlignment) {
        _printSettings.value = _printSettings.value.copy(horizontalAlignment = alignment)
    }

    fun setVerticalAlignment(alignment: VerticalAlignment) {
        _printSettings.value = _printSettings.value.copy(verticalAlignment = alignment)
    }

    fun setIsPrinting(isPrinting: Boolean) {
        _isPrinting.value = isPrinting
    }

    fun togglePageSelection() {
        _showPageSelection.value = !_showPageSelection.value
    }

    fun scanPrinters() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanMessage.value = "正在扫描网络打印机..."

            try {
                val scanner = NetworkPrinterScanner(getApplication())
                val discoveredPrinters = scanner.scanNetwork()

                if (discoveredPrinters.isEmpty()) {
                    _scanMessage.value = "未发现新的网络打印机"
                } else {
                    // 保存扫描到的打印机
                    discoveredPrinters.forEach { dp ->
                        val savedPrinter = SavedPrinter(
                            id = "${dp.ip}:${dp.port}",
                            name = dp.name,
                            ip = dp.ip,
                            port = dp.port,
                            type = dp.type
                        )
                        printerDataStore.savePrinter(savedPrinter)
                    }
                    _scanMessage.value = "发现 ${discoveredPrinters.size} 台新打印机"
                }
            } catch (e: Exception) {
                _scanMessage.value = "扫描失败: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun selectPrinter(printer: PrinterInfo?) {
        _selectedPrinter.value = printer
    }

    fun addPrinterByIp(ipAddress: String) {
        viewModelScope.launch {
            _scanMessage.value = "正在连接 $ipAddress..."

            try {
                val scanner = NetworkPrinterScanner(getApplication())
                val printer = scanner.checkPrinterAtIp(ipAddress)

                if (printer != null) {
                    val savedPrinter = SavedPrinter(
                        id = "${printer.ip}:${printer.port}",
                        name = printer.name,
                        ip = printer.ip,
                        port = printer.port,
                        type = printer.type
                    )
                    printerDataStore.savePrinter(savedPrinter)
                    _scanMessage.value = "已添加打印机: ${printer.name}"
                } else {
                    _scanMessage.value = "无法连接到 $ipAddress，请检查 IP 地址和打印机状态"
                }
            } catch (e: Exception) {
                _scanMessage.value = "连接失败: ${e.message}"
            }
        }
    }

    fun deletePrinter(printerId: String) {
        viewModelScope.launch {
            printerDataStore.deletePrinter(printerId)
            if (_selectedPrinter.value?.id == printerId) {
                _selectedPrinter.value = null
            }
        }
    }

    fun updatePrinterIp(printerId: String, newIp: String) {
        viewModelScope.launch {
            // 先删除旧的
            printerDataStore.deletePrinter(printerId)
            if (_selectedPrinter.value?.id == printerId) {
                _selectedPrinter.value = null
            }
            // 然后尝试连接新的IP
            addPrinterByIp(newIp)
        }
    }

    fun print(documentAdapter: PrintDocumentAdapter, jobName: String) {
        viewModelScope.launch {
            _isPrinting.value = true
            try {
                val printManager = getApplication<Application>().getSystemService(PrintManager::class.java)
                val attributes = buildPrintAttributes()
                printManager?.print(jobName, documentAdapter, attributes)
            } finally {
                _isPrinting.value = false
            }
        }
    }

    private fun buildPrintAttributes(): PrintAttributes {
        val settings = _printSettings.value
        val mediaSize = when (settings.paperSize) {
            PaperSize.A3 -> PrintAttributes.MediaSize.ISO_A3
            PaperSize.A4 -> PrintAttributes.MediaSize.ISO_A4
            PaperSize.A5 -> PrintAttributes.MediaSize.ISO_A5
            PaperSize.A6 -> PrintAttributes.MediaSize.ISO_A6
            PaperSize.Letter -> PrintAttributes.MediaSize.NA_LETTER
            PaperSize.Legal -> PrintAttributes.MediaSize.NA_LEGAL
            PaperSize.Tabloid -> PrintAttributes.MediaSize.NA_LEDGER
            PaperSize.B4 -> PrintAttributes.MediaSize.ISO_B4
            PaperSize.B5 -> PrintAttributes.MediaSize.ISO_B5
        }

        val finalMediaSize = if (settings.isLandscape) {
            mediaSize.asLandscape()
        } else {
            mediaSize.asPortrait()
        }

        val duplexMode = when (settings.duplexMode) {
            DuplexMode.Simplex -> PrintAttributes.DUPLEX_MODE_NONE
            DuplexMode.LongEdge -> PrintAttributes.DUPLEX_MODE_LONG_EDGE
            DuplexMode.ShortEdge -> PrintAttributes.DUPLEX_MODE_SHORT_EDGE
        }

        return PrintAttributes.Builder()
            .setMediaSize(finalMediaSize)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setDuplexMode(duplexMode)
            .build()
    }

    fun getPageNumbersToPrint(totalPages: Int): List<Int> {
        return when (val range = _printSettings.value.pageRange) {
            is PageRange.All -> (1..totalPages).toList()
            is PageRange.Odd -> (1..totalPages).filter { it % 2 == 1 }
            is PageRange.Even -> (1..totalPages).filter { it % 2 == 0 }
            is PageRange.Custom -> range.selectedPages.filter { it in 1..totalPages }.sorted()
        }
    }
}

data class PrinterInfo(
    val id: String,
    val name: String,
    val description: String,
    val isAvailable: Boolean
)

data class PrintJobInfo(
    val id: String,
    val documentName: String,
    val state: PrintJobState,
    val creationTime: Long
)

enum class PrintJobState {
    QUEUED, STARTED, BLOCKED, COMPLETED, FAILED, CANCELLED
}
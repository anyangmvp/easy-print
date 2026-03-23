package me.anyang.easyprint.print

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.pdf.PrintedPdfDocument
import kotlinx.coroutines.flow.StateFlow
import me.anyang.easyprint.data.PrintSettings
import me.anyang.easyprint.viewmodel.PrintViewModel
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PdfPrintDocumentAdapter(
    private val context: Context,
    private val uri: Uri,
    private val settings: PrintSettings,
    private val viewModel: PrintViewModel
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null
    private var pageCount: Int = 0

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        pdfDocument = PrintedPdfDocument(context, newAttributes)

        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val pdfRenderer = PdfRenderer(pfd)
                pageCount = pdfRenderer.pageCount
                pdfRenderer.close()
            }

            val pagesToPrint = viewModel.getPageNumbersToPrint(pageCount)
            val info = PrintDocumentInfo.Builder("document.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pagesToPrint.size)
                .build()

            val changed = oldAttributes != newAttributes
            callback.onLayoutFinished(info, changed)
        } catch (e: Exception) {
            callback.onLayoutFailed(e.message)
        }
    }

    override fun onWrite(
        pages: Array<PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { sourcePfd ->
                val pdfRenderer = PdfRenderer(sourcePfd)
                val pagesToPrint = viewModel.getPageNumbersToPrint(pdfRenderer.pageCount)

                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    pdfRenderer.close()
                    return
                }

                // For PDF, we'll copy the original with page selection
                // In a real implementation, you'd render specific pages
                FileInputStream(sourcePfd.fileDescriptor).use { input ->
                    FileOutputStream(destination?.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }

                pdfRenderer.close()
            }

            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback?.onWriteFailed(e.message)
        }
    }

    override fun onFinish() {
        pdfDocument?.close()
        pdfDocument = null
    }
}

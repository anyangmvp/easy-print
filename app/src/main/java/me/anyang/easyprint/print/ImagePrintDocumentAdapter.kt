package me.anyang.easyprint.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.pdf.PrintedPdfDocument
import me.anyang.easyprint.data.PrintSettings
import java.io.FileOutputStream
import java.io.IOException

class ImagePrintDocumentAdapter(
    private val context: Context,
    private val uri: Uri,
    private val settings: PrintSettings
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null
    private var bitmap: Bitmap? = null

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
            // Load bitmap to get dimensions
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                
                // Calculate sample size to avoid OOM
                options.inSampleSize = calculateInSampleSize(options, 2048, 2048)
                options.inJustDecodeBounds = false
                
                context.contentResolver.openInputStream(uri)?.use { stream2 ->
                    bitmap = BitmapFactory.decodeStream(stream2, null, options)
                }
            }

            val info = PrintDocumentInfo.Builder("image.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                .setPageCount(1)
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
        val currentBitmap = bitmap
        if (currentBitmap == null || currentBitmap.isRecycled) {
            callback?.onWriteFailed("Bitmap is null or recycled")
            return
        }

        if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            return
        }

        try {
            val page = pdfDocument?.startPage(0) ?: run {
                callback?.onWriteFailed("Failed to start page")
                return
            }

            val canvas = page.canvas
            val pageWidth = canvas.width.toFloat()
            val pageHeight = canvas.height.toFloat()

            // Calculate scaled dimensions with centering
            val (destRect, matrix) = calculateScaledRect(
                currentBitmap.width.toFloat(),
                currentBitmap.height.toFloat(),
                pageWidth,
                pageHeight,
                settings.scale
            )

            // Draw the bitmap
            canvas.save()
            canvas.concat(matrix)
            canvas.drawBitmap(currentBitmap, null, destRect, null)
            canvas.restore()

            pdfDocument?.finishPage(page)

            FileOutputStream(destination?.fileDescriptor).use { output ->
                pdfDocument?.writeTo(output)
            }

            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback?.onWriteFailed(e.message)
        } finally {
            pdfDocument?.close()
            pdfDocument = null
        }
    }

    override fun onFinish() {
        bitmap?.recycle()
        bitmap = null
        pdfDocument?.close()
        pdfDocument = null
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun calculateScaledRect(
        imageWidth: Float,
        imageHeight: Float,
        pageWidth: Float,
        pageHeight: Float,
        scale: Float
    ): Pair<RectF, Matrix> {
        val imageAspect = imageWidth / imageHeight
        val pageAspect = pageWidth / pageHeight

        val baseWidth: Float
        val baseHeight: Float

        if (imageAspect > pageAspect) {
            // Image is wider than page
            baseWidth = pageWidth
            baseHeight = pageWidth / imageAspect
        } else {
            // Image is taller than page
            baseHeight = pageHeight
            baseWidth = pageHeight * imageAspect
        }

        // Apply user scale
        val finalWidth = baseWidth * scale
        val finalHeight = baseHeight * scale

        // Center on page
        val left = (pageWidth - finalWidth) / 2
        val top = (pageHeight - finalHeight) / 2

        val destRect = RectF(left, top, left + finalWidth, top + finalHeight)
        val matrix = Matrix()

        return Pair(destRect, matrix)
    }
}

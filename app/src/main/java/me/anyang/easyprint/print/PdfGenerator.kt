package me.anyang.easyprint.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.anyang.easyprint.data.*
import java.io.File
import java.io.FileOutputStream

class PdfGenerator(private val context: Context) {

    companion object {
        const val TEMP_PDF_NAME = "temp.pdf"
    }

    suspend fun generatePrintPdf(
        sourceUri: Uri,
        fileType: FileType,
        settings: PrintSettings,
        selectedPages: List<Int>
    ): File? = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                TEMP_PDF_NAME
            )

            when (fileType) {
                FileType.PDF -> {
                    processPdf(sourceUri, settings, selectedPages, outputFile)
                }
                FileType.IMAGE -> {
                    processImage(sourceUri, settings, outputFile)
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun processPdf(
        sourceUri: Uri,
        settings: PrintSettings,
        selectedPages: List<Int>,
        outputFile: File
    ): File? {
        context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
            val pdfRenderer = PdfRenderer(pfd)
            val pdfDocument = PdfDocument()

            // 获取纸张尺寸（毫米转点，1英寸=72点，1英寸=25.4毫米）
            val paperWidthPts = if (settings.isLandscape) {
                settings.paperSize.heightMm / 25.4f * 72
            } else {
                settings.paperSize.widthMm / 25.4f * 72
            }
            val paperHeightPts = if (settings.isLandscape) {
                settings.paperSize.widthMm / 25.4f * 72
            } else {
                settings.paperSize.heightMm / 25.4f * 72
            }

            // 处理选中的页面
            val pagesToProcess = if (selectedPages.isEmpty()) {
                (0 until pdfRenderer.pageCount).toList()
            } else {
                selectedPages.map { it - 1 }.filter { it >= 0 && it < pdfRenderer.pageCount }
            }

            pagesToProcess.forEachIndexed { index, pageIndex ->
                val page = pdfRenderer.openPage(pageIndex)

                // 计算高质量渲染的缩放因子（目标是 600 DPI 用于高质量打印）
                val targetDpi = 600f
                val sourceDpi = 72f
                val renderScale = targetDpi / sourceDpi

                // 使用更高分辨率渲染 PDF 页面
                val renderWidth = (page.width * renderScale).toInt().coerceAtLeast(1)
                val renderHeight = (page.height * renderScale).toInt().coerceAtLeast(1)

                // 渲染到高质量位图
                val bitmap = Bitmap.createBitmap(
                    renderWidth,
                    renderHeight,
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                // 应用颜色模式
                val processedBitmap = applyColorMode(bitmap, settings.colorMode)

                // 计算绘制位置和大小
                // 注意：使用 600 DPI 的尺寸，但纸张是 72 DPI，所以需要转换比例
                val drawInfo = calculateDrawInfoForHighRes(
                    renderWidth.toFloat(),
                    renderHeight.toFloat(),
                    page.width.toFloat(),
                    page.height.toFloat(),
                    paperWidthPts,
                    paperHeightPts,
                    settings
                )

                val pageInfo = PdfDocument.PageInfo.Builder(
                    paperWidthPts.toInt(),
                    paperHeightPts.toInt(),
                    index + 1
                ).create()

                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas

                // 绘制到PDF页面
                val paint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                }
                canvas.drawBitmap(
                    processedBitmap,
                    null,
                    android.graphics.RectF(
                        drawInfo.x,
                        drawInfo.y,
                        drawInfo.x + drawInfo.width,
                        drawInfo.y + drawInfo.height
                    ),
                    paint
                )

                // 清理
                if (processedBitmap != bitmap) {
                    processedBitmap.recycle()
                }
                bitmap.recycle()
                page.close()

                pdfDocument.finishPage(pdfPage)
            }

            pdfRenderer.close()

            // 写入文件
            FileOutputStream(outputFile).use { output ->
                pdfDocument.writeTo(output)
            }
            pdfDocument.close()

            return outputFile
        }
        return null
    }

    private fun processImage(
        sourceUri: Uri,
        settings: PrintSettings,
        outputFile: File
    ): File? {
        // 加载图片
        val bitmap = loadImageBitmap(sourceUri) ?: return null

        val pdfDocument = PdfDocument()

        // 获取纸张尺寸
        val paperWidthPts = if (settings.isLandscape) {
            settings.paperSize.heightMm / 25.4f * 72
        } else {
            settings.paperSize.widthMm / 25.4f * 72
        }
        val paperHeightPts = if (settings.isLandscape) {
            settings.paperSize.widthMm / 25.4f * 72
        } else {
            settings.paperSize.heightMm / 25.4f * 72
        }

        val pageInfo = PdfDocument.PageInfo.Builder(
            paperWidthPts.toInt(),
            paperHeightPts.toInt(),
            1
        ).create()

        val pdfPage = pdfDocument.startPage(pageInfo)
        val canvas = pdfPage.canvas

        // 应用颜色模式
        val processedBitmap = applyColorMode(bitmap, settings.colorMode)

        // 计算绘制位置和大小
        val drawInfo = calculateDrawInfo(
            processedBitmap.width.toFloat(),
            processedBitmap.height.toFloat(),
            paperWidthPts,
            paperHeightPts,
            settings
        )

        // 绘制到PDF页面
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        canvas.drawBitmap(
            processedBitmap,
            null,
            android.graphics.RectF(
                drawInfo.x,
                drawInfo.y,
                drawInfo.x + drawInfo.width,
                drawInfo.y + drawInfo.height
            ),
            paint
        )

        // 清理
        if (processedBitmap != bitmap) {
            processedBitmap.recycle()
        }
        bitmap.recycle()

        pdfDocument.finishPage(pdfPage)

        // 写入文件
        FileOutputStream(outputFile).use { output ->
            pdfDocument.writeTo(output)
        }
        pdfDocument.close()

        return outputFile
    }

    private fun loadImageBitmap(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun applyColorMode(bitmap: Bitmap, colorMode: ColorMode): Bitmap {
        return when (colorMode) {
            ColorMode.Color -> bitmap
            ColorMode.Grayscale -> convertToGrayscale(bitmap)
            ColorMode.BlackWhite -> convertToBlackWhite(bitmap)
        }
    }

    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val grayscaleBitmap = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscaleBitmap
    }

    private fun convertToBlackWhite(bitmap: Bitmap): Bitmap {
        val bwBitmap = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bwBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply {
            // 转换为黑白
            set(floatArrayOf(
                85f, 85f, 85f, 0f, -128f * 255f,
                85f, 85f, 85f, 0f, -128f * 255f,
                85f, 85f, 85f, 0f, -128f * 255f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return bwBitmap
    }

    private data class DrawInfo(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    private fun calculateDrawInfo(
        contentWidth: Float,
        contentHeight: Float,
        paperWidth: Float,
        paperHeight: Float,
        settings: PrintSettings
    ): DrawInfo {
        val userScale = settings.scale / 100f

        // 首先根据用户缩放计算尺寸
        var scaledWidth = contentWidth * userScale
        var scaledHeight = contentHeight * userScale

        // 如果缩放后内容小于纸张，强制填满整个纸张（保持纵横比）
        if (scaledWidth < paperWidth || scaledHeight < paperHeight) {
            val scaleToFillWidth = paperWidth / scaledWidth
            val scaleToFillHeight = paperHeight / scaledHeight
            val fillScale = maxOf(scaleToFillWidth, scaleToFillHeight)
            scaledWidth *= fillScale
            scaledHeight *= fillScale
        }

        // 无论多大，都不能超过纸张边界
        val finalWidth = minOf(scaledWidth, paperWidth)
        val finalHeight = minOf(scaledHeight, paperHeight)

        // 计算水平位置（无页边距，0起点）
        val x = when (settings.horizontalAlignment) {
            HorizontalAlignment.Left -> 0f
            HorizontalAlignment.Center -> (paperWidth - finalWidth) / 2
            HorizontalAlignment.Right -> paperWidth - finalWidth
        }

        // 计算垂直位置（无页边距，0起点）
        val y = when (settings.verticalAlignment) {
            VerticalAlignment.Top -> 0f
            VerticalAlignment.Center -> (paperHeight - finalHeight) / 2
            VerticalAlignment.Bottom -> paperHeight - finalHeight
        }

        return DrawInfo(x, y, finalWidth, finalHeight)
    }

    private fun calculateDrawInfoForHighRes(
        renderWidth: Float,
        renderHeight: Float,
        originalWidth: Float,
        originalHeight: Float,
        paperWidth: Float,
        paperHeight: Float,
        settings: PrintSettings
    ): DrawInfo {
        val userScale = settings.scale / 100f

        // 应用用户缩放到原始尺寸
        val finalWidth = originalWidth * userScale
        val finalHeight = originalHeight * userScale

        // 计算绘制位置（无页边距，0起点）
        val x = when (settings.horizontalAlignment) {
            HorizontalAlignment.Left -> 0f
            HorizontalAlignment.Center -> (paperWidth - finalWidth) / 2
            HorizontalAlignment.Right -> paperWidth - finalWidth
        }

        val y = when (settings.verticalAlignment) {
            VerticalAlignment.Top -> 0f
            VerticalAlignment.Center -> (paperHeight - finalHeight) / 2
            VerticalAlignment.Bottom -> paperHeight - finalHeight
        }

        return DrawInfo(x, y, finalWidth, finalHeight)
    }
}
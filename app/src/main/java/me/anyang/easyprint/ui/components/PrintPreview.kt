package me.anyang.easyprint.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.anyang.easyprint.data.*
import java.io.IOException

@Composable
fun PrintPreview(
    uri: Uri,
    fileType: FileType,
    settings: PrintSettings,
    modifier: Modifier = Modifier,
    currentPage: Int = 0,
    onPageChange: (Int) -> Unit = {},
    totalPages: Int = 1,
    selectedPages: List<Int> = emptyList(),
    fileName: String = ""
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pageCount by remember { mutableIntStateOf(1) }

    // 使用选中的页面列表，如果没有则使用全部页面
    val effectiveSelectedPages = selectedPages.ifEmpty { (1..totalPages).toList() }
    val effectiveTotalPages = effectiveSelectedPages.size
    
    // currentPage 是选中页面列表中的索引（0, 1, 2...）
    val effectiveCurrentIndex = currentPage.coerceIn(0, (effectiveTotalPages - 1).coerceAtLeast(0))
    val effectiveCurrentPage = if (effectiveSelectedPages.isNotEmpty()) {
        effectiveSelectedPages[effectiveCurrentIndex] - 1
    } else 0

    // 用于文本文件预览
    var textContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        isLoading = true
        textContent = null
        when (fileType) {
            FileType.PDF -> {
                loadPdfPageCount(context, uri) { count ->
                    pageCount = count
                }
                loadPdfPage(context, uri, effectiveCurrentPage) { bmp ->
                    bitmap = bmp
                    isLoading = false
                }
            }
            FileType.IMAGE -> {
                loadImage(context, uri) { bmp ->
                    bitmap = bmp
                    isLoading = false
                }
            }
            FileType.TEXT -> {
                // 加载文本文件
                textContent = loadTextFile(context, uri)
                isLoading = false
            }
            else -> {
                // 其他类型文件，尝试作为文本加载，如果失败则显示提示
                textContent = loadTextFile(context, uri)
                isLoading = false
            }
        }
    }

    LaunchedEffect(uri, effectiveCurrentPage) {
        if (fileType == FileType.PDF) {
            isLoading = true
            loadPdfPage(context, uri, effectiveCurrentPage) { bmp ->
                bitmap = bmp
                isLoading = false
            }
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                bitmap != null -> {
                    PrintPreviewContent(
                        bitmap = bitmap!!,
                        settings = settings
                    )
                }
                textContent != null -> {
                    // 文本文件预览
                    TextFilePreview(
                        content = textContent!!,
                        settings = settings
                    )
                }
                else -> {
                    // 不支持的文件类型提示
                    UnsupportedFilePreview(fileType = fileType, fileName = fileName)
                }
            }
        }

        if (fileType == FileType.PDF && effectiveTotalPages > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (effectiveCurrentIndex > 0) onPageChange(effectiveCurrentIndex - 1) },
                    enabled = effectiveCurrentIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "上一页",
                        tint = if (effectiveCurrentIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${effectiveCurrentIndex + 1} / $effectiveTotalPages",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                IconButton(
                    onClick = { if (effectiveCurrentIndex < effectiveTotalPages - 1) onPageChange(effectiveCurrentIndex + 1) },
                    enabled = effectiveCurrentIndex < effectiveTotalPages - 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "下一页",
                        tint = if (effectiveCurrentIndex < effectiveTotalPages - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PrintPreviewContent(
    bitmap: Bitmap,
    settings: PrintSettings
) {
    val paperAspectRatio = remember(settings.paperSize, settings.isLandscape) {
        val width = if (settings.isLandscape) settings.paperSize.heightMm else settings.paperSize.widthMm
        val height = if (settings.isLandscape) settings.paperSize.widthMm else settings.paperSize.heightMm
        width / height
    }

    val scale = settings.scale / 100f

    val colorMatrix = remember(settings.colorMode) {
        when (settings.colorMode) {
            ColorMode.Color -> null
            ColorMode.Grayscale -> ColorMatrix().apply { setToSaturation(0f) }
            ColorMode.BlackWhite -> ColorMatrix(floatArrayOf(
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(paperAspectRatio)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White),
            contentAlignment = when {
                settings.horizontalAlignment == HorizontalAlignment.Left && settings.verticalAlignment == VerticalAlignment.Top ->
                    Alignment.TopStart
                settings.horizontalAlignment == HorizontalAlignment.Left && settings.verticalAlignment == VerticalAlignment.Center ->
                    Alignment.CenterStart
                settings.horizontalAlignment == HorizontalAlignment.Left && settings.verticalAlignment == VerticalAlignment.Bottom ->
                    Alignment.BottomStart
                settings.horizontalAlignment == HorizontalAlignment.Center && settings.verticalAlignment == VerticalAlignment.Top ->
                    Alignment.TopCenter
                settings.horizontalAlignment == HorizontalAlignment.Center && settings.verticalAlignment == VerticalAlignment.Center ->
                    Alignment.Center
                settings.horizontalAlignment == HorizontalAlignment.Center && settings.verticalAlignment == VerticalAlignment.Bottom ->
                    Alignment.BottomCenter
                settings.horizontalAlignment == HorizontalAlignment.Right && settings.verticalAlignment == VerticalAlignment.Top ->
                    Alignment.TopEnd
                settings.horizontalAlignment == HorizontalAlignment.Right && settings.verticalAlignment == VerticalAlignment.Center ->
                    Alignment.CenterEnd
                settings.horizontalAlignment == HorizontalAlignment.Right && settings.verticalAlignment == VerticalAlignment.Bottom ->
                    Alignment.BottomEnd
                else -> Alignment.Center
            }
        ) {
            if (scale <= 1f) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "打印预览",
                    modifier = Modifier.fillMaxHeight(scale),
                    contentScale = ContentScale.Fit,
                    colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(it) }
                )
            } else {
                val maxW = maxWidth
                val maxH = maxHeight
                val xOffset = when (settings.horizontalAlignment) {
                    HorizontalAlignment.Left -> maxW * (1f - 1f / scale) / 2
                    HorizontalAlignment.Center -> 0.dp
                    HorizontalAlignment.Right -> -maxW * (1f - 1f / scale) / 2
                }
                val yOffset = when (settings.verticalAlignment) {
                    VerticalAlignment.Top -> maxH * (1f - 1f / scale) / 2
                    VerticalAlignment.Center -> 0.dp
                    VerticalAlignment.Bottom -> -maxH * (1f - 1f / scale) / 2
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "打印预览",
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .offset(x = xOffset, y = yOffset),
                    contentScale = ContentScale.Fit,
                    colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(it) }
                )
            }
        }
    }
}

private fun loadPdfPageCount(
    context: android.content.Context,
    uri: Uri,
    onComplete: (Int) -> Unit
) {
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val pdfRenderer = PdfRenderer(pfd)
            onComplete(pdfRenderer.pageCount)
            pdfRenderer.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(1)
    }
}

private fun loadPdfPage(
    context: android.content.Context,
    uri: Uri,
    pageIndex: Int,
    onComplete: (Bitmap?) -> Unit
) {
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val pdfRenderer = PdfRenderer(pfd)
            if (pageIndex >= 0 && pageIndex < pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap(
                    page.width,
                    page.height,
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pdfRenderer.close()
                onComplete(bitmap)
            } else {
                pdfRenderer.close()
                onComplete(null)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
        onComplete(null)
    }
}

private fun loadImage(
    context: android.content.Context,
    uri: Uri,
    onComplete: (Bitmap?) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        onComplete(bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(null)
    }
}

private fun loadTextFile(context: android.content.Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                val content = reader.readText()
                // 限制文本长度，避免内存问题
                if (content.length > 50000) {
                    content.substring(0, 50000) + "\n\n[文件内容过长，仅显示前50000字符]"
                } else {
                    content
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
private fun TextFilePreview(
    content: String,
    settings: PrintSettings
) {
    val scrollState = rememberScrollState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun UnsupportedFilePreview(fileType: FileType, fileName: String = "") {
    // 根据文件扩展名判断类型
    val fileTypeName = when {
        fileName.endsWith(".docx", ignoreCase = true) || 
        fileName.endsWith(".doc", ignoreCase = true) -> "Word 文档"
        fileName.endsWith(".xlsx", ignoreCase = true) || 
        fileName.endsWith(".xls", ignoreCase = true) -> "Excel 表格"
        fileName.endsWith(".pptx", ignoreCase = true) || 
        fileName.endsWith(".ppt", ignoreCase = true) -> "PowerPoint 演示文稿"
        fileName.endsWith(".zip", ignoreCase = true) || 
        fileName.endsWith(".rar", ignoreCase = true) || 
        fileName.endsWith(".7z", ignoreCase = true) -> "压缩文件"
        fileName.endsWith(".apk", ignoreCase = true) -> "Android 应用"
        else -> "此文件类型"
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$fileTypeName 暂不支持预览",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击打印按钮，系统将尝试转换并打印",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
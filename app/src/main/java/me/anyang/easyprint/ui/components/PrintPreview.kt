package me.anyang.easyprint.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.anyang.easyprint.data.FileType
import me.anyang.easyprint.data.PaperSize
import me.anyang.easyprint.data.PrintSettings
import java.io.IOException

@Composable
fun PrintPreview(
    uri: Uri,
    fileType: FileType,
    settings: PrintSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        isLoading = true
        when (fileType) {
            FileType.PDF -> {
                loadPdfFirstPage(context, uri) { bmp ->
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
            else -> isLoading = false
        }
    }

    // 计算纸张比例
    val paperAspectRatio = remember(settings.paperSize, settings.isLandscape) {
        val width = if (settings.isLandscape) settings.paperSize.heightMm else settings.paperSize.widthMm
        val height = if (settings.isLandscape) settings.paperSize.widthMm else settings.paperSize.heightMm
        width / height
    }

    Box(
        modifier = modifier
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
                // 根据纸张比例显示，100%缩放时占满预览区域
                val scale = settings.scale / 100f
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.95f)
                            .aspectRatio(paperAspectRatio)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "打印预览",
                            modifier = Modifier
                                .fillMaxSize(scale.coerceIn(0.1f, 1f))
                                .padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (fileType) {
                            FileType.PDF -> Icons.Outlined.Description
                            FileType.IMAGE -> Icons.Outlined.Image
                            else -> Icons.Outlined.Description
                        },
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "无法预览",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun loadPdfFirstPage(
    context: android.content.Context,
    uri: Uri,
    onComplete: (Bitmap?) -> Unit
) {
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val pdfRenderer = PdfRenderer(pfd)
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
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
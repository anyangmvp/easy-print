package me.anyang.easyprint.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.anyang.easyprint.data.FileType
import me.anyang.easyprint.data.PrintFile
import me.anyang.easyprint.data.PrintSettings
import me.anyang.easyprint.data.PageRange
import java.io.IOException

@Composable
fun PrintPreviewCard(
    printFile: PrintFile?,
    settings: PrintSettings,
    pageRange: PageRange,
    customPages: String,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    if (printFile == null) return

    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var pageImages by remember(printFile.uri) { mutableStateOf<List<Pair<Int, Bitmap>>>(emptyList()) }
    var currentPageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(printFile.uri, isExpanded) {
        if (isExpanded && pageImages.isEmpty()) {
            loadPdfPages(context, printFile.uri, printFile.pageCount) { images ->
                pageImages = images
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = when (printFile.type) {
                            FileType.PDF -> Icons.Outlined.Description
                            FileType.IMAGE -> Icons.Outlined.Image
                            else -> Icons.Outlined.Description
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "打印预览",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = getPreviewSummary(printFile.pageCount, pageRange, customPages),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded)
                        Icons.Default.ChevronRight
                    else
                        Icons.Default.ChevronLeft,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    if (pageImages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val scrollState = rememberScrollState()
                        val pagesToShow = getPagesToShow(pageRange, customPages, totalPages)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pagesToShow.forEachIndexed { index, pageNum ->
                                if (pageNum in pageImages.indices) {
                                    val (page, bitmap) = pageImages[pageNum]
                                    PageThumbnail(
                                        bitmap = bitmap,
                                        pageNumber = page + 1,
                                        isSelected = index == currentPageIndex,
                                        onClick = { currentPageIndex = index },
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                            }
                        }

                        if (pagesToShow.size > 5) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "左右滑动查看更多页面",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageThumbnail(
    bitmap: Bitmap,
    pageNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "第 $pageNumber 页",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.707f),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "第 $pageNumber 页",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getPreviewSummary(
    totalPages: Int,
    pageRange: PageRange,
    customPages: String
): String {
    val pagesToPrint = when (pageRange) {
        is PageRange.All -> (1..totalPages).toList()
        is PageRange.Odd -> (1..totalPages).filter { it % 2 == 1 }
        is PageRange.Even -> (1..totalPages).filter { it % 2 == 0 }
        is PageRange.Custom -> parseCustomPages(customPages, totalPages)
    }
    return "将打印 ${pagesToPrint.size} / $totalPages 页"
}

private fun getPagesToShow(
    pageRange: PageRange,
    customPages: String,
    totalPages: Int
): List<Int> {
    return when (pageRange) {
        is PageRange.All -> (0 until totalPages).toList()
        is PageRange.Odd -> (0 until totalPages).filter { (it + 1) % 2 == 1 }
        is PageRange.Even -> (0 until totalPages).filter { (it + 1) % 2 == 0 }
        is PageRange.Custom -> parseCustomPages(customPages, totalPages).map { it - 1 }
    }
}

private fun parseCustomPages(customPages: String, totalPages: Int): List<Int> {
    if (customPages.isBlank()) return emptyList()
    val pages = mutableSetOf<Int>()
    try {
        customPages.split(",", "，").forEach { part ->
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val (start, end) = trimmed.split("-").map { it.trim().toInt() }
                for (i in start..minOf(end, totalPages)) {
                    if (i in 1..totalPages) pages.add(i)
                }
            } else {
                val pageNum = trimmed.toInt()
                if (pageNum in 1..totalPages) pages.add(pageNum)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return pages.sorted()
}

private fun loadPdfPages(
    context: android.content.Context,
    uri: String,
    pageCount: Int,
    onComplete: (List<Pair<Int, Bitmap>>) -> Unit
) {
    try {
        context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { pfd ->
            val pdfRenderer = PdfRenderer(pfd)
            val images = mutableListOf<Pair<Int, Bitmap>>()

            val pagesToLoad = if (pageCount > 10) {
                listOf(0, 1, 2, pageCount / 2, pageCount - 2, pageCount - 1)
            } else {
                (0 until pageCount).toList()
            }

            pagesToLoad.forEach { pageIndex ->
                if (pageIndex < pdfRenderer.pageCount) {
                    val page = pdfRenderer.openPage(pageIndex)
                    val bitmap = Bitmap.createBitmap(
                        page.width / 2,
                        page.height / 2,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    images.add(pageIndex to bitmap)
                }
            }

            pdfRenderer.close()
            onComplete(images.sortedBy { it.first })
        }
    } catch (e: IOException) {
        e.printStackTrace()
        onComplete(emptyList())
    }
}

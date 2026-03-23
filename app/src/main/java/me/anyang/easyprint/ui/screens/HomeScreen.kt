package me.anyang.easyprint.ui.screens

import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.anyang.easyprint.data.*
import me.anyang.easyprint.print.GeneratedPdfPrintDocumentAdapter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import me.anyang.easyprint.ui.components.PrintPreview
import me.anyang.easyprint.ui.components.PrintSettingsPanel
import me.anyang.easyprint.viewmodel.PrintViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import android.print.PrintManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sharedFileUri: android.net.Uri? = null,
    viewModel: PrintViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val selectedFile by viewModel.selectedFile.collectAsState()
    val printSettings by viewModel.printSettings.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val showPageSelection by viewModel.showPageSelection.collectAsState()
    val customPages by viewModel.customPages.collectAsState()
    var currentPreviewPage by remember { mutableIntStateOf(0) }
    
    // 当页码范围变化时，重置预览到第一页
    LaunchedEffect(printSettings.pageRange) {
        currentPreviewPage = 0
    }

    LaunchedEffect(sharedFileUri) {
        sharedFileUri?.let { uri ->
            loadFileFromUri(context, uri, viewModel)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            loadFileFromUri(context, it, viewModel)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Easy Print",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            FileSelectionCard(
                selectedFile = selectedFile,
                onSelectFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                onClearFile = viewModel::clearSelectedFile
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedFile != null) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column {
                        val selectedPages = viewModel.getSelectedPages(selectedFile!!.pageCount)
                        val effectiveTotalPages = selectedPages.size
                        
                        // currentPreviewPage 是选中页面列表中的索引（0, 1, 2...）
                        // 当页码选择变化时，重置预览到第一页
                        val previewIndex = currentPreviewPage.coerceIn(0, (effectiveTotalPages - 1).coerceAtLeast(0))

                        PrintPreviewCard(
                            file = selectedFile!!,
                            settings = printSettings,
                            onTogglePageSelection = viewModel::togglePageSelection,
                            currentPreviewPage = previewIndex,
                            onPreviewPageChange = { index ->
                                currentPreviewPage = index.coerceIn(0, selectedPages.size - 1)
                            },
                            selectedPages = selectedPages,
                            effectiveTotalPages = effectiveTotalPages
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        PrintSettingsPanel(
                            settings = printSettings,
                            totalPages = selectedFile!!.pageCount,
                            customPages = customPages,
                            onScaleChange = viewModel::updateScale,
                            onOrientationToggle = viewModel::toggleOrientation,
                            onPageRangeChange = viewModel::setPageRange,
                            onCustomPagesChange = viewModel::setCustomPages,
                            onPaperSizeChange = viewModel::setPaperSize,
                            onColorModeChange = viewModel::setColorMode,
                            onHorizontalAlignmentChange = viewModel::setHorizontalAlignment,
                            onVerticalAlignmentChange = viewModel::setVerticalAlignment
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        IOSPrintButton(
                            enabled = selectedFile != null && selectedFile?.type != FileType.UNKNOWN && !isPrinting,
                            isLoading = isPrinting,
                            onClick = {
                                selectedFile?.let { file ->
                                    startPrint(context, file, printSettings, viewModel)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "选择一个文件开始打印",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun FileSelectionCard(
    selectedFile: PrintFile?,
    onSelectFile: () -> Unit,
    onClearFile: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "press_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelectFile
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (selectedFile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "选择要打印的文件",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "支持 PDF、图片等文件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (selectedFile.type) {
                            FileType.PDF -> Icons.Outlined.PictureAsPdf
                            FileType.IMAGE -> Icons.Outlined.Image
                            else -> Icons.Outlined.InsertDriveFile
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedFile.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = formatFileSize(selectedFile.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClearFile) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PrintPreviewCard(
    file: PrintFile,
    settings: PrintSettings,
    onTogglePageSelection: () -> Unit,
    currentPreviewPage: Int,
    onPreviewPageChange: (Int) -> Unit,
    selectedPages: List<Int>,
    effectiveTotalPages: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "打印预览",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(onClick = onTogglePageSelection) {
                    Text(
                        text = "选择页面",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PrintPreview(
                uri = android.net.Uri.parse(file.uri),
                fileType = file.type,
                settings = settings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                currentPage = currentPreviewPage,
                onPageChange = onPreviewPageChange,
                totalPages = file.pageCount,
                selectedPages = selectedPages,
                fileName = file.name
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PreviewInfoItem(
                    icon = Icons.Outlined.Description,
                    label = "页数",
                    value = "$effectiveTotalPages"
                )
                PreviewInfoItem(
                    icon = Icons.Outlined.AspectRatio,
                    label = "方向",
                    value = if (settings.isLandscape) "横向" else "纵向"
                )
                PreviewInfoItem(
                    icon = Icons.Outlined.ZoomIn,
                    label = "缩放",
                    value = "${settings.scale.toInt()}%"
                )
            }
        }
    }
}

@Composable
private fun PreviewInfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun IOSPrintButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "press_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled && !isLoading) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Gray.copy(alpha = 0.5f),
                            Color.Gray.copy(alpha = 0.4f)
                        )
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "正在应用设置...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalPrintshop,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "开始打印",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

private fun loadFileFromUri(
    context: android.content.Context,
    uri: android.net.Uri,
    viewModel: PrintViewModel
) {
    try {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)

                val name = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L

                val mimeType = contentResolver.getType(uri)

                var pageCount = 1
                if (mimeType == "application/pdf" || name.endsWith(".pdf", ignoreCase = true)) {
                    try {
                        contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            val pdfRenderer = android.graphics.pdf.PdfRenderer(pfd)
                            pageCount = pdfRenderer.pageCount
                            pdfRenderer.close()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                viewModel.selectFile(uri, name, size, mimeType, pageCount)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "无法加载文件: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun startPrint(
    context: android.content.Context,
    file: PrintFile,
    settings: PrintSettings,
    viewModel: PrintViewModel
) {
    val printManager = context.getSystemService(PrintManager::class.java)
    val jobName = "EasyPrint_${file.name}"

    viewModel.setIsPrinting(true)

    kotlinx.coroutines.GlobalScope.launch {
        val generatedPdf = viewModel.generatePrintPdf(file)

        withContext(kotlinx.coroutines.Dispatchers.Main) {
            if (generatedPdf != null && generatedPdf.exists()) {
                val adapter = GeneratedPdfPrintDocumentAdapter(context, generatedPdf)

                val attributes = PrintAttributes.Builder()
                    .setMediaSize(
                        if (settings.isLandscape) {
                            when (settings.paperSize) {
                                PaperSize.A3 -> PrintAttributes.MediaSize.ISO_A3.asLandscape()
                                PaperSize.A4 -> PrintAttributes.MediaSize.ISO_A4.asLandscape()
                                PaperSize.A5 -> PrintAttributes.MediaSize.ISO_A5.asLandscape()
                                PaperSize.A6 -> PrintAttributes.MediaSize.ISO_A6.asLandscape()
                                PaperSize.Letter -> PrintAttributes.MediaSize.NA_LETTER.asLandscape()
                                PaperSize.Legal -> PrintAttributes.MediaSize.NA_LEGAL.asLandscape()
                                PaperSize.Tabloid -> PrintAttributes.MediaSize.NA_LEDGER.asLandscape()
                                PaperSize.B4 -> PrintAttributes.MediaSize.ISO_B4.asLandscape()
                                PaperSize.B5 -> PrintAttributes.MediaSize.ISO_B5.asLandscape()
                            }
                        } else {
                            when (settings.paperSize) {
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
                        }
                    )
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                printManager?.print(jobName, adapter, attributes)
                viewModel.setIsPrinting(false)
            } else {
                Toast.makeText(context, "生成打印文件失败", Toast.LENGTH_LONG).show()
                viewModel.setIsPrinting(false)
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
    }
}
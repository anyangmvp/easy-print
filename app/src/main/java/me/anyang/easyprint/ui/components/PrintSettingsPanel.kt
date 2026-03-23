package me.anyang.easyprint.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.anyang.easyprint.data.*

@Composable
fun PrintSettingsPanel(
    settings: PrintSettings,
    totalPages: Int,
    customPages: String,
    onScaleChange: (Float) -> Unit,
    onOrientationToggle: () -> Unit,
    onPageRangeChange: (PageRange) -> Unit,
    onCustomPagesChange: (String) -> Unit,
    onPaperSizeChange: (PaperSize) -> Unit = {},
    onColorModeChange: (ColorMode) -> Unit = {},
    onHorizontalAlignmentChange: (HorizontalAlignment) -> Unit = {},
    onVerticalAlignmentChange: (VerticalAlignment) -> Unit = {}
) {
    var showPageGrid by remember { mutableStateOf(false) }
    var selectedPages by remember { mutableStateOf<Set<Int>>((1..totalPages).toSet()) }

    // 当页面范围改变时更新选中状态
    LaunchedEffect(settings.pageRange, totalPages) {
        selectedPages = when (val range = settings.pageRange) {
            is PageRange.All -> (1..totalPages).toSet()
            is PageRange.Odd -> (1..totalPages).filter { it % 2 == 1 }.toSet()
            is PageRange.Even -> (1..totalPages).filter { it % 2 == 0 }.toSet()
            is PageRange.Custom -> range.selectedPages
        }
    }

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
            SettingsSection(title = "缩放与方向") {
                IOSSettingRow(
                    icon = Icons.Outlined.ZoomIn,
                    title = "缩放",
                    trailing = {
                        ScaleSelector(
                            scale = settings.scale,
                            onScaleChange = onScaleChange
                        )
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFFF2F2F7)
                )

                IOSSettingRow(
                    icon = Icons.Outlined.ScreenRotation,
                    title = "打印方向",
                    trailing = {
                        OrientationToggle(
                            isLandscape = settings.isLandscape,
                            onToggle = onOrientationToggle
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(title = "页面选择") {
                PageRangeSelector(
                    currentRange = settings.pageRange,
                    totalPages = totalPages,
                    selectedPages = selectedPages,
                    onRangeChange = { range ->
                        // 更新选中页面
                        val newSelectedPages = when (range) {
                            is PageRange.All -> (1..totalPages).toSet()
                            is PageRange.Odd -> (1..totalPages).filter { it % 2 == 1 }.toSet()
                            is PageRange.Even -> (1..totalPages).filter { it % 2 == 0 }.toSet()
                            is PageRange.Custom -> selectedPages
                        }
                        selectedPages = newSelectedPages
                        onPageRangeChange(range)
                        showPageGrid = true
                    },
                    onTogglePageGrid = { showPageGrid = !showPageGrid },
                    showPageGrid = showPageGrid
                )

                AnimatedVisibility(
                    visible = showPageGrid && totalPages > 0,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        PageNumberGrid(
                            totalPages = totalPages,
                            selectedPages = selectedPages,
                            onPageToggle = { page ->
                                selectedPages = if (selectedPages.contains(page)) {
                                    selectedPages - page
                                } else {
                                    selectedPages + page
                                }
                                // 更新为自定义范围
                                val sortedPages = selectedPages.sorted()
                                onPageRangeChange(PageRange.Custom(sortedPages, selectedPages))
                            }
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFFF2F2F7)
                )

                CustomPagesInputFullWidth(
                    value = customPages,
                    onValueChange = onCustomPagesChange
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(title = "纸张与颜色") {
                // 纸张类型选择
                IOSSettingRow(
                    icon = Icons.Outlined.Description,
                    title = "纸张类型",
                    trailing = {
                        PaperSizeSelector(
                            currentSize = settings.paperSize,
                            onSizeChange = onPaperSizeChange
                        )
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFFF2F2F7)
                )

                // 颜色模式选择
                IOSSettingRow(
                    icon = Icons.Outlined.Palette,
                    title = "颜色模式",
                    trailing = {
                        ColorModeSelector(
                            currentMode = settings.colorMode,
                            onModeChange = onColorModeChange
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(title = "对齐方式") {
                // 水平对齐
                IOSSettingRow(
                    icon = Icons.Outlined.FormatAlignLeft,
                    title = "水平对齐",
                    trailing = {
                        HorizontalAlignmentSelector(
                            currentAlignment = settings.horizontalAlignment,
                            onAlignmentChange = onHorizontalAlignmentChange
                        )
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFFF2F2F7)
                )

                // 垂直对齐
                IOSSettingRow(
                    icon = Icons.Outlined.VerticalAlignTop,
                    title = "垂直对齐",
                    trailing = {
                        VerticalAlignmentSelector(
                            currentAlignment = settings.verticalAlignment,
                            onAlignmentChange = onVerticalAlignmentChange
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun IOSSettingRow(
    icon: ImageVector,
    title: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
private fun ScaleSelector(
    scale: Float,
    onScaleChange: (Float) -> Unit
) {
    var inputValue by remember { mutableStateOf(scale.toInt().toString()) }

    LaunchedEffect(scale) {
        if (scale.toInt().toString() != inputValue) {
            inputValue = scale.toInt().toString()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 100% 按钮
        FilterChip(
            selected = scale == 100f,
            onClick = {
                onScaleChange(100f)
                inputValue = "100"
            },
            label = {
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = Color.White
            ),
            modifier = Modifier.height(32.dp)
        )

        // 自定义缩放输入框 - 显示纯数字，无%后缀
        BasicTextField(
            value = inputValue,
            onValueChange = { newValue: String ->
                // 允许空值，允许输入数字
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    inputValue = newValue
                    // 如果有有效数字，立即更新预览
                    val parsed = newValue.toFloatOrNull()
                    if (parsed != null && parsed in 10f..400f) {
                        onScaleChange(parsed)
                    }
                }
            },
            modifier = Modifier
                .width(56.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF9F9F9))
                .border(1.dp, Color(0xFFE5E5EA), RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun PaperSizeSelector(
    currentSize: PaperSize,
    onSizeChange: (PaperSize) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = currentSize.displayName,
                style = MaterialTheme.typography.bodyMedium
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PaperSize.entries.forEach { size ->
                DropdownMenuItem(
                    text = { Text(size.displayName) },
                    onClick = {
                        onSizeChange(size)
                        expanded = false
                    },
                    leadingIcon = if (size == currentSize) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun ColorModeSelector(
    currentMode: ColorMode,
    onModeChange: (ColorMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ColorMode.entries.forEach { mode ->
            val selected = mode == currentMode
            FilterChip(
                selected = selected,
                onClick = { onModeChange(mode) },
                label = {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

@Composable
private fun HorizontalAlignmentSelector(
    currentAlignment: HorizontalAlignment,
    onAlignmentChange: (HorizontalAlignment) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HorizontalAlignment.entries.forEach { alignment ->
            val selected = alignment == currentAlignment
            val icon = when (alignment) {
                HorizontalAlignment.Left -> Icons.Outlined.FormatAlignLeft
                HorizontalAlignment.Center -> Icons.Outlined.FormatAlignCenter
                HorizontalAlignment.Right -> Icons.Outlined.FormatAlignRight
            }
            IconButton(
                onClick = { onAlignmentChange(alignment) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else Color(0xFFF2F2F7)
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = alignment.displayName,
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun VerticalAlignmentSelector(
    currentAlignment: VerticalAlignment,
    onAlignmentChange: (VerticalAlignment) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VerticalAlignment.entries.forEach { alignment ->
            val selected = alignment == currentAlignment
            val icon = when (alignment) {
                VerticalAlignment.Top -> Icons.Outlined.VerticalAlignTop
                VerticalAlignment.Center -> Icons.Outlined.VerticalAlignCenter
                VerticalAlignment.Bottom -> Icons.Outlined.VerticalAlignBottom
            }
            IconButton(
                onClick = { onAlignmentChange(alignment) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else Color(0xFFF2F2F7)
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = alignment.displayName,
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun OrientationToggle(
    isLandscape: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF2F2F7))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OrientationButton(
            text = "纵向",
            selected = !isLandscape,
            onClick = { if (isLandscape) onToggle() }
        )
        OrientationButton(
            text = "横向",
            selected = isLandscape,
            onClick = { if (!isLandscape) onToggle() }
        )
    }
}

@Composable
private fun OrientationButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color.Transparent,
        label = "bg_color"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "text_color"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun PageRangeSelector(
    currentRange: PageRange,
    totalPages: Int,
    selectedPages: Set<Int>,
    onRangeChange: (PageRange) -> Unit,
    onTogglePageGrid: () -> Unit,
    showPageGrid: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 全部按钮 - 显示已选数量
            val allSelectedCount = selectedPages.size
            PageRangeButton(
                text = if (allSelectedCount == totalPages) "全部" else "全部($allSelectedCount)",
                selected = currentRange is PageRange.All,
                onClick = { onRangeChange(PageRange.All) },
                modifier = Modifier.weight(1f)
            )
            PageRangeButton(
                text = "奇数页",
                selected = currentRange is PageRange.Odd,
                onClick = { onRangeChange(PageRange.Odd) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PageRangeButton(
                text = "偶数页",
                selected = currentRange is PageRange.Even,
                onClick = { onRangeChange(PageRange.Even) },
                modifier = Modifier.weight(1f)
            )
            PageRangeButton(
                text = if (showPageGrid) "隐藏页码" else "选择页码",
                selected = showPageGrid,
                onClick = onTogglePageGrid,
                modifier = Modifier.weight(1f),
                isToggle = true
            )
        }
    }
}

@Composable
private fun PageRangeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isToggle: Boolean = false
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            selected && !isToggle -> MaterialTheme.colorScheme.primary
            selected && isToggle -> MaterialTheme.colorScheme.primaryContainer
            else -> Color(0xFFF2F2F7)
        },
        label = "bg"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            selected && !isToggle -> Color.White
            selected && isToggle -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "text"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun PageNumberGrid(
    totalPages: Int,
    selectedPages: Set<Int>,
    onPageToggle: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2F7))
            .padding(12.dp)
    ) {
        Text(
            text = "点击选择/取消页码 (已选 ${selectedPages.size}/$totalPages 页)",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val rows = (totalPages + 4) / 5
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until 5) {
                    val pageNumber = row * 5 + col + 1
                    if (pageNumber <= totalPages) {
                        val isSelected = selectedPages.contains(pageNumber)
                        PageNumberButton(
                            pageNumber = pageNumber,
                            isSelected = isSelected,
                            onClick = { onPageToggle(pageNumber) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (row < rows - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PageNumberButton(
    pageNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
        label = "bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE5E5EA),
        label = "border"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = pageNumber.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CustomPagesInputFullWidth(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "自定义页码范围",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "例如: 1,3,5-10,15 (使用逗号分隔，用-表示范围)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFFE5E5EA),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Numbers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}
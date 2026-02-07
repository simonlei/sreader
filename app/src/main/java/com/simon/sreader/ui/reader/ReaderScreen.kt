package com.simon.sreader.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simon.sreader.data.UserSettings
import com.simon.sreader.ui.theme.ReadingThemes
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 阅读主界面
 */
@Composable
fun ReaderScreen(
    bookId: Long,
    viewModel: ReaderViewModel,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenToc: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val density = LocalDensity.current

    // 保持屏幕常亮
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // 加载书籍
    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    val readingColors = ReadingThemes.fromName(settings.backgroundTheme)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readingColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        when {
            uiState.isLoading -> {
                // 加载中 - 展示进度信息
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.loadingProgress > 0f) {
                        // 大文件：显示确定性进度指示器
                        CircularProgressIndicator(
                            progress = { uiState.loadingProgress }
                        )
                    } else {
                        // 小文件或初始阶段：不确定性转圈动画
                        CircularProgressIndicator()
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.loadingPhase.isNotEmpty()) {
                        Text(
                            text = uiState.loadingPhase,
                            color = readingColors.textColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (uiState.loadingProgress > 0f) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(uiState.loadingProgress * 100).roundToInt()}%",
                            color = readingColors.textColor.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val warning = uiState.memoryWarning
                    if (warning != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = warning,
                            color = Color(0xFFFF9800),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            uiState.error != null -> {
                // 错误提示
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error ?: "未知错误",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "点击返回",
                        color = readingColors.textColor,
                        modifier = Modifier.clickable { onBack() }
                    )
                }
            }
            else -> {
                // 阅读内容区域
                ReadingContent(
                    uiState = uiState,
                    settings = settings,
                    readingColors = readingColors,
                    viewModel = viewModel
                )

                // 护眼模式遮罩
                if (settings.eyeCareMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x33FFCC00))
                    )
                }

                // 底部页码信息（始终显示）
                PageIndicator(
                    currentPage = uiState.currentPage,
                    totalPages = uiState.totalPages,
                    textColor = readingColors.textColor.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 8.dp)
                )

                // 工具栏（点击中间区域展开）
                AnimatedVisibility(
                    visible = uiState.showToolbar,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    ToolbarOverlay(
                        bookName = uiState.bookName,
                        currentPage = uiState.currentPage,
                        totalPages = uiState.totalPages,
                        onBack = onBack,
                        onSearch = onOpenSearch,
                        onToc = onOpenToc,
                        onSettings = onOpenSettings,
                        onPageChange = { viewModel.goToPage(it) },
                        onDismiss = { viewModel.setToolbarVisible(false) }
                    )
                }
            }
        }
    }
}

/**
 * 阅读内容区域（带手势处理）
 */
@Composable
private fun ReadingContent(
    uiState: ReaderUiState,
    settings: UserSettings,
    readingColors: com.simon.sreader.ui.theme.ReadingColors,
    viewModel: ReaderViewModel
) {
    val density = LocalDensity.current
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .onSizeChanged { size ->
                viewModel.setPageSize(size.width, size.height)
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val screenWidth = size.width.toFloat()
                    val leftZone = screenWidth * 0.3f
                    val rightZone = screenWidth * 0.7f

                    when {
                        // 中间区域 - 切换工具栏
                        offset.x in leftZone..rightZone -> {
                            viewModel.toggleToolbar()
                        }
                        // 左侧/右侧 - 翻下一页（按需求：左右点击都是下一页）
                        else -> {
                            viewModel.nextPage()
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = {
                        if (abs(dragAccumulator) > 100f) {
                            if (dragAccumulator < 0) {
                                // 左滑 → 下一页
                                viewModel.nextPage()
                            } else {
                                // 右滑 → 上一页
                                viewModel.previousPage()
                            }
                        }
                        dragAccumulator = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    }
                )
            }
    ) {
        Text(
            text = uiState.currentPageText,
            style = TextStyle(
                fontSize = settings.fontSize.sp,
                lineHeight = (settings.fontSize * settings.lineSpacingMultiplier).sp,
                color = readingColors.textColor
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 页码指示器
 */
@Composable
private fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = if (totalPages > 0) "${currentPage + 1} / $totalPages" else "",
        style = TextStyle(fontSize = 12.sp, color = textColor),
        modifier = modifier
    )
}

/**
 * 工具栏浮层
 */
@Composable
private fun ToolbarOverlay(
    bookName: String,
    currentPage: Int,
    totalPages: Int,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onToc: () -> Unit,
    onSettings: () -> Unit,
    onPageChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            }
    ) {
        // 顶部工具栏
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
                Text(
                    text = bookName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, "搜索")
                }
                IconButton(onClick = onToc) {
                    Icon(Icons.Default.FormatListBulleted, "目录")
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, "设置")
                }
            }
        }

        // 底部进度条
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                var sliderValue by remember(currentPage, totalPages) {
                    mutableFloatStateOf(
                        if (totalPages > 1) currentPage.toFloat() / (totalPages - 1) else 0f
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentPage + 1}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            if (totalPages > 1) {
                                val targetPage = (sliderValue * (totalPages - 1)).roundToInt()
                                onPageChange(targetPage)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors()
                    )
                    Text(
                        text = "$totalPages",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

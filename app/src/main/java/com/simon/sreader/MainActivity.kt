package com.simon.sreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simon.sreader.core.ReadingTimer
import com.simon.sreader.core.TocParser
import com.simon.sreader.data.AppDatabase
import com.simon.sreader.ui.home.HomeScreen
import com.simon.sreader.ui.home.HomeViewModel
import com.simon.sreader.ui.reader.ReaderScreen
import com.simon.sreader.ui.reader.ReaderViewModel
import com.simon.sreader.ui.reader.SearchBar
import com.simon.sreader.ui.reader.SettingsPanel
import com.simon.sreader.ui.reader.TocPanel
import com.simon.sreader.ui.stats.StatsScreen
import com.simon.sreader.ui.stats.StatsViewModel
import com.simon.sreader.ui.theme.SReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SReaderTheme {
                SReaderApp()
            }
        }
    }
}

@Composable
fun SReaderApp() {
    var currentScreen: Screen by remember { mutableStateOf(Screen.Home) }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            val viewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = viewModel,
                onOpenBook = { bookId ->
                    currentScreen = Screen.Reader(bookId)
                },
                onOpenStats = {
                    currentScreen = Screen.Stats
                }
            )
        }
        is Screen.Reader -> {
            ReaderScreenWithPanels(
                bookId = screen.bookId,
                onBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.Stats -> {
            val viewModel: StatsViewModel = viewModel()
            StatsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = Screen.Home }
            )
        }
    }
}

/**
 * 阅读页 + 搜索/目录/设置面板整合
 */
@Composable
private fun ReaderScreenWithPanels(
    bookId: Long,
    onBack: () -> Unit
) {
    val viewModel: ReaderViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // 面板显示状态
    var showSearch by remember { mutableStateOf(false) }
    var showToc by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // 目录数据
    var toc by remember { mutableStateOf<List<TocParser.TocEntry>>(emptyList()) }

    // 阅读计时器
    val context = LocalContext.current
    val readingTimer = remember {
        val db = AppDatabase.getInstance(context)
        ReadingTimer(db.readingStatsDao(), kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
    }

    // 生命周期监听 - 控制计时器
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, bookId) {
        readingTimer.setBook(bookId)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> readingTimer.start()
                Lifecycle.Event.ON_PAUSE -> readingTimer.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        readingTimer.start()

        onDispose {
            readingTimer.pause()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 文本加载完毕后解析目录
    if (uiState.currentPageText.isNotEmpty() && toc.isEmpty()) {
        val fullText = viewModel.getFullText()
        if (fullText.isNotEmpty()) {
            toc = TocParser.parse(fullText)
        }
    }

    // 主阅读界面
    ReaderScreen(
        bookId = bookId,
        viewModel = viewModel,
        onBack = onBack,
        onOpenSearch = {
            viewModel.setToolbarVisible(false)
            showSearch = true
        },
        onOpenToc = {
            viewModel.setToolbarVisible(false)
            showToc = true
        },
        onOpenSettings = {
            viewModel.setToolbarVisible(false)
            showSettings = true
        }
    )

    // 搜索栏叠加层
    if (showSearch) {
        SearchBar(
            fullText = viewModel.getFullText(),
            onNavigateToOffset = { offset ->
                viewModel.goToOffset(offset)
            },
            onClose = { showSearch = false }
        )
    }

    // 目录面板
    if (showToc && toc.isNotEmpty()) {
        val currentChapter = if (uiState.currentPage in viewModel.getPages().indices) {
            TocParser.findCurrentChapter(
                toc,
                viewModel.getPages()[uiState.currentPage].startOffset
            )
        } else -1

        TocPanel(
            toc = toc,
            currentChapterIndex = currentChapter,
            onSelectChapter = { entry ->
                viewModel.goToOffset(entry.charOffset)
                showToc = false
            },
            onDismiss = { showToc = false }
        )
    }

    // 设置面板
    if (showSettings) {
        SettingsPanel(
            settings = settings,
            onSettingsChange = { newSettings ->
                viewModel.saveSettings(newSettings)
            },
            onDismiss = { showSettings = false }
        )
    }
}
package com.simon.sreader.ui.reader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simon.sreader.core.FileLoader
import com.simon.sreader.core.TextPaginator
import com.simon.sreader.data.AppDatabase
import com.simon.sreader.data.BookRecord
import com.simon.sreader.data.UserSettings
import com.simon.sreader.ui.theme.ReadingColors
import com.simon.sreader.ui.theme.ReadingThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 阅读页 ViewModel
 */
class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val bookRecordDao = db.bookRecordDao()
    private val settingsDao = db.userSettingsDao()
    private val paginator = TextPaginator()

    // --- 状态 ---
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    /** 全文文本（内存中保存） */
    private var fullText: String = ""
    /** 分页结果 */
    private var pages: List<TextPaginator.PageInfo> = emptyList()
    /** 当前书籍记录 */
    private var currentBook: BookRecord? = null

    // --- 分页参数（由 UI 层传入） ---
    private var pageWidthPx: Int = 0
    private var pageHeightPx: Int = 0

    init {
        // 加载用户设置
        viewModelScope.launch {
            settingsDao.getSettings().collect { s ->
                _settings.value = s ?: UserSettings()
            }
        }
    }

    /**
     * 通过书籍ID加载文件
     */
    fun loadBook(bookId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    loadingPhase = "正在准备...",
                    loadingProgress = 0f,
                    memoryWarning = null
                )
                val book = bookRecordDao.getBookById(bookId)
                    ?: throw IllegalStateException("找不到书籍记录: $bookId")
                currentBook = book

                val context = getApplication<Application>()
                val uri = Uri.parse(book.fileUri)

                // 内存安全检查
                val fileSize = FileLoader.getFileSize(context, uri)
                if (fileSize > 0) {
                    val runtime = Runtime.getRuntime()
                    val maxMemory = runtime.maxMemory()
                    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                    val availableMemory = maxMemory - usedMemory
                    if (fileSize > availableMemory * 0.5) {
                        _uiState.value = _uiState.value.copy(
                            memoryWarning = "文件较大（${fileSize / 1024 / 1024}MB），可能影响阅读体验"
                        )
                    }
                }

                // 使用带进度回调的流式文件加载
                val (encoding, text) = FileLoader.loadFile(context, uri) { phase, progress ->
                    _uiState.value = _uiState.value.copy(
                        loadingPhase = phase,
                        loadingProgress = progress * 0.5f // 文件读取占总进度的 50%
                    )
                }

                fullText = text

                // 更新编码和总字符数
                val updatedBook = book.copy(
                    encoding = encoding,
                    totalChars = text.length,
                    lastReadTime = System.currentTimeMillis()
                )
                bookRecordDao.update(updatedBook)
                currentBook = updatedBook

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    bookName = book.fileName,
                    totalChars = text.length,
                    savedCharOffset = book.readPosition,
                    loadingPhase = "",
                    loadingProgress = 0f
                )

                // 如果已有页面尺寸，立即分页
                if (pageWidthPx > 0 && pageHeightPx > 0) {
                    repaginate()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}",
                    loadingPhase = "",
                    loadingProgress = 0f
                )
            }
        }
    }

    /**
     * 设置页面显示尺寸（UI 层在 onSizeChanged 时调用）
     */
    fun setPageSize(widthPx: Int, heightPx: Int) {
        if (widthPx == pageWidthPx && heightPx == pageHeightPx) return
        pageWidthPx = widthPx
        pageHeightPx = heightPx
        if (fullText.isNotEmpty()) {
            repaginate()
        }
    }

    /**
     * 重新分页
     */
    private fun repaginate() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingPhase = "正在排版...",
                loadingProgress = 0.5f // 从 50% 开始（文件读取已占 0~50%）
            )

            val s = _settings.value
            val fontSizePx = s.fontSize * getApplication<Application>().resources.displayMetrics.scaledDensity
            val result = paginator.paginate(
                text = fullText,
                widthPx = pageWidthPx,
                heightPx = pageHeightPx,
                fontSizePx = fontSizePx,
                lineSpacingMultiplier = s.lineSpacingMultiplier,
                onProgress = { progress ->
                    // 分页进度映射到总进度的 50%~100%
                    _uiState.value = _uiState.value.copy(
                        loadingPhase = "正在排版...",
                        loadingProgress = 0.5f + progress * 0.5f
                    )
                }
            )
            pages = result.pages

            // 恢复上次阅读位置
            val savedOffset = _uiState.value.savedCharOffset
            val page = if (savedOffset > 0) {
                paginator.findPageByOffset(pages, savedOffset)
            } else {
                _uiState.value.currentPage
            }

            val pageText = if (pages.isNotEmpty()) {
                paginator.getPageText(fullText, pages[page.coerceIn(0, pages.lastIndex)])
            } else ""

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentPage = page.coerceIn(0, (result.totalPages - 1).coerceAtLeast(0)),
                totalPages = result.totalPages,
                currentPageText = pageText,
                savedCharOffset = 0, // 已恢复，清除标记
                loadingPhase = "",
                loadingProgress = 0f
            )
        }
    }

    /**
     * 翻到下一页
     */
    fun nextPage() {
        val state = _uiState.value
        if (state.currentPage < state.totalPages - 1) {
            goToPage(state.currentPage + 1)
        }
    }

    /**
     * 翻到上一页
     */
    fun previousPage() {
        val state = _uiState.value
        if (state.currentPage > 0) {
            goToPage(state.currentPage - 1)
        }
    }

    /**
     * 跳转到指定页码（0-based）
     */
    fun goToPage(page: Int) {
        if (pages.isEmpty()) return
        val targetPage = page.coerceIn(0, pages.lastIndex)
        val pageText = paginator.getPageText(fullText, pages[targetPage])

        _uiState.value = _uiState.value.copy(
            currentPage = targetPage,
            currentPageText = pageText
        )

        // 保存阅读位置
        saveReadPosition(pages[targetPage].startOffset)
    }

    /**
     * 跳转到指定字符偏移量位置
     */
    fun goToOffset(charOffset: Int) {
        if (pages.isEmpty()) return
        val page = paginator.findPageByOffset(pages, charOffset)
        goToPage(page)
    }

    /**
     * 保存阅读位置
     */
    private fun saveReadPosition(charOffset: Int) {
        val book = currentBook ?: return
        viewModelScope.launch(Dispatchers.IO) {
            bookRecordDao.updateReadPosition(book.id, charOffset)
        }
    }

    /**
     * 切换工具栏显示
     */
    fun toggleToolbar() {
        _uiState.value = _uiState.value.copy(
            showToolbar = !_uiState.value.showToolbar
        )
    }

    /**
     * 设置显示/隐藏工具栏
     */
    fun setToolbarVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showToolbar = visible)
    }

    /**
     * 获取全文（用于搜索）
     */
    fun getFullText(): String = fullText

    /**
     * 获取分页列表（用于目录跳转）
     */
    fun getPages(): List<TextPaginator.PageInfo> = pages

    /**
     * 保存用户设置并触发重新分页
     */
    fun saveSettings(newSettings: UserSettings) {
        _settings.value = newSettings
        viewModelScope.launch(Dispatchers.IO) {
            settingsDao.save(newSettings)
        }
        onSettingsChanged()
    }

    /**
     * 设置变更后触发重新分页
     */
    fun onSettingsChanged() {
        if (fullText.isNotEmpty() && pageWidthPx > 0 && pageHeightPx > 0) {
            // 记住当前字符偏移量
            val currentOffset = if (pages.isNotEmpty() && _uiState.value.currentPage in pages.indices) {
                pages[_uiState.value.currentPage].startOffset
            } else 0
            _uiState.value = _uiState.value.copy(savedCharOffset = currentOffset)
            repaginate()
        }
    }
}

/**
 * 阅读页 UI 状态
 */
data class ReaderUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val bookName: String = "",
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalChars: Int = 0,
    val currentPageText: String = "",
    val showToolbar: Boolean = false,
    /** 待恢复的字符偏移量（分页完成后自动恢复） */
    val savedCharOffset: Int = 0,
    /** 加载阶段描述（如"正在读取文件..."、"正在排版..."） */
    val loadingPhase: String = "",
    /** 加载进度 (0f ~ 1f) */
    val loadingProgress: Float = 0f,
    /** 内存警告信息 */
    val memoryWarning: String? = null
)

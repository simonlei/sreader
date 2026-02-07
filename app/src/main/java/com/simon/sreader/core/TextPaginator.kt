package com.simon.sreader.core

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * 文本分页引擎
 * 根据显示区域尺寸、字体大小、行间距将全文切分为页面列表
 */
class TextPaginator {

    /**
     * 页面信息：起始/结束字符偏移量
     */
    data class PageInfo(
        val startOffset: Int,
        val endOffset: Int
    )

    /**
     * 分页结果
     */
    data class PaginationResult(
        val pages: List<PageInfo>,
        val totalPages: Int
    )

    /**
     * 执行分页（无进度回调版本，保持向后兼容）
     * @param text 全文文本
     * @param widthPx 显示区域宽度（像素）
     * @param heightPx 显示区域高度（像素）
     * @param fontSizePx 字体大小（像素）
     * @param lineSpacingMultiplier 行间距倍数
     * @return 分页结果
     */
    fun paginate(
        text: String,
        widthPx: Int,
        heightPx: Int,
        fontSizePx: Float,
        lineSpacingMultiplier: Float = 1.5f
    ): PaginationResult {
        return paginate(text, widthPx, heightPx, fontSizePx, lineSpacingMultiplier, onProgress = null)
    }

    /**
     * 执行分页（带进度回调版本）
     *
     * 优化策略：使用基于偏移量的窗口式测量，避免 O(n²) 的 substring 调用。
     * 每次仅从 currentOffset 取一个合理的窗口范围进行 StaticLayout 测量，
     * 而不是传入全部剩余文本。
     *
     * @param text 全文文本
     * @param widthPx 显示区域宽度（像素）
     * @param heightPx 显示区域高度（像素）
     * @param fontSizePx 字体大小（像素）
     * @param lineSpacingMultiplier 行间距倍数
     * @param onProgress 分页进度回调 (0f~1f)，传 null 则不报告进度
     * @return 分页结果
     */
    fun paginate(
        text: String,
        widthPx: Int,
        heightPx: Int,
        fontSizePx: Float,
        lineSpacingMultiplier: Float = 1.5f,
        onProgress: ((Float) -> Unit)?
    ): PaginationResult {
        if (text.isEmpty() || widthPx <= 0 || heightPx <= 0) {
            return PaginationResult(emptyList(), 0)
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizePx
        }

        val pages = mutableListOf<PageInfo>()
        var currentOffset = 0
        val textLength = text.length

        // 预估每页字符数，用于确定测量窗口大小
        // 粗略估算：每行字符数 ≈ widthPx / fontSizePx，每页行数 ≈ heightPx / (fontSizePx * lineSpacingMultiplier)
        val estimatedCharsPerLine = (widthPx / fontSizePx).toInt().coerceAtLeast(1)
        val estimatedLinesPerPage = (heightPx / (fontSizePx * lineSpacingMultiplier)).toInt().coerceAtLeast(1)
        val estimatedCharsPerPage = estimatedCharsPerLine * estimatedLinesPerPage
        // 窗口大小为预估页字符数的 5 倍，确保足够覆盖一页
        val baseWindowSize = estimatedCharsPerPage * 5

        var lastReportedPercent = -1

        while (currentOffset < textLength) {
            // 使用窗口限制：从 currentOffset 开始，仅取 windowSize 范围的文本进行测量
            var windowEnd = (currentOffset + baseWindowSize).coerceAtMost(textLength)

            // 创建 StaticLayout，直接在原始 text 上使用偏移量，避免 substring
            var layout = StaticLayout.Builder.obtain(
                text, currentOffset, windowEnd, textPaint, widthPx
            )
                .setLineSpacing(0f, lineSpacingMultiplier)
                .setIncludePad(false)
                .build()

            // 找出在可用高度内能显示多少行
            var linesInPage = 0
            for (i in 0 until layout.lineCount) {
                val lineBottom = layout.getLineBottom(i)
                if (lineBottom > heightPx) break
                linesInPage = i + 1
            }

            // 如果窗口内所有行都能放下，但还没到文本末尾，且行数等于窗口内总行数，
            // 说明窗口可能不够大（所有行都在一页内但文本还有更多）
            // 这种情况下整个窗口就是一页内容
            if (linesInPage == layout.lineCount && windowEnd < textLength) {
                // 窗口内所有行都放得下，直接使用窗口末尾作为页结束
                // 但需要检查是否窗口太小导致一页都没测完
                // 如果窗口所有行高度 < heightPx，需要扩大窗口
                val lastLineBottom = layout.getLineBottom(layout.lineCount - 1)
                if (lastLineBottom < heightPx) {
                    // 扩大窗口重新测量
                    windowEnd = textLength
                    layout = StaticLayout.Builder.obtain(
                        text, currentOffset, windowEnd, textPaint, widthPx
                    )
                        .setLineSpacing(0f, lineSpacingMultiplier)
                        .setIncludePad(false)
                        .build()

                    linesInPage = 0
                    for (i in 0 until layout.lineCount) {
                        val lineBottom = layout.getLineBottom(i)
                        if (lineBottom > heightPx) break
                        linesInPage = i + 1
                    }
                }
            }

            // 如果一行都放不下，至少放一行
            if (linesInPage == 0) linesInPage = 1

            // 计算这些行对应的字符偏移量（StaticLayout 返回的是相对于传入 start 的偏移）
            val pageEndOffset = if (linesInPage >= layout.lineCount) {
                windowEnd
            } else {
                // getLineStart 返回的偏移量是相对于传入的 text 的绝对偏移
                layout.getLineStart(linesInPage)
            }

            pages.add(PageInfo(currentOffset, pageEndOffset))
            currentOffset = pageEndOffset

            // 进度回调（每 1% 报告一次）
            if (onProgress != null) {
                val currentPercent = (currentOffset * 100 / textLength)
                if (currentPercent > lastReportedPercent) {
                    lastReportedPercent = currentPercent
                    onProgress.invoke(currentOffset.toFloat() / textLength)
                }
            }
        }

        // 至少保证有一页
        if (pages.isEmpty()) {
            pages.add(PageInfo(0, textLength))
        }

        onProgress?.invoke(1f)
        return PaginationResult(pages, pages.size)
    }

    /**
     * 根据字符偏移量查找对应的页码（0-based）
     */
    fun findPageByOffset(pages: List<PageInfo>, charOffset: Int): Int {
        if (pages.isEmpty()) return 0
        for (i in pages.indices) {
            if (charOffset < pages[i].endOffset) return i
        }
        return pages.lastIndex
    }

    /**
     * 获取指定页的文本内容
     */
    fun getPageText(text: String, pageInfo: PageInfo): String {
        val start = pageInfo.startOffset.coerceIn(0, text.length)
        val end = pageInfo.endOffset.coerceIn(start, text.length)
        return text.substring(start, end)
    }
}

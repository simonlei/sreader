package com.simon.sreader.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simon.sreader.data.BookDuration

/**
 * 阅读时长统计页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit
) {
    val tabIndex by viewModel.tabIndex.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val bookRanking by viewModel.bookRanking.collectAsState()
    val totalDuration by viewModel.totalDuration.collectAsState()

    val tabs = listOf("按天", "按周", "按月")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("阅读统计") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab 切换
            item {
                TabRow(selectedTabIndex = tabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = { viewModel.selectTab(index) },
                            text = { Text(title) }
                        )
                    }
                }
            }

            // 总计
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "总计",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(totalDuration),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 柱状图
            item {
                if (chartData.isNotEmpty()) {
                    BarChart(
                        data = chartData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 分割线
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "书籍阅读时长排行",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 书籍排行
            if (bookRanking.isEmpty()) {
                item {
                    Text(
                        text = "暂无数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                itemsIndexed(bookRanking) { index, book ->
                    BookRankingItem(index + 1, book)
                }
            }

            // 底部间距
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

/**
 * 使用 Canvas 自绘简易柱状图
 */
@Composable
private fun BarChart(
    data: List<ChartItem>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val maxValue = data.maxOf { it.durationSeconds }.coerceAtLeast(1)
        val barCount = data.size
        val totalWidth = size.width
        val chartHeight = size.height - 40f // 留出底部标签空间
        val barWidth = (totalWidth / barCount) * 0.6f
        val gap = (totalWidth / barCount) * 0.4f

        data.forEachIndexed { index, item ->
            val barHeight = (item.durationSeconds.toFloat() / maxValue) * chartHeight * 0.9f
            val x = index * (totalWidth / barCount) + gap / 2

            // 绘制柱子
            if (barHeight > 0) {
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(x, chartHeight - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }

            // 绘制底部标签
            drawContext.canvas.nativeCanvas.drawText(
                item.label,
                x + barWidth / 2,
                size.height - 4f,
                android.graphics.Paint().apply {
                    color = onSurfaceColor.hashCode()
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )

            // 柱顶数值（仅大于0时显示）
            if (item.durationSeconds > 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    formatDurationShort(item.durationSeconds),
                    x + barWidth / 2,
                    chartHeight - barHeight - 8f,
                    android.graphics.Paint().apply {
                        color = onSurfaceColor.hashCode()
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

/**
 * 书籍排行列表项
 */
@Composable
private fun BookRankingItem(rank: Int, book: BookDuration) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = when (rank) {
                1 -> Color(0xFFFFD700)
                2 -> Color(0xFFC0C0C0)
                3 -> Color(0xFFCD7F32)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.width(36.dp)
        )
        Icon(
            Icons.Default.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = book.fileName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = formatDuration(book.totalDuration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 格式化时长（秒 → 可读格式）
 */
private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}秒"
        seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
        else -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
    }
}

/**
 * 简短时长格式（用于图表标签）
 */
private fun formatDurationShort(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        else -> "${seconds / 3600}h${(seconds % 3600) / 60}m"
    }
}

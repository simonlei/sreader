package com.simon.sreader.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simon.sreader.core.TocParser

/**
 * 目录面板（BottomSheet）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TocPanel(
    toc: List<TocParser.TocEntry>,
    currentChapterIndex: Int,
    onSelectChapter: (TocParser.TocEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    // 自动滚动到当前章节
    LaunchedEffect(currentChapterIndex) {
        if (currentChapterIndex >= 0 && toc.isNotEmpty()) {
            listState.animateScrollToItem(currentChapterIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "目录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            HorizontalDivider()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                itemsIndexed(toc) { index, entry ->
                    TocItem(
                        entry = entry,
                        isCurrentChapter = index == currentChapterIndex,
                        onClick = { onSelectChapter(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TocItem(
    entry: TocParser.TocEntry,
    isCurrentChapter: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = if (entry.level > 1) 48.dp else 24.dp,
                end = 24.dp,
                top = 14.dp,
                bottom = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrentChapter) {
            Spacer(
                modifier = Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .padding(end = 0.dp)
            )
        }
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrentChapter) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

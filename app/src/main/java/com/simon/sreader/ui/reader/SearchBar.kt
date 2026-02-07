package com.simon.sreader.ui.reader

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.simon.sreader.core.TextSearchEngine

/**
 * 搜索栏
 */
@Composable
fun SearchBar(
    fullText: String,
    onNavigateToOffset: (Int) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TextSearchEngine.SearchResult>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(-1) }
    val focusRequester = remember { FocusRequester() }

    // 自动聚焦
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 搜索逻辑
    fun doSearch() {
        if (query.isNotEmpty()) {
            results = TextSearchEngine.search(fullText, query)
            currentIndex = if (results.isNotEmpty()) 0 else -1
            if (results.isNotEmpty()) {
                onNavigateToOffset(results[0].offset)
            }
        } else {
            results = emptyList()
            currentIndex = -1
        }
    }

    fun navigateNext() {
        if (results.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % results.size
            onNavigateToOffset(results[currentIndex].offset)
        }
    }

    fun navigatePrevious() {
        if (results.isNotEmpty()) {
            currentIndex = if (currentIndex - 1 < 0) results.lastIndex else currentIndex - 1
            onNavigateToOffset(results[currentIndex].offset)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = {
                    query = it
                    doSearch()
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("搜索...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // 匹配计数
            if (query.isNotEmpty()) {
                Text(
                    text = if (results.isNotEmpty()) "${currentIndex + 1}/${results.size}"
                    else "0/0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // 上一个 / 下一个
            IconButton(
                onClick = { navigatePrevious() },
                enabled = results.isNotEmpty()
            ) {
                Icon(Icons.Default.KeyboardArrowUp, "上一个")
            }
            IconButton(
                onClick = { navigateNext() },
                enabled = results.isNotEmpty()
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "下一个")
            }

            // 关闭
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "关闭搜索")
            }
        }
    }
}

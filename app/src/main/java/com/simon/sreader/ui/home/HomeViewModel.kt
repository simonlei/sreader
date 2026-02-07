package com.simon.sreader.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simon.sreader.core.FileLoader
import com.simon.sreader.data.AppDatabase
import com.simon.sreader.data.BookRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val bookRecordDao = db.bookRecordDao()

    private val _books = MutableStateFlow<List<BookRecord>>(emptyList())
    val books: StateFlow<List<BookRecord>> = _books.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            bookRecordDao.getAllBooks().collect { allBooks ->
                // 过滤不可访问的文件
                val accessibleBooks = allBooks.filter { book ->
                    try {
                        val uri = Uri.parse(book.fileUri)
                        FileLoader.isUriAccessible(getApplication(), uri)
                    } catch (e: Exception) {
                        false
                    }
                }
                _books.value = accessibleBooks
                _isLoading.value = false
            }
        }
    }

    /**
     * 打开新文件并创建记录
     * @return 新创建/已存在的书籍记录ID
     */
    fun openFile(uri: Uri, onResult: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()

                // 获取持久化 URI 权限
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val uriString = uri.toString()

                // 检查是否已存在
                val existing = bookRecordDao.getBookByUri(uriString)
                if (existing != null) {
                    // 更新最后阅读时间
                    bookRecordDao.update(existing.copy(lastReadTime = System.currentTimeMillis()))
                    onResult(existing.id)
                    return@launch
                }

                // 新文件 - 获取文件名
                val fileName = FileLoader.getFileName(context, uri)

                val record = BookRecord(
                    fileUri = uriString,
                    fileName = fileName,
                    lastReadTime = System.currentTimeMillis()
                )
                val id = bookRecordDao.insert(record)
                onResult(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 删除书籍记录
     */
    fun deleteBook(book: BookRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            bookRecordDao.delete(book)
        }
    }
}

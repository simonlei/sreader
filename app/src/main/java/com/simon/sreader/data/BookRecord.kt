package com.simon.sreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书籍阅读记录
 */
@Entity(tableName = "book_records")
data class BookRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 文件 SAF URI */
    val fileUri: String,
    /** 文件显示名 */
    val fileName: String,
    /** 检测到的编码格式 */
    val encoding: String = "UTF-8",
    /** 最后阅读时间戳 */
    val lastReadTime: Long = System.currentTimeMillis(),
    /** 阅读位置（字符偏移量） */
    val readPosition: Int = 0,
    /** 文本总字符数 */
    val totalChars: Int = 0
)

package com.simon.sreader.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 阅读时长统计记录（按天+按书籍）
 */
@Entity(
    tableName = "reading_stats",
    foreignKeys = [
        ForeignKey(
            entity = BookRecord::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId", "date"], unique = true)]
)
data class ReadingStats(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 关联的书籍ID */
    val bookId: Long,
    /** 日期 yyyy-MM-dd */
    val date: String,
    /** 当天阅读时长（秒） */
    val durationSeconds: Long = 0
)

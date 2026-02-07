package com.simon.sreader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingStatsDao {

    /** 增加指定书籍在指定日期的阅读时长 */
    @Query("""
        INSERT INTO reading_stats (bookId, date, durationSeconds) 
        VALUES (:bookId, :date, :seconds)
        ON CONFLICT(bookId, date) 
        DO UPDATE SET durationSeconds = durationSeconds + :seconds
    """)
    suspend fun addDuration(bookId: Long, date: String, seconds: Long)

    /** 按天查询阅读时长（最近N天） */
    @Query("""
        SELECT date, SUM(durationSeconds) as durationSeconds, 0 as id, 0 as bookId 
        FROM reading_stats 
        WHERE date >= :sinceDate 
        GROUP BY date 
        ORDER BY date ASC
    """)
    fun getDailyStats(sinceDate: String): Flow<List<ReadingStats>>

    /** 查询指定书籍的累计阅读时长 */
    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM reading_stats WHERE bookId = :bookId")
    suspend fun getTotalDurationForBook(bookId: Long): Long

    /** 获取所有书籍的累计阅读时长排行 */
    @Query("""
        SELECT b.id, b.fileName, COALESCE(SUM(s.durationSeconds), 0) as totalDuration
        FROM book_records b 
        LEFT JOIN reading_stats s ON b.id = s.bookId 
        GROUP BY b.id 
        ORDER BY totalDuration DESC
    """)
    fun getBookDurationRanking(): Flow<List<BookDuration>>

    /** 获取某段时间范围内的所有统计记录 */
    @Query("SELECT * FROM reading_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getStatsBetween(startDate: String, endDate: String): Flow<List<ReadingStats>>
}

/** 书籍累计阅读时长投影 */
data class BookDuration(
    val id: Long,
    val fileName: String,
    val totalDuration: Long
)

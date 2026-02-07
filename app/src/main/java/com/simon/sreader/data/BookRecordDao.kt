package com.simon.sreader.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookRecordDao {

    /** 按最近阅读时间倒序获取所有记录 */
    @Query("SELECT * FROM book_records ORDER BY lastReadTime DESC")
    fun getAllBooks(): Flow<List<BookRecord>>

    /** 根据ID获取单条记录 */
    @Query("SELECT * FROM book_records WHERE id = :id")
    suspend fun getBookById(id: Long): BookRecord?

    /** 根据文件URI查找记录 */
    @Query("SELECT * FROM book_records WHERE fileUri = :uri LIMIT 1")
    suspend fun getBookByUri(uri: String): BookRecord?

    /** 插入记录，冲突时替换 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookRecord): Long

    /** 更新记录 */
    @Update
    suspend fun update(book: BookRecord)

    /** 删除记录 */
    @Delete
    suspend fun delete(book: BookRecord)

    /** 更新阅读位置和时间 */
    @Query("UPDATE book_records SET readPosition = :position, lastReadTime = :time WHERE id = :id")
    suspend fun updateReadPosition(id: Long, position: Int, time: Long = System.currentTimeMillis())
}

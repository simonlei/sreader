package com.simon.sreader.core

import com.simon.sreader.data.ReadingStatsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 阅读计时器
 * 在 onResume 时开始计时，onPause 时暂停
 * 每 30 秒自动将累计时长写入数据库
 */
class ReadingTimer(
    private val readingStatsDao: ReadingStatsDao,
    private val scope: CoroutineScope
) {
    private var bookId: Long = 0
    private var isRunning = false
    private var accumulatedSeconds: Long = 0
    private var lastTickTime: Long = 0
    private var timerJob: Job? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 设置当前阅读的书籍
     */
    fun setBook(bookId: Long) {
        if (this.bookId != bookId) {
            // 切换书籍时，先保存之前的累计时长
            flush()
            this.bookId = bookId
            accumulatedSeconds = 0
        }
    }

    /**
     * 开始计时（onResume）
     */
    fun start() {
        if (isRunning || bookId == 0L) return
        isRunning = true
        lastTickTime = System.currentTimeMillis()

        // 启动定时保存任务
        timerJob = scope.launch(Dispatchers.IO) {
            while (isRunning) {
                delay(30_000) // 每 30 秒
                tick()
                flush()
            }
        }
    }

    /**
     * 暂停计时（onPause）
     */
    fun pause() {
        if (!isRunning) return
        isRunning = false
        timerJob?.cancel()
        timerJob = null
        tick()
        flush()
    }

    /**
     * 计算自上次 tick 以来经过的秒数
     */
    private fun tick() {
        val now = System.currentTimeMillis()
        val elapsed = (now - lastTickTime) / 1000
        if (elapsed > 0) {
            accumulatedSeconds += elapsed
            lastTickTime = now
        }
    }

    /**
     * 将累计时长写入数据库
     */
    private fun flush() {
        if (accumulatedSeconds <= 0 || bookId == 0L) return
        val today = dateFormat.format(Date())
        val seconds = accumulatedSeconds
        accumulatedSeconds = 0

        scope.launch(Dispatchers.IO) {
            try {
                readingStatsDao.addDuration(bookId, today, seconds)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

package com.simon.sreader.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simon.sreader.data.AppDatabase
import com.simon.sreader.data.BookDuration
import com.simon.sreader.data.ReadingStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 统计页 ViewModel
 */
class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val readingStatsDao = db.readingStatsDao()

    private val _tabIndex = MutableStateFlow(0) // 0=天, 1=周, 2=月
    val tabIndex: StateFlow<Int> = _tabIndex.asStateFlow()

    private val _chartData = MutableStateFlow<List<ChartItem>>(emptyList())
    val chartData: StateFlow<List<ChartItem>> = _chartData.asStateFlow()

    private val _bookRanking = MutableStateFlow<List<BookDuration>>(emptyList())
    val bookRanking: StateFlow<List<BookDuration>> = _bookRanking.asStateFlow()

    private val _totalDuration = MutableStateFlow(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadData(0)
        loadBookRanking()
    }

    fun selectTab(index: Int) {
        _tabIndex.value = index
        loadData(index)
    }

    private fun loadData(tab: Int) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val endDate = dateFormat.format(calendar.time)

            when (tab) {
                0 -> { // 按天 - 最近7天
                    calendar.add(Calendar.DAY_OF_YEAR, -6)
                    val startDate = dateFormat.format(calendar.time)
                    readingStatsDao.getStatsBetween(startDate, endDate).collect { stats ->
                        val dailyMap = stats.groupBy { it.date }
                            .mapValues { (_, list) -> list.sumOf { it.durationSeconds } }

                        val items = mutableListOf<ChartItem>()
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -6)
                        for (i in 0..6) {
                            val date = dateFormat.format(cal.time)
                            val dayLabel = SimpleDateFormat("MM/dd", Locale.getDefault()).format(cal.time)
                            items.add(ChartItem(dayLabel, dailyMap[date] ?: 0))
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        _chartData.value = items
                        _totalDuration.value = items.sumOf { it.durationSeconds }
                    }
                }
                1 -> { // 按周 - 最近4周
                    calendar.add(Calendar.WEEK_OF_YEAR, -3)
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    val startDate = dateFormat.format(calendar.time)
                    readingStatsDao.getStatsBetween(startDate, endDate).collect { stats ->
                        val items = mutableListOf<ChartItem>()
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.WEEK_OF_YEAR, -3)
                        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)

                        for (i in 0..3) {
                            val weekStart = dateFormat.format(cal.time)
                            cal.add(Calendar.DAY_OF_YEAR, 6)
                            val weekEnd = dateFormat.format(cal.time)
                            val weekSeconds = stats
                                .filter { it.date in weekStart..weekEnd }
                                .sumOf { it.durationSeconds }
                            val label = "第${i + 1}周"
                            items.add(ChartItem(label, weekSeconds))
                            cal.add(Calendar.DAY_OF_YEAR, 1) // 到下一周
                        }
                        _chartData.value = items
                        _totalDuration.value = items.sumOf { it.durationSeconds }
                    }
                }
                2 -> { // 按月 - 最近12个月
                    calendar.add(Calendar.MONTH, -11)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    val startDate = dateFormat.format(calendar.time)
                    readingStatsDao.getStatsBetween(startDate, endDate).collect { stats ->
                        val items = mutableListOf<ChartItem>()
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.MONTH, -11)

                        for (i in 0..11) {
                            val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                            val monthLabel = SimpleDateFormat("M月", Locale.getDefault()).format(cal.time)
                            val monthSeconds = stats
                                .filter { it.date.startsWith(yearMonth) }
                                .sumOf { it.durationSeconds }
                            items.add(ChartItem(monthLabel, monthSeconds))
                            cal.add(Calendar.MONTH, 1)
                        }
                        _chartData.value = items
                        _totalDuration.value = items.sumOf { it.durationSeconds }
                    }
                }
            }
        }
    }

    private fun loadBookRanking() {
        viewModelScope.launch {
            readingStatsDao.getBookDurationRanking().collect {
                _bookRanking.value = it
            }
        }
    }
}

data class ChartItem(
    val label: String,
    val durationSeconds: Long
)

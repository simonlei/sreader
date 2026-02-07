package com.simon.sreader

/**
 * 应用页面路由定义
 */
sealed class Screen {
    /** 首页 - 历史记录列表 */
    data object Home : Screen()

    /** 阅读页 - 传入书籍记录ID */
    data class Reader(val bookId: Long) : Screen()

    /** 统计页 - 阅读时长统计 */
    data object Stats : Screen()
}

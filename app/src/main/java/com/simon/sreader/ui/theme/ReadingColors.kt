package com.simon.sreader.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 阅读界面背景色主题定义
 */
data class ReadingColors(
    val background: Color,
    val textColor: Color,
    val name: String
)

object ReadingThemes {
    val White = ReadingColors(Color(0xFFFFFFFF), Color(0xFF333333), "white")
    val Beige = ReadingColors(Color(0xFFF5F0E1), Color(0xFF5B4636), "beige")
    val Green = ReadingColors(Color(0xFFCCE8CF), Color(0xFF2D5A2E), "green")
    val Dark = ReadingColors(Color(0xFF1E1E1E), Color(0xFFCCCCCC), "dark")

    val allThemes = listOf(White, Beige, Green, Dark)

    fun fromName(name: String): ReadingColors {
        return allThemes.find { it.name == name } ?: White
    }
}

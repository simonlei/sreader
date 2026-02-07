package com.simon.sreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户设置（单行记录，id 固定为 1）
 */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,
    /** 字体大小 (sp) */
    val fontSize: Float = 18f,
    /** 行间距倍数 */
    val lineSpacingMultiplier: Float = 1.5f,
    /** 背景色主题: "white" / "beige" / "green" / "dark" */
    val backgroundTheme: String = "white",
    /** 护眼模式是否开启 */
    val eyeCareMode: Boolean = false,
    /** 屏幕亮度 0f~1f, -1f 表示跟随系统 */
    val brightness: Float = -1f
)

package com.simon.sreader.ui.reader

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simon.sreader.data.UserSettings
import com.simon.sreader.ui.theme.ReadingThemes

/**
 * 设置面板（BottomSheet）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val view = LocalView.current

    // 应用亮度设置
    LaunchedEffect(settings.brightness) {
        val activity = view.context as? Activity ?: return@LaunchedEffect
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = if (settings.brightness < 0) {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        } else {
            settings.brightness
        }
        activity.window.attributes = layoutParams
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // --- 字体大小 ---
            SettingsSectionTitle("字体大小")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        val newSize = (settings.fontSize - 1f).coerceAtLeast(12f)
                        onSettingsChange(settings.copy(fontSize = newSize))
                    }
                ) {
                    Icon(Icons.Default.Remove, "减小字体")
                }
                Text(
                    text = "${settings.fontSize.toInt()} sp",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(60.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(
                    onClick = {
                        val newSize = (settings.fontSize + 1f).coerceAtMost(36f)
                        onSettingsChange(settings.copy(fontSize = newSize))
                    }
                ) {
                    Icon(Icons.Default.Add, "增大字体")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 行间距 ---
            SettingsSectionTitle("行间距")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LineSpacingChip("紧凑", 1.0f, settings.lineSpacingMultiplier == 1.0f) {
                    onSettingsChange(settings.copy(lineSpacingMultiplier = 1.0f))
                }
                LineSpacingChip("标准", 1.5f, settings.lineSpacingMultiplier == 1.5f) {
                    onSettingsChange(settings.copy(lineSpacingMultiplier = 1.5f))
                }
                LineSpacingChip("宽松", 2.0f, settings.lineSpacingMultiplier == 2.0f) {
                    onSettingsChange(settings.copy(lineSpacingMultiplier = 2.0f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 背景色 ---
            SettingsSectionTitle("背景色")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReadingThemes.allThemes.forEach { theme ->
                    ColorChip(
                        color = theme.background,
                        isSelected = settings.backgroundTheme == theme.name,
                        label = when (theme.name) {
                            "white" -> "白色"
                            "beige" -> "米黄"
                            "green" -> "浅绿"
                            "dark" -> "深灰"
                            else -> theme.name
                        }
                    ) {
                        onSettingsChange(settings.copy(backgroundTheme = theme.name))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 护眼模式 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("护眼模式", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.eyeCareMode,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(eyeCareMode = it))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 亮度调节 ---
            SettingsSectionTitle("亮度")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("☀", fontSize = 14.sp)
                Slider(
                    value = if (settings.brightness < 0) 0.5f else settings.brightness,
                    onValueChange = {
                        onSettingsChange(settings.copy(brightness = it))
                    },
                    valueRange = 0.01f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Text("☀", fontSize = 22.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "跟随系统",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (settings.brightness < 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.clickable {
                        onSettingsChange(settings.copy(brightness = -1f))
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun LineSpacingChip(
    label: String,
    value: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "$label ${value}x",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ColorChip(
    color: Color,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

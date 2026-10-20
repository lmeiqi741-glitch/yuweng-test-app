package com.xinyu.yuweng.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 心屿色板（照 03 文档 2.1）
val WarmGold = Color(0xFFF4E4C1)       // 暖阳金
val MistBlue = Color(0xFFB8D8E3)       // 浅雾蓝
val MossGreen = Color(0xFFA8C9A1)      // 苔藓绿
val DuskPurple = Color(0xFFC8B6E2)     // 暮云紫
val SunsetOrange = Color(0xFFF5C6A5)   // 夕霞橙
val MoonWhite = Color(0xFFFAF7F2)      // 月光白
val DeepGray = Color(0xFF4A4A4A)       // 深黛灰

// 文字色
val InkColor = Color(0xFF2D2D2D)       // 墨色
val DaiGray = Color(0xFF4A4A4A)        // 黛灰
val FogGray = Color(0xFF8C8C8C)        // 雾灰
val Divider = Color(0xFFD4D4D4)        // 分割线
val SecondaryBg = Color(0xFFF0EDE7)     // 二级背景

private val LightColorScheme = lightColorScheme(
    primary = WarmGold,
    onPrimary = InkColor,
    secondary = MistBlue,
    onSecondary = InkColor,
    tertiary = DuskPurple,
    background = MoonWhite,
    onBackground = InkColor,
    surface = MoonWhite,
    onSurface = InkColor,
    surfaceVariant = SecondaryBg,
    outline = Divider
)

@Composable
fun YuwengTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

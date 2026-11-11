package com.popsmoke.tracker.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 主题只做两件事：决定强调色，以及决定压在强调色上的文字用黑还是白。
 * 其余所有视觉都由 PopTokens 提供 —— 换主题等于换一个 accent，页面代码零改动。
 */
val LocalAccent = staticCompositionLocalOf { PopColor.Core.DefaultAccent }
val LocalOnAccent = staticCompositionLocalOf { Color.Black }

/**
 * 移植自 `utils/colorUtils.ts`：加权亮度 > 128 判为浅色。
 * 必须与 Web 版同一套判定，否则同一个主题色在两端会出现"一边黑字一边白字"。
 */
fun isLightAccent(color: Color): Boolean {
    val brightness = (color.red * 255f * 299f + color.green * 255f * 587f + color.blue * 255f * 114f) / 1000f
    return brightness > 128f
}

val accent: Color
    @Composable get() = LocalAccent.current

val onAccent: Color
    @Composable get() = LocalOnAccent.current

@Composable
fun PopTheme(
    accent: Color = PopColor.Core.DefaultAccent,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAccent provides accent,
        LocalOnAccent provides if (isLightAccent(accent)) Color.Black else Color.White,
        content = content,
    )
}

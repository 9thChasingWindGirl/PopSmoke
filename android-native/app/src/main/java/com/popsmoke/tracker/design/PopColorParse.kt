package com.popsmoke.tracker.design

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color

/** 把持久化用的 "#RRGGBB" 转成 Compose 颜色；解析失败回落默认主题色，不抛异常。 */
fun String.toPopColor(fallback: Color = PopColor.Core.DefaultAccent): Color =
    parseHexColor(this) ?: fallback

/**
 * 严格解析 "#RRGGBB" 或 "RRGGBB"，非法输入返回 null。
 *
 * 单独写一个而不是用 android.graphics.Color.parseColor：后者对非法输入**抛异常**，
 * 而取色器每次按键都会试解析一次，用 try/catch 控制流程既慢又容易漏。
 */
fun parseHexColor(input: String): Color? {
    val s = input.trim().removePrefix("#")
    if (s.length != 6) return null
    val v = s.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or v)
}


fun Color.toHexString(): String = String.format("#%06X", 0xFFFFFF and android.graphics.Color.rgb(
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
))

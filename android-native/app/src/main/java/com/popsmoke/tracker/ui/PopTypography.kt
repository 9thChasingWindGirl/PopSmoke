package com.popsmoke.tracker.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.popsmoke.tracker.R

/**
 * display = Bangers，body = HarmonyOS Sans。
 *
 * 两个必须知道的点：
 * 1. Bangers 是单字重的展示字体。这里把 Normal/Bold/Black 全部指向同一份文件，
 *    避免请求 900 时被系统替换或合成出失真的加粗 —— Web 版靠浏览器合成，
 *    原生侧要的是同一副字形。
 * 2. 中文字形不打包进包体（SC 字体单份 8MB）。Bangers / HarmonyOS Sans 都没有 CJK 字形，
 *    Android 的字形级兜底会自动落到系统 Noto Sans CJK —— 与 Web 版用的 Noto Sans SC 同源，
 *    所以中文观感是一致的，包体却少了 24MB。
 */
val PopDisplayFont = FontFamily(
    Font(R.font.bangers, FontWeight.Normal),
    Font(R.font.bangers, FontWeight.Bold),
    Font(R.font.bangers, FontWeight.Black),
)

val PopBodyFont = FontFamily(
    Font(R.font.harmonyos_sans_regular, FontWeight.Normal),
    Font(R.font.harmonyos_sans_bold, FontWeight.Bold),
    Font(R.font.harmonyos_sans_black, FontWeight.Black),
)

package com.popsmoke.tracker.data

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/** 记录所属的自然日（本地时区）。 */
fun SmokeLog.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

fun SmokeLog.isSameDayAs(otherMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
    localDate(zone) == Instant.ofEpochMilli(otherMillis).atZone(zone).toLocalDate()

fun startOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
    date.atStartOfDay(zone).toInstant().toEpochMilli()

/**
 * 时间显示：en 用 12 小时制带 AM/PM（对齐 toLocaleTimeString 在 en-US 下的 "03:05 PM"），
 * 其余语言用 24 小时制。避免用系统 locale 猜，否则同一份数据在不同设备上格式会跳。
 */
fun formatClock(timestamp: Long, language: String, zone: ZoneId = ZoneId.systemDefault()): String {
    val date = Date(timestamp)
    return if (language == "en") {
        SimpleDateFormat("hh:mm a", Locale.US).format(date)
    } else {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }
}

/** 月份标题：「2026年11月」/「November 2026」。 */
fun formatMonthTitle(year: Int, month: Int, language: String): String {
    val pattern = when (language) {
        "zh", "ja" -> "yyyy年M月"
        "ko" -> "yyyy년 M월"
        else -> "MMMM yyyy"
    }
    val locale = localeOf(language)
    val date = LocalDate.of(year, month, 1)
    return java.time.format.DateTimeFormatter.ofPattern(pattern, locale).format(date)
}

/** 日期标题：「2026年11月11日 星期三」/「Wednesday, November 11, 2026」。 */
fun formatDayTitle(date: LocalDate, language: String): String {
    val pattern = when (language) {
        "zh", "ja" -> "yyyy年M月d日 EEEE"
        "ko" -> "yyyy년 M월 d일 EEEE"
        else -> "EEEE, MMMM d, yyyy"
    }
    return java.time.format.DateTimeFormatter.ofPattern(pattern, localeOf(language)).format(date)
}

/** 紧凑日期：列表/区间里用，避免一行塞不下。 */
fun formatShortDate(date: LocalDate, language: String): String = when (language) {
    "zh" -> "${date.monthValue}月${date.dayOfMonth}日"
    "ja" -> "${date.monthValue}月${date.dayOfMonth}日"
    "ko" -> "${date.monthValue}월 ${date.dayOfMonth}일"
    else -> java.time.format.DateTimeFormatter.ofPattern("MMM d", Locale.US).format(date)
}

/**
 * 语言 → Locale。
 *
 * 不直接用 `Locale.getDefault()`：设备的系统语言和 App 内选择的语言是两件事，
 * 用系统 locale 会让"App 里切了语言但日期没变"，这种 bug 在截图对比里很难看出来。
 */
fun localeOf(language: String): Locale = when (language) {
    "zh" -> Locale.SIMPLIFIED_CHINESE
    "ja" -> Locale.JAPANESE
    "ko" -> Locale.KOREAN
    else -> Locale.US
}

/** 时段划分与 Web 版 PopDashboard 的 getLabel/getIcon 完全一致（分钟参与判定）。 */enum class DayPart(val icon: String, val labelKey: String) {
    LateNight("🌙", "lateNight"),
    EarlyMorning("☀", "earlyMorning"),
    Morning("🌤", "morning"),
    Noon("🌞", "noon"),
    Afternoon("🌅", "afternoon"),
    Evening("🌆", "evening"),
}

fun dayPartOf(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): DayPart {
    val time = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime()
    val hour = time.hour
    val minute = time.minute
    return when {
        (hour >= 23 && minute >= 30) || hour < 6 -> DayPart.LateNight
        hour < 8 -> DayPart.EarlyMorning
        hour < 12 -> DayPart.Morning
        hour < 14 -> DayPart.Noon
        hour < 18 -> DayPart.Afternoon
        else -> DayPart.Evening
    }
}

package com.popsmoke.tracker.feature.stats

import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.data.localDate
import com.popsmoke.tracker.data.startOfDayMillis
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** 同一天的所有记录。 */
data class DayGroup(
    val date: LocalDate,
    val logs: List<SmokeLog>,
) {
    val count: Int get() = logs.size
}

/** 同一个月的所有记录，天内按日期倒序。 */
data class MonthGroup(
    val year: Int,
    val month: Int,
    val days: List<DayGroup>,
) {
    val count: Int get() = days.sumOf { it.count }
}

/** 历史筛选条件。null 表示不限。 */
data class HistoryFilter(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val minCount: Int? = null,
    val maxCount: Int? = null,
) {
    val isActive: Boolean
        get() = startDate != null || endDate != null || minCount != null || maxCount != null
}

enum class HistorySortField { Date, Count }

data class HistorySort(
    val field: HistorySortField = HistorySortField.Date,
    val ascending: Boolean = false,
)

/** 历史页的派生结果。一次算完，页面只负责画。 */
data class HistoryView(
    val months: List<MonthGroup>,
    val totalRecords: Int,
    val filteredRecords: Int,
    val averagePerDay: Double,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
)

/**
 * 按「月 → 日」两级分组，并套用筛选与排序。
 *
 * 口径与 Web 版 PopHistory 的 groupedByMonth 一致：
 * 先按日聚合成 count，再用 count 做 min/max 筛选（而不是按单条记录筛），
 * 最后按月汇总。这个顺序不能反 —— 反了的话"每天至少 5 支"会把记录拆散。
 */
fun buildHistoryView(
    logs: List<SmokeLog>,
    filter: HistoryFilter = HistoryFilter(),
    sort: HistorySort = HistorySort(),
    zone: ZoneId = ZoneId.systemDefault(),
): HistoryView {
    val byDate = logs.groupBy { it.localDate(zone) }

    var days = byDate.map { (date, dayLogs) ->
        DayGroup(date = date, logs = dayLogs.sortedByDescending { it.timestamp })
    }

    filter.startDate?.let { start -> days = days.filter { it.date >= start } }
    filter.endDate?.let { end -> days = days.filter { it.date <= end } }
    filter.minCount?.let { min -> days = days.filter { it.count >= min } }
    filter.maxCount?.let { max -> days = days.filter { it.count <= max } }

    days = when (sort.field) {
        HistorySortField.Date -> if (sort.ascending) days.sortedBy { it.date } else days.sortedByDescending { it.date }
        HistorySortField.Count -> if (sort.ascending) days.sortedBy { it.count } else days.sortedByDescending { it.count }
    }

    var months = days
        .groupBy { it.date.year to it.date.monthValue }
        .map { (key, monthDays) ->
            MonthGroup(
                year = key.first,
                month = key.second,
                // 月内始终按日期倒序：月份排序可以变，日期顺序不该跟着乱
                days = monthDays.sortedByDescending { it.date },
            )
        }

    months = when (sort.field) {
        HistorySortField.Count -> if (sort.ascending) months.sortedBy { it.count } else months.sortedByDescending { it.count }
        HistorySortField.Date -> if (sort.ascending) {
            months.sortedWith(compareBy({ it.year }, { it.month }))
        } else {
            months.sortedWith(compareByDescending<MonthGroup> { it.year }.thenByDescending { it.month })
        }
    }

    val filteredRecords = days.sumOf { it.count }
    val totalDays = days.size
    val allDates = days.map { it.date }

    return HistoryView(
        months = months,
        totalRecords = logs.size,
        filteredRecords = filteredRecords,
        averagePerDay = if (totalDays > 0) {
            (filteredRecords.toDouble() / totalDays * 10).roundToInt() / 10.0
        } else {
            0.0
        },
        firstDate = allDates.minOrNull(),
        lastDate = allDates.maxOrNull(),
    )
}

/** 一张柱：日期 + 支数。 */
data class DailyBar(val date: LocalDate, val count: Int)

data class WeekView(
    val bars: List<DailyBar>,
    val total: Int,
    val peakDate: LocalDate?,
    val peakCount: Int,
)

/**
 * 本周（周一起算）。
 *
 * 与 Web 版一致：周一为一周起点，周日为终点；没有记录的日子也要出柱子（高度 0），
 * 否则横轴会跳日，看起来像漏数据。
 */
fun buildWeekView(
    logs: List<SmokeLog>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): WeekView {
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val bars = (0..6).map { offset ->
        val date = monday.plusDays(offset.toLong())
        val start = startOfDayMillis(date, zone)
        val end = startOfDayMillis(date.plusDays(1), zone)
        DailyBar(date, logs.count { it.timestamp in start until end })
    }
    val peak = bars.maxByOrNull { it.count }
    return WeekView(
        bars = bars,
        total = bars.sumOf { it.count },
        peakDate = peak?.takeIf { it.count > 0 }?.date,
        peakCount = peak?.count ?: 0,
    )
}

data class MonthView(
    val bars: List<DailyBar>,
    val total: Int,
    val previousTotal: Int,
    val peakDate: LocalDate?,
    val peakCount: Int,
)

/**
 * 某个月（默认当月，只统计到今天为止）。
 *
 * "只到今天"这一条很重要：不是当月时算整月，是当月时算到当天，
 * 否则横轴会伸出一段全是 0 的未来日期，把柱子压扁。
 */
fun buildMonthView(
    logs: List<SmokeLog>,
    month: LocalDate = LocalDate.now(),
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): MonthView {
    val first = month.withDayOfMonth(1)
    val isCurrentMonth = first.year == today.year && first.monthValue == today.monthValue
    val daysInMonth = first.lengthOfMonth()
    val lastDay = if (isCurrentMonth) today.dayOfMonth else daysInMonth

    val bars = (1..lastDay).map { day ->
        val date = first.withDayOfMonth(day)
        val start = startOfDayMillis(date, zone)
        val end = startOfDayMillis(date.plusDays(1), zone)
        DailyBar(date, logs.count { it.timestamp in start until end })
    }

    val prevFirst = first.minusMonths(1)
    val prevStart = startOfDayMillis(prevFirst.withDayOfMonth(1), zone)
    val prevEnd = startOfDayMillis(first, zone)
    val previousTotal = logs.count { it.timestamp in prevStart until prevEnd }

    val peak = bars.maxByOrNull { it.count }
    return MonthView(
        bars = bars,
        total = bars.sumOf { it.count },
        previousTotal = previousTotal,
        peakDate = peak?.takeIf { it.count > 0 }?.date,
        peakCount = peak?.count ?: 0,
    )
}

/** CSV 导出：每行一天，后面跟当天每支的时间点，最多 20 列（与 Web 版同格式）。 */
fun buildCsv(logs: List<SmokeLog>, year: Int, month: Int, zone: ZoneId = ZoneId.systemDefault()): String {
    val byDate = logs
        .filter { it.localDate(zone).year == year && it.localDate(zone).monthValue == month }
        .groupBy { it.localDate(zone) }
        .toSortedMap()

    if (byDate.isEmpty()) return ""

    val header = (listOf("Date") + (1..20).map { it.toString() }).joinToString(",")
    val rows = byDate.map { (date, dayLogs) ->
        val times = dayLogs
            .sortedBy { it.timestamp }
            .take(20)
            .map {
                val time = java.time.Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalTime()
                String.format("%02d:%02d", time.hour, time.minute)
            }
        val dateText = String.format("%04d/%02d/%02d", date.year, date.monthValue, date.dayOfMonth)
        (listOf(dateText) + times).joinToString(",")
    }
    return (listOf(header) + rows).joinToString("\n") + "\n"
}

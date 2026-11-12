package com.popsmoke.tracker.data.cloud

import com.popsmoke.tracker.data.SmokeLog
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * `smoke_logs` / 飞书中继 两条通道共用的行格式。
 *
 * 原生本地只存 `timestamp`，但远端表结构沿用了 Web 版的 `record_date + record_time + record_index`，
 * 所以每次上行都要把时间戳展开成这三列。放在一处而不是各写一遍：
 * 一旦两边的时区或格式串不一致，同一条记录在两张表里就会对不上。
 */
internal val LogDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
internal val LogTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun SmokeLog.toRow(userId: String, zone: ZoneId): JSONObject {
    val dateTime = Instant.ofEpochMilli(timestamp).atZone(zone)
    return JSONObject()
        .put("id", id)
        .put("user_id", userId)
        .put("record_date", dateTime.toLocalDate().format(LogDateFmt))
        .put("record_time", dateTime.toLocalTime().format(LogTimeFmt))
        .put("record_index", recordIndex)
        .put("timestamp", timestamp)
}

/**
 * 行 → 记录。远端允许缺时间戳（手填的多维表格里很常见），这时用日期 + 时间反推。
 * 反推失败就返回 null：宁可少恢复一条，也不能凭空造一条"1970 年"的记录。
 */
internal fun parseLogRow(json: JSONObject, zone: ZoneId, idPrefix: String): SmokeLog? {
    val dateText = json.optString("record_date").ifBlank { json.optString("date") }
    val timeText = json.optString("record_time")
    val timestamp = json.opt("timestamp")?.let { raw ->
        when (raw) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull()
            else -> null
        }
    } ?: parseLogTimestamp(dateText, timeText, zone) ?: return null

    val id = json.optString("id").ifBlank { "$idPrefix-$timestamp" }
    val index = json.opt("record_index")?.let { raw ->
        when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        }
    } ?: 0

    return SmokeLog(id = id, timestamp = timestamp, recordIndex = index.coerceAtLeast(0))
}

internal fun parseLogTimestamp(dateText: String, timeText: String, zone: ZoneId): Long? = runCatching {
    val date = LocalDate.parse(dateText, LogDateFmt)
    val time = runCatching { LocalTime.parse(timeText, LogTimeFmt) }.getOrDefault(LocalTime.MIDNIGHT)
    date.atTime(time).atZone(zone).toInstant().toEpochMilli()
}.getOrNull()

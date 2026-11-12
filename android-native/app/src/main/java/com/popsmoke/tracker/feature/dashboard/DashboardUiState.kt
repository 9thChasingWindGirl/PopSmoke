package com.popsmoke.tracker.feature.dashboard

import androidx.compose.ui.graphics.Color
import com.popsmoke.tracker.data.AppSettings
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.toPopColor

enum class LimitAlert { None, Warning, Reached }

/**
 * 这一屏需要的所有派生数据都在这里算好，Composable 不做业务计算。
 * 口径与 Web 版 PopDashboard 逐条对齐：今日 = 本地自然日；达标 = 当天有记录且不超过每日上限。
 */
data class DashboardUiState(
    val settings: AppSettings = AppSettings(),
    val todayLogs: List<SmokeLog> = emptyList(),
    val todayCount: Int = 0,
    val consecutiveDays: Int = 0,
    val lastRecordClock: String? = null,
    val alert: LimitAlert = LimitAlert.None,
) {
    val accent: Color get() = settings.themeColor.toPopColor()

    /** 仪表色：达上限红、接近上限琥珀、平时用主题色。 */
    val gaugeColor: Color
        get() = when (alert) {
            LimitAlert.Reached -> PopColor.Status.Error
            LimitAlert.Warning -> PopColor.Status.Amber
            LimitAlert.None -> accent
        }
}

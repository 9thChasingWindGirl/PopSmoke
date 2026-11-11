package com.popsmoke.tracker.feature

import com.popsmoke.tracker.data.AppSettings
import com.popsmoke.tracker.data.OpLog
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.feature.cloud.CloudUiState
import com.popsmoke.tracker.feature.dashboard.DashboardUiState

/**
 * 全应用状态。
 *
 * 为什么不是每个页面各持一份：History 和 Analysis 都要"全量记录 + 设置"，
 * 各自去读一遍文件就会出现两个页面数据不同步。这里保持单一来源，
 * 页面按需在自己的 ViewModel 里派生出视图模型（见 feature/stats）。
 *
 * logs 约定为**时间倒序**：所有页面都按这个顺序消费，谁都不再排序。
 */
data class AppUiState(
    val settings: AppSettings = AppSettings(),
    val logs: List<SmokeLog> = emptyList(),
    /** 操作日志按时间正序（终端从上往下读，最新的在最下面）。 */
    val opLogs: List<OpLog> = emptyList(),
    val dashboard: DashboardUiState = DashboardUiState(),
    val cloud: CloudUiState = CloudUiState(),
)

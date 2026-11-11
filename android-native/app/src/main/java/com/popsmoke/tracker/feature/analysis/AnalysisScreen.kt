package com.popsmoke.tracker.feature.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.data.AppSettings
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.data.formatMonthTitle
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.feature.cloud.CloudUiState
import com.popsmoke.tracker.feature.cloud.fill
import com.popsmoke.tracker.feature.stats.buildCsv
import com.popsmoke.tracker.feature.stats.buildMonthView
import com.popsmoke.tracker.feature.stats.buildWeekView
import com.popsmoke.tracker.i18n.PopStrings
import com.popsmoke.tracker.ui.atoms.PopButton
import com.popsmoke.tracker.ui.atoms.PopButtonVariant
import com.popsmoke.tracker.ui.atoms.PopCard
import com.popsmoke.tracker.ui.atoms.PopGroup
import com.popsmoke.tracker.ui.atoms.PopGroupLabel
import com.popsmoke.tracker.ui.atoms.PopPageTitle
import com.popsmoke.tracker.ui.chrome.LocalChromeInsets
import com.popsmoke.tracker.ui.atoms.PopDialog
import com.popsmoke.tracker.ui.atoms.PopOperationLog
import com.popsmoke.tracker.ui.atoms.PopSyncQueue
import com.popsmoke.tracker.ui.atoms.PopStatTile
import com.popsmoke.tracker.ui.atoms.PopToast
import com.popsmoke.tracker.ui.atoms.PopToastKind
import com.popsmoke.tracker.ui.charts.PopBar
import com.popsmoke.tracker.ui.charts.PopBarChart
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder
import java.io.File
import java.time.LocalDate
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.popInkMuted
import com.popsmoke.tracker.design.popInverseInk
import com.popsmoke.tracker.design.popSurface

private enum class ViewMode { Week, Month }

/**
 * 分析页：柱状图 + 统计 + 数据导出。
 *
 * 图表全部自绘（见 PopBarChart），因为第三方库改不动"没有黑描边"这件事。
 *
 * 云端同步这一块刻意做成"明确不可用"：Supabase / 飞书的鉴权、加密、差异合并
 * 属于单独一批（需要网络层 + 加密 + 冲突解决），在没接之前给一个能点的按钮去
 * 假装同步，比不给按钮更糟。所以这里显示"未配置"，并在设置页留出入入口。
 */
@Composable
fun AnalysisScreen(
    logs: List<SmokeLog>,
    settings: AppSettings,
    opLogs: List<com.popsmoke.tracker.data.OpLog>,
    strings: PopStrings,
    onClearOpLogs: () -> Unit,
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    cloud: CloudUiState = CloudUiState(),
    onSyncNow: () -> Unit = {},
    onSyncOneOp: (String) -> Unit = {},
    onRemoveOp: (String) -> Unit = {},
    onClearQueue: () -> Unit = {},
) {
    var mode by remember { mutableStateOf(ViewMode.Week) }
    var cursor by remember { mutableStateOf(LocalDate.now()) }
    var toast by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var clearQueueConfirm by remember { mutableStateOf(false) }
    var removeOpId by remember { mutableStateOf<String?>(null) }

    val today = LocalDate.now()
    val week = remember(logs) { buildWeekView(logs, today) }
    val month = remember(logs, cursor) { buildMonthView(logs, cursor, today) }

    val context = LocalContext.current
    val weekdayLabels = listOf(
        strings.mon, strings.tue, strings.wed, strings.thu, strings.fri, strings.sat, strings.sun,
    )

    val bars = if (mode == ViewMode.Week) {
        week.bars.mapIndexed { index, bar ->
            PopBar(
                label = weekdayLabels[index].take(3).uppercase(),
                value = bar.count,
                highlight = bar.count > settings.dailyLimit,
            )
        }
    } else {
        month.bars.map { bar ->
            PopBar(
                label = bar.date.dayOfMonth.toString(),
                value = bar.count,
                highlight = bar.count > settings.dailyLimit,
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        // 顶栏/底栏是浮层，页面要自己让出这两条的高度（见 PopChrome.ChromeInsets）
        contentPadding = PaddingValues(
            start = PopSpace.S4,
            end = PopSpace.S4,
            top = LocalChromeInsets.current.top + PopSpace.S4,
            bottom = LocalChromeInsets.current.bottom + PopSpace.S6,
        ),
        verticalArrangement = Arrangement.spacedBy(PopSpace.S6),
    ) {
        item { PopPageTitle(strings.analysis) }

        item {
            PopGroup(padding = PaddingValues(PopSpace.S4)) {
                // 图头：标题 + 周/月切换
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PopDisplay(
                        text = if (mode == ViewMode.Week) strings.thisWeek else strings.monthlyFreq,
                        modifier = Modifier.weight(1f),
                        size = PopTypeScale.Lg,
                        weight = FontWeight.Black,
                        uppercase = true,
                        maxLines = 1,
                    )
                    ModeChip(strings.weekData, mode == ViewMode.Week) { mode = ViewMode.Week }
                    Box(Modifier.padding(start = PopSpace.S1))
                    ModeChip(strings.monthData, mode == ViewMode.Month) { mode = ViewMode.Month }
                }

                if (mode == ViewMode.Month) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = PopSpace.S2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonthNavChip("← " + strings.prevMonth) { cursor = cursor.minusMonths(1) }
                        PopDisplay(
                            text = formatMonthTitle(cursor.year, cursor.monthValue, settings.language),
                            modifier = Modifier.weight(1f),
                            size = PopTypeScale.Sm,
                            weight = FontWeight.Black,
                            maxLines = 1,
                        )
                        MonthNavChip(strings.nextMonth + " →") { cursor = cursor.plusMonths(1) }
                    }
                }

                Box(Modifier.height(PopSpace.S4))
                PopBarChart(
                    bars = bars,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    accent = accent,
                    valueLabel = strings.count,
                )
            }
        }

        item {
            Column {
                PopGroupLabel(strings.stats)
                PopGroup {
                Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S2)) {
                    if (mode == ViewMode.Week) {
                        PopStatTile(
                            value = week.total.toString(),
                            caption = strings.weekTotal,
                            valueSize = PopTypeScale.Xxl,
                        )
                        PopStatTile(
                            value = week.peakDate?.let { peak -> weekdayLabels[peak.dayOfWeek.value - 1] } ?: "-",
                            caption = "${strings.mostSmokedDay} (${week.peakCount})",
                            valueSize = PopTypeScale.Xl,
                        )
                    } else {
                        PopStatTile(
                            value = month.total.toString(),
                            caption = strings.monthTotal,
                            valueSize = PopTypeScale.Xxl,
                        )
                        PopStatTile(
                            value = month.previousTotal.toString(),
                            caption = strings.lastMonthTotal,
                            valueSize = PopTypeScale.Xl,
                        )
                        PopStatTile(
                            value = month.peakDate?.let { "${it.dayOfMonth}${strings.day}" } ?: "-",
                            caption = "${strings.mostSmokedDate} (${month.peakCount})",
                            valueSize = PopTypeScale.Xl,
                        )
                    }
                }
                }
            }
        }

        item {
            Column {
                PopGroupLabel(strings.data)
                PopGroup {
                Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S3)) {
                    // 已登录就真的同步（推队列 + 拉云端），没登录才引导去设置页。
                    // 上批这里是个只会跳页的按钮，用户点完以为同步过了，其实什么都没发生。
                    PopButton(
                        text = if (cloud.signedIn) {
                            if (cloud.pendingSync > 0) {
                                "${strings.syncNow} (${cloud.pendingSync})"
                            } else {
                                strings.syncNow
                            }
                        } else {
                            strings.cloudSync
                        },
                        onClick = if (cloud.signedIn) onSyncNow else onGoToSettings,
                        modifier = Modifier.fillMaxWidth(),
                        variant = PopButtonVariant.Secondary,
                        enabled = !cloud.busy,
                    )
                    PopBody(
                        text = when {
                            cloud.busy -> strings.cloudSyncing
                            cloud.signedIn && cloud.hasFailedOp -> strings.syncHasFailedHint
                            cloud.signedIn && cloud.pendingSync > 0 -> strings.syncPendingCount.fill("n" to cloud.pendingSync)
                            cloud.signedIn -> strings.syncReadyHint
                            else -> strings.apiConfigRequired
                        },
                        size = PopTypeScale.Xs,
                        weight = FontWeight.Bold,
                        color = if (cloud.hasFailedOp) PopColor.Status.Error else popInkMuted,
                        maxLines = 3,
                    )

                    PopButton(
                        text = "${strings.exportCSV} (${formatMonthTitle(cursor.year, cursor.monthValue, settings.language)})",
                        onClick = {
                            val csv = buildCsv(logs, cursor.year, cursor.monthValue)
                            toast = if (csv.isEmpty()) {
                                false to strings.noDataToExport
                            } else {
                                val file = File(context.getExternalFilesDir(null), csvName(cursor))
                                runCatching { file.writeText(csv) }
                                    .map { true to "${strings.exportSuccess} · ${file.absolutePath}" }
                                    .getOrElse { false to (it.message ?: strings.exportFailed) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = PopButtonVariant.Secondary,
                    )
                    PopBody(
                        text = strings.csvNote,
                        size = PopTypeScale.Xs,
                        weight = FontWeight.Bold,
                        color = popInkMuted,
                        maxLines = 3,
                    )

                    val current = toast
                    if (current != null) {
                        PopToast(
                            visible = true,
                            title = if (current.first) strings.notificationSuccess else strings.notificationError,
                            message = current.second,
                            kind = if (current.first) PopToastKind.Success else PopToastKind.Error,
                            onClose = { toast = null },
                        )
                    }
                }
                }
            }
        }

        // 同步队列在操作日志之前：队列是"还欠什么"，需要动作；日志是"发生过什么"，只需查阅。
        item {
            Column {
                PopGroupLabel(strings.syncQueue)
                PopGroup {
                PopSyncQueue(
                    ops = cloud.queueOps,
                    strings = strings,
                    busy = cloud.busy,
                    onSyncAll = onSyncNow,
                    onSyncOne = onSyncOneOp,
                    onRemove = { removeOpId = it },
                    onClear = { clearQueueConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                }
            }
        }

        // 操作日志放在最后一屏：它是"查账"用的，不该挡在图表和统计前面。
        // 标题沿用卡片标签样式，面板本身是黑头终端，两层视觉有意撞在一起（Web 版就是这样）。
        item {
            Column {
                PopGroupLabel(strings.operationLog)
                PopGroup {
                    PopOperationLog(
                        logs = opLogs,
                        strings = strings,
                        onClear = onClearOpLogs,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // 清空队列：不可撤销，必须确认。
    PopDialog(
        visible = clearQueueConfirm,
        title = strings.syncClearQueue,
        message = strings.syncClearConfirm,
        onDismiss = { clearQueueConfirm = false },
        actions = {
            PopButton(
                text = strings.cancel,
                onClick = { clearQueueConfirm = false },
                modifier = Modifier.weight(1f),
                variant = PopButtonVariant.Secondary,
            )
            PopButton(
                text = strings.confirm,
                onClick = {
                    onClearQueue()
                    clearQueueConfirm = false
                },
                modifier = Modifier.weight(1f),
                variant = PopButtonVariant.Danger,
            )
        },
    )

    // 移除单条：只是"不再重试这条"，记录本身不动，文案要说清。
    PopDialog(
        visible = removeOpId != null,
        title = strings.syncRemove,
        message = strings.syncRemoveConfirm,
        onDismiss = { removeOpId = null },
        actions = {
            PopButton(
                text = strings.cancel,
                onClick = { removeOpId = null },
                modifier = Modifier.weight(1f),
                variant = PopButtonVariant.Secondary,
            )
            PopButton(
                text = strings.confirm,
                onClick = {
                    removeOpId?.let(onRemoveOp)
                    removeOpId = null
                },
                modifier = Modifier.weight(1f),
                variant = PopButtonVariant.Danger,
            )
        },
    )
}

private fun csvName(cursor: LocalDate): String =
    "smoking_logs_%04d%02d.csv".format(cursor.year, cursor.monthValue)


@Composable
private fun ModeChip(text: String, active: Boolean, onClick: () -> Unit) {
    val current = LocalSkin.current
    val accentColor = accent
    // 玻璃下"选中"不是反色块而是主题色淡染 + 胶囊外形：磨砂面上挖一块纯黑，
    // 会把背面的光全部遮掉，那一小块立刻变成不透的实体
    val shape = if (current.glass) RoundedCornerShape(percent = 50) else RoundedCornerShape(0.dp)
    val fill = when {
        active && current.glass -> accentColor.copy(alpha = 0.22f)
        active -> current.palette.inverseSurface
        else -> current.palette.surface
    }
    val ink = when {
        active && current.glass -> accentColor
        active -> current.palette.inverseInk
        else -> current.palette.ink
    }
    Box(
        Modifier
            .clip(shape)
            .background(fill)
            .then(
                if (current.glass) {
                    Modifier.border(
                        1.dp,
                        if (active) accentColor.copy(alpha = 0.5f) else current.palette.hairline,
                        shape,
                    )
                } else {
                    Modifier.popBorder(PopStroke.Thin, current.palette.ink)
                },
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (current.glass) PopSpace.S3 else PopSpace.S2,
                vertical = PopSpace.S1,
            ),
    ) {
        PopDisplay(
            text = text,
            size = PopTypeScale.Xs,
            weight = FontWeight.Black,
            color = ink,
            uppercase = true,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthNavChip(text: String, onClick: () -> Unit) {
    val current = LocalSkin.current
    val shape = if (current.glass) RoundedCornerShape(percent = 50) else RoundedCornerShape(0.dp)
    Box(
        Modifier
            .clip(shape)
            .background(current.palette.surface)
            .then(
                if (current.glass) {
                    Modifier.border(1.dp, current.palette.hairline, shape)
                } else {
                    Modifier.popBorder(PopStroke.Thin, current.palette.ink)
                },
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (current.glass) PopSpace.S3 else PopSpace.S2,
                vertical = PopSpace.S1,
            ),
    ) {
        PopDisplay(
            text = text,
            size = PopTypeScale.Xs,
            weight = FontWeight.Black,
            maxLines = 1,
        )
    }
}


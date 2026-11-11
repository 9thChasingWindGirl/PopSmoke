package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.data.sync.SyncOp
import com.popsmoke.tracker.data.sync.SyncOpStatus
import com.popsmoke.tracker.data.sync.SyncOpType
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.i18n.PopStrings
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.PopMono
import java.time.Instant
import java.time.ZoneId

private val SyncBodyHeight = 192.dp
private val SyncBadgeMinWidth = 56.dp

/**
 * 同步队列：一块和操作日志同族的"终端"。
 *
 * 它回答的是操作日志回答不了的问题：**哪些改动还没上云**。
 * 操作日志是"发生过什么"，队列是"还欠什么" —— 所以这里每行都带状态（待同步/失败）
 * 和动作（同步/移除），而不是只读。
 *
 * 与 Web 版的两处差异：
 * 1. 每行拆成两行（时间+内容+状态 / 动作）。手机宽度下把五个元素挤在一行，
 *    消息会被压成两三个字，等于没写。
 * 2. Web 用 `text-yellow-600` 这类浅色写在白底上，对比度不足；这里用同色系 600 档。
 */
@Composable
fun PopSyncQueue(
    ops: List<SyncOp>,
    strings: PopStrings,
    busy: Boolean,
    onSyncAll: () -> Unit,
    onSyncOne: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // ---- HEADER ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PopColor.Core.Black)
                .padding(horizontal = PopSpace.S4, vertical = PopSpace.S1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PopDisplay(
                text = strings.syncQueue,
                size = PopTypeScale.Sm,
                weight = FontWeight.Black,
                color = PopColor.Core.White,
                tracking = PopTracking.Wider,
                uppercase = true,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PopSpace.S2)) {
                PopMiniButton(
                    text = if (busy) strings.syncProcessing else strings.syncAll,
                    onClick = onSyncAll,
                    enabled = !busy && ops.isNotEmpty(),
                    color = accent,
                )
                PopMiniButton(
                    text = strings.syncClearQueue,
                    onClick = onClear,
                    enabled = !busy && ops.isNotEmpty(),
                )
            }
        }

        // ---- BODY ----
        // 有内容时是固定高的"窗口"（内部滚动），空态则按内容撑开：
        // 一块 192dp 的空白面板会让人以为列表没加载出来。
        Box(
            Modifier
                .fillMaxWidth()
                .then(if (ops.isEmpty()) Modifier else Modifier.height(SyncBodyHeight))
                .background(PopColor.Core.White),
        ) {
            if (ops.isEmpty()) {
                PopMono(
                    text = strings.syncQueueEmpty,
                    modifier = Modifier.padding(PopSpace.S4),
                    size = PopTypeScale.Sm,
                    color = PopColor.Neutral.Gray500,
                    maxLines = 2,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(SyncBodyHeight),
                    contentPadding = PaddingValues(horizontal = PopSpace.S4, vertical = PopSpace.S2),
                ) {
                    items(items = ops, key = { it.id }) { op ->
                        OpRow(
                            op = op,
                            strings = strings,
                            busy = busy,
                            onSync = { onSyncOne(op.id) },
                            onRemove = { onRemove(op.id) },
                        )
                    }
                }
            }
        }

        // ---- STATUS ----
        if (ops.isEmpty()) {
            // 空态没有状态栏，用一条黑线收口，否则白底会直接消失在画布上。
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(PopStroke.Default)
                    .background(PopColor.Core.Black),
            )
            return@Column
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PopColor.Neutral.Gray100)
                .drawBehind {
                    drawRect(
                        color = PopColor.Core.Black,
                        size = Size(size.width, PopStroke.Default.toPx()),
                    )
                }
                .padding(top = PopStroke.Default)
                .padding(horizontal = PopSpace.S4, vertical = PopSpace.S1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PopMono(
                text = "${ops.size} ${strings.syncEntries}",
                size = PopTypeScale.Xs,
                color = PopColor.Neutral.Gray600,
            )
            val (label, tint) = when {
                busy -> strings.syncProcessing to PopColor.Status.Info
                ops.any { it.status == SyncOpStatus.Failed } -> strings.syncFailedState to PopColor.Status.Error
                else -> strings.syncReady to PopColor.Status.Success
            }
            PopMono(text = label, size = PopTypeScale.Xs, weight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
private fun OpRow(
    op: SyncOp,
    strings: PopStrings,
    busy: Boolean,
    onSync: () -> Unit,
    onRemove: () -> Unit,
) {
    val (prefix, tint) = when (op.type) {
        SyncOpType.Create -> "[+]" to PopColor.Status.Success
        SyncOpType.Update -> "[~]" to PopColor.Status.Info
        SyncOpType.Delete -> "[-]" to PopColor.Status.Error
    }
    val failed = op.status == SyncOpStatus.Failed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = PopColor.Neutral.Gray200,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 1f),
                    size = Size(size.width, 1f),
                )
            }
            .padding(vertical = PopSpace.S1),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PopSpace.S2),
        ) {
            PopMono(
                text = clock(op.enqueuedAt),
                size = PopTypeScale.Xs,
                color = PopColor.Neutral.Gray500,
            )
            PopMono(text = prefix, size = PopTypeScale.Xs, weight = FontWeight.Bold, color = tint)
            PopMono(
                // 队列里的每一条都对应"某天第几支"，所以带序号比带日期更好认。
                text = "${labelOf(op.type, strings)} #${op.recordIndex}",
                modifier = Modifier.weight(1f),
                size = PopTypeScale.Xs,
                color = PopColor.Neutral.Gray700,
                maxLines = 1,
            )
        }

        // 状态与动作放同一行：徽标跟消息挤在一行时，"待同步"三个字会被压到贴着卡片边缘，
        // 看着像描边被裁了。挪下来之后左右各占一端，读起来是"这条什么状态、能做什么"。
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusBadge(text = if (failed) strings.syncFailedState else strings.syncPendingState, failed = failed)
            Box(Modifier.weight(1f))
            PopMiniButton(text = strings.syncOne, onClick = onSync, enabled = !busy, color = accent)
            Box(Modifier.padding(start = PopSpace.S1))
            PopMiniButton(text = strings.syncRemove, onClick = onRemove, enabled = !busy)
        }
    }
}

private fun labelOf(type: SyncOpType, strings: PopStrings): String = when (type) {
    SyncOpType.Create -> strings.opCreate
    SyncOpType.Update -> strings.opUpdate
    SyncOpType.Delete -> strings.opDelete
}

/** 状态徽标：黄底待同步、红底失败。字很小，靠底色而不是字重来区分。 */
@Composable
private fun StatusBadge(text: String, failed: Boolean) {
    Box(
        modifier = Modifier
            .background(if (failed) PopColor.Status.ErrorLight else PopColor.Status.WarningLight)
            .border(PopStroke.Thin, PopColor.Core.Black)
            // 最小宽度是给中文留的："待同步"三个字在 10sp 下几乎顶满，
            // 不留余量时描边会贴着字，看着像被裁掉一截。
            .defaultMinSize(minWidth = SyncBadgeMinWidth)
            .padding(horizontal = PopSpace.S1, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        PopMono(
            text = text,
            // 10sp 的中文在这个尺寸下笔画会糊成一片，"待同步"看着像"诗同步"。
            // 徽标本身有 24dp 高，用 12sp 完全放得下。
            size = PopTypeScale.Sm,
            weight = FontWeight.Bold,
            color = if (failed) PopColor.Status.Error else PopColor.Status.Amber,
            maxLines = 1,
        )
    }
}

private fun clock(timestamp: Long): String {
    val time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

/**
 * 终端条里的小按钮：24dp 高、2dp 描边，与 Web 的 `h-6 text-xs` 同量级。
 * enabled 用透明度而不是换色：这块面板里颜色已经承载了状态，再叠一层灰会让"失败"更难读。
 */
@Composable
fun PopMiniButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = PopColor.Neutral.Gray200,
) {
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .background(color)
            .border(PopStroke.Thin, PopColor.Core.Black)
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = PopSpace.S7)
            .padding(horizontal = PopSpace.S2, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        PopDisplay(
            text = text,
            size = PopTypeScale.Xs,
            weight = FontWeight.Black,
            uppercase = true,
            maxLines = 1,
        )
    }
}

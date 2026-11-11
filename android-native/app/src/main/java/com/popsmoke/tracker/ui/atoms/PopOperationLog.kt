package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.data.OpLog
import com.popsmoke.tracker.data.OpLogType
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.i18n.PopStrings
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.PopMono
import java.time.Instant
import java.time.ZoneId

private val BodyHeight = 192.dp

/**
 * 操作日志：一块"终端"。
 *
 * 它是给愿意翻日志的人看的，所以视觉上刻意和波普卡片区分开：黑标题条 + 等宽字 +
 * 灰色状态栏。三段结构（HEADER / BODY / STATUS）在 Web 版就是这样，
 * 这里逐段对齐，不因为"手机上少见"就简化掉状态栏 —— 那条 LIVE / PAUSED 是这个组件唯一的状态提示。
 *
 * 与 Web 的一处修正：Web 用 `text-green-400` 这类浅色写在白底上，对比度不足 3:1；
 * 这里换成同一色系的 500/600 档（Success / Info / Error / Amber），色相不变、可读性够。
 */
@Composable
fun PopOperationLog(
    logs: List<OpLog>,
    strings: PopStrings,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 暂停状态只属于这块面板：它影响的是"要不要自动跟到最新"，和业务无关。
    var isPaused by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // 新日志进来时自动滚到最新；暂停时不打扰（用户正在往回翻）。
    LaunchedEffect(logs.size, isPaused) {
        if (!isPaused && logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex)
    }

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
                text = strings.operationLog,
                size = PopTypeScale.Sm,
                weight = FontWeight.Black,
                color = PopColor.Core.White,
                tracking = PopTracking.Wider,
                uppercase = true,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PopSpace.S2)) {
                PopMiniButton(
                    text = if (isPaused) strings.opResume else strings.opPause,
                    onClick = { isPaused = !isPaused },
                )
                PopMiniButton(text = strings.opClearLog, onClick = onClear)
            }
        }

        // ---- BODY ----
        Box(
            Modifier
                .fillMaxWidth()
                .height(BodyHeight)
                .background(PopColor.Core.White),
        ) {
            if (logs.isEmpty()) {
                PopMono(
                    text = strings.opEmpty,
                    modifier = Modifier.padding(PopSpace.S4),
                    size = PopTypeScale.Sm,
                    color = PopColor.Neutral.Gray500,
                    maxLines = 2,
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().height(BodyHeight),
                    contentPadding = PaddingValues(horizontal = PopSpace.S4, vertical = PopSpace.S3),
                    verticalArrangement = Arrangement.spacedBy(PopSpace.S1),
                ) {
                    items(items = logs, key = { it.id }) { log ->
                        LogLine(log = log, strings = strings)
                    }
                }
            }
        }

        // ---- STATUS ----
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
                text = "${logs.size} ${strings.opEntries}",
                size = PopTypeScale.Xs,
                color = PopColor.Neutral.Gray600,
            )
            PopMono(
                text = if (isPaused) strings.opPaused else strings.opLive,
                size = PopTypeScale.Xs,
                weight = FontWeight.Bold,
                color = if (isPaused) PopColor.Status.Amber else PopColor.Status.Success,
            )
        }
    }
}

@Composable
private fun LogLine(log: OpLog, strings: PopStrings) {
    val (prefix, tint) = prefixOf(log.type)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(PopSpace.S2),
    ) {
        PopMono(
            text = clockWithSeconds(log.at),
            size = PopTypeScale.Xs,
            color = PopColor.Neutral.Gray500,
        )
        PopMono(text = prefix, size = PopTypeScale.Xs, weight = FontWeight.Bold, color = tint)
        PopMono(
            text = messageOf(log, strings),
            modifier = Modifier.weight(1f),
            size = PopTypeScale.Xs,
            color = PopColor.Neutral.Gray700,
        )
    }
}

private fun prefixOf(type: OpLogType): Pair<String, Color> = when (type) {
    OpLogType.Create -> "[+]" to PopColor.Status.Success
    OpLogType.Update -> "[~]" to PopColor.Status.Info
    OpLogType.Delete -> "[-]" to PopColor.Status.Error
    OpLogType.Clear -> "[×]" to PopColor.Status.Amber
    OpLogType.Sync -> "[↓]" to PopColor.Theme.Purple
    OpLogType.System -> "[·]" to PopColor.Neutral.Gray500
}

/**
 * 消息在渲染时拼，而不是写入时拼：切了语言，旧日志也跟着变。
 * 记录类日志带当天序号，批量类日志带条数 —— 这两类信息足够说明发生了什么，
 * 再多的字段（日期、完整时间）在 192dp 的小窗里也只是噪音。
 */
private fun messageOf(log: OpLog, strings: PopStrings): String {
    val label = when (log.type) {
        OpLogType.Create -> strings.opCreate
        OpLogType.Update -> strings.opUpdate
        OpLogType.Delete -> strings.opDelete
        OpLogType.Clear -> strings.opClear
        OpLogType.Sync -> strings.opSync
        OpLogType.System -> strings.opSystem
    }
    return when {
        log.recordIndex != null -> "$label #${log.recordIndex}"
        log.count != null -> "$label (${log.count})"
        else -> label
    }
}

private fun clockWithSeconds(timestamp: Long): String {
    val time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d:%02d".format(time.hour, time.minute, time.second)
}

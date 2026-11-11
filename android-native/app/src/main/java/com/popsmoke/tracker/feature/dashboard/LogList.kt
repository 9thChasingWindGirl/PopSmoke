package com.popsmoke.tracker.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.data.dayPartOf
import com.popsmoke.tracker.data.formatClock
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.i18n.PopStrings
import com.popsmoke.tracker.i18n.dayPartLabel
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.PopIcon
import com.popsmoke.tracker.ui.foundation.PopIconKind
import com.popsmoke.tracker.ui.foundation.PopTapTarget
import com.popsmoke.tracker.ui.foundation.popShadow
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.popInkMuted
import com.popsmoke.tracker.design.popInkSecondary
import com.popsmoke.tracker.design.popSurface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.ui.foundation.skinSurface

/**
 * 今日记录列表。注意两处与 Web 的差异，都是原生该做对的事：
 * 1. 删除键视觉 24.5dp，但触摸目标是 48dp（Web 版只有 24.5px，手指很难点准）
 * 2. 列表用 LazyColumn，滚动到屏幕外不会继续测量 —— Web 是全量渲染
 */
@Composable
fun LogList(
    logs: List<SmokeLog>,
    language: String,
    accent: Color,
    strings: PopStrings,
    onEdit: (SmokeLog) -> Unit,
    onDelete: (SmokeLog) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PopSpace.S3),
        contentPadding = PaddingValues(bottom = PopSpace.S4),
    ) {
        items(items = logs, key = { it.id }) { log ->
            LogRow(
                log = log,
                language = language,
                accent = accent,
                strings = strings,
                onEdit = { onEdit(log) },
                onDelete = { onDelete(log) },
            )
        }
    }
}

@Composable
private fun LogRow(
    log: SmokeLog,
    language: String,
    accent: Color,
    strings: PopStrings,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val part = dayPartOf(log.timestamp)
    val current = com.popsmoke.tracker.design.LocalSkin.current
    val rowShape = androidx.compose.foundation.shape.RoundedCornerShape(current.radiusField)
    val blockShape = androidx.compose.foundation.shape.RoundedCornerShape(current.radiusChip * 0.72f)
    // 玻璃下每一行都是一小片玻璃：白色细边 + 柔和落影；波普仍是"纸片 + 硬描边"
    val blockBorder = if (current.glass) {
        1.dp to Color.White.copy(alpha = 0.62f)
    } else {
        PopStroke.Thin to popInk
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (current.glass) {
                    Modifier.skinSurface(current, rowShape, popSurface, lift = 8.dp)
                } else {
                    Modifier
                        .popShadow(PopShadow.ListItem, color = popInk.copy(alpha = 0.1f))
                        .background(popSurface)
                        .popBorder(PopStroke.Thin, popInk)
                },
            )
            .padding(PopSpace.S3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PopSpace.S3),
        ) {
            // 时段图标块：主题色 30/255 透明度铺底（对应 `${themeColor}30`）
            Box(
                modifier = Modifier
                    .size(PopSpace.S10)
                    .clip(blockShape)
                    .background(accent.copy(alpha = if (current.glass) 0.24f else 0.19f))
                    .border(blockBorder.first, blockBorder.second, blockShape),
                contentAlignment = Alignment.Center,
            ) {
                PopBody(part.icon, size = PopTypeScale.Lg, weight = FontWeight.Normal)
            }
            Column {
                PopDisplay(
                    text = strings.dayPartLabel(part.labelKey),
                    size = PopTypeScale.Sm,
                    weight = FontWeight.Bold,
                    uppercase = true,
                )
                PopBody(
                    text = formatClock(log.timestamp, language),
                    size = PopTypeScale.Xs,
                    weight = FontWeight.Normal,
                    color = popInkMuted,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            PopDisplay(
                text = "#${log.recordIndex}",
                size = PopTypeScale.Base,
                weight = FontWeight.Bold,
                color = popInkSecondary,
            )
            // 编辑键：Web 是 28px 的黄底铅笔，这里视觉同为 24.5dp、触摸补到 48dp。
            // 两个小键之间不再加间距 —— 48dp 触摸区本身已经有足够的间隔。
            PopTapTarget(onClick = onEdit) {
                Box(
                    modifier = Modifier
                        .size(PopSpace.S7)
                        .clip(blockShape)
                        .background(PopColor.Neutral.Yellow200.copy(alpha = if (current.glass) 0.9f else 1f))
                        .border(blockBorder.first, blockBorder.second, blockShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PopIcon(PopIconKind.Pencil, size = PopSpace.S4)
                }
            }
            PopTapTarget(onClick = onDelete) {
                Box(
                    modifier = Modifier
                        .size(PopSpace.S7)
                        .clip(blockShape)
                        .background(PopColor.Neutral.Red200.copy(alpha = if (current.glass) 0.9f else 1f))
                        .border(blockBorder.first, blockBorder.second, blockShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PopIcon(PopIconKind.Trash, size = PopSpace.S4)
                }
            }
        }
    }
}

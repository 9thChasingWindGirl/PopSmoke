package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopLayout
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.i18n.PopStrings
import com.popsmoke.tracker.ui.foundation.PopDisplay
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.popInkMuted
import com.popsmoke.tracker.design.popSurface
import com.popsmoke.tracker.design.popInverseInk

/**
 * 时/分两列滚轮。
 *
 * 与 Web 版的差别只在手感：Web 用 `scroll-snap` + 点击跳转，原生用 SnapFlingBehavior，
 * 甩一下会自己停在整行上。选中判定取「离视口中心最近的那一行」，而不是 firstVisibleItemIndex ——
 * 后者在惯性滚动中途会跳变，数字会闪。
 *
 * 上下各留 (128-40)/2 = 44dp 的内边距，选中行才可能停在正中；这也是 Web 版那两个
 * 40px 占位块的等价物。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopTimeWheel(
    hour: Int,
    minute: Int,
    hourLabel: String,
    minuteLabel: String,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(PopSpace.S4)) {
            WheelLabel(hourLabel)
            WheelLabel(minuteLabel)
        }

        Box(Modifier.padding(top = PopSpace.S1)) {
            // 选中高亮条：横贯两列（含中间的冒号），画在滚轮底下。
            // 宽度取 matchParentSize —— 也就是整行滚轮的实际宽度，而不是外层可用宽度：
            // 用 fillMaxWidth 会一路铺到弹窗边缘，右端比「分」列多出一截，视觉上像放歪了。
            val wheelInk = popInk
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val row = PopLayout.TimeWheelRow.toPx()
                        val top = (size.height - row) / 2f
                        drawRect(
                            color = PopColor.Status.WarningLight.copy(alpha = 0.5f),
                            topLeft = Offset(0f, top),
                            size = Size(size.width, row),
                        )
                        val line = PopStroke.Thin.toPx()
                        val ink = wheelInk.copy(alpha = 0.5f)
                        drawRect(ink, topLeft = Offset(0f, top), size = Size(size.width, line))
                        drawRect(
                            ink,
                            topLeft = Offset(0f, top + row - line),
                            size = Size(size.width, line),
                        )
                    },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PopSpace.S4),
            ) {
                Wheel(count = 24, selected = hour, onSelected = onHourChange)
                PopDisplay(
                    text = ":",
                    size = PopTypeScale.Xxxl,
                    weight = FontWeight.Bold,
                )
                Wheel(count = 60, selected = minute, onSelected = onMinuteChange)
            }
        }
    }
}

@Composable
private fun WheelLabel(text: String) {
    PopDisplay(
        text = text,
        modifier = Modifier.width(PopLayout.TimeWheelColumn),
        size = PopTypeScale.Xs,
        weight = FontWeight.Black,
        textAlign = TextAlign.Center,
        uppercase = true,
        maxLines = 1,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Wheel(
    count: Int,
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val scope = rememberCoroutineScope()

    // 打开时直接落在当前值上；scrollToItem 会把该项顶到内容区起点，
    // 加上 44dp 的内边距，正好落在视口中心。
    LaunchedEffect(Unit) { state.scrollToItem(selected) }

    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2 - viewportCenter) }
                ?.index
                ?: selected
        }
    }

    LaunchedEffect(centered) {
        if (centered != selected) onSelected(centered)
    }

    LazyColumn(
        modifier = Modifier
            .width(PopLayout.TimeWheelColumn)
            .height(PopLayout.TimeWheelHeight)
            .background(popSurface)
            // 定死尺寸的盒子用 border 而不是 popBorder：描边在尺寸之内（等价 Web 的 border-box），
            // 行高才刚好等于吸附步长，选中行不会和视觉中线错开。
            // 所以这里只能手动跟随主题换色，不能换成 skinSurface（那会加 padding，打乱吸附）。
            .border(
                width = if (LocalSkin.current.glass) 1.dp else PopStroke.Thin,
                color = if (LocalSkin.current.glass) LocalSkin.current.palette.hairline else popInk,
            ),
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = (PopLayout.TimeWheelHeight - PopLayout.TimeWheelRow) / 2),
    ) {
        items(count) { index ->
            WheelRow(
                value = index,
                selected = index == centered,
                // 点一行只负责"滚过去"，选中值由滚动位置统一推导（见上面的 centered）。
                // 如果这里直接 onSelected，高亮跟着滚动中心走、数字却立刻变了，
                // 会出现"值已改、选中行还在原处"的错位。
                onClick = { scope.launch { state.animateScrollToItem(index) } },
            )
        }
    }
}

@Composable
private fun WheelRow(value: Int, selected: Boolean, onClick: () -> Unit) {
    val current = LocalSkin.current
    val accentColor = accent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PopLayout.TimeWheelRow)
            .then(
                if (selected) {
                    // 玻璃下选中行是主题色淡染，不是反色块：滚轮里出现一块纯黑，
                    // 会像一扇开着的窗，把后面的光全挡住了
                    Modifier.background(
                        if (current.glass) accentColor.copy(alpha = 0.18f) else current.palette.inverseSurface,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PopDisplay(
            text = value.toString().padStart(2, '0'),
            size = PopTypeScale.Xl,
            weight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = when {
                !selected -> popInkMuted
                current.glass -> accentColor
                else -> current.palette.inverseInk
            },
            maxLines = 1,
        )
    }
}

/** 供对话框用：把时间戳拆成时/分，保存时再拼回去（秒与毫秒归零，与 Web 一致）。 */
@Composable
fun PopTimePickerDialog(
    visible: Boolean,
    initialTimestamp: Long,
    strings: PopStrings,
    onCancel: () -> Unit,
    onSave: (Long) -> Unit,
) {
    if (!visible) return
    val zone = remember { ZoneId.systemDefault() }
    val initial = remember(initialTimestamp) {
        Instant.ofEpochMilli(initialTimestamp).atZone(zone)
    }
    var hour by remember(initialTimestamp) { mutableIntStateOf(initial.hour) }
    var minute by remember(initialTimestamp) { mutableIntStateOf(initial.minute) }

    PopDialog(
        visible = true,
        title = strings.editTime,
        onDismiss = onCancel,
        content = {
            PopTimeWheel(
                hour = hour,
                minute = minute,
                hourLabel = strings.hour,
                minuteLabel = strings.minute,
                onHourChange = { hour = it },
                onMinuteChange = { minute = it },
                modifier = Modifier.fillMaxWidth().padding(top = PopSpace.S2),
            )
        },
        actions = {
            PopButton(
                text = strings.cancel,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                variant = PopButtonVariant.Secondary,
            )
            PopButton(
                text = strings.confirm,
                onClick = {
                    val next = initial
                        .withHour(hour)
                        .withMinute(minute)
                        .withSecond(0)
                        .withNano(0)
                        .toInstant()
                        .toEpochMilli()
                    onSave(next)
                },
                modifier = Modifier.weight(1f),
            )
        },
    )
}

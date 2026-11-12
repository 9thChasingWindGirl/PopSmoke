package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.data.formatMonthTitle
import com.popsmoke.tracker.data.formatShortDate
import com.popsmoke.tracker.data.localeOf
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.popInkMuted
import com.popsmoke.tracker.design.popSurface

/**
 * 日期选择：字段 + 月历弹窗。
 *
 * 这里没有照搬 Web 的滚轮（PopDatePicker 是三列滚轮），原因很直接：
 * 选一个筛选区间要翻年月日三列、还要等惯性吸附，日历上一点就到了。
 * 视觉仍是同一套：7 列方格、2dp 黑边、选中项反色、今天加一个方框。
 *
 * 返回 null 表示"不限制"——筛选区间必须能清空，否则设了就再也去不掉。
 */
@Composable
fun PopDatePickerField(
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    placeholder: String,
    language: String,
    modifier: Modifier = Modifier,
    clearText: String = "×",
    confirmText: String = "OK",
    cancelText: String = "CANCEL",
) {
    var open by remember { mutableStateOf(false) }
    val current = LocalSkin.current
    val fieldShape = RoundedCornerShape(if (current.glass) current.radiusField else 0.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(fieldShape)
            .then(
                if (current.glass) {
                    Modifier
                        .background(current.palette.surfaceDeep)
                        .border(1.dp, current.palette.hairline, fieldShape)
                } else {
                    Modifier.background(popSurface).popBorder(PopStroke.Default, popInk)
                },
            )
            .clickable { open = true }
            .padding(horizontal = PopSpace.S3, vertical = PopSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PopDisplay(
            text = value?.let { formatShortDate(it, language) } ?: placeholder,
            modifier = Modifier.weight(1f),
            size = PopTypeScale.Base,
            weight = FontWeight.Bold,
            color = if (value == null) popInkMuted else popInk,
            maxLines = 1,
        )
        if (value != null) {
            Box(
                Modifier
                    .size(PopSpace.S6)
                    .clickable {
                        onChange(null)
                    },
                contentAlignment = Alignment.Center,
            ) {
                PopDisplay(text = clearText, size = PopTypeScale.Sm, weight = FontWeight.Black)
            }
        }
    }

    if (open) {
        // 用真正的 Dialog 窗口，而不是在当前布局里铺一层 fillMaxSize：
        // 这个字段通常放在卡片内部，布局层里的遮罩会被卡片宽度裁住，盖不满屏幕。
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { open = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            CalendarDialog(
                initial = value ?: LocalDate.now(),
                language = language,
                confirmText = confirmText,
                cancelText = cancelText,
                onDismiss = { open = false },
                onPick = {
                    onChange(it)
                    open = false
                },
            )
        }
    }
}

@Composable
private fun CalendarDialog(
    initial: LocalDate,
    language: String,
    confirmText: String,
    cancelText: String,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    var cursor by remember { mutableStateOf(initial.withDayOfMonth(1)) }
    var selected by remember { mutableStateOf(initial) }

    PopDialog(
        visible = true,
        title = formatMonthTitle(cursor.year, cursor.monthValue, language),
        onDismiss = onDismiss,
        actions = {
            PopButton(
                text = cancelText,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                variant = PopButtonVariant.Secondary,
            )
            PopButton(
                text = confirmText,
                onClick = { onPick(selected) },
                modifier = Modifier.weight(1f),
            )
        },
        content = {
        // 月份切换：只在弹窗内部改游标，不动当前选中值
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = PopSpace.S2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavSquare("‹") { cursor = cursor.minusMonths(1) }
            PopDisplay(
                text = formatMonthTitle(cursor.year, cursor.monthValue, language),
                modifier = Modifier.weight(1f),
                size = PopTypeScale.Base,
                weight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            NavSquare("›") { cursor = cursor.plusMonths(1) }
        }

        // 星期表头：用 CLDR 的窄名（一到日 / M T W ...），不硬编码
        val weekdayFormat = remember(language) {
            DateTimeFormatter.ofPattern("EEEEE", localeOf(language))
        }
        Row(Modifier.fillMaxWidth()) {
            (0..6).forEach { index ->
                val sample = LocalDate.of(2024, 1, 1).plusDays(index.toLong()) // 2024-01-01 是周一
                PopDisplay(
                    text = weekdayFormat.format(sample),
                    modifier = Modifier.weight(1f),
                    size = PopTypeScale.Xs,
                    weight = FontWeight.Black,
                    color = popInkMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }

        // 日期网格：周一为首列，与 buildWeekView 的口径一致
        val firstDay = cursor.withDayOfMonth(1)
        val leading = firstDay.dayOfWeek.value - 1
        val days = cursor.lengthOfMonth()
        val cells = leading + days
        val rows = (cells + 6) / 7

        Column(Modifier.padding(top = PopSpace.S1)) {
            repeat(rows) { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    repeat(7) { col ->
                        val cellIndex = row * 7 + col
                        val day = cellIndex - leading + 1
                        if (day in 1..days) {
                            DayCell(
                                day = day,
                                selected = cursor.withDayOfMonth(day) == selected,
                                isToday = cursor.withDayOfMonth(day) == today,
                                modifier = Modifier.weight(1f),
                                onClick = { selected = cursor.withDayOfMonth(day) },
                            )
                        } else {
                            Box(Modifier.weight(1f).size(PopSpace.S10))
                        }
                    }
                }
            }
        }
        },
    )
}

@Composable
private fun DayCell(
    day: Int,
    selected: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val current = LocalSkin.current
    val cellShape = RoundedCornerShape(if (current.glass) current.radiusChip else 0.dp)
    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .size(PopSpace.S10)
            .clip(cellShape)
            .background(
                when {
                    selected -> accent
                    current.glass -> Color.Transparent
                    else -> popSurface
                },
            )
            .then(
                if (current.glass) {
                    Modifier.border(
                        1.dp,
                        if (isToday) accent.copy(alpha = 0.85f) else Color.Transparent,
                        cellShape,
                    )
                } else {
                    Modifier.border(
                        PopStroke.Thin,
                        if (isToday || selected) popInk else PopColor.Neutral.Gray300,
                    )
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PopDisplay(
            text = day.toString(),
            size = PopTypeScale.Sm,
            weight = if (selected || isToday) FontWeight.Black else FontWeight.Bold,
            color = if (selected) com.popsmoke.tracker.design.onAccent else popInk,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun NavSquare(glyph: String, onClick: () -> Unit) {
    val current = LocalSkin.current
    val shape = if (current.glass) CircleShape else RectangleShape
    Box(
        Modifier
            .size(PopSpace.S10)
            .clip(shape)
            .background(if (current.glass) current.palette.surfaceDeep else popSurface)
            .then(
                if (current.glass) {
                    Modifier.border(1.dp, current.palette.hairline, shape)
                } else {
                    Modifier.border(PopStroke.Thin, popInk)
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PopDisplay(text = glyph, size = PopTypeScale.Lg, weight = FontWeight.Black)
    }
}

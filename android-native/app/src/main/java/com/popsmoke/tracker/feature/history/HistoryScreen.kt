package com.popsmoke.tracker.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.data.AppSettings
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.data.formatClock
import com.popsmoke.tracker.data.formatDayTitle
import com.popsmoke.tracker.data.formatMonthTitle
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.design.onAccent
import com.popsmoke.tracker.feature.stats.DayGroup
import com.popsmoke.tracker.feature.stats.HistoryFilter
import com.popsmoke.tracker.feature.stats.HistorySort
import com.popsmoke.tracker.feature.stats.HistorySortField
import com.popsmoke.tracker.feature.stats.HistoryView
import com.popsmoke.tracker.feature.stats.MonthGroup
import com.popsmoke.tracker.feature.stats.buildHistoryView
import com.popsmoke.tracker.i18n.PopStrings
import com.popsmoke.tracker.ui.atoms.PopButton
import com.popsmoke.tracker.ui.atoms.PopButtonVariant
import com.popsmoke.tracker.ui.atoms.PopCard
import com.popsmoke.tracker.ui.atoms.PopDatePickerField
import com.popsmoke.tracker.ui.atoms.PopInput
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.popShadow

/**
 * 历史页：筛选 + 「月 → 日 → 单条」三级展开 + 统计。
 *
 * 与原版的一处刻意差异：原版排序选中态靠放大 5% + 加粗（`scale-105 font-bold`），
 * 手机上几乎看不出差别。这里保留"再点一次反向"的语义，但选中态改用主题色反色底板，
 * 截图对比时"哪个在生效"必须一眼可见。
 */
@Composable
fun HistoryScreen(
    logs: List<SmokeLog>,
    settings: AppSettings,
    strings: PopStrings,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(HistoryFilter()) }
    var sort by remember { mutableStateOf(HistorySort()) }
    var expandedMonth by remember { mutableStateOf<String?>(null) }
    var expandedDay by remember { mutableStateOf<String?>(null) }

    val view = remember(logs, filter, sort) {
        buildHistoryView(logs = logs, filter = filter, sort = sort)
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = PopSpace.S4,
            end = PopSpace.S4,
            top = PopSpace.S4,
            bottom = PopSpace.S8,
        ),
        verticalArrangement = Arrangement.spacedBy(PopSpace.S6),
    ) {
        item { HistoryHeader(strings = strings, total = logs.size) }

        item {
            FilterCard(
                filter = filter,
                sort = sort,
                strings = strings,
                language = settings.language,
                onFilterChange = { filter = it },
                onSortChange = { sort = it },
                onReset = {
                    filter = HistoryFilter()
                    sort = HistorySort()
                },
            )
        }

        if (view.months.isEmpty()) {
            item {
                PopCard(modifier = Modifier.fillMaxWidth()) {
                    PopBody(
                        text = if (filter.isActive) strings.noMatchingRecords else strings.noRecords,
                        modifier = Modifier.fillMaxWidth(),
                        size = PopTypeScale.Base,
                        weight = FontWeight.Bold,
                        color = PopColor.Neutral.Gray700,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                    )
                }
            }
        } else {
            items(items = view.months, key = { "${it.year}-${it.month}" }) { month ->
                val key = "${month.year}-${month.month}"
                MonthCard(
                    month = month,
                    language = settings.language,
                    strings = strings,
                    expanded = expandedMonth == key,
                    expandedDay = expandedDay,
                    onToggleMonth = {
                        expandedMonth = if (expandedMonth == key) null else key
                        expandedDay = null
                    },
                    onToggleDay = { date ->
                        val tag = date.toString()
                        expandedDay = if (expandedDay == tag) null else tag
                    },
                )
            }
        }

        item { StatsCard(view = view, strings = strings) }
    }
}

@Composable
private fun HistoryHeader(strings: PopStrings, total: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PopDisplay(
            text = strings.history,
            size = PopTypeScale.Xxxl,
            weight = FontWeight.Black,
            maxLines = 1,
        )
        Box(Modifier.size(PopSpace.S2))
        Row(
            Modifier
                .graphicsLayer { rotationZ = 1f }
                .background(PopColor.Core.White)
                .popBorder(PopStroke.Thin, PopColor.Core.Black)
                .padding(horizontal = PopSpace.S3, vertical = PopSpace.S1),
        ) {
            PopBody(
                text = "${strings.totalCigarettes}: $total",
                size = PopTypeScale.Sm,
                weight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FilterCard(
    filter: HistoryFilter,
    sort: HistorySort,
    strings: PopStrings,
    language: String,
    onFilterChange: (HistoryFilter) -> Unit,
    onSortChange: (HistorySort) -> Unit,
    onReset: () -> Unit,
) {
    PopCard(modifier = Modifier.fillMaxWidth(), title = strings.filtersAndSort) {
        Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S2)) {
            FieldLabel(strings.startDate)
            PopDatePickerField(
                value = filter.startDate,
                onChange = { onFilterChange(filter.copy(startDate = it)) },
                placeholder = strings.selectStartDate,
                language = language,
                confirmText = strings.confirm,
                cancelText = strings.cancel,
            )

            FieldLabel(strings.endDate)
            PopDatePickerField(
                value = filter.endDate,
                onChange = { onFilterChange(filter.copy(endDate = it)) },
                placeholder = strings.selectEndDate,
                language = language,
                confirmText = strings.confirm,
                cancelText = strings.cancel,
            )

            Row(
                modifier = Modifier.padding(top = PopSpace.S1),
                horizontalArrangement = Arrangement.spacedBy(PopSpace.S3),
            ) {
                Column(Modifier.weight(1f)) {
                    FieldLabel(strings.minCount)
                    PopInput(
                        value = filter.minCount?.toString() ?: "",
                        onValueChange = { onFilterChange(filter.copy(minCount = it.toIntOrNull())) },
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(strings.maxCount)
                    PopInput(
                        value = filter.maxCount?.toString() ?: "",
                        onValueChange = { onFilterChange(filter.copy(maxCount = it.toIntOrNull())) },
                        placeholder = "99",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }

            Box(Modifier.size(PopSpace.S2))
            SortButton(
                text = strings.sortByDate,
                active = sort.field == HistorySortField.Date,
                arrow = if (sort.field == HistorySortField.Date) (if (sort.ascending) " ↑" else " ↓") else "",
                onClick = {
                    onSortChange(
                        HistorySort(
                            field = HistorySortField.Date,
                            ascending = sort.field == HistorySortField.Date && !sort.ascending,
                        ),
                    )
                },
            )
            SortButton(
                text = strings.sortByCount,
                active = sort.field == HistorySortField.Count,
                arrow = if (sort.field == HistorySortField.Count) (if (sort.ascending) " ↑" else " ↓") else "",
                onClick = {
                    onSortChange(
                        HistorySort(
                            field = HistorySortField.Count,
                            ascending = sort.field == HistorySortField.Count && !sort.ascending,
                        ),
                    )
                },
            )
            PopButton(
                text = strings.resetFilters,
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                variant = PopButtonVariant.Danger,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    PopDisplay(
        text = text,
        modifier = Modifier.padding(bottom = PopSpace.S1),
        size = PopTypeScale.Sm,
        weight = FontWeight.Black,
        maxLines = 1,
    )
}

@Composable
private fun SortButton(text: String, active: Boolean, arrow: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .popShadow(PopShadow.Pop)
            .background(if (active) accent else PopColor.Neutral.Gray200)
            .popBorder(PopStroke.Default, PopColor.Core.Black)
            .clickable(onClick = onClick)
            .padding(vertical = PopSpace.S3),
        contentAlignment = Alignment.Center,
    ) {
        PopDisplay(
            text = text + arrow,
            size = PopTypeScale.Sm,
            weight = FontWeight.Black,
            color = if (active) onAccent else PopColor.Core.Black,
            uppercase = true,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthCard(
    month: MonthGroup,
    language: String,
    strings: PopStrings,
    expanded: Boolean,
    expandedDay: String?,
    onToggleMonth: () -> Unit,
    onToggleDay: (java.time.LocalDate) -> Unit,
) {
    PopCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(0.dp),
    ) {
        // 月标题：灰底 + 底部 2dp 分隔线
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PopColor.Neutral.Gray100)
                .clickable(onClick = onToggleMonth)
                .padding(PopSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.graphicsLayer { rotationZ = if (expanded) 90f else 0f },
            ) {
                PopDisplay(
                    text = "▶",
                    size = PopTypeScale.Sm,
                    weight = FontWeight.Black,
                    color = PopColor.Neutral.Gray600,
                )
            }
            Box(Modifier.size(PopSpace.S2))
            PopDisplay(
                text = formatMonthTitle(month.year, month.month, language),
                modifier = Modifier.weight(1f),
                size = PopTypeScale.Lg,
                weight = FontWeight.Black,
                maxLines = 1,
            )
            Box(
                Modifier
                    .graphicsLayer { rotationZ = -2f }
                    .background(accent)
                    .popBorder(PopStroke.Thin, PopColor.Core.Black)
                    .padding(horizontal = PopSpace.S2, vertical = PopSpace.S1),
            ) {
                PopDisplay(
                    text = month.count.toString(),
                    size = PopTypeScale.Base,
                    weight = FontWeight.Black,
                    color = onAccent,
                    maxLines = 1,
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PopColor.Core.White)
                    .padding(PopSpace.S3),
                verticalArrangement = Arrangement.spacedBy(PopSpace.S2),
            ) {
                month.days.forEach { day ->
                    DayRow(
                        day = day,
                        language = language,
                        expanded = expandedDay == day.date.toString(),
                        onToggle = { onToggleDay(day.date) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    day: DayGroup,
    language: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .popShadow(PopShadow.ListItem, color = PopColor.Core.Black.copy(alpha = 0.12f))
                .background(PopColor.Neutral.Gray100)
                // Web 是 border-l-4：只有左边一条粗黑，不是四边
                .drawLeftBar(PopStroke.Default)
                .clickable(onClick = onToggle)
                .padding(PopSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.graphicsLayer { rotationZ = if (expanded) 90f else 0f }) {
                PopDisplay(
                    text = "▶",
                    size = PopTypeScale.Xs,
                    weight = FontWeight.Black,
                    color = PopColor.Neutral.Gray600,
                )
            }
            Box(Modifier.size(PopSpace.S2))
            PopDisplay(
                text = formatDayTitle(day.date, language),
                modifier = Modifier.weight(1f),
                size = PopTypeScale.Base,
                weight = FontWeight.Black,
                maxLines = 1,
            )
            Box(
                Modifier
                    .graphicsLayer { rotationZ = 1f }
                    .background(PopColor.Neutral.Yellow300)
                    .popBorder(PopStroke.Thin, PopColor.Core.Black)
                    .padding(horizontal = PopSpace.S2, vertical = PopSpace.S1),
            ) {
                PopDisplay(
                    text = day.count.toString(),
                    size = PopTypeScale.Sm,
                    weight = FontWeight.Black,
                    maxLines = 1,
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = PopSpace.S4),
                verticalArrangement = Arrangement.spacedBy(PopSpace.S1),
            ) {
                day.logs.forEach { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PopColor.Neutral.Gray100.copy(alpha = 0.6f))
                            .drawLeftBar(PopStroke.Thin)
                            .padding(PopSpace.S2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PopDisplay(
                            text = formatClock(log.timestamp, language),
                            modifier = Modifier.weight(1f),
                            size = PopTypeScale.Base,
                            weight = FontWeight.Black,
                            maxLines = 1,
                        )
                        PopBody(
                            text = "ID: ${log.id.take(8)}…",
                            size = PopTypeScale.Xs,
                            weight = FontWeight.Bold,
                            color = PopColor.Neutral.Gray500,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** 左侧竖条：对应 CSS 的 `border-l-4` / `border-l-2`。 */
private fun Modifier.drawLeftBar(width: Dp): Modifier =
    this.drawBehind {
        val w = width.toPx()
        drawRect(color = PopColor.Core.Black, size = Size(w, size.height))
    }.padding(start = width)

@Composable
private fun StatsCard(view: HistoryView, strings: PopStrings) {
    PopCard(modifier = Modifier.fillMaxWidth(), title = strings.stats) {
        Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(PopSpace.S3)) {
                StatCell(
                    label = strings.totalRecords,
                    value = view.totalRecords.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    label = strings.filteredRecords,
                    value = view.filteredRecords.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PopSpace.S3)) {
                StatCell(
                    label = strings.dateRange,
                    value = if (view.firstDate != null && view.lastDate != null) {
                        "${view.firstDate} → ${view.lastDate}"
                    } else {
                        "-"
                    },
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    label = strings.averagePerDay,
                    value = view.averagePerDay.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        PopBody(
            text = label,
            size = PopTypeScale.Sm,
            weight = FontWeight.Bold,
            color = PopColor.Neutral.Gray600,
            maxLines = 2,
        )
        PopDisplay(
            text = value,
            size = PopTypeScale.Lg,
            weight = FontWeight.Black,
            maxLines = 1,
        )
    }
}


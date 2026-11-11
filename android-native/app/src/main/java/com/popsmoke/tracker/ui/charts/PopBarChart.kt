package com.popsmoke.tracker.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.PopBodyFont
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.popShadow

/**
 * 柱状图，全部自绘。
 *
 * 为什么不用第三方图表库：它们的默认视觉（细线、浅灰网格、圆角柔和柱）与波普风冲突，
 * 而能改的只有颜色，改不动"没有 2dp 黑描边"这件事。自绘反而能把 4dp 轮廓带进图表里，
 * 让图表和卡片是同一套语言。数据量是个人记录级别（最多 31 根柱），性能不是问题。
 *
 * 交互只做一件事：点某根柱显示数值，再点别处取消。Web 版是 hover tooltip，
 * 手机没有 hover，触摸即"选中的那一根"，语义等价。
 */
data class PopBar(
    val label: String,
    val value: Int,
    val highlight: Boolean = false,
)

@Composable
fun PopBarChart(
    bars: List<PopBar>,
    modifier: Modifier = Modifier,
    accent: Color,
    overLimitColor: Color = PopColor.Status.Danger,
    valueLabel: String = "",
    maxLabels: Int = 8,
) {
    val measurer = rememberTextMeasurer()
    var selected by remember(bars) { mutableStateOf<Int?>(null) }

    val yMax = (bars.maxOfOrNull { it.value } ?: 0).coerceAtLeast(4)
    // Y 轴取整到 2 的倍数，刻度才是整数；柱子矮的时候也不至于顶格
    val axisTop = ((yMax + 1) / 2) * 2

    val labelStyle = TextStyle(
        fontFamily = PopBodyFont,
        fontSize = PopTypeScale.Xs,
        fontWeight = FontWeight.Bold,
        color = PopColor.Neutral.Gray600,
    )
    val axisStyle = TextStyle(
        fontFamily = PopBodyFont,
        fontSize = PopTypeScale.Xs,
        fontWeight = FontWeight.Bold,
        color = PopColor.Neutral.Gray500,
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val yAxisWidth = 26.dp
        val xAxisHeight = 26.dp
        // 每根柱的可用宽度；柱子占 62%，其余留白，柱间距才接近 Web 的 barCategoryGap 20%
        val plotWidth = maxWidth - yAxisWidth
        val plotHeight = maxHeight - xAxisHeight

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(bars) {
                    detectTapGestures { offset ->
                        val slot = plotWidth.toPx() / bars.size.coerceAtLeast(1)
                        val idx = ((offset.x - yAxisWidth.toPx()) / slot).toInt()
                        selected = if (idx in bars.indices) {
                            if (selected == idx) null else idx
                        } else {
                            null
                        }
                    }
                },
        ) {
            val axisW = PopStroke.Thin.toPx()
            val top = 0f
            val bottom = plotHeight.toPx()
            val left = yAxisWidth.toPx()

            // Y 轴刻度：0 / 中值 / 顶值，横线用浅灰
            val ticks = listOf(0, axisTop / 2, axisTop)
            ticks.forEach { t ->
                val y = bottom - (t.toFloat() / axisTop) * (bottom - top)
                drawLine(
                    color = PopColor.Neutral.Gray200,
                    start = Offset(left, y),
                    end = Offset(size.width, y),
                    strokeWidth = axisW * 0.5f,
                )
                val text = measurer.measure(t.toString(), axisStyle)
                drawText(
                    textLayoutResult = text,
                    topLeft = Offset(
                        x = left - text.size.width - PopSpace.S1.toPx(),
                        y = y - text.size.height / 2f,
                    ),
                )
            }

            // 坐标轴
            drawLine(
                color = PopColor.Core.Black,
                start = Offset(left, top),
                end = Offset(left, bottom),
                strokeWidth = axisW,
            )
            drawLine(
                color = PopColor.Core.Black,
                start = Offset(left, bottom),
                end = Offset(size.width, bottom),
                strokeWidth = axisW,
            )

            // 柱
            val slot = (size.width - left) / bars.size.coerceAtLeast(1)
            val barWidth = slot * 0.62f
            bars.forEachIndexed { index, bar ->
                val cx = left + slot * (index + 0.5f)
                if (bar.value > 0) {
                    val h = (bar.value.toFloat() / axisTop) * (bottom - top)
                    val fill = if (bar.highlight) overLimitColor else accent
                    val dimmed = selected != null && selected != index
                    drawRect(
                        color = if (dimmed) fill.copy(alpha = 0.45f) else fill,
                        topLeft = Offset(cx - barWidth / 2f, bottom - h),
                        size = Size(barWidth, h),
                    )
                    // 描边：2dp 纯黑，圆角在柱顶只给 2dp（对应 Web 的 radius 2）
                    drawRoundRect(
                        color = PopColor.Core.Black,
                        topLeft = Offset(cx - barWidth / 2f, bottom - h),
                        size = Size(barWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                        style = Stroke(width = axisW),
                    )
                }

                // X 轴标签：按 maxLabels 抽稀，避免 31 天时挤成一团
                val step = if (bars.size <= maxLabels) 1 else ceilDiv(bars.size, maxLabels)
                val showLabel = index % step == 0 || index == bars.size - 1
                if (showLabel) {
                    val text = measurer.measure(bar.label, labelStyle)
                    drawText(
                        textLayoutResult = text,
                        topLeft = Offset(
                            x = cx - text.size.width / 2f,
                            y = bottom + PopSpace.S1.toPx(),
                        ),
                    )
                }
            }
        }

        // 选中提示：贴在图上方的浮条，不做跟随动画（跟手反而更晃）
        val selectedIndex = selected
        if (selectedIndex != null && selectedIndex in bars.indices) {
            val bar = bars[selectedIndex]
            Box(Modifier.align(Alignment.TopEnd)) {
                SelectionBubble(
                    label = bar.label,
                    value = bar.value,
                    caption = valueLabel,
                )
            }
        }
    }
}

private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b

@Composable
private fun SelectionBubble(label: String, value: Int, caption: String) {
    Column(
        Modifier
            .background(PopColor.Core.White)
            .popBorder(PopStroke.Thin, PopColor.Core.Black)
            .popShadow(PopShadow.Pop)
            .padding(PopSpace.S2),
    ) {
        PopDisplay(
            text = label,
            size = PopTypeScale.Sm,
            weight = FontWeight.Black,
            maxLines = 1,
        )
        PopDisplay(
            text = if (caption.isEmpty()) value.toString() else "$caption $value",
            size = PopTypeScale.Base,
            weight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

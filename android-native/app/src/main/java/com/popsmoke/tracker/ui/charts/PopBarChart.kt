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
import androidx.compose.ui.graphics.Brush
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
import com.popsmoke.tracker.ui.foundation.chipShape
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.popShadow
import com.popsmoke.tracker.ui.foundation.skinSurface
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.popHairline
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.popInkMuted
import com.popsmoke.tracker.design.popInkSecondary
import com.popsmoke.tracker.design.popSurface

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
    val current = LocalSkin.current

    val yMax = (bars.maxOfOrNull { it.value } ?: 0).coerceAtLeast(4)
    // Y 轴取整到 2 的倍数，刻度才是整数；柱子矮的时候也不至于顶格
    val axisTop = ((yMax + 1) / 2) * 2

    val labelStyle = TextStyle(
        fontFamily = PopBodyFont,
        fontSize = PopTypeScale.Xs,
        fontWeight = FontWeight.Bold,
        color = popInkSecondary,
    )
    val axisStyle = TextStyle(
        fontFamily = PopBodyFont,
        fontSize = PopTypeScale.Xs,
        fontWeight = FontWeight.Bold,
        color = popInkMuted,
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val yAxisWidth = 26.dp
        val xAxisHeight = 26.dp
        // 每根柱的可用宽度；柱子占 62%，其余留白，柱间距才接近 Web 的 barCategoryGap 20%
        val plotWidth = maxWidth - yAxisWidth
        val plotHeight = maxHeight - xAxisHeight

        // Canvas 的 lambda 是绘制阶段，读不到 CompositionLocal
        val gridLine = popHairline
        val axisInk = popInk
        val glass = current.glass
        // 玻璃下坐标轴不能是纯黑实线：整张图会像被相框框住。退成一条极淡的 ink，
        // 仍然比网格线重一点，读者才知道"这是轴"而不是"这是又一条网格"
        val axisLine = if (glass) axisInk.copy(alpha = 0.16f) else axisInk
        val barRadius = if (glass) 6.dp else 2.dp
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
                    color = gridLine,
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
                color = axisLine,
                start = Offset(left, top),
                end = Offset(left, bottom),
                strokeWidth = if (glass) axisW * 0.6f else axisW,
            )
            drawLine(
                color = axisLine,
                start = Offset(left, bottom),
                end = Offset(size.width, bottom),
                strokeWidth = if (glass) axisW * 0.6f else axisW,
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
                    if (glass) {
                        // 玻璃柱：竖向渐变 + 大圆角 + 顶部一条白高光。
                        // 没有描边 —— 磨砂语言里"边缘"由高光和投影说明，不由黑线说明
                        val alpha = if (dimmed) 0.45f else 1f
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    fill.copy(alpha = 0.95f * alpha),
                                    fill.copy(alpha = 0.55f * alpha),
                                ),
                                startY = bottom - h,
                                endY = bottom,
                            ),
                            topLeft = Offset(cx - barWidth / 2f, bottom - h),
                            size = Size(barWidth, h),
                            cornerRadius = CornerRadius(barRadius.toPx()),
                        )
                        if (h > barRadius.toPx() * 1.6f) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.35f * alpha),
                                start = Offset(cx - barWidth / 2f + barRadius.toPx(), bottom - h + 1.5.dp.toPx()),
                                end = Offset(cx + barWidth / 2f - barRadius.toPx(), bottom - h + 1.5.dp.toPx()),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                    } else {
                        drawRect(
                            color = if (dimmed) fill.copy(alpha = 0.45f) else fill,
                            topLeft = Offset(cx - barWidth / 2f, bottom - h),
                            size = Size(barWidth, h),
                        )
                        // 描边：2dp 纯黑，圆角在柱顶只给 2dp（对应 Web 的 radius 2）
                        drawRoundRect(
                            color = axisInk,
                            topLeft = Offset(cx - barWidth / 2f, bottom - h),
                            size = Size(barWidth, h),
                            cornerRadius = CornerRadius(barRadius.toPx()),
                            style = Stroke(width = axisW),
                        )
                    }
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
    val current = LocalSkin.current
    Column(
        Modifier
            .then(
                if (current.glass) {
                    // 提示条要压在图表上方，所以用 surfaceStrong 而不是 surface：
                    // 半透明白叠在半透明白柱子上会读不清数字
                    Modifier.skinSurface(
                        current = current,
                        shape = current.chipShape(),
                        fill = current.palette.surfaceStrong,
                        lift = 10.dp,
                    )
                } else {
                    Modifier
                        .background(popSurface)
                        .popBorder(PopStroke.Thin, popInk)
                        .popShadow(PopShadow.Pop)
                },
            )
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

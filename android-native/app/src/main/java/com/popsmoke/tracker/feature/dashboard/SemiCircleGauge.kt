package com.popsmoke.tracker.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopMotion
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.design.popHairline
import com.popsmoke.tracker.design.popInkMuted

/**
 * 半环仪表。这是 L3（平台无对应）组件里最典型的一个，必须自绘。
 *
 * 几何直接照抄 Web 版 SVG：150 见方的圆弧、15 的描边、圆头端点、容器高 105（半圆 75 + 30 给数字）。
 * 进度动画 1000ms ease-out，和 Web 的 `transition-all duration-1000 ease-out` 同参数。
 *
 * 一处刻意的偏离：Web 给进度弧加了 `drop-shadow(0 4px 8px color/50)`（模糊阴影）。
 * 这里换成偏移 4dp、透明度 0.28 的硬阴影 —— 整站视觉规则是"零模糊"，
 * 单独一个模糊阴影会像贴上去的，硬偏移反而和卡片/按钮的阴影语言一致。
 */
@Composable
fun SemiCircleGauge(
    value: Int,
    max: Int,
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    val target = if (max <= 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = PopMotion.Gauge, easing = PopMotion.EaseOutCurve),
        label = "gauge-progress",
    )

    val gaugeSize = 150.dp
    val strokeWidth = 15.dp
    val containerHeight = 105.dp

    // 玻璃下轨道要更淡：中灰弧线压在磨砂面上会像一道划痕
    val trackColor = if (com.popsmoke.tracker.design.LocalSkin.current.glass) {
        popInk.copy(alpha = 0.07f)
    } else {
        popHairline
    }
    Box(modifier.width(gaugeSize).height(containerHeight)) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val radius = (gaugeSize.toPx() - sw) / 2f
            val arcTopLeft = Offset(sw / 2f, sw / 2f)
            val arcSize = Size(radius * 2f, radius * 2f)
            val arcStroke = Stroke(width = sw, cap = StrokeCap.Round)

            // 底槽
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = arcStroke,
            )

            if (progress > 0f) {
                // 硬偏移阴影
                translate(top = 4.dp.toPx()) {
                    drawArc(
                        color = color.copy(alpha = 0.28f),
                        startAngle = 180f,
                        sweepAngle = 180f * progress,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = arcStroke,
                    )
                }
                // 进度弧
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f * progress,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = arcStroke,
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PopDisplay(
                text = value.toString(),
                size = PopTypeScale.Gauge,
                weight = FontWeight.Black,
                color = color,
            )
            PopBody(
                text = label,
                size = PopTypeScale.Xs,
                weight = FontWeight.Bold,
                color = popInkMuted,
                tracking = PopTracking.Wider,
                uppercase = true,
            )
        }
    }
}

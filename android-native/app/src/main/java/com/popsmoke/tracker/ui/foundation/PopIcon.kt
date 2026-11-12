package com.popsmoke.tracker.ui.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.skin

enum class PopIconKind { Menu, Close, Plus, Trash, Pencil, Flame, Chart, Clock, Plug, Gear }

/**
 * 图标用 Canvas 画，不用矢量资源：
 * Web 版的图标是内联 SVG，strokeWidth 2（加号 3）在 24×24 视口里。
 * 直接按视口坐标缩放，尺度感才能和文本对齐；换成 drawable 反而要在 XML 和代码之间对两遍数。
 */
@Composable
fun PopIcon(
    kind: PopIconKind,
    size: Dp,
    color: Color = Color.Unspecified,
    strokeUnits: Float = 2f,
) {
    // 与文字同一套回落：玻璃主题的墨色是 #16161A，图标用纯黑会显得比旁边文字更重
    val actual = if (color == Color.Unspecified) skin.palette.ink else color
    Canvas(Modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        val w = strokeUnits * unit
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(actual, p(x1, y1), p(x2, y2), strokeWidth = w, cap = StrokeCap.Round)

        when (kind) {
            PopIconKind.Menu -> {
                line(4f, 6f, 20f, 6f)
                line(4f, 12f, 20f, 12f)
                line(4f, 18f, 20f, 18f)
            }

            PopIconKind.Close -> {
                line(6f, 6f, 18f, 18f)
                line(18f, 6f, 6f, 18f)
            }

            PopIconKind.Plus -> {
                line(12f, 4f, 12f, 20f)
                line(4f, 12f, 20f, 12f)
            }

            PopIconKind.Trash -> {
                line(4f, 7f, 20f, 7f)
                line(9f, 7f, 9f, 4f)
                line(9f, 4f, 15f, 4f)
                line(15f, 4f, 15f, 7f)
                line(6f, 7f, 7.2f, 20f)
                line(7.2f, 20f, 16.8f, 20f)
                line(16.8f, 20f, 18f, 7f)
                line(10f, 11f, 10f, 17f)
                line(14f, 11f, 14f, 17f)
            }

            // 铅笔：Web 版是 heroicons 的曲线路径，14dp 下曲线看不出差别，
            // 这里用「笔杆两条平行线 + 笔帽 + 笔尖」的直笔形，像素落点更干净。
            PopIconKind.Pencil -> {
                line(15.5f, 3.5f, 20.5f, 8.5f)
                line(15.5f, 3.5f, 4.5f, 14.5f)
                line(20.5f, 8.5f, 9.5f, 19.5f)
                line(4.5f, 14.5f, 9.5f, 19.5f)
                line(4.5f, 14.5f, 3f, 21f)
                line(3f, 21f, 9.5f, 19.5f)
            }

            // ---- 底栏五个页签 ----
            // 底栏图标只有 18–20dp，任何一条多余曲线都会糊成噪点，
            // 所以全部用「最少的笔画 + 大半径」：火苗一个轮廓、分析三根柱、
            // 历史一个圆两根指针、接口两个针脚、设置一个圆八根齿。
            PopIconKind.Flame -> {
                // 火苗必须是曲线，直线折角的火苗读作水滴。
                // 外轮廓一条路径闭合，内芯一根小弧 —— 19dp 下刚好还能分辨是火。
                drawPath(
                    path = Path().apply {
                        moveTo(12f * unit, 3f * unit)
                        cubicTo(15.4f * unit, 7.2f * unit, 18.6f * unit, 9.4f * unit, 18.6f * unit, 13.6f * unit)
                        cubicTo(18.6f * unit, 17.6f * unit, 15.6f * unit, 20.6f * unit, 12f * unit, 20.6f * unit)
                        cubicTo(8.4f * unit, 20.6f * unit, 5.4f * unit, 17.6f * unit, 5.4f * unit, 13.6f * unit)
                        cubicTo(5.4f * unit, 10.4f * unit, 7.6f * unit, 8.6f * unit, 9.2f * unit, 5.2f * unit)
                        close()
                    },
                    color = actual,
                    style = Stroke(width = w, cap = StrokeCap.Round),
                )
            }

            PopIconKind.Chart -> {
                line(5f, 20f, 20f, 20f)
                line(7.5f, 20f, 7.5f, 12.5f)
                line(12f, 20f, 12f, 6.5f)
                line(16.5f, 20f, 16.5f, 9.5f)
            }

            PopIconKind.Clock -> {
                drawCircle(actual, 9f * unit, p(12f, 12f), style = Stroke(width = w))
                line(12f, 7f, 12f, 12.2f)
                line(12f, 12.2f, 15.6f, 14f)
            }

            PopIconKind.Plug -> {
                line(9f, 3.5f, 9f, 7.5f)
                line(15f, 3.5f, 15f, 7.5f)
                drawRoundRect(
                    color = actual,
                    topLeft = p(6f, 7.5f),
                    size = Size(12f * unit, 7f * unit),
                    cornerRadius = CornerRadius(2.4f * unit, 2.4f * unit),
                    style = Stroke(width = w),
                )
                line(12f, 14.5f, 12f, 20.5f)
            }

            PopIconKind.Gear -> {
                drawCircle(actual, 4f * unit, p(12f, 12f), style = Stroke(width = w))
                // 八根齿：极坐标算，避免手写八个 cos/sin 常量对不齐
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val c = kotlin.math.cos(a).toFloat()
                    val s = kotlin.math.sin(a).toFloat()
                    line(12f + c * 7f, 12f + s * 7f, 12f + c * 9.6f, 12f + s * 9.6f)
                }
            }
        }
    }
}

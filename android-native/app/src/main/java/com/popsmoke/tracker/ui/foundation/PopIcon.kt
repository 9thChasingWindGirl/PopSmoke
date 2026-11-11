package com.popsmoke.tracker.ui.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.skin

enum class PopIconKind { Menu, Close, Plus, Trash, Pencil }

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
        }
    }
}

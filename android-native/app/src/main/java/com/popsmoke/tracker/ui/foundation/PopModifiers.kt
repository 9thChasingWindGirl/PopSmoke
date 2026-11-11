package com.popsmoke.tracker.ui.foundation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.LocalSkin

/**
 * 描边：**占尺寸**的边，语义等同 CSS 的 `border` + `box-sizing: border-box`。
 *
 * 这是原生侧最容易悄悄缩水的一处。Compose 的 `Modifier.border` 只画线、不参与测量，
 * 描边是压在 padding 上的；CSS 的 border 是盒子的一部分，内容和 padding 都往里让。
 * 结果：每个"内容撑开 + 有描边"的组件都会比 Web 版小 2×描边宽 ——
 * 卡片少 8dp、贴纸少 4dp、按钮少 8dp，单看都像"差一点"，叠一屏就成了位置偏移。
 *
 * 主题感知：波普主题画 4dp 纯黑；液态玻璃主题换成 1dp 的极淡 hairline ——
 * 磨砂面上压一条粗黑框会立刻回到波普味，两套语言的差别就在这里。
 * 用 composed 读皮肤，是为了让已经写在各页面里的调用点不必逐个改签名。
 */
fun Modifier.popBorder(
    width: Dp,
    color: Color = Color.Black,
    shape: Shape = RectangleShape,
): Modifier = composed {
    val current = LocalSkin.current
    if (current.glass) {
        val hairline = 1.dp
        border(hairline, current.palette.hairline, shape).padding(hairline)
    } else {
        border(width, color, shape).padding(width)
    }
}

/**
 * 硬阴影：在内容背后画一层同形状的纯色实心块，按偏移量平移，零模糊。
 *
 * 为什么波普不用 Modifier.shadow：那是模糊投影，一用立刻变 Material 味。
 * 这里的四条规则（见 docs/NATIVE-REWRITE-PLAN.md 第三节）：
 *   1. 零模糊，边缘必须是硬边
 *   2. 偏移量恒定，不随内容尺寸变化
 *   3. 按下时由调用方同步缩小偏移量，元素与阴影一起动
 *   4. 圆角形状的阴影必须与内容用同一个 Shape，否则圆角对不上
 *
 * 液态玻璃主题下"硬阴影"这个概念不存在，退化成柔和的两层扩散投影。
 * 保留这个函数名是为了让已有调用点零改动地跟着换主题。
 */
fun Modifier.popShadow(
    offset: Dp,
    color: Color = Color.Black,
    shape: Shape = RectangleShape,
): Modifier = composed {
    val current = LocalSkin.current
    if (current.glass) {
        softShadow(lift = offset, shape = shape, color = current.palette.ink)
    } else {
        drawBehind {
            val d = offset.toPx()
            if (d <= 0f) return@drawBehind

            translate(left = d, top = d) {
                when (val outline = shape.createOutline(size, layoutDirection, this)) {
                    is Outline.Rectangle -> drawRect(
                        color = color,
                        topLeft = outline.rect.topLeft,
                        size = outline.rect.size,
                    )

                    is Outline.Rounded -> {
                        val rect = outline.roundRect
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(rect.width, rect.height),
                            cornerRadius = rect.topLeftCornerRadius,
                        )
                    }

                    is Outline.Generic -> drawPath(outline.path, color)
                }
            }
        }
    }
}

/**
 * 柔和投影：复用 PopSurface 里的两层层叠实现，避免两处各写一份导致观感不一致。
 */
fun Modifier.softShadow(
    lift: Dp,
    shape: Shape,
    color: Color = Color.Black,
): Modifier = drawBehind {
    drawSoftShadow(shape.createOutline(size, layoutDirection, this), color, lift)
}

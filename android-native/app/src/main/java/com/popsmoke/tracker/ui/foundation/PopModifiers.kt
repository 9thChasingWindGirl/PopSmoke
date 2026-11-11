package com.popsmoke.tracker.ui.foundation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

/**
 * 波普描边：**占尺寸**的黑边，语义等同 CSS 的 `border` + `box-sizing: border-box`。
 *
 * 这是原生侧最容易悄悄缩水的一处。Compose 的 `Modifier.border` 只画线、不参与测量，
 * 描边是压在 padding 上的；CSS 的 border 是盒子的一部分，内容和 padding 都往里让。
 * 结果：每个"内容撑开 + 有描边"的组件都会比 Web 版小 2×描边宽 ——
 * 卡片少 8dp、贴纸少 4dp、按钮少 8dp，单看都像"差一点"，叠一屏就成了位置偏移。
 *
 * 写法：先画边（画在外沿），再让出边的厚度，最后才是真正的 padding。
 * 已经定死尺寸的元素（28×28 的色块、42×42 的图标键）不要用这个 ——
 * 那种情况 CSS 的 border-box 里描边本来就在尺寸之内，用 `border` 反而正确。
 */
fun Modifier.popBorder(
    width: Dp,
    color: Color = Color.Black,
    shape: Shape = RectangleShape,
): Modifier = this.border(width, color, shape).padding(width)

/**
 * 波普硬阴影：在内容背后画一层同形状的纯色实心块，按偏移量平移，零模糊。
 *
 * 为什么不用 Modifier.shadow：那是模糊投影，一用立刻变 Material 味。
 * 这里的四条规则（见 docs/NATIVE-REWRITE-PLAN.md 第三节）：
 *   1. 零模糊，边缘必须是硬边
 *   2. 偏移量恒定，不随内容尺寸变化
 *   3. 按下时由调用方同步缩小偏移量，元素与阴影一起动
 *   4. 圆角形状的阴影必须与内容用同一个 Shape，否则圆角对不上
 *
 * 说明：Compose 没有公开的 DrawScope.drawOutline，所以这里按 Outline 的三种类型分派，
 * 只用到 drawRect / drawRoundRect / drawPath 三个基础能力，行为可控且不依赖 API 细节。
 */
fun Modifier.popShadow(
    offset: Dp,
    color: Color = Color.Black,
    shape: Shape = RectangleShape,
): Modifier = drawBehind {
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

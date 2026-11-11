package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.popShadow

/**
 * 波普卡片：4dp 黑边 + 零模糊硬阴影 + 纸白底。
 * 全站复用率最高的组件 —— 卡片气质对了，一半的"味道"就对了。
 *
 * padding 默认 S6（1.5rem）对应 `p-6`；仪表盘那屏会传 S2（`p-2`），不要在这里写死。
 *
 * 带 title 时标题是一个贴在左上角、逆时针转 1° 的黑色标签（对应 Web 的
 * `absolute -top-5 left-4 bg-black text-white px-4 py-1 -rotate-1 border-2 border-white`），
 * 同时卡片顶部内边距加到 S8（Web 的 `pt-8`）。
 */
@Composable
fun PopCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    background: Color = PopColor.Core.Paper,
    shadow: Dp = PopShadow.PopLg,
    shape: Shape = RectangleShape,
    borderWidth: Dp = PopStroke.Default,
    padding: PaddingValues = PaddingValues(PopSpace.S6),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        val direction = LocalLayoutDirection.current
        val inner = if (title == null) {
            padding
        } else {
            PaddingValues(
                start = padding.calculateStartPadding(direction),
                top = maxOf(padding.calculateTopPadding(), PopSpace.S8),
                end = padding.calculateEndPadding(direction),
                bottom = padding.calculateBottomPadding(),
            )
        }

        Column(
            modifier = Modifier
                // 顺序要紧：popShadow 必须先于 background。Compose 的绘制类 modifier 按链顺序执行，
                // 反过来写阴影会盖在卡片上面（这个坑第一次编译就踩到了，靠截图像素采样才发现）。
                .popShadow(shadow, shape = shape)
                .background(background, shape)
                .popBorder(borderWidth, PopColor.Core.Black, shape)
                .padding(inner),
            content = content,
        )

        if (title != null) {
            // 标题标签必须被卡片宽度约束：早期版本只给 padding，长标题（比如「查看已保存的 API 设置」）
            // 会直接顶出屏幕右边。weight(fill = false) 让它在可用宽度内自适应，超长则省略。
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(horizontal = PopSpace.S6)
                    .graphicsLayer {
                        translationY = -PopSpace.S5.toPx()
                        rotationZ = -1f
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
            ) {
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .background(PopColor.Core.Black)
                        .border(PopStroke.Thin, PopColor.Core.White)
                        .padding(horizontal = PopSpace.S4, vertical = PopSpace.S1),
                ) {
                    PopDisplay(
                        text = title,
                        size = PopTypeScale.Lg,
                        weight = FontWeight.Black,
                        color = PopColor.Core.White,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

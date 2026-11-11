package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.skinSurface

/**
 * 全站复用率最高的组件 —— 卡片气质对了，一半的"味道"就对了。
 *
 * 两套皮肤下的差别不是"换个颜色"，而是整套形状语言：
 *
 * 波普：4dp 黑边 + 零模糊硬阴影 + 纸白底 + 直角，标题是贴在左上角、
 *       逆时针转 1° 的黑色标签（对应 Web 的 `-top-5 left-4 bg-black -rotate-1 border-2 border-white`）。
 * 玻璃：大圆角 + 半透明填充 + 顶部镜面高光 + 柔和投影，标题变成一枚悬浮的玻璃胶囊。
 *
 * 标题标签的旋转是波普专属：玻璃主题里任何一点旋转都会让"光从上往下打"的假设失效，
 * 高光边会和旋转方向打架，看起来像贴歪的贴纸。
 */
@Composable
fun PopCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    /** 传 [Color.Unspecified] 表示"用皮肤的卡片底色"，只有极少数需要压色的场景才覆盖。 */
    background: Color = Color.Unspecified,
    shadow: Dp = PopShadow.PopLg,
    /** 传 null 表示用皮肤的卡片圆角。 */
    shape: Shape? = null,
    padding: PaddingValues = PaddingValues(PopSpace.S6),
    content: @Composable ColumnScope.() -> Unit,
) {
    val current = LocalSkin.current
    val accentColor = accent
    val resolvedShape = shape ?: RoundedCornerShape(current.radiusCard)
    val fill = if (background == Color.Unspecified) current.palette.surface else background

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
                // 顺序要紧：面板外观必须是一个绘制层，内部固定"投影 → 填充 → 高光 → 描边"。
                // 早先拆成 popShadow + background + popBorder 三连，写反一次就会让阴影盖住卡片，
                // 而那种错误只能靠截图像素采样才看得出来。合成之后外部不可能写错。
                .skinSurface(
                    current = current,
                    shape = resolvedShape,
                    fill = fill,
                    lift = shadow,
                    tint = accentColor,
                )
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
                        rotationZ = if (current.glass) 0f else -1f
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
            ) {
                val labelShape = if (current.glass) {
                    RoundedCornerShape(percent = 50)
                } else {
                    RoundedCornerShape(0.dp)
                }
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .background(
                            color = if (current.glass) current.palette.surfaceStrong else current.palette.inverseSurface,
                            shape = labelShape,
                        )
                        .then(
                            if (current.glass) {
                                Modifier.border(1.dp, Color.White.copy(alpha = 0.9f), labelShape)
                            } else {
                                Modifier.border(PopStroke.Thin, current.palette.inverseInk, labelShape)
                            },
                        )
                        .padding(
                            horizontal = if (current.glass) PopSpace.S5 else PopSpace.S4,
                            vertical = if (current.glass) PopSpace.S2 else PopSpace.S1,
                        ),
                ) {
                    PopDisplay(
                        text = title,
                        size = PopTypeScale.Lg,
                        weight = FontWeight.Black,
                        color = if (current.glass) current.palette.ink else current.palette.inverseInk,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

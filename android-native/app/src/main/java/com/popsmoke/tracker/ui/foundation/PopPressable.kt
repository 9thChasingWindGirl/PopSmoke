package com.popsmoke.tracker.ui.foundation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.popsmoke.tracker.design.PopMotion
import com.popsmoke.tracker.design.PopShadow

/**
 * 波普按压交互：按下时元素朝阴影方向位移，同时阴影偏移量缩小，两者差值归零。
 *
 * 关键点是"阴影必须跟着缩"——只动元素不动阴影，视觉上按钮会像在飘；
 * 两者同步才有"被压进纸面"的错觉。Web 版靠 active:translate + active:shadow 达成，
 * 原生这层由动画驱动，且用 Dp 动画而不是图形变换，保证 4px 级别的位移不被临时缩放糊掉。
 *
 * 注意：没有 ripple。波普风靠位移和硬阴影表达反馈，涟漪会破坏这套语言。
 */
@Composable
fun PopPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    restShadow: Dp = PopShadow.Pop,
    pressedShadow: Dp = PopShadow.Press,
    shape: Shape = RectangleShape,
    shadowColor: Color = Color.Black,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.(pressed: Boolean) -> Unit,
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val active = pressed && enabled
    val shadow by animateDpAsState(
        targetValue = if (active) pressedShadow else restShadow,
        animationSpec = tween(PopMotion.Press),
        label = "pop-press-shadow",
    )
    val shift = restShadow - shadow

    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        ),
    ) {
        // 阴影层：始终与内容同尺寸，只改变偏移
        Box(
            Modifier
                .matchParentSize()
                .offset(x = shadow, y = shadow)
                .background(shadowColor, shape),
        )
        // 内容层：朝阴影方向移动同样多的距离
        Box(Modifier.offset(x = shift, y = shift)) {
            content(active)
        }
    }
}

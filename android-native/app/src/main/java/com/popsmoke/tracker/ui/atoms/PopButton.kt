package com.popsmoke.tracker.ui.atoms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopIconSize
import com.popsmoke.tracker.design.PopLayout
import com.popsmoke.tracker.design.PopMotion
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.design.onAccent
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.PopIcon
import com.popsmoke.tracker.ui.foundation.PopIconKind
import com.popsmoke.tracker.ui.foundation.PopPressable

enum class PopButtonVariant { Primary, Secondary, Danger }

/**
 * 按钮配方：4dp 描边 + 4dp 硬阴影 + 按下时阴影缩到 2dp、元素位移 2dp。
 * 三种变体的色值全部走令牌，主色变体直接吃当前主题色。
 */
@Composable
fun PopButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PopButtonVariant = PopButtonVariant.Primary,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
) {
    val themeAccent = accent
    val themeOnAccent = onAccent
    val background = when (variant) {
        PopButtonVariant.Primary -> themeAccent
        PopButtonVariant.Secondary -> PopColor.Core.White
        PopButtonVariant.Danger -> PopColor.Status.Danger
    }
    val foreground = when (variant) {
        PopButtonVariant.Primary -> themeOnAccent
        PopButtonVariant.Secondary -> PopColor.Core.Black
        PopButtonVariant.Danger -> PopColor.Core.White
    }

    PopPressable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        restShadow = PopShadow.Pop,
        pressedShadow = PopShadow.Press,
    ) {
        // 内层必须和外层同宽：PopPressable 的阴影层是 matchParentSize，
        // 内容不撑满时阴影会从右边和下边漏出来，变成一条黑带。
        val base = Modifier
            .background(background)
            .popBorder(PopStroke.Default, PopColor.Core.Black)
            .defaultMinSize(minWidth = PopLayout.ButtonMinWidth)
            .padding(horizontal = PopSpace.S3, vertical = PopSpace.S2)
        val inner = if (fillWidth) Modifier.fillMaxWidth().then(base) else base

        Box(inner, contentAlignment = Alignment.Center) {
            PopDisplay(
                text = text,
                size = PopTypeScale.Sm,
                tracking = PopTracking.Wider,
                uppercase = true,
                color = foreground,
            )
        }
    }
}

/**
 * 方形图标按钮：白底 + 2dp 描边 + 按下缩到 0.95（对应 `active:scale-95`）。
 * 尺寸 42dp 来自 w-12；点击区域靠外层 PopTapTarget 补足到 48dp 时再放大。
 */
@Composable
fun PopIconButton(
    icon: PopIconKind,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    boxSize: Dp = PopSpace.S12,
    iconSize: Dp = PopIconSize.Xs,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(PopMotion.Fast),
        label = "pop-icon-button-scale",
    )

    Box(
        modifier = modifier
            .size(boxSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(PopColor.Core.White)
            .border(PopStroke.Thin, PopColor.Core.Black)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        PopIcon(icon, size = iconSize)
    }
}

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopGlass
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
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.PopIcon
import com.popsmoke.tracker.ui.foundation.PopIconKind
import com.popsmoke.tracker.ui.foundation.PopPressable
import com.popsmoke.tracker.ui.foundation.jellyPress
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.skinSurface

enum class PopButtonVariant { Primary, Secondary, Danger }

/**
 * 按钮在两套皮肤下是两种完全不同的物理反馈：
 *
 * 波普：4dp 描边 + 4dp 硬阴影，按下时元素朝阴影方向位移、阴影同步收缩，
 *       读作"被压进纸面"；文案全大写、宽字距，是漫画里的标题字。
 *
 * 玻璃：胶囊形、半透明、果冻回弹。按下时物体本身发生形变（横向微涨、纵向压扁），
 *       而不是位移 —— 在磨砂面上做位移会像图层错位。文案用小写原样，只有主色按钮吃强调色。
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
    val current = LocalSkin.current

    if (current.glass) {
        // 按钮是"要按的东西"，在这套界面里都属于高频交互，所以都给材质。
        //
        // 但强度必须分层：主按钮 0.58 的彩玻璃（一屏一个焦点），次要按钮 0.2 的白玻璃
        // （抬起一点点，读作可点），危险按钮保持平面淡红 —— 红色本身已经是信号，
        // 再叠折射和亮边，整页最响的会变成"删除"。
        val flat = variant == PopButtonVariant.Danger
        val fill = when (variant) {
            // 主按钮：彩色玻璃而不是实心色块，要能透出背后的光
            PopButtonVariant.Primary -> themeAccent.copy(alpha = 0.58f)
            // 次要：白玻璃。0.2 刚好能从静默分组（0.3 但无折射、无投影、无亮边）里
            // 分出来，又不会变成第二块玻璃板
            PopButtonVariant.Secondary ->
                current.palette.surfaceStrong.copy(alpha = PopGlass.ControlAlpha)
            // 危险：淡红底 + 红字，而不是一整块实心红。
            // 实心红在浅色玻璃场景里是全屏最响的一块色，会盖过主题色的焦点；
            // 而"红"这个信号由文字和底色的色相承担就够了，不需要饱和度来喊。
            PopButtonVariant.Danger -> PopColor.Status.Danger.copy(alpha = 0.16f)
        }
        val foreground = when (variant) {
            PopButtonVariant.Primary -> themeOnAccent
            PopButtonVariant.Secondary -> current.palette.ink
            PopButtonVariant.Danger -> PopColor.Status.Danger
        }
        val shape = RoundedCornerShape(current.radiusButton)
        val interaction = remember { MutableInteractionSource() }

        val base = Modifier
            .jellyPress(interaction, enabled)
            .then(
                if (flat) {
                    Modifier
                        .clip(shape)
                        .background(fill, shape)
                        .then(
                            when (variant) {
                                PopButtonVariant.Secondary ->
                                    Modifier.border(1.dp, current.palette.ink.copy(alpha = 0.1f), shape)
                                PopButtonVariant.Danger ->
                                    Modifier.border(1.dp, PopColor.Status.Danger.copy(alpha = 0.28f), shape)
                                else -> Modifier
                            },
                        )
                } else {
                    Modifier.skinSurface(
                        current = current,
                        shape = shape,
                        fill = fill,
                        lift = 12.dp,
                        tint = themeAccent,
                    )
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .defaultMinSize(minWidth = PopLayout.ButtonMinWidth)
            .padding(horizontal = PopSpace.S5, vertical = PopSpace.S3)
        val inner = if (fillWidth) Modifier.fillMaxWidth().then(base) else base

        Box(
            modifier = modifier.alpha(if (enabled) 1f else 0.45f).then(inner),
            contentAlignment = Alignment.Center,
        ) {
            PopDisplay(
                text = text,
                size = PopTypeScale.Sm,
                weight = FontWeight.Bold,
                tracking = PopTracking.Normal,
                color = foreground,
            )
        }
        return
    }

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
 * 方形图标按钮。
 *
 * 波普：白底 + 2dp 描边 + 按下缩到 0.95（对应 `active:scale-95`）。
 * 玻璃：正圆 + 半透明 + 果冻按压。玻璃主题里方形小块会显得像"没圆角切干净的贴纸"，
 * 圆形反而更接近 iOS 26 的控件语言。
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
    val current = LocalSkin.current

    if (current.glass) {
        val interaction = remember { MutableInteractionSource() }
        Box(
            modifier = modifier
                .size(boxSize)
                .jellyPress(interaction)
                .skinSurface(
                    current = current,
                    shape = CircleShape,
                    // 圆形图标键在这套界面里出现频率很高（编辑、删除一条记录都有它），
                    // 0.055 的 surfaceAlpha 在浅背景上几乎看不见，按键的可点性丢了。
                    fill = current.palette.surfaceStrong.copy(alpha = PopGlass.ControlAlpha),
                    lift = 8.dp,
                )
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
            PopIcon(icon, size = iconSize, color = current.palette.ink)
        }
        return
    }

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

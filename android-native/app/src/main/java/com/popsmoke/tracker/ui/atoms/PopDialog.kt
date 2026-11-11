package com.popsmoke.tracker.ui.atoms

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopGlass
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopLayout
import com.popsmoke.tracker.design.PopMotion
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.skinSurface

/**
 * 全站唯一的弹窗基元。
 *
 * Web 版有 11 个弹窗组件，结构几乎一样（遮罩 + 白卡 + 描边标题 + 两个按钮），
 * 只是内容不同。这里合并成一个：以后改一次弹窗，全站 11 处一起变。
 *
 * 遮罩点击关闭；返回键关闭（BackHandler）—— 这是那条"视觉让路"的底线，
 * Web 版按返回键会直接退出 App，原生必须先把弹窗关掉。
 */
@Composable
fun PopDialog(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    titleColor: Color = Color.Unspecified,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    val current = LocalSkin.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(PopMotion.Fast)) + scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(PopMotion.Normal, easing = PopMotion.EaseOutCurve),
        ),
        exit = fadeOut(tween(PopMotion.Instant)),
    ) {
        Box(Modifier.fillMaxSize()) {
            // 遮罩单独一层，保证点卡片内部不会误触关闭
            Box(
                Modifier
                    .fillMaxSize()
                    .background(current.palette.scrim)
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = PopSpace.S4),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier
                        .widthIn(max = PopLayout.DialogMaxWidth)
                        .fillMaxWidth()
                        // 弹窗必须是最"实"的一层玻璃：它要压住下面的内容，
                        // 用跟卡片一样的透明度会透出底下的字，读起来很脏
                        .skinSurface(
                            current = current,
                            shape = RoundedCornerShape(current.radiusPanel),
                            fill = current.palette.surfaceStrong.copy(alpha = PopGlass.OverlayAlpha),
                            lift = PopShadow.Pop,
                        )
                        .padding(PopSpace.S6),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PopDisplay(
                        text = title,
                        size = PopTypeScale.Xxl,
                        tracking = PopTracking.Normal,
                        color = if (titleColor == Color.Unspecified) current.palette.ink else titleColor,
                        maxLines = 2,
                    )
                    if (message != null) {
                        PopBody(
                            text = message,
                            modifier = Modifier.padding(top = PopSpace.S3),
                            size = PopTypeScale.Sm,
                            weight = FontWeight.Normal,
                            color = current.palette.inkSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                        )
                    }
                    if (content != null) {
                        Column(
                            modifier = Modifier.padding(top = PopSpace.S4).fillMaxWidth(),
                            content = content,
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = PopSpace.S6).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PopSpace.S3),
                        content = actions,
                    )
                }
            }
        }
    }
}

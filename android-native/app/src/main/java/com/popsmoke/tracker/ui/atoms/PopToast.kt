package com.popsmoke.tracker.ui.atoms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopMotion
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder

enum class PopToastKind { Success, Error, Info, Warning }

/**
 * 页内提示条（对应 PopNotification）：4dp 描边 + 图标块 + 标题/正文 + 关闭键。
 *
 * 不用 Snackbar：它固定在屏幕底部，会盖住底栏；Web 版这一条是嵌在卡片流里的，
 * 位置本身就在传达"这条消息属于上面那个操作"。
 */
@Composable
fun PopToast(
    visible: Boolean,
    title: String,
    message: String,
    kind: PopToastKind,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (barColor, icon) = when (kind) {
        PopToastKind.Success -> PopColor.Status.Success to "✓"
        PopToastKind.Error -> PopColor.Status.Error to "✕"
        PopToastKind.Info -> PopColor.Status.Info to "i"
        PopToastKind.Warning -> PopColor.Status.Warning to "!"
    }
    val onBar = if (kind == PopToastKind.Warning) PopColor.Core.Black else PopColor.Core.White

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(tween(PopMotion.Normal)) + fadeIn(tween(PopMotion.Normal)),
        exit = shrinkVertically(tween(PopMotion.Normal)) + fadeOut(tween(PopMotion.Normal)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(PopColor.Core.White)
                .popBorder(PopStroke.Thin, PopColor.Core.Black),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .background(barColor)
                    .padding(horizontal = PopSpace.S3, vertical = PopSpace.S3),
                contentAlignment = Alignment.Center,
            ) {
                PopDisplay(text = icon, size = PopTypeScale.Lg, weight = FontWeight.Black, color = onBar)
            }
            Row(
                Modifier.weight(1f).padding(horizontal = PopSpace.S3, vertical = PopSpace.S2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    PopDisplay(
                        text = title,
                        size = PopTypeScale.Sm,
                        weight = FontWeight.Black,
                        color = barColor,
                        maxLines = 1,
                    )
                    PopBody(
                        text = message,
                        size = PopTypeScale.Xs,
                        weight = FontWeight.Bold,
                        color = PopColor.Neutral.Gray600,
                        maxLines = 2,
                    )
                }
            }
            Box(
                Modifier
                    .size(PopSpace.S6)
                    .padding(end = PopSpace.S3)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                PopDisplay(text = "✕", size = PopTypeScale.Xs, weight = FontWeight.Black)
            }
        }
    }
}

/** 颜色工具：判断主题色深浅，决定压在上面的是黑字还是白字。 */
fun Color.isLightColor(): Boolean {
    val r = red * 255f
    val g = green * 255f
    val b = blue * 255f
    return (r * 0.299f + g * 0.587f + b * 0.114f) > 160f
}

/** 反色文字。 */
fun Color.onColor(): Color = if (isLightColor()) Color.Black else Color.White

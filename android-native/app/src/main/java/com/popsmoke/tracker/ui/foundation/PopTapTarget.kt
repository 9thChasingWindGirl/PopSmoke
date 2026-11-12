package com.popsmoke.tracker.ui.foundation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 无障碍触摸目标：视觉只有 24.5dp 的小图标按钮，点击区域补到 48dp。
 * 这是那条"视觉让路"的底线的具体落地 —— Instagram/Twitter 之类的小图标都这么做。
 */
object PopA11y {
    val TapTarget: Dp = 48.dp
}

@Composable
fun PopTapTarget(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(PopA11y.TapTarget)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

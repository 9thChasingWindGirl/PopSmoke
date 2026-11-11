package com.popsmoke.tracker.ui.atoms

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.popInkSecondary

/**
 * 加载态：旋转的方块 + 文案。
 *
 * 不用转圈的 Material CircularProgressIndicator —— 圆弧的软边和整站的硬边冲突。
 * 这里转的是一个 4dp 描边的方块，转速对应 PopMotion.Slow，慢一点才不躁。
 */
@Composable
fun PopLoading(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val transition = rememberInfiniteTransition(label = "pop-loading")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pop-loading-angle",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(PopSpace.S14)
                .graphicsLayer { rotationZ = angle }
                .background(accent)
                .border(PopStroke.Default, popInk),
        )
        if (label != null) {
            Box(Modifier.size(PopSpace.S4))
            PopDisplay(
                text = label,
                size = PopTypeScale.Base,
                weight = FontWeight.Bold,
                color = popInkSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

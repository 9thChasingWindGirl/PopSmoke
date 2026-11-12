package com.popsmoke.tracker.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopMotion
import com.popsmoke.tracker.design.PopShadow

/**
 * 3D 翻转卡：正面仪表盘、背面今日记录。
 *
 * 两个必须踩准的参数：
 * 1. cameraDistance ≈ 1000dp 的像素值，对应 CSS `perspective: 1000px`。用默认值透视会夸张到变形。
 * 2. 背面先自己 rotateY(180°)，再叠加父层旋转，总角度 360° 才是正向。
 *
 * Compose 没有 backface-visibility，用角度过半来切换显示哪一面：
 * 90° 时两张脸都是侧立的（宽度收敛为 0），在此处切换看不见，也不会闪。
 *
 * 性能上有两处刻意为之：
 * - **背面只在后半个翻转里组合**。背面是 LazyColumn，700ms 里每帧都参与测量和绘制，
 *   是翻转掉帧的主因；前半程它根本不可见，没有理由让它活着。
 * - **cameraDistance 在组合期算成 px**。放在 graphicsLayer 的 lambda 里每帧都会
 *   `Dp.toPx()` 一次，纯属浪费。
 *
 * 正面始终参与测量：卡片高度由它决定，抽掉它背面就没有高度可依，会塌成 0。
 */
@Composable
fun FlipCard(
    flipped: Boolean,
    modifier: Modifier = Modifier,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = PopMotion.Flip, easing = PopMotion.EaseOutCurve),
        label = "flip-rotation",
    )

    val cameraDistancePx = with(LocalDensity.current) { 1000.dp.toPx() }
    val showFront = rotation < 90f

    Box(
        modifier
            .padding(end = PopShadow.PopLg, bottom = PopShadow.PopLg)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = cameraDistancePx
            },
    ) {
        Box(
            Modifier.graphicsLayer { alpha = if (showFront) 1f else 0f },
        ) {
            front()
        }
        if (!showFront) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { rotationY = 180f },
            ) {
                back()
            }
        }
    }
}

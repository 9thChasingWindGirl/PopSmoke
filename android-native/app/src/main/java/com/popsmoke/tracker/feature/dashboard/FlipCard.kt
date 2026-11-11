package com.popsmoke.tracker.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
 * Compose 没有 backface-visibility，用「过半程切 alpha」实现同一效果：
 * 90° 时两张脸都是侧立的，切换不可见，所以不会闪。
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

    // 高度由「正面」决定：正面是唯一参与布局的面，背面 matchParentSize 跟随。
    // 这正是 Web 版的结构（背面是 absolute inset-0）—— 早先两面都 fillMaxSize，
    // 卡片就被容器拉满，仪表区跟着变长，这一条是那处偏差的根源。
    Box(
        modifier
            .padding(end = PopShadow.PopLg, bottom = PopShadow.PopLg)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 1000.dp.toPx()
            },
    ) {
        Box(
            Modifier.graphicsLayer { alpha = if (rotation < 90f) 1f else 0f },
        ) {
            front()
        }
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationY = 180f
                    alpha = if (rotation >= 90f) 1f else 0f
                },
        ) {
            back()
        }
    }
}

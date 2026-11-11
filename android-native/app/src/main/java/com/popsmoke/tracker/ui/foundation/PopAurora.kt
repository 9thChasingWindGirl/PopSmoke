package com.popsmoke.tracker.ui.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopGlass
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 液态玻璃的底色：柔和的多色光斑 + 极细颗粒。
 *
 * 为什么这不是"纯装饰"：玻璃面板是半透明的，背后必须**有东西可透**。
 * 换成纯色底时，半透明面板看起来只是浅灰，和波普的纸白几乎没有区别，
 * 整套液态玻璃就塌成"圆角 + 灰边"。光斑是这个主题成立的前提。
 *
 * 性能：每个光斑是一个独立 layer，漂移只改变换矩阵（graphicsLayer 的 lambda 在绘制阶段求值），
 * 不触发重组，也不重绘光斑本身。周期取自令牌（默认 48 秒），慢到几乎察觉不到移动，
 * 但眨眼之间能看出"背景是活的"。
 */
@Composable
fun PopAurora(
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFFF97316),
    animate: Boolean = !LocalInspectionMode.current,
) {
    val density = LocalDensity.current
    val grain = rememberGrainBrush(density.density)

    // 三个光斑的冷/暖搭配：主光斑吃主题色，另两个固定为冷蓝与暖橘。
    // 不用"主题色的色相 ±140°"自动派生 —— 那样选到低饱和主题色时会出现脏灰，
    // 而冷暖对冲在任何主题色下都稳定。
    val blobs = remember(accent) {
        listOf(
            accent.copy(alpha = PopGlass.AuroraAlpha),
            Color(0xFF8FB4FF).copy(alpha = PopGlass.AuroraAlpha * 0.72f),
            Color(0xFFFFC4A3).copy(alpha = PopGlass.AuroraAlpha * 0.62f),
        )
    }

    // 必须无条件创建 transition：放在 if 分支里就是条件调用 composable，
    // 主题切换时组合结构会变，动画状态也会被丢掉。
    // 用 State 而不是 by 委托：by 会在组合阶段读值，每帧触发重组；
    // 包进 lambda 交给 graphicsLayer，就变成绘制阶段读取，只改变换矩阵。
    val transition = rememberInfiniteTransition(label = "aurora-drift")
    val phaseState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PopGlass.DriftDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aurora-phase",
    )
    val progress: () -> Float = { if (animate) phaseState.value else 0f }

    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val big = PopGlass.AuroraBlob
        val small = PopGlass.AuroraSecondary

        AuroraBlob(
            color = blobs[0],
            diameter = big,
            cx = 0.18f,
            cy = 0.12f,
            phase = 0f,
            drift = with(density) { 44.dp.toPx() },
            progress = progress,
            width = w,
            height = h,
        )
        AuroraBlob(
            color = blobs[1],
            diameter = big * 0.92f,
            cx = 0.92f,
            cy = 0.46f,
            phase = 2.1f,
            drift = with(density) { 52.dp.toPx() },
            progress = progress,
            width = w,
            height = h,
        )
        AuroraBlob(
            color = blobs[2],
            diameter = small,
            // 第三枚放在屏幕中下部而不是底边：卡片正好压在这一带，
            // 光斑全挤在四个角时玻璃没东西可透，看起来就是一块白板
            cx = 0.55f,
            cy = 0.66f,
            phase = 4.3f,
            drift = with(density) { 38.dp.toPx() },
            progress = progress,
            width = w,
            height = h,
        )

        // 颗粒层压在光斑之上、面板之下：磨砂感来自这一层。
        // 用固定种子生成，同一份代码在任何机器上渲染出的噪点一致 —— 截图基准不能靠运气。
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(grain) },
        )
    }
}

@Composable
private fun AuroraBlob(
    color: Color,
    diameter: Dp,
    cx: Float,
    cy: Float,
    phase: Float,
    drift: Float,
    progress: () -> Float,
    width: Float,
    height: Float,
) {
    val d = with(LocalDensity.current) { diameter.toPx() }
    val brush = remember(color, diameter) {
        // 衰减不能太快：0.52 处就掉到 42% 的话，光斑只剩中心一小团，
        // 屏幕中部会是纯底色，玻璃卡片在那里就没有东西可透
        Brush.radialGradient(
            0f to color,
            0.68f to color.copy(alpha = color.alpha * 0.55f),
            1f to color.copy(alpha = 0f),
        )
    }

    Box(
        Modifier
            // 基础位置用 offset（固定值，不读动画状态，不会触发 relayout），
            // 漂移放在 graphicsLayer 里（绘制阶段求值，只改变换矩阵）
            .offset {
                IntOffset(
                    x = (cx * width - d / 2f).roundToInt(),
                    y = (cy * height - d / 2f).roundToInt(),
                )
            }
            .size(diameter)
            .graphicsLayer {
                val t = progress() * (2f * Math.PI.toFloat())
                translationX = sin(t + phase) * drift
                translationY = cos(t * 0.68f + phase) * drift * 0.62f
            }
            .background(brush, CircleShape),
    )
}

/**
 * 磨砂颗粒：48×48 的重复贴图，一半白一半黑。
 *
 * 单个像素级的噪点铺满全屏会有摩尔纹，48dp 的图块在 3x 屏上接近 1px 颗粒，刚好。
 * 生成一次即可（约 2300 次绘制，只在首次组合时发生）。
 */
@Composable
private fun rememberGrainBrush(density: Float): Brush = remember(density) {
    val size = 48
    val bitmap = ImageBitmap(size, size)
    val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
    val paint = Paint()
    var seed = 20261112
    fun next(): Float {
        // 线性同余：固定种子 → 每次渲染噪点完全一致
        seed = seed * 1103515245 + 12345
        return ((seed ushr 16) and 0x7FFF) / 32767f
    }
    for (x in 0 until size) {
        for (y in 0 until size) {
            val v = next()
            val alpha = when {
                v > 0.80f -> PopGlass.GrainAlpha
                v < 0.22f -> PopGlass.GrainAlpha * 0.72f
                else -> 0f
            }
            if (alpha > 0f) {
                paint.color = if (v > 0.80f) Color.White.copy(alpha = alpha) else Color.Black.copy(alpha = alpha)
                canvas.drawCircle(
                    center = androidx.compose.ui.geometry.Offset(x + 0.5f, y + 0.5f),
                    radius = 0.55f,
                    paint = paint,
                )
            }
        }
    }
    ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
}

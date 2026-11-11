package com.popsmoke.tracker.ui.foundation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopGlass
import com.popsmoke.tracker.design.skin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 液态玻璃的底色层。
 *
 * 这一层比它看起来重要得多。第一版只有三团柔光斑，结果是：无论怎么调面板的透明度，
 * 玻璃都不像玻璃 —— 因为**柔光没有可被弯折的结构**。真实的玻璃之所以一眼可辨，
 * 是因为它把背后有边界的东西推歪、压扁；弯曲一团本身就是平滑渐变的雾，什么都看不出来。
 *
 * 所以这里画的是四种不同"频率"的光：
 *
 * - 三团大柔光斑：给整体定调，决定卡片里透出来的主色（低频，负责氛围）。
 * - 两条宽光带：带边缘相对清晰、有明确的"芯"。它们是折射的**证据** ——
 *   玻璃面板的边缘把这层光带推开时，错位肉眼可见（中频，负责质感）。
 * - 几道同心弧：极淡，只在右上角露一点，读作液面涟漪。它们提供曲线参照，
 *   直线型的折射在弧线上更容易被察觉（高频参照，负责"液体"的暗示）。
 * - 48dp 颗粒：磨砂手感，压在最上层（极高频，负责材质）。
 *
 * 性能：每枚光斑/光带都是独立 layer，漂移只改变换矩阵（graphicsLayer 的 lambda
 * 在绘制阶段求值），不触发重组，也不重绘自身。周期 48 秒，慢到几乎察觉不到，
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
    val canvasColor = skin.palette.canvas

    // 五个椭圆色团，冷暖对冲。
    //
    // 为什么是椭圆而不是圆、为什么是五个：玻璃要"透"就要有东西可透，
    // 但三枚等大的圆会互相融合成一团均匀的雾 —— 溶在一起之后，
    // 卡片压在哪都看不到层次。改成椭圆、五个、尺寸和位置错开，
    // 相邻色团的交界处就有了明暗与色相的转折，玻璃跨过这些交界时才"透得出内容"。
    //
    // 色相不按主题色派生：低饱和主题色派生 ±140° 会得到脏灰。
    // 这里固定一组冷暖对冲的色板，只让第一枚吃主题色，任何主题色都不会翻车。
    val blobs = remember(accent) {
        listOf(
            BlobSpec(accent.copy(alpha = 0.44f), 1.02f, 0.66f, 0.04f, 0.08f, 0f),
            BlobSpec(Color(0xFF7FA6FF).copy(alpha = 0.30f), 1.10f, 0.56f, 0.98f, 0.40f, 2.1f),
            BlobSpec(Color(0xFFFFB59B).copy(alpha = 0.18f), 0.88f, 0.50f, 0.60f, 0.68f, 4.3f),
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

    BoxWithConstraints(modifier.background(canvasColor)) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        // 色团逐个铺开。漂移量按尺寸给：大色团动得少一点，
        // 否则边缘扫过屏幕时会像一块布在抖。
        blobs.forEach { blob ->
            AuroraBlob(
                spec = blob,
                progress = progress,
                screenWidth = w,
                screenHeight = h,
                drift = with(density) { (24 + blob.widthFactor * 20).dp.toPx() },
            )
        }

        // ---- 光带：一条很柔的斜向光 ----
        // 只留一条，而且过渡拉长到 26%。更早的版本是两条 104dp 的硬边光带，
        // 边界确实清晰、折射也看得出来，但整屏读作"被划了两道"，
        // 像漏光而不像玻璃。结构与美感在这里是冲突的，取柔。
        GlassBand(
            color = accent.copy(alpha = PopGlass.FieldBandAlpha * 0.55f),
            angle = -27f,
            bandHeight = 132.dp,
            centerY = 0.235f,
            screenWidth = w,
            screenHeight = h,
            drift = with(density) { 26.dp.toPx() },
            phase = 1.4f,
            progress = progress,
        )

        // ---- 涟漪：极淡的曲线参照 ----
        val arcInk = skin.palette.ink
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawFieldArcs(arcInk, size.width, size.height) },
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

/**
 * 一条倾斜的宽光带。
 *
 * 渐变不能用"透明 → 实色 → 透明"两段式：那样整条带的明度是连续变化的，
 * 没有边，折射时位移过去了也看不出来。这里用六段，中段 0.44–0.56 是一段**平台**，
 * 平台两侧快速衰减 —— 有平台就有"芯"，芯的两条边界就是折射可见的参照物。
 */
@Composable
private fun GlassBand(
    color: Color,
    angle: Float,
    bandHeight: Dp,
    centerY: Float,
    screenWidth: Float,
    screenHeight: Float,
    drift: Float,
    phase: Float,
    progress: () -> Float,
) {
    val density = LocalDensity.current
    val radians = Math.toRadians(angle.toDouble()).toFloat()
    // 旋转后要盖住整个屏宽，宽度得除以 cos；再留 18% 余量，
    // 免得漂移时某一端先露出直角
    val bandWidthPx = screenWidth / cos(radians) * 1.18f
    val bandWidth = with(density) { bandWidthPx.toDp() }

    val brush = remember(color) {
        // 过渡拉到 26%，整条带子几乎没有边 —— 它的职责已经变成
        // "在左上方压一层暖光"，而不是提供可被弯折的结构。
        // 结构交给椭圆色团的交界去承担。
        Brush.verticalGradient(
            0f to color.copy(alpha = 0f),
            0.26f to color.copy(alpha = color.alpha * 0.34f),
            0.44f to color,
            0.56f to color,
            0.74f to color.copy(alpha = color.alpha * 0.34f),
            1f to color.copy(alpha = 0f),
        )
    }

    Box(
        Modifier
            .offset {
                IntOffset(
                    x = ((screenWidth - bandWidthPx) / 2f).roundToInt(),
                    y = (centerY * screenHeight - bandHeight.toPx() / 2f).roundToInt(),
                )
            }
            .size(bandWidth, bandHeight)
            .graphicsLayer {
                rotationZ = angle
                val t = progress() * 2f * Math.PI.toFloat()
                translationX = sin(t * 0.8f + phase) * drift
                translationY = cos(t * 0.5f + phase) * drift * 0.4f
            }
            .background(brush),
    )
}

/**
 * 右上角外的同心弧。
 *
 * 圆心放在屏幕外，所以画面上只露出右侧几条大弧 —— 像一次缓慢的涟漪扫过，
 * 而不是一圈同心靶。透明度随半径递减，远端几乎不可见，避免抢内容。
 */
private fun DrawScope.drawFieldArcs(ink: Color, w: Float, h: Float) {
    val cx = w * 1.16f
    val cy = h * 0.02f
    var radius = w * 0.62f
    val step = w * 0.145f
    var index = 0
    while (index < 5) {
        val fade = PopGlass.FieldArcAlpha * (1f - index * 0.16f)
        drawCircle(
            color = ink.copy(alpha = fade),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx()),
        )
        radius += step
        index++
    }
}

/** 一枚椭圆色团。[widthFactor] / [heightFactor] 相对屏幕尺寸，所以换机型布局不变形。 */
private data class BlobSpec(
    val color: Color,
    val widthFactor: Float,
    val heightFactor: Float,
    val cx: Float,
    val cy: Float,
    val phase: Float,
)

@Composable
private fun AuroraBlob(
    spec: BlobSpec,
    progress: () -> Float,
    screenWidth: Float,
    screenHeight: Float,
    drift: Float,
) {
    val density = LocalDensity.current
    val blobWidth = with(density) { (spec.widthFactor * screenWidth).toDp() }
    val blobHeight = with(density) { (spec.heightFactor * screenHeight).toDp() }

    val brush = remember(spec.color) {
        // 衰减不能太快：0.5 处就掉到 30% 的话，色团只剩中心一小团，
        // 屏幕中部会是纯底色，玻璃卡片在那里就没有东西可透。
        Brush.radialGradient(
            0f to spec.color,
            0.58f to spec.color.copy(alpha = spec.color.alpha * 0.62f),
            1f to spec.color.copy(alpha = 0f),
        )
    }

    Box(
        Modifier
            // 基础位置用 offset（固定值，不读动画状态，不会触发 relayout），
            // 漂移放在 graphicsLayer 里（绘制阶段求值，只改变换矩阵）
            .offset {
                IntOffset(
                    x = (spec.cx * screenWidth - blobWidth.toPx() / 2f).roundToInt(),
                    y = (spec.cy * screenHeight - blobHeight.toPx() / 2f).roundToInt(),
                )
            }
            .size(blobWidth, blobHeight)
            .graphicsLayer {
                val t = progress() * (2f * Math.PI.toFloat())
                translationX = sin(t + spec.phase) * drift
                translationY = cos(t * 0.68f + spec.phase) * drift * 0.62f
            }
            // CircleShape 在非正方形尺寸下会被拉成椭圆 —— 正是要的效果
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
                    center = Offset(x + 0.5f, y + 0.5f),
                    radius = 0.55f,
                    paint = paint,
                )
            }
        }
    }
    ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
}

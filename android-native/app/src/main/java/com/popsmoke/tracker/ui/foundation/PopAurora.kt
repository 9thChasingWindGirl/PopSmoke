package com.popsmoke.tracker.ui.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopGlass
import com.popsmoke.tracker.design.skin
import kotlin.math.hypot
import kotlin.math.max

/**
 * 液态玻璃的底色层。
 *
 * 这一层比它看起来重要得多。最早的版本只有三团柔光斑，结果是：无论怎么调面板的透明度，
 * 玻璃都不像玻璃 —— 因为**柔光没有可被弯折的结构**。真实的玻璃之所以一眼可辨，
 * 是因为它把背后有边界的东西推歪、压扁；弯曲一团本身就是平滑渐变的雾，什么都看不出来。
 *
 * 所以这里画的是四种不同"频率"的光：
 *
 * - 三团椭圆色团：给整体定调，决定卡片里透出来的主色（低频，负责氛围）。
 * - 一条宽光带：带边缘相对清晰，是柔和的中频参照。
 * - 一组单向的织纹斜线：间距 38dp、1dp 粗、白 13%。这一层纯粹是给折射当标尺的 ——
 *   玻璃边缘把线段推开时，线段的错位和断裂一眼可辨（高频，负责"证明折射"）。
 * - 48dp 颗粒：磨砂手感，压在最上层（极高频，负责材质）。
 *
 * ## 为什么这一层是静态的
 *
 * 背景原来是带漂移动画的（48 秒周期，用 graphicsLayer 在绘制阶段求值）。做折射时
 * 这个设计必须放弃：着色器要采样背景，采样源只能是**一张静态位图** —— 而位图一旦
 * 落后于屏幕上的背景，卡片边界处的织纹就会和外面错位，那比不做折射更糟。
 *
 * 让位图跟着动画每帧更新是不可行的（每帧一次全屏位图上传）。折中的第二条路是
 * "只有色团漂移、织纹不动"，但色团漂移恰恰是卡片内外色差的来源，同样会露馅。
 *
 * 所以整层改为静态，动态留给交互（按压时的果冻回弹与研究边缘光）。这个取舍是自觉的：
 * 背景的"活"是锦上添花，折射的"真"是材质成立的前提，两者冲突时保后者。
 *
 * ## 为什么只有一个绘制函数
 *
 * [drawAuroraBody] 同时被 [PopAurora]（画到屏幕上）和 [rememberBackdropImage]（画到
 * 离屏位图供着色器采样）调用。两边像素必须逐点一致 —— 只要有一处不一致，玻璃边缘
 * 就会出现一条色差或者纹理错位的接缝。所以这里不允许有第二份"近似的"实现。
 */
@Composable
fun PopAurora(
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFFF97316),
) {
    val density = LocalDensity.current
    val grain = rememberGrainBrush(density.density)
    val canvasColor = skin.palette.canvas
    val arcInk = skin.palette.ink
    Box(
        modifier
            .background(canvasColor)
            .drawBehind { drawAuroraBody(accent, grain, canvasColor, arcInk) },
    )
}

/**
 * 背景的全部内容，一次性画完。
 *
 * 参数只有主题色和颗粒笔刷，其余全是几何 —— 同一组入参必然得到同一张图，
 * 这是屏幕与离屏位图能对齐的前提。
 */
fun DrawScope.drawAuroraBody(
    accent: Color,
    grain: Brush,
    canvasColor: Color,
    arcInk: Color,
) {
    val w = size.width
    val h = size.height
    drawRect(color = canvasColor)

    // 五团椭圆色团，冷暖对冲，四面都铺到。
    //
    // 为什么是椭圆而不是圆：玻璃要"透"就要有东西可透，但等大的圆会互相融合成一团
    // 均匀的雾 —— 溶在一起之后，卡片压在哪都看不到层次。改成椭圆、尺寸和位置错开，
    // 相邻色团的交界处就有了明暗与色相的转折。
    //
    // 为什么要铺到四个角：只放三团时，卡片下半部分的边缘后面是一片纯底色。
    // 实测那一带的像素标准差只有 1.6 —— 等于在均匀灰上做折射，做得再对也看不出来。
    // 折射要可见，前提是**卡片边缘后面确实有东西**，所以宁可多两团，也不能留空档。
    //
    // 色相不按主题色派生：低饱和主题色派生 ±140° 会得到脏灰。
    // 这里固定一组冷暖对冲的色板，只让第一枚吃主题色，任何主题色都不会翻车。
    val blobs = listOf(
        BlobSpec(accent.copy(alpha = 0.44f), 1.02f, 0.66f, 0.04f, 0.08f),
        BlobSpec(Color(0xFF7FA6FF).copy(alpha = 0.30f), 1.10f, 0.56f, 0.98f, 0.40f),
        BlobSpec(Color(0xFFFFB59B).copy(alpha = 0.18f), 0.88f, 0.50f, 0.60f, 0.68f),
        BlobSpec(Color(0xFF8FD6C8).copy(alpha = 0.22f), 0.92f, 0.48f, 0.06f, 0.95f),
        BlobSpec(Color(0xFFB9A6F0).copy(alpha = 0.24f), 0.82f, 0.44f, 0.96f, 0.86f),
    )
    blobs.forEach { blob ->
        val bw = blob.widthFactor * w
        val bh = blob.heightFactor * h
        val cx = blob.cx * w
        val cy = blob.cy * h
        // radialGradient 只能画正圆，椭圆靠 scale 拉出来；pivot 取圆心，
        // 缩放时圆心不动，笔刷跟着一起被拉，形状才对。
        val radius = max(bw, bh) / 2f
        val brush = Brush.radialGradient(
            0f to blob.color,
            // 衰减不能太快：0.5 处就掉到 30% 的话，色团只剩中心一小团，
            // 屏幕中部会是纯底色，玻璃卡片在那里就没有东西可透。
            0.58f to blob.color.copy(alpha = blob.color.alpha * 0.62f),
            1f to blob.color.copy(alpha = 0f),
            center = Offset(cx, cy),
            radius = radius,
        )
        scale(
            scaleX = bw / (2f * radius),
            scaleY = bh / (2f * radius),
            pivot = Offset(cx, cy),
        ) {
            drawCircle(brush = brush, radius = radius, center = Offset(cx, cy))
        }
    }

    // 一条很柔的斜向光，定光位。
    //
    // 更早的版本是两条 104dp 的硬边光带，边界确实清晰、折射也看得出来，
    // 但整屏读作"被划了两道"，像漏光而不像玻璃。结构与美感在这里是冲突的，取柔。
    val bandColor = accent.copy(alpha = PopGlass.FieldBandAlpha * 0.55f)
    val bandCenterY = h * 0.235f
    val bandHeight = 132.dp.toPx()
    val bandWidth = w * 2f
    val bandBrush = Brush.verticalGradient(
        0f to bandColor.copy(alpha = 0f),
        0.26f to bandColor.copy(alpha = bandColor.alpha * 0.34f),
        0.44f to bandColor,
        0.56f to bandColor,
        0.74f to bandColor.copy(alpha = bandColor.alpha * 0.34f),
        1f to bandColor.copy(alpha = 0f),
    )
    rotate(degrees = -27f, pivot = Offset(w / 2f, bandCenterY)) {
        drawRect(
            brush = bandBrush,
            topLeft = Offset(w / 2f - bandWidth / 2f, bandCenterY - bandHeight / 2f),
            size = Size(bandWidth, bandHeight),
        )
    }

    drawFieldArcs(arcInk, w, h)
    drawWeave(w, h, PopGlass.WeaveSpacing.toPx(), PopGlass.WeaveStroke.toPx())
    drawRect(grain)
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

/**
 * 单向的极淡斜纹，读作纸张或织物的纹理方向。
 *
 * 这一层是整块玻璃能不能"看起来在折射"的前提。背景原本只有几团柔光，
 * 弯曲柔光等于没弯 —— 光斑的明度是连续变化的，位移过去之后前后完全一样。
 * 线段则相反：它有端点、有中断、有方向，被推开一点点就是肉眼可见的错位。
 *
 * 试过双向正交（菱形格），瞬间整屏读作"格子布"，把玻璃彻底压成了背景上的印花。
 * 单向就够了 —— 折射错位的可见性来自线段本身的中断，不来自交叉。
 *
 * 角度取 -27°：与那条斜向光带同向，两者的走向叠在一起，线条不显得突兀。
 */
private fun DrawScope.drawWeave(w: Float, h: Float, spacing: Float, stroke: Float) {
    if (spacing <= 0f) return
    val color = Color.White.copy(alpha = PopGlass.WeaveAlpha)
    // 旋转之后要盖住整个屏幕，线段长度按对角线给
    val half = hypot(w, h)
    rotate(-27f) {
        var y = -half
        while (y <= half) {
            drawLine(
                color = color,
                start = Offset(-half, y),
                end = Offset(half, y),
                strokeWidth = stroke,
            )
            y += spacing
        }
    }
}

/** 一枚椭圆色团。[widthFactor] / [heightFactor] 相对屏幕尺寸，所以换机型布局不变形。 */
private data class BlobSpec(
    val color: Color,
    val widthFactor: Float,
    val heightFactor: Float,
    val cx: Float,
    val cy: Float,
)

/**
 * 磨砂颗粒：48×48 的重复贴图，一半白一半黑。
 *
 * 单个像素级的噪点铺满全屏会有摩尔纹，48dp 的图块在 3x 屏上接近 1px 颗粒，刚好。
 * 生成一次即可（约 2300 次绘制，只在首次组合时发生）。
 */
@Composable
internal fun rememberGrainBrush(density: Float): Brush = remember(density) {
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

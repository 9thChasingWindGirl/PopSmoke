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

    // 六团椭圆色团，冷暖对冲，把画面铺满。
    //
    // 为什么是椭圆而不是圆：玻璃要"透"就要有东西可透，但等大的圆会互相融合成一团
    // 均匀的雾 —— 溶在一起之后，卡片压在哪都看不到层次。改成椭圆、尺寸和位置错开，
    // 相邻色团的交界处就有了明暗与色相的转折。
    //
    // 为什么要铺满而不是只铺四角：只铺边缘时，屏幕正中是一片纯色底 ——
    // 那正是内容最密集的地方，玻璃压上去等于在一张白纸上做折射，做得再对也看不见。
    // 第一版就吃过这个亏：卡片背面的像素标准差只有 1.6。现在中部也有色团过渡。
    //
    // 色相不按主题色派生：低饱和主题色派生 ±140° 会得到脏灰。
    // 这里固定一组冷暖对冲的色板，只让第一枚吃主题色，任何主题色都不会翻车。
    val blobs = listOf(
        BlobSpec(accent.copy(alpha = 0.5f), 1.05f, 0.72f, 0.02f, 0.05f),
        BlobSpec(Color(0xFF7FA6FF).copy(alpha = 0.40f), 1.15f, 0.62f, 1.00f, 0.32f),
        BlobSpec(Color(0xFFFFB59B).copy(alpha = 0.28f), 0.95f, 0.58f, 0.62f, 0.58f),
        BlobSpec(Color(0xFF8FD6C8).copy(alpha = 0.32f), 1.00f, 0.55f, 0.03f, 0.84f),
        BlobSpec(Color(0xFFB9A6F0).copy(alpha = 0.34f), 0.92f, 0.52f, 0.97f, 0.76f),
        BlobSpec(Color(0xFFFFD9E6).copy(alpha = 0.24f), 0.86f, 0.50f, 0.44f, 0.24f),
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
            0.68f to blob.color.copy(alpha = blob.color.alpha * 0.58f),
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

    // 三条有边界的色带，定光位。
    //
    // 比"一条柔光"多花不了几行，但决定了整块玻璃成不成立。色带的边是软的（十几 dp
    // 的过渡），却**有位置**：玻璃压上去，那条边会被折断、错开、变陡 —— 眼睛就是靠
    // 这个判断"中间隔了一层东西"。一路柔到底的渐变位移过去前后一样，弯了也看不出来。
    drawBands(accent, w, h)

    drawFieldArcs(arcInk, w, h)
    drawBokeh(w, h)
    drawWeave(w, h, arcInk, PopGlass.WeaveSpacing.toPx(), PopGlass.WeaveStroke.toPx())
    drawRect(grain)
}

/**
 * 斜向色带。
 *
 * 角度之间刻意差得远（-27° / 38°），宽度也不同 —— 三条同向的带会连成一整片，
 * 三条等宽正交的带会读作格子布。宽度和色相都错开，才是"光穿过有色玻璃"的样子。
 */
private fun DrawScope.drawBands(accent: Color, w: Float, h: Float) {
    val bands = listOf(
        BandSpec(-27f, 0.20f, 168f, accent, PopGlass.FieldBandAlpha),
        BandSpec(-27f, 0.66f, 96f, Color(0xFF7FA6FF), PopGlass.FieldBandAlpha * 0.62f),
        BandSpec(38f, 0.46f, 132f, Color(0xFF8FD6C8), PopGlass.FieldBandAlpha * 0.52f),
    )
    bands.forEach { band ->
        val thickness = band.thickness.dp.toPx()
        val cy = band.cy * h
        val color = band.color.copy(alpha = band.alpha)
        // 实心段只占 6%，两侧各 7% 过渡 —— 边界可辨但不硬。
        val brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.40f to Color.Transparent,
            0.47f to color,
            0.53f to color,
            0.60f to Color.Transparent,
            1f to Color.Transparent,
        )
        rotate(band.degrees, pivot = Offset(w / 2f, cy)) {
            drawRect(
                brush = brush,
                topLeft = Offset(w / 2f - w, cy - thickness / 2f),
                size = Size(w * 2f, thickness),
            )
        }
    }
}

private data class BandSpec(
    val degrees: Float,
    val cy: Float,
    val thickness: Float,
    val color: Color,
    val alpha: Float,
)

/**
 * 光斑（bokeh）。
 *
 * 背景原来只有低频色团，玻璃压上去前后几乎没有差别 —— 这是"不通透"的真正原因：
 * 不是玻璃不够透，是**背后没有东西可透**。
 *
 * 色团是平滑渐变，被位移之后前后完全一样；光斑不同，它有一个相对实的核心和一圈
 * 干净的边，玻璃把它推开时，那个圆的形状会被拉长、缺口、错位 —— 一眼就能看出
 * "中间隔了一层东西"。这是整屏里唯一能证明折射存在的结构（另一个是织纹）。
 *
 * 为什么用固定位置而不是随机：随机每次截图都不同，Paparazzi 会变成噪声对比，
 * 而且卡片压在哪完全不可控。这五个位置是挑过的 —— 四角各一个保证卡片边界外
 * 总有光斑，中间那个给正文背后一点透光。
 */
private fun DrawScope.drawBokeh(w: Float, h: Float) {
    // (cx, cy, 半径相对宽度, 颜色, 核心透明度)
    val spots = listOf(
        BokehSpec(0.14f, 0.13f, 0.20f, Color(0xFFFFE3D2), 0.22f),
        BokehSpec(0.92f, 0.22f, 0.24f, Color(0xFFD8E4FF), 0.20f),
        BokehSpec(0.06f, 0.52f, 0.18f, Color(0xFFCDEFE4), 0.18f),
        BokehSpec(0.86f, 0.63f, 0.22f, Color(0xFFFFD9E6), 0.16f),
        BokehSpec(0.30f, 0.90f, 0.26f, Color(0xFFE6DCFF), 0.18f),
        BokehSpec(0.66f, 0.78f, 0.14f, Color(0xFFFFF0CC), 0.20f),
    )
    spots.forEach { spot ->
        val center = Offset(spot.cx * w, spot.cy * h)
        val radius = spot.radius * w
        // 核心一直实到 0.62 才开始衰减：边界越"硬"，被折射推开时越看得见。
        // 一路柔到底的渐变是雾，不是光斑。
        drawCircle(
            brush = Brush.radialGradient(
                0f to spot.color.copy(alpha = spot.alpha),
                0.62f to spot.color.copy(alpha = spot.alpha * 0.88f),
                1f to spot.color.copy(alpha = 0f),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

private data class BokehSpec(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float,
)

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
 * 一组线画两遍：深色（墨 5%）压在白色（22%）上，错开半个线宽。
 * 只画白色时，浅色背景上几乎看不见 —— 而浅色恰好是这个主题的主调；
 * 只画深色时，深色区域又会把线吃掉。一深一浅叠着，无论背景明暗都有参照。
 *
 * 角度取 -27°：与那条斜向光带同向，两者的走向叠在一起，线条不显得突兀。
 */
private fun DrawScope.drawWeave(
    w: Float,
    h: Float,
    ink: Color,
    spacing: Float,
    stroke: Float,
) {
    if (spacing <= 0f) return
    // 旋转之后要盖住整个屏幕，线段长度按对角线给
    val half = hypot(w, h)
    val light = Color.White.copy(alpha = PopGlass.WeaveAlpha)
    val dark = ink.copy(alpha = PopGlass.WeaveInkAlpha)
    rotate(-27f) {
        var y = -half
        while (y <= half) {
            drawLine(color = light, start = Offset(-half, y), end = Offset(half, y), strokeWidth = stroke)
            drawLine(
                color = dark,
                start = Offset(-half, y + stroke * 0.5f),
                end = Offset(half, y + stroke * 0.5f),
                strokeWidth = stroke * 0.6f,
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

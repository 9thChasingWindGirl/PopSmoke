package com.popsmoke.tracker.ui.foundation

import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RuntimeShader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopGlass
import com.popsmoke.tracker.design.skin
import kotlin.math.min

/**
 * 背景快照：一张清晰版 + 三档模糊版 + 一张明度地图。
 *
 * ## 为什么是三档模糊
 *
 * 玻璃的"厚"不是靠不透明度，是靠**背景被丢掉了多少高频**。而屏幕上的元素并不等重：
 *
 * - 页面里的分组卡片（div）只是被一层玻璃轻轻盖住，背后的壁纸还能辨认轮廓；
 * - 底栏和顶栏是真的浮在内容上方，背后可能是正在滚动的文字，必须糊得更狠。
 *
 * 如果两者共用一档模糊，"层级"就只能靠不透明度去补 —— 那又回到了白塑料条。
 * 所以这里备两档（[soft] / [heavy]），由调用方按层级选。
 *
 * 两档都是**半分辨率**：`BitmapShader` 用 localMatrix 缩放回屏幕坐标，
 * 省一半内存，且放大本身又多加了一层柔和 —— 模糊层本来就该是软的。
 *
 * ## 为什么要有明度地图
 *
 * 壁纸是照片，明度从 0.1 到 0.95 都有。同一套"白 18% + 白边"的参数，
 * 在雪山上是"看不见的卡片"，在夜景里是"一块发灰的脏玻璃"，在中间调上才刚好。
 * 唯一的解法是让玻璃**知道自己背后有多亮**。
 *
 * [luma] 是 16×32 的明度网格（全屏覆盖，每格一字节级精度），[meanLuma] 按矩形取均值。
 * 网格而不是逐像素，是因为它要便宜到能在每个面板的 draw 阶段调用 —— 一次几十次加法，
 * 而不是几千次像素读取。
 */
class PopBackdrop internal constructor(
    /** 屏幕尺寸的清晰背景。壁纸原图，或程序化极光。 */
    val image: ImageBitmap,
    /** div 级模糊（弱）：内容卡片、输入框、按钮。 */
    val soft: ImageBitmap,
    /** chrome 级模糊（强）：顶栏、底栏、菜单、弹窗。 */
    val heavy: ImageBitmap,
    /** 极小图：给渐进模糊的"近端"用，绘制时靠矩阵放大，几乎不占内存。 */
    val tiny: ImageBitmap,
    val widthPx: Float,
    val heightPx: Float,
    /** 明度网格，行优先。 */
    private val luma: FloatArray,
    /** null 表示这台设备/这个渲染环境不支持 AGSL，调用方应跳过折射层。 */
    val shader: RuntimeShader?,
) {

    internal val softShader: BitmapShader? = soft.bitmapShader(widthPx)
    internal val heavyShader: BitmapShader? = heavy.bitmapShader(widthPx)

    internal val paint = Paint()

    /** 给 BitmapShader 切片绘制用的画笔（必须开位图过滤，半分辨率图放大才不会出马赛克）。 */
    internal val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** 给 Compose 外部（PopChrome 的切片绘制）直接用的位图。 */
    val heavyBitmap: android.graphics.Bitmap get() = heavy.asAndroidBitmap()
    val softBitmap: android.graphics.Bitmap get() = soft.asAndroidBitmap()
    val tinyBitmap: android.graphics.Bitmap get() = tiny.asAndroidBitmap()

    /**
     * 矩形区域的平均明度（0 = 纯黑，1 = 纯白）。
     *
     * [rect] 用的是**根坐标系**，和玻璃面板 `positionInRoot` 拿到的值同一套。
     */
    fun meanLuma(rect: Rect): Float {
        if (widthPx <= 0f || heightPx <= 0f) return 0.62f
        val c0 = ((rect.left / widthPx) * LumaCols).toInt().coerceIn(0, LumaCols - 1)
        val c1 = ((rect.right / widthPx) * LumaCols).toInt().coerceIn(c0, LumaCols - 1)
        val r0 = ((rect.top / heightPx) * LumaRows).toInt().coerceIn(0, LumaRows - 1)
        val r1 = ((rect.bottom / heightPx) * LumaRows).toInt().coerceIn(r0, LumaRows - 1)
        var sum = 0f
        var n = 0
        for (row in r0..r1) {
            val base = row * LumaCols
            for (col in c0..c1) {
                sum += luma[base + col]
                n++
            }
        }
        return if (n == 0) 0.62f else sum / n
    }

    fun toneAt(rect: Rect): GlassTone = GlassTone(meanLuma(rect))
}

/**
 * 玻璃背后的明暗状况 —— 决定内辉光是"亮"还是"暗"。
 *
 * ## 历史教训：明度不能喂给填充
 *
 * 上一版把背后明度喂进了填充浓度（暗背景白底 ×2、浮层换深色）。
 * 每一步单独看都有理由，合起来的效果是**div 的颜色跟着壁纸走**：
 * 同一张设置页，滚过亮区和暗区，卡片明暗跟着变 —— 界面读起来像在闪，
 * 用户的第一反馈就是"改变了 div 的色相，很难看"。
 *
 * 现在的规矩是：**填充永远恒定中性，玻璃对明暗的全部回应都长在边缘上**
 * —— 一圈宽的内辉光（见 [glowColor]）。中央平板区纹丝不动，
 * 背后是什么颜色就是什么颜色。
 */
@Immutable
data class GlassTone(val luma: Float) {

    /** 暗背景：浮层文字转浅。 */
    val dark: Boolean get() = luma < 0.42f

    /** 过亮背景：白玻璃没有边界。 */
    val bright: Boolean get() = luma > 0.78f

    /** 面板上的主文字色。 */
    val ink: Color
        get() = if (dark) Color(0xFFF4F4F6) else Color(0xFF16161A)

    /** 面板上的次要文字色。 */
    val inkSecondary: Color
        get() = if (dark) Color(0xCCF4F4F6) else Color(0xFF6B6B72)

    /**
     * 内辉光颜色：**背景暗则明，背景明则暗**。
     *
     * - 暗背景 → 白辉光：玻璃的边缘把背后的光收进来，一圈亮环勾出轮廓，
     *   同时把边缘附近的字底垫亮 —— 这是"玻璃在夜里发光"的读法。
     * - 亮背景 → 深墨辉光：白玻璃压在雪山/晴空上没有边界，
     *   强光下的玻璃边界本来就是一圈折射造成的暗环，不是高光。
     *
     * 分界取 0.52 而不是 0.5：草地、木桌这类中间调（0.55~0.6）里，
     * 暗辉光比白辉光更不容易把面板读成"脏了一圈"。
     */
    val glowColor: Color
        get() = if (luma >= 0.52f) Color(0xFF101014) else Color(0xFFFFFFFF)

    /**
     * 内辉光强度（0..1）：中间调最低，越极端越强。
     *
     * 即使中间调也保留 0.35 的底强度 —— 那圈辉光是这块材质的**身份特征**
     * （参考实现的"白内辉 40px"），完全消失玻璃就退化成一块半透明矩形。
     * 极端明暗时再加码：夜景到 0.9，雪山到 0.85。
     */
    val glowStrength: Float
        get() = when {
            luma >= 0.52f -> (0.35f + (luma - 0.52f) / 0.48f * 0.50f).coerceIn(0f, 1f)
            else -> (0.35f + (0.52f - luma) / 0.52f * 0.55f).coerceIn(0f, 1f)
        }

    /** 菲涅尔线的强度倍数：暗底上白线更显眼，亮底上要收一点。 */
    fun edgeScale(): Float = if (dark) 1.25f else if (bright) 0.85f else 1f

    companion object {
        /** 没有背景快照时（极老设备、波普主题）的默认判断。 */
        val Light = GlassTone(0.62f)
    }
}

private fun ImageBitmap.bitmapShader(screenWidthPx: Float): BitmapShader? {
    if (screenWidthPx <= 0f) return null
    return BitmapShader(
        asAndroidBitmap(),
        android.graphics.Shader.TileMode.CLAMP,
        android.graphics.Shader.TileMode.CLAMP,
    ).apply {
        // 半分辨率图要缩回屏幕坐标，否则着色器会用"图的一半有多大"去算 uv，
        // 整个背景会向右上角挤成一小块。
        val sx = width.toFloat() / screenWidthPx
        if (sx != 1f) setLocalMatrix(Matrix().apply { setScale(sx, sx) })
    }
}

private const val LumaCols = 16
private const val LumaRows = 32

/**
 * 把"被折射过的背景"画到当前 DrawScope 上，裁到 [shape]。
 *
 * [origin] 是当前绘制的左上角在根坐标系中的位置 —— 卡片内的 fragCoord 是局部坐标，
 * 要采样全屏位图就得把它平移回去。这个值必须来自 `onGloballyPositioned`，猜不出来。
 *
 * 圆角半径从 `Outline` 里读，而不是外面传一个"大概的值"：形状和着色器一旦不一致，
 * 折射带就会和真实轮廓错开一两个像素，边缘看起来是毛的。
 */
internal fun DrawScope.drawRefracted(
    backdrop: PopBackdrop,
    origin: Offset,
    shape: Shape,
    bezel: Float,
    depth: Float,
    dispersion: Float,
    strong: Boolean,
) {    val s = backdrop.shader ?: return
    val bs = (if (strong) backdrop.heavyShader else backdrop.softShader) ?: return

    val outline = shape.createOutline(size, layoutDirection, this)
    val radius = when (outline) {
        is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
        is Outline.Rectangle -> 0f
        is Outline.Generic -> 0f
    }

    // 壁不能比形状本身还宽：胶囊按钮的整个高度都是壁，再宽就没有平板区了，
    // 剖面会被截断成"整块都在位移"，反而糊。
    //
    // 另一条约束是圆角半径：SDF 距离在圆角区域的最大值就是半径，壁宽一旦超过它，
    // 圆角中心就落在带内且 t 永远到不了 1，位移在角上留下一个不回零的残留 ——
    // 表现为四个角各多出一块楔形色斑，与旁边的直边接不上。
    //
    // 还有一条下界：壁宽要跟着元件长大。固定的 26dp 放在卡片（380dp）上是 7%，
    // 放在满屏面板（780dp）上只剩 3%，边缘那条折射带细得像描边，
    // 一屏大玻璃就退化成"磨砂贴膜"。
    val shortSide = min(size.width, size.height)
    val maxBezel = min(shortSide * 0.075f, radius * 0.9f).coerceAtLeast(0.01f)
    val floor = shortSide * 0.05f
    val b = maxOf(bezel, floor).coerceIn(0.01f, maxBezel)

    // 位移量必须跟着元件的短边走，不能是固定 dp。
    // 26dp 的位移放在 380dp 高的卡片上是 7%，是一道好看的折边；
    // 放在 56dp 高的按钮上就是 46%，整个按钮会糊成一团 —— 小元件要用小位移。
    s.setInputShader("iBackdrop", bs)
    s.setFloatUniform("iSize", size.width, size.height)
    s.setFloatUniform("iOrigin", origin.x, origin.y)
    s.setFloatUniform("iRadius", radius)
    s.setFloatUniform("iBezel", b)
    s.setFloatUniform("iDepth", depth.coerceAtMost(min(b * 1.9f, shortSide * 0.15f)))
    s.setFloatUniform("iDisperse", dispersion)

    val path = Path().apply { addOutline(outline) }
    backdrop.paint.shader = s
    clipPath(path) {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, backdrop.paint)
        }
    }
}

/**
 * 适度模糊的背景切片：给"安静的底"（分组卡片）用的轻量玻璃输入。
 *
 * 分组不走折射着色器（那是浮动层的表演），但也不该把锐利的壁纸直接压在白块底下 ——
 * 那正是"材质不理想"的另一半：卡片后面 34% 的白盖不住叶脉和文字，
 * 背景的高频全部透进来。切片用 soft 档（约 8px 模糊），高频没了，
 * 山脊和色带还在，玻璃"隔了一层"的感觉由此而来。
 *
 * 坐标：切片要采样自己在根坐标系里的那一块背景。画布平移 -[origin] 后，
 * 画布内坐标恰好等于根坐标，BitmapShader 的 localMatrix 负责把半分辨率
 * 位图对回屏幕 —— 和折射着色器里的 iOrigin 是同一套思路。
 */
internal fun DrawScope.drawBackdropSlice(backdrop: PopBackdrop, origin: Offset) {
    val shader = backdrop.softShader ?: return
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val save = nc.save()
        nc.translate(-origin.x, -origin.y)
        backdrop.slicePaint.shader = shader
        nc.drawRect(0f, 0f, size.width, size.height, backdrop.slicePaint)
        nc.restoreToCount(save)
    }
}

/**
 * 内辉光：玻璃对背后明暗的**唯一**回应方式。
 *
 * ## 为什么是辉光，不是改填充
 *
 * 上一版让填充浓度跟着背后明度走（暗背景白底 ×2、浮层换深色），
 * 结果是 div 的颜色跟着壁纸滚动不断变化 —— 用户的第一反馈就是
 * "改变了 div 的色相，很难看"。现在填充恒定，明暗补偿全部长在边缘：
 *
 * - 背景暗 → 白辉光：玻璃把背后的光收进边缘，一圈亮环勾出轮廓；
 * - 背景亮 → 深墨辉光：强光下玻璃的边界是一圈折射造成的暗环。
 *
 * 中央平板区不动。辉光带宽度跟短边走（[PopGlass.GlowBand]，封顶短边 22%），
 * 模糊用 BlurMaskFilter 在描边上做 —— 描边居中，外半圈被 clip 裁掉，
 * 剩下的就是一圈从轮廓向内衰减的光带。BlurMaskFilter 在硬件画布与
 * layoutlib 快照里都受支持（不受支持的是非文本的 setShadowLayer）。
 *
 * [outline] 必须和面板真实形状同源 —— 辉光贴着轮廓走，形状错一圈就露馅。
 */
internal fun DrawScope.drawInnerGlow(outline: Outline, tone: GlassTone) {
    val strength = tone.glowStrength
    if (strength <= 0.01f) return
    val path = Path().apply { addOutline(outline) }
    val shortSide = min(size.width, size.height)
    // 12dp 在卡片上是身份特征，在 28dp 的圆片上就是半块饼 —— 带宽跟元件走
    val band = with(density) { PopGlass.GlowBand.toPx() }.coerceAtMost(shortSide * 0.22f)
    val blur = band * 0.55f
    val glow = glowPaint.apply {
        style = android.graphics.Paint.Style.STROKE
        // 描边居中：外半圈会被裁掉，内侧留 band；blur 再吃掉一点边缘，补上余量
        strokeWidth = band * 2f + blur
        color = tone.glowColor
            .copy(alpha = (PopGlass.GlowMaxAlpha * strength).coerceIn(0f, 1f))
            .toArgb()
        maskFilter = glowMaskFilter(blur)
    }
    clipPath(path) {
        drawIntoCanvas { it.nativeCanvas.drawPath(path.asAndroidPath(), glow) }
    }
}

/** 辉光描边共用一支画笔；BlurMaskFilter 按半径分桶缓存，避免每帧分配。 */
private val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

private val glowFilters = HashMap<Int, BlurMaskFilter>()

private fun glowMaskFilter(radiusPx: Float): BlurMaskFilter {
    // 0.5px 精度分桶：辉光对模糊半径 ±1px 不敏感，但每帧 new 一个 filter 会抖内存
    val key = (radiusPx * 2f).toInt().coerceAtLeast(1)
    return glowFilters.getOrPut(key) {
        BlurMaskFilter(key / 2f, BlurMaskFilter.Blur.NORMAL)
    }
}

/**
 * 软阴影。
 *
 * 上一版用「把圆角矩形外扩 + 下移」来模拟投影，还让上边只扩 22%、下边扩 100%。
 * 这是两个错误叠在一起：
 *
 * 1. **外扩出来的形状和卡片不同形**，上窄下宽，读起来就是"投影挪了位"——
 *    用户一眼就看出来了。
 * 2. 外扩的圆角是 `r + grow`，而真实投影是 `r` 的模糊副本，圆角处宽出来的量
 *    和直边处不一样，四个角会露出不对称的月牙。
 *
 * 现在改成对着色器画：形状、圆角、偏移全部和卡片同源，模糊用 SDF 距离的一次
 * smoothstep 衰减。一次绘制，没有层，也就没有环。
 *
 * [lift] 是唯一的偏移量。光源在正上方，所以只有 y 方向，x 恒为 0 ——
 * 任何侧向偏移在这个主题里都没有物理依据，只会让面板看起来贴歪了。
 *
 * @return 是否真的画了。false 表示这个渲染环境没有 AGSL，调用方应走外扩降级。
 */
internal fun DrawScope.drawSoftShadow(
    shape: Shape,
    blur: Float,
    lift: Float,
    spread: Float,
    alpha: Float,
    color: Color = Color.Black,
): Boolean {
    if (alpha <= 0f || blur <= 0f) return true
    val s = compileShadowShader() ?: return false

    val outline = shape.createOutline(size, layoutDirection, this)
    val radius = when (outline) {
        is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
        is Outline.Rectangle -> 0f
        is Outline.Generic -> 0f
    }

    // 画布要比卡片大一圈，阴影才有地方落。drawBehind 的绘制不会被自动裁剪，
    // 所以直接往负坐标画即可。
    // 模糊半径也要跟着短边走：34dp 的模糊放在 380dp 的卡片上是柔和的落影，
    // 放在 56dp 的按钮上会糊成一团墨渍。
    val shortSide = min(size.width, size.height)
    val b = blur.coerceAtMost(shortSide * 0.35f).coerceAtLeast(0.5f)
    val pad = b * 1.6f + kotlin.math.abs(lift) + spread + 2f
    val left = -pad
    val top = -pad
    val right = size.width + pad
    val bottom = size.height + pad

    s.setFloatUniform("iHalf", size.width / 2f, size.height / 2f)
    s.setFloatUniform("iCenter", size.width / 2f, size.height / 2f)
    s.setFloatUniform("iRadius", radius)
    s.setFloatUniform("iBlur", b)
    s.setFloatUniform("iSpread", spread)
    s.setFloatUniform("iLift", lift)
    s.setFloatUniform("iAlpha", alpha)
    s.setFloatUniform("iColor", color.red, color.green, color.blue, 1f)

    val paint = shadowPaint.apply { shader = s }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRect(left, top, right, bottom, paint)
    }
    return true
}

/** 阴影不走圆角矩形叠加，所以不需要裁剪；这个 Paint 只承载 shader。 */
private val shadowPaint = Paint().apply { isAntiAlias = true }

/**
 * 生成背景快照。来源变化、尺寸变化时重算一次，其余时候复用同一套位图。
 *
 * [wallpaper] 为 null 时退回程序化极光 —— 首次启动还没联网的那几秒、
 * 或者用户处在无网环境，界面都不该是空白。
 */
@Composable
fun rememberBackdrop(
    wallpaper: ImageBitmap?,
    accent: Color,
    widthPx: Int,
    heightPx: Int,
): PopBackdrop? {
    val density = LocalDensity.current
    val grain: Brush = rememberGrainBrush(density.density)
    val canvasColor = skin.palette.canvas
    val arcInk = skin.palette.ink
    return remember(wallpaper, accent, widthPx, heightPx, canvasColor, arcInk) {
        if (widthPx <= 0 || heightPx <= 0) {
            null
        } else {
            val image = if (wallpaper != null) {
                cropCover(wallpaper, widthPx, heightPx)
            } else {
                val generated = ImageBitmap(widthPx, heightPx)
                val canvas = Canvas(generated)
                CanvasDrawScope().draw(
                    density = density,
                    layoutDirection = LayoutDirection.Ltr,
                    canvas = canvas,
                    size = Size(widthPx.toFloat(), heightPx.toFloat()),
                ) {
                    drawAuroraBody(accent, grain, canvasColor, arcInk)
                }
                generated
            }
            PopBackdrop(
                image = image,
                // 两档模糊：weak 给内容区（背后轮廓还认得出），strong 给浮层（背后是滚动文字）
                soft = blurLayer(image, widthPx, heightPx, short = 55),
                heavy = blurLayer(image, widthPx, heightPx, short = 22),
                tiny = blurLayer(image, widthPx, heightPx, short = 12),
                widthPx = widthPx.toFloat(),
                heightPx = heightPx.toFloat(),
                luma = lumaGrid(image),
                shader = compileRefractionShader(),
            )
        }
    }
}

/**
 * 按 cover 规则裁切铺满（等价 CSS `background-size: cover`）。
 *
 * 壁纸是横构图（Bing 出 16:9），屏幕是竖的。cover 会裁掉两侧，只留中间 ——
 * 这是壁纸类 App 的标准做法，也正好留下构图主体。拉伸填满（fitXY）会把人脸压扁，
 * 那种变形在任何材质下都救不回来。
 */
private fun cropCover(src: ImageBitmap, width: Int, height: Int): ImageBitmap {
    return try {
        val s = src.asAndroidBitmap()
        if (s.width == width && s.height == height) return src
        val out = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val scale = maxOf(width / s.width.toFloat(), height / s.height.toFloat())
        val dw = s.width * scale
        val dh = s.height * scale
        val left = (width - dw) / 2f
        val top = (height - dh) / 2f
        canvas.drawBitmap(
            s,
            null,
            RectF(left, top, left + dw, top + dh),
            android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG),
        )
        out.asImageBitmap()
    } catch (t: Throwable) {
        // 极少数渲染环境不支持位图缩放，退回原图：位置会偏，但不崩
        src
    }
}

/**
 * 用「缩到 1/N 再放大回来」做模糊。
 *
 * 不是高斯，但玻璃后面那种中频虚化本来就不需要精确的核 —— 需要的是**把高频抹平**。
 * 降采样时每个目标像素吃掉 N×N 个源像素，已经完成了最重要的那一步平均；
 * 双线性放大负责把边界接圆滑。
 *
 * ## 为什么返回半分辨率
 *
 * 它是给 `BitmapShader` 用的，而着色器按屏幕坐标采样。半尺寸图配一个 0.5 的
 * localMatrix 就能对上，省掉一半内存；放大时那点柔和正好是模糊层想要的。
 *
 * ## 为什么按短边而不是像素比例
 *
 * 换一块分辨率更高的屏，玻璃的厚度不该跟着变。
 *
 * ## 模糊半径必须和位移量同量级
 *
 * 这一条是被模拟出来的：位移 34dp 配 4px 的模糊，规则纹理（草叶、栅栏、织纹）
 * 会被跨过好几个周期采样，画面上读作**锯齿和断裂**，而不是"被压弯"。
 * 把同一张背景先模糊到 8~10px 再位移，同样的参数立刻变成平滑的色块推移 ——
 * 因为高频已经被拿掉了，剩下的只有可以被连续压缩的低频。
 *
 * 这也解释了为什么玻璃一定要"糊"：不是为了让背景变好看，
 * 是为了让"位移"这件事**可读**。糊得不够，折射就是噪点。
 *
 * @param short 短边除以这个数就是缩略图边长。数值越小糊得越狠：
 * 22 是浮层级（背后有滚动文字，必须糊开），55 是内容级（约 8px 模糊，
 * 刚好盖掉高频、留住山脊与地平线这种可以被推开的低频）。
 */
private fun blurLayer(image: ImageBitmap, width: Int, height: Int, short: Int): ImageBitmap {
    val factor = (minOf(width, height) / short).coerceIn(2, 64)
    if (factor <= 1) return image
    val outW = (width / 2).coerceAtLeast(1)
    val outH = (height / 2).coerceAtLeast(1)
    return try {
        val src = image.asAndroidBitmap()
        val out = android.graphics.Bitmap.createBitmap(outW, outH, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val rect = android.graphics.Rect(0, 0, outW, outH)
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

        // 两层叠着画，而不是只画一层重模糊。
        //
        // 单层会把**低频也一起压平** —— 色带的走向、明暗的大块结构全糊成一团，
        // 卡片里读作"脏"，而不是"隔着一层玻璃"。真玻璃丢掉的是高频，不是全部。
        // 所以：一层中模糊铺底，再用 55% 的轻模糊盖上去，把中低频的结构捞回来。
        val small = android.graphics.Bitmap.createScaledBitmap(
            src,
            (outW / factor).coerceAtLeast(1),
            (outH / factor).coerceAtLeast(1),
            true,
        )
        canvas.drawBitmap(small, null, rect, paint)

        val light = (factor / 3).coerceAtLeast(1)
        val fine = android.graphics.Bitmap.createScaledBitmap(
            src,
            (outW / light).coerceAtLeast(1),
            (outH / light).coerceAtLeast(1),
            true,
        )
        paint.alpha = (255 * 0.55f).toInt()
        canvas.drawBitmap(fine, null, rect, paint)

        out.asImageBitmap()
    } catch (t: Throwable) {
        // 极少数渲染环境不支持位图缩放，退回清晰版：少一层模糊，但不崩
        image
    }
}

/**
 * 明度网格：把整张背景缩到 16×32 再逐格取感知明度。
 *
 * 缩略图本身完成了"区域平均"，不需要在这里再做卷积 —— 一格对应屏幕上
 * 几十个 dp，玻璃面板拿到的就是这个尺度上的明暗，再细也没有意义。
 */
private fun lumaGrid(image: ImageBitmap): FloatArray {
    val grid = FloatArray(LumaCols * LumaRows) { 0.62f }
    return try {
        val small = android.graphics.Bitmap.createScaledBitmap(
            image.asAndroidBitmap(), LumaCols, LumaRows, true,
        )
        val pixels = IntArray(LumaCols * LumaRows)
        small.getPixels(pixels, 0, LumaCols, 0, 0, LumaCols, LumaRows)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16 and 0xFF) / 255f
            val g = (p shr 8 and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            // 感知明度（Rec.709），不是简单平均：绿色的草地和蓝色的天空
            // 算术平均差不多，人眼看起来差一倍。
            grid[i] = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
        }
        grid
    } catch (t: Throwable) {
        grid
    }
}

/**
 * AGSL 折射。API 33 以下没有 `RuntimeShader`，返回 null，调用方降级。
 *
 * ## 剖面：这一版把峰值挪到了贴边
 *
 * 上一版是两端归零的驼峰，峰值落在壁宽的 43% 处。在 30dp 的壁上，
 * 最强的那圈扰动在**离边框 13dp 的地方**，边框本身几乎不动 ——
 * 视觉上就是"一圈一圈"，和最早那版甜甜圈是同一个东西，只是换了实现层。
 *
 * 参考实现（archisvaze/liquid-glass）用的是 `h(t) = (1-(1-t)^4)^0.25` 的表面高度，
 * 位移取它的斜率：**轮廓处最陡、向内单调衰减到零**。也就是最强扰动贴着边框，
 * 玻璃的"折"发生在最外面那一两毫米里。
 *
 * 这里照抄那个形状：`fall = pow(1-t, 2)` 单调递减，再用 `anchor` 让 t≈0 处
 * 归零（否则边界内外是两张图硬拼，会露一条 1px 的缝）。3% 的壁宽约等于 1px，
 * 肉眼看就是"贴着边折"。
 */
private fun compileRefractionShader(): RuntimeShader? = try {
    RuntimeShader(
        """
        uniform shader iBackdrop;
        uniform float2 iSize;
        uniform float2 iOrigin;
        uniform float iRadius;
        uniform float iBezel;
        uniform float iDepth;
        uniform float iDisperse;

        float sdRoundRect(float2 p, float2 hs, float r) {
            float2 q = abs(p) - hs + r;
            return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - r;
        }

        half4 main(float2 fragCoord) {
            float2 hs = iSize * 0.5;
            float2 p = fragCoord - hs;
            float r = min(iRadius, min(hs.x, hs.y));

            float d = sdRoundRect(p, hs, r);

            // SDF 的中央差分梯度 = 真法线。直边垂直、圆角径向，一套公式通吃。
            float e = 1.0;
            float2 nrm = float2(
                sdRoundRect(p + float2(e, 0.0), hs, r) - sdRoundRect(p - float2(e, 0.0), hs, r),
                sdRoundRect(p + float2(0.0, e), hs, r) - sdRoundRect(p - float2(0.0, e), hs, r)
            );
            nrm = normalize(nrm + float2(1e-5, 1e-5));

            // t: 0 = 轮廓上，1 = 壁的内沿
            float t = clamp(-d / max(iBezel, 0.001), 0.0, 1.0);

            // 边界归零（1px 内到位），然后单调衰减 —— 单峰，没有第二个环。
            float anchor = smoothstep(0.0, 0.03, t);
            float fall = pow(1.0 - t, 2.0);
            float amt = iDepth * anchor * fall;

            // 向内取：边缘看到的是更靠里的背景，整块背景被压向轮廓，
            // 读作"这块料把后面的东西挤扁了"。
            float2 uv = fragCoord + iOrigin - nrm * amt;

            half4 base = iBackdrop.eval(uv);

            // 玻璃的材料色不是"盖一层白"，是"让颜色更浓一点"。
            // 全靠 body 的白色半透明来建立存在感，结果是整块面板被漂白，
            // 背景的暖色穿不过来 —— 看起来像磨砂塑料。真玻璃的作用是提纯：
            // 稍稍提高饱和、稍稍提亮，颜色反而比外面更"实"。
            half luma = dot(base.rgb, half3(0.2126, 0.7152, 0.0722));
            base.rgb = mix(half3(luma), base.rgb, 1.16) * 0.995;
            base.rgb = min(base.rgb, half3(1.0));

            // 明暗回应不在这里做：着色器只负责光学（折射 + 提纯 + 色散），
            // 玻璃对背后明暗的全部回应统一走 Compose 层的内辉光（见 drawInnerGlow）。
            // 以前这里有一项"贴轮廓压暗内沿"，和辉光叠画会出现两套边界。

            if (iDisperse <= 0.0) {
                return base;
            }
            // 色散：蓝光偏折更多、红光更少。三通道各取一次，只在边缘强对比处露彩。
            float2 dv = nrm * amt * iDisperse * 0.5;
            return half4(
                iBackdrop.eval(uv - dv).r,
                base.g,
                iBackdrop.eval(uv + dv).b,
                base.a
            );
        }
        """.trimIndent(),
    )
} catch (t: Throwable) {
    // API < 33 抛 NoClassDefFoundError；AGSL 语法错误会抛 IllegalArgumentException。
    // 两种情况都只是"这台设备做不了折射"，不该让界面挂掉 —— 降级成半透明面板，
    // 少一层光学，但绝不能白屏。
    null
}

private var shadowShaderCache: RuntimeShader? = null
private var shadowShaderResolved = false

/** 阴影着色器，进程内只编译一次。 */
private fun compileShadowShader(): RuntimeShader? {
    if (shadowShaderResolved) return shadowShaderCache
    shadowShaderResolved = true
    shadowShaderCache = try {
        RuntimeShader(
            """
            uniform float2 iCenter;
            uniform float2 iHalf;
            uniform float iRadius;
            uniform float iBlur;
            uniform float iSpread;
            uniform float iLift;
            uniform float iAlpha;
            uniform float4 iColor;

            float sdRoundRect(float2 p, float2 hs, float r) {
                float2 q = abs(p) - hs + r;
                return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - r;
            }

            half4 main(float2 fragCoord) {
                float2 hs = iHalf + iSpread;
                float2 p = fragCoord - iCenter - float2(0.0, iLift);
                float d = sdRoundRect(p, hs, iRadius + iSpread);
                // 轮廓内也留一点：玻璃是半透明的，光会透过去，投影本来就在下面。
                // 完全挖掉会让卡片浮着没有根；全留会让卡片发灰。折中在 0.35 处起坡。
                float a = 1.0 - smoothstep(-iBlur * 0.35, iBlur * 0.9, d);
                return half4(iColor.rgb, a * iAlpha);
            }
            """.trimIndent(),
        )
    } catch (t: Throwable) {
        null
    }
    return shadowShaderCache
}

/** 折射参数的默认值，集中在这里，方便整体调软/调硬。 */
object RefractionDefaults {
    fun bezel(density: Density): Float = with(density) { PopGlass.RefractBezel.toPx() }
    fun depth(density: Density): Float = with(density) { PopGlass.RefractDepth.toPx() }
    fun dispersion(density: Density): Float = PopGlass.DispersionAmount
}

/** 阴影参数的默认值。 */
object ShadowDefaults {
    fun blur(density: Density): Float = with(density) { PopGlass.ShadowBlur.toPx() }
    fun lift(density: Density): Float = with(density) { PopGlass.ShadowLift.toPx() }
    fun spread(density: Density): Float = with(density) { PopGlass.ShadowSpread.toPx() }
}

/**
 * 背景快照的传递通道。
 *
 * 背景在 `PopApp` 根部生成一次，所有玻璃面板从这里取。用 CompositionLocal 而不是
 * 逐层传参：`skinSurface` 被 20 多个组件调用，中间隔着好几层布局，一路传下去
 * 会把每一层的签名都污染成"必须知道有个背景"。
 *
 * 为 null 表示当前环境不做折射（波普主题、或设备不支持 AGSL），
 * 消费方一律当作"没有这一层"处理。
 */
val LocalPopBackdrop = staticCompositionLocalOf<PopBackdrop?> { null }

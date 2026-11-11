package com.popsmoke.tracker.ui.foundation

import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.RuntimeShader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopGlass
import com.popsmoke.tracker.design.skin
import kotlin.math.min

/**
 * 背景快照 + 折射着色器 + 软阴影着色器。
 *
 * ## 为什么是"重画一份"而不是"抓屏"
 *
 * Compose 里拿不到真实的后台模糊（`Modifier.blur` 与 `RenderEffect` 都只能作用于自身内容），
 * 也拿不到"我下面那一层画了什么"。但这里不需要 —— 背景是我们自己画的，
 * 用同一段代码（[drawAuroraBody]）再画一遍到离屏位图即可，像素级一致。
 *
 * 这条路成立的前提只有一个：屏幕和位图必须是同一个绘制函数。一旦出现第二份"近似的"
 * 实现，卡片边界必然会露出色差或纹理错位的接缝。
 *
 * ## 折射的物理模型（这一版重写的地方）
 *
 * 上一版把位移写成"贴边最大、向内平方衰减"，并在边缘叠了五个同心环
 * （暗 bevel → 宽淡光晕 → 窄亮环 → 暖色环 → 1dp 描边）。结果是**读作甜甜圈**：
 * 环与环之间的每一条边界都被眼睛抓到了，玻璃没做出来，先做出了一圈圈的年轮。
 *
 * 参考 iOS 26 / archisvaze / shuding 的实现后，模型换成**玻璃壁**：
 *
 * - 玻璃不是一块平板，是**边缘厚、中间薄**（或反过来，取决于造型）的一块料。
 * - 光线在壁的斜面上折射，横向走一段 `tanθ × 壁厚` 才出射，所以边缘会把背后的
 *   东西**压缩**成一条密集的带 —— 那才是"折射"，不是"描边"。
 * - 关键是：**位移在边界处必须回到 0**。壁厚在轮廓处为零，光在掠射方向出不去，
 *   采样点自然接回原处。上一版边界处位移最大，于是边界内外是两张不同的图被硬拼在一起，
 *   那条拼缝只能用描边去盖 —— 一层盖不住就加第二层，最后盖成了甜甜圈。
 *
 * 现在用驼峰剖面：`t=0` 位移 0 → 壁腰处最大 → 进入平板区回到 0。两端都连续，
 * 所以**不需要任何描边去遮掩**，边缘只剩一根 1dp 的菲涅尔亮线。
 *
 * 另外法线也换了：上一版用 `normalize(从中心指向外)`，在直边上碰巧对，在圆角处全错。
 * 现在用 SDF 的中央差分梯度 —— 直边、圆角、胶囊三种形状同一套公式解出的真法线。
 *
 * ## 版本
 *
 * `RuntimeShader` 是 API 33 才有的类。低版本拿到的是 `NoClassDefFoundError`，
 * 用 [PopBackdrop.shader] 为 null 表示"这台机器做不了折射"，调用方直接跳过这一层。
 */
class PopBackdrop internal constructor(
    val image: ImageBitmap,
    /**
     * 预模糊过的同一张背景。
     *
     * 这一张才是玻璃真正要采样的东西 —— 上一版直接采样清晰版，于是"玻璃"里躺着
     * 一幅锐利的背景，怎么调参数都像一块透明塑料板。
     *
     * 真实玻璃内部是**模糊**的：光线在材料里多次散射，高频细节最先丢掉。Apple 的
     * Liquid Glass 之所以一眼是玻璃，一半功劳在 backdrop blur，而不在边缘折射。
     * Compose 拿不到 backdrop-filter，所以在生成快照时就把模糊做掉 ——
     * 背景是静态的，这笔开销只付一次。
     */
    val blurred: ImageBitmap,
    val widthPx: Float,
    val heightPx: Float,
    /** null 表示这台设备/这个渲染环境不支持 AGSL，调用方应跳过折射层。 */
    val shader: RuntimeShader?,
) {
    internal val bitmapShader: BitmapShader? =
        if (shader == null) null else BitmapShader(
            blurred.asAndroidBitmap(),
            android.graphics.Shader.TileMode.CLAMP,
            android.graphics.Shader.TileMode.CLAMP,
        )

    internal val paint = Paint()
}

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
    peak: Float,
    dispersion: Float,
) {
    val s = backdrop.shader ?: return
    val bs = backdrop.bitmapShader ?: return

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
    // 还有一条下界：壁宽要跟着元件长大。固定的 34dp 放在卡片（380dp）上是 9%，
    // 放在满屏面板（780dp）上只剩 4%，边缘那条折射带细得像描边，
    // 一屏大玻璃就退化成"磨砂贴膜"。
    val shortSide = min(size.width, size.height)
    val maxBezel = min(shortSide * 0.42f, radius * 0.9f).coerceAtLeast(0.01f)
    val floor = shortSide * 0.055f
    val b = maxOf(bezel, floor).coerceIn(0.01f, maxBezel)

    // 位移量必须跟着元件的短边走，不能是固定 dp。
    // 28dp 的位移放在 380dp 高的卡片上是 7%，是一道好看的折边；
    // 放在 56dp 高的按钮上就是 50%，整个按钮会糊成一团 —— 小元件要用小位移。
    s.setInputShader("iBackdrop", bs)
    s.setFloatUniform("iSize", size.width, size.height)
    s.setFloatUniform("iOrigin", origin.x, origin.y)
    s.setFloatUniform("iRadius", radius)
    s.setFloatUniform("iBezel", b)
    s.setFloatUniform("iDepth", depth.coerceAtMost(min(b * 1.1f, shortSide * 0.16f)))
    s.setFloatUniform("iPeak", peak)
    s.setFloatUniform("iDisperse", dispersion)
    s.setFloatUniform("iInner", PopGlass.InnerShadeAlpha)

    val path = Path().apply { addOutline(outline) }
    backdrop.paint.shader = s
    clipPath(path) {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, backdrop.paint)
        }
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
 * 生成背景快照。尺寸变化、主题色变化时重算一次，其余时候复用同一张位图。
 *
 * 背景现在是静态的（见 [PopAurora] 的说明），所以这张位图永远不会过期 ——
 * 这是它能便宜到"只在首帧算一次"的原因。
 */
@Composable
fun rememberBackdrop(accent: Color, widthPx: Int, heightPx: Int): PopBackdrop? {
    val density = LocalDensity.current
    val grain: Brush = rememberGrainBrush(density.density)
    val canvasColor = skin.palette.canvas
    val arcInk = skin.palette.ink
    return remember(accent, widthPx, heightPx, canvasColor, arcInk) {
        if (widthPx <= 0 || heightPx <= 0) {
            null
        } else {
            val image = ImageBitmap(widthPx, heightPx)
            val canvas = Canvas(image)
            CanvasDrawScope().draw(
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                canvas = canvas,
                size = Size(widthPx.toFloat(), heightPx.toFloat()),
            ) {
                drawAuroraBody(accent, grain, canvasColor, arcInk)
            }
            PopBackdrop(
                image = image,
                blurred = blurDownsample(image, widthPx, heightPx),
                widthPx = widthPx.toFloat(),
                heightPx = heightPx.toFloat(),
                shader = compileRefractionShader(),
            )
        }
    }
}

/**
 * 用「缩到 1/N 再放大回来」做模糊。
 *
 * 不是高斯，但玻璃后面那种大范围虚化本来就不需要精确的核 —— 需要的是**把高频抹平**。
 * 降采样时每个目标像素吃掉 N×N 个源像素，已经完成了最重要的那一步平均；
 * 双线性放大负责把边界接圆滑。两次插值之后，颗粒、织纹、光斑边缘全部化成色块，
 * 只留下低频的明暗与色相 —— 正是透过一块厚玻璃该看到的东西。
 *
 * 为什么不在着色器里做多采样卷积：那样每帧每个像素都要几十次纹理读取，
 * 一屏好几块玻璃就是几十万次；而这笔开销只需要付一次（背景是静态的）。
 *
 * [factor] 取 12：在 1080p 上是 90×160 的缩略图，放大后模糊半径约 24px ≈ 一块
 * 20dp 厚的玻璃。再大就把背景压成纯色块了，玻璃里会没有东西可看。
 */
private fun blurDownsample(image: ImageBitmap, width: Int, height: Int): ImageBitmap {
    // 模糊半径按短边算，不按像素比例 —— 换一块分辨率更高的屏，玻璃的厚度不该跟着变。
    val heavy = (minOf(width, height) / 26).coerceIn(6, 40)
    val light = (heavy / 4).coerceAtLeast(2)
    return try {
        val src = image.asAndroidBitmap()
        val out = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val rect = android.graphics.Rect(0, 0, width, height)
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

        // 两层叠着画，而不是只画一层重模糊。
        //
        // 单层重模糊会把**低频也一起压平** —— 色带的走向、明暗的大块结构全糊成一团，
        // 卡片里读作"脏"，而不是"隔着一层玻璃"。真玻璃丢掉的是高频，不是全部。
        // 所以：重模糊铺底，再用 40% 的轻模糊盖上去，把中低频的结构捞回来。
        val small = android.graphics.Bitmap.createScaledBitmap(src, width / heavy, height / heavy, true)
        canvas.drawBitmap(small, null, rect, paint)

        val fine = android.graphics.Bitmap.createScaledBitmap(src, width / light, height / light, true)
        paint.alpha = (255 * 0.38f).toInt()
        canvas.drawBitmap(fine, null, rect, paint)

        out.asImageBitmap()
    } catch (t: Throwable) {
        // 极少数渲染环境不支持位图缩放，退回清晰版：少一层模糊，但不崩
        image
    }
}

/**
 * AGSL 折射。API 33 以下没有 `RuntimeShader`，返回 null，调用方降级。
 *
 * 剖面参数（全部可在 tokens.json 里调）：
 * - `iBezel` 壁宽。只有这么宽的一圈会弯折光线，中心平板区一动不动 ——
 *   这一点很重要，卡片的文字都在中间，中间一动字就糊了。
 * - `iDepth` 壁腰处的最大横向位移。比 `iBezel` 大一些是正常的：位移超过壁宽
 *   意味着边缘采样到了平板区的背景，背景被"折叠"进去，这正是厚玻璃的读法。
 * - `iPeak` 驼峰的落点在壁宽的哪个比例处。取小值（0.28）= 峰贴着轮廓，
 *   边缘的压缩带窄而烈，最像真玻璃。
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
        uniform float iPeak;
        uniform float iDisperse;
        uniform float iInner;

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
            // 上一版用 normalize(p)，在圆角处方向就是错的，那是"环"读感的一半来源。
            float e = 1.0;
            float2 nrm = float2(
                sdRoundRect(p + float2(e, 0.0), hs, r) - sdRoundRect(p - float2(e, 0.0), hs, r),
                sdRoundRect(p + float2(0.0, e), hs, r) - sdRoundRect(p - float2(0.0, e), hs, r)
            );
            nrm = normalize(nrm + float2(1e-5, 1e-5));

            // t: 0 = 轮廓上，1 = 壁的内沿
            float t = clamp(-d / max(iBezel, 0.001), 0.0, 1.0);

            // 驼峰。t=0 处位移必须为 0，否则边界内外是两张图硬拼，接缝只能靠描边盖，
            // 盖一层不够就盖第二层 —— 那就是上一版的甜甜圈。
            //
            // 两个斜率都要留够宽度。第一版把上升段放在 t 的前 8%（约 1.8px）里，
            // 位移在 1.8px 内从 0 冲到满 —— 画面上的表现就是一根线，不是折射带。
            // 带内的梯度必须落在肉眼可分辨的尺度上（十几像素），才读作"背景被压弯"。
            float rise = smoothstep(0.0, 0.42, t);
            float fall = 1.0 - smoothstep(max(iPeak, 0.45), 1.0, t);
            float amt = iDepth * rise * fall;

            // 向内取：边缘看到的是更靠里的背景，整块背景被压向轮廓，
            // 读作"这块料把后面的东西挤扁了"。
            float2 uv = fragCoord + iOrigin - nrm * amt;

            half4 base = iBackdrop.eval(uv);

            // 玻璃的材料色不是"盖一层白"，是"让颜色更浓一点"。
            // 上一版全靠 body 的白色半透明来建立存在感，结果是整块面板被漂白，
            // 背景的暖色穿不过来 —— 看起来像磨砂塑料。真玻璃的作用是提纯：
            // 稍稍提高饱和、稍稍提亮，颜色反而比外面更"实"。
            half luma = dot(base.rgb, half3(0.2126, 0.7152, 0.0722));
            base.rgb = mix(half3(luma), base.rgb, 1.18) * 0.995;
            base.rgb = min(base.rgb, half3(1.0));

            // 壁厚暗面：贴轮廓最暗，向内 20dp 左右散尽。
            //
            // 这一层和边缘亮线是一对：亮线在外、暗面在内，两条加起来才是"有厚度的壁"。
            // 但只能有这一层 —— 光照的衰减必须是单调的，中间不能再有第二个峰，
            // 否则就会回到"一圈一圈"。
            float inner = exp(-t * 3.4) * iInner;
            base.rgb = base.rgb * (1.0 - inner);

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
    const val Peak = 0.28f
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

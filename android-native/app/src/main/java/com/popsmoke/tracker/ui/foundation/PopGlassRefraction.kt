package com.popsmoke.tracker.ui.foundation

import android.graphics.BitmapShader
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopGlass
import com.popsmoke.tracker.design.skin
import kotlin.math.max

/**
 * 背景快照 + 折射着色器。
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
 * ## 着色器在做什么
 *
 * 只做一件事：**按到边界的距离把采样点往外推**。位移量在内部为 0、贴边处最大
 * （`1 - clamp(-d / band)` 再平方），这是透镜的定义 —— 中心不走样，边缘弯折。
 * 绝不能整块均匀位移，那样读作"图片错位"而不是"玻璃"。
 *
 * 色散是同一个位移按 RGB 分别取一次样，偏差小到只在边缘的强对比处露一点彩边。
 *
 * ## 版本
 *
 * `RuntimeShader` 是 API 33 才有的类。低版本拿到的是 `NoClassDefFoundError`，
 * 用 [PopBackdrop.shader] 为 null 表示"这台机器做不了折射"，调用方直接跳过这一层，
 * 退回纯粹的半透明面板 —— 会少一点光学真实感，但不会崩、也不会变成白块。
 */
class PopBackdrop internal constructor(
    val image: ImageBitmap,
    val widthPx: Float,
    val heightPx: Float,
    /** null 表示这台设备/这个渲染环境不支持 AGSL，调用方应跳过折射层。 */
    val shader: RuntimeShader?,
) {
    internal val bitmapShader: BitmapShader? =
        if (shader == null) null else BitmapShader(
            image.asAndroidBitmap(),
            android.graphics.Shader.TileMode.CLAMP,
            android.graphics.Shader.TileMode.CLAMP,
        )

    internal val paint = android.graphics.Paint()
}

/**
 * 把"被折射过的背景"画到当前 DrawScope 上，裁到 [shape]。
 *
 * [origin] 是当前绘制的左上角在根坐标系中的位置 —— 卡片内的 fragCoord 是局部坐标，
 * 要采样全屏位图就得把它平移回去。这个值必须来自 `onGloballyPositioned`，猜不出来。
 */
internal fun DrawScope.drawRefractedBackdrop(
    backdrop: PopBackdrop,
    origin: Offset,
    shape: Shape,
    radiusPx: Float,
    band: Float,
    refraction: Float,
    dispersion: Float,
) {
    val s = backdrop.shader ?: return
    val bs = backdrop.bitmapShader ?: return

    s.setInputShader("iBackdrop", bs)
    s.setFloatUniform("iSize", size.width, size.height)
    s.setFloatUniform("iOrigin", origin.x, origin.y)
    s.setFloatUniform("iRadius", radiusPx)
    s.setFloatUniform("iBand", band)
    s.setFloatUniform("iRefract", refraction)
    s.setFloatUniform("iDisperse", dispersion)

    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    backdrop.paint.shader = s
    clipPath(path) {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRect(
                0f, 0f, size.width, size.height, backdrop.paint,
            )
        }
    }
}

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
            val shader = compileRefractionShader()
            PopBackdrop(
                image = image,
                widthPx = widthPx.toFloat(),
                heightPx = heightPx.toFloat(),
                shader = shader,
            )
        }
    }
}

/**
 * AGSL 折射。API 33 以下没有 `RuntimeShader`，返回 null，调用方降级。
 *
 * 位移公式的推导（对照 Metal 版社区实现）：
 * 社区通用的是 `offset = toCenter * (1 - r²)`，即中心位移最大 —— 那是**放大镜**。
 * 一块厚玻璃板正好相反：光线在边缘弯曲得最厉害，正中间几乎是直的。而且中心不位移
 * 还有个实际好处 —— 卡片的文字都在中间，中心一动字就糊了。
 *
 * 所以这里用 `1 - clamp(-sdf / band)` 而不是 `1 - r²`：以形状边界为基准，
 * 而不是以几何中心为基准。圆角矩形、胶囊、任意 Shape 都能用同一套。
 */
private fun compileRefractionShader(): RuntimeShader? = try {
    RuntimeShader(
        """
        uniform shader iBackdrop;
        uniform float2 iSize;
        uniform float2 iOrigin;
        uniform float iRadius;
        uniform float iBand;
        uniform float iRefract;
        uniform float iDisperse;

        float sdRoundRect(float2 p, float2 boxSize, float r) {
            float2 q = abs(p) - boxSize + r;
            return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - r;
        }

        half4 main(float2 fragCoord) {
            float2 boxSize = iSize * 0.5;
            float2 p = fragCoord - boxSize;
            float d = sdRoundRect(p, boxSize, iRadius);

            // 越贴边位移越大；平方让过渡集中到最后一小段，读起来才像"边缘在弯"
            float t = 1.0 - clamp(-d / iBand, 0.0, 1.0);
            float amt = t * t * iRefract;

            // 法线：从中心指向外。中心处 normalize(0) 是 NaN，偏一点避开
            float2 nrm = normalize(p + float2(0.0001, 0.0001));

            float2 uv = fragCoord + iOrigin - nrm * amt;
            half4 c = iBackdrop.eval(uv);
            if (iDisperse > 0.0) {
                float2 dv = nrm * amt * iDisperse;
                return half4(
                    iBackdrop.eval(uv + dv).r,
                    c.g,
                    iBackdrop.eval(uv - dv).b,
                    c.a
                );
            }
            return c;
            half4 c = iBackdrop.eval(uv);

            if (iDisperse > 0.0) {
                float2 dv = nrm * amt * iDisperse;
                return half4(
                    iBackdrop.eval(uv + dv).r,
                    c.g,
                    iBackdrop.eval(uv - dv).b,
                    c.a
                );
            }
            return c;
        }
        """.trimIndent(),
    )
} catch (t: Throwable) {
    // API < 33 抛 NoClassDefFoundError；AGSL 语法错误会抛 IllegalArgumentException。
    // 两种情况都只是"这台设备做不了折射"，不该让界面挂掉。
    null
}

/** 折射参数的默认值，集中在这里，方便整体调软/调硬。 */
object RefractionDefaults {
    fun band(density: Density): Float = with(density) { 34.dp.toPx() }
    fun radius(density: Density): Float = with(density) { PopGlass.RadiusCard.toPx() }
    fun amount(density: Density): Float = with(density) { 22.dp.toPx() }
    const val Dispersion = 0.06f
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

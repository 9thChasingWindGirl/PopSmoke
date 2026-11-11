package com.popsmoke.tracker.ui.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.min
import com.popsmoke.tracker.design.PopGlass
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSkin
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.design.skin

/**
 * 面板语气：决定填充强度，不决定颜色本身。
 *
 * 组件说"我是一个卡片"或"我是一个需要压住下方内容的弹窗"，由皮肤给出具体色值。
 */
enum class PopPanelTone { Surface, Strong, Deep, Accent, Inverse, Clear }

@Composable
fun toneColor(tone: PopPanelTone): Color {
    val current = skin
    return when (tone) {
        PopPanelTone.Surface -> current.palette.surface
        PopPanelTone.Strong -> current.palette.surfaceStrong
        PopPanelTone.Deep -> current.palette.surfaceDeep
        PopPanelTone.Accent -> accent
        PopPanelTone.Inverse -> current.palette.inverseSurface
        PopPanelTone.Clear -> Color.Transparent
    }
}

/**
 * 面板外观的唯一入口：替掉原来的 `popShadow + background + popBorder` 三连。
 *
 * 为什么合成一个 draw 层：三连写法要创建三层绘制节点，而且顺序写反就会阴影盖住卡片
 * （这个坑之前真的踩过，靠截图像素采样才发现）。合成后顺序在函数内部固定：
 * 投影 → 填充 → 高光 → 折射 → 描边，外部不可能写错。
 *
 * 描边宽度会同时让出尺寸（对应 CSS 的 border-box），内容不会压到描边上。
 */
fun Modifier.skinSurface(
    current: PopSkin,
    shape: Shape,
    fill: Color,
    lift: Dp = if (current.glass) PopGlass.ShadowLift else PopShadow.PopLg,
    tint: Color = Color.Transparent,
): Modifier {
    val stroke = if (current.glass) 1.dp else PopStroke.Default
    return this
        .drawBehind { drawPanel(current, shape, fill, lift, tint, stroke.toPx()) }
        .padding(stroke)
}

/**
 * 只有描边、没有填充的面板（分隔块、虚线框容器）。
 *
 * 玻璃主题下退化成一条极淡的 hairline：磨砂面上再压 4dp 黑框会立刻回到波普味。
 */
fun Modifier.skinOutline(
    current: PopSkin,
    shape: Shape,
    width: Dp = PopStroke.Default,
): Modifier = this
    .drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        val color = if (current.glass) current.palette.hairline else current.palette.ink
        val w = if (current.glass) 1.dp.toPx() else width.toPx()
        drawOutlineShape(outline, color, Stroke(w))
    }
    .padding(if (current.glass) 1.dp else width)

/**
 * 果冻按压。
 *
 * 波普的反馈是"被压进纸面"（位移 + 硬阴影收缩），玻璃的反馈是"物体本身有弹性"：
 * 按下横向微涨、纵向压扁，松手带一点过冲回弹。两者不能混用 ——
 * 在磨砂玻璃上做位移会像图层错位，在贴纸上做缩放会像贴纸缩小了。
 *
 * 用 graphLayer 而不是改尺寸：不触发重新测量，只在绘制阶段改变换矩阵。
 */
@Composable
fun Modifier.jellyPress(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val active = pressed && enabled
    val spec = if (active) {
        spring<Float>(
            dampingRatio = PopGlass.JellyPressDamping,
            stiffness = PopGlass.JellyPressStiffness,
        )
    } else {
        // 松手用更低的阻尼，才有"回弹过头一点点"的果冻感
        spring<Float>(
            dampingRatio = PopGlass.JellyReleaseDamping,
            stiffness = PopGlass.JellyReleaseStiffness,
        )
    }
    // 变量不能叫 scaleX / scaleY：graphicsLayer 的 lambda 里同名成员优先级更高，
    // 会变成 `this.scaleX = this.scaleX`，动画永远不会生效。
    val squashX by animateFloatAsState(
        targetValue = if (active) PopGlass.JellyPressSquash else 1f,
        animationSpec = spec,
        label = "jelly-scale-x",
    )
    val squashY by animateFloatAsState(
        targetValue = if (active) PopGlass.JellyPressScale else 1f,
        animationSpec = spec,
        label = "jelly-scale-y",
    )
    return this.graphicsLayer {
        scaleX = squashX
        scaleY = squashY
    }
}

/** 圆角形状的便捷构造：一套皮肤 + 一个尺寸档。 */
fun PopSkin.cardShape(): Shape = RoundedCornerShape(radiusCard)
fun PopSkin.panelShape(): Shape = RoundedCornerShape(radiusPanel)
fun PopSkin.fieldShape(): Shape = RoundedCornerShape(radiusField)
fun PopSkin.chipShape(): Shape = RoundedCornerShape(radiusChip)
fun PopSkin.buttonShape(): Shape = RoundedCornerShape(radiusButton)

// ---------------------------------------------------------------- 绘制实现

private fun DrawScope.drawPanel(
    current: PopSkin,
    shape: Shape,
    fill: Color,
    lift: Dp,
    tint: Color,
    strokeWidth: Float,
) {
    val outline = shape.createOutline(size, layoutDirection, this)
    if (current.glass) {
        drawGlassPanel(outline, fill, tint, lift, strokeWidth)
    } else {
        drawInkPanel(outline, fill, current.palette.ink, lift, strokeWidth)
    }
}

/** 波普：零模糊硬阴影 + 实心填充 + 纯色粗描边。 */
private fun DrawScope.drawInkPanel(
    outline: Outline,
    fill: Color,
    ink: Color,
    lift: Dp,
    strokeWidth: Float,
) {
    val d = lift.toPx()
    if (d > 0f) {
        translate(left = d, top = d) { drawOutlineShape(outline, ink) }
    }
    if (fill.alpha > 0f) drawOutlineShape(outline, fill)
    if (strokeWidth > 0f) drawOutlineShape(outline, ink, Stroke(strokeWidth))
}

/**
 * 液态玻璃面板。
 *
 * 这个函数改过一次口径，值得记下来。第一版是"半透明填充 + 顶部渐变高光 + 上亮下暗描边"，
 * 也就是**磨砂玻璃（glassmorphism）**。它的定义性缺陷是：只**散射**光，不**弯折**光。
 * Apple 在 WWDC25 里把这个区别当成整套材质的分水岭 —— 旧材质散射，新材质
 * 实时弯曲、塑形、聚拢光线（Lensing）。所以那版无论怎么调参，看上去都是一块
 * 磨砂白卡片，而不是玻璃。
 *
 * 现在按真实玻璃的成像顺序分成四层，每层各自负责一件事：
 *
 * 1. **body**  —— 半透明填充。中心最透（光线直穿），越靠边越实（材料更厚）。
 *    用 radial 而不是 vertical：竖向渐变会让卡片"上下有色差"，而玻璃的透光率
 *    差异是由厚度决定的，厚度在边上。
 * 2. **rim**   —— 菲涅尔轮廓聚光。真实玻璃沿整条轮廓都在聚光，圆角处曲率大、
 *    切线变化快，反射最强，所以四个角最亮。做法是按轮廓描一圈宽环再裁进形状里：
 *    描边以路径为中心，内外各占一半，裁掉外侧后恰好剩一条向内的亮环。
 * 3. **specular** —— 左上角一团柔光。这是"房间里有一盏灯"的唯一证据，
 *    缺了它整块玻璃会显得没有体积，像贴纸。
 * 4. **stroke** —— 上亮下暗的 1dp 边。外沿最亮，向内一步就掉下去，
 *    这一条给出"表面的边界"，而 rim 给出"玻璃的厚度"，两者不能合并。
 *
 * 色散的处理是近似的：外环用偏冷的白（#F2F6FF），内环用偏暖的白（#FFF4E8）。
 * 真实色散是不同波长折射率不同导致的边缘彩虹分离，真正的实现要按 RGB 三个
 * 通道分别做位移采样；这里没有背景纹理可采样（见下），退而求其次，
 * 用外冷内暖的双环把"边缘有光谱倾向"这个印象给出来。
 *
 * 关于背景模糊：Compose 拿不到 backdrop blur（`Modifier.blur` 与 `RenderEffect`
 * 都只作用于自身内容）。但这一版不再依赖它 —— 背景是自己画的极光 + 光带，
 * 低频且解析可知，玻璃只需要"足够透"让它们穿过来，再靠边缘光学建立厚度。
 * 这比每帧采样一张全屏位图便宜得多，且在 Paparazzi 里可复现。
 */
private fun DrawScope.drawGlassPanel(
    outline: Outline,
    fill: Color,
    tint: Color,
    lift: Dp,
    strokeWidth: Float,
) {
    val bounds = outline.bounds
    val path = outline.asPath()
    val rimBand = PopGlass.RimBand.toPx()

    // lift 为 0 表示"贴着边缘的条"（顶栏/底栏），不该有投影，否则会像浮空错位
    if (lift > 0.dp) {
        // 关键：投影像素必须**挖掉卡片自己的区域**。
        // 阴影是向外扩的圆角块，天然包含卡片内部；以前面板足够不透明（alpha 0.5）
        // 所以看不出来，降到 0.3 后整块面板里透出一层灰 —— 看起来像玻璃脏了。
        // 真实物体不会在自己的投影里显影，这里用 Difference 裁一刀。
        clipPath(path, ClipOp.Difference) {
            drawSoftShadow(outline, Color(0xFF16161A), lift)
        }
    }

    // 中心到最远角的距离：用它当 radial 的 1f，四个角才刚好落在"最实"的那一端
    val span = hypot(bounds.width / 2f, bounds.height / 2f).coerceAtLeast(1f)

    // ---- 1. body ----
    if (fill.alpha > 0f) {
        drawOutlineShape(
            outline,
            Brush.radialGradient(
                0f to fill.copy(alpha = fill.alpha * 0.78f),
                0.72f to fill,
                1f to fill.copy(alpha = (fill.alpha * 1.24f).coerceAtMost(1f)),
                center = bounds.center,
                radius = span,
            ),
        )
    }

    // ---- 2. rim ----
    // clip 到形状内，把描边的外半边裁掉，只留向内的那条环。
    //
    // 环的亮度不能是均匀的。真实玻璃的轮廓高光由菲涅尔项决定：入射角越接近掠射、
    // 反射越强，再叠上"光从左上来"这个方向，结果就是左上最亮、右下几乎不亮。
    // 用 sweepGradient 以形状中心扫一圈，亮度按角度调制 —— 这是整块面板里
    // 最像玻璃的一笔，比填充和高光加起来都重要。
    val rimLight = Color(0xFFF2F6FF)
    // 环的亮度由菲涅尔项决定：掠射角（正好在轮廓上）反射最强，向内迅速衰减。
    // Compose 没有"沿路径的法向渐变"，所以用两层叠加来近似这条衰减曲线：
    // 一层宽的、很淡（散射光晕），一层窄的、很亮（边缘那道光）。
    // 只有宽层时，边缘是一圈均匀的白 —— 看着像发光边框，不像玻璃。
    fun sweep(strength: Float) = Brush.sweepGradient(
        0.000f to rimLight.copy(alpha = PopGlass.RimAlpha * strength * 0.34f), // 3 点方向
        0.125f to rimLight.copy(alpha = PopGlass.RimAlpha * strength * 0.12f), // 右下，最暗
        0.250f to rimLight.copy(alpha = PopGlass.RimAlpha * strength * 0.26f), // 6 点
        0.375f to rimLight.copy(alpha = PopGlass.RimAlpha * strength * 0.52f), // 左下
        0.500f to rimLight.copy(alpha = PopGlass.RimAlpha * strength * 0.80f), // 9 点
        0.625f to rimLight.copy(alpha = PopGlass.RimAlpha * strength),         // 左上，最亮
        0.750f to rimLight.copy(alpha = PopGlass.RimAlpha * strength * 0.72f), // 12 点
        0.875f to rimLight.copy(alpha = PopGlass.RimAlpha * strength * 0.40f), // 右上
        1.000f to rimLight.copy(alpha = PopGlass.RimAlpha * strength * 0.34f),
        center = bounds.center,
    )
    val rimWide = sweep(0.30f)
    val rimSharp = sweep(1f)
    val sharpWidth = PopGlass.Dispersion.toPx() // 1dp，只做"一条线"，不做"一圈边"

    clipPath(path) {
        // 内壁暗边：真实玻璃有厚度，光在壁内折射衰减，紧贴外沿的内侧会压暗一条。
        // 做法是先画一条更宽的暗环，再用亮环盖住它的外侧 —— 露出来的那 bevel 宽
        // 的内圈就是"壁厚"。少了这条，面板永远像一张没有厚度的薄膜。
        drawPath(
            path,
            Color(0xFF16161A).copy(alpha = PopGlass.BevelAlpha),
            style = Stroke(width = (rimBand + PopGlass.Bevel.toPx()) * 2f),
        )
        // 宽而淡：向内 ~9dp 的散射光晕
        drawPath(path, rimWide, style = Stroke(width = rimBand * 2f))
        // 窄而亮：真正贴着轮廓的那道反光
        drawPath(path, rimSharp, style = Stroke(width = sharpWidth * 2f))
        // 内层窄暖环：与外层冷白错开，读作边缘的色散
        drawPath(
            path,
            Color(0xFFFFF3E6).copy(alpha = PopGlass.RimAlpha * 0.30f),
            style = Stroke(width = rimBand * 0.9f),
        )
    }

    // ---- 3. specular ----
    // 光源固定在左上。半径压得比较小，否则整块卡片都会泛白、通透感全无
    val specRadius = min(bounds.width, bounds.height) * 0.18f
    clipPath(path) {
        drawRect(
            Brush.radialGradient(
                0f to Color.White.copy(alpha = PopGlass.SpecularAlpha),
                1f to Color.White.copy(alpha = 0f),
                center = Offset(
                    x = bounds.left + bounds.width * 0.16f,
                    y = bounds.top + bounds.height * 0.055f,
                ),
                radius = specRadius,
            ),
        )
    }

    // ---- 4. 底部折射带 ----
    // 玻璃有厚度，底沿会把背后的暖光带上来一点。alpha 必须很低，否则卡片会被染色
    if (tint.alpha > 0f) {
        drawOutlineShape(
            outline,
            Brush.verticalGradient(
                0.86f to tint.copy(alpha = 0f),
                1f to tint.copy(alpha = PopGlass.RefractionAlpha),
            ),
        )
    }

    // ---- 5. stroke ----
    // 取 rim 的一半宽度：外沿要亮，但要细，粗了就变成"边框"而不是"边缘反光"
    drawOutlineShape(
        outline,
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = PopGlass.StrokeTopAlpha),
            0.5f to Color.White.copy(alpha = 0.46f),
            1f to Color.White.copy(alpha = PopGlass.StrokeBottomAlpha),
        ),
        Stroke(strokeWidth),
    )
}

/** 把 Outline 转成 Path。rim 的裁切需要路径本身，而不只是能拿来填充的形状。 */
private fun Outline.asPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

/**
 * 柔和投影：两层扩散的圆角块。
 *
 * 不用 `Modifier.shadow`：它不支持 y 偏移，而"卡片浮在表面上"必须有下方的偏移；
 * 也不用 `Paint.setShadowLayer`，那在硬件加速下对非文本不可靠。
 * 两层（大而淡 + 小而实）在浅底上已经看不出硬边，成本只有两次绘制。
 */
internal fun DrawScope.drawSoftShadow(outline: Outline, ink: Color, lift: Dp) {
    val grow = PopGlass.ShadowSpread.toPx() * 0.16f
    val dy = lift.toPx()
    // 只画"大而淡"的一层。
    // 早先是两层（大而淡 + 小而实），结果贴边那层在浅色背景上画出一圈灰线，
    // 看起来像卡片的描边而不是投影 —— 玻璃卡片的边缘必须是干净的白色高光。
    // 上方几乎不外扩（0.22），光从上面来，物体上沿不该有暗边；
    // 下方完整外扩并下移，才有"浮起来"的落影
    val wide = outline.expanded(grow, dy, topGrowFactor = 0.22f)
    if (wide != null) {
        drawOutlineShape(wide, ink.copy(alpha = PopGlass.ShadowAlpha * 0.9f))
    }
}

/** 把轮廓向外扩一圈并下移。[Outline.Generic] 无法安全扩张（任意路径），退回不画。 */
private fun Outline.expanded(grow: Float, dy: Float, topGrowFactor: Float = 1f): Outline? = when (this) {
    is Outline.Rectangle -> Outline.Rectangle(
        Rect(
            left = rect.left - grow,
            top = rect.top - grow * topGrowFactor + dy,
            right = rect.right + grow,
            bottom = rect.bottom + grow + dy,
        ),
    )

    is Outline.Rounded -> Outline.Rounded(
        RoundRect(
            left = roundRect.left - grow,
            top = roundRect.top - grow * topGrowFactor + dy,
            right = roundRect.right + grow,
            bottom = roundRect.bottom + grow + dy,
            cornerRadius = CornerRadius(
                roundRect.topLeftCornerRadius.x + grow,
                roundRect.topLeftCornerRadius.y + grow,
            ),
        ),
    )

    is Outline.Generic -> null
}

/** 按 Outline 类型分派绘制。Compose 没有公开的 drawOutline，三个基础能力足够覆盖我们的形状。 */
private fun DrawScope.drawOutlineShape(outline: Outline, color: Color, style: DrawStyle = Fill) {
    when (outline) {
        is Outline.Rectangle -> drawRect(
            color = color,
            topLeft = outline.rect.topLeft,
            size = outline.rect.size,
            style = style,
        )

        is Outline.Rounded -> drawRoundRect(
            color = color,
            topLeft = Offset(outline.roundRect.left, outline.roundRect.top),
            size = Size(outline.roundRect.width, outline.roundRect.height),
            cornerRadius = outline.roundRect.topLeftCornerRadius,
            style = style,
        )

        is Outline.Generic -> drawPath(outline.path, color, style = style)
    }
}

private fun DrawScope.drawOutlineShape(outline: Outline, brush: Brush, style: DrawStyle = Fill) {
    when (outline) {
        is Outline.Rectangle -> drawRect(
            brush = brush,
            topLeft = outline.rect.topLeft,
            size = outline.rect.size,
            style = style,
        )

        is Outline.Rounded -> drawRoundRect(
            brush = brush,
            topLeft = Offset(outline.roundRect.left, outline.roundRect.top),
            size = Size(outline.roundRect.width, outline.roundRect.height),
            cornerRadius = outline.roundRect.topLeftCornerRadius,
            style = style,
        )

        is Outline.Generic -> drawPath(outline.path, brush, style = style)
    }
}

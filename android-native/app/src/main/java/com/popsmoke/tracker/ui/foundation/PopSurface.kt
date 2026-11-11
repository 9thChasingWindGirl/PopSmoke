package com.popsmoke.tracker.ui.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * 投影 → 折射 → 体 → 高光 → 边缘线，外部不可能写错。
 *
 * 描边宽度会同时让出尺寸（对应 CSS 的 border-box），内容不会压到描边上。
 */
fun Modifier.skinSurface(
    current: PopSkin,
    shape: Shape,
    fill: Color,
    lift: Dp = if (current.glass) PopGlass.ShadowLift else PopShadow.PopLg,
    tint: Color = Color.Transparent,
    /**
     * 是否按背后明暗**改变颜色方向**（压白 / 压暗）。
     *
     * 只有真正的浮层开这个：顶栏、底栏。它们的文字可以跟着一起换色，
     * 所以玻璃本身也敢变成深色。
     *
     * 内容区的分组**不能**开：里面的字是固定的深色，底板一旦压暗就没法读。
     * 那里只做 alpha 补偿（见 [GlassTone.contentAlphaScale]）—— 同样是白，
     * 在夜景上更实，在雪山上更实，但永远是白的。
     */
    adaptive: Boolean = false,
    /** 用哪一档背景模糊。浮层用 true（更糊），内容区用 false。 */
    strongBlur: Boolean = false,
): Modifier {
    val stroke = if (current.glass) PopGlass.EdgeStroke else PopStroke.Default
    if (!current.glass) {
        return this
            .drawBehind { drawPanel(current, shape, fill, lift, tint, stroke.toPx(), null, Offset.Zero, false, false) }
            .padding(stroke)
    }
    // 玻璃面板要在自己区域里重画一遍"被弯折过的背景"，为此必须知道自己在根坐标系
    // 里的位置 —— fragCoord 是卡片局部坐标，采样全屏位图得先平移回去。这个值猜不出来，
    // 只能靠 onGloballyPositioned 拿。
    return composed {
        val backdrop = LocalPopBackdrop.current
        val origin = remember { mutableStateOf(Offset.Zero) }
        this
            .onGloballyPositioned { coords -> origin.value = coords.positionInRoot() }
            .drawBehind {
                drawPanel(
                    current, shape, fill, lift, tint, stroke.toPx(),
                    backdrop, origin.value, adaptive, strongBlur,
                )
            }
            .padding(stroke)
    }
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
    backdrop: PopBackdrop?,
    origin: Offset,
    adaptive: Boolean,
    strongBlur: Boolean,
) {
    val outline = shape.createOutline(size, layoutDirection, this)
    if (current.glass) {
        drawGlassPanel(
            outline, shape, fill, tint, lift, strokeWidth,
            backdrop, origin, adaptive, strongBlur,
        )
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
 * ## 这一版把边缘拆掉了
 *
 * 上一版按"真实玻璃的成像顺序"分了四层：body / rim / specular / stroke。
 * 每一层的理由单独看都成立，叠起来的结果是**五道同心环**：
 * 宽暗环（壁厚）→ 宽淡亮环（散射光晕）→ 窄亮环（轮廓反光）→ 暖色环（假色散）
 * → 1dp 上亮下暗描边。环与环之间的每条边界都被眼睛抓到了，于是面板读作甜甜圈。
 *
 * 教训：**玻璃的边界只有一根线**。所有"厚度感"都该由折射（背景被压弯）给出，
 * 而不是由一圈圈描边画出来。所以这里只剩：
 *
 * 1. **projection** —— 同形、垂直偏移、一次绘制的软阴影（见 [drawSoftShadow]）。
 * 2. **refraction** —— 背景在边缘 22dp 内被压弯（见 [drawRefracted]）。
 * 3. **body** —— 一层极淡的对角渐变。左上迎光略亮、右下背光略暗，
 *    这不是"渐变装饰"，是唯一的体积线索。
 * 4. **tint** —— 底部 12% 的一点点暖光，玻璃把背后的光带上来了。
 * 5. **edge** —— 一根 1dp 的菲涅尔亮线。左上最亮、右下几乎消失。
 *
 * 色散不再用彩色环假装，交给着色器按 RGB 分别位移（真色散只在强对比处才露彩）。
 *
 * ## 关于背景模糊
 *
 * Compose 拿不到 backdrop blur（`Modifier.blur` 与 `RenderEffect` 都只作用于自身内容）。
 * 但这一版不需要 —— 折射本身就是"对背景的一次重采样"，比模糊更接近玻璃的定义。
 */
private fun DrawScope.drawGlassPanel(
    outline: Outline,
    shape: Shape,
    fill: Color,
    tint: Color,
    lift: Dp,
    strokeWidth: Float,
    backdrop: PopBackdrop?,
    origin: Offset,
    adaptive: Boolean,
    strongBlur: Boolean,
) {
    val bounds = outline.bounds
    val ink = Color(0xFF14141A)

    // 这块玻璃背后有多亮。截到屏幕外（滚动时）用默认值，不会因为越界而拿不到色调。
    val rect = androidx.compose.ui.geometry.Rect(
        origin.x,
        origin.y,
        origin.x + size.width,
        origin.y + size.height,
    )
    val tone = backdrop?.toneAt(rect) ?: GlassTone.Light

    // ---- 0. 投影 ----
    // lift 为 0 表示"贴着边缘的条"（顶栏/底栏），不该有投影，否则会像浮空错位。
    //
    // 暗背景上要再收一半：照片的暗部本来就没有"落下阴影"的余地，
    // 压一层黑只会让面板边缘糊成脏边。
    if (lift > 0.dp) {
        val blur = ShadowDefaults.blur(this)
        val spread = ShadowDefaults.spread(this)
        val alpha = PopGlass.ShadowAlpha * if (tone.dark) 0.55f else 1f
        val drawn = drawSoftShadow(shape, blur, lift.toPx(), spread, alpha)
        if (!drawn) {
            // 没有 AGSL 的环境：退回一层外扩。注意**不加 topGrowFactor** ——
            // 上边少扩、下边多扩会让投影形状和卡片不同形，那正是"投影挪位"的来源。
            val grown = outline.expanded(spread + blur * 0.28f, lift.toPx())
            if (grown != null) {
                drawOutlineShape(grown, ink.copy(alpha = alpha * 0.7f))
            }
        }
    }

    // ---- 1. 折射 ----
    if (backdrop != null) {
        drawRefracted(
            backdrop = backdrop,
            origin = origin,
            shape = shape,
            bezel = RefractionDefaults.bezel(this),
            depth = RefractionDefaults.depth(this),
            dispersion = RefractionDefaults.dispersion(this),
            tone = tone,
            strong = strongBlur,
        )
    }

    // ---- 2. body ----
    // 对角渐变，左上亮右下暗。为什么不是"中心最透"的径向：
    // 径向会在边缘形成一圈比中心更实的边 —— 那又是一道环，只是分辨率低一点。
    //
    // 幅度要小。真机的观感里，"脏"几乎全部来自这层：背景被模糊之后再压一层白、
    // 白本身还有明暗坡度，读起来就是一块擦不干净的玻璃。体积交给折射去表达 ——
    // 边缘被压弯的地方自然会有明暗，不需要在整个面上再刷一道渐变。
    //
    // 颜色方向上，这里分两种元件（见 [GlassTone]）：
    // - 浮层（adaptive）：壁纸暗就换深色，文字一起换浅色 —— iOS 深色 tab bar 的做法；
    // - 内容（非 adaptive）：永远是白，只调浓度。里面是深色字，压暗就等于删掉内容。
    val body = if (adaptive) tone.adaptFill(fill) else fill.scaledForContent(tone)
    if (body.alpha > 0f) {
        val a = body.alpha
        val softness = (1f - a).coerceIn(0f, 1f)
        drawOutlineShape(
            outline,
            Brush.linearGradient(
                0f to body.copy(alpha = (a * (1f + 0.10f * softness)).coerceAtMost(1f)),
                0.45f to body,
                1f to body.copy(alpha = a * (1f - 0.14f * softness)),
                start = Offset(bounds.left, bounds.top),
                end = Offset(bounds.right, bounds.bottom),
            ),
        )
        // 实心元件（主按钮、选中态）在玻璃主题里不能只是"一块颜色"。
        // iOS 的实心控件都有同一条规则：表面上有一道从上往下的反射，
        // 上沿最亮、很快衰减到无。少了它，按钮会像贴纸贴在玻璃上 ——
        // 两套材质的对比太生硬，整屏读作"玻璃卡片 + 塑料按钮"。
        if (a >= 0.9f) {
            drawOutlineShape(
                outline,
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = PopGlass.SolidSheenAlpha),
                    0.40f to Color.White.copy(alpha = PopGlass.SolidSheenAlpha * 0.22f),
                    1f to Color.Transparent,
                ),
            )
        }
    }

    // ---- 3. tint 底带 ----
    // 玻璃有厚度，底沿会把背后的光带上来一点。alpha 必须很低，否则卡片会被染色。
    if (tint.alpha > 0f) {
        drawOutlineShape(
            outline,
            Brush.verticalGradient(
                0.88f to tint.copy(alpha = 0f),
                1f to tint.copy(alpha = PopGlass.TintAlpha),
            ),
        )
    }

    // ---- 4. edge ----
    // 整块面板上唯一允许被叫作"边框"的东西：一根 1dp 的线，一半在形状内、一半在外。
    // 亮度由菲涅尔项决定 —— 掠射（轮廓上）最强，再叠"光从左上来"，所以左上最亮。
    //
    // 亮背景（雪山、晴空）上白线会彻底消失，那时改用一根极淡的暗线：
    // 玻璃在强光下的边界是"一圈折射造成的暗边"，不是高光。
    //
    // 强度还要跟填充的透明度挂钩。玻璃的亮边是"表面反射"，越透明越显眼；
    // 一块实心橙色的按钮如果也镶一圈白边，那读作"贴纸描边"，
    // 是波普语言误闯进玻璃主题 —— 必须按透明度衰减掉。
    val solidity = body.alpha.coerceIn(0f, 1f)
    val edgeScale = (1f - solidity * 0.5f) * tone.edgeScale()
    // 亮背景上的玻璃边缘是"暗边"（光被折走，那圈比周围略暗），不是高光。
    // 阈值取 0.58 而不是 0.78：草地、木桌这种中间调已经是亮线看不见的区间了，
    // 而 0.78 只覆盖雪山，等于把一半的照片交给一条不存在的白线。
    val darkEdge = tone.luma > 0.58f
    val peak = (if (darkEdge) PopGlass.EdgePeakAlpha * 0.26f else PopGlass.EdgePeakAlpha) * edgeScale
    val minA = (if (darkEdge) PopGlass.EdgeMinAlpha * 1.6f else PopGlass.EdgeMinAlpha) * edgeScale
    val edgeColor = if (darkEdge) Color(0xFF101014) else Color.White
    fun at(v: Float) = minA + (peak - minA) * v
    val edge = Brush.sweepGradient(
        0.000f to edgeColor.copy(alpha = at(0.42f)), // 3 点
        0.125f to edgeColor.copy(alpha = minA),      // 右下，最暗
        0.250f to edgeColor.copy(alpha = at(0.22f)), // 6 点
        0.375f to edgeColor.copy(alpha = at(0.52f)), // 左下
        0.500f to edgeColor.copy(alpha = at(0.78f)), // 9 点
        0.625f to edgeColor.copy(alpha = peak),      // 左上，最亮
        0.750f to edgeColor.copy(alpha = at(0.66f)), // 12 点
        0.875f to edgeColor.copy(alpha = at(0.34f)), // 右上
        1.000f to edgeColor.copy(alpha = at(0.42f)),
        center = bounds.center,
    )
    if (strokeWidth > 0f) {
        drawOutlineShape(outline, edge, Stroke(strokeWidth))
    }
}

/**
 * 内容区填充：只调浓度，不改颜色。
 *
 * 白 18% 压在夜景照片上大概是 70 的灰，深字压上去等于没有；×3 到 0.54 之后
 * 读作"一块半透明的白卡"，壁纸的轮廓还在，字也读得清。
 *
 * 只对**浅色**填充生效：次要按钮是一层 ink 5%（深色），比例放大只会把它涂成
 * 一块黑斑；危险键的淡红同理 —— 它们是"内容的一部分"，不是压住背景的板。
 *
 * 实心元件跳过：它们的 alpha 就是它们的设计意图。
 */
private fun Color.scaledForContent(tone: GlassTone): Color {
    if (alpha <= 0.001f || alpha >= 0.9f) return this
    if (luminance() < 0.7f) return this
    return copy(alpha = (alpha * tone.contentAlphaScale()).coerceAtMost(0.96f))
}

/**
 * 柔和投影：同形、垂直偏移的模糊块。
 *
 * 不用 `Modifier.shadow`（不支持 y 偏移），也不用 `Paint.setShadowLayer`
 * （硬件加速下对非文本不可靠）。用 AGSL 在 [drawSoftShadow] 里一次画完，
 * 形状和卡片完全同源 —— 投影不可能"挪位"。
 *
 * [Outline.Generic] 无法安全扩张（任意路径），降级路径直接不画。
 */
private fun Outline.expanded(grow: Float, dy: Float): Outline? = when (this) {
    is Outline.Rectangle -> Outline.Rectangle(
        Rect(
            left = rect.left - grow,
            top = rect.top - grow + dy,
            right = rect.right + grow,
            bottom = rect.bottom + grow + dy,
        ),
    )

    is Outline.Rounded -> Outline.Rounded(
        RoundRect(
            left = roundRect.left - grow,
            top = roundRect.top - grow + dy,
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

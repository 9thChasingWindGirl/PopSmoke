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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
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
 * 液态玻璃：柔和投影 + 半透明填充 + 顶部镜面高光 + 底部折射 + 亮面描边。
 *
 * 关于"背景模糊"：真正的 backdrop blur 在 Compose 里拿不到（`Modifier.blur` 只模糊自身内容，
 * `RenderEffect` 同理）。但这里不需要它 —— 玻璃背后的东西是我们自己画的极光光斑，
 * 本身就是低频的柔和渐变，没有需要被模糊的高频细节。半透明填充 + 高光边
 * 已经能得到同样的通透感，而且不用在每帧采样一张全屏位图。
 */
private fun DrawScope.drawGlassPanel(
    outline: Outline,
    fill: Color,
    tint: Color,
    lift: Dp,
    strokeWidth: Float,
) {
    // lift 为 0 表示"贴着边缘的条"（顶栏/底栏），不该有投影，否则会像浮空错位
    if (lift > 0.dp) {
        drawSoftShadow(outline, Color(0xFF16161A), lift)
    }

    if (fill.alpha > 0f) {
        drawOutlineShape(
            outline,
            Brush.verticalGradient(
                0f to fill.copy(alpha = (fill.alpha + 0.07f).coerceAtMost(1f)),
                0.55f to fill,
                1f to fill.copy(alpha = (fill.alpha - 0.05f).coerceAtLeast(0f)),
            ),
        )
    }

    // 顶部镜面高光：整块面板从顶部的白色 42% 迅速衰减，模拟光打在玻璃上沿
    drawOutlineShape(
        outline,
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = PopGlass.SheenAlpha),
            0.48f to Color.White.copy(alpha = 0f),
        ),
    )

    // 底部折射：极窄的一条带，带一点当前主题色。这是"玻璃有厚度"的关键暗示，
    // 少了它面板会像一层半透明糊纸。alpha 必须很低，否则整块卡片会染色。
    if (tint.alpha > 0f) {
        drawOutlineShape(
            outline,
            Brush.verticalGradient(
                0.84f to tint.copy(alpha = 0f),
                1f to tint.copy(alpha = PopGlass.RefractionAlpha),
            ),
        )
    }

    // 描边：上亮下暗。真实玻璃的边缘不是均匀一条线，而是上沿受光、下沿背光。
    // 这一条比填充更决定"看起来像不像玻璃"。
    drawOutlineShape(
        outline,
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = PopGlass.StrokeTopAlpha),
            0.5f to Color.White.copy(alpha = 0.5f),
            1f to Color.White.copy(alpha = PopGlass.StrokeBottomAlpha),
        ),
        Stroke(strokeWidth),
    )
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

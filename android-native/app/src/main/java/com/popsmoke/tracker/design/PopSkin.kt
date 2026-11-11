package com.popsmoke.tracker.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 两套视觉语言。
 *
 * 为什么要有这一层：波普的视觉参数（4dp 黑描边、零模糊硬阴影、纸白底、直角）
 * 原先散写在三十多个组件里，每处都是 `PopColor.Core.Black` 直接落地。
 * 想再长出一套液态玻璃，如果靠在每个组件里写 `if (style == ...)`，
 * 三十多个文件各长出一份分支，两套主题会慢慢互相污染——改一处只对一半组件生效。
 *
 * 所以组件只说"我是一个面板 / 一个按钮 / 一个输入框"，由皮肤决定它长什么样。
 * 换主题等于换一个 PopSkin，页面代码零改动。
 */
enum class PopThemeStyle(val id: String) {
    /** 液态玻璃：磨砂通透、光感流动、果冻回弹。默认。 */
    Liquid("liquid"),

    /** 波普漫画：粗黑描边、硬阴影、纸面点阵。 */
    Pop("pop");

    companion object {
        val Default = Liquid

        /** 未知值一律退回默认主题：宁可样式不对，也不要因为一条脏数据白屏。 */
        fun fromId(id: String?): PopThemeStyle =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: Default
    }
}

/**
 * 语义色板。组件只认这些名字，不认具体色值。
 *
 * 名字都是**语义**而不是外观（ink / surface / hairline），这样同一份组件代码
 * 在两套主题下取值不同但含义不变。如果这里出现 "black" / "paper" 之类的名字，
 * 玻璃主题就只能被迫填黑色，语义层也就白做了。
 */
@Immutable
data class PopPalette(
    /** 页面最底层。玻璃主题下会被极光光斑覆盖，此处只是兜底色。 */
    val canvas: Color,
    /** 卡片、面板、按钮的填充。玻璃主题下是半透明白，让背景的光透上来。 */
    val surface: Color,
    /** 需要压住下方内容的面板（弹窗、菜单），比 surface 更实。 */
    val surfaceStrong: Color,
    /** 凹陷感的小块（输入框、滑块槽、次级标签）。 */
    val surfaceDeep: Color,
    /** 主文字，同时是波普主题的描边色。玻璃主题下不是纯黑。 */
    val ink: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    /** 反色块，用于"黑标签"这类强调。 */
    val inverseSurface: Color,
    val inverseInk: Color,
    /** 细分隔线。 */
    val hairline: Color,
    /** 弹窗遮罩。 */
    val scrim: Color,
)

/**
 * 一整套皮肤。除了颜色，还包含"形状语言"与两个行为开关：
 *
 * - [hardInk]：是否用粗黑描边。波普为 true，玻璃为 false（改用白色高光描边）。
 * - [glass]：面板是否走玻璃绘制（半透明 + 高光 + 柔和投影）。
 *
 * 这两个开关决定 `skinSurface` 走哪条绘制路径，而不是让组件自己判断。
 */
@Immutable
data class PopSkin(
    val style: PopThemeStyle,
    val palette: PopPalette,
    val radiusCard: Dp,
    val radiusPanel: Dp,
    val radiusField: Dp,
    val radiusChip: Dp,
    val radiusButton: Dp,
    val hardInk: Boolean,
    val glass: Boolean,
)

object PopSkins {

    val Pop = PopSkin(
        style = PopThemeStyle.Pop,
        palette = PopPalette(
            canvas = PopColor.Core.Canvas,
            surface = PopColor.Core.Paper,
            surfaceStrong = PopColor.Core.White,
            surfaceDeep = PopColor.Core.White,
            ink = PopColor.Core.Black,
            inkSecondary = PopColor.Text.Secondary,
            inkMuted = PopColor.Neutral.Gray400,
            inverseSurface = PopColor.Core.Black,
            inverseInk = PopColor.Core.White,
            hairline = PopColor.Neutral.Gray200,
            scrim = PopColor.Core.Black.copy(alpha = 0.72f),
        ),
        // 波普是直角：贴纸要像贴在纸上，圆角会把它变成"卡片"
        radiusCard = PopRadius.None,
        radiusPanel = PopRadius.None,
        radiusField = PopRadius.None,
        radiusChip = PopRadius.None,
        radiusButton = PopRadius.None,
        hardInk = true,
        glass = false,
    )

    val Liquid = PopSkin(
        style = PopThemeStyle.Liquid,
        palette = PopPalette(
            canvas = PopColor.Glass.Canvas,
            surface = PopColor.Glass.Tint.copy(alpha = PopGlass.SurfaceAlpha),
            surfaceStrong = PopColor.Glass.Tint.copy(alpha = PopGlass.SurfaceStrongAlpha),
            surfaceDeep = PopColor.Glass.Tint.copy(alpha = PopGlass.SurfaceDeepAlpha),
            ink = PopColor.Glass.Ink,
            inkSecondary = PopColor.Glass.InkSecondary,
            inkMuted = PopColor.Glass.InkMuted,
            inverseSurface = PopColor.Glass.Ink,
            inverseInk = PopColor.Glass.Tint,
            hairline = PopColor.Glass.Hairline.copy(alpha = 0.45f),
            scrim = PopColor.Glass.Scrim.copy(alpha = 0.34f),
        ),
        radiusCard = PopGlass.RadiusCard,
        radiusPanel = PopGlass.RadiusPanel,
        radiusField = PopGlass.RadiusField,
        radiusChip = PopGlass.RadiusChip,
        radiusButton = PopGlass.RadiusButton,
        hardInk = false,
        glass = true,
    )

    fun of(style: PopThemeStyle): PopSkin = when (style) {
        PopThemeStyle.Liquid -> Liquid
        PopThemeStyle.Pop -> Pop
    }
}

/**
 * 当前皮肤。默认液态玻璃 —— 即使某个 Composable 忘了包 PopTheme（预览、单独测试），
 * 出来的也是新主题，而不是悄悄退回成旧样式。
 */
val LocalSkin = staticCompositionLocalOf { PopSkins.Liquid }

val LocalAccent = staticCompositionLocalOf { PopColor.Core.DefaultAccent }
val LocalOnAccent = staticCompositionLocalOf { Color.Black }

/**
 * 移植自 `utils/colorUtils.ts`：加权亮度 > 128 判为浅色。
 * 必须与 Web 版同一套判定，否则同一个主题色在两端会出现"一边黑字一边白字"。
 */
fun isLightAccent(color: Color): Boolean {
    val brightness = (color.red * 255f * 299f + color.green * 255f * 587f + color.blue * 255f * 114f) / 1000f
    return brightness > 128f
}

val accent: Color
    @Composable get() = LocalAccent.current

val onAccent: Color
    @Composable get() = LocalOnAccent.current

val skin: PopSkin
    @Composable get() = LocalSkin.current

/**
 * 语义色在组合作用域的快捷入口。
 *
 * 组件里原先写的是 `PopColor.Core.White` 这类**外观名**，现在换成 `popSurface` 这类**语义名**。
 * 这样做而不是让每个组件各自 `val current = LocalSkin.current`，是因为改造成本差一个数量级：
 * 那些引用有近百处，散在二十多个文件里，逐个加局部变量必然会漏，漏一处就是一块
 * 在玻璃主题下发白的硬边方块。
 */
val popSurface: Color @Composable get() = LocalSkin.current.palette.surface
val popSurfaceStrong: Color @Composable get() = LocalSkin.current.palette.surfaceStrong
val popSurfaceDeep: Color @Composable get() = LocalSkin.current.palette.surfaceDeep
val popInk: Color @Composable get() = LocalSkin.current.palette.ink
val popInkSecondary: Color @Composable get() = LocalSkin.current.palette.inkSecondary
val popInkMuted: Color @Composable get() = LocalSkin.current.palette.inkMuted
val popHairline: Color @Composable get() = LocalSkin.current.palette.hairline
val popInverseSurface: Color @Composable get() = LocalSkin.current.palette.inverseSurface
val popInverseInk: Color @Composable get() = LocalSkin.current.palette.inverseInk

@Composable
fun PopTheme(
    accent: Color = PopColor.Core.DefaultAccent,
    style: PopThemeStyle = PopThemeStyle.Default,
    content: @Composable () -> Unit,
) {
    val current = remember(style) { PopSkins.of(style) }
    CompositionLocalProvider(
        LocalAccent provides accent,
        LocalOnAccent provides if (isLightAccent(accent)) PopColor.Core.Black else PopColor.Core.White,
        LocalSkin provides current,
        content = content,
    )
}

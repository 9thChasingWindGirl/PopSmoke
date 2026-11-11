// GENERATED FILE — DO NOT EDIT.
// 由 design/tokens.json 经 tools/gen-tokens.mjs 生成。改动请改 JSON 后重新生成。
// 校验：node tools/gen-tokens.mjs --check

package com.popsmoke.tracker.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** 设计令牌 v1 · 来源 styles/designSystem.ts + styles/componentStyles.ts + index.html */
object PopColor {
    object Core {
        val Black = Color(0xFF000000)
        val White = Color(0xFFFFFFFF)
        val Paper = Color(0xFFFDFBF7)
        val Canvas = Color(0xFFF0F0F0)
        val SurfaceMuted = Color(0xFFEEF2FF)
        val DefaultAccent = Color(0xFFF97316)
    }
    object Theme {
        val Gold = Color(0xFFFFD700)
        val Coral = Color(0xFFFF6B6B)
        val Teal = Color(0xFF4ECDC4)
        val Lime = Color(0xFFC7F464)
        val Purple = Color(0xFFA78BFA)
        val Orange = Color(0xFFFF9F43)
    }
    object Status {
        val Success = Color(0xFF22C55E)
        val SuccessLight = Color(0xFFDCFCE7)
        val Error = Color(0xFFEF4444)
        val ErrorLight = Color(0xFFFEE2E2)
        val Warning = Color(0xFFEAB308)
        val WarningLight = Color(0xFFFEF9C3)
        val WarningBorder = Color(0xFFFDE047)
        val Info = Color(0xFF3B82F6)
        val InfoLight = Color(0xFFDBEAFE)
        val Danger = Color(0xFFFF4D4D)
        val Amber = Color(0xFFF59E0B)
    }
    object Text {
        val Primary = Color(0xFF000000)
        val Secondary = Color(0xFF374151)
        val Muted = Color(0xFF6B7280)
        val Light = Color(0xFF9CA3AF)
    }
    object Glass {
        val Canvas = Color(0xFFDCDAE9)
        val Tint = Color(0xFFFFFFFF)
        val Ink = Color(0xFF16161A)
        val InkSecondary = Color(0xFF4E4E5C)
        val InkMuted = Color(0xFF8B8B99)
        val Hairline = Color(0xFFC9C9D4)
        val Scrim = Color(0xFF12121A)
    }
    object Neutral {
        val Gray100 = Color(0xFFF3F4F6)
        val Gray200 = Color(0xFFE5E7EB)
        val Gray300 = Color(0xFFD1D5DB)
        val Gray400 = Color(0xFF9CA3AF)
        val Gray500 = Color(0xFF6B7280)
        val Gray600 = Color(0xFF4B5563)
        val Gray700 = Color(0xFF374151)
        val Yellow200 = Color(0xFFFEF08A)
        val Yellow300 = Color(0xFFFDE047)
        val Red200 = Color(0xFFFECACA)
    }
}

data class PopThemePreset(
    val id: String,
    val color: Color,
    val names: Map<String, String>,
) {
    fun name(lang: String): String = names[lang] ?: names["en"] ?: id
}

object PopPresets {
    val All: List<PopThemePreset> = listOf(
        PopThemePreset("gold", Color(0xFFFFD700), mapOf("en" to "Gold", "zh" to "金色", "ja" to "ゴールド", "ko" to "골드")),
        PopThemePreset("coral", Color(0xFFFF6B6B), mapOf("en" to "Coral", "zh" to "珊瑚红", "ja" to "コーラル", "ko" to "코랄")),
        PopThemePreset("teal", Color(0xFF4ECDC4), mapOf("en" to "Teal", "zh" to "青绿", "ja" to "ティール", "ko" to "틸")),
        PopThemePreset("lime", Color(0xFFC7F464), mapOf("en" to "Lime", "zh" to "浅绿", "ja" to "ライム", "ko" to "라임")),
        PopThemePreset("purple", Color(0xFFA78BFA), mapOf("en" to "Purple", "zh" to "紫色", "ja" to "パープル", "ko" to "퍼플")),
        PopThemePreset("orange", Color(0xFFFF9F43), mapOf("en" to "Orange", "zh" to "橙色", "ja" to "オレンジ", "ko" to "オレンジ")),
    )

    fun byId(id: String): PopThemePreset? = All.firstOrNull { it.id == id }
    fun byColor(c: Color): PopThemePreset? = All.firstOrNull { it.color == c }
}

object PopStroke {
    val Thin = 2.dp
    val Default = 4.dp
    val Thick = 6.dp
}

object PopRadius {
    val None = 0.dp
    val Sm = 2.dp
    val Md = 16.dp
    val Lg = 24.dp
    val Full = 9999.dp
}

/** 硬阴影偏移量，全部零模糊。用矩形平移实现，禁止用模糊阴影。 */
object PopShadow {
    val Xs = 1.dp
    val Pop = 4.dp
    val PopHover = 2.dp
    val PopLg = 8.dp
    val Press = 2.dp
    val Smoke = 7.dp
    val SmokePressed = 3.5.dp
    val ListItem = 2.dp
}

object PopSpace {
    val S1 = 3.5.dp
    val S2 = 7.dp
    val S3 = 10.5.dp
    val S4 = 14.dp
    val S5 = 17.5.dp
    val S6 = 21.dp
    val S7 = 24.5.dp
    val S8 = 28.dp
    val S10 = 35.dp
    val S12 = 42.dp
    val S14 = 49.dp
    val S16 = 56.dp
    val S20 = 70.dp
    val S24 = 84.dp
}

object PopTypeScale {
    val Xs = 10.5.sp
    val Sm = 12.25.sp
    val Base = 14.sp
    val Lg = 15.75.sp
    val Xl = 17.5.sp
    val Xxl = 21.sp
    val Xxxl = 26.25.sp
    val Xxxxl = 31.5.sp
    val Fivexl = 42.sp
    val Sixxl = 52.5.sp
    val Gauge = 35.sp
}

object PopLineHeight {
    val Xs = 14.sp
    val Sm = 17.5.sp
    val Base = 21.sp
    val Lg = 24.5.sp
    val Xl = 24.5.sp
    val Xxl = 28.sp
    val Xxxl = 31.5.sp
    val Xxxxl = 35.sp
    val Fivexl = 42.sp
    val Sixxl = 52.5.sp
    val Gauge = 52.5.sp
    fun of(size: androidx.compose.ui.unit.TextUnit) = when (size) {
        PopTypeScale.Xs -> Xs
        PopTypeScale.Sm -> Sm
        PopTypeScale.Base -> Base
        PopTypeScale.Lg -> Lg
        PopTypeScale.Xl -> Xl
        PopTypeScale.Xxl -> Xxl
        PopTypeScale.Xxxl -> Xxxl
        PopTypeScale.Xxxxl -> Xxxxl
        PopTypeScale.Fivexl -> Fivexl
        PopTypeScale.Sixxl -> Sixxl
        PopTypeScale.Gauge -> Gauge
        else -> androidx.compose.ui.unit.TextUnit.Unspecified
    }
}

object PopWeight {
    const val Normal = 400
    const val Bold = 700
    const val Black = 900
}

object PopTracking {
    val Tight = -0.025.em
    val Normal = 0.em
    val Wide = 0.025.em
    val Wider = 0.05.em
    val Widest = 0.1.em
}

object PopIconSize {
    val Xs = 21.dp
    val Sm = 21.dp
    val Md = 28.dp
    val Lg = 28.dp
}

object PopLayout {
    val ContentMax = 448.dp
    val HeaderHeight = 60.dp
    val BottomBarHeight = 60.dp
    val DotGridSize = 20.dp
    val DotRadius = 1.dp
    val SmokeButtonMax = 280.dp
    val ButtonMinWidth = 80.dp
    val DialogMaxWidth = 336.dp
    val AlertMax = 392.dp
    val IconButton = 42.dp
    val Chip = 28.dp
    val ThemePanelWidth = 280.dp
    val MenuMinWidth = 80.dp
    val TimeWheelHeight = 128.dp
    val TimeWheelRow = 40.dp
    val TimeWheelColumn = 80.dp
    val SvPanelHeight = 120.dp
    val HueBarHeight = 28.dp
}

/** 液态玻璃主题参数。后缀即单位：Alpha 为透明度，Px 为 dp，Ms 为毫秒，其余为倍数。 */
object PopGlass {
    const val SurfaceAlpha = 0.055f
    const val SurfaceStrongAlpha = 0.18f
    const val SurfaceDeepAlpha = 0.12f
    val EdgeStroke = 1.dp
    const val EdgePeakAlpha = 0.68f
    const val EdgeMinAlpha = 0.04f
    val RefractBezel = 34.dp
    val RefractDepth = 30.dp
    const val RefractPeak = 0.45f
    const val DispersionAmount = 0.26f
    const val SpecularAlpha = 0.16f
    const val SpecularRadius = 0.13f
    const val TintAlpha = 0.1f
    const val FieldBandAlpha = 0.17f
    const val FieldArcAlpha = 0.06f
    const val WeaveAlpha = 0.3f
    val WeaveSpacing = 40.dp
    val WeaveStroke = 1.dp
    const val ShadowAlpha = 0.055f
    val ShadowBlur = 34.dp
    val ShadowLift = 7.dp
    val ShadowSpread = 0.dp
    val RadiusCard = 34.dp
    val RadiusPanel = 28.dp
    val RadiusField = 20.dp
    val RadiusChip = 16.dp
    val RadiusButton = 26.dp
    val AuroraBlob = 560.dp
    val AuroraSecondary = 420.dp
    const val AuroraAlpha = 0.78f
    const val GrainAlpha = 0.03f
    const val DriftDuration = 48000
    const val JellyPressScale = 0.945f
    const val JellyPressSquash = 1.03f
    const val JellyPressDamping = 0.42f
    const val JellyPressStiffness = 780f
    const val JellyReleaseDamping = 0.3f
    const val JellyReleaseStiffness = 420f
    const val WeaveInkAlpha = 0.085f
    const val SolidSheenAlpha = 0.2f
    const val InnerShadeAlpha = 0.11f
    const val GroupAlpha = 0.3f
    const val DividerAlpha = 0.09f
    const val ChromeVeilAlpha = 0.1f
    const val NavAlpha = 0.17f
    const val ControlAlpha = 0.2f
    const val OverlayAlpha = 0.66f
}

object PopMotion {
    const val Instant = 100
    const val Press = 150
    const val Fast = 150
    const val Normal = 300
    const val Slow = 500
    const val Flip = 700
    const val Gauge = 1000
    const val Pulse = 2000
    const val Bounce = 1000

    val Smooth = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val BounceEase = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f)
    val EaseOutCurve = CubicBezierEasing(0f, 0f, 0.58f, 1f)
    val Linear = CubicBezierEasing(0f, 0f, 1f, 1f)
}

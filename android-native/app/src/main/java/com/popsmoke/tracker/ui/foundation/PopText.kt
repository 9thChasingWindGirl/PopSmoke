package com.popsmoke.tracker.ui.foundation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.popsmoke.tracker.design.PopLineHeight
import com.popsmoke.tracker.design.skin
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.PopBodyFont
import com.popsmoke.tracker.ui.PopDisplayFont

/**
 * 用 BasicText 而不是 Text：不引入 Material，就不会带进默认字号阶梯、默认行高和 contentColor，
 * 所有排版参数都来自令牌。
 */
@Composable
private fun PopTextBase(
    text: String,
    family: FontFamily,
    modifier: Modifier,
    size: TextUnit,
    weight: FontWeight,
    color: Color,
    tracking: TextUnit,
    italic: Boolean,
    uppercase: Boolean,
    textAlign: TextAlign,
    maxLines: Int,
    lineHeight: TextUnit,
) {
    // 行高不传时按字号取 Tailwind 的配套值。这不是"锦上添花"：
    // Compose 默认行高来自字体度量，比 Web 矮 10%~15%，而卡片高度=内容高度，
    // 一行行累下来就是整块卡片、标签、按钮的位置漂移。
    //
    // 坑：只设 lineHeight 是不够的。lineHeightStyle 不显式给值时，这一版 Compose
    // 会把行高当"最小行距"处理，单行文本量出来仍是字体度量的高度（实测把标签行高
    // 改成 40sp 后，标签高度一个像素都没变）。必须同时写 LineHeightStyle(Center, Trim.None)
    // 才是 CSS line-height 的 half-leading 语义。
    val lh = if (lineHeight == TextUnit.Unspecified) PopLineHeight.of(size) else lineHeight
    // 默认色来自皮肤而不是写死黑：玻璃主题的"墨"是 #16161A，纯黑压在磨砂面上会发脏。
    // 用 Unspecified 做哨兵，避免在每个组件里各写一遍 LocalSkin.current。
    val actual = if (color == Color.Unspecified) skin.palette.ink else color

    BasicText(
        text = if (uppercase) text.uppercase() else text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = family,
            fontSize = size,
            fontWeight = weight,
            color = actual,
            letterSpacing = tracking,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            textAlign = textAlign,
            lineHeight = lh,
            lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
            ),
            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 展示字体：标题、数字、按钮文案。
 *
 * 波普用 Bangers —— 那种手绘漫画字（自带倾斜、笔画粗细不均）是整站"贴纸感"的来源。
 * 液态玻璃主题必须换成 HarmonyOS Sans：Bangers 在磨砂玻璃上会立刻破功，
 * 因为玻璃的语言是"精确的几何 + 细腻的材质"，漫画字讲的是"手绘 + 粗粝"，
 * 两种语言放一起，前者会显得像给玻璃贴了一张手写便签。
 */
@Composable
fun PopDisplay(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = PopTypeScale.Xl,
    weight: FontWeight = FontWeight.Black,
    color: Color = Color.Unspecified,
    tracking: TextUnit = PopTracking.Normal,
    italic: Boolean = false,
    uppercase: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    lineHeight: TextUnit = TextUnit.Unspecified,
) = PopTextBase(
    text = text,
    family = if (skin.glass) PopBodyFont else PopDisplayFont,
    modifier = modifier,
    size = size,
    weight = weight,
    color = color,
    tracking = tracking,
    // 玻璃主题下忽略斜体：真斜体在磨砂面上会显得歪，而且和"光从上往下打"的假设打架
    italic = if (skin.glass) false else italic,
    uppercase = uppercase,
    textAlign = textAlign,
    maxLines = maxLines,
    lineHeight = lineHeight,
)

/**
 * 品牌字的字形：**无论哪套皮肤都用 Bangers**。
 *
 * [PopDisplay] 在玻璃主题下会换成 HarmonyOS Sans —— 那条规则是对的，漫画字做界面文案
 * 会破功。但品牌标识不是文案，是 logo：它必须和 Web 版、图标、官网是同一副字形，
 * 否则用户会觉得"这不是同一个 App"。
 *
 * 字号和字距对齐 `assets/logo.svg`：font-size 24、letter-spacing 1.2px（≈0.05em）。
 */
@Composable
fun PopLogotype(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = PopTypeScale.Xl,
    color: Color = Color.Unspecified,
) = PopTextBase(
    text = text,
    family = PopDisplayFont,
    modifier = modifier,
    size = size,
    weight = FontWeight.Normal,
    color = color,
    tracking = size * 0.05f,
    italic = false,
    uppercase = false,
    textAlign = TextAlign.Start,
    maxLines = 1,
    lineHeight = TextUnit.Unspecified,
)

/**
 * 顶栏品牌：`POP` + `SMOKE`，后半段吃主题色。
 *
 * 两段必须是两个 Text —— 颜色不同。中间那个空隙对应原 logo 里的一个空格，
 * 用 3dp 固定值而不是真的打一个空格：Bangers 的空格宽度会随字号变，
 * 而品牌标识的字形比例在四个语言、两种皮肤下都应该一致。
 */
@Composable
fun PopBrand(
    accent: Color,
    modifier: Modifier = Modifier,
    size: TextUnit = PopTypeScale.Xl,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        PopLogotype(text = "POP", size = size)
        Spacer(Modifier.width(3.dp))
        PopLogotype(text = "SMOKE", size = size, color = accent)
    }
}

/** 正文字体（HarmonyOS Sans）：说明、时间、标签。 */@Composable
fun PopBody(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = PopTypeScale.Base,
    weight: FontWeight = FontWeight.Bold,
    color: Color = Color.Unspecified,
    tracking: TextUnit = PopTracking.Normal,
    italic: Boolean = false,
    uppercase: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 2,
    lineHeight: TextUnit = TextUnit.Unspecified,
) = PopTextBase(
    text = text,
    family = PopBodyFont,
    modifier = modifier,
    size = size,
    weight = weight,
    color = color,
    tracking = tracking,
    italic = italic,
    uppercase = uppercase,
    textAlign = textAlign,
    maxLines = maxLines,
    lineHeight = lineHeight,
)

/**
 * 等宽字体：只给操作日志那种"终端"面板用。
 *
 * 用系统等宽而不是把另一套 Web 字体塞进来 —— 终端里的对齐靠的是字符宽度一致，
 * 系统等宽在四种语言下都能保证这一点，自定义字体反而不一定包含中日韩全角字形。
 */
@Composable
fun PopMono(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = PopTypeScale.Sm,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
) = PopTextBase(
    text = text,
    family = FontFamily.Monospace,
    modifier = modifier,
    size = size,
    weight = weight,
    color = color,
    tracking = PopTracking.Normal,
    italic = false,
    uppercase = false,
    textAlign = textAlign,
    maxLines = maxLines,
    lineHeight = TextUnit.Unspecified,
)

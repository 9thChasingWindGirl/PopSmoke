package com.popsmoke.tracker.ui.foundation

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.popsmoke.tracker.design.PopLineHeight
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

    BasicText(
        text = if (uppercase) text.uppercase() else text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = family,
            fontSize = size,
            fontWeight = weight,
            color = color,
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

/** 展示字体（Bangers）：标题、数字、按钮文案。 */
@Composable
fun PopDisplay(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = PopTypeScale.Xl,
    weight: FontWeight = FontWeight.Black,
    color: Color = Color.Black,
    tracking: TextUnit = PopTracking.Normal,
    italic: Boolean = false,
    uppercase: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    lineHeight: TextUnit = TextUnit.Unspecified,
) = PopTextBase(
    text = text,
    family = PopDisplayFont,
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

/** 正文字体（HarmonyOS Sans）：说明、时间、标签。 */
@Composable
fun PopBody(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = PopTypeScale.Base,
    weight: FontWeight = FontWeight.Bold,
    color: Color = Color.Black,
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
    color: Color = Color.Black,
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

package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopGlass
import com.popsmoke.tracker.design.PopLineHeight
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.PopDisplayFont
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.skinSurface

/**
 * 输入框：4dp 黑边 + 白底 + 展示字体，对应 Web 的
 * `border-4 border-black p-2 font-display`。
 *
 * 用 BasicTextField 而不是 Material 的 TextField：后者自带下划线容器色、聚焦指示和
 * 默认内边距，覆盖成本比自建高，而且会带进 Material 味。
 *
 * 原生额外补的两件事：IME 动作（搜索/完成会收起键盘）和错误态描边 —— 这两项 Web 版没有。
 */
@Composable
fun PopInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    error: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val current = LocalSkin.current
    val style = TextStyle(
        fontFamily = PopDisplayFont,
        fontSize = PopTypeScale.Base,
        fontWeight = FontWeight.Bold,
        color = current.palette.ink,
        lineHeight = PopLineHeight.of(PopTypeScale.Base),
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(current.palette.ink),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        decorationBox = { inner ->
            val shape = RoundedCornerShape(current.radiusField)
            Box(
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (current.glass && !error) {
                            // 输入框是内容区里唯一"要动手"的控件，给它一层薄玻璃：
                            // 比分组略透、带边缘折射、没有投影（它是嵌在分组里的槽，不该浮起来）。
                            // 错误态不走玻璃 —— 红边是警示信号，折射会把那条线的边界糊掉。
                            //
                            // 再加一根 6% 的内边线：浅色背景上，"槽"和"分组"都是一层白，
                            // 只靠 alpha 差（0.08 vs 0.18）读不出"这里可以输入"。
                            Modifier
                                .skinSurface(
                                    current = current,
                                    shape = shape,
                                    fill = current.palette.surfaceStrong.copy(alpha = PopGlass.ControlAlpha * 0.5f),
                                    lift = 0.dp,
                                )
                                .border(1.dp, current.palette.ink.copy(alpha = 0.06f), shape)
                        } else {
                            Modifier
                                // 玻璃下输入框是"凹槽"：比卡片更透、更暗一点，才读得出可以输入
                                .background(
                                    if (error) PopColor.Status.ErrorLight.copy(alpha = 0.66f) else current.palette.surfaceDeep,
                                    shape,
                                )
                                .popBorder(
                                    PopStroke.Default,
                                    if (error) PopColor.Status.Error else current.palette.ink,
                                    shape,
                                )
                        },
                    )
                    // Web 是 p-1/p-2；原生给足高度，否则触摸目标太小
                    .padding(horizontal = PopSpace.S3, vertical = PopSpace.S2),
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    PopDisplay(
                        text = placeholder,
                        size = PopTypeScale.Base,
                        weight = FontWeight.Bold,
                        color = current.palette.inkMuted,
                    )
                }
                inner()
            }
        },
    )
}

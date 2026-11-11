package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopLineHeight
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.PopDisplayFont
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder

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
    val style = TextStyle(
        fontFamily = PopDisplayFont,
        fontSize = PopTypeScale.Base,
        fontWeight = FontWeight.Bold,
        color = PopColor.Core.Black,
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
        cursorBrush = SolidColor(PopColor.Core.Black),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(if (error) PopColor.Status.ErrorLight else PopColor.Core.White)
                    .popBorder(
                        PopStroke.Default,
                        if (error) PopColor.Status.Error else PopColor.Core.Black,
                    )
                    // Web 是 p-1/p-2；原生给足高度，否则触摸目标太小
                    .padding(horizontal = PopSpace.S3, vertical = PopSpace.S2),
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    PopDisplay(
                        text = placeholder,
                        size = PopTypeScale.Base,
                        weight = FontWeight.Bold,
                        color = PopColor.Neutral.Gray400,
                    )
                }
                inner()
            }
        },
    )
}

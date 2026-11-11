package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.parseHexColor
import com.popsmoke.tracker.ui.foundation.PopDisplay

/**
 * 取色器：色号输入 + 实时预览 + 预设色块。
 *
 * 与 Web 版（canvas 上的 HSV 渐变面板）的差异：手机上在小面板里拖色相条很难点准，
 * 而"知道色号直接填"和"从常用色里挑"覆盖了绝大多数实际用法。两条路径都保留：
 * 输入框接受 #RRGGBB，色块点一下即生效。
 */
@Composable
fun PopColorPicker(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<String> = emptyList(),
) {
    var draft by remember(value) { mutableStateOf(value) }
    val preview = parseHexColor(draft) ?: parseHexColor(value) ?: PopColor.Core.DefaultAccent

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(PopSpace.S10)
                    .background(preview)
                    .border(PopStroke.Thin, PopColor.Core.Black),
            )
            Box(Modifier.size(PopSpace.S2))
            PopInput(
                value = draft,
                onValueChange = { text ->
                    draft = text
                    val hex = text.trim().let { if (it.startsWith("#")) it else "#$it" }
                    if (hex.length == 7 && parseHexColor(hex) != null) onChange(hex.uppercase())
                },
                modifier = Modifier.weight(1f),
                placeholder = "#RRGGBB",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
        }

        if (presets.isNotEmpty()) {
            Box(Modifier.size(PopSpace.S3))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PopSpace.S2),
            ) {
                presets.forEach { hex ->
                    val color = parseHexColor(hex) ?: return@forEach
                    val selected = hex.equals(value, ignoreCase = true)
                    Box(
                        Modifier
                            .weight(1f)
                            .size(PopSpace.S10)
                            .background(color)
                            .border(
                                if (selected) PopStroke.Default else PopStroke.Thin,
                                PopColor.Core.Black,
                            )
                            .clickable {
                                draft = hex
                                onChange(hex)
                            },
                    )
                }
            }
            Box(Modifier.size(PopSpace.S1))
            PopDisplay(
                text = "TAP A SWATCH OR TYPE A HEX",
                size = PopTypeScale.Xs,
                weight = FontWeight.Bold,
                color = PopColor.Neutral.Gray400,
                maxLines = 1,
            )
        }
    }
}

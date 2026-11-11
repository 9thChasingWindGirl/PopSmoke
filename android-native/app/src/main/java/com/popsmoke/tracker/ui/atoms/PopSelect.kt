package com.popsmoke.tracker.ui.atoms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopMotion
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder

/**
 * 选择器：触发器 + **就地展开**的选项列表。
 *
 * 与 Web 版的一处刻意差异：Web 是浮层下拉，点了别处会关掉，还要自己监听 document 点击。
 * 手机上浮层会盖住手指、且下拉在长表单里容易被键盘顶飞；就地展开让"选了哪个"始终可见，
 * 也不需要外部点击关闭这层逻辑。视觉仍是同一套：白底、4dp 黑边、选中项反色。
 */
@Composable
fun PopSelect(
    label: String,
    value: String,
    options: List<PopSelectOption>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PopColor.Core.White)
                .popBorder(PopStroke.Default, PopColor.Core.Black)
                .clickable { expanded = !expanded }
                .semantics { role = Role.Button }
                .padding(horizontal = PopSpace.S3, vertical = PopSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Box(Modifier.size(PopSpace.S2))
            }
            PopDisplay(
                text = "$label: $value",
                modifier = Modifier.weight(1f),
                size = PopTypeScale.Base,
                weight = FontWeight.Bold,
                maxLines = 1,
            )
            PopDisplay(
                text = if (expanded) "▲" else "▼",
                size = PopTypeScale.Sm,
                weight = FontWeight.Bold,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(PopMotion.Normal)),
            exit = shrinkVertically(tween(PopMotion.Normal)),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = PopSpace.S1)) {
                options.forEach { option ->
                    val selected = option.value == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = PopSpace.S1)
                            .background(if (selected) PopColor.Core.Black else PopColor.Core.White)
                            .popBorder(PopStroke.Thin, PopColor.Core.Black)
                            .clickable {
                                onSelect(option.value)
                                expanded = false
                            }
                            .padding(horizontal = PopSpace.S3, vertical = PopSpace.S2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (option.swatch != null) {
                            Box(
                                Modifier
                                    .size(PopSpace.S6)
                                    .background(option.swatch)
                                    .border(PopStroke.Thin, PopColor.Core.Black),
                            )
                            Box(Modifier.size(PopSpace.S2))
                        }
                        PopDisplay(
                            text = option.label,
                            modifier = Modifier.weight(1f),
                            size = PopTypeScale.Base,
                            weight = FontWeight.Bold,
                            color = if (selected) PopColor.Core.White else PopColor.Core.Black,
                            maxLines = 1,
                        )
                        if (option.hint != null) {
                            PopDisplay(
                                text = option.hint,
                                size = PopTypeScale.Xs,
                                weight = FontWeight.Bold,
                                color = if (selected) PopColor.Core.White else PopColor.Neutral.Gray500,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

data class PopSelectOption(
    val value: String,
    val label: String,
    val swatch: Color? = null,
    val hint: String? = null,
)

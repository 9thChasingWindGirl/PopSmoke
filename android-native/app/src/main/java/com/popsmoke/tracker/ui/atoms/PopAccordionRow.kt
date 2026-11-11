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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopMotion
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.PopTapTarget
import com.popsmoke.tracker.ui.foundation.popBorder

/**
 * 折叠行：▶/▼ + 标题 + 计数徽标，点击展开。
 *
 * 展开箭头用 `rotate` 动画而不是换字符（Web 是两个 emoji 字符 ↔ 文案表），
 * 原生转起来更连贯，也免了多语言下的字形回落差异。
 */
@Composable
fun PopAccordionRow(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeColor: Color = com.popsmoke.tracker.design.accent,
    badgeTextColor: Color = Color.Black,
    subtitle: String? = null,
    background: Color = PopColor.Neutral.Gray100,
    borderWidth: Dp = PopStroke.Thin,
    titleSize: androidx.compose.ui.unit.TextUnit = PopTypeScale.Lg,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .popBorder(borderWidth, PopColor.Core.Black)
                .clickable(onClick = onToggle)
                .semantics { role = Role.Button }
                .padding(horizontal = PopSpace.S3, vertical = PopSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PopDisplay(
                text = "▶",
                modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 90f else 0f },
                size = PopTypeScale.Sm,
                weight = FontWeight.Black,
                color = PopColor.Neutral.Gray600,
            )
            Spacer(Modifier.width(PopSpace.S2))
            Column(Modifier.weight(1f)) {
                PopDisplay(
                    text = title,
                    size = titleSize,
                    weight = FontWeight.Black,
                    maxLines = 1,
                )
                if (subtitle != null) {
                    PopDisplay(
                        text = subtitle,
                        size = PopTypeScale.Xs,
                        weight = FontWeight.Bold,
                        color = PopColor.Neutral.Gray500,
                        maxLines = 1,
                    )
                }
            }
            if (badge != null) {
                Box(
                    Modifier
                        .background(badgeColor)
                        .border(PopStroke.Thin, PopColor.Core.Black)
                        .padding(horizontal = PopSpace.S3, vertical = PopSpace.S1),
                ) {
                    PopDisplay(
                        text = badge,
                        size = PopTypeScale.Base,
                        weight = FontWeight.Black,
                        color = badgeTextColor,
                        maxLines = 1,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(PopMotion.Normal)),
            exit = shrinkVertically(tween(PopMotion.Normal)),
        ) {
            Column(Modifier.fillMaxWidth().padding(start = PopSpace.S4, top = PopSpace.S1)) {
                content()
            }
        }
    }
}

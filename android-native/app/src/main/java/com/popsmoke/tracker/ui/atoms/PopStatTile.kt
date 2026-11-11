package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.chipShape
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.skinSurface
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.popInkMuted
import com.popsmoke.tracker.design.popSurfaceDeep

/**
 * 统计格：灰底 + 2dp 描边 + 居中大数字 + 大写说明。
 * 对应 Web 里满页复用的 `p-2 bg-gray-50 border-2 border-black text-center`。
 *
 * 玻璃主题下它是**凹槽**而不是贴纸：圆角、半透明填充、没有投影（lift = 0）。
 * 统计格本来就在卡片内部，再给它加落影会让"卡片里还浮着两张小卡片"，层次反而更乱。
 */
@Composable
fun PopStatTile(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    valueColor: Color = popInk,
    valueSize: TextUnit = PopTypeScale.Xl,
    background: Color = popSurfaceDeep,
) {
    val current = LocalSkin.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (current.glass) {
                    // 凹槽，不是玻璃。它已经在"静默分组"里面了，再来一层折射 +
                    // 白边就成了"卡片里还嵌着一块玻璃"，层次反而更乱。
                    Modifier
                        .clip(current.chipShape())
                        .background(background)
                        // 再压 3% 墨色：纯白底在浅色分组里几乎看不出来，
                        // 叠一点暗才读得出"这是一个凹进去的格子"
                        .background(popInk.copy(alpha = 0.03f))
                } else {
                    Modifier
                        .background(background)
                        .popBorder(PopStroke.Thin, popInk)
                },
            )
            .padding(PopSpace.S2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PopDisplay(
            text = value,
            size = valueSize,
            weight = FontWeight.Black,
            color = valueColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        PopBody(
            text = caption,
            size = PopTypeScale.Xs,
            weight = FontWeight.Bold,
            color = popInkMuted,
            tracking = PopTracking.Wide,
            uppercase = true,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

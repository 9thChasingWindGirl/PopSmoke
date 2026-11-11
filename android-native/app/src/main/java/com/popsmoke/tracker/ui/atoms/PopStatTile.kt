package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
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
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder

/**
 * 统计格：灰底 + 2dp 描边 + 居中大数字 + 大写说明。
 * 对应 Web 里满页复用的 `p-2 bg-gray-50 border-2 border-black text-center`。
 */
@Composable
fun PopStatTile(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    valueColor: Color = PopColor.Core.Black,
    valueSize: TextUnit = PopTypeScale.Xl,
    background: Color = PopColor.Neutral.Gray100,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .popBorder(PopStroke.Thin, PopColor.Core.Black)
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
            color = PopColor.Neutral.Gray500,
            tracking = PopTracking.Wide,
            uppercase = true,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

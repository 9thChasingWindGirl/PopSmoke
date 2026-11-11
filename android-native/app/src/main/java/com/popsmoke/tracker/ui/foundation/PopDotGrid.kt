package com.popsmoke.tracker.ui.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopLayout

/**
 * 页面底纹：20dp 网格上每格一个 1dp 黑点。
 * 对应 index.html 里的 `radial-gradient(#000 1px, transparent 1px)` + `background-size: 20px`。
 * 这是整套视觉的"纸面"，少了它卡片会像浮在纯色上，纸感就没了。
 */
@Composable
fun PopDotGrid(
    modifier: Modifier = Modifier,
    color: Color = PopColor.Core.Black,
) {
    Canvas(modifier) {
        val step = PopLayout.DotGridSize.toPx()
        val radius = PopLayout.DotRadius.toPx()
        if (step <= 0f) return@Canvas
        var y = step / 2f
        while (y < size.height + step) {
            var x = step / 2f
            while (x < size.width + step) {
                drawCircle(color, radius, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

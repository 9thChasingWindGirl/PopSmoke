package com.popsmoke.tracker.ui.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Canvas as BitmapCanvas
import androidx.compose.ui.platform.LocalDensity
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopLayout

/**
 * 页面底纹：20dp 网格上每格一个 1dp 黑点。
 * 对应 index.html 里的 `radial-gradient(#000 1px, transparent 1px)` + `background-size: 20px`。
 * 这是整套视觉的"纸面"，少了它卡片会像浮在纯色上，纸感就没了。
 *
 * 实现方式换过一次：早先是双重循环逐点 `drawCircle`，一屏 1080×2400 约 6500 个点，
 * 每次重绘都是 6500 次绘制调用——这正是"翻转卡片掉帧"和列表滚动发涩的主要来源。
 * 现在把一格的图案烘焙成 20×20 的位图，再用 `ImageShader(TileMode.Repeated)` 平铺，
 * 整屏只剩一次 `drawRect`。视觉完全一致，成本从 6500 次降到 1 次。
 */
@Composable
fun PopDotGrid(
    modifier: Modifier = Modifier,
    color: Color = PopColor.Core.Black,
) {
    val density = LocalDensity.current
    val brush = remember(density, color) {
        val tile = with(density) { PopLayout.DotGridSize.roundToPx() }.coerceAtLeast(1)
        val radius = with(density) { PopLayout.DotRadius.toPx() }
        val bitmap = ImageBitmap(tile, tile)
        BitmapCanvas(bitmap).drawCircle(
            center = Offset(tile / 2f, tile / 2f),
            radius = radius,
            paint = Paint().apply { this.color = color },
        )
        ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
    }

    Canvas(modifier) {
        drawRect(brush)
    }
}

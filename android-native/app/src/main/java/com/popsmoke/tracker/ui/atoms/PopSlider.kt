package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import kotlin.math.roundToInt

/**
 * 单值滑块：灰底轨道 + 主题色进度 + 黑色方形滑块。
 *
 * Web 版是原生 `<input type=range>` 加样式覆盖，手指粗细的拇指由浏览器决定；
 * 原生自绘反而能把拇指做成 20dp 的黑方块 —— 与整站"方块 + 描边"的语言一致。
 *
 * 交互上有一处必须保留：拖动时按整数步进并实时回调，松手不再二次提交，
 * 否则拖动过程中父层重组会让滑块跳一下。
 */
@Composable
fun PopSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 50,
    enabled: Boolean = true,
) {
    val span = (max - min).coerceAtLeast(1)
    val fraction = ((value - min).toFloat() / span).coerceIn(0f, 1f)
    val thumb = PopSpace.S5
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(PopSpace.S6)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, min.toFloat()..max.toFloat(), span)
            },
    ) {
        val trackHeight = PopSpace.S4
        val usable = with(density) { (maxWidth - thumb).toPx() }
        val thumbPx = with(density) { thumb.toPx() }

        // 命中判断用整条轨道，而不是只有拇指：手指落在哪儿都能拖，不需要先抓住滑块
        Box(
            Modifier
                .fillMaxWidth()
                .height(PopSpace.S6)
                .pointerInput(enabled, min, max, usable) {
                    if (!enabled) return@pointerInput
                    fun update(x: Float) {
                        val ratio = ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f)
                        val next = (min + ratio * span).roundToInt()
                        if (next != value) onValueChange(next)
                    }
                    detectHorizontalDragGestures(
                        onDragStart = { update(it.x) },
                        onHorizontalDrag = { change, _ -> update(change.position.x) },
                    )
                },
        )

        // 轨道
        Box(
            Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.CenterStart)
                .background(PopColor.Neutral.Gray200, androidx.compose.foundation.shape.RoundedCornerShape(PopSpace.S1))
                .border(
                    PopStroke.Thin,
                    PopColor.Core.Black,
                    androidx.compose.foundation.shape.RoundedCornerShape(PopSpace.S1),
                ),
        )

        // 进度（与轨道同高，右侧留出拇指的一半，避免超出）
        Box(
            Modifier
                .height(trackHeight)
                .fillMaxWidth(fraction)
                .align(Alignment.CenterStart)
                .background(
                    com.popsmoke.tracker.design.accent,
                    androidx.compose.foundation.shape.RoundedCornerShape(PopSpace.S1),
                ),
        )

        // 拇指：黑方块，压在进度条上
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { androidx.compose.ui.unit.IntOffset((usable * fraction).roundToInt(), 0) }
                .size(thumb)
                .background(PopColor.Core.White)
                .border(PopStroke.Thin, PopColor.Core.Black),
        )
    }
}

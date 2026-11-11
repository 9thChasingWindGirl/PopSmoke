package com.popsmoke.tracker.ui.atoms

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopLayout
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.parseHexColor
import com.popsmoke.tracker.design.toHexString
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder

/**
 * 取色器：色相条 + 饱和度/明度面板 + 色号输入 + 预设色块。
 *
 * 三条与性能/手感相关的决定：
 *
 * 1. **拖动时只改本地预览，松手才回调**。回调一路上会走 `updateSettings → 写文件`，
 *    每帧一次全量落盘足以让手指跟不上，甚至把主线程拖到 ANR。松开手指再提交一次，
 *    颜色变化本身是连续的，用户看不出差别。
 * 2. **面板做成横向矩形（120dp）而不是正方形**。它要装在顶栏那个 280dp 的浮层里，
 *    正方形面板会把高度顶出屏幕，底部预设色块就被窗口裁掉了——用户"看不到色盘"就是这么来的。
 * 3. 保留色号输入与预设色块。知道色号的人不该被逼着拖滑块，常用色也不该每次都拖一遍。
 */
@Composable
fun PopColorPicker(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<String> = emptyList(),
) {
    var draft by remember(value) { mutableStateOf(value) }
    val current = parseHexColor(draft) ?: parseHexColor(value) ?: PopColor.Core.DefaultAccent
    val hsv = remember(current) { current.toHsv() }

    // 拖动中：只更新 draft；松手：提交一次
    fun updateColor(next: Color) {
        draft = next.toHexString()
    }

    fun commit() {
        if (!draft.equals(value, ignoreCase = true) && parseHexColor(draft) != null) {
            onChange(draft)
        }
    }

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(PopSpace.S10)
                    .background(current)
                    .popBorder(PopStroke.Thin, PopColor.Core.Black),
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

        Box(Modifier.size(PopSpace.S2))

        // 饱和度（横）× 明度（纵）
        SvPanel(
            hue = hsv[0],
            saturation = hsv[1],
            brightness = hsv[2],
            onPick = { s, v -> updateColor(Color.hsv(hsv[0], s, v)) },
            onCommit = ::commit,
        )

        Box(Modifier.size(PopSpace.S2))

        HueBar(
            hue = hsv[0],
            onPick = { h -> updateColor(Color.hsv(h, hsv[1].coerceAtLeast(0.05f), hsv[2].coerceAtLeast(0.05f))) },
            onCommit = ::commit,
        )

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
                            .height(PopSpace.S10)
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

/** 饱和度 × 明度面板：白色到纯色相铺底，再压一层由透明到黑的纵向渐变。 */
@Composable
private fun SvPanel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onPick: (Float, Float) -> Unit,
    onCommit: () -> Unit,
) {
    val pure = Color.hsv(hue, 1f, 1f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(PopLayout.SvPanelHeight)
            .popBorder(PopStroke.Thin, PopColor.Core.Black)
            .pointerInput(hue) {
                // size 是 PointerInputScope 的属性，awaitEachGesture 里看不到，先取出来
                val width = size.width.toFloat().coerceAtLeast(1f)
                val height = size.height.toFloat().coerceAtLeast(1f)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun emit(position: Offset) {
                        onPick(
                            (position.x / width).coerceIn(0f, 1f),
                            (1f - position.y / height).coerceIn(0f, 1f),
                        )
                    }
                    emit(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        emit(change.position)
                        change.consume()
                    }
                    onCommit()
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, pure)))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            // 指示环：白圈黑边，深浅底色上都看得见
            val cx = saturation * size.width
            val cy = (1f - brightness) * size.height
            drawCircle(Color.Black, radius = 7.dp.toPx(), center = Offset(cx, cy))
            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(cx, cy))
        }
    }
}

/** 色相条：0-360 的循环渐变，指示器是一个小方框。 */
@Composable
private fun HueBar(
    hue: Float,
    onPick: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(PopLayout.HueBarHeight)
            .popBorder(PopStroke.Thin, PopColor.Core.Black)
            .pointerInput(Unit) {
                val width = size.width.toFloat().coerceAtLeast(1f)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun emit(position: Offset) {
                        onPick(((position.x / width).coerceIn(0f, 1f)) * 360f)
                    }
                    emit(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        emit(change.position)
                        change.consume()
                    }
                    onCommit()
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.horizontalGradient(
                    listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red,
                    ),
                ),
            )
            val x = (hue / 360f) * size.width
            drawRect(
                color = Color.White,
                topLeft = Offset(x - 4.dp.toPx(), 0f),
                size = Size(8.dp.toPx(), size.height),
            )
            drawRect(
                color = Color.Black,
                topLeft = Offset(x - 4.dp.toPx(), 0f),
                size = Size(8.dp.toPx(), size.height),
                style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
            )
        }
    }
}

/** Compose Color → HSV[3]。走 android.graphics 的转换，省得手写 rgb→hsv 的边界处理。 */
private fun Color.toHsv(): FloatArray = FloatArray(3).also { out ->
    AndroidColor.colorToHSV(
        AndroidColor.rgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()),
        out,
    )
}

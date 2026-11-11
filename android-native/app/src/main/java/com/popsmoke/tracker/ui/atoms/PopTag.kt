package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.design.onAccent
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.PopDisplay

/**
 * 手写贴纸：主题色底 + 2dp 描边 + 默认 -2° 倾斜（对应 `-rotate-2`）。
 * 这个轻微歪斜是波普风的标志性细节，不要为了"整齐"把它摆正。
 */
@Composable
fun PopTag(
    text: String,
    modifier: Modifier = Modifier,
    background: Color? = null,
    contentColor: Color? = null,
    rotation: Float = -2f,
) {
    val bg = background ?: accent
    val fg = contentColor ?: onAccent
    Box(
        modifier
            .graphicsLayer { rotationZ = rotation }
            .background(bg)
            .popBorder(PopStroke.Thin, PopColor.Core.Black)
            .padding(horizontal = PopSpace.S6, vertical = PopSpace.S3),
    ) {
        PopDisplay(
            text = text,
            size = PopTypeScale.Base,
            tracking = PopTracking.Normal,
            uppercase = true,
            color = fg,
            // 允许折行：英文文案较长，单行会在窄屏被省略号截断（Web 版是自然换行）
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

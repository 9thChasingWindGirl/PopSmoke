package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.design.onAccent
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.skinSurface
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.design.popInk
import androidx.compose.ui.unit.dp

/**
 * 标签。
 *
 * 波普：手写贴纸 —— 主题色底 + 2dp 描边 + 默认 -2° 倾斜（对应 `-rotate-2`）。
 * 这个轻微歪斜是标志性细节，不要为了"整齐"把它摆正。
 * 玻璃：胶囊 + 半透明 + 不旋转。倾斜会让高光方向与整体光照矛盾，
 * 磨砂面上任何歪斜都立刻读成"没对齐"。
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
    val current = LocalSkin.current
    val pill = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .graphicsLayer { rotationZ = if (current.glass) 0f else rotation }
            .then(
                if (current.glass) {
                    Modifier
                        .skinSurface(current, pill, bg.copy(alpha = 0.62f), lift = 10.dp, tint = bg)
                        .padding(horizontal = PopSpace.S5, vertical = PopSpace.S2)
                } else {
                    Modifier
                        .background(bg)
                        .popBorder(PopStroke.Thin, popInk)
                        .padding(horizontal = PopSpace.S6, vertical = PopSpace.S3)
                },
            ),
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

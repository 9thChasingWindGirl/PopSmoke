package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopLayout
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.popShadow

/**
 * 锚定浮层：贴在触发元素下方、**右边缘对齐**。
 *
 * 为什么不用 `Popup(alignment = Alignment.TopEnd)`：那个 alignment 是把浮层的左上角
 * 放到锚点区域的 (width, 0) 处（即锚点右上角），面板会整个跑到屏幕外。
 * 这里自己算位置：右边缘对齐锚点、上沿落在锚点下方、再夹在窗口内。
 *
 * 为什么这次不用 `PopSelect` 那种「就地展开」：顶栏只有 60dp 高，就地展开会把
 * 整个内容区往下推、页面跳动；而这里的浮层本来就贴在屏幕右上角，不会盖住手指
 * 正在点的东西。两处的取舍不一样，所以保留两种形态。
 *
 * `PopupProperties(focusable = true)` 自带"点外部关闭"和"返回键关闭"，不需要手写监听。
 */
@Composable
fun PopMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    offsetY: Dp = 0.dp,
    width: Dp? = null,
    padding: PaddingValues = PaddingValues(PopSpace.S3),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val density = LocalDensity.current
    val offsetPx = with(density) { offsetY.roundToPx() }
    val edge = with(density) { PopSpace.S2.roundToPx() }

    val positionProvider = object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
        ): IntOffset {
            val x = (anchorBounds.right - popupContentSize.width)
                .coerceIn(edge, (windowSize.width - popupContentSize.width - edge).coerceAtLeast(edge))
            val y = (anchorBounds.bottom + offsetPx)
                .coerceAtMost((windowSize.height - popupContentSize.height - edge).coerceAtLeast(edge))
            return IntOffset(x, y)
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        PopMenuPanel(modifier = modifier, width = width, padding = padding, content = content)
    }
}

/** 浮层面板本体，单独抽出来是为了能在截图测试里直接渲染（Popup 是独立窗口，JVM 截图抓不到）。 */
@Composable
fun PopMenuPanel(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    padding: PaddingValues = PaddingValues(PopSpace.S3),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.widthIn(min = PopLayout.MenuMinWidth))
            // 顺序与 PopCard 一致：阴影先画、底色后盖，反过来阴影会压住内容
            .popShadow(PopShadow.PopLg)
            .background(PopColor.Core.White)
            .popBorder(PopStroke.Default, PopColor.Core.Black)
            .padding(padding),
        content = content,
    )
}

/**
 * 菜单项：Web 版是 `px-4 py-2`，选中态浅灰底（不是反色），按下时不换色。
 * 这里把整行做成点击区 —— 文字菜单最小可靠点击高度就该是这一行的全高。
 */
@Composable
fun PopMenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) PopColor.Neutral.Gray200 else PopColor.Core.White)
            .clickable(onClick = onClick)
            .padding(horizontal = PopSpace.S4, vertical = PopSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PopDisplay(
            text = text,
            size = PopTypeScale.Base,
            weight = if (selected) FontWeight.Black else FontWeight.Bold,
            maxLines = 1,
        )
    }
}

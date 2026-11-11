package com.popsmoke.tracker.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopGlass
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.popInkMuted
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.popShadow

/**
 * 分组容器 —— 内容区的「安静的底」。
 *
 * ## 为什么要造这一层
 *
 * 液态玻璃最容易犯的错，是把**每一个**容器都做成玻璃。单看一块确实惊艳，
 * 可一屏铺满之后就没有任何一块是焦点：眼睛不知道该落哪里，而且每块玻璃都在跟背景抢注意力，
 * 最后的结果是「什么都不像玻璃」。这不是参数问题，是预算问题 ——
 * 材质和颜色一样，全用等于没用。
 *
 * 所以这里定一条规矩：**玻璃只给浮动层**（顶栏、底栏、弹窗、菜单、主按钮）。
 * 页面内容一律用这种平底：一层压住背景的浅白 + 一根发丝线。
 * 没有折射、没有高光边、没有投影 —— 它负责让字读得清、让分组看得见，
 * 但**不参与材质表演**。这样才有东西衬托出顶上那条浮动导航是玻璃。
 *
 * 波普主题下它退化成原来的贴纸语言：纸白底 + 4dp 黑框 + 硬阴影。
 */
@Composable
fun PopGroup(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(PopSpace.S4),
    shape: Shape? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val current = LocalSkin.current
    val resolved = shape ?: RoundedCornerShape(current.radiusPanel)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (current.glass) {
                    Modifier.background(Color.White.copy(alpha = PopGlass.GroupAlpha), resolved)
                } else {
                    Modifier
                        .popShadow(PopShadow.Pop, shape = resolved)
                        .background(current.palette.surface, resolved)
                },
            )
            // 描边在玻璃下自动退成 hairline（见 popBorder），不必在这里分支
            .popBorder(PopStroke.Default, current.palette.ink, resolved)
            .clip(resolved)
            .padding(padding),
        content = content,
    )
}

/**
 * 分组标题：放在**组外面**，不是组里面的第一行。
 *
 * 参考 macOS / 各家系统设置页的做法 —— 标题在组外，用弱化的小字，
 * 一眼就能看出「下面是同一类的几条」。反过来把标题做成组内第一行、
 * 或者做成压在组边上的悬浮标签（上一版就是那样），读者会把它当成一个可点的条目。
 */
@Composable
fun PopGroupLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val current = LocalSkin.current
    PopDisplay(
        text = text,
        modifier = modifier.padding(start = PopSpace.S2, bottom = PopSpace.S2),
        size = if (current.glass) PopTypeScale.Sm else PopTypeScale.Base,
        weight = if (current.glass) FontWeight.Bold else FontWeight.Black,
        color = if (current.glass) popInkMuted else popInk,
        tracking = if (current.glass) PopTracking.Wide else PopTracking.Normal,
        maxLines = 2,
    )
}

/**
 * 一个完整的小节：标题在组外 + 一块静默分组。
 *
 * 这是页面里最常写的组合，包一层是为了让调用点从
 * `PopCard(title = ...){}` 换过来只改函数名，不改缩进、不改括号 ——
 * 结构迁移最怕的就是顺手改出一处多/少一个大括号。
 */
@Composable
fun PopSection(
    title: String,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(PopSpace.S4),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        PopGroupLabel(title)
        PopGroup(padding = padding, content = content)
    }
}

/**
 * 组内分隔线。
 *
 * 只要行是「一坨可点的东西」，就应该有线把它和下一行分开 —— 这是列表的语法，
 * 比留白更省地方，也更像系统设置。玻璃下是一条 9% 的发丝线，
 * 波普下是一根实黑（分栏线是漫画分格的语言）。
 */
@Composable
fun PopDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp,
) {
    val current = LocalSkin.current
    val thickness = if (current.glass) 1.dp else 1.5.dp
    val color = if (current.glass) current.palette.ink.copy(alpha = PopGlass.DividerAlpha) else current.palette.ink
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(thickness)
            .background(color),
    )
}

/**
 * 页面大标题。
 *
 * 上一版把标题做成一枚悬浮玻璃胶囊，压在整块玻璃面板的上沿 ——
 * 那是全屏最丑的一处：两层玻璃叠在一起，胶囊的圆角边缘和面板的圆角边缘只差几像素，
 * 读起来像贴纸贴歪了。标题本来就该是字，不需要材质。
 *
 * 左对齐、贴内容边、下面可以挂一行小字（统计、说明）。
 */
@Composable
fun PopPageTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val current = LocalSkin.current
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            PopDisplay(
                text = title,
                modifier = Modifier.weight(1f),
                size = PopTypeScale.Xxxl,
                weight = FontWeight.Black,
                color = current.palette.ink,
                maxLines = 1,
            )
            if (trailing != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PopSpace.S2),
                    content = trailing,
                )
            }
        }
        if (subtitle != null) {
            Box(Modifier.height(PopSpace.S1))
            PopBody(
                text = subtitle,
                size = PopTypeScale.Sm,
                weight = FontWeight.Bold,
                color = current.palette.inkMuted,
                maxLines = 2,
            )
        }
    }
}

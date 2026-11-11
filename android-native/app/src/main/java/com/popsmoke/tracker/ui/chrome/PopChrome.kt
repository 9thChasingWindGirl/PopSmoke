package com.popsmoke.tracker.ui.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.data.Languages
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopLayout
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.ui.atoms.PopColorPicker
import com.popsmoke.tracker.ui.atoms.PopMenu
import com.popsmoke.tracker.ui.atoms.PopMenuItem
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.PopTapTarget

/**
 * 顶栏。三个容易被忽略但必须做对的点：
 * 1. 高度 = 内容 60dp + 状态栏 inset，白色底一直铺到屏幕顶端（Web 版用 paddingTop 模拟，原生用真实 inset）
 * 2. 四周 4dp 黑边：这是波普"贴纸感"的一部分，不要为了沉浸式去掉顶部那条线
 * 3. 语言和主题色块视觉很小，触摸目标补到 48dp
 *
 * 高度用 `padding(top) + heightIn(min)` 而不是 `height(60 + inset)`：
 * 后者把高度钉死了，系统字体放大（fontScale 1.3/1.5）时文字会被裁掉；
 * 前者让内容自己撑开，inset 只算一次，也顺带解决了刘海屏裁切区。
 *
 * 两个小键是**弹菜单**，不是点一下循环切换：Web 版就是弹层，循环切换在四种语言下
 * 要连点三次才能回到上一种，没法撤销。菜单还顺带把"当前是哪一种"显示出来。
 */
@Composable
fun PopTopBar(
    accentColor: Color,
    languageLabel: String,
    topInset: Dp,
    themeDescription: String,
    languageDescription: String,
    currentTheme: String,
    currentLanguage: String,
    themePresets: List<String>,
    onTitleClick: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var themeMenuOpen by remember { mutableStateOf(false) }
    var languageMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PopColor.Core.White)
            // border 必须在 windowInsets padding 之前：它要包住「含状态栏」的整块矩形，
            // 放到里面就会变成"状态栏下方一条线"，顶上多出一段白边，看着像预留过高。
            .border(PopStroke.Default, PopColor.Core.Black)
            .padding(top = topInset)
            .heightIn(min = PopLayout.HeaderHeight)
            .padding(horizontal = PopSpace.S3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.pointerInput(Unit) { detectTapGestures { onTitleClick() } },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PopDisplay(
                text = "POP",
                size = PopTypeScale.Xl,
                weight = FontWeight.Normal,
                tracking = PopTracking.Wide,
            )
            PopDisplay(
                text = "SMOKE",
                size = PopTypeScale.Xl,
                weight = FontWeight.Normal,
                tracking = PopTracking.Wide,
                color = accentColor,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PopSpace.S2),
        ) {
            Box {
                PopTapTarget(
                    onClick = {
                        languageMenuOpen = false
                        themeMenuOpen = true
                    },
                    modifier = Modifier.semantics {
                        contentDescription = themeDescription
                        role = Role.Button
                    },
                ) {
                    Box(
                        Modifier
                            .size(PopLayout.Chip)
                            .background(accentColor)
                            .border(PopStroke.Thin, PopColor.Core.Black),
                    )
                }

                // 浮层锚定在 48dp 触摸区上，所以 y 偏移取 0：面板上沿落在触摸区下沿，
                // 视觉上距色块约 10dp（Web 版是色块下 8px）。取负值会把面板压到色块边上。
                PopMenu(
                    expanded = themeMenuOpen,
                    onDismiss = { themeMenuOpen = false },
                    width = PopLayout.ThemePanelWidth,
                ) {
                    PopColorPicker(
                        value = currentTheme,
                        // 不在这里关面板：Web 的取色面板也是选完继续留着（可以连着试几种颜色），
                        // 关掉靠点外部。如果一改就关，手动输色号时输到第 7 位面板就没了。
                        onChange = onSelectTheme,
                        presets = themePresets,
                    )
                }
            }

            // 语言键：Web 版是 w-8 h-8 的固定方块（2rem），不是按文字撑开的。
            // 早先用 padding 撑开，实测只有 22dp 见方、字号还大一号，和左边的色块摆在一起明显不齐。
            Box {
                PopTapTarget(
                    onClick = {
                        themeMenuOpen = false
                        languageMenuOpen = true
                    },
                    modifier = Modifier.semantics {
                        contentDescription = languageDescription
                        role = Role.Button
                    },
                ) {
                    Box(
                        Modifier
                            .size(PopLayout.Chip)
                            .border(PopStroke.Thin, PopColor.Core.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        PopDisplay(
                            text = languageLabel,
                            size = PopTypeScale.Xs,
                            weight = FontWeight.Bold,
                            uppercase = true,
                        )
                    }
                }

                PopMenu(
                    expanded = languageMenuOpen,
                    onDismiss = { languageMenuOpen = false },
                    // 固定 80dp + 零内边距：对应 Web 的 `min-w-[80px]`，菜单项自己贴边（px-4 py-2）。
                    // 面板宽度必须定死 —— 菜单项是 fillMaxWidth，面板一旦交给内容自适应
                    // 就会一路铺满屏幕（这一版实测过，语言菜单变成一条横贯全屏的白条）。
                    width = PopLayout.MenuMinWidth,
                    padding = PaddingValues(0.dp),
                ) {
                    Languages.all.forEach { code ->
                        PopMenuItem(
                            text = Languages.shortLabel(code),
                            selected = code == currentLanguage,
                            onClick = {
                                onSelectLanguage(code)
                                languageMenuOpen = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 底栏：5 个等宽文字页签，顶部 4dp 黑线，高度 = 60dp + 手势条 inset。
 * 选中态黑字、未选中灰字 60% 透明度；整块高度都是点击区（不做 24dp 的小按钮）。
 *
 * 高度必须是**固定值**，不能像顶栏那样用 `heightIn(min)`：
 * 页签的子布局是 `fillMaxHeight()`，一旦最大高度不受限，它就会撑满整个剩余空间，
 * 把上面的内容区压成 0（实测表现：内容一片空白、页签文字跑到屏幕中间）。
 * 底栏只有一行文字，本来也不需要随字体放大而变高。
 */
@Composable
fun PopBottomBar(
    items: List<String>,
    selectedIndex: Int,
    bottomInset: Dp,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PopLayout.BottomBarHeight + bottomInset)
            .background(PopColor.Core.White)
            .drawBehind {
                drawRect(
                    color = PopColor.Core.Black,
                    size = Size(size.width, PopStroke.Default.toPx()),
                )
            }
            .padding(top = PopStroke.Default, bottom = bottomInset, start = PopSpace.S2, end = PopSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PopSpace.S2),
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val interaction = remember(index) { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onSelect(index) },
                    )
                    .graphicsLayer { alpha = if (selected) 1f else 0.6f },
                contentAlignment = Alignment.Center,
            ) {
                PopDisplay(
                    text = label,
                    size = PopTypeScale.Sm,
                    weight = FontWeight.Normal,
                    tracking = PopTracking.Wide,
                    color = if (selected) PopColor.Core.Black else PopColor.Neutral.Gray400,
                )
            }
        }
    }
}

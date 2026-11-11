package com.popsmoke.tracker.ui.chrome

import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.data.Languages
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopGlass
import com.popsmoke.tracker.design.PopLayout
import com.popsmoke.tracker.design.PopSkin
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.ui.atoms.PopColorPicker
import com.popsmoke.tracker.ui.atoms.PopMenu
import com.popsmoke.tracker.ui.atoms.PopMenuItem
import com.popsmoke.tracker.ui.foundation.LocalPopBackdrop
import com.popsmoke.tracker.ui.foundation.PopBrand
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.PopIcon
import com.popsmoke.tracker.ui.foundation.PopIconKind
import com.popsmoke.tracker.ui.foundation.PopTapTarget
import com.popsmoke.tracker.ui.foundation.skinSurface

/**
 * 外壳（顶栏 / 底栏）占掉的高度。
 *
 * 这一层存在的唯一原因：顶栏和底栏现在是**浮在内容之上**的两个兄弟节点（不再是 Column 的成员），
 * 内容要能滚到它们底下去，就必须自己知道自己该让出多少。页面的 `contentPadding`
 * 从这里取，而不是各自写死 60dp —— 一旦某一版把底栏做高了两像素，只有这里要改。
 */
@androidx.compose.runtime.Immutable
data class ChromeInsets(val top: Dp = 0.dp, val bottom: Dp = 0.dp)

val LocalChromeInsets = staticCompositionLocalOf { ChromeInsets() }

/** 底栏的一项：一个字 + 一个图标。图标只有玻璃主题用得到（波普是纯文字页签）。 */
data class PopTabItem(val label: String, val icon: PopIconKind)

/** 玻璃底栏是浮动胶囊，高度与边距都集中在这里，PopApp 算内容留白时共用。 */
object PopNavMetrics {
    val Height = 58.dp
    val Margin = PopSpace.S4
}

/**
 * 顶栏。
 *
 * ## 这一版改了什么
 *
 * 上一版是「一条不透明的磨砂白条 + 下沿一条发丝线」。问题在于它读起来是**一块玻璃板**，
 * 而不是一层贴在内容上方的膜：白条自己的明度和背景差得很远，边界又是一条硬线，
 * 于是屏幕被切成上下两块。用户拿来的参考图里，顶栏是**模糊**的 —— 没有边界，
 * 上面那层只是把背后的东西糊掉、再渐隐下去。
 *
 * 现在玻璃主题下的顶栏是：**背景的模糊切片 + 一层极淡的白纱 + 下沿渐变到全透明**。
 * 那块模糊切片就是 `PopBackdrop.blurred`（玻璃面板内侧采样的同一张图），
 * 所以顶栏和页面里的玻璃是同一个材质，不是两种白。
 *
 * 波普主题完全不变：白底 + 4dp 黑框是贴纸语言的一部分。
 *
 * ## 三条保留的规矩
 *
 * 1. 高度 = 内容 60dp + 状态栏 inset，白色底一直铺到屏幕顶端
 * 2. 语言和主题色块视觉很小，触摸目标补到 48dp
 * 3. 两个小键是**弹菜单**，不是点一下循环切换
 */
@Composable
fun PopTopBar(
    accentColor: Color,
    languageLabel: String,
    topInset: Dp,
    themeDescription: String,
    languageDescription: String,
    customColorLabel: String,
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
    val current = LocalSkin.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .chromeSurface(current, lineAtBottom = current.glass.not())
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
            // 品牌标识是 logo，不是界面文案：两套皮肤都用 Bangers（见 PopLogotype）。
            PopBrand(accent = accentColor)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            // 负间距是为了把**视觉**收紧，而不是把触摸目标缩小。
            //
            // 每个圆片外面套着 48dp 的无障碍触摸目标（PopA11y.TapTarget），圆片只有 28dp，
            // 于是两个圆片之间天然隔着 20dp 的空白 —— 直接写 spacedBy(5dp)，
            // 屏幕上看到的间隙是 25dp，怎么调数字都觉得"散"。
            // -10dp 之后视觉间隙 10dp，触摸目标仍各剩 38dp（这两个键都是弹菜单，误触成本低）。
            horizontalArrangement = Arrangement.spacedBy((-10).dp),
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
                            .then(
                                if (current.glass) {
                                    // 玻璃小圆片：圆才让折射有解（方形 radius=0，
                                    // 壁宽会被 clamp 到 0.01dp，等于没有折射）
                                    Modifier.skinSurface(
                                        current = current,
                                        shape = CircleShape,
                                        fill = accentColor.copy(alpha = 0.52f),
                                        lift = 0.dp,
                                    )
                                } else {
                                    Modifier
                                        .background(accentColor)
                                        .border(PopStroke.Thin, PopColor.Core.Black)
                                },
                            ),
                    )
                }

                PopMenu(
                    expanded = themeMenuOpen,
                    onDismiss = { themeMenuOpen = false },
                    width = PopLayout.ThemePanelWidth,
                ) {
                    PopColorPicker(
                        value = currentTheme,
                        onChange = onSelectTheme,
                        presets = themePresets,
                        customLabel = customColorLabel,
                    )
                }
            }

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
                            .then(
                                if (current.glass) {
                                    Modifier.skinSurface(
                                        current = current,
                                        shape = CircleShape,
                                        fill = current.palette.surfaceStrong.copy(alpha = 0.46f),
                                        lift = 0.dp,
                                    )
                                } else {
                                    Modifier.border(
                                        PopStroke.Thin,
                                        PopColor.Core.Black,
                                        androidx.compose.ui.graphics.RectangleShape,
                                    )
                                },
                            ),
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
 * 底栏。
 *
 * ## 玻璃：一枚浮在底部的胶囊
 *
 * 原来是一条整宽的白条，和顶栏一起把屏幕夹成一个表格。参考图里的底栏是**浮起来**的：
 * 左右留边、下沿离手势条还有一段、圆角很大、材质和页面里任何一块卡片都不一样。
 * 这一条是整套界面里唯一「应该」是玻璃的东西 —— 它真的盖在内容上面，
 * 内容从它下面滚过去，所以它的通透才有意义。
 *
 * 每一项是「图标 + 字」上下叠，选中项染主题色并垫一块淡染胶囊。只有字的话，
 * 五个页签在扫视时是一排等重的灰色文字，找不到自己在哪一页。
 *
 * ## 波普：保持原样
 *
 * 整宽、顶部黑线、纯文字。贴纸语言里没有"浮起的胶囊"，那是另一套语法。
 */
@Composable
fun PopBottomBar(
    items: List<PopTabItem>,
    selectedIndex: Int,
    bottomInset: Dp,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = LocalSkin.current
    if (current.glass) {
        // 整圆的浮岛：58dp 高时 corner = 29dp，正好是胶囊。
        // 用 CircleShape 而不是 radiusButton(26dp) —— 后者在矮条上还留着 3dp 的直边，
        // 那段直边会让边缘折射在那儿"断一下"，浮岛看起来像被削平了两头。
        val shape = CircleShape
        Box(
            modifier
                .fillMaxWidth()
                .padding(
                    start = PopNavMetrics.Margin,
                    end = PopNavMetrics.Margin,
                    bottom = bottomInset + PopNavMetrics.Margin,
                ),
        ) {
            val accentColor = accent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PopNavMetrics.Height)
                    .skinSurface(
                        current = current,
                        shape = shape,
                        // 0.14：浮岛必须明显比内容区的分组更透，背后的色带和织纹要能继续
                        // 在岛里存在。材质感交给边缘折射与那根菲涅尔线 —— 靠不透明度堆出来的
                        // "白"，在浅底上只会读成一条塑料条，周围还被投影糊出一圈灰雾。
                        fill = current.palette.surfaceStrong.copy(alpha = PopGlass.NavAlpha),
                        // 投影收近：18dp 的落距在矮条上离岛太远，读作"底下垫了一块板"。
                        lift = 10.dp,
                        tint = accentColor,
                    )
                    .padding(horizontal = PopSpace.S1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val interaction = remember(index) { MutableInteractionSource() }
                    // 选中色做动画：页签切换时颜色滑过去，比直接跳变更像"同一个指示器在移动"
                    val ink = animateColorAsState(
                        targetValue = if (selected) accentColor else current.palette.inkSecondary,
                        label = "nav-ink-$index",
                    )
                    val bed = animateColorAsState(
                        // 0.2：浮岛本身已经很透，选中胶囊如果只有 0.15，在浅底上几乎读不出边界 ——
                        // 它在岛里是「当前在哪一页」的唯一指示，必须站得住
                        targetValue = if (selected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                        label = "nav-bed-$index",
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(PopNavMetrics.Height - PopSpace.S2)
                            .background(bed.value, shape)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onSelect(index) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PopIcon(kind = item.icon, size = 19.dp, color = ink.value, strokeUnits = 1.8f)
                        Spacer(Modifier.height(3.dp))
                        PopDisplay(
                            text = item.label,
                            size = PopTypeScale.Xs,
                            weight = if (selected) FontWeight.Black else FontWeight.Bold,
                            tracking = PopTracking.Normal,
                            color = ink.value,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PopLayout.BottomBarHeight + bottomInset)
            .chromeSurface(current, lineAtBottom = false)
            .padding(top = PopStroke.Default, bottom = bottomInset, start = PopSpace.S2, end = PopSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PopSpace.S2),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val interaction = remember(index) { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(PopLayout.BottomBarHeight)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                PopDisplay(
                    text = item.label,
                    size = PopTypeScale.Sm,
                    weight = FontWeight.Normal,
                    tracking = PopTracking.Wide,
                    color = if (selected) current.palette.ink else current.palette.inkMuted,
                )
            }
        }
    }
}

/**
 * 顶栏/底栏的底。
 *
 * 波普：纯白 + 4dp 黑框（框要包住含状态栏的整块矩形，放到 inset 里面会变成
 * "状态栏下方一条线"，顶上多出一段白边，看着像预留过高）。
 *
 * 玻璃：见 [chromeBlur]。这里只保留一个降级：拿不到背景快照时（极老设备）
 * 退回一层极淡的白纱，至少保证字读得清。
 */
@Composable
private fun Modifier.chromeSurface(current: PopSkin, lineAtBottom: Boolean): Modifier =
    if (current.glass) {
        chromeBlur()
    } else {
        background(PopColor.Core.White).border(PopStroke.Default, PopColor.Core.Black)
    }

/**
 * 背景的模糊切片，下沿渐变到透明。
 *
 * 为什么不是"一层半透明白"：那读起来是一块盖在内容上的板子，而且和背景之间
 * 一定有一条边界（明度差摆在那儿）。模糊切片的好处是它**本来就是背景**，
 * 只是丢了高频 —— 上方看起来像背景本身虚化了，下沿用 DST_IN 把它渐渐擦掉，
 * 于是没有边界，只有一段"失焦"。
 *
 * 实现上必须走 saveLayer：要先把这个条带画进离屏层，再用一层 0→1 的 alpha 渐变
 * 以 DST_IN 擦除它。直接画在画布上擦，会把下面已经画好的内容一起擦掉。
 */
@Composable
private fun Modifier.chromeBlur(): Modifier = composed {
    val backdrop = LocalPopBackdrop.current
    val origin = remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    this
        .onGloballyPositioned { coords -> origin.value = coords.positionInRoot() }
        .drawBehind {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@drawBehind
            if (backdrop == null) {
                // 极老设备（没有背景快照）：退回一层渐隐的白纱，
                // 至少保证白字/黑字都读得清，也不会有一条硬边把屏幕切开
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.55f),
                        0.62f to Color.White.copy(alpha = 0.34f),
                        1f to Color.Transparent,
                    ),
                )
                return@drawBehind
            }
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                val layer = nc.saveLayer(0f, 0f, w, h, null)

                val slice = nc.save()
                nc.clipRect(0f, 0f, w, h)
                nc.translate(-origin.value.x, -origin.value.y)
                nc.drawBitmap(backdrop.blurred.asAndroidBitmap(), 0f, 0f, chromeImagePaint)
                nc.restoreToCount(slice)

                // 白纱：模糊带在深色光斑上会压不住字，给一层很淡的白统一明度
                chromeVeilPaint.color = android.graphics.Color.argb(
                    (255 * PopGlass.ChromeVeilAlpha).toInt(), 255, 255, 255,
                )
                nc.drawRect(0f, 0f, w, h, chromeVeilPaint)

                // 下沿渐隐：上 55% 完全不透明，最后 18% 散尽
                chromeMaskPaint.shader = android.graphics.LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(
                        0xFFFFFFFF.toInt(),
                        0xFFFFFFFF.toInt(),
                        0x8CFFFFFF.toInt(),
                        0x00FFFFFF,
                    ),
                    floatArrayOf(0f, 0.55f, 0.82f, 1f),
                    android.graphics.Shader.TileMode.CLAMP,
                )
                chromeMaskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                nc.drawRect(0f, 0f, w, h, chromeMaskPaint)
                nc.restoreToCount(layer)
            }
        }
}

private val chromeImagePaint = android.graphics.Paint(
    android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG,
)
private val chromeVeilPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
private val chromeMaskPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

package com.popsmoke.tracker.ui.chrome

import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
import com.popsmoke.tracker.ui.foundation.GlassTone
import com.popsmoke.tracker.ui.foundation.LocalPopBackdrop
import com.popsmoke.tracker.ui.foundation.PopBackdrop
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

    /** 渐进模糊的过渡带高度：低于 12dp 像硬切，高于 32dp 像蒙版。 */
    val Ramp = 26.dp
}

/**
 * 顶栏。
 *
 * ## 这一版：模糊从"一条"变成"一段"
 *
 * 上一版顶栏是背景的模糊切片 + 一层白纱，下沿用 alpha 渐变擦掉。边界是没有了，
 * 但整条是一个**均匀**的模糊 —— 内容滚上来的时候，会在下沿突然从"很糊"跳到"清晰"，
 * 中间没有过渡。参考图里那种"内容从里面浮出来"的感觉，靠的是**模糊强度本身在渐变**。
 *
 * 所以现在叠两层：heavy（背后有文字，必须糊开）+ soft（保留轮廓），
 * 各自配一个方向的遮罩。贴顶栏的一段是 heavy 全强度，往下 heavy 退、soft 顶一段，
 * 最后一起散尽。这叫 progressive blur，视觉上等价于"4~6 层逐级递减"，
 * 但只花两次绘制 —— 因为两档模糊图是预生成好的。
 *
 * ## 三条保留的规矩
 *
 * 1. 高度 = 内容 64dp + 状态栏 inset，模糊一直铺到屏幕顶端
 * 2. 语言和主题色块视觉很小，触摸目标补到 48dp
 * 3. 两个小键是**弹菜单**，不是点一下循环切换
 */
@Composable
fun PopTopBar(
    accentColor: Color,
    languageLabel: String,
    topInset: Dp,
    tone: GlassTone,
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
    // 暗壁纸上品牌字必须是浅色，否则黑字压在夜山上等于没写。
    val ink = if (current.glass) tone.ink else Color.Unspecified

    Row(
        modifier = modifier
            .fillMaxWidth()
            .chromeSurface(current, edge = ChromeEdge.Bottom, tone = tone)
            // 过渡带是顶栏**外面**的一段：模糊从条内一直化到条下，
            // 内容从这段里浮出来。给它单独让出高度，内容才不会在条内被糊掉一半。
            .padding(top = topInset, bottom = PopNavMetrics.Ramp)
            .heightIn(min = PopLayout.HeaderHeight)
            .padding(horizontal = PopSpace.S3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .pointerInput(Unit) { detectTapGestures { onTitleClick() } }
                // 品牌标识是 logo，不是界面文案：两套皮肤都用 Bangers（见 PopLogotype）。
                // 26sp + 左边距 24dp：17.5sp 装在 60dp 的条里，视觉权重还不如右上角两个小圆片，
                // 一眼看过去像"页面没做完"。标识是这个 App 的脸，它得先站住。
                .padding(start = PopSpace.S2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PopBrand(accent = accentColor, size = PopTypeScale.Xxxl, ink = ink)
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
                                        strongBlur = true,
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
                                        strongBlur = true,
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
                            color = if (current.glass) tone.ink else Color.Unspecified,
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
 * ## 选中与按压是两件事
 *
 * 参考图把这两个语义分得很干净：
 *
 * - **选中**只换颜色，没有背景块。用形状表达选中会和玻璃的边界打架 ——
 *   一块淡色胶囊压在玻璃里，边缘和内容同色，读作"脏了一块"。
 * - **按压**是一团光，跟着按下的 x 走，横跨约两格、纵向吃满整个胶囊、边缘完全渐隐。
 *   它读作"材质内部被点亮"，不是"选中了某一格"—— 所以**不按格子裁切**。
 *
 * 光放开手就散，选中留在这里。两个状态不共用一个视觉通道，才不会互相干扰。
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
    tone: GlassTone,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = LocalSkin.current
    if (current.glass) {
        // 整圆的浮岛：58dp 高时 corner = 29dp，正好是胶囊。
        // 用 CircleShape 而不是 radiusButton(26dp) —— 后者在矮条上还留着 3dp 的直边，
        // 那段直边会让边缘折射在那儿"断一下"，浮岛看起来像被削平了两头。
        val shape = CircleShape
        val accentColor = accent
        val ink = tone.ink
        val inkMuted = tone.inkSecondary
        // 暗底上主题色要提亮，否则深绿/深青压在夜山上读不清（选中态是唯一的定位线索）
        val selectedInk = if (tone.dark) accentColor.lighten(0.32f) else accentColor

        var glowX by remember { mutableFloatStateOf(0f) }
        var pressed by remember { mutableStateOf(false) }
        val glowAlpha by animateFloatAsState(
            targetValue = if (pressed) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (pressed) 110 else 340,
                easing = LinearOutSlowInEasing,
            ),
            label = "nav-glow",
        )

        Box(modifier.fillMaxWidth().chromeSurface(current, edge = ChromeEdge.BottomEdge, tone = tone)) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = PopNavMetrics.Margin,
                        end = PopNavMetrics.Margin,
                        bottom = bottomInset + PopNavMetrics.Margin,
                    )
                    .fillMaxWidth()
                    .height(PopNavMetrics.Height)
                    .skinSurface(
                        current = current,
                        shape = shape,
                        // 浮岛必须明显比内容区的分组更透，背后的东西要能继续在岛里存在。
                        // 材质感交给边缘折射与那圈内辉光 —— 靠不透明度堆出来的"白"，
                        // 在浅底上只会读成一条塑料条。填充不跟壁纸变色，
                        // 明暗回应长在边缘的内辉光上（见 GlassTone.glowColor）。
                        fill = current.palette.surfaceStrong.copy(alpha = PopGlass.NavAlpha),
                        lift = 10.dp,
                        tint = accentColor,
                        strongBlur = true,
                    )
                    // 观察按压位置，不消费事件 —— 子项的 clickable 照常工作。
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val change = awaitPointerEvent(PointerEventPass.Initial)
                                    .changes.firstOrNull() ?: continue
                                if (change.pressed) {
                                    glowX = change.position.x
                                    pressed = true
                                } else if (pressed) {
                                    pressed = false
                                }
                            }
                        }
                    }
                    .drawBehind {
                        if (glowAlpha <= 0.01f) return@drawBehind
                        // 半径 1.6 格宽：小于 1 格读作"这个按钮亮了"，
                        // 大于 2 格会盖住邻居、又变成"选中了一块区域"。
                        val radius = (size.width / items.size.coerceAtLeast(1)) * 1.6f
                        val center = Offset(glowX, size.height / 2f)
                        // 亮底上白光看不见，改用主题色：光的意义是"这里被按到了"，
                        // 不是"这里有一团白"。暗底上反过来 —— 主题色会陷进深色玻璃里。
                        val light = if (tone.dark) Color.White else accentColor
                        val outline = shape.createOutline(size, layoutDirection, this)
                        clipPath(Path().apply { addOutline(outline) }) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0f to light.copy(alpha = 0.32f * glowAlpha),
                                    0.45f to light.copy(alpha = 0.18f * glowAlpha),
                                    1f to Color.Transparent,
                                    center = center,
                                    radius = radius,
                                ),
                                radius = radius,
                                center = center,
                            )
                        }
                    }
                    .padding(horizontal = PopSpace.S1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val interaction = remember(index) { MutableInteractionSource() }
                    // 选中色做动画：页签切换时颜色滑过去，比直接跳变更像"同一个指示器在移动"
                    val itemInk = animateColorAsState(
                        targetValue = if (selected) selectedInk else inkMuted,
                        label = "nav-ink-$index",
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(PopNavMetrics.Height - PopSpace.S2)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onSelect(index) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PopIcon(
                            kind = item.icon,
                            size = 19.dp,
                            color = itemInk.value,
                            strokeUnits = if (selected) 2.1f else 1.7f,
                        )
                        Spacer(Modifier.height(3.dp))
                        PopDisplay(
                            text = item.label,
                            size = PopTypeScale.Xs,
                            weight = if (selected) FontWeight.Black else FontWeight.Bold,
                            tracking = PopTracking.Normal,
                            color = itemInk.value,
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
            .chromeSurface(current, edge = ChromeEdge.None, tone = tone)
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

/** 外壳的哪条边需要软化。 */
private enum class ChromeEdge { None, Bottom, BottomEdge }

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
private fun Modifier.chromeSurface(
    current: PopSkin,
    edge: ChromeEdge,
    tone: GlassTone,
): Modifier =
    if (current.glass) {
        chromeBlur(edge, tone)
    } else {
        background(PopColor.Core.White).border(PopStroke.Default, PopColor.Core.Black)
    }

/**
 * 背景的模糊切片 + 渐进过渡带。
 *
 * ## 为什么不是"一层半透明白"
 *
 * 那读起来是一块盖在内容上的板子，而且和背景之间一定有一条边界（明度差摆在那儿）。
 * 模糊切片的好处是它**本来就是背景**，只是丢了高频。
 *
 * ## 渐进模糊（progressive blur）
 *
 * 两层切片叠着画，各配一个方向的遮罩：
 *
 * - `heavy`（背后有滚动文字的强度）从贴紧外壳的一侧开始全强度，越过过渡带 60% 处退出；
 * - `soft`（还看得清轮廓的强度）在过渡带中段顶上来，到末端散尽。
 *
 * 结果是"最靠近外壳最糊、往内容方向越来越清"，中间没有强度突变 ——
 * 眼下的感受是内容从玻璃里浮出来，而不是被一刀切断。
 *
 * 真做 4~6 层要预生成同样多的模糊图，那是几十 MB 换一条肉眼几乎分不出的过渡；
 * 两档已经是这个尺度上的收益上限。
 *
 * ## 实现必须走 saveLayer
 *
 * 要先把切片画进离屏层，再用一层 alpha 渐变以 DST_IN 擦除它。直接画在画布上擦，
 * 会把下面已经画好的内容一起擦掉。
 */
@Composable
private fun Modifier.chromeBlur(edge: ChromeEdge, tone: GlassTone): Modifier = composed {
    val backdrop = LocalPopBackdrop.current
    val origin = remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { coords -> origin.value = coords.positionInRoot() }
        .drawBehind {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@drawBehind
            if (edge == ChromeEdge.None) return@drawBehind
            if (backdrop == null) {
                // 极老设备（没有背景快照）：退回一层渐隐的纱，
                // 至少保证白字/黑字都读得清，也不会有一条硬边把屏幕切开
                val veil = if (tone.dark) Color.Black else Color.White
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to veil.copy(alpha = 0.55f),
                        0.62f to veil.copy(alpha = 0.34f),
                        1f to Color.Transparent,
                    ),
                )
                return@drawBehind
            }
            val ramp = PopNavMetrics.Ramp.toPx().coerceAtMost(h * 0.7f)
            drawBlurStack(backdrop, origin.value, w, h, edge, ramp, tone)
        }
}

/**
 * 两次切片绘制。`heavy` 与 `soft` 的遮罩相反：前者在近端满、后者在远端补位。
 *
 * 坐标说明：切片的 `drawBitmap` 用的是全屏位图，所以要先把画布平移回根坐标系；
 * 而遮罩（DST_IN）用的是**局部**坐标 —— 它必须和这个节点自己的区域对齐。
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlurStack(
    backdrop: PopBackdrop,
    origin: Offset,
    w: Float,
    h: Float,
    edge: ChromeEdge,
    ramp: Float,
    tone: GlassTone,
) {
    val start = h - ramp
    // 顶栏：贴顶满、往下散；底栏：贴底满、往上散。
    val nearIsTop = edge == ChromeEdge.Bottom

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas

        fun paintLayer(bitmap: android.graphics.Bitmap, alphas: FloatArray) {
            val layer = nc.saveLayer(0f, 0f, w, h, null)
            val save = nc.save()
            nc.clipRect(0f, 0f, w, h)
            nc.translate(-origin.x, -origin.y)
            // 模糊图是半分辨率的，必须用矩阵缩回屏幕尺寸 —— 直接 drawBitmap 会画出
            // 一块只有屏幕一半大的图，在角落里看起来像一块贴在背景上的白纸。
            blurMatrix.setScale(backdrop.widthPx / bitmap.width, backdrop.heightPx / bitmap.height)
            nc.drawBitmap(bitmap, blurMatrix, chromeImagePaint)
            nc.restoreToCount(save)

            // 纱：模糊带在深色光斑/夜山上压不住字，给一层极淡的中性色统一明度。
            // 暗背景用黑纱而不是白纱 —— 白纱会把整条顶栏洗成灰，和"深色玻璃"的读法相反。
            val veil = if (tone.dark) 0 else 255
            chromeVeilPaint.color = android.graphics.Color.argb(
                (255 * PopGlass.ChromeVeilAlpha).toInt(), veil, veil, veil,
            )
            nc.drawRect(0f, 0f, w, h, chromeVeilPaint)

            // 遮罩只在 [start, start+ramp] 之间变化，区间外用 CLAMP 的端点值：
            // 上面的部分保留（alpha 1），下面的部分擦掉（alpha 0）。
            chromeMaskPaint.shader = android.graphics.LinearGradient(
                0f, start, 0f, start + ramp,
                intArrayOf(
                    argb(alphas[0]), argb(alphas[1]), argb(alphas[2]), argb(alphas[3]),
                ),
                floatArrayOf(0f, 0.55f, 0.80f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
            chromeMaskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            nc.drawRect(0f, 0f, w, h, chromeMaskPaint)
            chromeMaskPaint.xfermode = null
            nc.restoreToCount(layer)
        }

        // 两层的遮罩方向相反：heavy 在近端满、末端归零；soft 在过渡带中后段顶上来。
        // 叠完之后，模糊强度沿过渡带单调下降 —— 这就是 progressive blur 的全部内容。
        if (nearIsTop) {
            paintLayer(backdrop.heavyBitmap, floatArrayOf(1f, 1f, 0.42f, 0f))
            paintLayer(backdrop.softBitmap, floatArrayOf(0f, 0f, 0.66f, 0f))
        } else {
            paintLayer(backdrop.heavyBitmap, floatArrayOf(0f, 0.42f, 1f, 1f))
            paintLayer(backdrop.softBitmap, floatArrayOf(0f, 0.66f, 0f, 0f))
        }
    }
}

/** 把 0..1 的 alpha 折成 ARGB 掩膜色（RGB 恒白，只用 alpha 通道）。 */
private fun argb(alpha: Float): Int =
    ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or 0x00FFFFFF

/** 主题色提亮：暗底上用它做选中色，纯 accent 会陷进背景里。 */
private fun Color.lighten(fraction: Float): Color = Color(
    red = red + (1f - red) * fraction,
    green = green + (1f - green) * fraction,
    blue = blue + (1f - blue) * fraction,
    alpha = alpha,
)

private val chromeImagePaint = android.graphics.Paint(
    android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG,
)
private val chromeVeilPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
private val chromeMaskPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

/** 半分辨率模糊图 → 屏幕坐标。复用同一个对象，避免每帧在 draw 里 new。 */
private val blurMatrix = android.graphics.Matrix()

package com.popsmoke.tracker.feature.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopLayout
import com.popsmoke.tracker.design.PopMotion
import com.popsmoke.tracker.design.PopRadius
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTracking
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.design.onAccent
import com.popsmoke.tracker.i18n.PopStrings
import com.popsmoke.tracker.ui.atoms.PopCard
import com.popsmoke.tracker.ui.atoms.PopIconButton
import com.popsmoke.tracker.ui.atoms.PopTag
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.PopIcon
import com.popsmoke.tracker.ui.foundation.PopIconKind
import com.popsmoke.tracker.ui.foundation.PopPressable
import com.popsmoke.tracker.design.popHairline
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.popInkMuted
import com.popsmoke.tracker.design.popInkSecondary
import com.popsmoke.tracker.design.popSurface
import com.popsmoke.tracker.design.popSurfaceDeep
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.ui.foundation.jellyPress
import com.popsmoke.tracker.ui.foundation.skinSurface
import androidx.compose.runtime.remember

/**
 * 第 3 批的关键节点：仪表盘一屏。
 * 这一屏同时压到硬阴影、描边、半环仪表、3D 翻转、按压位移、纸张底纹六套视觉规则，
 * 做出来就能判断整条重写路线对不对。
 */
@Composable
fun DashboardScreen(
    ui: DashboardUiState,
    strings: PopStrings,
    onRecord: () -> Unit,
    onRequestEdit: (SmokeLog) -> Unit,
    onRequestDelete: (SmokeLog) -> Unit,
    modifier: Modifier = Modifier,
    initialFlipped: Boolean = false,
) {
    // initialFlipped 只给截图测试用：真实翻转态下背面尺寸由正面决定，
    // 单独渲染背面是量不出「是否与主区域一致」的。
    var flipped by rememberSaveable { mutableStateOf(initialFlipped) }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = PopSpace.S2, vertical = PopSpace.S2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 卡片区：weight 只用来「分配剩余空间」，卡片本身按内容高度居中，不参与拉伸。
        // 顺序也不能反：widthIn 必须在 fillMaxWidth 之前，否则 min 已经被撑满，上限失效。
        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = PopLayout.ContentMax)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            FlipCard(
                flipped = flipped,
                modifier = Modifier.fillMaxWidth(),
                front = {
                    DashboardFront(
                        ui = ui,
                        strings = strings,
                        onRecord = onRecord,
                        onOpenLogs = { flipped = true },
                    )
                },
                back = {
                    DashboardBack(
                        ui = ui,
                        strings = strings,
                        onBack = { flipped = false },
                        onRequestEdit = onRequestEdit,
                        onRequestDelete = onRequestDelete,
                    )
                },
            )
        }

        // 提示条在卡片区之外单独占位：卡片区是 weight，所以提示条永远是「先量固定高度」的那个，
        // 底栏在更外层、高度固定，任何情况下都不会被挤走。
        if (ui.alert != LimitAlert.None) {
            AlertBanner(
                alert = ui.alert,
                strings = strings,
                modifier = Modifier.padding(top = PopSpace.S2),
            )
        }
    }
}

@Composable
private fun DashboardFront(
    ui: DashboardUiState,
    strings: PopStrings,
    onRecord: () -> Unit,
    onOpenLogs: () -> Unit,
) {
    PopCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(PopSpace.S2),
    ) {
        // 标题 + 翻面键
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = PopSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PopDisplay(
                text = strings.currentGaugeLevel,
                modifier = Modifier.weight(1f),
                size = PopTypeScale.Xl,
                weight = FontWeight.Bold,
                color = popInk,
                tracking = PopTracking.Wider,
                uppercase = true,
                maxLines = 2,
            )
            PopIconButton(
                icon = PopIconKind.Menu,
                onClick = onOpenLogs,
                description = strings.cdRecentLogs,
            )
        }

        // 仪表：固定 105dp 高（半圆 75 + 数字区 30），只做横向居中。
        // Web 版这里是 flex-1，但卡片高度=内容高度时它分不到额外空间；
        // 之前给 weight(1f) 就等于把「整块剩余高度」塞给仪表区，卡片被拉长近一倍。
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            SemiCircleGauge(
                value = ui.todayCount,
                max = ui.settings.dailyLimit,
                color = ui.gaugeColor,
                label = strings.smokes,
            )
        }

        // 连续达标贴纸
        PopTag(
            text = "${strings.consecutiveDaysWithinLimit}: ${ui.consecutiveDays} ${strings.days}",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = PopSpace.S4),
        )

        // 绘制阶段读不到 CompositionLocal，颜色必须在组合期先取出来
        val hairlineColor = popHairline
        // 上次记录 / 每日上限
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PopSpace.S4)
                .drawBehind {
                    drawRect(
                        color = hairlineColor,
                        size = Size(size.width, PopStroke.Thin.toPx()),
                    )
                }
                .padding(top = PopSpace.S4),
        ) {
            FooterCell(
                label = strings.lastRecord,
                value = ui.lastRecordClock ?: "--:--",
                modifier = Modifier.weight(1f),
            )
            FooterCell(
                label = strings.dailyLimit,
                value = ui.settings.dailyLimit.toString(),
                valueColor = ui.gaugeColor,
                modifier = Modifier.weight(1f),
            )
        }

        SmokeButton(
            label = strings.smokeButton,
            description = strings.cdRecordNow,
            onClick = onRecord,
        )
    }
}

@Composable
private fun FooterCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = popInk,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PopBody(
            text = label,
            size = PopTypeScale.Sm,
            weight = FontWeight.Bold,
            color = popInkMuted,
            uppercase = true,
            maxLines = 1,
        )
        PopDisplay(
            text = value,
            size = PopTypeScale.Xl,
            weight = FontWeight.Black,
            color = valueColor,
        )
    }
}

/**
 * BOOM 按钮 —— 整屏最大的一块颜色，两套皮肤的差别在这里最明显。
 *
 * 波普：7dp 硬阴影 + 按下位移 3.5dp，配合漫画字斜着压上来，读作"盖章"。
 * 玻璃：胶囊形、果冻按压、彩色半透明，光从顶部打下来在胶囊上留一道高光。
 * 文案也不再是斜体 "BOOM!"，而是正常字重的主标题 + 一行副标题。
 */
@Composable
private fun SmokeButton(
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    val themeAccent = accent
    val themeOnAccent = onAccent
    val current = LocalSkin.current

    if (current.glass) {
        val interaction = remember { MutableInteractionSource() }
        val pill = RoundedCornerShape(percent = 50)
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = PopSpace.S4),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = PopLayout.SmokeButtonMax)
                    .fillMaxWidth()
                    .jellyPress(interaction)
                    .skinSurface(
                        current = current,
                        shape = pill,
                        fill = themeAccent.copy(alpha = 0.9f),
                        lift = 18.dp,
                        tint = themeAccent,
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                    }
                    .padding(
                        start = PopSpace.S6,
                        end = PopSpace.S4,
                        top = PopSpace.S4,
                        bottom = PopSpace.S4,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    PopDisplay(
                        text = label,
                        size = PopTypeScale.Xxl,
                        weight = FontWeight.Bold,
                        color = themeOnAccent,
                        tracking = PopTracking.Tight,
                    )
                    PopBody(
                        text = "BOOM!",
                        size = PopTypeScale.Sm,
                        weight = FontWeight.Bold,
                        color = themeOnAccent.copy(alpha = 0.78f),
                        tracking = PopTracking.Wider,
                        uppercase = true,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(PopSpace.S12)
                        .background(Color.White.copy(alpha = 0.24f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PopIcon(PopIconKind.Plus, size = PopSpace.S6, strokeUnits = 2.5f, color = themeOnAccent)
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(top = PopSpace.S4),
        contentAlignment = Alignment.Center,
    ) {
        PopPressable(
            onClick = onClick,
            modifier = Modifier
                .widthIn(max = PopLayout.SmokeButtonMax)
                .fillMaxWidth()
                .semantics {
                    contentDescription = description
                    role = Role.Button
                },
            restShadow = PopShadow.Smoke,
            pressedShadow = PopShadow.SmokePressed,
            shape = RoundedCornerShape(PopRadius.Md),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeAccent, RoundedCornerShape(PopRadius.Md))
                    .popBorder(
                        PopStroke.Default,
                        popInk,
                        RoundedCornerShape(PopRadius.Md),
                    )
                    .padding(PopSpace.S5),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    PopDisplay(
                        text = "BOOM!",
                        size = PopTypeScale.Xxxl,
                        weight = FontWeight.Black,
                        color = themeOnAccent,
                        tracking = PopTracking.Tight,
                        italic = true,
                    )
                    PopDisplay(
                        text = label,
                        size = PopTypeScale.Xl,
                        weight = FontWeight.Black,
                        color = themeOnAccent,
                        tracking = PopTracking.Wider,
                        uppercase = true,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(PopSpace.S14)
                        .background(popSurface, CircleShape)
                        .border(PopStroke.Default, popInk, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PopIcon(PopIconKind.Plus, size = PopSpace.S7, strokeUnits = 3f)
                }
            }
        }
    }
}

@Composable
private fun DashboardBack(
    ui: DashboardUiState,
    strings: PopStrings,
    onBack: () -> Unit,
    onRequestEdit: (SmokeLog) -> Unit,
    onRequestDelete: (SmokeLog) -> Unit,
) {
    PopCard(
        modifier = Modifier.fillMaxSize(),
        background = popSurfaceDeep,
        padding = PaddingValues(PopSpace.S2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = PopSpace.S4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PopDisplay(
                text = strings.todayLogs,
                modifier = Modifier.weight(1f),
                size = PopTypeScale.Xxl,
                weight = FontWeight.Black,
                uppercase = true,
            )
            PopIconButton(
                icon = PopIconKind.Close,
                onClick = onBack,
                description = strings.cdBackToDashboard,
            )
        }

        if (ui.todayLogs.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PopDisplay(
                    text = strings.noRecords,
                    size = PopTypeScale.Xl,
                    color = popInkMuted,
                )
            }
        } else {
            LogList(
                logs = ui.todayLogs,
                language = ui.settings.language,
                accent = ui.accent,
                strings = strings,
                onEdit = onRequestEdit,
                onDelete = onRequestDelete,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 阈值提示条：接近上限时脉冲闪烁，达上限时上下跳动（对应 animate-pulse / animate-bounce）。 */
@Composable
private fun AlertBanner(
    alert: LimitAlert,
    strings: PopStrings,
    modifier: Modifier = Modifier,
) {
    val reached = alert == LimitAlert.Reached
    val background = if (reached) PopColor.Status.Error else PopColor.Status.WarningBorder
    val glass = LocalSkin.current.glass
    val foreground = if (reached) Color.White else popInk
    val text = if (reached) "🚫 ${strings.limitReachedMsg}" else "⚠️ ${strings.warningMsg}"

    val transition = rememberInfiniteTransition(label = "alert")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(PopMotion.Pulse / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alert-pulse",
    )
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PopMotion.Bounce / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alert-bounce",
    )

    Box(
        modifier = modifier
            // max-w-md = 28rem；widthIn 必须写在 fillMaxWidth 前面，顺序反了上限会被 min 顶掉
            .widthIn(max = PopLayout.AlertMax)
            .fillMaxWidth()
            .graphicsLayer {
                // 玻璃下不做上下弹跳：那是"贴纸被风吹"的语言。这里只做极轻的呼吸。
                alpha = if (reached) 1f else pulse
                translationY = if (reached && !glass) -PopSpace.S2.toPx() * bounce else 0f
            }
            .then(
                if (glass) {
                    Modifier
                        .skinSurface(
                            current = LocalSkin.current,
                            shape = RoundedCornerShape(percent = 50),
                            fill = background.copy(alpha = 0.88f),
                            lift = 14.dp,
                            tint = background,
                        )
                        .padding(horizontal = PopSpace.S5, vertical = PopSpace.S3)
                } else {
                    Modifier
                        .background(background)
                        .popBorder(PopStroke.Default, popInk)
                        .padding(PopSpace.S3)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        PopBody(
            text = text,
            size = PopTypeScale.Base,
            weight = FontWeight.Bold,
            color = foreground,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

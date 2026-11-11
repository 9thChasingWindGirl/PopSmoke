package com.popsmoke.tracker.feature.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.i18n.PopStrings
import com.popsmoke.tracker.ui.atoms.PopButton
import com.popsmoke.tracker.ui.atoms.PopButtonVariant
import com.popsmoke.tracker.ui.atoms.PopInput
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.popInkMuted
import com.popsmoke.tracker.design.popInkSecondary
import com.popsmoke.tracker.design.popSurface
import com.popsmoke.tracker.design.popSurfaceDeep
import androidx.compose.ui.graphics.Color

/**
 * 设置页的「云端账户」区块（Supabase）。
 *
 * 三态而不是一个开关：
 * 1. 没填地址 / key → 只给一个去配置的入口，不显示登录表单（否则输完密码才发现发不出去）
 * 2. 已配置未登录 → 邮箱 + 密码 + 登录/注册
 * 3. 已登录 → 邮箱 + 上传/恢复/登出
 *
 * 密码只存在内存里：登录成功就没人再需要它，落盘只会多一个泄露面。
 */
@Composable
fun AccountSection(
    cloud: CloudUiState,
    strings: PopStrings,
    onGoToApi: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onPushSettings: () -> Unit,
    onPullSettings: () -> Unit,
    onDismissNotice: () -> Unit,
    onSyncNow: () -> Unit = {},
    /**
     * 组内标题。设置页里这段内容的上面已经有分组标题（在组外面），
     * 这里再画一次会得到两行一样的字，所以由调用方决定要不要。
     */
    showTitle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    // 密码只活在内存里：转屏丢了就重输，比留在 Bundle 里安全。
    var password by remember { mutableStateOf("") }
    val busy = cloud.busy

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PopSpace.S2)) {
        if (showTitle) {
            PopDisplay(
                text = strings.cloudAccount,
                size = PopTypeScale.Base,
                weight = FontWeight.Black,
                maxLines = 1,
            )
        }

        when {
            !cloud.supabaseReady -> {
                PopBody(
                    text = strings.cloudNotConfigured,
                    size = PopTypeScale.Sm,
                    weight = FontWeight.Bold,
                    color = popInkSecondary,
                    maxLines = 3,
                )
                PopButton(
                    text = strings.apiManagement,
                    onClick = onGoToApi,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            cloud.user == null -> {
                PopBody(
                    text = strings.cloudAccountHint,
                    size = PopTypeScale.Sm,
                    weight = FontWeight.Bold,
                    color = popInkSecondary,
                    maxLines = 3,
                )
                PopInput(
                    value = email,
                    onValueChange = { email = it.trim() },
                    placeholder = strings.emailPlaceholder,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                PopInput(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = strings.passwordPlaceholder,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PopSpace.S2)) {
                    PopButton(
                        text = strings.loginAction,
                        onClick = { onSignIn(email, password) },
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                    )
                    PopButton(
                        text = strings.signUp,
                        onClick = { onSignUp(email, password) },
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        variant = PopButtonVariant.Secondary,
                    )
                }
            }

            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(popSurfaceDeep)
                        .popBorder(PopStroke.Thin, popInk)
                        .padding(PopSpace.S3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PopDisplay(
                        text = cloud.user.email.ifBlank { cloud.user.id },
                        modifier = Modifier.weight(1f),
                        size = PopTypeScale.Sm,
                        weight = FontWeight.Black,
                        maxLines = 1,
                    )
                    Box(
                        Modifier
                            .background(PopColor.Status.Success)
                            .popBorder(PopStroke.Thin, popInk)
                            .padding(horizontal = PopSpace.S2, vertical = 2.dp),
                    ) {
                        PopDisplay(
                            text = strings.signedInAs.uppercase(),
                            size = PopTypeScale.Xs,
                            weight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }

                // 记录同步与设置同步分成两块：一块是"我记了哪些烟"，一块是"App 怎么配的"。
                // 混在一起会让用户以为点一次就把所有东西都同步了，实际上两者走的是不同接口。
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .popBorder(PopStroke.Thin, popInk)
                        .padding(PopSpace.S2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        PopDisplay(
                            text = if (cloud.pendingSync > 0) {
                                strings.syncPendingCount.fill("n" to cloud.pendingSync)
                            } else {
                                strings.syncQueue
                            },
                            size = PopTypeScale.Sm,
                            weight = FontWeight.Black,
                            color = if (cloud.hasFailedOp) PopColor.Status.Error else popInk,
                            maxLines = 1,
                        )
                        PopBody(
                            text = when {
                                cloud.hasFailedOp -> strings.syncHasFailedHint
                                cloud.pendingSync > 0 -> strings.syncNow
                                else -> strings.syncReadyHint
                            },
                            size = PopTypeScale.Xs,
                            weight = FontWeight.Bold,
                            color = popInkMuted,
                            maxLines = 2,
                        )
                    }
                    Box(Modifier.padding(start = PopSpace.S2))
                    PopButton(
                        text = strings.syncNow,
                        onClick = onSyncNow,
                        enabled = !busy,
                        fillWidth = false,
                        variant = PopButtonVariant.Secondary,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(PopSpace.S2)) {
                    PopButton(
                        text = strings.cloudSettingsUp,
                        onClick = onPushSettings,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                    )
                    PopButton(
                        text = strings.cloudSettingsDown,
                        onClick = onPullSettings,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        variant = PopButtonVariant.Secondary,
                    )
                }
                PopButton(
                    text = strings.signOut,
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    variant = PopButtonVariant.Danger,
                )
            }
        }

        if (busy) {
            PopBody(
                text = strings.cloudSyncing,
                size = PopTypeScale.Xs,
                weight = FontWeight.Bold,
                color = popInkMuted,
                maxLines = 1,
            )
        }

        // 提示条只在账户/设置相关的动作后出现；飞书备份的提示留在接口页，
        // 免得"备份了 47 条"这句话飘在设置页里让人找不到来源。
        val notice = cloud.notice
        if (notice != null && notice.kind != CloudNoticeKind.BackupDone && notice.kind != CloudNoticeKind.Restored) {
            NoticeBand(text = notice.message(strings), isError = notice.isError, onClose = onDismissNotice)
        }
    }
}

/** 轻量提示条：不抢焦点、不遮挡，点一下消失。 */
@Composable
internal fun NoticeBand(text: String, isError: Boolean, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(if (isError) PopColor.Neutral.Red200 else PopColor.Status.SuccessLight)
            .popBorder(PopStroke.Thin, popInk)
            .padding(PopSpace.S2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PopBody(
                text = text,
                modifier = Modifier.weight(1f),
                size = PopTypeScale.Xs,
                weight = FontWeight.Bold,
                color = popInk,
                maxLines = 4,
            )
            PopDisplay(
                text = "×",
                modifier = Modifier
                    .padding(start = PopSpace.S2)
                    .clickable(onClick = onClose),
                size = PopTypeScale.Base,
                weight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

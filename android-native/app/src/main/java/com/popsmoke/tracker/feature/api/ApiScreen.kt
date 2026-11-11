package com.popsmoke.tracker.feature.api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.data.ApiConfig
import com.popsmoke.tracker.data.ApiConfigStore
import com.popsmoke.tracker.data.maskSecret
import com.popsmoke.tracker.data.maskUrl
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.feature.cloud.CloudNoticeKind
import com.popsmoke.tracker.feature.cloud.CloudUiState
import com.popsmoke.tracker.feature.cloud.NoticeBand
import com.popsmoke.tracker.feature.cloud.isError
import com.popsmoke.tracker.feature.cloud.message
import com.popsmoke.tracker.i18n.PopStrings
import com.popsmoke.tracker.ui.atoms.PopButton
import com.popsmoke.tracker.ui.atoms.PopButtonVariant
import com.popsmoke.tracker.ui.atoms.PopCard
import com.popsmoke.tracker.ui.atoms.PopInput
import com.popsmoke.tracker.ui.atoms.PopToast
import com.popsmoke.tracker.ui.atoms.PopToastKind
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.popBorder
import java.io.File

/**
 * API 配置页。
 *
 * 界面与 Web 版同构：配置状态 → 编辑表单 → 保存/清空。
 * 但**同步动作本身不在这一批**：Supabase 的鉴权、飞书多维表的读写、差异合并
 * 都需要网络层与冲突解决，那是独立一批的工作量。在没接之前把按钮做成"能点但没用"
 * 比不给按钮更糟，所以这一页只负责把配置安全地存下来，并在分析页明确提示"未接入"。
 *
 * 配置用 Keystore 的 AES-GCM 加密后落盘（见 SecureStore），不需要用户再记一个密码。
 */
@Composable
fun ApiScreen(
    strings: PopStrings,
    modifier: Modifier = Modifier,
    cloud: CloudUiState = CloudUiState(),
    onBackupToFeishu: () -> Unit = {},
    onRestoreFromFeishu: () -> Unit = {},
    onDismissNotice: () -> Unit = {},
    onConfigSaved: () -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember { ApiConfigStore(File(context.filesDir, "pop-data")) }
    var config by remember { mutableStateOf(store.load()) }
    var draft by remember(config) { mutableStateOf(config) }
    var toast by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = PopSpace.S4,
            end = PopSpace.S4,
            top = PopSpace.S4,
            bottom = PopSpace.S8,
        ),
        verticalArrangement = Arrangement.spacedBy(PopSpace.S6),
    ) {
        item {
            PopCard(modifier = Modifier.fillMaxWidth(), title = strings.apiManagement) {
                Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S3)) {
                    PopBody(
                        text = strings.syncDescription,
                        size = PopTypeScale.Sm,
                        weight = FontWeight.Bold,
                        color = PopColor.Neutral.Gray600,
                        maxLines = 4,
                    )

                    StatusRow(strings.feishuApi, config.hasFeishu)
                    StatusRow("Supabase", config.hasSupabase)

                    if (config.isEmpty) {
                        PopBody(
                            text = strings.noApiConfig,
                            size = PopTypeScale.Xs,
                            weight = FontWeight.Bold,
                            color = PopColor.Status.Danger,
                            maxLines = 3,
                        )
                    }
                }
            }
        }

        item {
            PopCard(modifier = Modifier.fillMaxWidth(), title = strings.apiUrl) {
                Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S2)) {
                    FieldLabel(strings.feishuApi + " · " + strings.apiUrl)
                    PopInput(
                        value = draft.feishuUrl,
                        onValueChange = { draft = draft.copy(feishuUrl = it.trim()) },
                        placeholder = "https://open.feishu.cn/…",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )

                    Box(Modifier.height(PopSpace.S2))
                    FieldLabel(strings.feishuApi + " · " + strings.accessKey)
                    PopInput(
                        value = draft.feishuKey,
                        onValueChange = { draft = draft.copy(feishuKey = it.trim()) },
                        placeholder = strings.accessKey,
                    )

                    Box(Modifier.height(PopSpace.S2))
                    FieldLabel("Supabase · " + strings.apiUrl)
                    PopInput(
                        value = draft.supabaseUrl,
                        onValueChange = { draft = draft.copy(supabaseUrl = it.trim()) },
                        placeholder = "https://xxxx.supabase.co",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )

                    Box(Modifier.height(PopSpace.S2))
                    FieldLabel(strings.anonKey)
                    PopInput(
                        value = draft.supabaseAnonKey,
                        onValueChange = { draft = draft.copy(supabaseAnonKey = it.trim()) },
                        placeholder = strings.anonKey,
                    )

                    Box(Modifier.height(PopSpace.S3))
                    PopButton(
                        text = strings.saveApiSettings,
                        onClick = {
                            store.save(draft)
                            config = store.load()
                            onConfigSaved()
                            toast = true to strings.notificationSuccess
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PopButton(
                        text = strings.reset,
                        onClick = {
                            val empty = ApiConfig()
                            store.save(empty)
                            config = empty
                            draft = empty
                            onConfigSaved()
                            toast = true to strings.reset
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = PopButtonVariant.Secondary,
                    )

                    val current = toast
                    if (current != null) {
                        PopToast(
                            visible = true,
                            title = if (current.first) strings.notificationSuccess else strings.notificationError,
                            message = current.second,
                            kind = if (current.first) PopToastKind.Success else PopToastKind.Error,
                            onClose = { toast = null },
                        )
                    }
                }
            }
        }

        item {
            PopCard(modifier = Modifier.fillMaxWidth(), title = strings.feishuDataSync) {
                Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S2)) {
                    PopBody(
                        text = strings.feishuSyncHint,
                        size = PopTypeScale.Xs,
                        weight = FontWeight.Bold,
                        color = PopColor.Neutral.Gray600,
                        maxLines = 3,
                    )
                    PopButton(
                        text = strings.feishuBackupNow,
                        onClick = onBackupToFeishu,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = config.hasFeishu && !cloud.busy,
                    )
                    PopButton(
                        text = strings.feishuRestoreNow,
                        onClick = onRestoreFromFeishu,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = config.hasFeishu && !cloud.busy,
                        variant = PopButtonVariant.Secondary,
                    )
                    if (cloud.busy) {
                        PopBody(
                            text = strings.cloudSyncing,
                            size = PopTypeScale.Xs,
                            weight = FontWeight.Bold,
                            color = PopColor.Neutral.Gray500,
                            maxLines = 1,
                        )
                    }
                    val notice = cloud.notice
                    if (notice != null && (notice.kind == CloudNoticeKind.BackupDone || notice.kind == CloudNoticeKind.Restored)) {
                        NoticeBand(
                            text = notice.message(strings),
                            isError = notice.isError,
                            onClose = onDismissNotice,
                        )
                    }
                }
            }
        }

        item {
            PopCard(modifier = Modifier.fillMaxWidth(), title = strings.viewSavedApiSettings) {
                Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S2)) {
                    KeyValue(strings.apiUrl, maskUrl(config.feishuUrl))
                    KeyValue(strings.apiUrl, maskUrl(config.supabaseUrl))
                    KeyValue(strings.anonKey, maskSecret(config.supabaseAnonKey))
                    KeyValue(strings.accessKey, maskSecret(config.feishuKey))
                    Box(Modifier.height(PopSpace.S2))
                    PopBody(
                        text = strings.apiConfigRequired,
                        size = PopTypeScale.Xs,
                        weight = FontWeight.Bold,
                        color = PopColor.Neutral.Gray500,
                        maxLines = 3,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, configured: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PopColor.Neutral.Gray100)
            .popBorder(PopStroke.Thin, PopColor.Core.Black)
            .padding(PopSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PopDisplay(
            text = label,
            modifier = Modifier.weight(1f),
            size = PopTypeScale.Base,
            weight = FontWeight.Black,
            maxLines = 1,
        )
        Box(
            Modifier
                .background(if (configured) PopColor.Status.Success else PopColor.Status.Danger)
                .popBorder(PopStroke.Thin, PopColor.Core.Black)
                .padding(horizontal = PopSpace.S2, vertical = 2.dp),
        ) {
            PopDisplay(
                text = if (configured) "READY" else "EMPTY",
                size = PopTypeScale.Xs,
                weight = FontWeight.Black,
                color = PopColor.Core.White,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PopBody(
            text = key,
            modifier = Modifier.weight(1f),
            size = PopTypeScale.Xs,
            weight = FontWeight.Bold,
            color = PopColor.Neutral.Gray600,
            maxLines = 1,
        )
        PopDisplay(
            text = value,
            size = PopTypeScale.Sm,
            weight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    PopDisplay(
        text = text,
        modifier = Modifier.padding(bottom = PopSpace.S1),
        size = PopTypeScale.Sm,
        weight = FontWeight.Black,
        maxLines = 2,
    )
}

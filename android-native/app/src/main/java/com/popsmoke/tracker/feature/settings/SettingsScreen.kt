package com.popsmoke.tracker.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.BuildConfig
import com.popsmoke.tracker.data.ApiConfigStore
import com.popsmoke.tracker.data.AppSettings
import com.popsmoke.tracker.data.LocalStore
import com.popsmoke.tracker.data.OpLog
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.data.StoreCodec
import com.popsmoke.tracker.data.StoreState
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopLayout
import com.popsmoke.tracker.design.PopPresets
import com.popsmoke.tracker.design.PopShadow
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.PopStroke
import com.popsmoke.tracker.design.PopThemeStyle
import com.popsmoke.tracker.design.PopTypeScale
import com.popsmoke.tracker.design.accent
import com.popsmoke.tracker.design.onAccent
import com.popsmoke.tracker.design.toHexString
import com.popsmoke.tracker.feature.cloud.AccountSection
import com.popsmoke.tracker.feature.cloud.CloudUiState
import com.popsmoke.tracker.feature.cloud.message
import com.popsmoke.tracker.i18n.PopStrings
import com.popsmoke.tracker.ui.atoms.PopButton
import com.popsmoke.tracker.ui.atoms.PopButtonVariant
import com.popsmoke.tracker.ui.atoms.PopCard
import com.popsmoke.tracker.ui.atoms.PopColorPicker
import com.popsmoke.tracker.ui.atoms.PopDialog
import com.popsmoke.tracker.ui.atoms.PopSelect
import com.popsmoke.tracker.ui.atoms.PopSelectOption
import com.popsmoke.tracker.ui.atoms.PopSlider
import com.popsmoke.tracker.ui.atoms.PopToast
import com.popsmoke.tracker.ui.atoms.PopToastKind
import com.popsmoke.tracker.ui.foundation.PopBody
import com.popsmoke.tracker.ui.foundation.PopDisplay
import com.popsmoke.tracker.ui.foundation.buttonShape
import com.popsmoke.tracker.ui.foundation.jellyPress
import com.popsmoke.tracker.ui.foundation.popBorder
import com.popsmoke.tracker.ui.foundation.skinSurface
import java.io.File
import com.popsmoke.tracker.design.popInk
import com.popsmoke.tracker.design.popInkMuted
import com.popsmoke.tracker.design.popInkSecondary
import com.popsmoke.tracker.design.popSurface
import com.popsmoke.tracker.design.popSurfaceDeep
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

/**
 * 设置页：阈值、语言、主题、数据管理、关于。
 *
 * 与 Web 版的差异只有一处必要的：主题选色把"预设色"和"自定义色号"放在同一屏，
 * 不再套一层下拉。手机上"点开下拉 → 再滚到色块"要多两次点击，
 * 而这一页本来就是低频深度操作，平铺反而更快。
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    strings: PopStrings,
    language: String,
    logs: List<SmokeLog>,
    opLogs: List<OpLog>,
    onSetLanguage: (String) -> Unit,
    onSetThemeColor: (String) -> Unit,
    /** 主题风格（液态玻璃 / 波普漫画）。与主题色正交：同一套配色在两套风格下都成立。 */
    onSetThemeStyle: (String) -> Unit = {},
    onSetDailyLimit: (Int) -> Unit,
    onSetWarningLimit: (Int) -> Unit,
    onClearLogs: () -> Unit,
    onImportBackup: (StoreState, Boolean) -> Unit,
    onGoToApi: () -> Unit,
    cloud: CloudUiState = CloudUiState(),
    onSignIn: (String, String) -> Unit = { _, _ -> },
    onSignUp: (String, String) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    onPushSettings: () -> Unit = {},
    onPullSettings: () -> Unit = {},
    onResolveConflict: (Boolean) -> Unit = {},
    onDismissConflict: () -> Unit = {},
    onDismissNotice: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<StoreState?>(null) }
    var toast by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    // 关于栏的外链：先弹确认再跳。Web 版也是这么做的（PopExternalLinkWarning），
    // 一个"关于"页里的按钮直接把用户甩到浏览器，很容易被当成 App 自己的行为。
    var pendingLink by remember { mutableStateOf<String?>(null) }

    val dataDir = remember { File(context.filesDir, "pop-data") }
    // 导入/清空之后文件大小会变，所以把数量也作为 key：不然"存储占用"会一直显示旧值。
    val storageBytes = remember(logs.size, opLogs.size) { LocalStore(dataDir).sizeBytes() }
    val apiConfig = remember { ApiConfigStore(dataDir).load() }

    // 导入走系统文件选择器：应用私有目录里的备份用户根本找不到，
    // 而 SAF 让"从下载目录挑一份备份"这件事不需要任何存储权限。
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        val parsed = text?.let { StoreCodec.decode(it) }
        if (parsed == null) {
            toast = false to strings.backupInvalid
        } else {
            pendingImport = parsed
        }
    }

    val presets = PopPresets.All.map { preset ->
        PopSelectOption(
            value = preset.color.toHexString(),
            label = preset.name(language),
            swatch = preset.color,
        )
    }
    val languages = listOf(
        PopSelectOption("en", "English"),
        PopSelectOption("zh", "中文"),
        PopSelectOption("ja", "日本語"),
        PopSelectOption("ko", "한국어"),
    )

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
            PopCard(modifier = Modifier.fillMaxWidth(), title = strings.settings) {
                Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S6)) {
                    // 账户 / 云端
                    Column {
                        AccountSection(
                            cloud = cloud,
                            strings = strings,
                            onGoToApi = onGoToApi,
                            onSignIn = onSignIn,
                            onSignUp = onSignUp,
                            onSignOut = onSignOut,
                            onPushSettings = onPushSettings,
                            onPullSettings = onPullSettings,
                            onDismissNotice = onDismissNotice,
                            onSyncNow = onSyncNow,
                        )
                        // "当前使用本地模式"只在没登录时成立：已登录还说自己是本地模式，
                        // 用户会以为刚才的登录没生效。
                        if (!cloud.signedIn) {
                            Box(Modifier.height(PopSpace.S3))
                            PopBody(
                                text = strings.localModeHint,
                                size = PopTypeScale.Xs,
                                weight = FontWeight.Bold,
                                color = popInkMuted,
                                maxLines = 2,
                            )
                        }
                        Box(Modifier.height(PopSpace.S2))
                        PopButton(
                            text = if (apiConfig.isEmpty) strings.api else strings.apiManagement,
                            onClick = onGoToApi,
                            modifier = Modifier.fillMaxWidth(),
                            variant = PopButtonVariant.Secondary,
                        )
                    }

                    // 阈值
                    Column {
                        SectionTitle(strings.thresholdSettings)
                        LimitRow(
                            label = strings.dailyLimit,
                            value = settings.dailyLimit,
                            badgeBg = popInk,
                            // 反色徽标上的数字必须是纯白：popSurface 在玻璃下是半透明白，
                            // 压在深色徽标上会发灰
                            badgeFg = Color.White,
                            rotate = -2f,
                        )
                        PopSlider(
                            value = settings.dailyLimit,
                            onValueChange = onSetDailyLimit,
                            min = 1,
                            max = 50,
                        )
                        Box(Modifier.height(PopSpace.S4))
                        LimitRow(
                            label = strings.warningLimit,
                            value = settings.warningLimit,
                            badgeBg = PopColor.Neutral.Yellow300,
                            badgeFg = popInk,
                            rotate = 1f,
                        )
                        PopSlider(
                            value = settings.warningLimit,
                            onValueChange = onSetWarningLimit,
                            min = 1,
                            max = settings.dailyLimit,
                        )
                    }

                    // 语言
                    Column {
                        SectionTitle("${strings.language}")
                        PopSelect(
                            label = strings.language,
                            value = languages.first { it.value == settings.language }.label,
                            options = languages,
                            onSelect = onSetLanguage,
                        )
                    }

                    // 主题风格
                    Column {
                        StyleSection(
                            value = settings.themeStyle,
                            strings = strings,
                            onChange = onSetThemeStyle,
                        )
                    }

                    // 主题
                    Column {
                        SectionTitle(strings.themeColor)
                        PopSelect(
                            label = strings.theme,
                            value = settings.themeColor,
                            options = presets,
                            onSelect = onSetThemeColor,
                            leading = {
                                Box(
                                    Modifier
                                        .padding(end = PopSpace.S2)
                                        .width(PopSpace.S6)
                                        .height(PopSpace.S6)
                                        .background(accent)
                                        .popBorder(PopStroke.Thin, popInk),
                                )
                            },
                        )
                        Box(Modifier.height(PopSpace.S2))
                        PopColorPicker(
                            value = settings.themeColor,
                            onChange = onSetThemeColor,
                            presets = PopPresets.All.map { it.color.toHexString() },
                        )
                    }

                    // 数据
                    Column {
                        SectionTitle(strings.dataManagement)
                        InfoBlock(strings.storageMethod, "JSON / app-private")
                        Box(Modifier.height(PopSpace.S2))
                        InfoBlock(
                            strings.storageUsage,
                            "${storageBytes / 1024.0f}".take(5) + " KB",
                        )
                        Box(Modifier.height(PopSpace.S3))

                        // 备份 / 恢复：这是"换手机"那一天唯一能救命的东西，
                        // 所以给足两条路径 —— 导出到文件、从文件选回来（SAF，不要存储权限）。
                        PopDisplay(
                            text = strings.backupSection,
                            modifier = Modifier.padding(bottom = PopSpace.S2),
                            size = PopTypeScale.Base,
                            weight = FontWeight.Black,
                            maxLines = 1,
                        )
                        PopButton(
                            text = strings.backupExport,
                            onClick = {
                                val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US)
                                    .format(java.util.Date())
                                val file = File(context.getExternalFilesDir(null), "popsmoke-$stamp.json")
                                val json = StoreCodec.encode(
                                    StoreState(logs = logs, settings = settings, opLogs = opLogs),
                                    exportedAt = System.currentTimeMillis(),
                                )
                                toast = runCatching { file.writeText(json, Charsets.UTF_8) }
                                    .map { true to "${strings.backupExported} · ${file.absolutePath}" }
                                    .getOrElse { false to (it.message ?: strings.exportFailed) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            variant = PopButtonVariant.Secondary,
                        )
                        Box(Modifier.height(PopSpace.S2))
                        PopButton(
                            text = strings.backupImport,
                            onClick = { importPicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            variant = PopButtonVariant.Secondary,
                        )
                        Box(Modifier.height(PopSpace.S2))
                        PopBody(
                            text = strings.backupNote,
                            size = PopTypeScale.Xs,
                            weight = FontWeight.Bold,
                            color = popInkMuted,
                            maxLines = 3,
                        )

                        val currentToast = toast
                        if (currentToast != null) {
                            Box(Modifier.height(PopSpace.S3))
                            PopToast(
                                visible = true,
                                title = if (currentToast.first) strings.notificationSuccess else strings.notificationError,
                                message = currentToast.second,
                                kind = if (currentToast.first) PopToastKind.Success else PopToastKind.Error,
                                onClose = { toast = null },
                            )
                        }

                        Box(Modifier.height(PopSpace.S4))
                        PopButton(
                            text = strings.clearOldData,
                            onClick = { showClearDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            variant = PopButtonVariant.Danger,
                        )
                    }
                }
            }
        }

        item {
            PopCard(modifier = Modifier.fillMaxWidth(), title = strings.about) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(PopSpace.S2),
                ) {
                    LinkRow(strings.blog) { pendingLink = BLOG_URL }
                    LinkRow(strings.github) { pendingLink = GITHUB_URL }
                    PopDisplay(
                        text = "ver.${BuildConfig.VERSION_NAME}",
                        size = PopTypeScale.Xs,
                        weight = FontWeight.Black,
                        color = popInkMuted,
                        maxLines = 1,
                    )
                    PopBody(
                        text = strings.fontAttribution,
                        size = PopTypeScale.Xs,
                        weight = FontWeight.Bold,
                        color = popInkMuted,
                        maxLines = 2,
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        PopDialog(
            visible = true,
            title = strings.clearData,
            message = strings.clearDataConfirm,
            titleColor = PopColor.Status.Error,
            onDismiss = { showClearDialog = false },
            actions = {
                PopButton(
                    text = strings.cancel,
                    onClick = { showClearDialog = false },
                    modifier = Modifier.weight(1f),
                    variant = PopButtonVariant.Secondary,
                )
                PopButton(
                    text = strings.confirm,
                    onClick = {
                        onClearLogs()
                        showClearDialog = false
                    },
                    modifier = Modifier.weight(1f),
                    variant = PopButtonVariant.Danger,
                )
            },
        )
    }

    // 登录后两边设置不一致：必须让用户选，不能替他决定。
    cloud.conflict?.let {
        PopDialog(
            visible = true,
            title = strings.cloudConflictTitle,
            message = strings.cloudConflictMessage,
            onDismiss = onDismissConflict,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S1)) {
                    PopBody(
                        text = "${strings.dailyLimit}: ${it.dailyLimit} · ${strings.warningLimit}: ${it.warningLimit}",
                        size = PopTypeScale.Xs,
                        weight = FontWeight.Bold,
                        color = popInkSecondary,
                        maxLines = 2,
                    )
                    PopBody(
                        text = "${strings.themeColor}: ${it.themeColor} · ${strings.language}: ${it.language.uppercase()}",
                        size = PopTypeScale.Xs,
                        weight = FontWeight.Bold,
                        color = popInkSecondary,
                        maxLines = 2,
                    )
                }
            },
            actions = {
                PopButton(
                    text = strings.cloudKeepLocal,
                    onClick = { onResolveConflict(false) },
                    modifier = Modifier.weight(1f),
                    variant = PopButtonVariant.Secondary,
                )
                PopButton(
                    text = strings.cloudUseCloud,
                    onClick = { onResolveConflict(true) },
                    modifier = Modifier.weight(1f),
                )
            },
        )
    }

    // 导入方式：合并 / 覆盖做成两个整宽按钮而不是三个并排 —— 336dp 的弹窗里
    // 三个按钮各自只剩 90dp，"REPLACE" 这种词会挤成省略号。
    pendingImport?.let { incoming ->
        PopDialog(
            visible = true,
            title = strings.importModeTitle,
            message = strings.importModeMessage,
            onDismiss = { pendingImport = null },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S3)) {
                    PopButton(
                        text = strings.importMerge,
                        onClick = {
                            onImportBackup(incoming, false)
                            toast = true to "${strings.backupImported} ${incoming.logs.size}"
                            pendingImport = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PopButton(
                        text = strings.importReplace,
                        onClick = {
                            onImportBackup(incoming, true)
                            toast = true to "${strings.backupImported} ${incoming.logs.size}"
                            pendingImport = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = PopButtonVariant.Danger,
                    )
                }
            },
            actions = {
                PopButton(
                    text = strings.cancel,
                    onClick = { pendingImport = null },
                    modifier = Modifier.weight(1f),
                    variant = PopButtonVariant.Secondary,
                )
            },
        )
    }

    // 外链确认：先说清楚"要离开这个 App"，再交给系统浏览器。
    // 用 ACTION_VIEW 而不是内置 WebView —— 登录态、cookie、证书都归浏览器管，App 不背这个包袱。
    pendingLink?.let { url ->
        PopDialog(
            visible = true,
            title = strings.externalLinkWarningTitle,
            message = "${strings.externalLinkWarningMessage}\n$url",
            onDismiss = { pendingLink = null },
            actions = {
                PopButton(
                    text = strings.externalLinkCancel,
                    onClick = { pendingLink = null },
                    modifier = Modifier.weight(1f),
                    variant = PopButtonVariant.Secondary,
                )
                PopButton(
                    text = strings.externalLinkContinue,
                    onClick = {
                        pendingLink = null
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }.onFailure {
                            // 设备上没有浏览器（少见的定制 ROM）不该静默失败
                            toast = false to (it.message ?: strings.exportFailed)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            },
        )
    }
}

/** 与 Web 版 PopSettings 的"关于"栏保持同一组地址。 */
private const val BLOG_URL = "https://blog.whysoserious.dpdns.org/"
private const val GITHUB_URL = "https://github.com/9thChasingWindGirl"

@Composable
private fun SectionTitle(text: String) {
    PopDisplay(
        text = text,
        modifier = Modifier.padding(bottom = PopSpace.S2),
        size = PopTypeScale.Base,
        weight = FontWeight.Black,
        maxLines = 2,
    )
}

@Composable
private fun LimitRow(
    label: String,
    value: Int,
    badgeBg: androidx.compose.ui.graphics.Color,
    badgeFg: androidx.compose.ui.graphics.Color,
    rotate: Float,
) {
    val current = com.popsmoke.tracker.design.LocalSkin.current
    val chip = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = PopSpace.S2),
        verticalAlignment = Alignment.Bottom,
    ) {
        PopDisplay(
            text = label,
            modifier = Modifier.weight(1f),
            size = PopTypeScale.Lg,
            weight = FontWeight.Black,
            maxLines = 1,
        )
        Box(
            Modifier
                // 玻璃下徽标是一枚小胶囊，不旋转：歪斜会和高光方向打架
                .graphicsRotation(if (current.glass) 0f else rotate)
                .then(
                    if (current.glass) {
                        // 0.9 的实心色块在这个界面里是唯一的"不透明色斑"，
                        // 一眼就把玻璃语言打断。改成彩色玻璃：0.55 让底色透上来，
                        // 边缘交给 skinSurface 的 rim/bevel 去建立厚度。
                        Modifier
                            .skinSurface(
                                current = current,
                                shape = chip,
                                fill = badgeBg.copy(alpha = 0.55f),
                                lift = 6.dp,
                                tint = badgeBg,
                            )
                            .padding(horizontal = PopSpace.S3, vertical = PopSpace.S1)
                    } else {
                        Modifier
                            .background(badgeBg)
                            .popBorder(PopStroke.Thin, popInk)
                            .padding(horizontal = PopSpace.S2, vertical = PopSpace.S1)
                    },
                ),
        ) {
            PopDisplay(
                text = value.toString(),
                size = PopTypeScale.Xl,
                weight = FontWeight.Black,
                color = badgeFg,
                maxLines = 1,
            )
        }
    }
}

private fun Modifier.graphicsRotation(degrees: Float): Modifier =
    this.graphicsLayer {
        rotationZ = degrees
        transformOrigin = TransformOrigin(0.5f, 0.5f)
    }

@Composable
private fun InfoBlock(label: String, value: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(popSurfaceDeep)
            .popBorder(PopStroke.Thin, popInk)
            .padding(PopSpace.S3),
    ) {
        PopBody(
            text = label,
            size = PopTypeScale.Sm,
            weight = FontWeight.Bold,
            color = popInkSecondary,
            maxLines = 1,
        )
        PopDisplay(
            text = value,
            size = PopTypeScale.Base,
            weight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(popSurface)
            .popBorder(PopStroke.Thin, popInk)
            .clickable(onClick = onClick)
            .padding(PopSpace.S3),
        contentAlignment = Alignment.Center,
    ) {
        PopDisplay(
            text = text,
            size = PopTypeScale.Base,
            weight = FontWeight.Black,
            uppercase = true,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------- 主题风格切换

/**
 * 主题风格：两张可点的预览片，不是下拉。
 *
 * 只有两个选项，而"液态玻璃长什么样"用文字说不清 —— 缩略图把两种视觉语言并排摊开，
 * 用户看到选项的瞬间就已经看到了结果，不需要先切过去、再切回来比较。
 *
 * 缩略图固定画**它自己代表的**风格，不跟随当前主题：否则在玻璃主题下，
 * 波普那张也会被渲染成玻璃，两个选项长得一样，这个控件就失去意义了。
 */
@Composable
private fun StyleSection(
    value: String,
    strings: PopStrings,
    onChange: (String) -> Unit,
) {
    val selected = PopThemeStyle.fromId(value)
    SectionTitle(strings.themeStyle)
    Column(verticalArrangement = Arrangement.spacedBy(PopSpace.S2)) {
        StyleOption(
            style = PopThemeStyle.Liquid,
            title = strings.styleLiquid,
            desc = strings.styleLiquidDesc,
            selected = selected == PopThemeStyle.Liquid,
            onClick = { onChange(PopThemeStyle.Liquid.id) },
        )
        StyleOption(
            style = PopThemeStyle.Pop,
            title = strings.stylePop,
            desc = strings.stylePopDesc,
            selected = selected == PopThemeStyle.Pop,
            onClick = { onChange(PopThemeStyle.Pop.id) },
        )
    }
}

@Composable
private fun StyleOption(
    style: PopThemeStyle,
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val current = LocalSkin.current
    val accentColor = accent
    // 选中态沿用全站约定：波普是反色块，玻璃是主题色淡染 + 一圈细光环。
    // 玻璃下不套反色块 —— 磨砂面上挖一块纯黑会像图层破了洞。
    val shape: Shape = if (current.glass) current.buttonShape() else RectangleShape
    val fill = when {
        !selected -> current.palette.surface
        current.glass -> accentColor.copy(alpha = 0.16f)
        else -> current.palette.inverseSurface
    }
    val inverted = selected && !current.glass
    val titleColor = if (inverted) current.palette.inverseInk else current.palette.ink
    val descColor = if (inverted) {
        current.palette.inverseInk.copy(alpha = 0.72f)
    } else {
        current.palette.inkMuted
    }

    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .then(if (current.glass) Modifier.jellyPress(interaction) else Modifier)
            .skinSurface(
                current = current,
                shape = shape,
                fill = fill,
                lift = if (current.glass) 10.dp else PopShadow.Pop,
            )
            .then(
                if (current.glass && selected) {
                    Modifier.border(1.5.dp, accentColor.copy(alpha = 0.55f), shape)
                } else {
                    Modifier
                },
            )
            .padding(PopSpace.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StylePreview(
            style = style,
            accentColor = accentColor,
            modifier = Modifier
                .width(84.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(if (current.glass) 12.dp else 0.dp)),
        )
        Box(Modifier.width(PopSpace.S3))
        Column(Modifier.weight(1f)) {
            PopDisplay(
                text = title,
                size = PopTypeScale.Base,
                weight = FontWeight.Black,
                color = titleColor,
                maxLines = 1,
            )
            PopBody(
                text = desc,
                size = PopTypeScale.Xs,
                weight = FontWeight.Bold,
                color = descColor,
                maxLines = 2,
            )
        }
    }
}

/**
 * 84×52 的风格缩略图：背景 + 一张卡片 + 一枚按钮。
 *
 * 只画三样东西是有意的 —— 再多就是一张假截图，在 84dp 宽里全是噪点；
 * 这三样恰好是两套语言分歧最大的地方：背景有没有光、卡片是圆角还是黑框、按钮是彩色块还是彩色胶囊。
 */
@Composable
private fun StylePreview(
    style: PopThemeStyle,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val glass = style == PopThemeStyle.Liquid
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (glass) {
            // 极光必须画：没有光斑，"透"这件事在静态缩略图里根本看不出来
            drawRect(Brush.verticalGradient(listOf(Color(0xFFEFEDF8), Color(0xFFD2CFE4))))
            drawBlob(Offset(w * 0.14f, h * 1.02f), h * 1.25f, accentColor.copy(alpha = 0.7f))
            drawBlob(Offset(w * 0.94f, h * 0.16f), h * 1.0f, Color(0xFF8FB4FF).copy(alpha = 0.6f))
        } else {
            drawRect(PopColor.Core.Paper)
            // 点阵纸纹：波普的"纸感"一半来自这里，缩略图省略掉会显得比实际更干净
            val step = 5.dp.toPx()
            val dot = 0.7.dp.toPx()
            var y = step / 2f
            while (y < h) {
                var x = step / 2f
                while (x < w) {
                    drawCircle(Color(0x1A000000), dot, Offset(x, y))
                    x += step
                }
                y += step
            }
        }

        val corner = 7.dp.toPx()
        // 卡片
        drawPreviewBar(
            left = w * 0.13f,
            top = h * 0.22f,
            width = w * 0.60f,
            height = h * 0.22f,
            glass = glass,
            corner = corner,
            fill = if (glass) Color.White.copy(alpha = 0.62f) else Color.White,
        )
        // 按钮
        drawPreviewBar(
            left = w * 0.13f,
            top = h * 0.58f,
            width = w * 0.44f,
            height = h * 0.20f,
            glass = glass,
            corner = 999f,
            fill = if (glass) accentColor.copy(alpha = 0.9f) else accentColor,
        )
    }
}

private fun DrawScope.drawBlob(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * 缩略图里的一根"条"。两套语言在这里分开：
 * 玻璃是柔和落影 + 半透明白填充 + 纯白细边；波普是硬偏移黑块 + 实心填充 + 黑描边。
 */
private fun DrawScope.drawPreviewBar(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    glass: Boolean,
    corner: Float,
    fill: Color,
) {
    if (glass) {
        val radius = CornerRadius(minOf(corner, height / 2f))
        drawRoundRect(
            color = Color(0xFF16161A).copy(alpha = 0.10f),
            topLeft = Offset(left, top + height * 0.22f),
            size = Size(width, height),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = fill,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = radius,
            style = Stroke(1.dp.toPx()),
        )
    } else {
        val ink = Color(0xFF111111)
        val offset = 2.dp.toPx()
        drawRect(ink, topLeft = Offset(left + offset, top + offset), size = Size(width, height))
        drawRect(fill, topLeft = Offset(left, top), size = Size(width, height))
        drawRect(
            color = ink,
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(1.5.dp.toPx()),
        )
    }
}




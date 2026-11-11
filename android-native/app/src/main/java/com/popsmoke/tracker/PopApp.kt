package com.popsmoke.tracker

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.popsmoke.tracker.data.Languages
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.data.StoreState
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopPresets
import com.popsmoke.tracker.design.toHexString
import com.popsmoke.tracker.feature.AppUiState
import com.popsmoke.tracker.feature.analysis.AnalysisScreen
import com.popsmoke.tracker.feature.api.ApiScreen
import com.popsmoke.tracker.feature.dashboard.DashboardScreen
import com.popsmoke.tracker.feature.history.HistoryScreen
import com.popsmoke.tracker.feature.settings.SettingsScreen
import com.popsmoke.tracker.i18n.PopStrings
import com.popsmoke.tracker.i18n.popStrings
import com.popsmoke.tracker.ui.atoms.PopButton
import com.popsmoke.tracker.ui.atoms.PopButtonVariant
import com.popsmoke.tracker.ui.atoms.PopDialog
import com.popsmoke.tracker.ui.atoms.PopTimePickerDialog
import com.popsmoke.tracker.ui.chrome.PopBottomBar
import com.popsmoke.tracker.ui.chrome.PopTopBar
import com.popsmoke.tracker.ui.foundation.LocalPopBackdrop
import com.popsmoke.tracker.ui.foundation.PopAurora
import com.popsmoke.tracker.ui.foundation.PopDotGrid
import com.popsmoke.tracker.ui.foundation.rememberBackdrop

/** 所有会改数据的动作集中在一个数据类里，页面只声明"我能做什么"，不持有 ViewModel。 */
data class PopActions(
    val onRecord: () -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onUpdateLogTime: (String, Long) -> Unit = { _, _ -> },
    val onClearLogs: () -> Unit = {},
    val onClearOpLogs: () -> Unit = {},
    val onSetLanguage: (String) -> Unit = {},
    val onSetThemeColor: (String) -> Unit = {},
    val onSetThemeStyle: (String) -> Unit = {},
    val onSetDailyLimit: (Int) -> Unit = {},
    val onSetWarningLimit: (Int) -> Unit = {},
    val onImportBackup: (StoreState, Boolean) -> Unit = { _, _ -> },
    val onSignIn: (String, String) -> Unit = { _, _ -> },
    val onSignUp: (String, String) -> Unit = { _, _ -> },
    val onSignOut: () -> Unit = {},
    val onPushSettings: () -> Unit = {},
    val onPullSettings: () -> Unit = {},
    val onResolveConflict: (Boolean) -> Unit = {},
    val onDismissConflict: () -> Unit = {},
    val onSyncNow: () -> Unit = {},
    val onSyncSingleOp: (String) -> Unit = {},
    val onRemoveQueueOp: (String) -> Unit = {},
    val onClearQueue: () -> Unit = {},
    val onBackupToFeishu: () -> Unit = {},
    val onRestoreFromFeishu: () -> Unit = {},
    val onDismissNotice: () -> Unit = {},
    val onApiConfigSaved: () -> Unit = {},
)

private enum class PopTab { Tracker, Analysis, History, Api, Settings }

/**
 * 应用外壳：纸面底纹 + 顶栏 + 内容 + 底栏，外加全站唯一的弹窗宿主。
 *
 * 弹窗放在这一层而不是页面里，是为了让遮罩盖住顶栏和底栏（与 Web 版一致），
 * 同时保证只有一个弹窗能活着 —— 这比每个页面各挂一个 PopConfirm 干净。
 *
 * 返回键：不在记录页时先回记录页，在记录页则交给系统（退出）。
 * 这一条是 Web 版没有的，手机上按返回直接退出会让人以为 App 崩了。
 */
@Composable
fun PopApp(
    ui: AppUiState,
    actions: PopActions,
) {
    val language = ui.settings.language
    val strings: PopStrings = remember(language) { popStrings(language) }
    // safeDrawing 而不是 statusBars：前者把刘海/挖孔也算进去，顶栏内容不会被摄像头挖掉一块。
    val topInset = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Top)
        .asPaddingValues()
        .calculateTopPadding()
    val bottomInset = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    val haptic = LocalHapticFeedback.current

    var pendingDelete by remember { mutableStateOf<SmokeLog?>(null) }
    var pendingEdit by remember { mutableStateOf<SmokeLog?>(null) }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = PopTab.entries[tabIndex.coerceIn(0, PopTab.entries.lastIndex)]
    val themePresets = remember { PopPresets.All.map { it.color.toHexString() } }

    val tabs = listOf(
        strings.tracker,
        strings.analysis,
        strings.history,
        strings.api,
        strings.settings,
    )

    BackHandler(enabled = tab != PopTab.Tracker) { tabIndex = 0 }

    // 背景由皮肤决定：波普是纸面点阵，玻璃是极光光斑。
    // 玻璃面板是半透明的，背后必须有东西可透，否则整套"通透"就塌成浅灰圆角。
    val current = LocalSkin.current
    val glass = current.glass
    BoxWithConstraints(Modifier.fillMaxSize().background(current.palette.canvas)) {
        if (current.glass) {
            PopAurora(Modifier.matchParentSize(), accent = ui.dashboard.accent)
        } else {
            PopDotGrid(Modifier.matchParentSize())
        }

        // 背景快照：玻璃面板靠它做折射。背景是静态的，所以这张位图只在
        // 尺寸或主题色变化时重算一次，之后一直复用。波普主题不需要，给 null。
        val backdrop = if (glass) {
            rememberBackdrop(
                accent = ui.dashboard.accent,
                widthPx = constraints.maxWidth,
                heightPx = constraints.maxHeight,
            )
        } else {
            null
        }

        CompositionLocalProvider(LocalPopBackdrop provides backdrop) {
        Column(Modifier.fillMaxSize()) {
            PopTopBar(
                accentColor = ui.dashboard.accent,
                languageLabel = Languages.shortLabel(language),
                topInset = topInset,
                themeDescription = strings.cdThemeColor,
                languageDescription = strings.cdLanguage,
                currentTheme = ui.settings.themeColor,
                currentLanguage = language,
                themePresets = themePresets,
                onTitleClick = { tabIndex = 0 },
                onSelectTheme = actions.onSetThemeColor,
                onSelectLanguage = actions.onSetLanguage,
            )

            Box(Modifier.weight(1f)) {
                // 页签切换：淡入 + 极轻微放大。波普主题下时长更短——
                // 贴纸不该有"飘进来"的缓动，那会让它看起来像塑料。
                AnimatedContent(
                    targetState = tab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        // transitionSpec 不是 composable，皮肤要在外面先取好
                        val enterSpec = if (glass) {
                            tween<Float>(durationMillis = 260, delayMillis = 40)
                        } else {
                            tween<Float>(durationMillis = 120)
                        }
                        (fadeIn(enterSpec) + scaleIn(
                            initialScale = if (glass) 0.97f else 1f,
                            animationSpec = enterSpec,
                        )) togetherWith fadeOut(tween(if (glass) 150 else 80))
                    },
                    label = "tab-switch",
                ) { activeTab ->
                when (activeTab) {
                    PopTab.Tracker -> DashboardScreen(
                        ui = ui.dashboard,
                        strings = strings,
                        onRecord = {
                            // 记录是这个 App 最核心的一次点击，值得一个触觉确认：
                            // 屏幕可能没正对着看，震动比动画更快让人知道"记上了"。
                            // 用 TextHandleMove（一次轻振）而不是 LongPress —— 后者在 Android 上
                            // 是实打实的 500ms 长振，点一下麻半秒，反而像卡住了。
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            actions.onRecord()
                        },
                        onRequestEdit = { pendingEdit = it },
                        onRequestDelete = { pendingDelete = it },
                        modifier = Modifier.fillMaxSize(),
                    )

                    PopTab.Analysis -> AnalysisScreen(
                        logs = ui.logs,
                        settings = ui.settings,
                        opLogs = ui.opLogs,
                        strings = strings,
                        onClearOpLogs = actions.onClearOpLogs,
                        // 登录入口在设置页，不是接口页：接口页只填地址和 key。
                        onGoToSettings = { tabIndex = PopTab.Settings.ordinal },
                        modifier = Modifier.fillMaxSize(),
                        cloud = ui.cloud,
                        onSyncNow = actions.onSyncNow,
                        onSyncOneOp = actions.onSyncSingleOp,
                        onRemoveOp = actions.onRemoveQueueOp,
                        onClearQueue = actions.onClearQueue,
                    )

                    PopTab.History -> HistoryScreen(
                        logs = ui.logs,
                        settings = ui.settings,
                        strings = strings,
                        modifier = Modifier.fillMaxSize(),
                    )

                    PopTab.Api -> ApiScreen(
                        strings = strings,
                        modifier = Modifier.fillMaxSize(),
                        cloud = ui.cloud,
                        onBackupToFeishu = actions.onBackupToFeishu,
                        onRestoreFromFeishu = actions.onRestoreFromFeishu,
                        onDismissNotice = actions.onDismissNotice,
                        onConfigSaved = actions.onApiConfigSaved,
                    )

                    PopTab.Settings -> SettingsScreen(
                        settings = ui.settings,
                        strings = strings,
                        language = language,
                        logs = ui.logs,
                        opLogs = ui.opLogs,
                        onSetLanguage = actions.onSetLanguage,
                        onSetThemeColor = actions.onSetThemeColor,
                        onSetThemeStyle = actions.onSetThemeStyle,
                        onSetDailyLimit = actions.onSetDailyLimit,
                        onSetWarningLimit = actions.onSetWarningLimit,
                        onClearLogs = actions.onClearLogs,
                        onImportBackup = actions.onImportBackup,
                        onGoToApi = { tabIndex = PopTab.Api.ordinal },
                        cloud = ui.cloud,
                        onSignIn = actions.onSignIn,
                        onSignUp = actions.onSignUp,
                        onSignOut = actions.onSignOut,
                        onPushSettings = actions.onPushSettings,
                        onPullSettings = actions.onPullSettings,
                        onResolveConflict = actions.onResolveConflict,
                        onDismissConflict = actions.onDismissConflict,
                        onSyncNow = actions.onSyncNow,
                        onDismissNotice = actions.onDismissNotice,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                }
            }

            PopBottomBar(
                items = tabs,
                selectedIndex = tabIndex,
                bottomInset = bottomInset,
                onSelect = { tabIndex = it },
            )
        }
        }

        PopDialog(
            visible = pendingDelete != null,
            title = strings.delete,
            message = strings.confirmDelete,
            titleColor = PopColor.Status.Warning,
            onDismiss = { pendingDelete = null },
            actions = {
                PopButton(
                    text = strings.cancel,
                    onClick = { pendingDelete = null },
                    modifier = Modifier.weight(1f),
                    variant = PopButtonVariant.Secondary,
                )
                PopButton(
                    text = strings.delete,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pendingDelete?.let { actions.onDelete(it.id) }
                        pendingDelete = null
                    },
                    modifier = Modifier.weight(1f),
                    variant = PopButtonVariant.Danger,
                )
            },
        )

        pendingEdit?.let { editing ->
            PopTimePickerDialog(
                visible = true,
                initialTimestamp = editing.timestamp,
                strings = strings,
                onCancel = { pendingEdit = null },
                onSave = { next ->
                    actions.onUpdateLogTime(editing.id, next)
                    pendingEdit = null
                },
            )
        }
    }
}

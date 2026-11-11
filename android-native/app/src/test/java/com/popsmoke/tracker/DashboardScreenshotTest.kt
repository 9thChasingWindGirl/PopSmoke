package com.popsmoke.tracker

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.popsmoke.tracker.data.AppSettings
import com.popsmoke.tracker.data.OpLog
import com.popsmoke.tracker.data.OpLogType
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.data.cloud.CloudUser
import com.popsmoke.tracker.data.sync.SyncOp
import com.popsmoke.tracker.data.sync.SyncOpStatus
import com.popsmoke.tracker.data.sync.SyncOpType
import com.popsmoke.tracker.data.cloud.RemoteSettings
import com.popsmoke.tracker.feature.cloud.CloudUiState
import com.popsmoke.tracker.design.PopColor
import com.popsmoke.tracker.design.PopLayout
import com.popsmoke.tracker.design.PopPresets
import com.popsmoke.tracker.design.PopSpace
import com.popsmoke.tracker.design.LocalSkin
import com.popsmoke.tracker.design.PopTheme
import com.popsmoke.tracker.design.PopThemeStyle
import com.popsmoke.tracker.design.toHexString
import com.popsmoke.tracker.design.toPopColor
import com.popsmoke.tracker.feature.analysis.AnalysisScreen
import com.popsmoke.tracker.feature.api.ApiScreen
import com.popsmoke.tracker.feature.dashboard.DashboardScreen
import com.popsmoke.tracker.feature.dashboard.DashboardUiState
import com.popsmoke.tracker.feature.dashboard.LimitAlert
import com.popsmoke.tracker.feature.history.HistoryScreen
import com.popsmoke.tracker.feature.settings.SettingsScreen
import com.popsmoke.tracker.i18n.popStrings
import com.popsmoke.tracker.ui.atoms.PopCard
import com.popsmoke.tracker.ui.atoms.PopColorPicker
import com.popsmoke.tracker.ui.atoms.PopMenuPanel
import com.popsmoke.tracker.ui.atoms.PopMenuItem
import com.popsmoke.tracker.ui.atoms.PopOperationLog
import com.popsmoke.tracker.ui.atoms.PopSyncQueue
import com.popsmoke.tracker.ui.atoms.PopTimePickerDialog
import com.popsmoke.tracker.ui.chrome.PopBottomBar
import com.popsmoke.tracker.ui.chrome.PopTopBar
import com.popsmoke.tracker.ui.foundation.PopAurora
import com.popsmoke.tracker.ui.foundation.PopDotGrid
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * 用 Paparazzi 在 JVM 里把 Compose 渲染成 PNG，不需要设备/模拟器。
 *
 * 这不是"好看用的截图"，是视觉回归的基座：
 * 后续任何一次改动都可以 `recordPaparazziDebug` 重录、`verifyPaparazziDebug` 比对，
 * 把"无损"从口头承诺变成可拦截的 diff。
 */
class DashboardScreenshotTest {

    /**
     * 设置页要注册系统文件选择器（导入备份用），而 Paparazzi 的 JVM 环境里没有 Activity，
     * 找不到 ActivityResultRegistryOwner 会直接抛 IllegalStateException。
     * 这里给一个空实现：截图只关心布局，不关心能不能真的弹出选择器。
     */
    private class EmptyRegistryOwner : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) = Unit
        }
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    private fun at(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        return LocalDate.of(2026, 11, 11).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    }

    private fun sampleState(
        count: Int,
        language: String,
        themeColor: String,
        limit: Int = 10,
        warningLimit: Int = 5,
    ): DashboardUiState {
        val logs = (1..count).map { index ->
            SmokeLog(
                id = "sample-$index",
                timestamp = at(9 + index, 5 * index % 60),
                recordIndex = index,
            )
        }.sortedByDescending { it.timestamp }

        return DashboardUiState(
            settings = AppSettings(
                language = language,
                themeColor = themeColor,
                dailyLimit = limit,
                warningLimit = warningLimit,
            ),
            todayLogs = logs,
            todayCount = count,
            consecutiveDays = if (count == 0) 0 else 4,
            lastRecordClock = when {
                count == 0 -> null
                language == "en" -> "09:35 AM"
                else -> "09:35"
            },
            alert = when {
                count >= limit -> LimitAlert.Reached
                count >= warningLimit -> LimitAlert.Warning
                else -> LimitAlert.None
            },
        )
    }

    @Composable
    private fun Screen(
        state: DashboardUiState,
        accent: Color,
        style: PopThemeStyle = PopThemeStyle.Pop,
    ) {
        PopTheme(accent = accent, style = style) {
            Backdrop(accent) {
                DashboardScreen(
                    ui = state,
                    strings = popStrings(state.settings.language),
                    onRecord = {},
                    onRequestEdit = {},
                    onRequestDelete = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    /**
     * 页面底：波普是点阵纸纹，玻璃是极光光斑。
     * 必须和 PopApp 用同一套判断 —— 否则截图里的底和真机上不一样，基准就失去意义。
     */
    @Composable
    private fun Backdrop(accent: Color, content: @Composable BoxScope.() -> Unit) {
        val skin = LocalSkin.current
        Box(Modifier.fillMaxSize().background(skin.palette.canvas)) {
            if (skin.glass) {
                PopAurora(Modifier.matchParentSize(), accent = accent, animate = false)
            } else {
                PopDotGrid(Modifier.matchParentSize())
            }
            content()
        }
    }

    /** 整屏骨架：顶栏 + 内容 + 底栏。所有整屏用例共用，避免每处重复三段式。 */
    @Composable
    private fun Chrome(
        state: DashboardUiState,
        languageLabel: String,
        tabs: List<String>,
        flipped: Boolean = false,
        style: PopThemeStyle = PopThemeStyle.Pop,
    ) {
        PopTheme(accent = state.accent, style = style) {
            Backdrop(state.accent) {
                Column(Modifier.fillMaxSize()) {
                    PopTopBar(
                        accentColor = state.accent,
                        languageLabel = languageLabel,
                        topInset = 24.dp,
                        themeDescription = "",
                        languageDescription = "",
                        currentTheme = state.settings.themeColor,
                        currentLanguage = state.settings.language,
                        themePresets = emptyList(),
                        onTitleClick = {},
                        onSelectTheme = {},
                        onSelectLanguage = {},
                    )
                    DashboardScreen(
                        ui = state,
                        strings = popStrings(state.settings.language),
                        onRecord = {},
                        onRequestEdit = {},
                        onRequestDelete = {},
                        modifier = Modifier.weight(1f),
                        initialFlipped = flipped,
                    )
                    PopBottomBar(
                        items = tabs,
                        selectedIndex = 0,
                        bottomInset = 16.dp,
                        onSelect = {},
                    )
                }
            }
        }
    }

    @Test
    fun front_en_default_theme() {
        val state = sampleState(count = 3, language = "en", themeColor = "#F97316")
        paparazzi.snapshot { Screen(state, state.accent) }
    }

    @Test
    fun front_zh_teal_theme() {
        val state = sampleState(count = 6, language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot { Screen(state, state.accent) }
    }

    @Test
    fun front_ja_limit_reached() {
        val state = sampleState(count = 10, language = "ja", themeColor = "#FF6B6B")
        paparazzi.snapshot { Screen(state, state.accent) }
    }

    @Test
    fun front_en_empty() {
        val state = sampleState(count = 0, language = "en", themeColor = "#FFD700")
        paparazzi.snapshot { Screen(state, state.accent) }
    }

    /** 整屏（含顶栏与底栏）：用于和 Web 版并排比对"味道"。 */
    @Test
    fun full_screen_with_chrome_en() {
        val state = sampleState(count = 3, language = "en", themeColor = "#F97316")
        paparazzi.snapshot { Chrome(state, "EN", listOf("TRACKER", "ANALYSIS", "HISTORY", "API", "SETTINGS")) }
    }

    /** 整屏 + 达到上限提示：用来钉住"提示条不得改变底栏位置"这条红线。 */
    @Test
    fun full_screen_with_warning_zh() {
        val state = sampleState(count = 6, language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot { Chrome(state, "中", listOf("记录", "分析", "历史", "接口", "设置")) }
    }

    /**
     * 背面（今日记录列表）：必须走真实翻转态渲染。
     *
     * 早先这个用例是把背面单独拿出来 fillMaxSize 渲染的，于是截图里背面被拉成整屏高、
     * 和主区域对不上 —— 那既不是真实效果，也量不出"背面是否与正面等大"。
     * 现在通过 initialFlipped 进入翻转态，背面由正面高度约束。
     */
    @Test
    fun back_flipped_logs_zh() {
        val state = sampleState(count = 5, language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot { Chrome(state, "中", listOf("记录", "分析", "历史", "接口", "设置"), flipped = true) }
    }

    /** 满额 10 条：验证背面在正面高度内滚动，不撑大卡片、不顶开底栏。 */
    @Test
    fun back_flipped_logs_full_zh() {
        val state = sampleState(count = 10, language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot { Chrome(state, "中", listOf("记录", "分析", "历史", "接口", "设置"), flipped = true) }
    }

    // ------------------------------------------------------------------
    // 其余四个页面
    // ------------------------------------------------------------------

    /** 跨三个月的记录，用来同时验证「月 → 日」两级分组和月度统计。 */
    private fun crossMonthLogs(): List<SmokeLog> {
        val zone = ZoneId.systemDefault()
        val days = listOf(
            LocalDate.of(2026, 11, 11) to 3,
            LocalDate.of(2026, 11, 10) to 7,
            LocalDate.of(2026, 11, 8) to 2,
            LocalDate.of(2026, 10, 30) to 11,
            LocalDate.of(2026, 10, 21) to 4,
            LocalDate.of(2026, 9, 15) to 6,
        )
        var index = 0
        return days.flatMap { (date, count) ->
            (1..count).map { slot ->
                index += 1
                SmokeLog(
                    id = "hist-${date}-$slot",
                    timestamp = date.atTime(8 + slot, 5 * slot % 60).atZone(zone).toInstant().toEpochMilli(),
                    recordIndex = index,
                )
            }
        }.sortedByDescending { it.timestamp }
    }

    @Composable
    private fun Page(
        settings: AppSettings,
        languageLabel: String,
        tabs: List<String>,
        selectedIndex: Int,
        style: PopThemeStyle = PopThemeStyle.Pop,
        content: @Composable () -> Unit,
    ) {
        val accent = settings.themeColor.toPopColor()
        PopTheme(accent = accent, style = style) {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides EmptyRegistryOwner()) {
                Backdrop(accent) {
                    Column(Modifier.fillMaxSize()) {
                        PopTopBar(
                            accentColor = accent,
                            languageLabel = languageLabel,
                            topInset = 24.dp,
                            themeDescription = "",
                            languageDescription = "",
                            currentTheme = settings.themeColor,
                            currentLanguage = settings.language,
                            themePresets = emptyList(),
                            onTitleClick = {},
                            onSelectTheme = {},
                            onSelectLanguage = {},
                        )
                        Box(Modifier.weight(1f)) { content() }
                        PopBottomBar(
                            items = tabs,
                            selectedIndex = selectedIndex,
                            bottomInset = 16.dp,
                            onSelect = {},
                        )
                    }
                }
            }
        }
    }

    @Test
    fun history_page_zh() {
        val settings = AppSettings(language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            Page(settings, "中", listOf("记录", "分析", "历史", "接口", "设置"), 2) {
                HistoryScreen(
                    logs = crossMonthLogs(),
                    settings = settings,
                    strings = popStrings("zh"),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Test
    fun analysis_week_en() {
        val settings = AppSettings(language = "en", themeColor = "#F97316", dailyLimit = 6)
        paparazzi.snapshot {
            Page(settings, "EN", listOf("TRACKER", "ANALYSIS", "HISTORY", "API", "SETTINGS"), 1) {
                AnalysisScreen(
                    logs = crossMonthLogs(),
                    settings = settings,
                    opLogs = sampleOpLogs(),
                    strings = popStrings("en"),
                    onClearOpLogs = {},
                    onGoToSettings = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Test
    fun settings_page_en() {
        val settings = AppSettings(language = "en", themeColor = "#F97316")
        paparazzi.snapshot {
            Page(settings, "EN", listOf("TRACKER", "ANALYSIS", "HISTORY", "API", "SETTINGS"), 4) {
                SettingsScreen(
                    settings = settings,
                    strings = popStrings("en"),
                    language = "en",
                    logs = crossMonthLogs(),
                    opLogs = sampleOpLogs(),
                    onSetLanguage = {},
                    onSetThemeColor = {},
                    onSetDailyLimit = {},
                    onSetWarningLimit = {},
                    onClearLogs = {},
                    onImportBackup = { _, _ -> },
                    onGoToApi = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    /** 已登录：账户卡换成邮箱 + 三个动作。 */
    @Test
    fun settings_account_signed_in_zh() {
        val settings = AppSettings(language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            Page(settings, "中", listOf("记录", "分析", "历史", "接口", "设置"), 4) {
                SettingsScreen(
                    settings = settings,
                    strings = popStrings("zh"),
                    language = "zh",
                    logs = crossMonthLogs(),
                    opLogs = sampleOpLogs(),
                    onSetLanguage = {},
                    onSetThemeColor = {},
                    onSetDailyLimit = {},
                    onSetWarningLimit = {},
                    onClearLogs = {},
                    onImportBackup = { _, _ -> },
                    onGoToApi = {},
                    cloud = CloudUiState(
                        supabaseReady = true,
                        user = CloudUser(id = "u-1", email = "me@example.com"),
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    /** 未登录：邮箱/密码 + 登录/注册。 */
    @Test
    fun settings_account_signed_out_zh() {
        val settings = AppSettings(language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            Page(settings, "中", listOf("记录", "分析", "历史", "接口", "设置"), 4) {
                SettingsScreen(
                    settings = settings,
                    strings = popStrings("zh"),
                    language = "zh",
                    logs = crossMonthLogs(),
                    opLogs = sampleOpLogs(),
                    onSetLanguage = {},
                    onSetThemeColor = {},
                    onSetDailyLimit = {},
                    onSetWarningLimit = {},
                    onClearLogs = {},
                    onImportBackup = { _, _ -> },
                    onGoToApi = {},
                    cloud = CloudUiState(supabaseReady = true),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    /** 登录后两边设置不一致时的裁决弹窗。 */
    @Test
    fun settings_cloud_conflict_zh() {
        val settings = AppSettings(language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            Page(settings, "中", listOf("记录", "分析", "历史", "接口", "设置"), 4) {
                SettingsScreen(
                    settings = settings,
                    strings = popStrings("zh"),
                    language = "zh",
                    logs = crossMonthLogs(),
                    opLogs = sampleOpLogs(),
                    onSetLanguage = {},
                    onSetThemeColor = {},
                    onSetDailyLimit = {},
                    onSetWarningLimit = {},
                    onClearLogs = {},
                    onImportBackup = { _, _ -> },
                    onGoToApi = {},
                    cloud = CloudUiState(
                        supabaseReady = true,
                        user = CloudUser(id = "u-1", email = "me@example.com"),
                        conflict = RemoteSettings(
                            dailyLimit = 20,
                            warningLimit = 15,
                            themeColor = "#FF6B6B",
                            language = "en",
                        ),
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Test
    fun api_page_zh() {
        val settings = AppSettings(language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            Page(settings, "中", listOf("记录", "分析", "历史", "接口", "设置"), 3) {
                ApiScreen(strings = popStrings("zh"), modifier = Modifier.fillMaxSize())
            }
        }
    }

    // ------------------------------------------------------------------
    // 第三批：时间编辑 / 操作日志 / 顶栏浮层
    // ------------------------------------------------------------------

    /**
     * 队列样例：两条正常待推送 + 一条失败。
     * 故意混着三种类型和两种状态，一张图就能看出图标、状态徽标、两行布局是否都对。
     */
    private fun sampleQueueOps(): List<SyncOp> = listOf(
        SyncOp("q-1", SyncOpType.Create, "u-1", "log-3", at(11, 20), 3, at(11, 20)),
        SyncOp("q-2", SyncOpType.Update, "u-1", "log-2", at(12, 5), 2, at(12, 5)),
        SyncOp(
            "q-3",
            SyncOpType.Delete,
            "u-1",
            "log-1",
            at(9, 12),
            1,
            at(12, 40),
            status = SyncOpStatus.Failed,
            attempts = 1,
            lastError = "network",
        ),
    )

    /** 同步队列卡：终端外观、状态徽标、单条动作按钮。 */
    @Test
    fun sync_queue_zh() {
        val settings = AppSettings(language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            Page(settings, "中", listOf("记录", "分析", "历史", "接口", "设置"), 1) {
                AnalysisScreen(
                    logs = crossMonthLogs(),
                    settings = settings,
                    opLogs = sampleOpLogs(),
                    strings = popStrings("zh"),
                    onClearOpLogs = {},
                    onGoToSettings = {},
                    cloud = CloudUiState(
                        supabaseReady = true,
                        user = CloudUser(id = "u-1", email = "me@example.com"),
                        queueOps = sampleQueueOps(),
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    /** 队列卡本体：整页截图看不到它（在第三屏），所以单独渲染一张。 */
    @Test
    fun sync_queue_card_zh() {
        paparazzi.snapshot {
            PopTheme(accent = "#4ECDC4".toPopColor(), style = PopThemeStyle.Pop) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(PopColor.Core.Canvas)
                        .padding(PopSpace.S4),
                ) {
                    PopSyncQueue(
                        ops = sampleQueueOps(),
                        strings = popStrings("zh"),
                        busy = false,
                        onSyncAll = {},
                        onSyncOne = {},
                        onRemove = {},
                        onClear = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    /** 空队列：占位文案与状态栏都要在，否则用户分不清「没东西可推」和「界面坏了」。 */
    @Test
    fun sync_queue_empty_zh() {
        paparazzi.snapshot {
            PopTheme(accent = "#4ECDC4".toPopColor(), style = PopThemeStyle.Pop) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(PopColor.Core.Canvas)
                        .padding(PopSpace.S4),
                ) {
                    PopSyncQueue(
                        ops = emptyList(),
                        strings = popStrings("zh"),
                        busy = false,
                        onSyncAll = {},
                        onSyncOne = {},
                        onRemove = {},
                        onClear = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    private fun sampleOpLogs(): List<OpLog> = listOf(
        OpLog("op-1", OpLogType.Create, at(9, 12), recordIndex = 1),
        OpLog("op-2", OpLogType.Create, at(10, 40), recordIndex = 2),
        OpLog("op-3", OpLogType.Update, at(11, 5), recordIndex = 2),
        OpLog("op-4", OpLogType.Delete, at(12, 18), recordIndex = 1),
        OpLog("op-5", OpLogType.Clear, at(13, 2), count = 9),
        OpLog("op-6", OpLogType.Sync, at(14, 30), count = 12),
    )

    /** 时间选择器：滚轮静止态必须能看出「选中行是黑底白字、其余灰字」。 */
    @Test
    fun time_picker_dialog_zh() {
        val settings = AppSettings(language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            PopTheme(accent = settings.themeColor.toPopColor(), style = PopThemeStyle.Pop) {
                Box(Modifier.fillMaxSize().background(PopColor.Core.Canvas)) {
                    PopTimePickerDialog(
                        visible = true,
                        initialTimestamp = at(14, 35),
                        strings = popStrings("zh"),
                        onCancel = {},
                        onSave = {},
                    )
                }
            }
        }
    }

    /** 操作日志终端卡：黑头、等宽行、灰状态栏三段都要在。 */
    @Test
    fun operation_log_zh() {
        val settings = AppSettings(language = "zh", themeColor = "#4ECDC4")
        val strings = popStrings("zh")
        paparazzi.snapshot {
            PopTheme(accent = settings.themeColor.toPopColor(), style = PopThemeStyle.Pop) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(PopColor.Core.Canvas)
                        .padding(PopSpace.S4),
                ) {
                    PopCard(modifier = Modifier.fillMaxWidth(), title = strings.operationLog) {
                        PopOperationLog(
                            logs = sampleOpLogs(),
                            strings = strings,
                            onClear = {},
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    /**
     * 顶栏取色浮层。
     *
     * Popup 是独立窗口，Paparazzi 抓的是根视图，所以这里直接渲染面板本体
     * （PopMenuPanel + PopColorPicker）—— 要钉的是面板的 280dp 宽、4dp 描边和硬阴影，
     * 弹出位置由 Popup 负责，不需要在截图里验证。
     */
    @Test
    fun topbar_theme_menu_zh() {
        paparazzi.snapshot {
            PopTheme(accent = "#4ECDC4".toPopColor(), style = PopThemeStyle.Pop) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(PopColor.Core.Canvas)
                        .padding(PopSpace.S6),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    PopMenuPanel(width = PopLayout.ThemePanelWidth) {
                        PopColorPicker(
                            value = "#4ECDC4",
                            onChange = {},
                            presets = PopPresets.All.map { it.color.toHexString() },
                        )
                    }
                }
            }
        }
    }

    /** 顶栏语言菜单：四项竖排，选中项浅灰底。 */
    @Test
    fun topbar_language_menu_zh() {
        paparazzi.snapshot {
            PopTheme(accent = "#4ECDC4".toPopColor(), style = PopThemeStyle.Pop) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(PopColor.Core.Canvas)
                        .padding(PopSpace.S6),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    PopMenuPanel(width = PopLayout.MenuMinWidth, padding = PaddingValues(0.dp)) {
                        listOf("EN" to false, "中" to true, "日" to false, "한" to false).forEach { (label, active) ->
                            PopMenuItem(
                                text = label,
                                selected = active,
                                onClick = {},
                            )
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 液态玻璃主题
    //
    // 与上面那批波普用例并存，而不是替换它们：两套皮肤共用同一批组件，
    // 只留一套基准的话，另一套被改坏时没有任何信号。
    // ------------------------------------------------------------------

    /** 首屏：看玻璃卡片的高光边、柔和投影，以及光斑有没有被卡片"吃掉"。 */
    @Test
    fun glass_front_en() {
        val state = sampleState(count = 3, language = "en", themeColor = "#F97316")
        paparazzi.snapshot { Screen(state, state.accent, PopThemeStyle.Liquid) }
    }

    @Test
    fun glass_front_empty_zh() {
        val state = sampleState(count = 0, language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot { Screen(state, state.accent, PopThemeStyle.Liquid) }
    }

    /** 整屏：顶栏底栏的半透明条、发丝线，以及极光背景在整屏上的分布。 */
    @Test
    fun glass_full_screen_zh() {
        val state = sampleState(count = 6, language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            Chrome(state, "中", listOf("记录", "分析", "历史", "接口", "设置"), style = PopThemeStyle.Liquid)
        }
    }

    /** 翻转背面：记录列表在玻璃上的分隔线密度。 */
    @Test
    fun glass_back_flipped_zh() {
        val state = sampleState(count = 5, language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            Chrome(state, "中", listOf("记录", "分析", "历史", "接口", "设置"), flipped = true, style = PopThemeStyle.Liquid)
        }
    }

    /** 取色浮层：玻璃面板 + 色盘，最容易出现"浮层顶出屏幕"或"高光把内容糊住"。 */
    @Test
    fun glass_theme_menu_zh() {
        paparazzi.snapshot {
            PopTheme(accent = "#4ECDC4".toPopColor(), style = PopThemeStyle.Liquid) {
                Backdrop("#4ECDC4".toPopColor()) {
                    Box(
                        Modifier.fillMaxSize().padding(PopSpace.S6),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        PopMenuPanel(width = PopLayout.ThemePanelWidth) {
                            PopColorPicker(
                                value = "#4ECDC4",
                                onChange = {},
                                presets = PopPresets.All.map { it.color.toHexString() },
                            )
                        }
                    }
                }
            }
        }
    }

    /** 队列卡与操作日志是"深色头 + 彩色状态"的密集区域，玻璃下最容易脏。 */
    @Test
    fun glass_sync_queue_zh() {
        paparazzi.snapshot {
            PopTheme(accent = "#4ECDC4".toPopColor(), style = PopThemeStyle.Liquid) {
                Backdrop("#4ECDC4".toPopColor()) {
                    Box(Modifier.fillMaxSize().padding(PopSpace.S4)) {
                        PopSyncQueue(
                            ops = sampleQueueOps(),
                            strings = popStrings("zh"),
                            busy = false,
                            onSyncAll = {},
                            onSyncOne = {},
                            onRemove = {},
                            onClear = {},
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    /** 设置页在玻璃下是最"密"的一屏：卡片、输入、开关、彩色标签全在一页里。 */
    @Test
    fun glass_settings_zh() {
        val settings = AppSettings(language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            Page(settings, "中", listOf("记录", "分析", "历史", "接口", "设置"), 4, style = PopThemeStyle.Liquid) {
                SettingsScreen(
                    settings = settings,
                    strings = popStrings("zh"),
                    language = "zh",
                    logs = crossMonthLogs(),
                    opLogs = sampleOpLogs(),
                    onSetLanguage = {},
                    onSetThemeColor = {},
                    onSetDailyLimit = {},
                    onSetWarningLimit = {},
                    onClearLogs = {},
                    onImportBackup = { _, _ -> },
                    onGoToApi = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Test
    fun glass_history_zh() {
        val settings = AppSettings(language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            Page(settings, "中", listOf("记录", "分析", "历史", "接口", "设置"), 2, style = PopThemeStyle.Liquid) {
                HistoryScreen(
                    logs = crossMonthLogs(),
                    settings = settings,
                    strings = popStrings("zh"),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Test
    fun glass_analysis_zh() {
        val settings = AppSettings(language = "zh", themeColor = "#4ECDC4")
        paparazzi.snapshot {
            Page(settings, "中", listOf("记录", "分析", "历史", "接口", "设置"), 1, style = PopThemeStyle.Liquid) {
                AnalysisScreen(
                    logs = crossMonthLogs(),
                    settings = settings,
                    opLogs = sampleOpLogs(),
                    strings = popStrings("zh"),
                    onClearOpLogs = {},
                    onGoToSettings = {},
                    modifier = Modifier.fillMaxSize(),
                    cloud = CloudUiState(),
                    onSyncNow = {},
                    onSyncOneOp = {},
                    onRemoveOp = {},
                    onClearQueue = {},
                )
            }
        }
    }
}

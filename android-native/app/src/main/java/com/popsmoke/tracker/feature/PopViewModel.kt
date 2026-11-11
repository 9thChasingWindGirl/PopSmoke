package com.popsmoke.tracker.feature

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.popsmoke.tracker.data.ApiConfigStore
import com.popsmoke.tracker.data.AppSettings
import com.popsmoke.tracker.data.LocalStore
import com.popsmoke.tracker.data.LogRepository
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.data.StoreState
import com.popsmoke.tracker.data.cloud.CloudIssue
import com.popsmoke.tracker.data.cloud.CloudResult
import com.popsmoke.tracker.data.cloud.CloudService
import com.popsmoke.tracker.data.cloud.EncryptedSessionStore
import com.popsmoke.tracker.data.cloud.LogSyncService
import com.popsmoke.tracker.data.cloud.SignUpOutcome
import com.popsmoke.tracker.data.cloud.toRemote
import com.popsmoke.tracker.data.sync.FileSyncQueueStore
import com.popsmoke.tracker.data.sync.SyncQueue
import com.popsmoke.tracker.data.formatClock
import com.popsmoke.tracker.data.startOfDayMillis
import com.popsmoke.tracker.feature.cloud.CloudNotice
import com.popsmoke.tracker.feature.cloud.CloudNoticeKind
import com.popsmoke.tracker.feature.cloud.CloudUiState
import com.popsmoke.tracker.feature.dashboard.DashboardUiState
import com.popsmoke.tracker.feature.dashboard.LimitAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 全应用唯一的 ViewModel。
 *
 * Web 版是每个页面 useState + 父层传 props；原生侧改成一个 StateFlow 下发，
 * 好处是"记录一支烟"这一个动作，仪表盘、历史、分析三处会同时更新，不需要回调链。
 *
 * 云端动作也在这里收口：它们改的是同一份设置 / 记录，结果却要落在账户卡上，
 * 拆两个 ViewModel 只会多一层转发。
 */
class PopViewModel(
    private val repository: LogRepository,
    private val cloud: CloudService,
    private val apiConfigStore: ApiConfigStore,
    private val queue: SyncQueue,
    private val logSync: LogSyncService,
) : ViewModel() {

    private val _ui = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = _ui.asStateFlow()

    private val _cloud = MutableStateFlow(CloudUiState())

    init {
        viewModelScope.launch {
            // 读盘失败也要往下走：登录态恢复不该被一份坏数据文件拖死。
            runCatching { repository.init() }
            refreshCloudReadiness()
            val restored = runCatching { cloud.restore() }.getOrNull()
            _cloud.update { it.copy(user = restored) }
        }
        // 这一行是整个界面能不能"动"的关键。
        // 派生 UI 状态时抛一次异常，collect 就再也收不到后续数据，
        // 表现是"点记录没反应、重开也一样"——外面必须接住，单帧失败退回上一帧。
        viewModelScope.launch {
            repository.state.collect { store ->
                _ui.value = runCatching { buildUi(store) }.getOrDefault(_ui.value)
            }
        }
        viewModelScope.launch {
            _cloud.collect { state -> _ui.update { it.copy(cloud = state) } }
        }
        // 队列变化要立刻反映到账户卡的"N 条待同步"和分析页的队列卡上。
        viewModelScope.launch {
            queue.ops.collect { runCatching { refreshQueue() } }
        }
    }

    fun record() {
        viewModelScope.launch { repository.record() }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun updateLogTime(id: String, timestamp: Long) {
        viewModelScope.launch { repository.updateLogTime(id, timestamp) }
    }

    fun clearAllLogs() {
        viewModelScope.launch { repository.clearLogs() }
    }

    fun clearOpLogs() {
        viewModelScope.launch { repository.clearOpLogs() }
    }

    /** 导入备份：合并（保留现有）或覆盖（连设置一起换成备份里的）。 */
    fun importBackup(incoming: StoreState, replace: Boolean) {
        viewModelScope.launch { repository.importState(incoming, replace) }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch { repository.updateSettings { it.copy(language = language) } }
    }

    fun setThemeColor(hex: String) {
        viewModelScope.launch { repository.updateSettings { it.copy(themeColor = hex) } }
    }

    /**
     * 每日上限与预警阈值是一对联动值：上限不能低于阈值。
     * Web 版在 onChange 里顺手把阈值一起压下来，这里保持同一语义。
     */
    fun setDailyLimit(limit: Int) {
        viewModelScope.launch {
            repository.updateSettings { settings ->
                val clamped = limit.coerceIn(1, 50)
                settings.copy(
                    dailyLimit = clamped,
                    warningLimit = settings.warningLimit.coerceAtMost(clamped),
                )
            }
        }
    }

    fun setWarningLimit(limit: Int) {
        viewModelScope.launch {
            repository.updateSettings { settings ->
                settings.copy(warningLimit = limit.coerceIn(1, settings.dailyLimit))
            }
        }
    }

    // ---- 云端 ----

    /** 接口配置页保存后调用：账户卡要立刻从"去配置"变成登录表单。 */
    fun refreshCloudReadiness() {
        val config = apiConfigStore.load()
        _cloud.update {
            it.copy(supabaseReady = config.hasSupabase, feishuReady = config.hasFeishu)
        }
    }

    fun signIn(email: String, password: String) = launchCloud {
        when (val result = cloud.signIn(email, password)) {
            is CloudResult.Ok -> {
                _cloud.update { it.copy(user = result.value, notice = CloudNotice(CloudNoticeKind.SignedIn)) }
                refreshQueue()
                reconcileSettingsAfterSignIn()
            }
            is CloudResult.Fail -> notice(result.issue)
        }
    }

    fun signUp(email: String, password: String) = launchCloud {
        when (val result = cloud.signUp(email, password)) {
            is CloudResult.Ok -> {
                val outcome: SignUpOutcome = result.value
                if (outcome.session != null) {
                    _cloud.update { it.copy(user = outcome.session.user, notice = CloudNotice(CloudNoticeKind.SignedIn)) }
                    refreshQueue()
                    reconcileSettingsAfterSignIn()
                } else {
                    // 项目开了邮箱验证：注册成功但还没有会话，不能假装登录成功。
                    _cloud.update { it.copy(notice = CloudNotice(CloudNoticeKind.SignUpConfirm)) }
                }
            }
            is CloudResult.Fail -> notice(result.issue)
        }
    }

    fun signOut() = launchCloud {
        cloud.signOut()
        _cloud.update { it.copy(user = null, conflict = null, notice = CloudNotice(CloudNoticeKind.SignedOut)) }
        // 队列不清空：它属于刚登出的那个账号，下次登回来还要继续推。
        // 只是不再显示 —— 否则界面上会挂着"待同步 3 条"而没有任何账号可同步。
        refreshQueue()
    }

    fun pushSettingsToCloud() = launchCloud {
        val settings = _ui.value.settings
        when (val result = cloud.pushSettings(settings)) {
            is CloudResult.Ok ->
                _cloud.update { it.copy(notice = CloudNotice(CloudNoticeKind.SettingsPushed), conflict = null) }
            is CloudResult.Fail -> notice(result.issue)
        }
    }

    fun pullSettingsFromCloud() = launchCloud {
        when (val result = cloud.fetchRemoteSettings()) {
            is CloudResult.Ok -> {
                val remote = result.value
                if (remote == null) {
                    notice(CloudIssue.NoCloudSettings)
                } else {
                    repository.updateSettings { remote.toAppSettings() }
                    _cloud.update { it.copy(notice = CloudNotice(CloudNoticeKind.SettingsPulled), conflict = null) }
                }
            }
            is CloudResult.Fail -> notice(result.issue)
        }
    }

    /**
     * 登录后处理"两边设置不一样"。
     *
     * 不在登录流程里自动覆盖：可能是在新手机上装了 App 先调了半天主题，
     * 也可能只是想拿回云端的配置 —— 这两种意图从数据上看一模一样，必须问。
     * 云端还没有配置（首次登录）则直接把本地上传，这也是一种"问"，只是答案唯一。
     */
    fun resolveConflict(useCloud: Boolean) = launchCloud {
        val remote = _cloud.value.conflict
        if (remote == null) return@launchCloud
        if (useCloud) {
            repository.updateSettings { remote.toAppSettings() }
            _cloud.update { it.copy(conflict = null, notice = CloudNotice(CloudNoticeKind.SettingsPulled)) }
        } else {
            when (val result = cloud.pushSettings(_ui.value.settings)) {
                is CloudResult.Ok ->
                    _cloud.update { it.copy(conflict = null, notice = CloudNotice(CloudNoticeKind.SettingsPushed)) }
                is CloudResult.Fail -> {
                    _cloud.update { it.copy(conflict = null) }
                    notice(result.issue)
                }
            }
        }
    }

    fun dismissNotice() {
        _cloud.update { it.copy(notice = null) }
    }

    /** 关闭冲突弹窗：不选任何一边，本地保持原样，下次登录还会再问。 */
    fun dismissConflict() {
        _cloud.update { it.copy(conflict = null) }
    }

    // ---- 记录双向同步 ----

    /**
     * 立即同步：先推队列，再拉云端。
     *
     * 顺序不能反：先拉会把云端旧值盖在本地尚未推送的修改上（虽然队列里的 id 会被跳过，
     * 但新增的记录会先被拉进来占位）。先推后拉，语义上就是"本地的改动先落地，再看别人改了什么"。
     */
    fun syncNow() = launchCloud {
        when (val result = logSync.syncAll()) {
            is CloudResult.Ok -> {
                val report = result.value
                _cloud.update {
                    it.copy(
                        notice = CloudNotice(
                            kind = CloudNoticeKind.LogsSynced,
                            uploaded = report.pushed,
                            pulled = report.pulled,
                            failed = report.failed,
                            pending = report.pending,
                        ),
                    )
                }
            }
            is CloudResult.Fail -> notice(result.issue)
        }
    }

    /** 队列里单条重试：失败后最常用的动作，不该逼用户"全部重试"。 */
    fun syncSingleOp(opId: String) = launchCloud {
        when (val result = logSync.pushOne(opId)) {
            is CloudResult.Ok ->
                _cloud.update { it.copy(notice = CloudNotice(CloudNoticeKind.LogsSynced, uploaded = 1)) }
            is CloudResult.Fail -> notice(result.issue)
        }
    }

    fun removeQueueOp(opId: String) {
        viewModelScope.launch { queue.remove(opId) }
    }

    fun clearQueue() {
        viewModelScope.launch { queue.clear() }
    }

    /** 把队列里属于当前账号的项同步到界面状态。 */
    private fun refreshQueue() {
        val userId = _cloud.value.user?.id
        val visible = if (userId == null) emptyList() else queue.opsFor(userId)
        _cloud.update { it.copy(queueOps = visible) }
    }

    /** 把本地记录补到飞书：只传远端没有的 id。 */
    fun backupToFeishu() = launchCloud {
        when (val result = cloud.backupLogs(_ui.value.logs)) {
            is CloudResult.Ok -> _cloud.update {
                val report = result.value
                it.copy(
                    notice = CloudNotice(
                        kind = CloudNoticeKind.BackupDone,
                        uploaded = report.uploaded,
                        skipped = report.skipped,
                        failed = report.failed,
                    ),
                )
            }
            is CloudResult.Fail -> notice(result.issue)
        }
    }

    /** 从飞书拉回来并合并。返回新增条数，0 条也照样提示"已是最新"。 */
    fun restoreFromFeishu() = launchCloud {
        when (val result = cloud.fetchFeishuLogs()) {
            is CloudResult.Ok -> {
                val added = repository.mergeRemoteLogs(result.value)
                _cloud.update { it.copy(notice = CloudNotice(CloudNoticeKind.Restored, added = added)) }
            }
            is CloudResult.Fail -> notice(result.issue)
        }
    }

    private fun reconcileSettingsAfterSignIn() {
        viewModelScope.launch {
            when (val result = cloud.fetchRemoteSettings()) {
                is CloudResult.Ok -> {
                    val remote = result.value
                    when {
                        remote == null -> cloud.pushSettings(_ui.value.settings)
                        remote.toAppSettings() != _ui.value.settings ->
                            _cloud.update { it.copy(conflict = remote) }
                    }
                }
                // 拿不到配置不影响"已登录"这个事实：把失败留到用户主动点同步时再报，
                // 否则一登录就弹红条，看起来像登录失败。
                is CloudResult.Fail -> Unit
            }
        }
    }

    private fun notice(issue: CloudIssue) {
        _cloud.update { it.copy(notice = CloudNotice(CloudNoticeKind.Failed, issue = issue)) }
    }

    private fun launchCloud(block: suspend () -> Unit) {
        viewModelScope.launch {
            _cloud.update { it.copy(busy = true) }
            try {
                block()
            } finally {
                _cloud.update { it.copy(busy = false) }
            }
        }
    }

    private fun buildUi(store: StoreState): AppUiState {
        val sorted = store.logs.sortedByDescending { it.timestamp }
        return AppUiState(
            settings = store.settings,
            logs = sorted,
            opLogs = store.opLogs.sortedBy { it.at },
            dashboard = buildDashboard(sorted, store.settings),
            cloud = _cloud.value,
        )
    }

    private fun buildDashboard(logs: List<SmokeLog>, settings: AppSettings): DashboardUiState {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val todayStart = startOfDayMillis(today, zone)
        val todayLogs = logs.filter { it.timestamp >= todayStart }

        val count = todayLogs.size
        return DashboardUiState(
            settings = settings,
            todayLogs = todayLogs,
            todayCount = count,
            consecutiveDays = consecutiveDaysWithinLimit(logs, settings, today, zone),
            lastRecordClock = todayLogs.firstOrNull()?.let { formatClock(it.timestamp, settings.language, zone) },
            alert = when {
                count >= settings.dailyLimit -> LimitAlert.Reached
                count >= settings.warningLimit -> LimitAlert.Warning
                else -> LimitAlert.None
            },
        )
    }

    /**
     * 连续达标天数：从今天往回数，某天有记录且当天支数不超过每日上限才算达标，遇到不达标就停。
     * 与 Web 版口径完全一致（包括"今天还没记录时会中断"这一点）。
     */
    private fun consecutiveDaysWithinLimit(
        logs: List<SmokeLog>,
        settings: AppSettings,
        today: LocalDate,
        zone: ZoneId,
    ): Int {
        var count = 0
        var cursor = today
        repeat(365) {
            val start = startOfDayMillis(cursor, zone)
            val end = startOfDayMillis(cursor.plusDays(1), zone)
            val dayCount = logs.count { it.timestamp in start until end }
            if (dayCount in 1..settings.dailyLimit) {
                count++
                cursor = cursor.minusDays(1)
            } else {
                return count
            }
        }
        return count
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dataDir = File(appContext.filesDir, "pop-data")
                    val store = LocalStore(dataDir)
                    val configStore = ApiConfigStore(dataDir)
                    val cloud = CloudService(
                        configProvider = { configStore.load() },
                        sessions = EncryptedSessionStore(dataDir),
                    )
                    val queue = SyncQueue(FileSyncQueueStore(dataDir))
                    // 仓库要知道"现在是谁在记"，才决定要不要入队；
                    // 但仓库本身不认识网络层，只拿一个取 id 的函数。
                    val repository = LogRepository(store, queue) { cloud.currentUser()?.id }
                    val logSync = LogSyncService(queue, cloud, repository)
                    return PopViewModel(repository, cloud, configStore, queue, logSync) as T
                }
            }
        }
    }
}

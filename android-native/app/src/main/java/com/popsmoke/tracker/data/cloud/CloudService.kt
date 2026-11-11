package com.popsmoke.tracker.data.cloud

import com.popsmoke.tracker.data.ApiConfig
import com.popsmoke.tracker.data.AppSettings
import com.popsmoke.tracker.data.SmokeLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 一次飞书备份的结果。skipped = 远端已经有、这次没有再传的条数。 */
data class BackupReport(val uploaded: Int, val skipped: Int, val failed: Int)

/**
 * 两条云端通道的调度层，也是唯一知道"会话怎么续"的地方。
 *
 * 分工刻意按两条通道各自的强项来：
 * - Supabase = 账号 + 配置（app_settings 表）。跨设备最想同步的其实是"上限 20 支、
 *   警告 15 支、橙色主题"，这些值一丢，App 就不是自己的了。
 * - 飞书 = 记录备份。用户已经在那儿维护了一张表，原生端只负责往里写、从里读。
 *
 * 两者不互相兜底：飞书没配就不影响登录，Supabase 没配也不影响备份。
 */
class CloudService(
    private val configProvider: () -> ApiConfig,
    private val sessions: SessionStore,
    private val transport: HttpTransport = UrlConnectionTransport(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
) {

    val config: ApiConfig get() = configProvider()
    val supabaseReady: Boolean get() = config.hasSupabase
    val feishuReady: Boolean get() = config.hasFeishu

    /**
     * 当前登录用户的内存快照。
     *
     * 不用每次读文件：每次"记一支烟"都要问一次"登录了吗"，而会话文件是
     * Keystore 加密的，读一次要解密一次 —— 落盘路径上不该出现这种开销。
     * 快照在登录 / 登出 / 刷新时同步更新（见各处的 assign）。
     */
    @Volatile
    private var userSnapshot: CloudUser? = sessions.load()?.user

    fun currentUser(): CloudUser? = userSnapshot

    /** 冷启动时把会话读回来；令牌过期就顺手刷一次，刷不动就当作登出。 */
    suspend fun restore(): CloudUser? = withContext(Dispatchers.IO) {
        val session = sessions.load() ?: return@withContext null
        if (session.isFresh(clock())) return@withContext session.user
        // 配置被清空时连不上服务端，但也不该顺手把用户"登出"：他登出再登录才是主动选择。
        if (!supabaseReady) return@withContext session.user
        when (val refreshed = api().refresh(session.refreshToken)) {
            is CloudResult.Ok -> {
                sessions.save(refreshed.value)
                userSnapshot = refreshed.value.user
                refreshed.value.user
            }
            is CloudResult.Fail -> {
                sessions.clear()
                userSnapshot = null
                null
            }
        }
    }

    suspend fun signIn(email: String, password: String): CloudResult<CloudUser> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()
            when {
                cleanEmail.isBlank() || password.isBlank() ->
                    return@withContext CloudResult.Fail(CloudIssue.MissingFields)
                password.length < 6 -> return@withContext CloudResult.Fail(CloudIssue.WeakPassword)
                !supabaseReady -> return@withContext CloudResult.Fail(CloudIssue.NoSupabaseConfig)
            }
            when (val result = api().signIn(cleanEmail, password)) {
                is CloudResult.Ok -> {
                    sessions.save(result.value)
                    userSnapshot = result.value.user
                    CloudResult.Ok(result.value.user)
                }
                is CloudResult.Fail -> result
            }
        }

    suspend fun signUp(email: String, password: String): CloudResult<SignUpOutcome> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()
            when {
                cleanEmail.isBlank() || password.isBlank() ->
                    return@withContext CloudResult.Fail(CloudIssue.MissingFields)
                password.length < 6 -> return@withContext CloudResult.Fail(CloudIssue.WeakPassword)
                !supabaseReady -> return@withContext CloudResult.Fail(CloudIssue.NoSupabaseConfig)
            }
            when (val result = api().signUp(cleanEmail, password)) {
                is CloudResult.Ok -> {
                    result.value.session?.let {
                        sessions.save(it)
                        userSnapshot = it.user
                    }
                    result
                }
                is CloudResult.Fail -> result
            }
        }

    /** 登出一定会清本地会话，即使服务端请求失败 —— 用户点"登出"的意图是本机先脱离账号。 */
    suspend fun signOut(): CloudResult<Unit> = withContext(Dispatchers.IO) {
        val session = sessions.load()
        if (session != null && supabaseReady) {
            api().signOut(session.accessToken)
        }
        sessions.clear()
        userSnapshot = null
        CloudResult.Ok(Unit)
    }

    suspend fun fetchRemoteSettings(): CloudResult<RemoteSettings?> =
        authorized { api, session -> api.fetchSettings(session.accessToken, session.user.id) }

    /**
     * 拉取该账号的全部记录，自动翻页。
     *
     * 上限 [MaxLogs] 是保护而不是限制：单账号 2 万条已经远超"一天 50 支"的任何真实用法，
     * 真到那一天更该做增量同步（按 updated_at 水位）而不是继续翻全表。
     */
    suspend fun fetchCloudLogs(): CloudResult<List<SmokeLog>> = authorized { api, session ->
        val all = mutableListOf<SmokeLog>()
        var offset = 0
        while (true) {
            val page = when (val result = api.fetchLogs(session.accessToken, session.user.id, LogPageSize, offset)) {
                is CloudResult.Ok -> result.value
                is CloudResult.Fail -> return@authorized result
            }
            all += page
            if (page.size < LogPageSize) break
            offset += LogPageSize
            if (offset >= MaxLogs) break
        }
        CloudResult.Ok(all.toList())
    }

    suspend fun pushLog(log: SmokeLog): CloudResult<Unit> =
        authorized { api, session -> api.upsertLog(session.accessToken, session.user.id, log) }

    suspend fun deleteCloudLog(logId: String): CloudResult<Unit> =
        authorized { api, session -> api.deleteLog(session.accessToken, session.user.id, logId) }

    suspend fun pushSettings(settings: AppSettings): CloudResult<Unit> =
        authorized { api, session -> api.pushSettings(session.accessToken, session.user.id, settings) }

    suspend fun fetchFeishuLogs(): CloudResult<List<SmokeLog>> = withContext(Dispatchers.IO) {
        if (!feishuReady) return@withContext CloudResult.Fail(CloudIssue.NoFeishuConfig)
        FeishuApi(configProvider(), transport, zone).fetchLogs()
    }

    /**
     * 只上传远端没有的记录。
     *
     * 判断依据是"先 GET 一遍远端 id 集合"，而不是本地记一份"已备份"清单：
     * 清单会随着清数据、换设备、手动改表而失真，而远端自己的 id 永远是准的。
     * 代价是每次多一个 GET，换来的是重复上传这件事不会发生。
     */
    suspend fun backupLogs(logs: List<SmokeLog>): CloudResult<BackupReport> =
        withContext(Dispatchers.IO) {
            if (!feishuReady) return@withContext CloudResult.Fail(CloudIssue.NoFeishuConfig)
            val api = FeishuApi(configProvider(), transport, zone)
            val remote = when (val fetched = api.fetchLogs()) {
                is CloudResult.Ok -> fetched.value
                is CloudResult.Fail -> return@withContext fetched
            }
            val remoteIds = remote.map { it.id }.toSet()
            val pending = logs.filter { it.id !in remoteIds }.sortedBy { it.timestamp }

            var uploaded = 0
            var failure: CloudResult.Fail? = null
            for (log in pending) {
                when (val result = api.submit(log)) {
                    is CloudResult.Ok -> uploaded++
                    is CloudResult.Fail -> if (failure == null) failure = result
                }
            }
            val failed = pending.size - uploaded
            // 一条都没成功时把原因抛出去（多半是网络或密钥）；部分成功就报告数字，
            // 让用户看到"传了 47 条、失败 3 条"而不是一句笼统的失败。
            if (failed > 0 && uploaded == 0) {
                failure ?: CloudResult.Fail(CloudIssue.Unknown)
            } else {
                CloudResult.Ok(BackupReport(uploaded = uploaded, skipped = logs.size - pending.size, failed = failed))
            }
        }

    private suspend fun <T> authorized(
        call: (SupabaseApi, CloudSession) -> CloudResult<T>,
    ): CloudResult<T> = withContext(Dispatchers.IO) {
        if (!supabaseReady) return@withContext CloudResult.Fail(CloudIssue.NoSupabaseConfig)
        val api = api()
        var session = sessions.load() ?: return@withContext CloudResult.Fail(CloudIssue.SessionExpired)

        if (!session.isFresh(clock())) {
            session = when (val refreshed = api.refresh(session.refreshToken)) {
                is CloudResult.Ok -> refreshed.value.also { sessions.save(it) }
                is CloudResult.Fail -> {
                    sessions.clear()
                    userSnapshot = null
                    return@withContext CloudResult.Fail(CloudIssue.SessionExpired)
                }
            }
        }

        val first = call(api, session)
        if (first.issueOrNull() != CloudIssue.SessionExpired) return@withContext first

        // 令牌可能在服务端被提前吊销（改密码、别处登出）。刷新一次再试，仍失败才算过期。
        val refreshed = when (val result = api.refresh(session.refreshToken)) {
            is CloudResult.Ok -> result.value.also { sessions.save(it) }
            is CloudResult.Fail -> {
                sessions.clear()
                userSnapshot = null
                return@withContext CloudResult.Fail(CloudIssue.SessionExpired)
            }
        }
        call(api, refreshed)
    }

    private fun api(): SupabaseApi = SupabaseApi(configProvider(), transport, clock, zone)
}

/** PostgREST 单页上限，与 [SupabaseApi] 里的分页步长保持一致。 */
private const val LogPageSize = 1000
private const val MaxLogs = 20_000

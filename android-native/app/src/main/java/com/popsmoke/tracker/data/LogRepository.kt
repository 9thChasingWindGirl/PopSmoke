package com.popsmoke.tracker.data

import com.popsmoke.tracker.data.sync.SyncOp
import com.popsmoke.tracker.data.sync.SyncOpType
import com.popsmoke.tracker.data.sync.SyncQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 单一数据源：内存里持一份 StoreState，变更立刻反映到 StateFlow，落盘放到 IO 线程。
 * 页面只订阅 Flow，不持有可变状态 —— 这是原生侧和 Web 版 useState 最大的结构差异。
 *
 * 所有写操作都在这里顺手记一条操作日志：日志和分析页是同一份状态的两种视图，
 * 如果让调用方各自补日志，迟早会有某条路径漏记（Web 版就是在 App.tsx 里手动补的）。
 *
 * 同一处也负责"入队"：登录状态下每一次改动都追加一条待推送操作。
 * 放在仓库而不是页面里，理由和操作日志一样 —— 页面会漏，数据源的写路径不会。
 */
class LogRepository(
    private val store: LocalStore,
    private val queue: SyncQueue? = null,
    private val currentUser: () -> String? = { null },
) {

    private val _state = MutableStateFlow(StoreState())
    val state: StateFlow<StoreState> = _state.asStateFlow()

    /**
     * 写路径串行锁。
     *
     * 所有写操作都是「读当前状态 → 算出下一份 → 提交」，中间那段如果在两个协程里交错，
     * 后写的那份会丢掉先写的那次修改（连点两下只留下一支，就是这么来的）。
     * 这不是并发性能问题：写入本身是几十微秒的内存拷贝，排队比丢数据划算得多。
     */
    private val writeLock = Mutex()

    @Volatile
    private var loaded = false

    /**
     * 首次读盘。
     *
     * `loaded` 这个标志不只是省一次 IO：如果用户在 init 完成前就点了一次记录，
     * 那次写入会基于空状态算出「1 支」并落盘，把磁盘上的历史记录整个覆盖掉。
     * 所以写路径在拿锁后也会调一次 [ensureLoaded]，谁先到都保证"先读再改"。
     */
    suspend fun init() = writeLock.withLock {
        ensureLoaded()
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        _state.value = withContext(Dispatchers.IO) { store.load() }
        loaded = true
    }

    suspend fun record(now: Long = System.currentTimeMillis(), id: String = UUID.randomUUID().toString()) =
        writeLock.withLock {
            ensureLoaded()
            val current = _state.value
            val nextIndex = current.logs.count { it.isSameDayAs(now) } + 1
            val log = SmokeLog(id = id, timestamp = now, recordIndex = nextIndex)
            commit(
                current.copy(
                    logs = current.logs + log,
                    opLogs = current.opLogs.withOp(
                        OpLog(id = UUID.randomUUID().toString(), type = OpLogType.Create, at = now, recordIndex = nextIndex),
                    ),
                ),
            )
            enqueue(SyncOpType.Create, log)
        }

    suspend fun delete(id: String) = writeLock.withLock {
        ensureLoaded()
        val current = _state.value
        val target = current.logs.firstOrNull { it.id == id } ?: return@withLock
        commit(
            current.copy(
                logs = current.logs.filterNot { it.id == id },
                opLogs = current.opLogs.withOp(
                    OpLog(
                        id = UUID.randomUUID().toString(),
                        type = OpLogType.Delete,
                        at = System.currentTimeMillis(),
                        recordIndex = target.recordIndex,
                    ),
                ),
            ),
        )
        enqueue(SyncOpType.Delete, target)
    }

    /**
     * 改一条记录的时间。
     *
     * recordIndex 是「当天第几支」，所以跨天编辑必须重算，否则记录会顶着旧日期的编号。
     * 同一天内改动保留原编号：那只是把这一支挪到另一个时刻，它在当天的序号没变。
     */
    suspend fun updateLogTime(id: String, timestamp: Long) = writeLock.withLock {
        ensureLoaded()
        val current = _state.value
        val target = current.logs.firstOrNull { it.id == id } ?: return@withLock
        val sameDay = target.isSameDayAs(timestamp)
        val newIndex = if (sameDay) {
            target.recordIndex
        } else {
            current.logs.count { it.id != id && it.isSameDayAs(timestamp) } + 1
        }
        val updated = target.copy(timestamp = timestamp, recordIndex = newIndex)
        commit(
            current.copy(
                logs = current.logs.map { if (it.id == id) updated else it },
                opLogs = current.opLogs.withOp(
                    OpLog(
                        id = UUID.randomUUID().toString(),
                        type = OpLogType.Update,
                        at = System.currentTimeMillis(),
                        recordIndex = newIndex,
                    ),
                ),
            ),
        )
        enqueue(SyncOpType.Update, updated)
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) = writeLock.withLock {
        ensureLoaded()
        val current = _state.value
        commit(current.copy(settings = transform(current.settings)))
    }

    /**
     * 只清记录，保留设置（对应 Web 的 clearLogsOnly：主题和语言不该被一起清掉）。
     *
     * 同时把队列里这些记录的操作丢掉：清空之后如果还把旧记录推上去，
     * 用户下次同步会看到它们"自己回来了"，那比清不掉更让人困惑。
     * 云端已有的记录不动 —— 清空是本地操作，与 Web 版一致。
     */
    suspend fun clearLogs() = writeLock.withLock {
        ensureLoaded()
        val current = _state.value
        if (current.logs.isEmpty()) return@withLock
        commit(
            current.copy(
                logs = emptyList(),
                opLogs = current.opLogs.withOp(
                    OpLog(
                        id = UUID.randomUUID().toString(),
                        type = OpLogType.Clear,
                        at = System.currentTimeMillis(),
                        count = current.logs.size,
                    ),
                ),
            ),
        )
        queue?.dropForLogs(current.logs.map { it.id }.toSet())
    }

    suspend fun clearOpLogs() = writeLock.withLock {
        ensureLoaded()
        commit(_state.value.copy(opLogs = emptyList()))
    }

    /**
     * 导入备份。replace = 整体覆盖（连设置一起），否则按 id 合并（后到的覆盖同 id）。
     * 无论哪种，本地操作日志都保留 —— 它是审计痕迹，不该被一次导入抹掉。
     */
    suspend fun importState(incoming: StoreState, replace: Boolean): Int = writeLock.withLock {
        ensureLoaded()
        val current = _state.value
        val mergedLogs = if (replace) {
            incoming.logs
        } else {
            val byId = LinkedHashMap<String, SmokeLog>()
            current.logs.forEach { byId[it.id] = it }
            incoming.logs.forEach { byId[it.id] = it }
            byId.values.toList()
        }
        val addedIds = mergedLogs.map { it.id }.toSet() - current.logs.map { it.id }.toSet()
        commit(
            current.copy(
                logs = mergedLogs,
                settings = if (replace) incoming.settings else current.settings,
                opLogs = current.opLogs.withOp(
                    OpLog(
                        id = UUID.randomUUID().toString(),
                        type = OpLogType.Sync,
                        at = System.currentTimeMillis(),
                        count = addedIds.size,
                    ),
                ),
            ),
        )
        // 从备份恢复进来的记录也要有机会进云端：用户点"恢复"时想的是"数据回来"，
        // 不是"数据只回到这台手机"。已在队列里的同 id 会被合并规则吸收，不会重复。
        mergedLogs.filter { it.id in addedIds }.forEach { enqueue(SyncOpType.Create, it) }
        return addedIds.size
    }

    /**
     * 合并云端 / 飞书拉回来的记录：只补本地没有的 id，然后按天重算"当天第几支"。
     *
     * 为什么必须重算：远端的 record_index 是它那台设备当天数出来的。
     * 手机记了 3 支、平板记了 2 支，合并后那天有 5 支，若各留各的编号，
     * 列表里会出现两个 #3、缺 #4，用户会以为数据错乱。
     *
     * 与 [importState] 的区别：备份导入是用户主动选的"恢复"，序号照备份保留；
     * 这里是从另一台设备补齐，序号的唯一权威只能是合并后的本地全量。
     */
    suspend fun mergeRemoteLogs(remote: List<SmokeLog>): Int = writeLock.withLock {
        ensureLoaded()
        val current = _state.value
        val known = current.logs.map { it.id }.toSet()
        val added = remote.filter { it.id !in known }
        if (added.isEmpty()) return@withLock 0
        commit(
            current.copy(
                logs = reindexByDay(current.logs + added),
                opLogs = current.opLogs.withOp(
                    OpLog(
                        id = UUID.randomUUID().toString(),
                        type = OpLogType.Sync,
                        at = System.currentTimeMillis(),
                        count = added.size,
                    ),
                ),
            ),
        )
        return added.size
    }

    /**
     * 应用云端拉回来的全量记录，返回实际变动的条数。
     *
     * 三条规则，缺一条都会让同步变得"看起来成功但数据不对"：
     * 1. 云端有、本地没有 → 新增（另一台设备记的）
     * 2. 两边都有但时间不同 → 只有本地队列里没有该记录时才采用云端值；
     *    有 pending 说明本地改过，覆盖等于抹掉用户刚做的编辑
     * 3. [skipIds] 里的记录一律不动，理由同上，只是这一层为了少查一次队列
     */
    suspend fun applyRemote(remote: List<SmokeLog>, skipIds: Set<String> = emptySet()): Int = writeLock.withLock {
        ensureLoaded()
        val current = _state.value
        val localById = current.logs.associateBy { it.id }
        val merged = LinkedHashMap<String, SmokeLog>(current.logs.size + remote.size)
        current.logs.forEach { merged[it.id] = it }

        var changed = 0
        for (remoteLog in remote) {
            val local = localById[remoteLog.id]
            when {
                local == null -> {
                    merged[remoteLog.id] = remoteLog
                    changed++
                }
                remoteLog.id in skipIds -> Unit
                local.timestamp != remoteLog.timestamp -> {
                    merged[remoteLog.id] = local.copy(timestamp = remoteLog.timestamp)
                    changed++
                }
            }
        }
        if (changed == 0) return@withLock 0

        commit(
            current.copy(
                logs = reindexByDay(merged.values.toList()),
                opLogs = current.opLogs.withOp(
                    OpLog(
                        id = UUID.randomUUID().toString(),
                        type = OpLogType.Sync,
                        at = System.currentTimeMillis(),
                        count = changed,
                    ),
                ),
            ),
        )
        return changed
    }

    /**
     * 登录状态下把一次改动记进待推送队列；未登录就什么都不做（纯本地模式）。
     *
     * `currentUser()` 要读会话文件、走一次 Keystore 解密，绝不能留在主线程上——
     * 每记一支就卡一下的观感比少同步一次糟糕得多，所以这里显式挪到 IO。
     */
    private suspend fun enqueue(type: SyncOpType, log: SmokeLog) {
        val target = queue ?: return
        val userId = withContext(Dispatchers.IO) { currentUser() } ?: return
        target.enqueue(
            SyncOp(
                id = UUID.randomUUID().toString(),
                type = type,
                userId = userId,
                logId = log.id,
                timestamp = log.timestamp,
                recordIndex = log.recordIndex,
                enqueuedAt = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * 提交一次变更。
     *
     * 内存状态先落地（界面立刻反映这次操作），再把写文件挪到 IO。
     * **落盘失败一律吃掉**：以前这里让异常往上抛，结果是一条写盘异常会把调用方
     * 的 `viewModelScope.launch` 整个掀掉，而收集 `state` 的那个协程一旦死掉，
     * 界面就再也不跟随数据变化——表现就是"点了没反应，重开也一样"。
     * 现在失败只留一条 System 操作日志（不再触发落盘），界面继续工作。
     */
    private suspend fun commit(next: StoreState) {
        _state.value = next
        val failure = withContext(Dispatchers.IO) { runCatching { store.save(next) }.exceptionOrNull() }
        if (failure != null) {
            _state.update { current ->
                current.copy(
                    opLogs = current.opLogs.withOp(
                        OpLog(
                            id = UUID.randomUUID().toString(),
                            type = OpLogType.System,
                            at = System.currentTimeMillis(),
                        ),
                    ),
                )
            }
        }
    }
}

/** 追加一条操作日志，只保留最近 [MaxOpLogs] 条（终端卡里滚太久没人看，还会拖慢落盘）。 */
private fun List<OpLog>.withOp(op: OpLog): List<OpLog> =
    (this + op).takeLast(MaxOpLogs)

/**
 * 按自然日重排「当天第几支」。
 *
 * 只在同一时区内比较：跨时区搬数据时，一天算在哪天取决于当前设备所在时区，
 * 这与仪表盘"今日"的口径保持一致，不会出现列表和统计对不上的情况。
 */
private fun reindexByDay(logs: List<SmokeLog>, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): List<SmokeLog> {
    val byDay = logs.groupBy { log ->
        java.time.Instant.ofEpochMilli(log.timestamp).atZone(zone).toLocalDate()
    }
    val indexById = HashMap<String, Int>(logs.size)
    byDay.forEach { (_, dayLogs) ->
        dayLogs.sortedBy { it.timestamp }.forEachIndexed { index, log ->
            indexById[log.id] = index + 1
        }
    }
    return logs.map { log -> indexById[log.id]?.let { log.copy(recordIndex = it) } ?: log }
}

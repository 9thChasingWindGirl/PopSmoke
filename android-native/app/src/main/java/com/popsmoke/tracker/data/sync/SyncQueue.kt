package com.popsmoke.tracker.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class SyncOpType { Create, Update, Delete }

enum class SyncOpStatus { Pending, Failed }

/**
 * 队列里的一次"待上行"操作。
 *
 * 为什么存整份快照（timestamp / recordIndex）而不是只存 logId：
 * 处理队列时再回头去本地查记录，遇到"记录已经被删"就只能猜。快照让每个操作自包含，
 * 重放行为可预测 —— 一条 update 无论本地现在是什么样，上传的都是当时用户看到的那个值。
 */
data class SyncOp(
    val id: String,
    val type: SyncOpType,
    /** 这条操作属于哪个账号。换号登录时必须能区分，否则会把 A 的记录推给 B。 */
    val userId: String,
    val logId: String,
    val timestamp: Long,
    val recordIndex: Int,
    val enqueuedAt: Long,
    val status: SyncOpStatus = SyncOpStatus.Pending,
    val attempts: Int = 0,
    val lastError: String? = null,
)

/** 队列落盘的抽象。文件实现给真机用，内存实现给测试用。 */
interface SyncQueueStore {
    fun load(): List<SyncOp>
    fun save(ops: List<SyncOp>)
}

class FileSyncQueueStore(baseDir: File) : SyncQueueStore {

    private val file = File(baseDir, "sync-queue.json")

    override fun load(): List<SyncOp> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("ops") ?: JSONArray()
            buildList {
                for (i in 0 until array.length()) {
                    array.optJSONObject(i)?.let { parseOp(it) }?.let { add(it) }
                }
            }
        }.getOrDefault(emptyList())
    }

    override fun save(ops: List<SyncOp>) {
        file.parentFile?.mkdirs()
        val array = JSONArray()
        ops.forEach { array.put(writeOp(it)) }
        val root = JSONObject().put("version", 1).put("ops", array)
        // 先写临时文件再改名：落盘中途被杀掉时，宁可留着旧队列，也不留一个截断的 JSON。
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(root.toString())
        if (!temp.renameTo(file)) {
            file.writeText(root.toString())
            temp.delete()
        }
    }

    private fun writeOp(op: SyncOp): JSONObject = JSONObject()
        .put("id", op.id)
        .put("type", op.type.name)
        .put("userId", op.userId)
        .put("logId", op.logId)
        .put("ts", op.timestamp)
        .put("idx", op.recordIndex)
        .put("at", op.enqueuedAt)
        .put("status", op.status.name)
        .put("attempts", op.attempts)
        .apply { if (op.lastError != null) put("error", op.lastError) }

    private fun parseOp(json: JSONObject): SyncOp? {
        val id = json.optString("id").ifBlank { return null }
        val logId = json.optString("logId").ifBlank { return null }
        val type = runCatching { SyncOpType.valueOf(json.optString("type")) }.getOrNull() ?: return null
        val status = runCatching { SyncOpStatus.valueOf(json.optString("status")) }
            .getOrDefault(SyncOpStatus.Pending)
        return SyncOp(
            id = id,
            type = type,
            userId = json.optString("userId"),
            logId = logId,
            timestamp = json.optLong("ts"),
            recordIndex = json.optInt("idx"),
            enqueuedAt = json.optLong("at"),
            status = status,
            attempts = json.optInt("attempts"),
            lastError = json.optString("error").ifBlank { null },
        )
    }
}

class MemorySyncQueueStore(initial: List<SyncOp> = emptyList()) : SyncQueueStore {
    var ops: List<SyncOp> = initial
    override fun load(): List<SyncOp> = ops
    override fun save(ops: List<SyncOp>) {
        this.ops = ops
    }
}

/**
 * 上行队列。所有网络写入都先落到这里，再由 [pushPending] 一条条推。
 *
 * 为什么不直接写云端：手机经常处在"弱网 + 随手记一支"的场景，如果记一支就同步一次，
 * 用户在地铁里连点三下会看到三次失败。队列把"记录"和"上传"解耦：
 * 记录永远瞬时成功，上传什么时候成功由网络决定，失败也只是一条待重试的操作。
 *
 * 合并规则是这段代码真正的重点（见 [enqueue]）：同一条记录连续改两次时间，
 * 队列里不该积两条 update；先改后删，也不该留下"上传一条已经删掉的记录"。
 */
class SyncQueue(private val store: SyncQueueStore) {

    private val mutex = Mutex()
    private val _ops = MutableStateFlow(store.load())
    val ops: StateFlow<List<SyncOp>> = _ops.asStateFlow()

    fun pendingCount(): Int = _ops.value.size

    /**
     * 该账号本地还有未推送操作的记录 id。拉取时用这个集合避免覆盖本地的新改动。
     * 必须按账号过滤：别的账号留下的 pending 与当前账号的同名 id 毫无关系。
     */
    fun pendingLogIds(userId: String): Set<String> =
        _ops.value.filter { it.userId == userId }.map { it.logId }.toSet()

    /** 该账号待推送的操作，按入队顺序。 */
    fun opsFor(userId: String): List<SyncOp> = _ops.value.filter { it.userId == userId }

    suspend fun enqueue(op: SyncOp) = mutex.withLock {
        val current = _ops.value
        val next = when (op.type) {
            // 新增后又被修改：上传一次最终状态即可，中间态没有意义。
            SyncOpType.Create -> current.filterNot { it.logId == op.logId } + op

            // 还没上传过的新增被改了：把快照更新掉，仍然只算一次新增。
            SyncOpType.Update -> {
                val pendingCreate = current.firstOrNull {
                    it.logId == op.logId && it.type == SyncOpType.Create
                }
                if (pendingCreate != null) {
                    current.map {
                        if (it.id == pendingCreate.id) {
                            pendingCreate.copy(
                                timestamp = op.timestamp,
                                recordIndex = op.recordIndex,
                                enqueuedAt = op.enqueuedAt,
                            )
                        } else {
                            it
                        }
                    }
                } else {
                    // 同一条记录只留最近一次修改：前一次的值永远不会被谁看到。
                    current.filterNot {
                        it.logId == op.logId && it.type == SyncOpType.Update
                    } + op
                }
            }

            // 删除吸收掉这条记录所有未推送的操作：远端最终只需要"没有这条"。
            SyncOpType.Delete -> current.filterNot { it.logId == op.logId } + op
        }
        persist(next)
    }

    suspend fun remove(opId: String) = mutex.withLock {
        persist(_ops.value.filterNot { it.id == opId })
    }

    suspend fun clear() = mutex.withLock { persist(emptyList()) }

    /** 丢掉某条记录的所有待推送操作（清空本地数据时用：已经清掉的记录不该再传上去）。 */
    suspend fun dropForLogs(logIds: Set<String>) = mutex.withLock {
        if (logIds.isEmpty()) return@withLock
        persist(_ops.value.filterNot { it.logId in logIds })
    }

    suspend fun markFailed(opId: String, error: String) = mutex.withLock {
        persist(
            _ops.value.map {
                if (it.id == opId) {
                    it.copy(status = SyncOpStatus.Failed, attempts = it.attempts + 1, lastError = error)
                } else {
                    it
                }
            },
        )
    }

    /** 重试前把状态改回待同步，避免成功后界面还挂着 FAILED。 */
    suspend fun markRetrying(opId: String) = mutex.withLock {
        persist(
            _ops.value.map {
                if (it.id == opId && it.status == SyncOpStatus.Failed) {
                    it.copy(status = SyncOpStatus.Pending)
                } else {
                    it
                }
            },
        )
    }

    private fun persist(next: List<SyncOp>) {
        _ops.value = next
        store.save(next)
    }
}

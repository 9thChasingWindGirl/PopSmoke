package com.popsmoke.tracker.data.cloud

import com.popsmoke.tracker.data.LogRepository
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.data.sync.SyncOp
import com.popsmoke.tracker.data.sync.SyncOpType
import com.popsmoke.tracker.data.sync.SyncQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 一次上行推送的结果。skipped = 记录在本地已不存在、不必再传的条数。 */
data class PushReport(val pushed: Int, val failed: Int, val skipped: Int)

/** 一次完整同步的结果，喂给界面上的提示条。 */
data class SyncReport(val pushed: Int, val pulled: Int, val failed: Int, val pending: Int) {
    val changed: Int get() = pushed + pulled
}

/**
 * 记录的双向同步。
 *
 * 两个方向刻意用了不同的权威来源，这是这套设计的核心：
 *
 * - **上行**由队列决定。本地每改一次就入队一条操作，什么时候推、推几条、失败后重试
 *   都只看队列。好处是"记一支烟"永远瞬时成功，地铁里连点三下也不会弹三个错误。
 *
 * - **下行**由队列反推。拉回来的记录若与本地同 id 但时间不同，只有当这条记录
 *   **不**在队列里时才用远端覆盖 —— 队列里有 pending 就说明本地改过而云端还不知道，
 *   此时用远端覆盖等于把用户刚调的时间悄悄抹掉。换句话说，队列在这里充当了
 *   "本地有未提交修改"的唯一标记，不需要再维护一张脏标记表。
 *
 * 明确不做：远端删除的传播。`smoke_logs` 没有墓碑列，远端少了一条无法区分
 * "在另一台设备删了"和"本地新增还没上传"，直接按缺失删本地会误删新记录。
 */
class LogSyncService(
    private val queue: SyncQueue,
    private val cloud: CloudService,
    private val repository: LogRepository,
) {

    /**
     * 按队列顺序推送，直到队列空或第一条失败。
     *
     * 失败即停（而不是继续试后面几条）：网络断了之后每一条都会失败，
     * 继续跑只会把整个队列标成 FAILED，用户反而看不出是哪条先出的问题。
     */
    suspend fun pushPending(): CloudResult<PushReport> = withContext(Dispatchers.IO) {
        if (!cloud.supabaseReady) return@withContext CloudResult.Fail(CloudIssue.NoSupabaseConfig)
        val userId = cloud.currentUser()?.id
            ?: return@withContext CloudResult.Fail(CloudIssue.SessionExpired)

        var pushed = 0
        var skipped = 0
        var failure: CloudResult.Fail? = null

        while (true) {
            // 只看当前账号：换了账号登录时，上一个人的积压不该推给新的那个人。
            val op = queue.opsFor(userId).firstOrNull() ?: break
            when (val result = push(op)) {
                is CloudResult.Ok -> {
                    if (result.value) pushed++ else skipped++
                    queue.remove(op.id)
                }
                is CloudResult.Fail -> {
                    queue.markFailed(op.id, result.detail ?: result.issue.name)
                    failure = result
                    break
                }
            }
        }

        // 一条都没推成功时才把错误抛给界面；部分成功就报数字，
        // 用户看到"传了 3 条、失败 1 条"比一句"同步失败"有用得多。
        if (pushed == 0 && skipped == 0 && failure != null) {
            failure
        } else {
            CloudResult.Ok(PushReport(pushed = pushed, failed = if (failure != null) 1 else 0, skipped = skipped))
        }
    }

    /** 单独重试一条：界面上每行的"同步"按钮就是它。 */
    suspend fun pushOne(opId: String): CloudResult<Unit> = withContext(Dispatchers.IO) {
        val userId = cloud.currentUser()?.id
            ?: return@withContext CloudResult.Fail(CloudIssue.SessionExpired)
        val op = queue.ops.value.firstOrNull { it.id == opId && it.userId == userId }
            ?: return@withContext CloudResult.Ok(Unit)
        queue.markRetrying(opId)
        when (val result = push(op)) {
            is CloudResult.Ok -> {
                queue.remove(opId)
                CloudResult.Ok(Unit)
            }
            is CloudResult.Fail -> {
                queue.markFailed(opId, result.detail ?: result.issue.name)
                result
            }
        }
    }

    suspend fun pullMerge(): CloudResult<Int> = withContext(Dispatchers.IO) {
        if (!cloud.supabaseReady) return@withContext CloudResult.Fail(CloudIssue.NoSupabaseConfig)
        val userId = cloud.currentUser()?.id
            ?: return@withContext CloudResult.Fail(CloudIssue.SessionExpired)
        when (val result = cloud.fetchCloudLogs()) {
            is CloudResult.Ok -> CloudResult.Ok(repository.applyRemote(result.value, queue.pendingLogIds(userId)))
            is CloudResult.Fail -> result
        }
    }

    /** 界面上的"立即同步"：先把本地积压推上去，再把云端的新增拉下来。 */
    suspend fun syncAll(): CloudResult<SyncReport> {
        val push = when (val result = pushPending()) {
            is CloudResult.Ok -> result.value
            is CloudResult.Fail -> return result
        }
        val pulled = when (val result = pullMerge()) {
            is CloudResult.Ok -> result.value
            is CloudResult.Fail -> return result
        }
        return CloudResult.Ok(
            SyncReport(
                pushed = push.pushed,
                pulled = pulled,
                failed = push.failed,
                pending = queue.pendingCount(),
            ),
        )
    }

    /**
     * 真正发一次请求。返回 false 表示"这条不用传了"：
     * 记录已经不在本地（比如清空数据），再传上去等于把它复活。
     */
    private suspend fun push(op: SyncOp): CloudResult<Boolean> {
        if (op.type != SyncOpType.Delete) {
            val exists = repository.state.value.logs.any { it.id == op.logId }
            if (!exists) return CloudResult.Ok(false)
        }
        return when (op.type) {
            SyncOpType.Create, SyncOpType.Update -> {
                val log = SmokeLog(id = op.logId, timestamp = op.timestamp, recordIndex = op.recordIndex)
                when (val result = cloud.pushLog(log)) {
                    is CloudResult.Ok -> CloudResult.Ok(true)
                    is CloudResult.Fail -> result
                }
            }
            SyncOpType.Delete -> when (val result = cloud.deleteCloudLog(op.logId)) {
                is CloudResult.Ok -> CloudResult.Ok(true)
                is CloudResult.Fail -> result
            }
        }
    }
}

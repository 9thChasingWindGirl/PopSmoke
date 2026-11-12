package com.popsmoke.tracker

import com.popsmoke.tracker.data.ApiConfig
import com.popsmoke.tracker.data.LocalStore
import com.popsmoke.tracker.data.LogRepository
import com.popsmoke.tracker.data.cloud.CloudIssue
import com.popsmoke.tracker.data.cloud.CloudResult
import com.popsmoke.tracker.data.cloud.CloudService
import com.popsmoke.tracker.data.cloud.CloudSession
import com.popsmoke.tracker.data.cloud.CloudUser
import com.popsmoke.tracker.data.cloud.HttpRequest
import com.popsmoke.tracker.data.cloud.HttpResponse
import com.popsmoke.tracker.data.cloud.HttpTransport
import com.popsmoke.tracker.data.cloud.InMemorySessionStore
import com.popsmoke.tracker.data.cloud.LogSyncService
import com.popsmoke.tracker.data.sync.FileSyncQueueStore
import com.popsmoke.tracker.data.sync.MemorySyncQueueStore
import com.popsmoke.tracker.data.sync.SyncOp
import com.popsmoke.tracker.data.sync.SyncOpStatus
import com.popsmoke.tracker.data.sync.SyncOpType
import com.popsmoke.tracker.data.sync.SyncQueue
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.ZoneId

/**
 * 同步队列与双向同步的行为测试。
 *
 * 这一层最容易出的不是崩溃，而是"看起来同步了但数据不对"：
 * 同一条记录推两遍、远端旧值把本地刚改的时间盖掉、换账号把上一个人的记录推出去。
 * 这些在界面上只表现为"偶尔重复一条"，必须靠断言请求和队列状态才能发现。
 */
class LogSyncServiceTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val now = 1_700_000_000_000L
    private val zone: ZoneId = ZoneId.of("UTC")
    private val userId = "u-1"

    private val config = ApiConfig(
        supabaseUrl = "https://proj.supabase.co",
        supabaseAnonKey = "anon-key",
    )

    private class FakeTransport(val handler: (HttpRequest) -> HttpResponse) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override fun execute(request: HttpRequest): HttpResponse {
            requests += request
            return handler(request)
        }
    }

    private fun session() = CloudSession(
        accessToken = "at-0",
        refreshToken = "rt-0",
        expiresAt = now + 3_600_000,
        user = CloudUser(id = userId, email = "me@example.com"),
    )

    private fun cloud(transport: HttpTransport, signedIn: Boolean = true): CloudService {
        val sessions = InMemorySessionStore(if (signedIn) session() else null)
        return CloudService({ config }, sessions, transport, clock = { now }, zone = zone)
    }

    private fun repo(queue: SyncQueue, signedIn: Boolean = true): LogRepository {
        val dir = temp.newFolder()
        return LogRepository(LocalStore(dir), queue) { if (signedIn) userId else null }
    }

    private fun queue(store: MemorySyncQueueStore = MemorySyncQueueStore()) = SyncQueue(store)

    // ---- 队列合并规则 ----

    @Test
    fun unauthenticatedWrites_doNotEnterQueue() = runBlocking {
        val queue = queue()
        val repository = repo(queue, signedIn = false)
        repository.record(now, id = "log-1")

        assertTrue("未登录不该攒出永远推不出去的积压", queue.ops.value.isEmpty())
    }

    @Test
    fun createThenUpdate_collapsesToOneCreate() = runBlocking {
        val queue = queue()
        val repository = repo(queue)
        repository.record(now, id = "log-1")
        repository.updateLogTime("log-1", now + 60_000)

        val ops = queue.ops.value
        assertEquals("新增后改时间不该变成两条操作", 1, ops.size)
        assertEquals(SyncOpType.Create, ops[0].type)
        assertEquals("快照要取最新值", now + 60_000, ops[0].timestamp)
    }

    @Test
    fun repeatedUpdates_keepOnlyLatest() = runBlocking {
        val queue = queue()
        val repository = repo(queue)
        repository.record(now, id = "log-1")
        // 先把 create 推掉，后面的 update 才能独立积累
        queue.clear()
        repository.updateLogTime("log-1", now + 60_000)
        repository.updateLogTime("log-1", now + 120_000)

        val ops = queue.ops.value
        assertEquals(1, ops.size)
        assertEquals(SyncOpType.Update, ops[0].type)
        assertEquals(now + 120_000, ops[0].timestamp)
    }

    @Test
    fun delete_absorbsPendingOperations() = runBlocking {
        val queue = queue()
        val repository = repo(queue)
        repository.record(now, id = "log-1")
        repository.delete("log-1")

        val ops = queue.ops.value
        assertEquals(1, ops.size)
        assertEquals(SyncOpType.Delete, ops[0].type)
    }

    @Test
    fun queue_survivesRestart() = runBlocking {
        val dir = temp.newFolder()
        val first = SyncQueue(FileSyncQueueStore(dir))
        first.enqueue(
            SyncOp("op-1", SyncOpType.Create, userId, "log-1", now, 1, now),
        )
        first.markFailed("op-1", "boom")

        val second = SyncQueue(FileSyncQueueStore(dir))
        val restored = second.ops.value.single()
        assertEquals("log-1", restored.logId)
        assertEquals(SyncOpStatus.Failed, restored.status)
        assertEquals("boom", restored.lastError)
    }

    @Test
    fun pendingLogIds_areScopedToAccount() = runBlocking {
        val queue = queue()
        queue.enqueue(SyncOp("a", SyncOpType.Create, "u-1", "log-1", now, 1, now))
        queue.enqueue(SyncOp("b", SyncOpType.Create, "u-2", "log-2", now, 1, now))

        assertEquals(setOf("log-1"), queue.pendingLogIds("u-1"))
        assertEquals(setOf("log-2"), queue.pendingLogIds("u-2"))
    }

    // ---- 上行 ----

    @Test
    fun push_uploadsCreateWithUpsertSemantics() = runBlocking {
        val transport = FakeTransport { request ->
            assertEquals("POST", request.method)
            assertTrue(request.url, request.url.contains("/rest/v1/smoke_logs?on_conflict=id"))
            assertEquals("Bearer at-0", request.headers["Authorization"])
            assertTrue(request.headers["Prefer"].orEmpty().contains("merge-duplicates"))
            val body = JSONObject(request.body!!)
            assertEquals("log-1", body.getString("id"))
            assertEquals(userId, body.getString("user_id"))
            assertEquals("2023-11-14", body.getString("record_date"))
            HttpResponse(201, "")
        }
        val queue = queue()
        val repository = repo(queue)
        repository.record(now, id = "log-1")

        val report = LogSyncService(queue, cloud(transport), repository).pushPending()

        assertTrue(report is CloudResult.Ok)
        assertEquals(1, (report as CloudResult.Ok).value.pushed)
        assertTrue("成功的操作要出队", queue.ops.value.isEmpty())
    }

    @Test
    fun push_stopsAtFirstFailure_andKeepsBothOperations() = runBlocking {
        val transport = FakeTransport { HttpResponse(500, """{"message":"boom"}""") }
        val queue = queue()
        val repository = repo(queue)
        repository.record(now, id = "log-1")
        repository.record(now + 1000, id = "log-2")

        val result = LogSyncService(queue, cloud(transport), repository).pushPending()

        // 一条都没推成功时要把错误抛给界面，否则用户只看到队列没变、不知道是网络问题
        assertTrue(result is CloudResult.Fail)
        assertEquals("失败一条就停，不要连着刷失败", 1, transport.requests.size)
        assertEquals(2, queue.ops.value.size)
        assertEquals(SyncOpStatus.Failed, queue.ops.value[0].status)
        assertEquals(SyncOpStatus.Pending, queue.ops.value[1].status)
    }

    @Test
    fun push_deleteUsesDeclarativeUrl() = runBlocking {
        val transport = FakeTransport { request ->
            assertEquals("DELETE", request.method)
            assertTrue(request.url, request.url.contains("/rest/v1/smoke_logs?id=eq.log-1"))
            assertTrue(request.url, request.url.contains("user_id=eq.$userId"))
            HttpResponse(204, "")
        }
        val queue = queue()
        val repository = repo(queue)
        repository.record(now, id = "log-1")
        queue.clear()
        repository.delete("log-1")

        val report = LogSyncService(queue, cloud(transport), repository).pushPending()

        assertTrue(report is CloudResult.Ok)
        assertTrue(queue.ops.value.isEmpty())
    }

    @Test
    fun push_skipsOperationForLocalDeletedLog() = runBlocking {
        val transport = FakeTransport { error("本地已经没有这条记录，不该发请求") }
        val queue = queue()
        val repository = repo(queue)
        queue.enqueue(SyncOp("op-1", SyncOpType.Create, userId, "ghost", now, 1, now))

        val report = LogSyncService(queue, cloud(transport), repository).pushPending()

        assertTrue(report is CloudResult.Ok)
        assertEquals(1, (report as CloudResult.Ok).value.skipped)
        assertTrue("跳过的操作也要出队，否则会一直卡住队列", queue.ops.value.isEmpty())
    }

    @Test
    fun push_withoutSession_reportsExpiredWithoutNetwork() = runBlocking {
        val transport = FakeTransport { error("没登录不该发请求") }
        val queue = queue()
        val repository = repo(queue)

        val result = LogSyncService(queue, cloud(transport, signedIn = false), repository).pushPending()

        assertEquals(CloudIssue.SessionExpired, (result as CloudResult.Fail).issue)
        assertTrue(transport.requests.isEmpty())
    }

    // ---- 下行 ----

    @Test
    fun pull_addsRemoteOnlyRecords_andReindexesDay() = runBlocking {
        val remote = """
            [{"id":"remote-1","user_id":"u-1","timestamp":${now - 60_000},
              "record_date":"2023-11-13","record_time":"22:00","record_index":7}]
        """.trimIndent()
        val transport = FakeTransport { request ->
            assertTrue(request.url, request.url.contains("/rest/v1/smoke_logs"))
            assertTrue(request.url, request.url.contains("user_id=eq.$userId"))
            HttpResponse(200, remote)
        }
        val queue = queue()
        val repository = repo(queue)
        repository.record(now, id = "log-1")

        val pulled = LogSyncService(queue, cloud(transport), repository).pullMerge()

        assertEquals(1, (pulled as CloudResult.Ok).value)
        val logs = repository.state.value.logs
        assertEquals(2, logs.size)
        assertEquals(
            "远端自报的序号 7 要按合并后的本地全量重算",
            listOf(1, 2),
            logs.sortedBy { it.timestamp }.map { it.recordIndex },
        )
    }

    @Test
    fun pull_overwritesLocalValueWhenNothingPending() = runBlocking {
        val transport = FakeTransport {
            HttpResponse(200, """[{"id":"log-1","timestamp":${now + 500_000}}]""")
        }
        val queue = queue()
        val repository = repo(queue)
        repository.record(now, id = "log-1")
        queue.clear() // 模拟"本地这条早就推上去了"

        LogSyncService(queue, cloud(transport), repository).pullMerge()

        assertEquals(now + 500_000, repository.state.value.logs.single().timestamp)
    }

    @Test
    fun pull_keepsLocalValueWhenOperationPending() = runBlocking {
        val transport = FakeTransport {
            HttpResponse(200, """[{"id":"log-1","timestamp":${now + 500_000}}]""")
        }
        val queue = queue()
        val repository = repo(queue)
        repository.record(now, id = "log-1")
        repository.updateLogTime("log-1", now + 90_000)

        LogSyncService(queue, cloud(transport), repository).pullMerge()

        assertEquals(
            "队列里还有未推送的修改，云端旧值不能盖掉它",
            now + 90_000,
            repository.state.value.logs.single().timestamp,
        )
    }

    @Test
    fun syncAll_pushesBeforeItPulls() = runBlocking {
        val transport = FakeTransport { request ->
            when (request.method) {
                "POST" -> HttpResponse(201, "")
                else -> HttpResponse(200, "[]")
            }
        }
        val queue = queue()
        val repository = repo(queue)
        repository.record(now, id = "log-1")

        val report = LogSyncService(queue, cloud(transport), repository).syncAll()

        assertTrue(report is CloudResult.Ok)
        val methods = transport.requests.map { it.method }
        assertEquals(listOf("POST", "GET"), methods)
        assertTrue("推完之后队列应为空", queue.ops.value.isEmpty())
    }
}

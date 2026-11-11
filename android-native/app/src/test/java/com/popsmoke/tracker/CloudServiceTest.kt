package com.popsmoke.tracker

import com.popsmoke.tracker.data.ApiConfig
import com.popsmoke.tracker.data.AppSettings
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.data.cloud.CloudIssue
import com.popsmoke.tracker.data.cloud.CloudResult
import com.popsmoke.tracker.data.cloud.CloudService
import com.popsmoke.tracker.data.cloud.CloudSession
import com.popsmoke.tracker.data.cloud.CloudUser
import com.popsmoke.tracker.data.cloud.HttpRequest
import com.popsmoke.tracker.data.cloud.HttpResponse
import com.popsmoke.tracker.data.cloud.HttpTransport
import com.popsmoke.tracker.data.cloud.InMemorySessionStore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 云端调度层的行为测试。
 *
 * 这里不连真服务：换掉 [HttpTransport] 就能断言"发了什么请求、按什么顺序、带没带令牌"。
 * 云端逻辑最容易错的地方都在这种"顺序"上 —— 令牌过期忘了刷新、401 之后不重试、
 * 备份时把已经存在的记录又传一遍。这些错误在真机上只表现为"偶尔重复 / 偶尔要求重新登录"，
 * 光靠点几下是看不出来的。
 */
class CloudServiceTest {

    private val now = 1_700_000_000_000L

    private val config = ApiConfig(
        feishuUrl = "https://relay.test/api",
        supabaseUrl = "https://proj.supabase.co",
        supabaseAnonKey = "anon-key",
    )

    private class FakeTransport(val handler: (HttpRequest) -> HttpResponse) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override fun execute(request: HttpRequest): HttpResponse {
            requests += request
            return handler(request)
        }

        fun only(pathFragment: String): List<HttpRequest> = requests.filter { it.url.contains(pathFragment) }
    }

    private fun service(
        transport: HttpTransport,
        sessions: InMemorySessionStore = InMemorySessionStore(),
        configProvider: () -> ApiConfig = { config },
    ) = CloudService(configProvider, sessions, transport, clock = { now })

    private fun freshSession(token: String = "at-0", refresh: String = "rt-0") = CloudSession(
        accessToken = token,
        refreshToken = refresh,
        expiresAt = now + 3_600_000,
        user = CloudUser(id = "u-1", email = "me@example.com"),
    )

    private fun sessionJson(token: String, refresh: String) =
        """{"access_token":"$token","refresh_token":"$refresh","expires_in":3600,
           |"user":{"id":"u-1","email":"me@example.com"}}""".trimMargin()

    @Test
    fun signIn_postsCredentials_andStoresSession() = runBlocking {
        val transport = FakeTransport { request ->
            assertEquals("POST", request.method)
            assertEquals("https://proj.supabase.co/auth/v1/token?grant_type=password", request.url)
            assertEquals("anon-key", request.headers["apikey"])
            assertEquals("me@example.com", JSONObject(request.body!!).getString("email"))
            HttpResponse(200, sessionJson("at-1", "rt-1"))
        }
        val sessions = InMemorySessionStore()
        val result = service(transport, sessions).signIn("  me@example.com  ", "secret1")

        assertTrue(result is CloudResult.Ok)
        assertEquals("me@example.com", (result as CloudResult.Ok).value.email)
        assertEquals("at-1", sessions.load()?.accessToken)
    }

    @Test
    fun signIn_withWrongPassword_mapsToInvalidCredentials() = runBlocking {
        val transport = FakeTransport {
            HttpResponse(400, """{"error":"invalid_grant","error_description":"Invalid login credentials"}""")
        }
        val result = service(transport).signIn("me@example.com", "wrong-password")

        assertEquals(CloudIssue.InvalidCredentials, (result as CloudResult.Fail).issue)
    }

    @Test
    fun signIn_rejectsShortPasswordBeforeNetwork() = runBlocking {
        val transport = FakeTransport { error("不应该发出请求") }
        val result = service(transport).signIn("me@example.com", "123")

        assertEquals(CloudIssue.WeakPassword, (result as CloudResult.Fail).issue)
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun signUp_withoutSession_asksForEmailConfirmation() = runBlocking {
        val transport = FakeTransport {
            HttpResponse(200, """{"id":"u-1","email":"me@example.com","confirmation_sent_at":"2026-01-01"}""")
        }
        val sessions = InMemorySessionStore()
        val result = service(transport, sessions).signUp("me@example.com", "secret1")

        val outcome = (result as CloudResult.Ok).value
        assertNull(outcome.session)
        assertTrue(outcome.needsEmailConfirmation)
        assertNull(sessions.load())
    }

    /** 冷启动时令牌过期：必须先换票，再带着新票去读设置。 */
    @Test
    fun expiredSession_refreshesBeforeUse() = runBlocking {
        val sessions = InMemorySessionStore(freshSession(token = "old", refresh = "rt-old").copy(expiresAt = now - 1))
        val transport = FakeTransport { request ->
            when {
                request.url.contains("grant_type=refresh_token") ->
                    HttpResponse(200, sessionJson("new", "rt-new"))
                request.url.contains("/rest/v1/app_settings") -> {
                    assertEquals("Bearer new", request.headers["Authorization"])
                    HttpResponse(200, "[]")
                }
                else -> error("意外的请求 ${request.url}")
            }
        }

        val result = service(transport, sessions).fetchRemoteSettings()

        assertTrue(result is CloudResult.Ok)
        assertEquals("new", sessions.load()?.accessToken)
        assertEquals(2, transport.requests.size)
    }

    /** 令牌被服务端提前吊销：401 之后刷新并重试一次，用户不该看到"请重新登录"。 */
    @Test
    fun unauthorizedResponse_refreshesAndRetriesOnce() = runBlocking {
        val sessions = InMemorySessionStore(freshSession(token = "stale", refresh = "rt-1"))
        val transport = FakeTransport { request ->
            when {
                request.url.contains("grant_type=refresh_token") ->
                    HttpResponse(200, sessionJson("good", "rt-2"))
                request.headers["Authorization"] == "Bearer stale" ->
                    HttpResponse(401, """{"message":"JWT expired"}""")
                else -> HttpResponse(
                    200,
                    """[{"daily_limit":20,"warning_limit":15,"theme_color":"#FF6B6B","language":"zh"}]""",
                )
            }
        }

        val result = service(transport, sessions).fetchRemoteSettings()

        val remote = (result as CloudResult.Ok).value
        assertEquals(20, remote?.dailyLimit)
        assertEquals("#FF6B6B", remote?.themeColor)
        assertEquals(3, transport.requests.size)
    }

    @Test
    fun refreshFailure_dropsSession() = runBlocking {
        val sessions = InMemorySessionStore(freshSession(token = "stale", refresh = "rt-1"))
        val transport = FakeTransport { request ->
            if (request.url.contains("grant_type=refresh_token")) {
                HttpResponse(400, """{"error":"invalid_grant"}""")
            } else {
                HttpResponse(401, "{}")
            }
        }

        val result = service(transport, sessions).fetchRemoteSettings()

        assertEquals(CloudIssue.SessionExpired, (result as CloudResult.Fail).issue)
        assertNull(sessions.load())
    }

    @Test
    fun pushSettings_patchesExistingRow() = runBlocking {
        val sessions = InMemorySessionStore(freshSession())
        val transport = FakeTransport { request ->
            assertEquals("PATCH", request.method)
            assertTrue(request.url.contains("app_settings?user_id=eq.u-1"))
            assertEquals("return=representation", request.headers["Prefer"])
            HttpResponse(200, """[{"user_id":"u-1"}]""")
        }

        val result = service(transport, sessions)
            .pushSettings(AppSettings(language = "zh", themeColor = "#4ECDC4", dailyLimit = 20, warningLimit = 15))

        assertTrue(result is CloudResult.Ok)
        assertEquals(1, transport.requests.size)
        val body = JSONObject(transport.requests.single().body!!)
        assertEquals(20, body.getInt("daily_limit"))
        assertEquals("#4ECDC4", body.getString("theme_color"))
    }

    /** 表里还没有这一行时，PATCH 返回空数组，必须再 POST 一次把行建出来。 */
    @Test
    fun pushSettings_insertsWhenRowMissing() = runBlocking {
        val sessions = InMemorySessionStore(freshSession())
        val transport = FakeTransport { request ->
            when (request.method) {
                "PATCH" -> HttpResponse(200, "[]")
                "POST" -> {
                    assertEquals("https://proj.supabase.co/rest/v1/app_settings", request.url)
                    assertEquals("u-1", JSONObject(request.body!!).getString("user_id"))
                    HttpResponse(201, "")
                }
                else -> error("意外的请求 ${request.method}")
            }
        }

        val result = service(transport, sessions).pushSettings(AppSettings())

        assertTrue(result is CloudResult.Ok)
        assertEquals(listOf("PATCH", "POST"), transport.requests.map { it.method })
    }

    /** 备份的核心承诺：已经在飞书里的记录不会再写一遍。 */
    @Test
    fun backup_uploadsOnlyRecordsMissingInFeishu() = runBlocking {
        val sessions = InMemorySessionStore()
        val transport = FakeTransport { request ->
            when (request.method) {
                "GET" -> HttpResponse(
                    200,
                    """{"code":0,"msg":"ok","data":[{"id":"a","timestamp":1000,"record_index":1}]}""",
                )
                "POST" -> HttpResponse(200, """{"code":0,"msg":"ok","data":{"id":"b"}}""")
                else -> error("意外的请求 ${request.method}")
            }
        }

        val logs = listOf(
            SmokeLog(id = "a", timestamp = 1000, recordIndex = 1),
            SmokeLog(id = "b", timestamp = 2000, recordIndex = 2),
        )
        val result = service(transport, sessions).backupLogs(logs)

        val report = (result as CloudResult.Ok).value
        assertEquals(1, report.uploaded)
        assertEquals(1, report.skipped)
        assertEquals(0, report.failed)

        val posted = transport.requests.filter { it.method == "POST" }
        assertEquals(1, posted.size)
        assertEquals("b", JSONObject(posted.single().body!!).getString("id"))
        assertEquals("local", JSONObject(posted.single().body!!).getString("user_id"))
    }

    @Test
    fun feishuErrorCode_becomesBadResponse() = runBlocking {
        val transport = FakeTransport {
            HttpResponse(200, """{"code":1,"msg":"表格不存在","data":null}""")
        }

        val result = service(transport).fetchFeishuLogs()

        assertEquals(CloudIssue.BadResponse, (result as CloudResult.Fail).issue)
        assertEquals("表格不存在", (result as CloudResult.Fail).detail)
    }

    @Test
    fun missingConfig_failsFastWithoutRequests() = runBlocking {
        val transport = FakeTransport { error("不应该发出请求") }
        val empty = ApiConfig()
        val result = service(transport, configProvider = { empty }).fetchRemoteSettings()

        assertEquals(CloudIssue.NoSupabaseConfig, (result as CloudResult.Fail).issue)
        assertEquals(CloudIssue.NoFeishuConfig, (service(transport, configProvider = { empty }).fetchFeishuLogs() as CloudResult.Fail).issue)
        assertTrue(transport.requests.isEmpty())
    }
}

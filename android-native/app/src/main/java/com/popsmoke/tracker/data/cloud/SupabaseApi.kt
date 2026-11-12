package com.popsmoke.tracker.data.cloud

import com.popsmoke.tracker.data.ApiConfig
import com.popsmoke.tracker.data.AppSettings
import com.popsmoke.tracker.data.SmokeLog
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId

/** 云端 app_settings 的一行。字段名按 Supabase 表结构（snake_case）解析。 */
data class RemoteSettings(
    val dailyLimit: Int,
    val warningLimit: Int,
    val themeColor: String,
    val language: String,
) {
    fun toAppSettings(): AppSettings = AppSettings(
        language = language,
        themeColor = themeColor,
        dailyLimit = dailyLimit,
        warningLimit = warningLimit,
    )
}

fun AppSettings.toRemote(): RemoteSettings = RemoteSettings(
    dailyLimit = dailyLimit,
    warningLimit = warningLimit,
    themeColor = themeColor,
    language = language,
)

/** 注册的两种结局：直接拿到会话，或者"验证邮件已发出，还没登录"。 */
data class SignUpOutcome(val session: CloudSession?, val needsEmailConfirmation: Boolean)

/**
 * Supabase 的两个端点族：
 * - GoTrue（/auth/v1）：邮箱密码登录、注册、刷新、登出
 * - PostgREST（/rest/v1）：app_settings 表的读写
 *
 * 手写而不是引官方 SDK：官方 Kotlin SDK（gotrue-kt）会带进 Ktor 一整条依赖链，
 * 而这里只需要 5 个端点。协议本身是稳定的 REST，写死路径反而更可控。
 */
class SupabaseApi(
    private val config: ApiConfig,
    private val transport: HttpTransport,
    private val clock: () -> Long,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    private val base: String get() = config.supabaseUrl.trimEnd('/')

    private fun authHeaders(token: String?): Map<String, String> = buildMap {
        put("apikey", config.supabaseAnonKey)
        put("Accept", "application/json")
        if (token != null) put("Authorization", "Bearer $token")
    }

    fun signIn(email: String, password: String): CloudResult<CloudSession> = call {
        val body = JSONObject().put("email", email).put("password", password).toString()
        val response = transport.execute(
            HttpRequest(
                method = "POST",
                url = "$base/auth/v1/token?grant_type=password",
                headers = authHeaders(null),
                body = body,
            ),
        )
        if (response.code !in 200..299) {
            return@call CloudResult.Fail(authIssue(response.code, response.body), errorText(response.body))
        }
        parseSession(response.body)
    }

    fun signUp(email: String, password: String): CloudResult<SignUpOutcome> = call {
        val body = JSONObject().put("email", email).put("password", password).toString()
        val response = transport.execute(
            HttpRequest(
                method = "POST",
                url = "$base/auth/v1/signup",
                headers = authHeaders(null),
                body = body,
            ),
        )
        if (response.code !in 200..299) {
            return@call CloudResult.Fail(authIssue(response.code, response.body), errorText(response.body))
        }
        val json = runCatching { JSONObject(response.body) }.getOrNull()
            ?: return@call CloudResult.Fail(CloudIssue.BadResponse)
        // 项目开了"邮箱验证"时，signup 只返回 user，不带 access_token。
        val session = parseSessionOrNull(json)
        if (session == null) {
            val hasUser = !json.optJSONObject("user").isNullOrDefault() || json.has("id")
            if (!hasUser) return@call CloudResult.Fail(CloudIssue.BadResponse)
        }
        CloudResult.Ok(SignUpOutcome(session, session == null))
    }

    fun refresh(refreshToken: String): CloudResult<CloudSession> = call {
        val body = JSONObject().put("refresh_token", refreshToken).toString()
        val response = transport.execute(
            HttpRequest(
                method = "POST",
                url = "$base/auth/v1/token?grant_type=refresh_token",
                headers = authHeaders(null),
                body = body,
            ),
        )
        if (response.code !in 200..299) {
            return@call CloudResult.Fail(
                if (response.code in 400..403) CloudIssue.SessionExpired else httpIssue(response.code, response.body),
                errorText(response.body),
            )
        }
        parseSession(response.body)
    }

    fun signOut(token: String): CloudResult<Unit> = call {
        val response = transport.execute(
            HttpRequest(method = "POST", url = "$base/auth/v1/logout", headers = authHeaders(token)),
        )
        // 401 说明令牌早就失效了，对"登出"这个意图来说结果是一样的：本地清掉即可。
        if (response.code in 200..299 || response.code == 401) CloudResult.Ok(Unit)
        else CloudResult.Fail(httpIssue(response.code, response.body), errorText(response.body))
    }

    fun fetchSettings(token: String, userId: String): CloudResult<RemoteSettings?> = call {
        val response = transport.execute(
            HttpRequest(
                method = "GET",
                url = "$base/rest/v1/app_settings?select=*&user_id=eq.${encode(userId)}&limit=1",
                headers = authHeaders(token),
            ),
        )
        if (response.code == 401) return@call CloudResult.Fail(CloudIssue.SessionExpired, errorText(response.body))
        if (response.code !in 200..299) {
            return@call CloudResult.Fail(httpIssue(response.code, response.body), errorText(response.body))
        }
        val rows = runCatching { JSONArray(response.body) }.getOrNull()
            ?: return@call CloudResult.Fail(CloudIssue.BadResponse)
        if (rows.length() == 0) return@call CloudResult.Ok(null)
        CloudResult.Ok(parseSettings(rows.optJSONObject(0) ?: return@call CloudResult.Ok(null)))
    }

    /**
     * 先 PATCH 再按需 POST，与 Web 端 app_settings 的写法一致。
     *
     * 为什么不直接 POST + on_conflict=user_id：表里可能还有自增主键 `id`，
     * 冲突目标换成 user_id 时 PostgREST 会要求该列有唯一约束，配错就直接 500。
     * PATCH 是幂等的，行存在与否都不会报错。
     */
    fun pushSettings(token: String, userId: String, settings: AppSettings): CloudResult<Unit> = call {
        val payload = JSONObject()
            .put("daily_limit", settings.dailyLimit)
            .put("warning_limit", settings.warningLimit)
            .put("theme_color", settings.themeColor)
            .put("language", settings.language)

        val patch = transport.execute(
            HttpRequest(
                method = "PATCH",
                url = "$base/rest/v1/app_settings?user_id=eq.${encode(userId)}",
                headers = authHeaders(token) + mapOf("Prefer" to "return=representation"),
                body = payload.toString(),
            ),
        )
        if (patch.code == 401) return@call CloudResult.Fail(CloudIssue.SessionExpired, errorText(patch.body))
        if (patch.code !in 200..299) {
            return@call CloudResult.Fail(httpIssue(patch.code, patch.body), errorText(patch.body))
        }
        val updated = runCatching { JSONArray(patch.body) }.getOrNull()
        if (updated != null && updated.length() > 0) return@call CloudResult.Ok(Unit)

        val insertBody = JSONObject(payload.toString()).put("user_id", userId)
        val insert = transport.execute(
            HttpRequest(
                method = "POST",
                url = "$base/rest/v1/app_settings",
                headers = authHeaders(token) + mapOf("Prefer" to "return=minimal"),
                body = insertBody.toString(),
            ),
        )
        if (insert.code !in 200..299) {
            return@call CloudResult.Fail(httpIssue(insert.code, insert.body), errorText(insert.body))
        }
        CloudResult.Ok(Unit)
    }

    /**
     * 拉取该账号的记录。
     *
     * 显式带 limit/offset：PostgREST 单次最多返回 1000 行，多出来的会被静默截断。
     * 用户看到"云端 1000 条"而本地明明记了 1200 支，只会以为数据丢了 —— 这种沉默的截断
     * 比报错更难排查，所以分页由调用方循环（见 CloudService.fetchCloudLogs）。
     */
    fun fetchLogs(
        token: String,
        userId: String,
        limit: Int = PageSize,
        offset: Int = 0,
    ): CloudResult<List<SmokeLog>> = call {
        val response = transport.execute(
            HttpRequest(
                method = "GET",
                url = "$base/rest/v1/smoke_logs?select=*&user_id=eq.${encode(userId)}" +
                    "&order=timestamp.asc&limit=$limit&offset=$offset",
                headers = authHeaders(token),
            ),
        )
        if (response.code == 401) return@call CloudResult.Fail(CloudIssue.SessionExpired, errorText(response.body))
        if (response.code !in 200..299) {
            return@call CloudResult.Fail(httpIssue(response.code, response.body), errorText(response.body))
        }
        val rows = runCatching { JSONArray(response.body) }.getOrNull()
            ?: return@call CloudResult.Fail(CloudIssue.BadResponse)
        val logs = buildList {
            for (i in 0 until rows.length()) {
                rows.optJSONObject(i)?.let { parseLogRow(it, zone, "sb") }?.let { add(it) }
            }
        }
        CloudResult.Ok(logs)
    }

    /**
     * 上传一条记录。
     *
     * 用 POST + `on_conflict=id`（等价于 Web 端的 upsert），不用 insert：
     * 队列重试会把同一条记录再发一次，insert 撞主键返回 409 会把"网络抖了一下"
     * 变成"这条永远同步不上去"。
     */
    fun upsertLog(token: String, userId: String, log: SmokeLog): CloudResult<Unit> = call {
        val response = transport.execute(
            HttpRequest(
                method = "POST",
                url = "$base/rest/v1/smoke_logs?on_conflict=id",
                headers = authHeaders(token) + mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
                body = log.toRow(userId, zone).toString(),
            ),
        )
        if (response.code == 401) return@call CloudResult.Fail(CloudIssue.SessionExpired, errorText(response.body))
        if (response.code !in 200..299) {
            return@call CloudResult.Fail(httpIssue(response.code, response.body), errorText(response.body))
        }
        CloudResult.Ok(Unit)
    }

    fun deleteLog(token: String, userId: String, logId: String): CloudResult<Unit> = call {
        val response = transport.execute(
            HttpRequest(
                method = "DELETE",
                url = "$base/rest/v1/smoke_logs?id=eq.${encode(logId)}&user_id=eq.${encode(userId)}",
                headers = authHeaders(token),
            ),
        )
        if (response.code == 401) return@call CloudResult.Fail(CloudIssue.SessionExpired, errorText(response.body))
        if (response.code !in 200..299) {
            return@call CloudResult.Fail(httpIssue(response.code, response.body), errorText(response.body))
        }
        CloudResult.Ok(Unit)
    }

    private fun parseSettings(json: JSONObject): RemoteSettings? {
        // 老数据里两种命名都出现过（Web 端做过兼容），这里同样都认。
        fun pickInt(snake: String, camel: String): Int? =
            if (json.has(snake)) json.optInt(snake).takeIf { it > 0 } else json.optInt(camel).takeIf { it > 0 }

        fun pickText(snake: String, camel: String): String? =
            json.optString(snake).ifBlank { json.optString(camel) }.ifBlank { null }

        val daily = pickInt("daily_limit", "dailyLimit") ?: return null
        val warning = pickInt("warning_limit", "warningLimit") ?: daily
        return RemoteSettings(
            dailyLimit = daily,
            warningLimit = warning.coerceIn(1, daily),
            themeColor = pickText("theme_color", "themeColor") ?: "#F97316",
            language = pickText("language", "") ?: "en",
        )
    }

    private fun parseSession(body: String): CloudResult<CloudSession> {
        val json = runCatching { JSONObject(body) }.getOrNull()
            ?: return CloudResult.Fail(CloudIssue.BadResponse)
        return parseSessionOrNull(json)?.let { CloudResult.Ok(it) }
            ?: CloudResult.Fail(CloudIssue.BadResponse)
    }

    private fun parseSessionOrNull(json: JSONObject): CloudSession? {
        val token = json.optString("access_token")
        if (token.isBlank()) return null
        val user = json.optJSONObject("user") ?: json
        val id = user.optString("id")
        if (id.isBlank()) return null
        val expiresIn = json.optLong("expires_in", 3600L).coerceAtLeast(60L)
        return CloudSession(
            accessToken = token,
            refreshToken = json.optString("refresh_token"),
            expiresAt = clock() + expiresIn * 1000L,
            user = CloudUser(id = id, email = user.optString("email")),
        )
    }

    /** 网络异常统一收敛成 [CloudIssue.Network]，不让 IOException 冒到页面层。 */
    private inline fun <T> call(block: () -> CloudResult<T>): CloudResult<T> = try {
        block()
    } catch (e: java.io.IOException) {
        CloudResult.Fail(CloudIssue.Network, e.message)
    } catch (e: org.json.JSONException) {
        CloudResult.Fail(CloudIssue.BadResponse, e.message)
    }
}

/** PostgREST 的单页上限；同时是服务端默认的 max rows，取满说明可能还有下一页。 */
private const val PageSize = 1000

private fun JSONObject?.isNullOrDefault(): Boolean = this == null || this.length() == 0

/** PostgREST 的 eq 过滤值需要 URL 编码；邮箱和 uuid 里只有 `@` 与 `-`，手写足够。 */
internal fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

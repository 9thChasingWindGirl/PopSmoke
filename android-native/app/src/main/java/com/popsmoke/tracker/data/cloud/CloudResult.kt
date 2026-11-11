package com.popsmoke.tracker.data.cloud

import org.json.JSONObject

/**
 * 云端失败原因。只保留"用户能据此做出不同反应"的几种：
 * 没配置 → 去配置；密码错 → 重新输；会话过期 → 重新登录；网络 → 过会儿再试。
 * 其余一律归到 [Unknown]，避免把服务端的英文原文直接糊到界面上。
 */
enum class CloudIssue {
    NoSupabaseConfig,
    NoFeishuConfig,
    Network,
    InvalidCredentials,
    EmailNotConfirmed,
    SessionExpired,
    RateLimited,
    TableMissing,
    Server,
    BadResponse,
    MissingFields,
    WeakPassword,
    AlreadyRegistered,
    NoCloudSettings,
    Unknown,
}

sealed interface CloudResult<out T> {
    data class Ok<T>(val value: T) : CloudResult<T>
    data class Fail(val issue: CloudIssue, val detail: String? = null) : CloudResult<Nothing>
}

fun <T> CloudResult<T>.valueOrNull(): T? = (this as? CloudResult.Ok)?.value

fun CloudResult<*>.issueOrNull(): CloudIssue? = (this as? CloudResult.Fail)?.issue

/** 把 HTTP 状态码 + 响应体翻成 [CloudIssue]。 */
internal fun httpIssue(code: Int, body: String): CloudIssue = when {
    code == 401 || code == 403 -> CloudIssue.SessionExpired
    code == 404 -> CloudIssue.TableMissing
    code == 429 -> CloudIssue.RateLimited
    code in 500..599 -> CloudIssue.Server
    code in 400..499 -> CloudIssue.BadResponse
    else -> CloudIssue.Unknown
}

/** Supabase 的错误体两种形状都要认：GoTrue 用 `error/error_description`，PostgREST 用 `message`。 */
internal fun errorText(body: String): String? = runCatching {
    val json = JSONObject(body)
    json.optString("msg")
        .ifBlank { json.optString("error_description") }
        .ifBlank { json.optString("message") }
        .ifBlank { json.optString("error") }
        .ifBlank { null }
}.getOrNull()

/** GoTrue 的登录失败区分得比状态码细：401 既可能是密码错，也可能是邮箱没验证。 */
internal fun authIssue(code: Int, body: String): CloudIssue {
    val text = errorText(body)?.lowercase().orEmpty()
    return when {
        text.contains("already registered") || text.contains("already been registered") ->
            CloudIssue.AlreadyRegistered
        text.contains("email not confirmed") || text.contains("email_not_confirmed") ->
            CloudIssue.EmailNotConfirmed
        text.contains("password should be") || text.contains("weak password") ->
            CloudIssue.WeakPassword
        text.contains("invalid login") || text.contains("invalid_grant") || code == 400 && text.contains("credentials") ->
            CloudIssue.InvalidCredentials
        code == 429 -> CloudIssue.RateLimited
        code == 401 -> CloudIssue.SessionExpired
        code in 500..599 -> CloudIssue.Server
        else -> httpIssue(code, body)
    }
}

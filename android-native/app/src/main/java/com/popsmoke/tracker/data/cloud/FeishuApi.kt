package com.popsmoke.tracker.data.cloud

import com.popsmoke.tracker.data.ApiConfig
import com.popsmoke.tracker.data.SmokeLog
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId

/**
 * 飞书那边的"中继"接口。不是飞书开放平台的原生 API，而是项目自建的一个 HTTP 端点
 * 转发到多维表格（Web 版就是这么用的，见 services/api/clients/FeishuClient.ts）。
 * 所以这里只对齐三条契约：
 *
 * - `GET  {url}?user_id=local` → `{code, msg, data: [record…]}`
 * - `POST {url}` body 一条记录 → `{code, msg, data: {id}}`
 * - 成功判据是 `code` 为 0 或 200（中继脚本早期两种都返回过）
 *
 * 用户 ID 固定 `local`：原生版没有账号体系绑定在飞书上，一条烟就是一条烟，
 * 换设备靠 id 去重而不是靠用户隔离。
 */
class FeishuApi(
    private val config: ApiConfig,
    private val transport: HttpTransport,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    private fun headers(): Map<String, String> = buildMap {
        put("Accept", "application/json")
        if (config.feishuKey.isNotBlank()) put("X-Access-Key", config.feishuKey.trim())
    }

    fun fetchLogs(): CloudResult<List<SmokeLog>> {
        if (!config.hasFeishu) return CloudResult.Fail(CloudIssue.NoFeishuConfig)
        return try {
            val url = "${config.feishuUrl.trimEnd('/')}?user_id=$UserId"
            val response = transport.execute(HttpRequest("GET", url, headers()))
            if (response.code !in 200..299) {
                return CloudResult.Fail(httpIssue(response.code, response.body), errorText(response.body))
            }
            val json = JSONObject(response.body)
            if (!json.isOk()) {
                return CloudResult.Fail(CloudIssue.BadResponse, json.optString("msg"))
            }
            val array = json.opt("data").let { it as? JSONArray }
                ?: return CloudResult.Ok(emptyList())
            val logs = buildList {
                for (i in 0 until array.length()) {
                    array.optJSONObject(i)?.let { parseRecord(it) }?.let { add(it) }
                }
            }
            CloudResult.Ok(logs)
        } catch (e: java.io.IOException) {
            CloudResult.Fail(CloudIssue.Network, e.message)
        } catch (e: org.json.JSONException) {
            CloudResult.Fail(CloudIssue.BadResponse, e.message)
        }
    }

    fun submit(log: SmokeLog): CloudResult<Unit> {
        if (!config.hasFeishu) return CloudResult.Fail(CloudIssue.NoFeishuConfig)
        return try {
            val body = toJson(log).toString()
            val response = transport.execute(
                HttpRequest("POST", config.feishuUrl.trimEnd('/'), headers(), body),
            )
            if (response.code !in 200..299) {
                return CloudResult.Fail(httpIssue(response.code, response.body), errorText(response.body))
            }
            val json = runCatching { JSONObject(response.body) }.getOrNull()
                ?: return CloudResult.Fail(CloudIssue.BadResponse)
            if (!json.isOk()) return CloudResult.Fail(CloudIssue.BadResponse, json.optString("msg"))
            CloudResult.Ok(Unit)
        } catch (e: java.io.IOException) {
            CloudResult.Fail(CloudIssue.Network, e.message)
        } catch (e: org.json.JSONException) {
            CloudResult.Fail(CloudIssue.BadResponse, e.message)
        }
    }

    private fun toJson(log: SmokeLog): JSONObject = log.toRow(UserId, zone)

    /**
     * 远端记录允许缺时间戳（手填的多维表格里很常见），这时用日期 + 时间反推。
     * 反推失败就丢掉这条：宁可少恢复一条，也不能造一条"1970 年"的记录。
     */
    private fun parseRecord(json: JSONObject): SmokeLog? = parseLogRow(json, zone, "fs")

    companion object {
        const val UserId = "local"
    }
}

private fun JSONObject.isOk(): Boolean {
    val code = optInt("code", -1)
    return code == 0 || code == 200
}

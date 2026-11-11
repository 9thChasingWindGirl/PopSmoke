package com.popsmoke.tracker.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 数据文件格式的唯一实现。
 *
 * 抽出来是因为它现在有两个消费者：本地落盘（LocalStore）和备份导出/导入（设置页）。
 * 两边如果各写一份 JSON 拼装，最先出问题的就是"备份能导出但导不回来"——字段名差一个字母，
 * 用户要到换手机那天才发现。
 *
 * 版本号只用于将来做迁移，当前读的时候对缺字段一律取默认值：
 * 旧备份（没有 opLogs）必须还能导进来，这比校验严格更重要。
 */
object StoreCodec {

    const val FormatVersion = 2

    fun encode(state: StoreState, exportedAt: Long? = null): String {
        val settings = JSONObject()
            .put("language", state.settings.language)
            .put("themeColor", state.settings.themeColor)
            .put("dailyLimit", state.settings.dailyLimit)
            .put("warningLimit", state.settings.warningLimit)

        val logs = JSONArray()
        state.logs.forEach { log ->
            logs.put(
                JSONObject()
                    .put("id", log.id)
                    .put("ts", log.timestamp)
                    .put("idx", log.recordIndex),
            )
        }

        val opLogs = JSONArray()
        state.opLogs.forEach { op ->
            opLogs.put(
                JSONObject()
                    .put("id", op.id)
                    .put("type", op.type.name)
                    .put("at", op.at)
                    .apply {
                        op.recordIndex?.let { put("idx", it) }
                        op.count?.let { put("n", it) }
                    },
            )
        }

        return JSONObject()
            .put("v", FormatVersion)
            .apply { exportedAt?.let { put("exportedAt", it) } }
            .put("settings", settings)
            .put("logs", logs)
            .put("opLogs", opLogs)
            .toString()
    }

    /** 解析失败返回 null（调用方据此提示"这不是一份有效备份"），而不是静默给一个空数据。 */
    fun decode(text: String): StoreState? = runCatching {
        val root = JSONObject(text)
        val settingsJson = root.optJSONObject("settings")
        val settings = if (settingsJson == null) {
            AppSettings()
        } else {
            AppSettings(
                language = settingsJson.optString("language", "en"),
                themeColor = settingsJson.optString("themeColor", "#F97316"),
                dailyLimit = settingsJson.optInt("dailyLimit", 10),
                warningLimit = settingsJson.optInt("warningLimit", 5),
            )
        }

        val logsJson = root.optJSONArray("logs") ?: JSONArray()
        val logs = buildList {
            for (i in 0 until logsJson.length()) {
                val item = logsJson.optJSONObject(i)
                val id = item?.optString("id").orEmpty()
                if (item != null && id.isNotBlank()) {
                    add(
                        SmokeLog(
                            id = id,
                            timestamp = item.optLong("ts"),
                            recordIndex = item.optInt("idx"),
                        ),
                    )
                }
            }
        }

        val opsJson = root.optJSONArray("opLogs") ?: JSONArray()
        val ops = buildList {
            for (i in 0 until opsJson.length()) {
                val item = opsJson.optJSONObject(i)
                val type = item?.let {
                    runCatching { OpLogType.valueOf(it.optString("type")) }.getOrNull()
                }
                if (item != null && type != null) {
                    add(
                        OpLog(
                            id = item.optString("id"),
                            type = type,
                            at = item.optLong("at"),
                            recordIndex = if (item.has("idx")) item.optInt("idx") else null,
                            count = if (item.has("n")) item.optInt("n") else null,
                        ),
                    )
                }
            }
        }

        StoreState(logs = logs, settings = settings, opLogs = ops)
    }.getOrNull()
}

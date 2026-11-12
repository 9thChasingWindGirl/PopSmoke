package com.popsmoke.tracker.data

/** 一条吸烟记录。字段刻意比 Web 版窄：账号、云端表、同步状态都属于同步层，本地屏不关心。 */
data class SmokeLog(
    val id: String,
    val timestamp: Long,
    val recordIndex: Int,
)

data class AppSettings(
    val language: String = "en",
    val themeColor: String = "#F97316",
    /**
     * 视觉语言：liquid（液态玻璃，默认）/ pop（波普漫画）。
     *
     * 存 id 字符串而不是枚举：这个字段会写进备份文件和云端 app_settings，
     * 将来增删主题时，旧数据里出现未知值必须能被安静地忽略（PopThemeStyle.fromId 会退回默认），
     * 而不是让 JSON 解析整个失败、把用户的记录一起赔进去。
     */
    val themeStyle: String = "liquid",
    val dailyLimit: Int = 10,
    val warningLimit: Int = 5,
)

data class StoreState(
    val logs: List<SmokeLog> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val opLogs: List<OpLog> = emptyList(),
)

/**
 * 操作日志：只记「谁在什么时候对数据做了什么」，不存渲染好的句子。
 *
 * 为什么不存 message：Web 版在写入时拼英文句子（`Added smoke record (...)`），
 * 于是切换语言后历史日志还停在旧语言。这里存结构化字段，文案在渲染时按当前语言取。
 */
enum class OpLogType { Create, Update, Delete, Clear, Sync, System }

data class OpLog(
    val id: String,
    val type: OpLogType,
    val at: Long,
    /** 涉及的记录是当天第几支（新增/更新/删除时有值）。 */
    val recordIndex: Int? = null,
    /** 批量操作的条数（清空时有值）。 */
    val count: Int? = null,
)

const val MaxOpLogs = 100

/** 语言顺序与 Web 版顶部下拉一致：EN / 中 / 日 / 한。 */
object Languages {
    val all = listOf("en", "zh", "ja", "ko")

    fun next(current: String): String {
        val index = all.indexOf(current)
        return all[(index + 1).mod(all.size)]
    }

    fun shortLabel(code: String): String = when (code) {
        "zh" -> "中"
        "ja" -> "日"
        "ko" -> "한"
        else -> "EN"
    }
}

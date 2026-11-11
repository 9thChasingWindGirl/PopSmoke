package com.popsmoke.tracker.data

import java.io.File

/**
 * 极简本地持久化：单个 JSON 文件，写入采用「先写临时文件再改名」，
 * 避免断电/被杀进程时留下半个文件。
 *
 * 说明：这一批的首要目标是验证视觉与交互，所以没有引入 Room。
 * 等数据模型定型（同步字段、云端表结构确认）后再换成 Room，
 * 替换点只有这个类和 LogRepository 的两处调用，页面代码不受影响。
 *
 * JSON 的编解码统一交给 StoreCodec：本地文件和备份文件必须是同一套格式，
 * 否则「导出的备份导不回来」这种问题要到换手机那天才会暴露。
 */
class LocalStore(private val baseDir: File) {

    private val file = File(baseDir, FILE_NAME)
    private val tempFile = File(baseDir, "$FILE_NAME.tmp")

    fun load(): StoreState = runCatching {
        if (!file.exists()) return StoreState()
        StoreCodec.decode(file.readText(Charsets.UTF_8)) ?: StoreState()
    }.getOrElse { StoreState() }

    fun save(state: StoreState) {
        baseDir.mkdirs()
        tempFile.writeText(StoreCodec.encode(state), Charsets.UTF_8)
        if (file.exists()) file.delete()
        tempFile.renameTo(file)
    }

    private companion object {
        const val FILE_NAME = "popsmoke.json"
    }

    /** 当前数据文件大小（字节）。设置页的"存储占用"用它，不估算。 */
    fun sizeBytes(): Long = if (file.exists()) file.length() else 0L
}

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

    fun load(): StoreState = runCatching {
        if (!file.exists()) return StoreState()
        StoreCodec.decode(file.readText(Charsets.UTF_8)) ?: StoreState()
    }.getOrElse { StoreState() }

    /**
     * 保存。三处刻意为之：
     *
     * 1. **临时文件名唯一**。早先固定用 `popsmoke.json.tmp`，两个保存交错时会互相踩：
     *    A 写 tmp、B 写 tmp、A 改名成功、B 改名时 tmp 已不存在 —— 结果主文件还在，
     *    但内容是谁的说不准。唯一名字让每次保存各自独立。
     * 2. **不先删主文件**。`renameTo` 在 Android（Linux）上同分区是原子替换，会覆盖目标。
     *    原来"先 delete 再 rename"留了一个窗口：这中间进程被杀，数据就真没了。
     * 3. **rename 失败退回复制**。极少数机型/SD 挂载点上 rename 会返回 false，
     *    这时宁可多一次 IO，也不能让这次修改凭空消失。
     *
     * @Synchronized 保证同一进程内的保存串行，配合唯一临时名挡住跨协程并发。
     */
    @Synchronized
    fun save(state: StoreState) {
        baseDir.mkdirs()
        val text = StoreCodec.encode(state)
        val temp = File.createTempFile("$FILE_NAME.", ".tmp", baseDir)
        try {
            temp.writeText(text, Charsets.UTF_8)
            if (!temp.renameTo(file)) {
                temp.copyTo(file, overwrite = true)
                temp.delete()
            }
        } catch (t: Throwable) {
            runCatching { temp.delete() }
            throw t
        }
    }

    private companion object {
        const val FILE_NAME = "popsmoke.json"
    }

    /** 当前数据文件大小（字节）。设置页的"存储占用"用它，不估算。 */
    fun sizeBytes(): Long = if (file.exists()) file.length() else 0L
}

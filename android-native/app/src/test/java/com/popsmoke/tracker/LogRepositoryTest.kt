package com.popsmoke.tracker

import com.popsmoke.tracker.data.LocalStore
import com.popsmoke.tracker.data.LogRepository
import com.popsmoke.tracker.data.OpLogType
import com.popsmoke.tracker.data.SmokeLog
import com.popsmoke.tracker.data.StoreCodec
import com.popsmoke.tracker.data.StoreState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.ZoneId

/**
 * 仓库层的行为测试。
 *
 * 截图只能证明"看起来对"，改时间、记操作日志、导入合并这些是靠数据说话的部分：
 * 一旦跨天改时间算错序号，界面上只会显示一个不对的 #N，肉眼很难发现。
 */
class LogRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun repo(): LogRepository = LogRepository(LocalStore(tmp.newFolder()))

    private fun at(hour: Int, minute: Int, dayOffset: Long = 0): Long =
        LocalDate.now()
            .plusDays(dayOffset)
            .atTime(hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    @Test
    fun record_assignsDailyIndex_andWritesCreateOp() = runBlocking {
        val repo = repo()
        repo.init()

        repo.record(now = at(9, 0))
        repo.record(now = at(10, 30))

        val state = repo.state.value
        assertEquals(listOf(1, 2), state.logs.map { it.recordIndex })
        assertEquals(2, state.opLogs.count { it.type == OpLogType.Create })
    }

    @Test
    fun editingTimeWithinSameDay_keepsIndex() = runBlocking {
        val repo = repo()
        repo.init()
        repo.record(now = at(9, 0))
        repo.record(now = at(10, 0))

        val first = repo.state.value.logs.first { it.recordIndex == 1 }
        repo.updateLogTime(first.id, at(11, 45))

        val updated = repo.state.value.logs.first { it.id == first.id }
        assertEquals(1, updated.recordIndex)
        assertEquals(at(11, 45), updated.timestamp)
        assertTrue(repo.state.value.opLogs.any { it.type == OpLogType.Update })
    }

    /** 跨天改时间是唯一的"序号会变"的场景：新日期的第几支必须重算。 */
    @Test
    fun editingTimeAcrossDay_reassignsIndexInTargetDay() = runBlocking {
        val repo = repo()
        repo.init()
        repo.record(now = at(9, 0))
        repo.record(now = at(10, 0))
        repo.record(now = at(9, 0, dayOffset = -1))

        val todayFirst = repo.state.value.logs.first { it.timestamp == at(9, 0) }
        repo.updateLogTime(todayFirst.id, at(12, 0, dayOffset = -1))

        val moved = repo.state.value.logs.first { it.id == todayFirst.id }
        // 昨天已经有一支（序号 1），移过去就是第 2 支
        assertEquals(2, moved.recordIndex)
    }

    @Test
    fun deleteAndClear_writeOps() = runBlocking {
        val repo = repo()
        repo.init()
        repo.record(now = at(9, 0))
        repo.record(now = at(10, 0))

        val target = repo.state.value.logs.first()
        repo.delete(target.id)
        assertEquals(1, repo.state.value.logs.size)
        assertTrue(repo.state.value.opLogs.any { it.type == OpLogType.Delete && it.recordIndex == target.recordIndex })

        repo.clearLogs()
        assertTrue(repo.state.value.logs.isEmpty())
        assertTrue(repo.state.value.opLogs.any { it.type == OpLogType.Clear && it.count == 1 })

        repo.clearOpLogs()
        assertTrue(repo.state.value.opLogs.isEmpty())
    }

    @Test
    fun importMerge_addsOnlyMissingRecords() = runBlocking {
        val repo = repo()
        repo.init()
        repo.record(now = at(9, 0))

        val existing = repo.state.value.logs.single()
        val incoming = StoreState(
            logs = listOf(
                existing,
                SmokeLog(id = "imported-1", timestamp = at(8, 0), recordIndex = 1),
            ),
        )

        val added = repo.importState(incoming, replace = false)
        assertEquals(1, added)
        assertEquals(2, repo.state.value.logs.size)
        assertTrue(repo.state.value.opLogs.any { it.type == OpLogType.Sync })
    }

    /**
     * 从另一台设备合并回来的记录，序号必须按合并后的本地全量重排。
     * 远端带过来的 idx 只能当参考：它那台设备当天只记了 2 支，本地已经有 1 支，
     * 直接沿用就会出现两个 #1。
     */
    @Test
    fun mergeRemoteLogs_recomputesIndexWithinDay() = runBlocking {
        val repo = repo()
        repo.init()
        repo.record(now = at(9, 0))

        val added = repo.mergeRemoteLogs(
            listOf(
                SmokeLog(id = "remote-early", timestamp = at(8, 0), recordIndex = 1),
                SmokeLog(id = "remote-late", timestamp = at(10, 0), recordIndex = 2),
            ),
        )

        assertEquals(2, added)
        val ordered = repo.state.value.logs.sortedBy { it.timestamp }
        assertEquals(listOf(1, 2, 3), ordered.map { it.recordIndex })
        // 8:00 远端、9:00 本地、10:00 远端 —— 序号按时间而不是按来源排。
        assertEquals(listOf("remote-early", "remote-late"), ordered.filter { it.id.startsWith("remote-") }.map { it.id })

        // 再合一次同样的数据：已经都在本地了，不应产生任何写入。
        assertEquals(0, repo.mergeRemoteLogs(listOf(SmokeLog("remote-early", at(8, 0), 1))))
    }

    @Test
    fun storeCodec_survivesRoundTrip() {        val state = StoreState(
            logs = listOf(SmokeLog(id = "a", timestamp = 1_700_000_000_000, recordIndex = 3)),
            opLogs = listOf(
                com.popsmoke.tracker.data.OpLog(
                    id = "op",
                    type = OpLogType.Delete,
                    at = 1_700_000_100_000,
                    recordIndex = 2,
                ),
            ),
        )

        val decoded = StoreCodec.decode(StoreCodec.encode(state))
        assertNotNull(decoded)
        assertEquals(state.logs, decoded!!.logs)
        assertEquals(state.opLogs, decoded.opLogs)
    }

    /** 旧备份没有 opLogs 字段也必须能读进来 —— 用户不会因为升级就丢掉历史记录。 */
    @Test
    fun storeCodec_acceptsLegacyFileWithoutOpLogs() {
        val legacy = """{"v":1,"settings":{"language":"zh","themeColor":"#4ECDC4","dailyLimit":8,"warningLimit":4},
            |"logs":[{"id":"x","ts":1700000000000,"idx":1}]}""".trimMargin()

        val decoded = StoreCodec.decode(legacy)
        assertNotNull(decoded)
        assertEquals(1, decoded!!.logs.size)
        assertEquals("zh", decoded.settings.language)
        assertTrue(decoded.opLogs.isEmpty())
    }
}

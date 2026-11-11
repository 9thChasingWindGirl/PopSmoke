package com.popsmoke.tracker.feature.cloud

import com.popsmoke.tracker.data.cloud.CloudIssue
import com.popsmoke.tracker.data.cloud.CloudUser
import com.popsmoke.tracker.data.cloud.RemoteSettings
import com.popsmoke.tracker.data.sync.SyncOp

/** 一次云端动作结束后要告诉用户什么。文案在 UI 层按当前语言翻译，这里只存事实。 */
enum class CloudNoticeKind {
    SignedIn,
    SignedOut,
    SignUpConfirm,
    SettingsPushed,
    SettingsPulled,
    BackupDone,
    Restored,
    LogsSynced,
    Failed,
}

data class CloudNotice(
    val kind: CloudNoticeKind,
    val issue: CloudIssue? = null,
    val uploaded: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val added: Int = 0,
    /** 记录同步：下行合并的条数与推送后仍留在队列里的条数。 */
    val pulled: Int = 0,
    val pending: Int = 0,
)

/**
 * 云端相关界面状态。刻意拆成三块：
 * - 可用性（配置有没有填）→ 决定账户卡显示"去配置"还是登录表单
 * - 身份（登录没有）→ 决定显示邮箱还是输入框
 * - 一次性结果（notice / conflict）→ 提示条与冲突弹窗，消费后清空
 */
data class CloudUiState(
    val supabaseReady: Boolean = false,
    val feishuReady: Boolean = false,
    val user: CloudUser? = null,
    val busy: Boolean = false,
    val notice: CloudNotice? = null,
    /** 登录后发现云端设置与本地不同，等用户裁决。 */
    val conflict: RemoteSettings? = null,
    /** 待推送队列。未登录时为空 —— 纯本地模式不该攒出"永远推不出去"的积压。 */
    val queueOps: List<SyncOp> = emptyList(),
) {
    val signedIn: Boolean get() = user != null
    val pendingSync: Int get() = queueOps.size
    val hasFailedOp: Boolean get() = queueOps.any { it.status == com.popsmoke.tracker.data.sync.SyncOpStatus.Failed }
}

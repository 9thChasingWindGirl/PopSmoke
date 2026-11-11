package com.popsmoke.tracker.feature.cloud

import com.popsmoke.tracker.data.cloud.CloudIssue
import com.popsmoke.tracker.i18n.PopStrings

/** 生成器不处理占位符（它只保证四种语言键齐全），替换留到这里做。 */
fun String.fill(vararg args: Pair<String, Any>): String =
    args.fold(this) { acc, (key, value) -> acc.replace("{$key}", value.toString()) }

fun CloudIssue.message(strings: PopStrings): String = when (this) {
    CloudIssue.NoSupabaseConfig, CloudIssue.NoFeishuConfig -> strings.issueNoConfig
    CloudIssue.Network -> strings.issueNetwork
    CloudIssue.InvalidCredentials -> strings.issueCredentials
    CloudIssue.EmailNotConfirmed -> strings.issueEmailNotConfirmed
    CloudIssue.SessionExpired -> strings.issueSessionExpired
    CloudIssue.RateLimited -> strings.issueRateLimited
    CloudIssue.TableMissing -> strings.issueTableMissing
    CloudIssue.Server -> strings.issueServer
    CloudIssue.BadResponse -> strings.issueBadResponse
    CloudIssue.MissingFields -> strings.issueMissingFields
    CloudIssue.WeakPassword -> strings.issueWeakPassword
    CloudIssue.AlreadyRegistered -> strings.issueAlreadyRegistered
    CloudIssue.NoCloudSettings -> strings.issueNoCloudSettings
    CloudIssue.Unknown -> strings.issueUnknown
}

/**
 * 提示条文案。
 *
 * 备份结果刻意报三个数字而不是一句"成功"：用户点的是"备份"，真正关心的是
 * "这一百多条都进去了吗、有没有漏"。只说成功，等于让他下次再点一遍确认。
 */
fun CloudNotice.message(strings: PopStrings): String = when (kind) {
    CloudNoticeKind.SignedIn -> strings.signedInAs
    CloudNoticeKind.SignedOut -> strings.signOut
    CloudNoticeKind.SignUpConfirm -> strings.emailUnverified
    CloudNoticeKind.SettingsPushed, CloudNoticeKind.SettingsPulled -> strings.syncSuccess
    CloudNoticeKind.BackupDone -> buildList {
        add(strings.cloudUploaded.fill("n" to uploaded))
        if (skipped > 0) add(strings.cloudSkipped.fill("n" to skipped))
        if (failed > 0) add(strings.cloudFailedCount.fill("n" to failed))
    }.joinToString(" · ")
    CloudNoticeKind.Restored ->
        if (added == 0) strings.cloudUpToDate else strings.cloudRestoredCount.fill("n" to added)
    CloudNoticeKind.LogsSynced -> {
        val pushed = uploaded
        if (pushed == 0 && pulled == 0 && failed == 0 && pending == 0) {
            strings.syncNothing
        } else {
            buildList {
                if (pushed > 0) add(strings.cloudUploaded.fill("n" to pushed))
                if (pulled > 0) add(strings.cloudDownloaded.fill("n" to pulled))
                if (failed > 0) add(strings.cloudFailedCount.fill("n" to failed))
                if (pending > 0) add(strings.syncPendingCount.fill("n" to pending))
            }.joinToString(" · ")
        }
    }
    CloudNoticeKind.Failed -> (issue ?: CloudIssue.Unknown).message(strings)
}

val CloudNotice.isError: Boolean get() = kind == CloudNoticeKind.Failed

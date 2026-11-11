package com.popsmoke.tracker

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.popsmoke.tracker.design.PopTheme
import com.popsmoke.tracker.design.PopThemeStyle
import com.popsmoke.tracker.design.toPopColor
import com.popsmoke.tracker.feature.PopViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PopViewModel by viewModels { PopViewModel.factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 边到边：状态栏/手势条透明，图标固定深色（因为我们的顶栏和底栏都是白底）。
        // 不要依赖系统深色模式自动判定，否则在深色系统下会出现白底白图标。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )

        setContent {
            val ui by viewModel.ui.collectAsStateWithLifecycle()
            // 壁纸是独立的一条流：它比记录数据晚到（要联网），不能拖住首帧。
            val wallpaper by viewModel.wallpaperImage.collectAsStateWithLifecycle()

            PopTheme(
                accent = ui.settings.themeColor.toPopColor(),
                style = PopThemeStyle.fromId(ui.settings.themeStyle),
            ) {
                PopApp(
                    ui = ui,
                    wallpaper = wallpaper,
                    actions = PopActions(
                        onRecord = viewModel::record,
                        onDelete = viewModel::delete,
                        onUpdateLogTime = viewModel::updateLogTime,
                        onClearLogs = viewModel::clearAllLogs,
                        onClearOpLogs = viewModel::clearOpLogs,
                        onSetLanguage = viewModel::setLanguage,
                        onSetThemeColor = viewModel::setThemeColor,
                        onSetThemeStyle = viewModel::setThemeStyle,
                        onSetDailyLimit = viewModel::setDailyLimit,
                        onSetWarningLimit = viewModel::setWarningLimit,
                        onImportBackup = viewModel::importBackup,
                        onSignIn = viewModel::signIn,
                        onSignUp = viewModel::signUp,
                        onSignOut = viewModel::signOut,
                        onPushSettings = viewModel::pushSettingsToCloud,
                        onPullSettings = viewModel::pullSettingsFromCloud,
                        onResolveConflict = viewModel::resolveConflict,
                        onDismissConflict = viewModel::dismissConflict,
                        onSyncNow = viewModel::syncNow,
                        onSyncSingleOp = viewModel::syncSingleOp,
                        onRemoveQueueOp = viewModel::removeQueueOp,
                        onClearQueue = viewModel::clearQueue,
                        onBackupToFeishu = viewModel::backupToFeishu,
                        onRestoreFromFeishu = viewModel::restoreFromFeishu,
                        onDismissNotice = viewModel::dismissNotice,
                        onApiConfigSaved = viewModel::refreshCloudReadiness,
                    ),
                )
            }
        }
    }
}

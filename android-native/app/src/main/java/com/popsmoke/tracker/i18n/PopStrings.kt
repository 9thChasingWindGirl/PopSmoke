package com.popsmoke.tracker.i18n

// DO NOT EDIT —— 本文件由 tools/gen-strings.mjs 从 i18n/i18n_*.ts 生成。
// 改文案请改 web 端的 i18n 源文件，然后跑 `node tools/gen-strings.mjs`。
//
// 缺失的键在运行时回落到英文，而不是抛异常或显示空白：
// 宁可露出一句英文，也不要在界面上留一个洞。

val PopStringKeyCount = 485

private val EN_TABLE: Map<String, String> = mapOf(
    "appTitle" to "SMOKE",
    "appSubtitle" to "CONTROLLER",
    "tracker" to "TRACKER",
    "notificationSuccess" to "Success!",
    "notificationError" to "Error!",
    "notificationInfo" to "Info",
    "notificationWarning" to "Warning!",
    "analysis" to "ANALYSIS",
    "settings" to "SETTINGS",
    "history" to "HISTORY",
    "lastRecord" to "LAST RECORD",
    "smokeButton" to "SMOKE",
    "smokes" to "SMOKES",
    "clickToLog" to "Click the button below to log",
    "todayLogs" to "TODAY'S LOGS",
    "noRecords" to "No records yet",
    "recentLogs" to "RECENT LOGS",
    "last24Hours" to "The last 24 hours of activity",
    "currentGaugeLevel" to "Today's Smoking Count",
    "smokeUnit" to "Smokes",
    "consecutiveDaysWithinLimit" to "Consecutive Days Within Limit",
    "days" to "days",
    "lateNight" to "Late Night",
    "earlyMorning" to "Early Morning",
    "morning" to "Morning",
    "noon" to "Noon",
    "afternoon" to "Afternoon",
    "evening" to "Evening",
    "dailyLimit" to "Daily Limit",
    "warningLimit" to "Warning Level",
    "thresholdSettings" to "Threshold Settings",
    "themeColor" to "Theme Color",
    "language" to "Language",
    "customHex" to "Custom Hex",
    "customColor" to "Custom Color",
    "theme" to "Theme",
    "warningMsg" to "WARNING: Approaching Daily Limit!",
    "limitReachedMsg" to "DAILY LIMIT REACHED!",
    "totalCigarettes" to "Total Cigarettes",
    "exportCSV" to "Export CSV",
    "csvNote" to "Contains Date, Time, and Monthly grouping tags.",
    "noDataToExport" to "No data to export for this month.",
    "export" to "Export",
    "exportSuccess" to "Export successful",
    "exportFailed" to "Export failed",
    "weeklyFreq" to "Weekly",
    "monthlyFreq" to "Monthly",
    "thisWeek" to "This Week",
    "weekData" to "Week",
    "monthData" to "Month",
    "weekTotal" to "This Week Total",
    "mostSmokedDay" to "Most Smoked Day",
    "monthTotal" to "This Month Total",
    "lastMonthTotal" to "Last Month Total",
    "mostSmokedDate" to "Most Smoked Date",
    "day" to "th",
    "mon" to "Mon",
    "tue" to "Tue",
    "wed" to "Wed",
    "thu" to "Thu",
    "fri" to "Fri",
    "sat" to "Sat",
    "sun" to "Sun",
    "more" to "MORE / EDIT",
    "back" to "BACK",
    "next" to "NEXT",
    "prev" to "PREV",
    "delete" to "DEL",
    "edit" to "EDIT",
    "rename" to "RENAME",
    "reset" to "RESET",
    "close" to "CLOSE",
    "hide" to "HIDE",
    "save" to "SAVE",
    "cancel" to "CANCEL",
    "confirm" to "CONFIRM",
    "confirmDelete" to "Are you sure you want to delete this record?",
    "editTime" to "Edit Time",
    "never" to "Never",
    "view" to "View",
    "about" to "About",
    "blog" to "Blog",
    "github" to "GitHub",
    "stats" to "Stats",
    "data" to "Data",
    "hour" to "HOUR",
    "minute" to "MINUTE",
    "count" to "Count",
    "clearLocalData" to "Clear Local Data",
    "currentMonth" to "Current Month",
    "prevMonth" to "Prev Month",
    "nextMonth" to "Next Month",
    "confirmClearData" to "Confirm Clear Data",
    "cancelClearData" to "Cancel Clear Data",
    "clearDataWarning" to "Warning!",
    "clearDataMessage" to "Are you sure you want to clear all local data? This action is irreversible!",
    "clearData" to "Clear Data",
    "clearDataConfirm" to "This will clear all smoking logs. This action cannot be undone. Are you sure?",
    "yesClear" to "Yes, Clear",
    "loadMore" to "Load More",
    "storageError" to "Storage Error",
    "storageErrorHint" to "Please go to Settings and clear some old data to free up space.",
    "goToSettings" to "Go to Settings",
    "dismiss" to "Dismiss",
    "filtersAndSort" to "Filters & Sorting",
    "startDate" to "Start Date",
    "endDate" to "End Date",
    "minCount" to "Min Count",
    "maxCount" to "Max Count",
    "sortByDate" to "Sort by Date",
    "sortByCount" to "Sort by Count",
    "resetFilters" to "Reset Filters",
    "noMatchingRecords" to "No matching records",
    "totalRecords" to "Total Records",
    "filteredRecords" to "Filtered Records",
    "dateRange" to "Date Range",
    "averagePerDay" to "Average Per Day",
    "prevPage" to "Prev Page",
    "nextPage" to "Next Page",
    "feishuApi" to "Feishu API",
    "feishuApiSettings" to "Feishu API Settings",
    "feishuDataSync" to "Feishu Data Sync",
    "apiUrl" to "API URL",
    "syncFromFeishu" to "Sync from Feishu",
    "syncToFeishu" to "Sync to Feishu",
    "startSync" to "Start Sync",
    "syncing" to "Syncing...",
    "targetTableName" to "Target Table Name",
    "accessKey" to "Access Key",
    "accessKeyDialogTitle" to "Authentication Password",
    "accessKeyDialogMessage" to "Please enter Feishu API write access key:",
    "submitToFeishu" to "Submit to Feishu",
    "submitting" to "Submitting...",
    "startSubmit" to "Start Submit",
    "syncFromSupabaseOnly" to "Sync with Supabase",
    "syncDiffSummary" to "Sync Summary",
    "cloudOnlyRecords" to "Cloud Only Records",
    "confirmSync" to "Confirm Sync",
    "localRecords" to "Local records",
    "cloudRecords" to "Cloud records",
    "localOnly" to "Local only",
    "cloudOnly" to "Cloud only",
    "conflicts" to "Conflicts",
    "upload" to "Upload",
    "download" to "Download",
    "uploadOnly" to "Upload Only",
    "downloadOnly" to "Download Only",
    "foundNewRecords" to "Found {count} new records",
    "foundConflictingRecords" to "{count} conflicting records",
    "needUploadRecords" to "Need to upload {count} records",
    "needDownloadRecords" to "Need to download {count} records",
    "uploadCount" to "Upload {count}",
    "downloadCount" to "Download {count}",
    "syncCompleted" to "Sync with {source} completed: {message}",
    "syncWithFeishuCompleted" to "Sync with Feishu completed",
    "syncWithSupabaseCompleted" to "Sync with Supabase completed",
    "cloudSync" to "Cloud Sync",
    "cloudSyncing" to "Syncing...",
    "loginFirst" to "Please login first to sync",
    "syncSuccess" to "Sync successful",
    "syncFailed" to "Sync failed",
    "noNewRecordsToSync" to "No new records to sync",
    "uploaded" to "Uploaded",
    "downloaded" to "Downloaded",
    "records" to "records",
    "feishuSyncSuccess" to "Successfully synced from Feishu API",
    "feishuSyncToStorage" to "Successfully synced from Feishu API to localStorage",
    "feishuSyncToCloud" to "Successfully synced from Feishu API and uploaded to Supabase",
    "syncedRecordsFromFeishu" to "Synced {newCount} new records from Feishu (Total: {totalCount}, Duplicates: {duplicateCount})",
    "importedCount" to "Imported Count",
    "totalCount" to "Total Count",
    "submittedCount" to "Submitted Count",
    "failedCount" to "Failed Count",
    "signIn" to "Sign In to Supabase",
    "signUp" to "Sign Up",
    "signOut" to "Sign Out",
    "email" to "Email",
    "password" to "Password",
    "passwordPlaceholder" to "Enter your password",
    "noAccount" to "Don't have an account? Sign Up",
    "haveAccount" to "Already have an account? Sign In",
    "loading" to "Loading...",
    "initializing" to "Initializing...",
    "connecting" to "Connecting...",
    "authenticating" to "Authenticating...",
    "resetting" to "Resetting...",
    "restoring" to "Restoring...",
    "skipLogin" to "Skip login, use local mode",
    "verifyEmail" to "Registration successful! Please check your email and click the verification link.",
    "localModeHint" to "Currently using local mode. Sign in to sync data to cloud.",
    "accountManagement" to "Account Management",
    "uploadAvatar" to "Upload Avatar",
    "uploading" to "Uploading...",
    "emailPlaceholder" to "Enter your email",
    "updateEmail" to "Update Email",
    "phone" to "Phone Number",
    "phonePlaceholder" to "Enter your phone number",
    "updatePhone" to "Update Phone",
    "resetPassword" to "Reset Password",
    "currentPassword" to "Current Password",
    "newPassword" to "New Password",
    "confirmPassword" to "Confirm Password",
    "passwordMinLength" to "Password must be at least 6 characters",
    "passwordMismatch" to "Passwords do not match",
    "passwordResetSuccess" to "Password reset successful",
    "passwordResetFailed" to "Password reset failed",
    "currentPasswordIncorrect" to "Current password is incorrect",
    "signOutConfirm" to "Are you sure you want to sign out?",
    "cloudDataFound" to "Cloud Data Found",
    "cloudDataFoundMessage" to "We found {count} records in your cloud storage. Would you like to download them?",
    "downloadFromCloud" to "Download from Cloud",
    "skipDownload" to "Skip",
    "loginRequired" to "Login Required",
    "loginRequiredMessage" to "Your Supabase API is configured but you are not logged in. Please log in to access your cloud data.",
    "loginNow" to "Login Now",
    "selectDataSource" to "Select Data Source",
    "selectDataSourceMessage" to "Please select which data source to get data from:",
    "apiManagement" to "API INF",
    "api" to "API INF",
    "saveApiSettings" to "Save",
    "securityPassword" to "Security Password",
    "enterPassword" to "Enter Security Password",
    "selectSyncSource" to "Select Sync Source",
    "firstLaunchSyncMessage" to "Welcome! We detected you have cloud sync configured, but no local data yet. Please select which data source to retrieve your history from:",
    "feishu" to "Feishu",
    "supabase" to "Supabase",
    "storageMethod" to "Storage Method",
    "dataManagement" to "Data Management",
    "storageUsage" to "Storage Usage",
    "clearOldData" to "Clear Local Data",
    "systemLog" to "System Logs",
    "viewSystemLogs" to "View System Logs",
    "apiUrlDisplay" to "Saved API Settings",
    "apiConfigRequired" to "API Configuration Required",
    "deleteApiSettings" to "Delete API Settings",
    "confirmDeleteApiSettings" to "Are you sure you want to delete the saved API settings?",
    "renameApiSettings" to "Rename API Settings",
    "resetApiSettings" to "Reset API Settings",
    "enterNewPassword" to "Enter new security password for API settings:",
    "apiSettingsVerifyPassword" to "Verify security password in saved API settings to modify",
    "viewSavedApiSettings" to "View Saved API Settings",
    "previousLoginFound" to "Previous Login Found",
    "previousLoginFoundMessage" to "We found a previous login record. Do you want to restore your login status? You need to verify your security password first.",
    "restoreLogin" to "Restore Login",
    "continueLocalMode" to "Continue Local Mode",
    "enterSecurityPasswordForRestore" to "Enter your security password to restore login status",
    "clear" to "Clear",
    "today" to "Today",
    "selectDate" to "Select date",
    "selectStartDate" to "Select start date",
    "selectEndDate" to "Select end date",
    "january" to "Jan",
    "february" to "Feb",
    "march" to "Mar",
    "april" to "Apr",
    "may" to "May",
    "june" to "Jun",
    "july" to "Jul",
    "august" to "Aug",
    "september" to "Sep",
    "october" to "Oct",
    "november" to "Nov",
    "december" to "Dec",
    "sunday" to "Su",
    "monday" to "Mo",
    "tuesday" to "Tu",
    "wednesday" to "We",
    "thursday" to "Th",
    "friday" to "Fr",
    "saturday" to "Sa",
    "presetColors" to "Preset Colors",
    "fontAttribution" to "This app uses HarmonyOS Sans font, provided by Huawei Device Co., Ltd.",
    "anonKey" to "Anon Key",
    "operationLog" to "Operation Log",
    "feishuPasswordRequired" to "Feishu API settings are encrypted. Please enter password to view.",
    "apiPasswordRequired" to "API settings are encrypted. Please enter password to view.",
    "passwordError" to "Password incorrect. Please try again.",
    "dataCleared" to "Data Cleared",
    "bucketNotExist" to "Storage bucket does not exist, please contact administrator",
    "permissionDenied" to "Permission denied, please check bucket settings",
    "avatarUploadSuccess" to "Avatar uploaded successfully",
    "avatarUploadFailed" to "Avatar upload failed",
    "enterEmail" to "Please enter email address",
    "emailBindSuccess" to "Email bound successfully, please check your email for verification",
    "emailBindFailed" to "Email binding failed",
    "passwordSameAsCurrent" to "New password cannot be the same as current password",
    "passwordRequirements" to "Password requirements: at least 6 characters",
    "passwordRequirement" to "Password requirements",
    "passwordTooShort" to "Password must be at least 6 characters",
    "resetSuccess" to "Password reset successfully",
    "resetFailed" to "Failed to reset password",
    "confirmPasswordPlaceholder" to "Confirm password",
    "accountSettings" to "Account Settings",
    "continue" to "Continue",
    "signOutFailed" to "Sign out failed",
    "used" to "Used",
    "mb" to "MB",
    "chinese" to "Chinese",
    "japanese" to "Japanese",
    "korean" to "Korean",
    "noOperationRecords" to "No operation records",
    "showRecent5" to "Show recent 5",
    "showAll" to "Show all",
    "clearAll" to "Clear all",
    "createRecord" to "Create record",
    "updateRecord" to "Update record",
    "deleteRecord" to "Delete record",
    "noRecordsToday" to "No records today. Tap the button above to add one!",
    "confirmDeleteMessage" to "Are you sure you want to delete this record?",
    "editRecord" to "Edit Record",
    "editingRecord" to "Editing record",
    "left" to "left",
    "externalLinkWarningTitle" to "External Link",
    "externalLinkWarningMessage" to "You are about to visit an external website:",
    "fieldsToUpdate" to "Fields to update",
    "localDataNewerThanFeishu" to "Local data is newer than Feishu, please back up local data first",
    "localDataNewerThanCloud" to "Local data is newer than cloud, please upload local data first",
    "externalLinkContinue" to "Continue",
    "externalLinkCancel" to "Cancel",
    "somethingWentWrong" to "Something went wrong",
    "unexpectedError" to "An unexpected error occurred",
    "tryAgain" to "Try Again",
    "refreshPage" to "Refresh Page",
    "errorDetails" to "Error Details",
    "howToGetSupabaseApi" to "How to get Supabase API credentials?",
    "supabaseGuideTitle" to "Supabase Setup Guide",
    "whatIsSupabase" to "What is Supabase?",
    "supabaseDescription" to "Supabase is an open-source Firebase alternative that provides PostgreSQL database, authentication, real-time subscriptions, and more.",
    "supabaseUseCase" to "Using Supabase allows your smoking records to sync across multiple devices and be securely backed up in the cloud.",
    "supabaseRequirements" to "What you need:",
    "supabaseEmail" to "An email address (for registering a Supabase account)",
    "supabaseTime" to "About 5-10 minutes to complete configuration",
    "supabaseStep1" to "Step 1: Register Supabase Account",
    "supabaseStep1Desc1" to "Visit Supabase website:",
    "supabaseStep1Desc2" to "Click \"Start your project\" or \"Sign In\" in the top right corner",
    "supabaseStep1Desc3" to "Register/login with GitHub account or email",
    "supabaseTip" to "💡 Tip: Supabase offers a free plan that is sufficient for personal use.",
    "supabaseStep2" to "Step 2: Create New Project",
    "supabaseStep2Desc1" to "After logging in, click the \"New project\" button",
    "supabaseStep2Desc2" to "Select or create an organization",
    "supabaseStep2Desc3" to "Fill in project information:",
    "supabaseStep2Desc4" to "Name: Project name (e.g., popsmoke-tracker)",
    "supabaseStep2Desc5" to "Database Password: Set a database password (remember it)",
    "supabaseStep2Desc6" to "Region: Choose the server region closest to you",
    "supabaseStep2Desc7" to "Click \"Create new project\" to create the project",
    "supabaseNote" to "⏳ Note: Project creation takes 1-2 minutes, please be patient.",
    "supabaseStep3" to "Step 3: Get API Credentials",
    "supabaseStep3Desc1" to "After project creation, go to the project Dashboard",
    "supabaseStep3Desc2" to "Click \"Project Settings\" in the left menu",
    "supabaseStep3Desc3" to "Select the \"API\" tab",
    "supabaseStep3Desc4" to "Find the following information in the \"Project API keys\" section:",
    "supabaseProjectUrl" to "Project URL:",
    "supabaseAnonKey" to "anon public:",
    "supabaseStep4" to "Step 4: Configure in This App",
    "supabaseStep4Desc1" to "Copy \"Project URL\" to the <strong>SUPABASE_URL</strong> field in this app",
    "supabaseStep4Desc2" to "Copy \"anon public\" key to the <strong>SUPABASE_ANON_KEY</strong> field in this app",
    "supabaseStep4Desc3" to "Click the save button to complete configuration",
    "supabaseAfterConfig" to "✅ After configuration, you can:",
    "supabaseSyncDevices" to "Sync smoking records across different devices",
    "supabaseCloudBackup" to "Cloud backup to prevent data loss",
    "supabaseCloudSync" to "Use cloud sync to download/upload data",
    "supabasePrev" to "Previous",
    "supabaseNext" to "Next",
    "supabaseComplete" to "Complete",
    "newVersionAvailable" to "New Version Available",
    "releaseNotes" to "What's New",
    "updateNow" to "Update Now",
    "later" to "Later",
    "noReleaseNotes" to "No release notes available",
    "updateHint" to "Updating to the latest version ensures you have the newest features and bug fixes.",
    "recording" to "Recording...",
    "operationFrequent" to "Too frequent, please try again later",
    "generalSettings" to "General Settings",
    "storageInfo" to "Storage Information",
    "checkStorage" to "Check Storage",
    "changeAvatar" to "Change Avatar",
    "bindEmail" to "Bind Email",
    "confirmSignOut" to "Confirm Sign Out",
    "signOutWarning" to "Are you sure you want to sign out? Your data will be saved locally.",
    "externalLinkWarning" to "You are leaving this app to visit an external website. Continue?",
    "statistics" to "Statistics",
    "thisMonth" to "This Month",
    "maxDayRecord" to "Max Day Record",
    "chart" to "Chart",
    "weekView" to "Week View",
    "monthView" to "Month View",
    "noData" to "No data available",
    "sync" to "Synchronization",
    "syncDescription" to "Sync your data with cloud services for backup and multi-device access.",
    "noApiConfig" to "No API configured. Please set up API in Settings first.",
    "operationLogs" to "Operation Logs",
    "clearLogs" to "Clear Logs",
    "conflicting" to "Conflicting",
    "syncResult" to "Sync Result",
    "comingSoon" to "This screen arrives in a later batch",
    "cdThemeColor" to "Change theme color",
    "cdLanguage" to "Change language",
    "cdRecentLogs" to "View today's logs",
    "cdBackToDashboard" to "Back to dashboard",
    "cdDeleteRecord" to "Delete record",
    "cdRecordNow" to "Log a smoke now",
    "cdExpandMonth" to "Expand month",
    "cdCollapseMonth" to "Collapse month",
    "cdOpenDatePicker" to "Open date picker",
    "cdDecrease" to "Decrease",
    "cdIncrease" to "Increase",
    "cdEditRecord" to "Edit record time",
    "opCreate" to "Record added",
    "opUpdate" to "Time updated",
    "opDelete" to "Record deleted",
    "opClear" to "Records cleared",
    "opSync" to "Backup imported",
    "opSystem" to "System",
    "opPause" to "PAUSE",
    "opResume" to "RESUME",
    "opClearLog" to "CLEAR",
    "opEntries" to "entries",
    "opLive" to "LIVE",
    "opPaused" to "PAUSED",
    "opEmpty" to "No operation records",
    "backupSection" to "Backup & Restore",
    "backupExport" to "Export backup (JSON)",
    "backupImport" to "Import backup",
    "backupNote" to "A JSON file with every record and setting. Import it after switching or reinstalling.",
    "backupExported" to "Exported",
    "backupInvalid" to "This file is not a valid backup",
    "importModeTitle" to "Import mode",
    "importModeMessage" to "Merge keeps current records and adds the missing ones. Replace swaps everything, settings included.",
    "importMerge" to "Merge",
    "importReplace" to "Replace all",
    "backupImported" to "Imported",
    "cloudAccount" to "Cloud account",
    "cloudAccountHint" to "Sign in with Supabase to sync limits and theme across devices.",
    "cloudNotConfigured" to "Supabase is not configured yet. Add the URL and anon key on the API page.",
    "cloudSettingsUp" to "Upload settings",
    "cloudSettingsDown" to "Restore settings",
    "cloudConflictTitle" to "Settings differ",
    "cloudConflictMessage" to "Cloud settings differ from this device. Which one should win?",
    "cloudUseCloud" to "Use cloud",
    "cloudKeepLocal" to "Keep local",
    "feishuBackupNow" to "Back up to Feishu",
    "feishuRestoreNow" to "Restore from Feishu",
    "feishuSyncHint" to "Only records missing from the sheet are uploaded; duplicates are skipped by id.",
    "loginAction" to "Sign in",
    "emailUnverified" to "Confirmation email sent. Verify it, then sign in.",
    "cloudUploaded" to "{n} uploaded",
    "cloudSkipped" to "{n} already there",
    "cloudFailedCount" to "{n} failed",
    "cloudRestoredCount" to "{n} new records",
    "cloudUpToDate" to "Already up to date",
    "cloudDownloaded" to "{n} downloaded",
    "syncNothing" to "Already up to date",
    "syncPendingCount" to "{n} pending",
    "syncNow" to "Sync Now",
    "syncQueue" to "Sync Queue",
    "syncQueueEmpty" to "No operations in queue",
    "syncAll" to "Sync All",
    "syncOne" to "Sync",
    "syncRemove" to "Remove",
    "syncClearQueue" to "Clear",
    "syncEntries" to "entries",
    "syncPendingState" to "PENDING",
    "syncFailedState" to "FAILED",
    "syncProcessing" to "PROCESSING",
    "syncReady" to "READY",
    "syncPending" to "Pending",
    "syncClearConfirm" to "Clear the whole queue?",
    "syncHasFailedHint" to "Some changes failed to sync — retry from the queue below",
    "syncReadyHint" to "All local changes are synced",
    "syncRemoveConfirm" to "Remove this operation from the queue?",
    "signedInAs" to "Signed in",
    "issueNetwork" to "Network unavailable. Check your connection.",
    "issueCredentials" to "Wrong email or password.",
    "issueEmailNotConfirmed" to "Email not confirmed yet.",
    "issueSessionExpired" to "Session expired. Sign in again.",
    "issueServer" to "Server error. Try again later.",
    "issueTableMissing" to "Table not found. Check the Supabase schema.",
    "issueBadResponse" to "Unexpected response from the server.",
    "issueNoConfig" to "API is not configured.",
    "issueWeakPassword" to "Password needs at least 6 characters.",
    "issueAlreadyRegistered" to "This email is already registered.",
    "issueMissingFields" to "Fill in both email and password.",
    "issueNoCloudSettings" to "No settings saved in the cloud yet.",
    "issueRateLimited" to "Too many attempts. Wait a moment.",
    "issueUnknown" to "Something went wrong.",
)

private val ZH_TABLE: Map<String, String> = mapOf(
    "appTitle" to "吸烟",
    "appSubtitle" to "记录",
    "tracker" to "记录",
    "notificationSuccess" to "成功！",
    "notificationError" to "错误！",
    "notificationInfo" to "信息",
    "notificationWarning" to "警告！",
    "analysis" to "分析",
    "settings" to "设置",
    "history" to "历史记录",
    "lastRecord" to "上次记录",
    "smokeButton" to "吸烟",
    "smokes" to "根",
    "clickToLog" to "点击记录",
    "todayLogs" to "今日记录",
    "noRecords" to "今日暂无记录",
    "recentLogs" to "最近记录",
    "last24Hours" to "过去24小时的活动",
    "currentGaugeLevel" to "今日吸烟数量",
    "smokeUnit" to "支",
    "consecutiveDaysWithinLimit" to "连续达标",
    "days" to "天",
    "lateNight" to "深夜",
    "earlyMorning" to "清晨",
    "morning" to "上午",
    "noon" to "中午",
    "afternoon" to "下午",
    "evening" to "晚上",
    "dailyLimit" to "每日上限",
    "warningLimit" to "提醒阈值",
    "thresholdSettings" to "阈值设置",
    "themeColor" to "主题颜色",
    "language" to "语言 / Language",
    "customHex" to "自定义颜色",
    "customColor" to "自定义颜色",
    "theme" to "主题",
    "warningMsg" to "警告：接近每日上限！",
    "limitReachedMsg" to "已达每日上限！",
    "totalCigarettes" to "吸烟总数",
    "exportCSV" to "导出 CSV",
    "csvNote" to "包含日期、时间及月度标签。",
    "noDataToExport" to "当前月份没有数据可导出。",
    "export" to "导出",
    "exportSuccess" to "导出成功",
    "exportFailed" to "导出失败",
    "weeklyFreq" to "每周",
    "monthlyFreq" to "每月",
    "thisWeek" to "本周",
    "weekData" to "周数据",
    "monthData" to "月数据",
    "weekTotal" to "本周总数",
    "mostSmokedDay" to "吸烟最多",
    "monthTotal" to "本月总数",
    "lastMonthTotal" to "上月总数",
    "mostSmokedDate" to "吸烟最多",
    "day" to "日",
    "mon" to "周一",
    "tue" to "周二",
    "wed" to "周三",
    "thu" to "周四",
    "fri" to "周五",
    "sat" to "周六",
    "sun" to "周日",
    "more" to "更多 / 编辑",
    "back" to "返回",
    "next" to "下一页",
    "prev" to "上一页",
    "delete" to "删除",
    "edit" to "编辑",
    "rename" to "重命名",
    "reset" to "重置",
    "close" to "关闭",
    "hide" to "隐藏",
    "save" to "保存",
    "cancel" to "取消",
    "confirm" to "确认",
    "confirmDelete" to "确定要删除这条记录吗？",
    "editTime" to "修改时间",
    "never" to "从未",
    "view" to "视图",
    "about" to "关于",
    "blog" to "博客",
    "github" to "GitHub",
    "stats" to "统计",
    "data" to "数据",
    "hour" to "时",
    "minute" to "分",
    "count" to "数量",
    "clearLocalData" to "清除本地数据",
    "currentMonth" to "当月",
    "prevMonth" to "上月",
    "nextMonth" to "下月",
    "confirmClearData" to "确认清除",
    "cancelClearData" to "取消",
    "clearDataWarning" to "警告！",
    "clearDataMessage" to "确定要清除所有本地数据吗？此操作不可恢复！",
    "clearData" to "清除数据",
    "clearDataConfirm" to "这将清除所有吸烟记录。此操作无法撤销。您确定吗？",
    "yesClear" to "确认清除",
    "loadMore" to "加载更多",
    "storageError" to "存储错误",
    "storageErrorHint" to "请前往设置清除一些旧数据以释放空间。",
    "goToSettings" to "前往设置",
    "dismiss" to "关闭",
    "filtersAndSort" to "筛选和排序",
    "startDate" to "开始日期",
    "endDate" to "结束日期",
    "minCount" to "最小数量",
    "maxCount" to "最大数量",
    "sortByDate" to "按日期排序",
    "sortByCount" to "按数量排序",
    "resetFilters" to "重置筛选",
    "noMatchingRecords" to "没有匹配的记录",
    "totalRecords" to "总记录数",
    "filteredRecords" to "筛选后记录数",
    "dateRange" to "日期范围",
    "averagePerDay" to "平均每天",
    "prevPage" to "上一页",
    "nextPage" to "下一页",
    "feishuApi" to "飞书 API",
    "feishuApiSettings" to "飞书API设置",
    "feishuDataSync" to "飞书数据同步",
    "apiUrl" to "API地址",
    "syncFromFeishu" to "从飞书同步到本地",
    "syncToFeishu" to "同步到飞书",
    "startSync" to "开始同步",
    "syncing" to "同步中...",
    "targetTableName" to "目标表名",
    "accessKey" to "访问密钥",
    "accessKeyDialogTitle" to "认证密码",
    "accessKeyDialogMessage" to "请输入飞书API写入访问密钥:",
    "submitToFeishu" to "提交到飞书",
    "submitting" to "提交中...",
    "startSubmit" to "开始提交",
    "syncFromSupabaseOnly" to "与Supabase同步",
    "syncDiffSummary" to "同步摘要",
    "cloudOnlyRecords" to "仅云端记录",
    "confirmSync" to "确认同步",
    "localRecords" to "本地记录",
    "cloudRecords" to "云端记录",
    "localOnly" to "仅本地",
    "cloudOnly" to "仅云端",
    "conflicts" to "冲突",
    "upload" to "上传",
    "download" to "下载",
    "uploadOnly" to "仅上传",
    "downloadOnly" to "仅下载",
    "foundNewRecords" to "发现 {count} 条新记录",
    "foundConflictingRecords" to "{count} 条冲突记录",
    "needUploadRecords" to "需要上传 {count} 条记录",
    "needDownloadRecords" to "需要下载 {count} 条记录",
    "uploadCount" to "上传 {count} 条",
    "downloadCount" to "下载 {count} 条",
    "syncCompleted" to "与{source}同步完成：{message}",
    "syncWithFeishuCompleted" to "与飞书同步完成",
    "syncWithSupabaseCompleted" to "与Supabase同步完成",
    "cloudSync" to "云同步",
    "cloudSyncing" to "同步中...",
    "loginFirst" to "请先登录后再进行云同步",
    "syncSuccess" to "同步成功",
    "syncFailed" to "同步失败",
    "noNewRecordsToSync" to "没有新的记录需要同步",
    "uploaded" to "上传",
    "downloaded" to "下载",
    "records" to "条",
    "feishuSyncSuccess" to "飞书API同步成功",
    "feishuSyncToStorage" to "飞书API同步到本地存储成功",
    "feishuSyncToCloud" to "飞书API同步并上传到Supabase成功",
    "syncedRecordsFromFeishu" to "从飞书同步了 {newCount} 条新记录（总计：{totalCount} 条，重复：{duplicateCount} 条）",
    "importedCount" to "导入数量",
    "totalCount" to "总数量",
    "submittedCount" to "成功提交",
    "failedCount" to "提交失败",
    "signIn" to "登录到Supabase",
    "signUp" to "注册",
    "signOut" to "登出",
    "email" to "邮箱",
    "password" to "密码",
    "passwordPlaceholder" to "请输入密码",
    "noAccount" to "没有账号？立即注册",
    "haveAccount" to "已有账号？立即登录",
    "loading" to "加载中...",
    "initializing" to "初始化中...",
    "connecting" to "连接中...",
    "authenticating" to "认证中...",
    "resetting" to "重置中...",
    "restoring" to "恢复中...",
    "skipLogin" to "跳过登录，使用本地模式",
    "verifyEmail" to "注册成功！请查收验证邮件并点击链接完成验证。",
    "localModeHint" to "当前使用本地模式，登录后可同步数据到云端",
    "accountManagement" to "账户管理",
    "uploadAvatar" to "上传头像",
    "uploading" to "上传中...",
    "emailPlaceholder" to "请输入邮箱",
    "updateEmail" to "更新邮箱",
    "phone" to "手机号码",
    "phonePlaceholder" to "请输入手机号码",
    "updatePhone" to "更新手机",
    "resetPassword" to "重置密码",
    "currentPassword" to "当前密码",
    "newPassword" to "新密码",
    "confirmPassword" to "确认密码",
    "passwordMinLength" to "密码长度至少为6位",
    "passwordMismatch" to "两次输入的密码不一致",
    "passwordResetSuccess" to "密码重置成功",
    "passwordResetFailed" to "密码重置失败",
    "currentPasswordIncorrect" to "当前密码不正确",
    "signOutConfirm" to "确定要登出吗？",
    "cloudDataFound" to "发现云端数据",
    "cloudDataFoundMessage" to "我们在您的云端存储中发现了 {count} 条记录。您是否要下载它们？",
    "downloadFromCloud" to "从云端下载",
    "skipDownload" to "跳过",
    "loginRequired" to "需要登录",
    "loginRequiredMessage" to "您的 Supabase API 已配置但尚未登录。请登录以访问您的云端数据。",
    "loginNow" to "立即登录",
    "selectDataSource" to "选择数据源",
    "selectDataSourceMessage" to "请选择要从哪个数据源获取数据：",
    "apiManagement" to "API管理",
    "api" to "API",
    "saveApiSettings" to "保存",
    "securityPassword" to "安全密码",
    "enterPassword" to "请输入安全密码",
    "selectSyncSource" to "选择同步源",
    "firstLaunchSyncMessage" to "欢迎使用！检测到您已配置云端同步，但本地暂无数据。请选择从哪个数据源获取您的历史记录：",
    "feishu" to "飞书",
    "supabase" to "Supabase",
    "storageMethod" to "存储方式",
    "dataManagement" to "数据管理",
    "storageUsage" to "存储占用",
    "clearOldData" to "清理本地数据",
    "systemLog" to "系统日志",
    "viewSystemLogs" to "查看系统日志",
    "apiUrlDisplay" to "已保存的API设置",
    "apiConfigRequired" to "需要API配置",
    "deleteApiSettings" to "删除API设置",
    "confirmDeleteApiSettings" to "确定要删除已保存的API设置吗？",
    "renameApiSettings" to "重命名API设置",
    "resetApiSettings" to "重置API设置",
    "enterNewPassword" to "请输入新的API设置安全密码：",
    "apiSettingsVerifyPassword" to "在已保存的API设置中验证安全密码进行修改",
    "viewSavedApiSettings" to "查看已保存的API设置",
    "previousLoginFound" to "发现之前的登录记录",
    "previousLoginFoundMessage" to "我们发现了之前的登录记录。您是否需要恢复登录状态？需要先验证安全密码。",
    "restoreLogin" to "恢复登录",
    "continueLocalMode" to "继续本地模式",
    "enterSecurityPasswordForRestore" to "请输入安全密码以恢复登录状态",
    "clear" to "清除",
    "today" to "今天",
    "selectDate" to "选择日期",
    "selectStartDate" to "选择开始日期",
    "selectEndDate" to "选择结束日期",
    "january" to "1月",
    "february" to "2月",
    "march" to "3月",
    "april" to "4月",
    "may" to "5月",
    "june" to "6月",
    "july" to "7月",
    "august" to "8月",
    "september" to "9月",
    "october" to "10月",
    "november" to "11月",
    "december" to "12月",
    "sunday" to "日",
    "monday" to "一",
    "tuesday" to "二",
    "wednesday" to "三",
    "thursday" to "四",
    "friday" to "五",
    "saturday" to "六",
    "presetColors" to "预设颜色",
    "fontAttribution" to "本应用使用了 HarmonyOS Sans 字体，由华为终端有限公司提供。",
    "anonKey" to "匿名密钥",
    "operationLog" to "操作日志",
    "feishuPasswordRequired" to "飞书 API 设置已加密，请输入密码查看。",
    "apiPasswordRequired" to "API 设置已加密，请输入密码查看。",
    "passwordError" to "密码错误，请重试。",
    "dataCleared" to "数据已清除",
    "bucketNotExist" to "存储桶不存在，请联系管理员",
    "permissionDenied" to "权限不足，请检查存储桶设置",
    "avatarUploadSuccess" to "头像上传成功",
    "avatarUploadFailed" to "头像上传失败",
    "enterEmail" to "请输入邮箱地址",
    "emailBindSuccess" to "邮箱绑定成功，请检查邮箱验证",
    "emailBindFailed" to "邮箱绑定失败",
    "passwordSameAsCurrent" to "新密码不能与当前密码相同",
    "passwordRequirements" to "密码要求：至少6位字符",
    "passwordRequirement" to "密码要求",
    "passwordTooShort" to "密码至少需要6位字符",
    "resetSuccess" to "密码重置成功",
    "resetFailed" to "密码重置失败",
    "confirmPasswordPlaceholder" to "确认密码",
    "accountSettings" to "账户设置",
    "continue" to "继续",
    "signOutFailed" to "登出失败",
    "used" to "已用",
    "mb" to "MB",
    "chinese" to "中文",
    "japanese" to "日本語",
    "korean" to "韩语",
    "noOperationRecords" to "暂无操作记录",
    "showRecent5" to "显示最近5条",
    "showAll" to "显示全部",
    "clearAll" to "清空",
    "createRecord" to "新增记录",
    "updateRecord" to "编辑记录",
    "deleteRecord" to "删除记录",
    "noRecordsToday" to "今天还没有记录，点击上方按钮添加！",
    "confirmDeleteMessage" to "确定要删除这条记录吗？",
    "editRecord" to "编辑记录",
    "editingRecord" to "正在编辑记录",
    "left" to "剩余",
    "externalLinkWarningTitle" to "外部链接",
    "externalLinkWarningMessage" to "您即将访问外部网站：",
    "fieldsToUpdate" to "需要补齐字段",
    "localDataNewerThanFeishu" to "本地数据比飞书更新，建议先备份本地数据",
    "localDataNewerThanCloud" to "本地数据比云端更新，建议先上传本地数据",
    "externalLinkContinue" to "继续",
    "externalLinkCancel" to "取消",
    "somethingWentWrong" to "出现了错误",
    "unexpectedError" to "发生未知错误",
    "tryAgain" to "重试",
    "refreshPage" to "刷新页面",
    "errorDetails" to "错误详情",
    "howToGetSupabaseApi" to "如何获取 Supabase API 凭证？",
    "supabaseGuideTitle" to "Supabase 配置指南",
    "whatIsSupabase" to "什么是 Supabase？",
    "supabaseDescription" to "Supabase 是一个开源的 Firebase 替代品，提供 PostgreSQL 数据库、身份验证、实时订阅等功能。",
    "supabaseUseCase" to "使用 Supabase 可以让您的吸烟记录数据在多个设备间同步，并在云端安全备份。",
    "supabaseRequirements" to "需要准备：",
    "supabaseEmail" to "一个邮箱地址（用于注册 Supabase 账号）",
    "supabaseTime" to "约 5-10 分钟时间完成配置",
    "supabaseStep1" to "步骤 1：注册 Supabase 账号",
    "supabaseStep1Desc1" to "访问 Supabase 官网：",
    "supabaseStep1Desc2" to "点击右上角的 \"Start your project\" 或 \"Sign In\"",
    "supabaseStep1Desc3" to "使用 GitHub 账号或邮箱注册/登录",
    "supabaseTip" to "💡 提示：Supabase 提供免费套餐，足以满足个人使用需求。",
    "supabaseStep2" to "步骤 2：创建新项目",
    "supabaseStep2Desc1" to "登录后点击 \"New project\" 按钮",
    "supabaseStep2Desc2" to "选择或创建一个组织（Organization）",
    "supabaseStep2Desc3" to "填写项目信息：",
    "supabaseStep2Desc4" to "Name：项目名称（如 popsmoke-tracker）",
    "supabaseStep2Desc5" to "Database Password：设置数据库密码（请牢记）",
    "supabaseStep2Desc6" to "Region：选择距离您最近的服务器区域",
    "supabaseStep2Desc7" to "点击 \"Create new project\" 创建项目",
    "supabaseNote" to "⏳ 注意：项目创建需要 1-2 分钟，请耐心等待。",
    "supabaseStep3" to "步骤 3：获取 API 凭证",
    "supabaseStep3Desc1" to "项目创建完成后，进入项目 Dashboard",
    "supabaseStep3Desc2" to "点击左侧菜单中的 \"Project Settings\"（项目设置）",
    "supabaseStep3Desc3" to "选择 \"API\" 选项卡",
    "supabaseStep3Desc4" to "在 \"Project API keys\" 部分找到以下信息：",
    "supabaseProjectUrl" to "Project URL：",
    "supabaseAnonKey" to "anon public：",
    "supabaseStep4" to "步骤 4：配置到本应用",
    "supabaseStep4Desc1" to "复制 \"Project URL\" 到本应用的 <strong>SUPABASE_URL</strong> 字段",
    "supabaseStep4Desc2" to "复制 \"anon public\" 密钥到本应用的 <strong>SUPABASE_ANON_KEY</strong> 字段",
    "supabaseStep4Desc3" to "点击保存按钮完成配置",
    "supabaseAfterConfig" to "✅ 配置完成后，您可以：",
    "supabaseSyncDevices" to "在不同设备间同步吸烟记录",
    "supabaseCloudBackup" to "数据云端备份，防止丢失",
    "supabaseCloudSync" to "使用云同步功能下载/上传数据",
    "supabasePrev" to "上一步",
    "supabaseNext" to "下一步",
    "supabaseComplete" to "完成",
    "newVersionAvailable" to "发现新版本",
    "releaseNotes" to "更新内容",
    "updateNow" to "立即更新",
    "later" to "稍后提醒",
    "noReleaseNotes" to "暂无更新说明",
    "updateHint" to "更新到最新版本以获得最新功能和错误修复。",
    "recording" to "记录中...",
    "operationFrequent" to "操作过于频繁，请稍后再试",
    "generalSettings" to "通用设置",
    "storageInfo" to "存储信息",
    "checkStorage" to "检查存储",
    "changeAvatar" to "更换头像",
    "bindEmail" to "绑定邮箱",
    "confirmSignOut" to "确认退出",
    "signOutWarning" to "确定要退出登录吗？您的数据将保存在本地。",
    "externalLinkWarning" to "您即将离开本应用访问外部网站。是否继续？",
    "statistics" to "统计信息",
    "thisMonth" to "本月",
    "maxDayRecord" to "最大日记录",
    "chart" to "图表",
    "weekView" to "周视图",
    "monthView" to "月视图",
    "noData" to "暂无数据",
    "sync" to "数据同步",
    "syncDescription" to "与云服务同步数据，实现备份和多设备访问。",
    "noApiConfig" to "未配置API。请先在设置中配置API。",
    "operationLogs" to "操作日志",
    "clearLogs" to "清除日志",
    "conflicting" to "冲突",
    "syncResult" to "同步结果",
    "comingSoon" to "此屏将在后续批次接入",
    "cdThemeColor" to "切换主题颜色",
    "cdLanguage" to "切换语言",
    "cdRecentLogs" to "查看今日记录",
    "cdBackToDashboard" to "返回仪表盘",
    "cdDeleteRecord" to "删除记录",
    "cdRecordNow" to "立即记录一支",
    "cdExpandMonth" to "展开月份",
    "cdCollapseMonth" to "收起月份",
    "cdOpenDatePicker" to "打开日期选择",
    "cdDecrease" to "减少",
    "cdIncrease" to "增加",
    "cdEditRecord" to "修改记录时间",
    "opCreate" to "新增记录",
    "opUpdate" to "修改时间",
    "opDelete" to "删除记录",
    "opClear" to "清空记录",
    "opSync" to "导入备份",
    "opSystem" to "系统",
    "opPause" to "暂停",
    "opResume" to "继续",
    "opClearLog" to "清空",
    "opEntries" to "条记录",
    "opLive" to "实时",
    "opPaused" to "已暂停",
    "opEmpty" to "暂无操作记录",
    "backupSection" to "备份与恢复",
    "backupExport" to "导出备份（JSON）",
    "backupImport" to "导入备份",
    "backupNote" to "包含全部记录与设置的 JSON 文件；换机或重装后可完整恢复。",
    "backupExported" to "已导出",
    "backupInvalid" to "这不是有效的备份文件",
    "importModeTitle" to "导入方式",
    "importModeMessage" to "合并会保留现有记录并补齐缺失的记录；覆盖会用备份替换全部记录与设置。",
    "importMerge" to "合并",
    "importReplace" to "覆盖全部",
    "backupImported" to "已导入",
    "cloudAccount" to "云端账户",
    "cloudAccountHint" to "用 Supabase 账户登录，同步每日上限、阈值与主题。",
    "cloudNotConfigured" to "尚未配置 Supabase。请到接口页填写地址与 anon key。",
    "cloudSettingsUp" to "上传设置",
    "cloudSettingsDown" to "从云端恢复",
    "cloudConflictTitle" to "设置不一致",
    "cloudConflictMessage" to "云端保存的设置与本地不同，用哪一份？",
    "cloudUseCloud" to "使用云端",
    "cloudKeepLocal" to "保留本地",
    "feishuBackupNow" to "备份到飞书",
    "feishuRestoreNow" to "从飞书恢复",
    "feishuSyncHint" to "只上传表里还没有的记录，按 id 去重，不会重复写入。",
    "loginAction" to "登录",
    "emailUnverified" to "验证邮件已发送，请先到邮箱确认再登录。",
    "cloudUploaded" to "上传 {n} 条",
    "cloudSkipped" to "已存在 {n} 条",
    "cloudFailedCount" to "失败 {n} 条",
    "cloudRestoredCount" to "新增 {n} 条记录",
    "cloudUpToDate" to "已是最新",
    "cloudDownloaded" to "下载 {n} 条",
    "syncNothing" to "已是最新",
    "syncPendingCount" to "待同步 {n} 条",
    "syncNow" to "立即同步",
    "syncQueue" to "同步队列",
    "syncQueueEmpty" to "队列为空",
    "syncAll" to "全部同步",
    "syncOne" to "同步",
    "syncRemove" to "移除",
    "syncClearQueue" to "清空",
    "syncEntries" to "条记录",
    "syncPendingState" to "待同步",
    "syncFailedState" to "失败",
    "syncProcessing" to "处理中",
    "syncReady" to "就绪",
    "syncPending" to "待同步",
    "syncClearConfirm" to "确定清空整个队列吗？",
    "syncHasFailedHint" to "有改动同步失败，可在下方队列里重试",
    "syncReadyHint" to "本地改动已全部同步",
    "syncRemoveConfirm" to "确定从队列中移除这条操作吗？",
    "signedInAs" to "已登录",
    "issueNetwork" to "网络不可用，请检查连接。",
    "issueCredentials" to "邮箱或密码不正确。",
    "issueEmailNotConfirmed" to "邮箱尚未验证。",
    "issueSessionExpired" to "登录已过期，请重新登录。",
    "issueServer" to "服务端出错，请稍后再试。",
    "issueTableMissing" to "找不到数据表，请检查 Supabase 表结构。",
    "issueBadResponse" to "服务端返回了无法识别的响应。",
    "issueNoConfig" to "尚未配置 API。",
    "issueWeakPassword" to "密码至少 6 位。",
    "issueAlreadyRegistered" to "该邮箱已注册。",
    "issueMissingFields" to "请填写邮箱和密码。",
    "issueNoCloudSettings" to "云端还没有保存过设置。",
    "issueRateLimited" to "尝试过于频繁，请稍后再试。",
    "issueUnknown" to "出了点问题。",
)

private val JA_TABLE: Map<String, String> = mapOf(
    "appTitle" to "喫煙",
    "appSubtitle" to "管理",
    "tracker" to "記録",
    "notificationSuccess" to "成功！",
    "notificationError" to "エラー！",
    "notificationInfo" to "情報",
    "notificationWarning" to "警告！",
    "analysis" to "分析",
    "settings" to "設定",
    "history" to "履歴",
    "lastRecord" to "前回の記録",
    "smokeButton" to "喫煙",
    "smokes" to "本",
    "clickToLog" to "下のボタンをクリックして記録",
    "todayLogs" to "今日の記録",
    "noRecords" to "今日の記録はありません",
    "recentLogs" to "最近の記録",
    "last24Hours" to "過去24時間のアクティビティ",
    "currentGaugeLevel" to "今日の喫煙数",
    "smokeUnit" to "本",
    "consecutiveDaysWithinLimit" to "連続達成日数",
    "days" to "日",
    "lateNight" to "深夜",
    "earlyMorning" to "早朝",
    "morning" to "午前",
    "noon" to "昼",
    "afternoon" to "午後",
    "evening" to "夜",
    "dailyLimit" to "1日の制限",
    "warningLimit" to "警告レベル",
    "thresholdSettings" to "閾値設定",
    "themeColor" to "テーマ色",
    "language" to "言語 / Language",
    "customHex" to "カスタム色",
    "customColor" to "カスタム色",
    "theme" to "テーマ",
    "warningMsg" to "警告：制限に近づいています！",
    "limitReachedMsg" to "1日の制限に達しました！",
    "totalCigarettes" to "総本数",
    "exportCSV" to "CSV出力",
    "csvNote" to "日付、時間、月次タグが含まれます。",
    "noDataToExport" to "この月にはエクスポートするデータがありません。",
    "export" to "エクスポート",
    "exportSuccess" to "エクスポート成功",
    "exportFailed" to "エクスポート失敗",
    "weeklyFreq" to "週間",
    "monthlyFreq" to "月間",
    "thisWeek" to "今週",
    "weekData" to "週データ",
    "monthData" to "月データ",
    "weekTotal" to "今週合計",
    "mostSmokedDay" to "最も喫煙した曜日",
    "monthTotal" to "今月合計",
    "lastMonthTotal" to "先月合計",
    "mostSmokedDate" to "最も喫煙した日",
    "day" to "日",
    "mon" to "月",
    "tue" to "火",
    "wed" to "水",
    "thu" to "木",
    "fri" to "金",
    "sat" to "土",
    "sun" to "日",
    "more" to "詳細 / 編集",
    "back" to "戻る",
    "next" to "次へ",
    "prev" to "前へ",
    "delete" to "削除",
    "edit" to "編集",
    "rename" to "名前を変更",
    "reset" to "リセット",
    "close" to "閉じる",
    "hide" to "非表示",
    "save" to "保存",
    "cancel" to "キャンセル",
    "confirm" to "確認",
    "confirmDelete" to "この記録を削除してもよろしいですか？",
    "editTime" to "時間を編集",
    "never" to "なし",
    "view" to "表示",
    "about" to "について",
    "blog" to "ブログ",
    "github" to "GitHub",
    "stats" to "統計",
    "data" to "データ",
    "hour" to "時",
    "minute" to "分",
    "count" to "本数",
    "clearLocalData" to "ローカルデータをクリア",
    "currentMonth" to "当月",
    "prevMonth" to "前月",
    "nextMonth" to "次月",
    "confirmClearData" to "データをクリア",
    "cancelClearData" to "キャンセル",
    "clearDataWarning" to "警告！",
    "clearDataMessage" to "すべてのローカルデータをクリアしてもよろしいですか？この操作は元に戻せません！",
    "clearData" to "データをクリア",
    "clearDataConfirm" to "これによりすべての喫煙記録が削除されます。この操作は元に戻せません。よろしいですか？",
    "yesClear" to "はい、クリア",
    "loadMore" to "もっと読み込む",
    "storageError" to "ストレージエラー",
    "storageErrorHint" to "設定に移動して古いデータを削除し、スペースを解放してください。",
    "goToSettings" to "設定へ",
    "dismiss" to "閉じる",
    "filtersAndSort" to "フィルターと並べ替え",
    "startDate" to "開始日",
    "endDate" to "終了日",
    "minCount" to "最小数",
    "maxCount" to "最大数",
    "sortByDate" to "日付で並べ替え",
    "sortByCount" to "数で並べ替え",
    "resetFilters" to "フィルターをリセット",
    "noMatchingRecords" to "一致する記録がありません",
    "totalRecords" to "総記録数",
    "filteredRecords" to "フィルター後の記録数",
    "dateRange" to "日付範囲",
    "averagePerDay" to "1日あたりの平均",
    "prevPage" to "前のページ",
    "nextPage" to "次のページ",
    "feishuApi" to "Feishu API",
    "feishuApiSettings" to "Feishu API設定",
    "feishuDataSync" to "Feishuデータ同期",
    "apiUrl" to "API URL",
    "syncFromFeishu" to "Feishuから同期",
    "syncToFeishu" to "Feishuに同期",
    "startSync" to "同期開始",
    "syncing" to "同期中...",
    "targetTableName" to "対象テーブル名",
    "accessKey" to "アクセスキー",
    "accessKeyDialogTitle" to "認証パスワード",
    "accessKeyDialogMessage" to "Feishu API書き込みアクセスキーを入力してください:",
    "submitToFeishu" to "Feishuに送信",
    "submitting" to "送信中...",
    "startSubmit" to "送信開始",
    "syncFromSupabaseOnly" to "Supabaseと同期",
    "syncDiffSummary" to "同期概要",
    "cloudOnlyRecords" to "クラウドのみのレコード",
    "confirmSync" to "同期を確認",
    "localRecords" to "ローカルレコード",
    "cloudRecords" to "クラウドレコード",
    "localOnly" to "ローカルのみ",
    "cloudOnly" to "クラウドのみ",
    "conflicts" to "競合",
    "upload" to "アップロード",
    "download" to "ダウンロード",
    "uploadOnly" to "アップロードのみ",
    "downloadOnly" to "ダウンロードのみ",
    "foundNewRecords" to "{count}件の新しいレコードが見つかりました",
    "foundConflictingRecords" to "{count}件の競合レコード",
    "needUploadRecords" to "{count}件のレコードをアップロードする必要があります",
    "needDownloadRecords" to "{count}件のレコードをダウンロードする必要があります",
    "uploadCount" to "{count}件アップロード",
    "downloadCount" to "{count}件ダウンロード",
    "syncCompleted" to "{source}との同期が完了しました：{message}",
    "syncWithFeishuCompleted" to "Feishuとの同期が完了しました",
    "syncWithSupabaseCompleted" to "Supabaseとの同期が完了しました",
    "cloudSync" to "クラウド同期",
    "cloudSyncing" to "同期中...",
    "loginFirst" to "同期するにはログインしてください",
    "syncSuccess" to "同期成功",
    "syncFailed" to "同期失敗",
    "noNewRecordsToSync" to "同期する新しいレコードがありません",
    "uploaded" to "アップロード",
    "downloaded" to "ダウンロード",
    "records" to "件",
    "feishuSyncSuccess" to "Feishu APIからの同期成功",
    "feishuSyncToStorage" to "Feishu APIからローカルストレージへの同期成功",
    "feishuSyncToCloud" to "Feishu APIからSupabaseへの同期成功",
    "syncedRecordsFromFeishu" to "Feishuから {newCount} 件の新しいレコードを同期しました（総数：{totalCount} 件、重複：{duplicateCount} 件）",
    "importedCount" to "インポート数",
    "totalCount" to "総数",
    "submittedCount" to "送信成功数",
    "failedCount" to "送信失敗数",
    "signIn" to "ログイン到Supabase",
    "signUp" to "登録",
    "signOut" to "ログアウト",
    "email" to "メール",
    "password" to "パスワード",
    "passwordPlaceholder" to "パスワードを入力",
    "noAccount" to "アカウントがない場合: 登録",
    "haveAccount" to "すでにアカウントがある場合: ログイン",
    "loading" to "読み込み中...",
    "initializing" to "初期化中...",
    "connecting" to "接続中...",
    "authenticating" to "認証中...",
    "resetting" to "リセット中...",
    "restoring" to "復元中...",
    "skipLogin" to "ログインをスキップしてローカルモードを使用",
    "verifyEmail" to "登録成功！確認メールを確認し、リンクをクリックして確認を完了してください。",
    "localModeHint" to "現在ローカルモードを使用中。ログインしてクラウドにデータを同期",
    "accountManagement" to "アカウント管理",
    "uploadAvatar" to "アバターをアップロード",
    "uploading" to "アップロード中...",
    "emailPlaceholder" to "メールアドレスを入力",
    "updateEmail" to "メールを更新",
    "phone" to "電話番号",
    "phonePlaceholder" to "電話番号を入力",
    "updatePhone" to "電話番号を更新",
    "resetPassword" to "パスワードをリセット",
    "currentPassword" to "現在のパスワード",
    "newPassword" to "新しいパスワード",
    "confirmPassword" to "パスワードを確認",
    "passwordMinLength" to "パスワードは6文字以上である必要があります",
    "passwordMismatch" to "パスワードが一致しません",
    "passwordResetSuccess" to "パスワードリセット成功",
    "passwordResetFailed" to "パスワードリセット失敗",
    "currentPasswordIncorrect" to "現在のパスワードが正しくありません",
    "signOutConfirm" to "ログアウトしてもよろしいですか？",
    "cloudDataFound" to "クラウドデータ発見",
    "cloudDataFoundMessage" to "クラウドストレージに {count} 件の記録が見つかりました。ダウンロードしますか？",
    "downloadFromCloud" to "クラウドからダウンロード",
    "skipDownload" to "スキップ",
    "loginRequired" to "ログインが必要",
    "loginRequiredMessage" to "Supabase APIは設定されていますが、ログインしていません。クラウドデータにアクセスするにはログインしてください。",
    "loginNow" to "今すぐログイン",
    "selectDataSource" to "データソースを選択",
    "selectDataSourceMessage" to "データを取得するデータソースを選択してください：",
    "apiManagement" to "API管理",
    "api" to "API",
    "saveApiSettings" to "保存",
    "securityPassword" to "セキュリティパスワード",
    "enterPassword" to "セキュリティパスワードを入力",
    "selectSyncSource" to "同期ソースを選択",
    "firstLaunchSyncMessage" to "ようこそ！クラウド同期が設定されていますが、ローカルデータがありません。履歴を取得するデータソースを選択してください：",
    "feishu" to "Feishu",
    "supabase" to "Supabase",
    "storageMethod" to "ストレージ方式",
    "dataManagement" to "データ管理",
    "storageUsage" to "ストレージ使用量",
    "clearOldData" to "ローカルデータをクリア",
    "systemLog" to "システムログ",
    "viewSystemLogs" to "システムログを表示",
    "apiUrlDisplay" to "保存されたAPI設定",
    "apiConfigRequired" to "API設定が必要",
    "deleteApiSettings" to "API設定を削除",
    "confirmDeleteApiSettings" to "保存されたAPI設定を削除してもよろしいですか？",
    "renameApiSettings" to "API設定の名前を変更",
    "resetApiSettings" to "API設定をリセット",
    "enterNewPassword" to "API設定の新しいセキュリティパスワードを入力してください：",
    "apiSettingsVerifyPassword" to "保存されたAPI設定でセキュリティパスワードを確認して変更してください",
    "viewSavedApiSettings" to "保存されたAPI設定を表示",
    "previousLoginFound" to "以前のログイン記録が見つかりました",
    "previousLoginFoundMessage" to "以前のログイン記録が見つかりました。ログイン状態を復元しますか？まずセキュリティパスワードを確認する必要があります。",
    "restoreLogin" to "ログインを復元",
    "continueLocalMode" to "ローカルモードを続行",
    "enterSecurityPasswordForRestore" to "ログイン状態を復元するためにセキュリティパスワードを入力してください",
    "clear" to "クリア",
    "today" to "今日",
    "selectDate" to "日付を選択",
    "selectStartDate" to "開始日を選択",
    "selectEndDate" to "終了日を選択",
    "january" to "1月",
    "february" to "2月",
    "march" to "3月",
    "april" to "4月",
    "may" to "5月",
    "june" to "6月",
    "july" to "7月",
    "august" to "8月",
    "september" to "9月",
    "october" to "10月",
    "november" to "11月",
    "december" to "12月",
    "sunday" to "日",
    "monday" to "月",
    "tuesday" to "火",
    "wednesday" to "水",
    "thursday" to "木",
    "friday" to "金",
    "saturday" to "土",
    "presetColors" to "プリセットカラー",
    "fontAttribution" to "このアプリは Huawei Device Co., Ltd. 提供の HarmonyOS Sans フォントを使用しています。",
    "anonKey" to "匿名キー",
    "operationLog" to "操作ログ",
    "feishuPasswordRequired" to "Feishu API設定は暗号化されています。表示するにはパスワードを入力してください。",
    "apiPasswordRequired" to "API設定は暗号化されています。表示するにはパスワードを入力してください。",
    "passwordError" to "パスワードが正しくありません。もう一度お試しください。",
    "dataCleared" to "データがクリアされました",
    "bucketNotExist" to "ストレージバケットが存在しません。管理者に連絡してください",
    "permissionDenied" to "権限が拒否されました。バケット設定を確認してください",
    "avatarUploadSuccess" to "アバターが正常にアップロードされました",
    "avatarUploadFailed" to "アバターのアップロードに失敗しました",
    "enterEmail" to "メールアドレスを入力してください",
    "emailBindSuccess" to "メールが正常にバインドされました。確認メールをご確認ください",
    "emailBindFailed" to "メールのバインドに失敗しました",
    "passwordSameAsCurrent" to "新しいパスワードは現在のパスワードと同じにできません",
    "passwordRequirements" to "パスワード要件：6文字以上",
    "passwordRequirement" to "パスワード要件",
    "passwordTooShort" to "パスワードは6文字以上必要です",
    "resetSuccess" to "パスワードリセット成功",
    "resetFailed" to "パスワードリセット失敗",
    "confirmPasswordPlaceholder" to "パスワード確認",
    "accountSettings" to "アカウント設定",
    "continue" to "続ける",
    "signOutFailed" to "サインアウトに失敗しました",
    "used" to "使用済み",
    "mb" to "MB",
    "chinese" to "中国語",
    "japanese" to "日本語",
    "korean" to "韓国語",
    "noOperationRecords" to "操作記録なし",
    "showRecent5" to "直近5件を表示",
    "showAll" to "すべて表示",
    "clearAll" to "すべてクリア",
    "createRecord" to "記録を作成",
    "updateRecord" to "記録を更新",
    "deleteRecord" to "記録を削除",
    "noRecordsToday" to "本日の記録はありません。上のボタンをタップして追加してください！",
    "confirmDeleteMessage" to "この記録を削除してもよろしいですか？",
    "editRecord" to "記録の編集",
    "editingRecord" to "記録を編集中",
    "left" to "残り",
    "externalLinkWarningTitle" to "外部リンク",
    "externalLinkWarningMessage" to "外部ウェブサイトにアクセスしようとしています：",
    "fieldsToUpdate" to "フィールドの更新",
    "localDataNewerThanFeishu" to "ローカルデータがFeishuより新しいです。まずローカルデータをバックアップしてください",
    "localDataNewerThanCloud" to "ローカルデータがクラウドより新しいです。まずローカルデータをアップロードしてください",
    "externalLinkContinue" to "続ける",
    "externalLinkCancel" to "キャンセル",
    "somethingWentWrong" to "エラーが発生しました",
    "unexpectedError" to "予期しないエラーが発生しました",
    "tryAgain" to "再試行",
    "refreshPage" to "ページを更新",
    "errorDetails" to "エラーの詳細",
    "howToGetSupabaseApi" to "Supabase API 認証情報を取得方法は？",
    "supabaseGuideTitle" to "Supabase セットアップガイド",
    "whatIsSupabase" to "Supabase とは？",
    "supabaseDescription" to "Supabase は、PostgreSQL データベース、認証、リアルタイムサブスクリプションなどを提供するオープンソースの Firebase 代替です。",
    "supabaseUseCase" to "Supabase を使用すると、吸烟記録を複数のデバイスで同期し、クラウドに安全にバックアップできます。",
    "supabaseRequirements" to "必要なもの：",
    "supabaseEmail" to "Supabase アカウント登録用のメールアドレス",
    "supabaseTime" to "設定完了まで約5〜10分",
    "supabaseStep1" to "ステップ1：Supabase アカウントを登録",
    "supabaseStep1Desc1" to "Supabase ウェブサイトにアクセス：",
    "supabaseStep1Desc2" to "右上の \"Start your project\" または \"Sign In\" をクリック",
    "supabaseStep1Desc3" to "GitHub アカウントまたはメールで登録/ログイン",
    "supabaseTip" to "💡 ヒント：Supabase は個人使用には十分な無料プランを提供しています。",
    "supabaseStep2" to "ステップ2：新プロジェクトを作成",
    "supabaseStep2Desc1" to "ログイン後、\"New project\" ボタンをクリック",
    "supabaseStep2Desc2" to "組織を選択または作成",
    "supabaseStep2Desc3" to "プロジェクト情報を入力：",
    "supabaseStep2Desc4" to "Name：プロジェクト名（例：popsmoke-tracker）",
    "supabaseStep2Desc5" to "Database Password：データベースパスワードを設定（覚えておいてください）",
    "supabaseStep2Desc6" to "Region：最も近いサーバーリージョンを選択",
    "supabaseStep2Desc7" to "\"Create new project\" をクリックしてプロジェクトを作成",
    "supabaseNote" to "⏳ 注意：プロジェクトの作成には1〜2分かかりますので、お待ちください。",
    "supabaseStep3" to "ステップ3：API 認証情報を取得",
    "supabaseStep3Desc1" to "プロジェクト作成後、プロジェクトダッシュボードに入る",
    "supabaseStep3Desc2" to "左メニューの \"Project Settings\" をクリック",
    "supabaseStep3Desc3" to "\"API\" タブを選択",
    "supabaseStep3Desc4" to "\"Project API keys\" セクションで以下の情報を確認：",
    "supabaseProjectUrl" to "Project URL：",
    "supabaseAnonKey" to "anon public：",
    "supabaseStep4" to "ステップ4：このアプリに設定",
    "supabaseStep4Desc1" to "このアプリの <strong>SUPABASE_URL</strong> フィールドに \"Project URL\" をコピー",
    "supabaseStep4Desc2" to "このアプリの <strong>SUPABASE_ANON_KEY</strong> フィールドに \"anon public\" キーをコピー",
    "supabaseStep4Desc3" to "保存ボタンをクリックして設定完了",
    "supabaseAfterConfig" to "✅ 設定完了後、以下が可能に：",
    "supabaseSyncDevices" to "異なるデバイス間で吸烟記録を同期",
    "supabaseCloudBackup" to "データ損失防止のクラウドバックアップ",
    "supabaseCloudSync" to "クラウド同期でデータのダウンロード/アップロード",
    "supabasePrev" to "前へ",
    "supabaseNext" to "次へ",
    "supabaseComplete" to "完了",
    "newVersionAvailable" to "新しいバージョンが利用可能です",
    "releaseNotes" to "更新内容",
    "updateNow" to "今すぐ更新",
    "later" to "後で通知",
    "noReleaseNotes" to "リリースノートはありません",
    "updateHint" to "最新バージョンに更新すると、最新の機能とバグ修正が利用できます。",
    "recording" to "記録中...",
    "operationFrequent" to "操作が頻繁です。しばらくお待ちください",
    "generalSettings" to "一般設定",
    "storageInfo" to "ストレージ情報",
    "checkStorage" to "ストレージ確認",
    "changeAvatar" to "アバター変更",
    "bindEmail" to "メール紐付け",
    "confirmSignOut" to "ログアウト確認",
    "signOutWarning" to "ログアウトしてもよろしいですか？データはローカルに保存されます。",
    "externalLinkWarning" to "外部ウェブサイトに移動します。続行しますか？",
    "statistics" to "統計情報",
    "thisMonth" to "今月",
    "maxDayRecord" to "最大日別記録",
    "chart" to "チャート",
    "weekView" to "週ビュー",
    "monthView" to "月ビュー",
    "noData" to "データなし",
    "sync" to "データ同期",
    "syncDescription" to "クラウドサービスとデータを同期して、バックアップとマルチデバイスアクセスを実現します。",
    "noApiConfig" to "APIが設定されていません。先に設定でAPIを構成してください。",
    "operationLogs" to "操作ログ",
    "clearLogs" to "ログ消去",
    "conflicting" to "競合",
    "syncResult" to "同期結果",
    "comingSoon" to "この画面は次のバッチで実装されます",
    "cdThemeColor" to "テーマ色を変更",
    "cdLanguage" to "言語を変更",
    "cdRecentLogs" to "今日の記録を見る",
    "cdBackToDashboard" to "ダッシュボードに戻る",
    "cdDeleteRecord" to "記録を削除",
    "cdRecordNow" to "今すぐ記録する",
    "cdExpandMonth" to "月を展開",
    "cdCollapseMonth" to "月を折りたたむ",
    "cdOpenDatePicker" to "日付選択を開く",
    "cdDecrease" to "減らす",
    "cdIncrease" to "増やす",
    "cdEditRecord" to "記録時刻を編集",
    "opCreate" to "記録を追加",
    "opUpdate" to "時刻を変更",
    "opDelete" to "記録を削除",
    "opClear" to "記録を消去",
    "opSync" to "バックアップを読み込み",
    "opSystem" to "システム",
    "opPause" to "一時停止",
    "opResume" to "再開",
    "opClearLog" to "クリア",
    "opEntries" to "件",
    "opLive" to "ライブ",
    "opPaused" to "一時停止中",
    "opEmpty" to "操作記録なし",
    "backupSection" to "バックアップと復元",
    "backupExport" to "バックアップを書き出す（JSON）",
    "backupImport" to "バックアップを読み込む",
    "backupNote" to "全記録と設定を含む JSON ファイル。機種変更や再インストール後に復元できます。",
    "backupExported" to "書き出しました",
    "backupInvalid" to "有効なバックアップではありません",
    "importModeTitle" to "読み込み方法",
    "importModeMessage" to "統合は現在の記録を残して不足分を追加します。置き換えは設定を含めてすべて入れ替えます。",
    "importMerge" to "統合",
    "importReplace" to "すべて置き換え",
    "backupImported" to "読み込みました",
    "cloudAccount" to "クラウドアカウント",
    "cloudAccountHint" to "Supabase でログインすると上限・しきい値・テーマを端末間で同期します。",
    "cloudNotConfigured" to "Supabase が未設定です。API ページで URL と anon key を入力してください。",
    "cloudSettingsUp" to "設定をアップロード",
    "cloudSettingsDown" to "設定を復元",
    "cloudConflictTitle" to "設定が一致しません",
    "cloudConflictMessage" to "クラウドの設定がこの端末と異なります。どちらを使いますか？",
    "cloudUseCloud" to "クラウドを使う",
    "cloudKeepLocal" to "ローカルを残す",
    "feishuBackupNow" to "Feishu にバックアップ",
    "feishuRestoreNow" to "Feishu から復元",
    "feishuSyncHint" to "シートにない記録だけをアップロードします（id で重複を判定）。",
    "loginAction" to "ログイン",
    "emailUnverified" to "確認メールを送信しました。メールを確認してからログインしてください。",
    "cloudUploaded" to "{n} 件アップロード",
    "cloudSkipped" to "{n} 件は既存",
    "cloudFailedCount" to "{n} 件失敗",
    "cloudRestoredCount" to "{n} 件の新しい記録",
    "cloudUpToDate" to "すでに最新です",
    "cloudDownloaded" to "{n} 件ダウンロード",
    "syncNothing" to "すでに最新です",
    "syncPendingCount" to "保留中 {n} 件",
    "syncNow" to "今すぐ同期",
    "syncQueue" to "同期キュー",
    "syncQueueEmpty" to "キューに操作なし",
    "syncAll" to "全て同期",
    "syncOne" to "同期",
    "syncRemove" to "削除",
    "syncClearQueue" to "クリア",
    "syncEntries" to "エントリー",
    "syncPendingState" to "保留中",
    "syncFailedState" to "失敗",
    "syncProcessing" to "処理中",
    "syncReady" to "準備完了",
    "syncPending" to "保留中",
    "syncClearConfirm" to "キューを全てクリアしますか？",
    "syncHasFailedHint" to "同期に失敗した変更があります。下のキューから再試行できます",
    "syncReadyHint" to "ローカルの変更はすべて同期済みです",
    "syncRemoveConfirm" to "この操作をキューから削除しますか？",
    "signedInAs" to "ログイン中",
    "issueNetwork" to "ネットワークに接続できません。",
    "issueCredentials" to "メールアドレスまたはパスワードが違います。",
    "issueEmailNotConfirmed" to "メールが未確認です。",
    "issueSessionExpired" to "セッションが切れました。再ログインしてください。",
    "issueServer" to "サーバーエラーです。後でもう一度お試しください。",
    "issueTableMissing" to "テーブルが見つかりません。Supabase のスキーマを確認してください。",
    "issueBadResponse" to "サーバーの応答を解釈できません。",
    "issueNoConfig" to "API が未設定です。",
    "issueWeakPassword" to "パスワードは 6 文字以上必要です。",
    "issueAlreadyRegistered" to "このメールは登録済みです。",
    "issueMissingFields" to "メールとパスワードを入力してください。",
    "issueNoCloudSettings" to "クラウドに設定がまだありません。",
    "issueRateLimited" to "試行が多すぎます。少し待ってください。",
    "issueUnknown" to "問題が発生しました。",
)

private val KO_TABLE: Map<String, String> = mapOf(
    "appTitle" to "흡연",
    "appSubtitle" to "관리",
    "tracker" to "기록",
    "notificationSuccess" to "성공！",
    "notificationError" to "오류！",
    "notificationInfo" to "정보",
    "notificationWarning" to "경고！",
    "analysis" to "분석",
    "settings" to "설정",
    "history" to "기록",
    "lastRecord" to "마지막 기록",
    "smokeButton" to "흡연",
    "smokes" to "개",
    "clickToLog" to "아래 버튼을 클릭하여 기록",
    "todayLogs" to "오늘의 기록",
    "noRecords" to "오늘 기록 없음",
    "recentLogs" to "최근 기록",
    "last24Hours" to "최근 24시간 활동",
    "currentGaugeLevel" to "오늘의 흡연 수",
    "smokeUnit" to "개",
    "consecutiveDaysWithinLimit" to "연속 달성 일수",
    "days" to "일",
    "lateNight" to "심야",
    "earlyMorning" to "새벽",
    "morning" to "오전",
    "noon" to "점심",
    "afternoon" to "오후",
    "evening" to "저녘",
    "dailyLimit" to "일일 한도",
    "warningLimit" to "경고 수준",
    "thresholdSettings" to "임계값 설정",
    "themeColor" to "테마 색상",
    "language" to "언어 / Language",
    "customHex" to "사용자 지정 색상",
    "customColor" to "사용자 지정 색상",
    "theme" to "테마",
    "warningMsg" to "경고: 일일 한도에 근접!",
    "limitReachedMsg" to "일일 한도 도달!",
    "totalCigarettes" to "총 담배 수",
    "exportCSV" to "CSV 내보내기",
    "csvNote" to "날짜, 시간 및 월별 그룹 태그가 포함됩니다.",
    "noDataToExport" to "이번 달에 내보낼 데이터가 없습니다.",
    "export" to "내보내기",
    "exportSuccess" to "내보내기 성공",
    "exportFailed" to "내보내기 실패",
    "weeklyFreq" to "주간",
    "monthlyFreq" to "월간",
    "thisWeek" to "이번 주",
    "weekData" to "주 데이터",
    "monthData" to "월 데이터",
    "weekTotal" to "이번 주 합계",
    "mostSmokedDay" to "가장 많이 흡연한 요일",
    "monthTotal" to "이번 달 합계",
    "lastMonthTotal" to "지난 달 합계",
    "mostSmokedDate" to "가장 많이 흡연한 날",
    "day" to "일",
    "mon" to "월",
    "tue" to "화",
    "wed" to "수",
    "thu" to "목",
    "fri" to "금",
    "sat" to "토",
    "sun" to "일",
    "more" to "더보기 / 편집",
    "back" to "뒤로",
    "next" to "다음",
    "prev" to "이전",
    "delete" to "삭제",
    "edit" to "편집",
    "rename" to "이름 변경",
    "reset" to "재설정",
    "close" to "닫기",
    "hide" to "숨기기",
    "save" to "저장",
    "cancel" to "취소",
    "confirm" to "확인",
    "confirmDelete" to "이 기록을 삭제하시겠습니까?",
    "editTime" to "시간 편집",
    "never" to "없음",
    "view" to "보기",
    "about" to "정보",
    "blog" to "블로그",
    "github" to "GitHub",
    "stats" to "통계",
    "data" to "데이터",
    "hour" to "시",
    "minute" to "분",
    "count" to "개수",
    "clearLocalData" to "로컬 데이터 지우기",
    "currentMonth" to "현재 월",
    "prevMonth" to "이전 월",
    "nextMonth" to "다음 월",
    "confirmClearData" to "데이터 지우기 확인",
    "cancelClearData" to "취소",
    "clearDataWarning" to "경고!",
    "clearDataMessage" to "모든 로컬 데이터를 지우시겠습니까? 이 작업은 되돌릴 수 없습니다!",
    "clearData" to "데이터 지우기",
    "clearDataConfirm" to "이 작업은 모든 흡연 기록을 지웁니다. 이 작업은 되돌릴 수 없습니다. 계속하시겠습니까?",
    "yesClear" to "예, 지우기",
    "loadMore" to "더 불러오기",
    "storageError" to "저장소 오류",
    "storageErrorHint" to "설정으로 이동하여 오래된 데이터를 삭제하고 공간을 확보하세요.",
    "goToSettings" to "설정으로",
    "dismiss" to "닫기",
    "filtersAndSort" to "필터 및 정렬",
    "startDate" to "시작 날짜",
    "endDate" to "종료 날짜",
    "minCount" to "최소 수",
    "maxCount" to "최대 수",
    "sortByDate" to "날짜순 정렬",
    "sortByCount" to "수량순 정렬",
    "resetFilters" to "필터 재설정",
    "noMatchingRecords" to "일치하는 기록 없음",
    "totalRecords" to "총 기록 수",
    "filteredRecords" to "필터링된 기록 수",
    "dateRange" to "날짜 범위",
    "averagePerDay" to "일일 평균",
    "prevPage" to "이전 페이지",
    "nextPage" to "다음 페이지",
    "feishuApi" to "Feishu API",
    "feishuApiSettings" to "Feishu API 설정",
    "feishuDataSync" to "Feishu 데이터 동기화",
    "apiUrl" to "API URL",
    "syncFromFeishu" to "Feishu에서 동기화",
    "syncToFeishu" to "Feishu로 동기화",
    "startSync" to "동기화 시작",
    "syncing" to "동기화 중...",
    "targetTableName" to "대상 테이블 이름",
    "accessKey" to "접근 키",
    "accessKeyDialogTitle" to "인증 비밀번호",
    "accessKeyDialogMessage" to "Feishu API 쓰기 접근 키를 입력하세요:",
    "submitToFeishu" to "Feishu로 제출",
    "submitting" to "제출 중...",
    "startSubmit" to "제출 시작",
    "syncFromSupabaseOnly" to "Supabase와 동기화",
    "syncDiffSummary" to "동기화 요약",
    "cloudOnlyRecords" to "클라우드 전용 레코드",
    "confirmSync" to "동기화 확인",
    "localRecords" to "로컬 레코드",
    "cloudRecords" to "클라우드 레코드",
    "localOnly" to "로컬 전용",
    "cloudOnly" to "클라우드 전용",
    "conflicts" to "충돌",
    "upload" to "업로드",
    "download" to "다운로드",
    "uploadOnly" to "업로드만",
    "downloadOnly" to "다운로드만",
    "foundNewRecords" to "{count}개의 새 레코드를 찾았습니다",
    "foundConflictingRecords" to "{count}개의 충돌 레코드",
    "needUploadRecords" to "{count}개의 레코드를 업로드해야 합니다",
    "needDownloadRecords" to "{count}개의 레코드를 다운로드해야 합니다",
    "uploadCount" to "{count}개 업로드",
    "downloadCount" to "{count}개 다운로드",
    "syncCompleted" to "{source}와의 동기화가 완료되었습니다：{message}",
    "syncWithFeishuCompleted" to "Feishu와의 동기화가 완료되었습니다",
    "syncWithSupabaseCompleted" to "Supabase와의 동기화가 완료되었습니다",
    "cloudSync" to "클라우드 동기화",
    "cloudSyncing" to "동기화 중...",
    "loginFirst" to "동기화하려면 먼저 로그인하세요",
    "syncSuccess" to "동기화 성공",
    "syncFailed" to "동기화 실패",
    "noNewRecordsToSync" to "동기화할 새 레코드가 없습니다",
    "uploaded" to "업로드됨",
    "downloaded" to "다운로드됨",
    "records" to "개",
    "feishuSyncSuccess" to "Feishu API에서 동기화 성공",
    "feishuSyncToStorage" to "Feishu API에서 로컬 스토리지로 동기화 성공",
    "feishuSyncToCloud" to "Feishu API에서 Supabase로 동기화 성공",
    "syncedRecordsFromFeishu" to "Feishu에서 {newCount}개의 새 레코드를 동기화했습니다（총：{totalCount}개，중복：{duplicateCount}개）",
    "importedCount" to "가져온 수",
    "totalCount" to "총 수",
    "submittedCount" to "제출 성공 수",
    "failedCount" to "제출 실패 수",
    "signIn" to "로그인到Supabase",
    "signUp" to "가입",
    "signOut" to "로그아웃",
    "email" to "이메일",
    "password" to "비밀번호",
    "passwordPlaceholder" to "비밀번호 입력",
    "noAccount" to "계정이 없으신가요? 가입",
    "haveAccount" to "이미 계정이 있으신가요? 로그인",
    "loading" to "로딩 중...",
    "initializing" to "초기화 중...",
    "connecting" to "연결 중...",
    "authenticating" to "인증 중...",
    "resetting" to "초기화 중...",
    "restoring" to "복원 중...",
    "skipLogin" to "로그인 건너뛰기, 로컬 모드 사용",
    "verifyEmail" to "가입 성공! 이메일을 확인하고 확인 링크를 클릭하세요.",
    "localModeHint" to "현재 로컬 모드 사용 중. 로그인하여 클라우드에 데이터 동기화",
    "accountManagement" to "계정 관리",
    "uploadAvatar" to "아바타 업로드",
    "uploading" to "업로드 중...",
    "emailPlaceholder" to "이메일 입력",
    "updateEmail" to "이메일 업데이트",
    "phone" to "전화번호",
    "phonePlaceholder" to "전화번호 입력",
    "updatePhone" to "전화번호 업데이트",
    "resetPassword" to "비밀번호 재설정",
    "currentPassword" to "현재 비밀번호",
    "newPassword" to "새 비밀번호",
    "confirmPassword" to "비밀번호 확인",
    "passwordMinLength" to "비밀번호는 6자 이상이어야 합니다",
    "passwordMismatch" to "비밀번호가 일치하지 않습니다",
    "passwordResetSuccess" to "비밀번호 재설정 성공",
    "passwordResetFailed" to "비밀번호 재설정 실패",
    "currentPasswordIncorrect" to "현재 비밀번호가 올바르지 않습니다",
    "signOutConfirm" to "로그아웃 하시겠습니까?",
    "cloudDataFound" to "클라우드 데이터 발견",
    "cloudDataFoundMessage" to "클라우드 스토리지에 {count}개의 기록이 있습니다. 다운로드하시겠습니까?",
    "downloadFromCloud" to "클라우드에서 다운로드",
    "skipDownload" to "건너뛰기",
    "loginRequired" to "로그인 필요",
    "loginRequiredMessage" to "Supabase API가 구성되었지만 로그인되지 않았습니다. 클라우드 데이터에 액세스하려면 로그인하세요.",
    "loginNow" to "지금 로그인",
    "selectDataSource" to "데이터 소스 선택",
    "selectDataSourceMessage" to "데이터를 가져올 데이터 소스를 선택하세요:",
    "apiManagement" to "API 관리",
    "api" to "API",
    "saveApiSettings" to "저장",
    "securityPassword" to "보안 비밀번호",
    "enterPassword" to "보안 비밀번호 입력",
    "selectSyncSource" to "동기화 소스 선택",
    "firstLaunchSyncMessage" to "환영합니다! 클라우드 동기화가 설정되어 있지만 로컬 데이터가 없습니다. 기록을 가져올 데이터 소스를 선택하세요:",
    "feishu" to "Feishu",
    "supabase" to "Supabase",
    "storageMethod" to "스토리지 방식",
    "dataManagement" to "데이터 관리",
    "storageUsage" to "스토리지 사용량",
    "clearOldData" to "로컬 데이터 지우기",
    "systemLog" to "시스템 로그",
    "viewSystemLogs" to "시스템 로그 보기",
    "apiUrlDisplay" to "저장된 API 설정",
    "apiConfigRequired" to "API 설정 필요",
    "deleteApiSettings" to "API 설정 삭제",
    "confirmDeleteApiSettings" to "저장된 API 설정을 삭제하시겠습니까?",
    "renameApiSettings" to "API 설정 이름 변경",
    "resetApiSettings" to "API 설정 재설정",
    "enterNewPassword" to "API 설정의 새 보안 비밀번호를 입력하세요:",
    "apiSettingsVerifyPassword" to "저장된 API 설정에서 보안 비밀번호를 확인하여 수정하세요",
    "viewSavedApiSettings" to "저장된 API 설정 보기",
    "previousLoginFound" to "이전 로그인 기록 발견",
    "previousLoginFoundMessage" to "이전 로그인 기록이 발견되었습니다. 로그인 상태를 복원하시겠습니까? 먼저 보안 비밀번호를 확인해야 합니다.",
    "restoreLogin" to "로그인 복원",
    "continueLocalMode" to "로컬 모드 계속",
    "enterSecurityPasswordForRestore" to "로그인 상태를 복원하려면 보안 비밀번호를 입력하세요",
    "clear" to "지우기",
    "today" to "오늘",
    "selectDate" to "날짜 선택",
    "selectStartDate" to "시작 날짜 선택",
    "selectEndDate" to "종료 날짜 선택",
    "january" to "1월",
    "february" to "2월",
    "march" to "3월",
    "april" to "4월",
    "may" to "5월",
    "june" to "6월",
    "july" to "7월",
    "august" to "8월",
    "september" to "9월",
    "october" to "10월",
    "november" to "11월",
    "december" to "12월",
    "sunday" to "일",
    "monday" to "월",
    "tuesday" to "화",
    "wednesday" to "수",
    "thursday" to "목",
    "friday" to "금",
    "saturday" to "토",
    "presetColors" to "프리셋 색상",
    "fontAttribution" to "본 앱은 Huawei Device Co., Ltd.에서 제공하는 HarmonyOS Sans 글꼴을 사용합니다.",
    "anonKey" to "익명 키",
    "operationLog" to "작업 로그",
    "feishuPasswordRequired" to "Feishu API 설정이 암호화되어 있습니다. 복호화하려면 비밀번호를 입력하세요.",
    "apiPasswordRequired" to "API 설정이 암호화되어 있습니다. 복호화하려면 비밀번호를 입력하세요.",
    "passwordError" to "비밀번호가 올바르지 않습니다. 다시 시도해주세요.",
    "dataCleared" to "데이터가 삭제되었습니다",
    "bucketNotExist" to "스토리지 버킷이 존재하지 않습니다. 관리자에게 문의하세요",
    "permissionDenied" to "권한이 거부되었습니다. 버킷 설정을 확인하세요",
    "avatarUploadSuccess" to "아바타가 성공적으로 업로드되었습니다",
    "avatarUploadFailed" to "아바타 업로드 실패",
    "enterEmail" to "이메일 주소를 입력하세요",
    "emailBindSuccess" to "이메일이 성공적으로 바인딩되었습니다. 인증 이메일을 확인하세요",
    "emailBindFailed" to "이메일 바인딩 실패",
    "passwordSameAsCurrent" to "새 비밀번호는 현재 비밀번호와 같을 수 없습니다",
    "passwordRequirements" to "비밀번호 요구사항: 6자 이상",
    "passwordRequirement" to "비밀번호 요구사항",
    "passwordTooShort" to "비밀번호는 6자 이상이어야 합니다",
    "resetSuccess" to "비밀번호 재설정 성공",
    "resetFailed" to "비밀번호 재설정 실패",
    "confirmPasswordPlaceholder" to "비밀번호 확인",
    "accountSettings" to "계정 설정",
    "continue" to "계속",
    "signOutFailed" to "로그아웃 실패",
    "used" to "사용됨",
    "mb" to "MB",
    "chinese" to "중국어",
    "japanese" to "일본어",
    "korean" to "한국어",
    "noOperationRecords" to "작업 기록 없음",
    "showRecent5" to "최근 5개 표시",
    "showAll" to "모두 표시",
    "clearAll" to "모두 지우기",
    "createRecord" to "기록 생성",
    "updateRecord" to "기록 수정",
    "deleteRecord" to "기록 삭제",
    "noRecordsToday" to "오늘 기록이 없습니다. 위 버튼을 눌러 추가하세요!",
    "confirmDeleteMessage" to "이 기록을 삭제하시겠습니까?",
    "editRecord" to "기록 편집",
    "editingRecord" to "기록 편집 중",
    "left" to "남음",
    "externalLinkWarningTitle" to "외부 링크",
    "externalLinkWarningMessage" to "외부 웹사이트를 방문하려고 합니다:",
    "fieldsToUpdate" to "필드 업데이트",
    "localDataNewerThanFeishu" to "로컬 데이터가 Feishu보다 최신입니다. 먼저 로컬 데이터를 백업하세요",
    "localDataNewerThanCloud" to "로컬 데이터가 클라우드보다 최신입니다. 먼저 로컬 데이터를 업로드하세요",
    "externalLinkContinue" to "계속",
    "externalLinkCancel" to "취소",
    "somethingWentWrong" to "오류가 발생했습니다",
    "unexpectedError" to "예기치 않은 오류가 발생했습니다",
    "tryAgain" to "다시 시도",
    "refreshPage" to "페이지 새로고침",
    "errorDetails" to "오류 세부 정보",
    "howToGetSupabaseApi" to "Supabase API 자격 증명을 어떻게 얻나요?",
    "supabaseGuideTitle" to "Supabase 설정 가이드",
    "whatIsSupabase" to "Supabase란?",
    "supabaseDescription" to "Supabase는 PostgreSQL 데이터베이스, 인증, 실시간 구독 등을 제공하는 오픈소스 Firebase 대안입니다.",
    "supabaseUseCase" to "Supabase를 사용하면 흡연 기록을 여러 기기에서 동기화하고 클라우드에 안전하게 백업할 수 있습니다.",
    "supabaseRequirements" to "필요한 것:",
    "supabaseEmail" to "Supabase 계정을注册하는 데 사용할 이메일 주소",
    "supabaseTime" to "설정 완료까지 약 5~10분",
    "supabaseStep1" to "1단계: Supabase 계정 등록",
    "supabaseStep1Desc1" to "Supabase 웹사이트 방문:",
    "supabaseStep1Desc2" to "오른쪽 상단의 \"Start your project\" 또는 \"Sign In\" 클릭",
    "supabaseStep1Desc3" to "GitHub 계정 또는 이메�로 등록/로그인",
    "supabaseTip" to "💡 팁: Supabase는 개인 사용에 충분한 무료 플랜을 제공합니다.",
    "supabaseStep2" to "2단계: 새 프로젝트 만들기",
    "supabaseStep2Desc1" to "로그인 후 \"New project\" 버튼 클릭",
    "supabaseStep2Desc2" to "조직 선택 또는 만들기",
    "supabaseStep2Desc3" to "프로젝트 정보 입력:",
    "supabaseStep2Desc4" to "Name: 프로젝트 이름 (예: popsmoke-tracker)",
    "supabaseStep2Desc5" to "Database Password: 데이터베이스 비밀번호 설정 (기억하세요)",
    "supabaseStep2Desc6" to "Region: 가장 가까운 서버 지역 선택",
    "supabaseStep2Desc7" to "\"Create new project\"를 클릭하여 프로젝트 만들기",
    "supabaseNote" to "⏳ 참고: 프로젝트 생성에는 1~2분이 소요되므로 잠시 기다려주세요.",
    "supabaseStep3" to "3단계: API 자격 증명 가져오기",
    "supabaseStep3Desc1" to "프로젝트 생성 후 프로젝트 대시보드로 이동",
    "supabaseStep3Desc2" to "왼쪽 메뉴의 \"Project Settings\" 클릭",
    "supabaseStep3Desc3" to "\"API\" 탭 선택",
    "supabaseStep3Desc4" to "\"Project API keys\" 섹션에서 다음 정보를 찾으세요:",
    "supabaseProjectUrl" to "Project URL:",
    "supabaseAnonKey" to "anon public:",
    "supabaseStep4" to "4단계: 이 앱에서 구성",
    "supabaseStep4Desc1" to "\"Project URL\"을 이 앱의 <strong>SUPABASE_URL</strong> 필드에 복사",
    "supabaseStep4Desc2" to "\"anon public\" 키를 이 앱의 <strong>SUPABASE_ANON_KEY</strong> 필드에 복사",
    "supabaseStep4Desc3" to "저장 버튼을 클릭하여 설정 완료",
    "supabaseAfterConfig" to "✅ 설정 후 다음과 같은 것이 가능합니다:",
    "supabaseSyncDevices" to "다른 기기에서 흡연 기록 동기화",
    "supabaseCloudBackup" to "데이터 손실을 방지하는 클라우드 백업",
    "supabaseCloudSync" to "클라우드 동기화를 사용하여 데이터 다운로드/업로드",
    "supabasePrev" to "이전",
    "supabaseNext" to "다음",
    "supabaseComplete" to "완료",
    "newVersionAvailable" to "새 버전 사용 가능",
    "releaseNotes" to "업데이트 내용",
    "updateNow" to "지금 업데이트",
    "later" to "나중에 알림",
    "noReleaseNotes" to "릴리스 노트가 없습니다",
    "updateHint" to "최신 버전으로 업데이트하면 최신 기능과 버그 수정을 이용할 수 있습니다.",
    "recording" to "기록 중...",
    "operationFrequent" to "작업이 너무 빠릅니다. 잠시 후 다시 시도해 주세요",
    "generalSettings" to "일반 설정",
    "storageInfo" to "저장소 정보",
    "checkStorage" to "저장소 확인",
    "changeAvatar" to "아바타 변경",
    "bindEmail" to "이메일 연결",
    "confirmSignOut" to "로그아웃 확인",
    "signOutWarning" to "로그아웃하시겠습니까? 데이터는 로컬에 저장됩니다.",
    "externalLinkWarning" to "외부 웹사이트로 이동합니다. 계속하시겠습니까?",
    "statistics" to "통계 정보",
    "thisMonth" to "이번 달",
    "maxDayRecord" to "최대 일일 기록",
    "chart" to "차트",
    "weekView" to "주간 보기",
    "monthView" to "월간 보기",
    "noData" to "데이터 없음",
    "sync" to "데이터 동기화",
    "syncDescription" to "클라우드 서비스와 데이터를 동기화하여 백업 및 멀티 디바이스 액세스를 구현합니다.",
    "noApiConfig" to "API가 구성되지 않았습니다. 먼저 설정에서 API를 구성하세요.",
    "operationLogs" to "작업 로그",
    "clearLogs" to "로그 삭제",
    "conflicting" to "충돌",
    "syncResult" to "동기화 결과",
    "comingSoon" to "이 화면은 다음 배치에서 구현됩니다",
    "cdThemeColor" to "테마 색상 변경",
    "cdLanguage" to "언어 변경",
    "cdRecentLogs" to "오늘의 기록 보기",
    "cdBackToDashboard" to "대시보드로 돌아가기",
    "cdDeleteRecord" to "기록 삭제",
    "cdRecordNow" to "지금 기록하기",
    "cdExpandMonth" to "월 펼치기",
    "cdCollapseMonth" to "월 접기",
    "cdOpenDatePicker" to "날짜 선택 열기",
    "cdDecrease" to "감소",
    "cdIncrease" to "증가",
    "cdEditRecord" to "기록 시간 수정",
    "opCreate" to "기록 추가",
    "opUpdate" to "시간 수정",
    "opDelete" to "기록 삭제",
    "opClear" to "기록 비우기",
    "opSync" to "백업 가져오기",
    "opSystem" to "시스템",
    "opPause" to "일시정지",
    "opResume" to "계속",
    "opClearLog" to "지우기",
    "opEntries" to "항목",
    "opLive" to "라이브",
    "opPaused" to "일시정지됨",
    "opEmpty" to "작업 기록 없음",
    "backupSection" to "백업 및 복원",
    "backupExport" to "백업 내보내기(JSON)",
    "backupImport" to "백업 가져오기",
    "backupNote" to "모든 기록과 설정이 담긴 JSON 파일입니다. 기기 변경이나 재설치 후 복원하세요.",
    "backupExported" to "내보냈습니다",
    "backupInvalid" to "유효한 백업 파일이 아닙니다",
    "importModeTitle" to "가져오기 방식",
    "importModeMessage" to "병합은 현재 기록을 유지하고 빠진 기록을 추가합니다. 교체는 설정까지 전부 바꿉니다.",
    "importMerge" to "병합",
    "importReplace" to "전부 교체",
    "backupImported" to "가져왔습니다",
    "cloudAccount" to "클라우드 계정",
    "cloudAccountHint" to "Supabase로 로그인하면 일일 한도·경고 기준·테마를 기기 간에 동기화합니다.",
    "cloudNotConfigured" to "Supabase가 설정되지 않았습니다. API 페이지에서 URL과 anon key를 입력하세요.",
    "cloudSettingsUp" to "설정 업로드",
    "cloudSettingsDown" to "설정 복원",
    "cloudConflictTitle" to "설정이 다릅니다",
    "cloudConflictMessage" to "클라우드 설정이 이 기기와 다릅니다. 어느 쪽을 사용할까요?",
    "cloudUseCloud" to "클라우드 사용",
    "cloudKeepLocal" to "로컬 유지",
    "feishuBackupNow" to "Feishu로 백업",
    "feishuRestoreNow" to "Feishu에서 복원",
    "feishuSyncHint" to "시트에 없는 기록만 업로드하며 id로 중복을 걸러냅니다.",
    "loginAction" to "로그인",
    "emailUnverified" to "확인 메일을 보냈습니다. 메일 확인 후 로그인하세요.",
    "cloudUploaded" to "{n}건 업로드",
    "cloudSkipped" to "{n}건 기존",
    "cloudFailedCount" to "{n}건 실패",
    "cloudRestoredCount" to "새 기록 {n}건",
    "cloudUpToDate" to "이미 최신입니다",
    "cloudDownloaded" to "{n}건 다운로드",
    "syncNothing" to "이미 최신입니다",
    "syncPendingCount" to "대기 {n}건",
    "syncNow" to "지금 동기화",
    "syncQueue" to "동기화 큐",
    "syncQueueEmpty" to "큐에 작업 없음",
    "syncAll" to "전체 동기화",
    "syncOne" to "동기화",
    "syncRemove" to "제거",
    "syncClearQueue" to "비우기",
    "syncEntries" to "항목",
    "syncPendingState" to "대기중",
    "syncFailedState" to "실패",
    "syncProcessing" to "처리중",
    "syncReady" to "준비됨",
    "syncPending" to "대기",
    "syncClearConfirm" to "큐를 모두 비우시겠습니까?",
    "syncHasFailedHint" to "동기화에 실패한 변경이 있습니다. 아래 큐에서 다시 시도하세요",
    "syncReadyHint" to "로컬 변경이 모두 동기화되었습니다",
    "syncRemoveConfirm" to "이 작업을 큐에서 제거하시겠습니까?",
    "signedInAs" to "로그인됨",
    "issueNetwork" to "네트워크에 연결할 수 없습니다.",
    "issueCredentials" to "이메일 또는 비밀번호가 틀렸습니다.",
    "issueEmailNotConfirmed" to "이메일이 아직 확인되지 않았습니다.",
    "issueSessionExpired" to "세션이 만료되었습니다. 다시 로그인하세요.",
    "issueServer" to "서버 오류입니다. 잠시 후 다시 시도하세요.",
    "issueTableMissing" to "테이블을 찾을 수 없습니다. Supabase 스키마를 확인하세요.",
    "issueBadResponse" to "서버 응답을 해석할 수 없습니다.",
    "issueNoConfig" to "API가 설정되지 않았습니다.",
    "issueWeakPassword" to "비밀번호는 6자 이상이어야 합니다.",
    "issueAlreadyRegistered" to "이미 등록된 이메일입니다.",
    "issueMissingFields" to "이메일과 비밀번호를 입력하세요.",
    "issueNoCloudSettings" to "클라우드에 저장된 설정이 없습니다.",
    "issueRateLimited" to "시도가 너무 많습니다. 잠시 후 다시 시도하세요.",
    "issueUnknown" to "문제가 발생했습니다.",
)

/** 类型安全的文案访问器：每个键都是一个只读属性，拼错编译期就报错。 */
class PopStrings(private val table: Map<String, String>) {
    /** 取文案；本语言缺失时回落英文，仍缺失则返回键名（截图里一眼能看见）。 */
    operator fun get(key: String): String = table[key] ?: EN_TABLE[key] ?: key

    val appTitle: String get() = get("appTitle")
    val appSubtitle: String get() = get("appSubtitle")
    val tracker: String get() = get("tracker")
    val notificationSuccess: String get() = get("notificationSuccess")
    val notificationError: String get() = get("notificationError")
    val notificationInfo: String get() = get("notificationInfo")
    val notificationWarning: String get() = get("notificationWarning")
    val analysis: String get() = get("analysis")
    val settings: String get() = get("settings")
    val history: String get() = get("history")
    val lastRecord: String get() = get("lastRecord")
    val smokeButton: String get() = get("smokeButton")
    val smokes: String get() = get("smokes")
    val clickToLog: String get() = get("clickToLog")
    val todayLogs: String get() = get("todayLogs")
    val noRecords: String get() = get("noRecords")
    val recentLogs: String get() = get("recentLogs")
    val last24Hours: String get() = get("last24Hours")
    val currentGaugeLevel: String get() = get("currentGaugeLevel")
    val smokeUnit: String get() = get("smokeUnit")
    val consecutiveDaysWithinLimit: String get() = get("consecutiveDaysWithinLimit")
    val days: String get() = get("days")
    val lateNight: String get() = get("lateNight")
    val earlyMorning: String get() = get("earlyMorning")
    val morning: String get() = get("morning")
    val noon: String get() = get("noon")
    val afternoon: String get() = get("afternoon")
    val evening: String get() = get("evening")
    val dailyLimit: String get() = get("dailyLimit")
    val warningLimit: String get() = get("warningLimit")
    val thresholdSettings: String get() = get("thresholdSettings")
    val themeColor: String get() = get("themeColor")
    val language: String get() = get("language")
    val customHex: String get() = get("customHex")
    val customColor: String get() = get("customColor")
    val theme: String get() = get("theme")
    val warningMsg: String get() = get("warningMsg")
    val limitReachedMsg: String get() = get("limitReachedMsg")
    val totalCigarettes: String get() = get("totalCigarettes")
    val exportCSV: String get() = get("exportCSV")
    val csvNote: String get() = get("csvNote")
    val noDataToExport: String get() = get("noDataToExport")
    val export: String get() = get("export")
    val exportSuccess: String get() = get("exportSuccess")
    val exportFailed: String get() = get("exportFailed")
    val weeklyFreq: String get() = get("weeklyFreq")
    val monthlyFreq: String get() = get("monthlyFreq")
    val thisWeek: String get() = get("thisWeek")
    val weekData: String get() = get("weekData")
    val monthData: String get() = get("monthData")
    val weekTotal: String get() = get("weekTotal")
    val mostSmokedDay: String get() = get("mostSmokedDay")
    val monthTotal: String get() = get("monthTotal")
    val lastMonthTotal: String get() = get("lastMonthTotal")
    val mostSmokedDate: String get() = get("mostSmokedDate")
    val day: String get() = get("day")
    val mon: String get() = get("mon")
    val tue: String get() = get("tue")
    val wed: String get() = get("wed")
    val thu: String get() = get("thu")
    val fri: String get() = get("fri")
    val sat: String get() = get("sat")
    val sun: String get() = get("sun")
    val more: String get() = get("more")
    val back: String get() = get("back")
    val next: String get() = get("next")
    val prev: String get() = get("prev")
    val delete: String get() = get("delete")
    val edit: String get() = get("edit")
    val rename: String get() = get("rename")
    val reset: String get() = get("reset")
    val close: String get() = get("close")
    val hide: String get() = get("hide")
    val save: String get() = get("save")
    val cancel: String get() = get("cancel")
    val confirm: String get() = get("confirm")
    val confirmDelete: String get() = get("confirmDelete")
    val editTime: String get() = get("editTime")
    val never: String get() = get("never")
    val view: String get() = get("view")
    val about: String get() = get("about")
    val blog: String get() = get("blog")
    val github: String get() = get("github")
    val stats: String get() = get("stats")
    val data: String get() = get("data")
    val hour: String get() = get("hour")
    val minute: String get() = get("minute")
    val count: String get() = get("count")
    val clearLocalData: String get() = get("clearLocalData")
    val currentMonth: String get() = get("currentMonth")
    val prevMonth: String get() = get("prevMonth")
    val nextMonth: String get() = get("nextMonth")
    val confirmClearData: String get() = get("confirmClearData")
    val cancelClearData: String get() = get("cancelClearData")
    val clearDataWarning: String get() = get("clearDataWarning")
    val clearDataMessage: String get() = get("clearDataMessage")
    val clearData: String get() = get("clearData")
    val clearDataConfirm: String get() = get("clearDataConfirm")
    val yesClear: String get() = get("yesClear")
    val loadMore: String get() = get("loadMore")
    val storageError: String get() = get("storageError")
    val storageErrorHint: String get() = get("storageErrorHint")
    val goToSettings: String get() = get("goToSettings")
    val dismiss: String get() = get("dismiss")
    val filtersAndSort: String get() = get("filtersAndSort")
    val startDate: String get() = get("startDate")
    val endDate: String get() = get("endDate")
    val minCount: String get() = get("minCount")
    val maxCount: String get() = get("maxCount")
    val sortByDate: String get() = get("sortByDate")
    val sortByCount: String get() = get("sortByCount")
    val resetFilters: String get() = get("resetFilters")
    val noMatchingRecords: String get() = get("noMatchingRecords")
    val totalRecords: String get() = get("totalRecords")
    val filteredRecords: String get() = get("filteredRecords")
    val dateRange: String get() = get("dateRange")
    val averagePerDay: String get() = get("averagePerDay")
    val prevPage: String get() = get("prevPage")
    val nextPage: String get() = get("nextPage")
    val feishuApi: String get() = get("feishuApi")
    val feishuApiSettings: String get() = get("feishuApiSettings")
    val feishuDataSync: String get() = get("feishuDataSync")
    val apiUrl: String get() = get("apiUrl")
    val syncFromFeishu: String get() = get("syncFromFeishu")
    val syncToFeishu: String get() = get("syncToFeishu")
    val startSync: String get() = get("startSync")
    val syncing: String get() = get("syncing")
    val targetTableName: String get() = get("targetTableName")
    val accessKey: String get() = get("accessKey")
    val accessKeyDialogTitle: String get() = get("accessKeyDialogTitle")
    val accessKeyDialogMessage: String get() = get("accessKeyDialogMessage")
    val submitToFeishu: String get() = get("submitToFeishu")
    val submitting: String get() = get("submitting")
    val startSubmit: String get() = get("startSubmit")
    val syncFromSupabaseOnly: String get() = get("syncFromSupabaseOnly")
    val syncDiffSummary: String get() = get("syncDiffSummary")
    val cloudOnlyRecords: String get() = get("cloudOnlyRecords")
    val confirmSync: String get() = get("confirmSync")
    val localRecords: String get() = get("localRecords")
    val cloudRecords: String get() = get("cloudRecords")
    val localOnly: String get() = get("localOnly")
    val cloudOnly: String get() = get("cloudOnly")
    val conflicts: String get() = get("conflicts")
    val upload: String get() = get("upload")
    val download: String get() = get("download")
    val uploadOnly: String get() = get("uploadOnly")
    val downloadOnly: String get() = get("downloadOnly")
    val foundNewRecords: String get() = get("foundNewRecords")
    val foundConflictingRecords: String get() = get("foundConflictingRecords")
    val needUploadRecords: String get() = get("needUploadRecords")
    val needDownloadRecords: String get() = get("needDownloadRecords")
    val uploadCount: String get() = get("uploadCount")
    val downloadCount: String get() = get("downloadCount")
    val syncCompleted: String get() = get("syncCompleted")
    val syncWithFeishuCompleted: String get() = get("syncWithFeishuCompleted")
    val syncWithSupabaseCompleted: String get() = get("syncWithSupabaseCompleted")
    val cloudSync: String get() = get("cloudSync")
    val cloudSyncing: String get() = get("cloudSyncing")
    val loginFirst: String get() = get("loginFirst")
    val syncSuccess: String get() = get("syncSuccess")
    val syncFailed: String get() = get("syncFailed")
    val noNewRecordsToSync: String get() = get("noNewRecordsToSync")
    val uploaded: String get() = get("uploaded")
    val downloaded: String get() = get("downloaded")
    val records: String get() = get("records")
    val feishuSyncSuccess: String get() = get("feishuSyncSuccess")
    val feishuSyncToStorage: String get() = get("feishuSyncToStorage")
    val feishuSyncToCloud: String get() = get("feishuSyncToCloud")
    val syncedRecordsFromFeishu: String get() = get("syncedRecordsFromFeishu")
    val importedCount: String get() = get("importedCount")
    val totalCount: String get() = get("totalCount")
    val submittedCount: String get() = get("submittedCount")
    val failedCount: String get() = get("failedCount")
    val signIn: String get() = get("signIn")
    val signUp: String get() = get("signUp")
    val signOut: String get() = get("signOut")
    val email: String get() = get("email")
    val password: String get() = get("password")
    val passwordPlaceholder: String get() = get("passwordPlaceholder")
    val noAccount: String get() = get("noAccount")
    val haveAccount: String get() = get("haveAccount")
    val loading: String get() = get("loading")
    val initializing: String get() = get("initializing")
    val connecting: String get() = get("connecting")
    val authenticating: String get() = get("authenticating")
    val resetting: String get() = get("resetting")
    val restoring: String get() = get("restoring")
    val skipLogin: String get() = get("skipLogin")
    val verifyEmail: String get() = get("verifyEmail")
    val localModeHint: String get() = get("localModeHint")
    val accountManagement: String get() = get("accountManagement")
    val uploadAvatar: String get() = get("uploadAvatar")
    val uploading: String get() = get("uploading")
    val emailPlaceholder: String get() = get("emailPlaceholder")
    val updateEmail: String get() = get("updateEmail")
    val phone: String get() = get("phone")
    val phonePlaceholder: String get() = get("phonePlaceholder")
    val updatePhone: String get() = get("updatePhone")
    val resetPassword: String get() = get("resetPassword")
    val currentPassword: String get() = get("currentPassword")
    val newPassword: String get() = get("newPassword")
    val confirmPassword: String get() = get("confirmPassword")
    val passwordMinLength: String get() = get("passwordMinLength")
    val passwordMismatch: String get() = get("passwordMismatch")
    val passwordResetSuccess: String get() = get("passwordResetSuccess")
    val passwordResetFailed: String get() = get("passwordResetFailed")
    val currentPasswordIncorrect: String get() = get("currentPasswordIncorrect")
    val signOutConfirm: String get() = get("signOutConfirm")
    val cloudDataFound: String get() = get("cloudDataFound")
    val cloudDataFoundMessage: String get() = get("cloudDataFoundMessage")
    val downloadFromCloud: String get() = get("downloadFromCloud")
    val skipDownload: String get() = get("skipDownload")
    val loginRequired: String get() = get("loginRequired")
    val loginRequiredMessage: String get() = get("loginRequiredMessage")
    val loginNow: String get() = get("loginNow")
    val selectDataSource: String get() = get("selectDataSource")
    val selectDataSourceMessage: String get() = get("selectDataSourceMessage")
    val apiManagement: String get() = get("apiManagement")
    val api: String get() = get("api")
    val saveApiSettings: String get() = get("saveApiSettings")
    val securityPassword: String get() = get("securityPassword")
    val enterPassword: String get() = get("enterPassword")
    val selectSyncSource: String get() = get("selectSyncSource")
    val firstLaunchSyncMessage: String get() = get("firstLaunchSyncMessage")
    val feishu: String get() = get("feishu")
    val supabase: String get() = get("supabase")
    val storageMethod: String get() = get("storageMethod")
    val dataManagement: String get() = get("dataManagement")
    val storageUsage: String get() = get("storageUsage")
    val clearOldData: String get() = get("clearOldData")
    val systemLog: String get() = get("systemLog")
    val viewSystemLogs: String get() = get("viewSystemLogs")
    val apiUrlDisplay: String get() = get("apiUrlDisplay")
    val apiConfigRequired: String get() = get("apiConfigRequired")
    val deleteApiSettings: String get() = get("deleteApiSettings")
    val confirmDeleteApiSettings: String get() = get("confirmDeleteApiSettings")
    val renameApiSettings: String get() = get("renameApiSettings")
    val resetApiSettings: String get() = get("resetApiSettings")
    val enterNewPassword: String get() = get("enterNewPassword")
    val apiSettingsVerifyPassword: String get() = get("apiSettingsVerifyPassword")
    val viewSavedApiSettings: String get() = get("viewSavedApiSettings")
    val previousLoginFound: String get() = get("previousLoginFound")
    val previousLoginFoundMessage: String get() = get("previousLoginFoundMessage")
    val restoreLogin: String get() = get("restoreLogin")
    val continueLocalMode: String get() = get("continueLocalMode")
    val enterSecurityPasswordForRestore: String get() = get("enterSecurityPasswordForRestore")
    val clear: String get() = get("clear")
    val today: String get() = get("today")
    val selectDate: String get() = get("selectDate")
    val selectStartDate: String get() = get("selectStartDate")
    val selectEndDate: String get() = get("selectEndDate")
    val january: String get() = get("january")
    val february: String get() = get("february")
    val march: String get() = get("march")
    val april: String get() = get("april")
    val may: String get() = get("may")
    val june: String get() = get("june")
    val july: String get() = get("july")
    val august: String get() = get("august")
    val september: String get() = get("september")
    val october: String get() = get("october")
    val november: String get() = get("november")
    val december: String get() = get("december")
    val sunday: String get() = get("sunday")
    val monday: String get() = get("monday")
    val tuesday: String get() = get("tuesday")
    val wednesday: String get() = get("wednesday")
    val thursday: String get() = get("thursday")
    val friday: String get() = get("friday")
    val saturday: String get() = get("saturday")
    val presetColors: String get() = get("presetColors")
    val fontAttribution: String get() = get("fontAttribution")
    val anonKey: String get() = get("anonKey")
    val operationLog: String get() = get("operationLog")
    val feishuPasswordRequired: String get() = get("feishuPasswordRequired")
    val apiPasswordRequired: String get() = get("apiPasswordRequired")
    val passwordError: String get() = get("passwordError")
    val dataCleared: String get() = get("dataCleared")
    val bucketNotExist: String get() = get("bucketNotExist")
    val permissionDenied: String get() = get("permissionDenied")
    val avatarUploadSuccess: String get() = get("avatarUploadSuccess")
    val avatarUploadFailed: String get() = get("avatarUploadFailed")
    val enterEmail: String get() = get("enterEmail")
    val emailBindSuccess: String get() = get("emailBindSuccess")
    val emailBindFailed: String get() = get("emailBindFailed")
    val passwordSameAsCurrent: String get() = get("passwordSameAsCurrent")
    val passwordRequirements: String get() = get("passwordRequirements")
    val passwordRequirement: String get() = get("passwordRequirement")
    val passwordTooShort: String get() = get("passwordTooShort")
    val resetSuccess: String get() = get("resetSuccess")
    val resetFailed: String get() = get("resetFailed")
    val confirmPasswordPlaceholder: String get() = get("confirmPasswordPlaceholder")
    val accountSettings: String get() = get("accountSettings")
    val `continue`: String get() = get("continue")
    val signOutFailed: String get() = get("signOutFailed")
    val used: String get() = get("used")
    val mb: String get() = get("mb")
    val chinese: String get() = get("chinese")
    val japanese: String get() = get("japanese")
    val korean: String get() = get("korean")
    val noOperationRecords: String get() = get("noOperationRecords")
    val showRecent5: String get() = get("showRecent5")
    val showAll: String get() = get("showAll")
    val clearAll: String get() = get("clearAll")
    val createRecord: String get() = get("createRecord")
    val updateRecord: String get() = get("updateRecord")
    val deleteRecord: String get() = get("deleteRecord")
    val noRecordsToday: String get() = get("noRecordsToday")
    val confirmDeleteMessage: String get() = get("confirmDeleteMessage")
    val editRecord: String get() = get("editRecord")
    val editingRecord: String get() = get("editingRecord")
    val left: String get() = get("left")
    val externalLinkWarningTitle: String get() = get("externalLinkWarningTitle")
    val externalLinkWarningMessage: String get() = get("externalLinkWarningMessage")
    val fieldsToUpdate: String get() = get("fieldsToUpdate")
    val localDataNewerThanFeishu: String get() = get("localDataNewerThanFeishu")
    val localDataNewerThanCloud: String get() = get("localDataNewerThanCloud")
    val externalLinkContinue: String get() = get("externalLinkContinue")
    val externalLinkCancel: String get() = get("externalLinkCancel")
    val somethingWentWrong: String get() = get("somethingWentWrong")
    val unexpectedError: String get() = get("unexpectedError")
    val tryAgain: String get() = get("tryAgain")
    val refreshPage: String get() = get("refreshPage")
    val errorDetails: String get() = get("errorDetails")
    val howToGetSupabaseApi: String get() = get("howToGetSupabaseApi")
    val supabaseGuideTitle: String get() = get("supabaseGuideTitle")
    val whatIsSupabase: String get() = get("whatIsSupabase")
    val supabaseDescription: String get() = get("supabaseDescription")
    val supabaseUseCase: String get() = get("supabaseUseCase")
    val supabaseRequirements: String get() = get("supabaseRequirements")
    val supabaseEmail: String get() = get("supabaseEmail")
    val supabaseTime: String get() = get("supabaseTime")
    val supabaseStep1: String get() = get("supabaseStep1")
    val supabaseStep1Desc1: String get() = get("supabaseStep1Desc1")
    val supabaseStep1Desc2: String get() = get("supabaseStep1Desc2")
    val supabaseStep1Desc3: String get() = get("supabaseStep1Desc3")
    val supabaseTip: String get() = get("supabaseTip")
    val supabaseStep2: String get() = get("supabaseStep2")
    val supabaseStep2Desc1: String get() = get("supabaseStep2Desc1")
    val supabaseStep2Desc2: String get() = get("supabaseStep2Desc2")
    val supabaseStep2Desc3: String get() = get("supabaseStep2Desc3")
    val supabaseStep2Desc4: String get() = get("supabaseStep2Desc4")
    val supabaseStep2Desc5: String get() = get("supabaseStep2Desc5")
    val supabaseStep2Desc6: String get() = get("supabaseStep2Desc6")
    val supabaseStep2Desc7: String get() = get("supabaseStep2Desc7")
    val supabaseNote: String get() = get("supabaseNote")
    val supabaseStep3: String get() = get("supabaseStep3")
    val supabaseStep3Desc1: String get() = get("supabaseStep3Desc1")
    val supabaseStep3Desc2: String get() = get("supabaseStep3Desc2")
    val supabaseStep3Desc3: String get() = get("supabaseStep3Desc3")
    val supabaseStep3Desc4: String get() = get("supabaseStep3Desc4")
    val supabaseProjectUrl: String get() = get("supabaseProjectUrl")
    val supabaseAnonKey: String get() = get("supabaseAnonKey")
    val supabaseStep4: String get() = get("supabaseStep4")
    val supabaseStep4Desc1: String get() = get("supabaseStep4Desc1")
    val supabaseStep4Desc2: String get() = get("supabaseStep4Desc2")
    val supabaseStep4Desc3: String get() = get("supabaseStep4Desc3")
    val supabaseAfterConfig: String get() = get("supabaseAfterConfig")
    val supabaseSyncDevices: String get() = get("supabaseSyncDevices")
    val supabaseCloudBackup: String get() = get("supabaseCloudBackup")
    val supabaseCloudSync: String get() = get("supabaseCloudSync")
    val supabasePrev: String get() = get("supabasePrev")
    val supabaseNext: String get() = get("supabaseNext")
    val supabaseComplete: String get() = get("supabaseComplete")
    val newVersionAvailable: String get() = get("newVersionAvailable")
    val releaseNotes: String get() = get("releaseNotes")
    val updateNow: String get() = get("updateNow")
    val later: String get() = get("later")
    val noReleaseNotes: String get() = get("noReleaseNotes")
    val updateHint: String get() = get("updateHint")
    val recording: String get() = get("recording")
    val operationFrequent: String get() = get("operationFrequent")
    val generalSettings: String get() = get("generalSettings")
    val storageInfo: String get() = get("storageInfo")
    val checkStorage: String get() = get("checkStorage")
    val changeAvatar: String get() = get("changeAvatar")
    val bindEmail: String get() = get("bindEmail")
    val confirmSignOut: String get() = get("confirmSignOut")
    val signOutWarning: String get() = get("signOutWarning")
    val externalLinkWarning: String get() = get("externalLinkWarning")
    val statistics: String get() = get("statistics")
    val thisMonth: String get() = get("thisMonth")
    val maxDayRecord: String get() = get("maxDayRecord")
    val chart: String get() = get("chart")
    val weekView: String get() = get("weekView")
    val monthView: String get() = get("monthView")
    val noData: String get() = get("noData")
    val sync: String get() = get("sync")
    val syncDescription: String get() = get("syncDescription")
    val noApiConfig: String get() = get("noApiConfig")
    val operationLogs: String get() = get("operationLogs")
    val clearLogs: String get() = get("clearLogs")
    val conflicting: String get() = get("conflicting")
    val syncResult: String get() = get("syncResult")
    val comingSoon: String get() = get("comingSoon")
    val cdThemeColor: String get() = get("cdThemeColor")
    val cdLanguage: String get() = get("cdLanguage")
    val cdRecentLogs: String get() = get("cdRecentLogs")
    val cdBackToDashboard: String get() = get("cdBackToDashboard")
    val cdDeleteRecord: String get() = get("cdDeleteRecord")
    val cdRecordNow: String get() = get("cdRecordNow")
    val cdExpandMonth: String get() = get("cdExpandMonth")
    val cdCollapseMonth: String get() = get("cdCollapseMonth")
    val cdOpenDatePicker: String get() = get("cdOpenDatePicker")
    val cdDecrease: String get() = get("cdDecrease")
    val cdIncrease: String get() = get("cdIncrease")
    val cdEditRecord: String get() = get("cdEditRecord")
    val opCreate: String get() = get("opCreate")
    val opUpdate: String get() = get("opUpdate")
    val opDelete: String get() = get("opDelete")
    val opClear: String get() = get("opClear")
    val opSync: String get() = get("opSync")
    val opSystem: String get() = get("opSystem")
    val opPause: String get() = get("opPause")
    val opResume: String get() = get("opResume")
    val opClearLog: String get() = get("opClearLog")
    val opEntries: String get() = get("opEntries")
    val opLive: String get() = get("opLive")
    val opPaused: String get() = get("opPaused")
    val opEmpty: String get() = get("opEmpty")
    val backupSection: String get() = get("backupSection")
    val backupExport: String get() = get("backupExport")
    val backupImport: String get() = get("backupImport")
    val backupNote: String get() = get("backupNote")
    val backupExported: String get() = get("backupExported")
    val backupInvalid: String get() = get("backupInvalid")
    val importModeTitle: String get() = get("importModeTitle")
    val importModeMessage: String get() = get("importModeMessage")
    val importMerge: String get() = get("importMerge")
    val importReplace: String get() = get("importReplace")
    val backupImported: String get() = get("backupImported")
    val cloudAccount: String get() = get("cloudAccount")
    val cloudAccountHint: String get() = get("cloudAccountHint")
    val cloudNotConfigured: String get() = get("cloudNotConfigured")
    val cloudSettingsUp: String get() = get("cloudSettingsUp")
    val cloudSettingsDown: String get() = get("cloudSettingsDown")
    val cloudConflictTitle: String get() = get("cloudConflictTitle")
    val cloudConflictMessage: String get() = get("cloudConflictMessage")
    val cloudUseCloud: String get() = get("cloudUseCloud")
    val cloudKeepLocal: String get() = get("cloudKeepLocal")
    val feishuBackupNow: String get() = get("feishuBackupNow")
    val feishuRestoreNow: String get() = get("feishuRestoreNow")
    val feishuSyncHint: String get() = get("feishuSyncHint")
    val loginAction: String get() = get("loginAction")
    val emailUnverified: String get() = get("emailUnverified")
    val cloudUploaded: String get() = get("cloudUploaded")
    val cloudSkipped: String get() = get("cloudSkipped")
    val cloudFailedCount: String get() = get("cloudFailedCount")
    val cloudRestoredCount: String get() = get("cloudRestoredCount")
    val cloudUpToDate: String get() = get("cloudUpToDate")
    val cloudDownloaded: String get() = get("cloudDownloaded")
    val syncNothing: String get() = get("syncNothing")
    val syncPendingCount: String get() = get("syncPendingCount")
    val syncNow: String get() = get("syncNow")
    val syncQueue: String get() = get("syncQueue")
    val syncQueueEmpty: String get() = get("syncQueueEmpty")
    val syncAll: String get() = get("syncAll")
    val syncOne: String get() = get("syncOne")
    val syncRemove: String get() = get("syncRemove")
    val syncClearQueue: String get() = get("syncClearQueue")
    val syncEntries: String get() = get("syncEntries")
    val syncPendingState: String get() = get("syncPendingState")
    val syncFailedState: String get() = get("syncFailedState")
    val syncProcessing: String get() = get("syncProcessing")
    val syncReady: String get() = get("syncReady")
    val syncPending: String get() = get("syncPending")
    val syncClearConfirm: String get() = get("syncClearConfirm")
    val syncHasFailedHint: String get() = get("syncHasFailedHint")
    val syncReadyHint: String get() = get("syncReadyHint")
    val syncRemoveConfirm: String get() = get("syncRemoveConfirm")
    val signedInAs: String get() = get("signedInAs")
    val issueNetwork: String get() = get("issueNetwork")
    val issueCredentials: String get() = get("issueCredentials")
    val issueEmailNotConfirmed: String get() = get("issueEmailNotConfirmed")
    val issueSessionExpired: String get() = get("issueSessionExpired")
    val issueServer: String get() = get("issueServer")
    val issueTableMissing: String get() = get("issueTableMissing")
    val issueBadResponse: String get() = get("issueBadResponse")
    val issueNoConfig: String get() = get("issueNoConfig")
    val issueWeakPassword: String get() = get("issueWeakPassword")
    val issueAlreadyRegistered: String get() = get("issueAlreadyRegistered")
    val issueMissingFields: String get() = get("issueMissingFields")
    val issueNoCloudSettings: String get() = get("issueNoCloudSettings")
    val issueRateLimited: String get() = get("issueRateLimited")
    val issueUnknown: String get() = get("issueUnknown")
}

val EN = PopStrings(EN_TABLE)
val ZH = PopStrings(ZH_TABLE)
val JA = PopStrings(JA_TABLE)
val KO = PopStrings(KO_TABLE)

fun popStrings(language: String): PopStrings = when (language) {
    "zh" -> ZH
    "ja" -> JA
    "ko" -> KO
    else -> EN
}

/** 时段标签的键（见 DayPart.labelKey）→ 本地化文案。 */
fun PopStrings.dayPartLabel(key: String): String = when (key) {
    "lateNight" -> lateNight
    "earlyMorning" -> earlyMorning
    "morning" -> morning
    "noon" -> noon
    "afternoon" -> afternoon
    else -> evening
}

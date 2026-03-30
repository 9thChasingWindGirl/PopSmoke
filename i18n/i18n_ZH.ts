import { Language } from '../types';

export const i18n_ZH = {
  // ============ App Level / Common ============
  appTitle: '吸烟',
  appSubtitle: '记录',
  tracker: '记录',
  notificationSuccess: '成功！',
  notificationError: '错误！',
  notificationInfo: '信息',
  notificationWarning: '警告！',
  
  // ============ Navigation ============
  analysis: '分析',
  settings: '设置',
  history: '历史记录',
  
  // ============ Dashboard ============
  lastRecord: '上次记录',
  smokeButton: '吸烟',
  clickToLog: '点击记录',
  todayLogs: '今日记录',
  noRecords: '今日暂无记录',
  recentLogs: '最近记录',
  last24Hours: '过去24小时的活动',
  currentGaugeLevel: '今日吸烟数量',
  smokeUnit: '支',
  consecutiveDaysWithinLimit: '连续达标',
  days: '天',
  smokes: '根',
  
  // ============ Time Periods ============
  lateNight: '深夜',
  earlyMorning: '清晨',
  morning: '上午',
  noon: '中午',
  afternoon: '下午',
  evening: '晚上',
  
  // ============ Settings - Threshold ============
  dailyLimit: '每日上限',
  warningLimit: '提醒阈值',
  thresholdSettings: '阈值设置',
  
  // ============ Settings - Theme & Language ============
  themeColor: '主题颜色',
  language: '语言 / Language',
  customHex: '自定义颜色',
  customColor: '自定义颜色',
  theme: '主题',
  
  // ============ Dashboard - Messages ============
  warningMsg: '警告：接近每日上限！',
  limitReachedMsg: '已达每日上限！',
  
  // ============ Analysis - Export ============
  totalCigarettes: '吸烟总数',
  exportCSV: '导出 CSV',
  csvNote: '包含日期、时间及月度标签。',
  noDataToExport: '当前月份没有数据可导出。',
  export: '导出',
  exportSuccess: '导出成功',
  exportFailed: '导出失败',
  
  // ============ Analysis - Statistics ============
  weeklyFreq: '每周',
  monthlyFreq: '每月',
  thisWeek: '本周',
  weekData: '周数据',
  monthData: '月数据',
  weekTotal: '本周总数',
  mostSmokedDay: '吸烟最多',
  monthTotal: '本月总数',
  lastMonthTotal: '上月总数',
  mostSmokedDate: '吸烟最多',
  
  // ============ Calendar / Weekdays ============
  day: '日',
  mon: '周一',
  tue: '周二',
  wed: '周三',
  thu: '周四',
  fri: '周五',
  sat: '周六',
  sun: '周日',
  
  // ============ Common Actions ============
  more: '更多 / 编辑',
  back: '返回',
  next: '下一页',
  prev: '上一页',
  delete: '删除',
  edit: '编辑',
  rename: '重命名',
  reset: '重置',
  close: '关闭',
  hide: '隐藏',
  save: '保存',
  cancel: '取消',
  confirm: '确认',
  confirmDelete: '确定要删除这条记录吗？',
  editTime: '修改时间',
  never: '从未',
  view: '视图',
  about: '关于',
  blog: '博客',
  github: 'GitHub',
  stats: '统计',
  data: '数据',
  hour: '时',
  minute: '分',
  count: '数量',
  
  // ============ History Page ============
  clearLocalData: '清除本地数据',
  currentMonth: '当月',
  prevMonth: '上月',
  nextMonth: '下月',
  confirmClearData: '确认清除',
  cancelClearData: '取消',
  clearDataWarning: '警告！',
  clearDataMessage: '确定要清除所有本地数据吗？此操作不可恢复！',
  clearData: '清除数据',
  clearDataConfirm: '这将清除所有吸烟记录。此操作无法撤销。您确定吗？',
  yesClear: '确认清除',
  loadMore: '加载更多',
  
  // ============ Storage / Error ============
  storageError: '存储错误',
  storageErrorHint: '请前往设置清除一些旧数据以释放空间。',
  goToSettings: '前往设置',
  dismiss: '关闭',
  
  // ============ History - Filters & Sort ============
  filtersAndSort: '筛选和排序',
  startDate: '开始日期',
  endDate: '结束日期',
  minCount: '最小数量',
  maxCount: '最大数量',
  sortByDate: '按日期排序',
  sortByCount: '按数量排序',
  resetFilters: '重置筛选',
  noMatchingRecords: '没有匹配的记录',
  totalRecords: '总记录数',
  filteredRecords: '筛选后记录数',
  dateRange: '日期范围',
  averagePerDay: '平均每天',
  prevPage: '上一页',
  nextPage: '下一页',
  
  // ============ API Management - Feishu ============
  feishuApi: '飞书 API',
  feishuApiSettings: '飞书API设置',
  feishuDataSync: '飞书数据同步',
  apiUrl: 'API地址',
  syncFromFeishu: '从飞书同步到本地',
  syncToFeishu: '同步到飞书',
  startSync: '开始同步',
  syncing: '同步中...',
  targetTableName: '目标表名',
  accessKey: '访问密钥',
  accessKeyDialogTitle: '认证密码',
  accessKeyDialogMessage: '请输入飞书API写入访问密钥:',
  submitToFeishu: '提交到飞书',
  submitting: '提交中...',
  startSubmit: '开始提交',
  syncFromSupabaseOnly: '与Supabase同步',
  syncDiffSummary: '同步摘要',
  cloudOnlyRecords: '仅云端记录',
  confirmSync: '确认同步',
  localRecords: '本地记录',
  cloudRecords: '云端记录',
  localOnly: '仅本地',
  cloudOnly: '仅云端',
  conflicts: '冲突',
  upload: '上传',
  download: '下载',
  uploadOnly: '仅上传',
  downloadOnly: '仅下载',
  foundNewRecords: '发现 {count} 条新记录',
  foundConflictingRecords: '{count} 条冲突记录',
  needUploadRecords: '需要上传 {count} 条记录',
  needDownloadRecords: '需要下载 {count} 条记录',
  uploadCount: '上传 {count} 条',
  downloadCount: '下载 {count} 条',
  syncCompleted: '与{source}同步完成：{message}',
  syncWithFeishuCompleted: '与飞书同步完成',
  syncWithSupabaseCompleted: '与Supabase同步完成',
  
  // ============ API Management - Cloud Sync ============
  cloudSync: '云同步',
  cloudSyncing: '同步中...',
  loginFirst: '请先登录后再进行云同步',
  syncSuccess: '同步成功',
  syncFailed: '同步失败',
  noNewRecordsToSync: '没有新的记录需要同步',
  uploaded: '上传',
  downloaded: '下载',
  records: '条',
  
  // ============ API Management - Feishu Sync Messages ============
  feishuSyncSuccess: '飞书API同步成功',
  feishuSyncToStorage: '飞书API同步到本地存储成功',
  feishuSyncToCloud: '飞书API同步并上传到Supabase成功',
  syncedRecordsFromFeishu: '从飞书同步了 {newCount} 条新记录（总计：{totalCount} 条，重复：{duplicateCount} 条）',
  
  // ============ API Management - Import/Export Count ============
  importedCount: '导入数量',
  totalCount: '总数量',
  submittedCount: '成功提交',
  failedCount: '提交失败',
  
  // ============ Auth - Sign In/Up ============
  signIn: '登录到Supabase',
  signUp: '注册',
  signOut: '登出',
  email: '邮箱',
  password: '密码',
  passwordPlaceholder: '请输入密码',
  noAccount: '没有账号？立即注册',
  haveAccount: '已有账号？立即登录',
  loading: '加载中...',
  initializing: '初始化中...',
  connecting: '连接中...',
  authenticating: '认证中...',
  resetting: '重置中...',
  restoring: '恢复中...',
  skipLogin: '跳过登录，使用本地模式',
  verifyEmail: '注册成功！请查收验证邮件并点击链接完成验证。',
  localModeHint: '当前使用本地模式，登录后可同步数据到云端',
  
  // ============ Auth - Account Management ============
  accountManagement: '账户管理',
  uploadAvatar: '上传头像',
  uploading: '上传中...',
  emailPlaceholder: '请输入邮箱',
  updateEmail: '更新邮箱',
  phone: '手机号码',
  phonePlaceholder: '请输入手机号码',
  updatePhone: '更新手机',
  
  // ============ Auth - Password Reset ============
  resetPassword: '重置密码',
  currentPassword: '当前密码',
  newPassword: '新密码',
  confirmPassword: '确认密码',
  passwordMinLength: '密码长度至少为6位',
  passwordMismatch: '两次输入的密码不一致',
  passwordResetSuccess: '密码重置成功',
  passwordResetFailed: '密码重置失败',
  currentPasswordIncorrect: '当前密码不正确',
  signOutConfirm: '确定要登出吗？',
  
  // ============ Cloud Data Dialog ============
  cloudDataFound: '发现云端数据',
  cloudDataFoundMessage: '我们在您的云端存储中发现了 {count} 条记录。您是否要下载它们？',
  downloadFromCloud: '从云端下载',
  skipDownload: '跳过',
  loginRequired: '需要登录',
  loginRequiredMessage: '您的 Supabase API 已配置但尚未登录。请登录以访问您的云端数据。',
  loginNow: '立即登录',
  
  // ============ API Management Page ============
  apiManagement: 'API管理',
  api: 'API',
  saveApiSettings: '保存',
  securityPassword: '安全密码',
  enterPassword: '请输入安全密码',
  selectSyncSource: '选择同步源',
  feishu: '飞书',
  supabase: 'Supabase',
  storageMethod: '存储方式',
  dataManagement: '数据管理',
  storageUsage: '存储占用',
  clearOldData: '清理本地数据',
  systemLog: '系统日志',
  apiUrlDisplay: '已保存的API设置',
  apiConfigRequired: '需要API配置',
  syncFromFeishuOnly: '从飞书同步',

  deleteApiSettings: '删除API设置',
  confirmDeleteApiSettings: '确定要删除已保存的API设置吗？',
  renameApiSettings: '重命名API设置',
  resetApiSettings: '重置API设置',
  enterNewPassword: '请输入新的API设置安全密码：',
  apiSettingsVerifyPassword: '在已保存的API设置中验证安全密码进行修改',
  viewSavedApiSettings: '查看已保存的API设置',
  
  // ============ Login Restore ============
  previousLoginFound: '发现之前的登录记录',
  previousLoginFoundMessage: '我们发现了之前的登录记录。您是否需要恢复登录状态？需要先验证安全密码。',
  restoreLogin: '恢复登录',
  continueLocalMode: '继续本地模式',
  enterSecurityPasswordForRestore: '请输入安全密码以恢复登录状态',
  
  // ============ DatePicker ============
  clear: '清除',
  today: '今天',
  selectDate: '选择日期',
  selectStartDate: '选择开始日期',
  selectEndDate: '选择结束日期',
  
  // ============ Months ============
  january: '1月',
  february: '2月',
  march: '3月',
  april: '4月',
  may: '5月',
  june: '6月',
  july: '7月',
  august: '8月',
  september: '9月',
  october: '10月',
  november: '11月',
  december: '12月',
  
  // ============ Weekdays Short ============
  sunday: '日',
  monday: '一',
  tuesday: '二',
  wednesday: '三',
  thursday: '四',
  friday: '五',
  saturday: '六',
  
  // ============ Color Picker ============
  presetColors: '预设颜色',
  
  // ============ Font Attribution ============
  fontAttribution: '本应用使用了 HarmonyOS Sans 字体，由华为终端有限公司提供。',
  
  // ============ API Management - Anon Key ============
  anonKey: '匿名密钥',
  
  // ============ Analysis Page - Operation Log ============
  operationLog: '操作日志',
  feishuPasswordRequired: '飞书 API 设置已加密，请输入密码查看。',
  apiPasswordRequired: 'API 设置已加密，请输入密码查看。',
  passwordError: '密码错误，请重试。',
  
  // ============ Settings - Messages ============
  dataCleared: '数据已清除',
  
  // ============ Settings - Error Messages ============
  bucketNotExist: '存储桶不存在，请联系管理员',
  permissionDenied: '权限不足，请检查存储桶设置',
  avatarUploadSuccess: '头像上传成功',
  avatarUploadFailed: '头像上传失败',
  enterEmail: '请输入邮箱地址',
  emailBindSuccess: '邮箱绑定成功，请检查邮箱验证',
  emailBindFailed: '邮箱绑定失败',
  passwordSameAsCurrent: '新密码不能与当前密码相同',
  passwordRequirements: '密码要求：至少6位字符',
  signOutFailed: '登出失败',
  
  // ============ Settings - Storage Info ============
  used: '已用',
  mb: 'MB',
  
  // ============ Settings - Language Names ============
  chinese: '中文',
  japanese: '日本語',
  korean: '韩语',
  
  // ============ Operation Log Component ============
  noOperationRecords: '暂无操作记录',
  showRecent5: '显示最近5条',
  showAll: '显示全部',
  clearAll: '清空',
  createRecord: '新增记录',
  updateRecord: '编辑记录',
  deleteRecord: '删除记录',
  
  // ============ External Link Warning ============
  externalLinkWarningTitle: '外部链接',
  externalLinkWarningMessage: '您即将访问外部网站：',
  externalLinkContinue: '继续',
  externalLinkCancel: '取消',
  
  // ============ Error Boundary ============
  somethingWentWrong: '出现了错误',
  unexpectedError: '发生未知错误',
  tryAgain: '重试',
  refreshPage: '刷新页面',
  errorDetails: '错误详情'
};

export type I18nZH = typeof i18n_ZH;

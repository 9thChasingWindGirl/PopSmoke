import { Language } from '../types';

export const i18n_JP = {
  // ============ App Level / Common ============
  appTitle: '喫煙',
  appSubtitle: '管理',
  tracker: '記録',
  notificationSuccess: '成功！',
  notificationError: 'エラー！',
  notificationInfo: '情報',
  notificationWarning: '警告！',
  
  // ============ Navigation ============
  analysis: '分析',
  settings: '設定',
  history: '履歴',
  
  // ============ Dashboard ============
  lastRecord: '前回の記録',
  smokeButton: '喫煙',
  clickToLog: '記録する',
  todayLogs: '今日の記録',
  noRecords: '今日の記録はありません',
  currentGaugeLevel: '今日の喫煙数',
  smokeUnit: '本',
  
  // ============ Settings - Threshold ============
  dailyLimit: '1日の制限',
  warningLimit: '警告レベル',
  thresholdSettings: '閾値設定',
  
  // ============ Settings - Theme & Language ============
  themeColor: 'テーマ色',
  language: '言語 / Language',
  customHex: 'カスタム色',
  customColor: 'カスタム色',
  theme: 'テーマ',
  
  // ============ Dashboard - Messages ============
  warningMsg: '警告：制限に近づいています！',
  limitReachedMsg: '1日の制限に達しました！',
  
  // ============ Analysis - Export ============
  totalCigarettes: '総本数',
  exportCSV: 'CSV出力',
  csvNote: '日付、時間、月次タグが含まれます。',
  noDataToExport: 'この月にはエクスポートするデータがありません。',
  exportSuccess: 'エクスポート成功',
  exportFailed: 'エクスポート失敗',
  
  // ============ Analysis - Statistics ============
  weeklyFreq: '週間',
  monthlyFreq: '月間',
  thisWeek: '今週',
  weekData: '週データ',
  monthData: '月データ',
  weekTotal: '今週合計',
  mostSmokedDay: '最も喫煙した曜日',
  monthTotal: '今月合計',
  lastMonthTotal: '先月合計',
  mostSmokedDate: '最も喫煙した日',
  
  // ============ Calendar / Weekdays ============
  day: '日',
  mon: '月',
  tue: '火',
  wed: '水',
  thu: '木',
  fri: '金',
  sat: '土',
  sun: '日',
  
  // ============ Common Actions ============
  more: '詳細 / 編集',
  back: '戻る',
  next: '次へ',
  prev: '前へ',
  delete: '削除',
  edit: '編集',
  rename: '名前を変更',
  reset: 'リセット',
  close: '閉じる',
  hide: '非表示',
  save: '保存',
  cancel: 'キャンセル',
  confirm: '確認',
  editTime: '時間を編集',
  never: 'なし',
  view: '表示',
  about: 'について',
  blog: 'ブログ',
  github: 'GitHub',
  stats: '統計',
  data: 'データ',
  hour: '時',
  minute: '分',
  count: '本数',
  
  // ============ History Page ============
  clearLocalData: 'ローカルデータをクリア',
  currentMonth: '当月',
  prevMonth: '前月',
  nextMonth: '次月',
  confirmClearData: 'データをクリア',
  cancelClearData: 'キャンセル',
  clearDataWarning: '警告！',
  clearDataMessage: 'すべてのローカルデータをクリアしてもよろしいですか？この操作は元に戻せません！',
  clearData: 'データをクリア',
  clearDataConfirm: 'これによりすべての喫煙記録が削除されます。この操作は元に戻せません。よろしいですか？',
  yesClear: 'はい、クリア',
  loadMore: 'もっと読み込む',
  
  // ============ Storage / Error ============
  storageError: 'ストレージエラー',
  storageErrorHint: '設定に移動して古いデータを削除し、スペースを解放してください。',
  goToSettings: '設定へ',
  dismiss: '閉じる',
  
  // ============ History - Filters & Sort ============
  filtersAndSort: 'フィルターと並べ替え',
  startDate: '開始日',
  endDate: '終了日',
  minCount: '最小数',
  maxCount: '最大数',
  sortByDate: '日付で並べ替え',
  sortByCount: '数で並べ替え',
  resetFilters: 'フィルターをリセット',
  noMatchingRecords: '一致する記録がありません',
  totalRecords: '総記録数',
  filteredRecords: 'フィルター後の記録数',
  dateRange: '日付範囲',
  averagePerDay: '1日あたりの平均',
  prevPage: '前のページ',
  nextPage: '次のページ',
  
  // ============ API Management - Feishu ============
  feishuApi: 'Feishu API',
  feishuApiSettings: 'Feishu API設定',
  feishuDataSync: 'Feishuデータ同期',
  apiUrl: 'API URL',
  syncFromFeishu: 'Feishuから同期',
  syncToFeishu: 'Feishuに同期',
  startSync: '同期開始',
  syncing: '同期中...',
  targetTableName: '対象テーブル名',
  accessKey: 'アクセスキー',
  accessKeyDialogTitle: '認証パスワード',
  accessKeyDialogMessage: 'Feishu API書き込みアクセスキーを入力してください:',
  submitToFeishu: 'Feishuに送信',
  submitting: '送信中...',
  startSubmit: '送信開始',
  
  // ============ API Management - Cloud Sync ============
  cloudSync: 'クラウド同期',
  cloudSyncing: '同期中...',
  loginFirst: '同期するにはログインしてください',
  syncSuccess: '同期成功',
  syncFailed: '同期失敗',
  noNewRecordsToSync: '同期する新しいレコードがありません',
  uploaded: 'アップロード',
  downloaded: 'ダウンロード',
  records: '件',
  
  // ============ API Management - Feishu Sync Messages ============
  feishuSyncSuccess: 'Feishu APIからの同期成功',
  feishuSyncToStorage: 'Feishu APIからローカルストレージへの同期成功',
  feishuSyncToCloud: 'Feishu APIからSupabaseへの同期成功',
  syncedRecordsFromFeishu: 'Feishuから {newCount} 件の新しいレコードを同期しました（総数：{totalCount} 件、重複：{duplicateCount} 件）',
  
  // ============ API Management - Import/Export Count ============
  importedCount: 'インポート数',
  totalCount: '総数',
  submittedCount: '送信成功数',
  failedCount: '送信失敗数',
  
  // ============ Auth - Sign In/Up ============
  signIn: 'ログイン到Supabase',
  signUp: '登録',
  signOut: 'ログアウト',
  email: 'メール',
  password: 'パスワード',
  passwordPlaceholder: 'パスワードを入力',
  noAccount: 'アカウントがない場合: 登録',
  haveAccount: 'すでにアカウントがある場合: ログイン',
  loading: '読み込み中...',
  skipLogin: 'ログインをスキップしてローカルモードを使用',
  verifyEmail: '登録成功！確認メールを確認し、リンクをクリックして確認を完了してください。',
  localModeHint: '現在ローカルモードを使用中。ログインしてクラウドにデータを同期',
  
  // ============ Auth - Account Management ============
  accountManagement: 'アカウント管理',
  uploadAvatar: 'アバターをアップロード',
  uploading: 'アップロード中...',
  emailPlaceholder: 'メールアドレスを入力',
  updateEmail: 'メールを更新',
  phone: '電話番号',
  phonePlaceholder: '電話番号を入力',
  updatePhone: '電話番号を更新',
  
  // ============ Auth - Password Reset ============
  resetPassword: 'パスワードをリセット',
  currentPassword: '現在のパスワード',
  newPassword: '新しいパスワード',
  confirmPassword: 'パスワードを確認',
  passwordMinLength: 'パスワードは6文字以上である必要があります',
  passwordMismatch: 'パスワードが一致しません',
  passwordResetSuccess: 'パスワードリセット成功',
  passwordResetFailed: 'パスワードリセット失敗',
  currentPasswordIncorrect: '現在のパスワードが正しくありません',
  signOutConfirm: 'ログアウトしてもよろしいですか？',
  
  // ============ Cloud Data Dialog ============
  cloudDataFound: 'クラウドデータ発見',
  cloudDataFoundMessage: 'クラウドストレージに {count} 件の記録が見つかりました。ダウンロードしますか？',
  downloadFromCloud: 'クラウドからダウンロード',
  skipDownload: 'スキップ',
  loginRequired: 'ログインが必要',
  loginRequiredMessage: 'Supabase APIは設定されていますが、ログインしていません。クラウドデータにアクセスするにはログインしてください。',
  loginNow: '今すぐログイン',
  
  // ============ API Management Page ============
  apiManagement: 'API管理',
  api: 'API',
  saveApiSettings: '保存',
  securityPassword: 'セキュリティパスワード',
  enterPassword: 'セキュリティパスワードを入力',
  selectSyncSource: '同期ソースを選択',
  feishu: 'Feishu',
  supabase: 'Supabase',
  storageMethod: 'ストレージ方式',
  dataManagement: 'データ管理',
  storageUsage: 'ストレージ使用量',
  clearOldData: 'ローカルデータをクリア',
  apiUrlDisplay: '保存されたAPI設定',
  apiConfigRequired: 'API設定が必要',
  syncFromFeishuOnly: 'Feishuから同期',
  syncFromSupabaseOnly: 'Supabaseから同期',
  deleteApiSettings: 'API設定を削除',
  confirmDeleteApiSettings: '保存されたAPI設定を削除してもよろしいですか？',
  renameApiSettings: 'API設定の名前を変更',
  resetApiSettings: 'API設定をリセット',
  enterNewPassword: 'API設定の新しいセキュリティパスワードを入力してください：',
  apiSettingsVerifyPassword: '保存されたAPI設定でセキュリティパスワードを確認して変更してください',
  viewSavedApiSettings: '保存されたAPI設定を表示',
  
  // ============ Login Restore ============
  previousLoginFound: '以前のログイン記録が見つかりました',
  previousLoginFoundMessage: '以前のログイン記録が見つかりました。ログイン状態を復元しますか？まずセキュリティパスワードを確認する必要があります。',
  restoreLogin: 'ログインを復元',
  continueLocalMode: 'ローカルモードを続行',
  enterSecurityPasswordForRestore: 'ログイン状態を復元するためにセキュリティパスワードを入力してください',
  
  // ============ DatePicker ============
  clear: 'クリア',
  today: '今日',
  selectDate: '日付を選択',
  selectStartDate: '開始日を選択',
  selectEndDate: '終了日を選択',
  
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
  monday: '月',
  tuesday: '火',
  wednesday: '水',
  thursday: '木',
  friday: '金',
  saturday: '土',
  
  // ============ Color Picker ============
  presetColors: 'プリセットカラー',
  
  // ============ Font Attribution ============
  fontAttribution: 'このアプリは Huawei Device Co., Ltd. 提供の HarmonyOS Sans フォントを使用しています。',
  
  // ============ API Management - Anon Key ============
  anonKey: '匿名キー',
  
  // ============ Analysis Page - Operation Log ============
  operationLog: '操作ログ',
  feishuPasswordRequired: 'Feishu API設定は暗号化されています。表示するにはパスワードを入力してください。',
  apiPasswordRequired: 'API設定は暗号化されています。表示するにはパスワードを入力してください。',
  passwordError: 'パスワードが正しくありません。もう一度お試しください。',
  
  // ============ Settings - Messages ============
  dataCleared: 'データがクリアされました',
  
  // ============ Settings - Error Messages ============
  bucketNotExist: 'ストレージバケットが存在しません。管理者に連絡してください',
  permissionDenied: '権限が拒否されました。バケット設定を確認してください',
  avatarUploadSuccess: 'アバターが正常にアップロードされました',
  avatarUploadFailed: 'アバターのアップロードに失敗しました',
  enterEmail: 'メールアドレスを入力してください',
  emailBindSuccess: 'メールが正常にバインドされました。確認メールをご確認ください',
  emailBindFailed: 'メールのバインドに失敗しました',
  passwordSameAsCurrent: '新しいパスワードは現在のパスワードと同じにできません',
  passwordRequirements: 'パスワード要件：6文字以上',
  signOutFailed: 'サインアウトに失敗しました',
  
  // ============ Settings - Storage Info ============
  used: '使用済み',
  mb: 'MB',
  
  // ============ Settings - Language Names ============
  chinese: '中国語',
  japanese: '日本語',
  korean: '韓国語',
  
  // ============ Operation Log Component ============
  noOperationRecords: '操作記録なし',
  showRecent5: '最近5件を表示',
  showAll: 'すべて表示',
  clearAll: 'すべてクリア',
  createLog: '作成',
  updateLog: '更新',
  deleteLog: '削除',
  
  // ============ External Link Warning ============
  externalLinkWarningTitle: '外部リンク',
  externalLinkWarningMessage: '外部ウェブサイトにアクセスしようとしています：',
  externalLinkContinue: '続ける',
  externalLinkCancel: 'キャンセル',
  
  // ============ Error Boundary ============
  somethingWentWrong: 'エラーが発生しました',
  tryAgain: '再試行',
  refreshPage: 'ページを更新',
  errorDetails: 'エラーの詳細'
};

export type I18nJP = typeof i18n_JP;

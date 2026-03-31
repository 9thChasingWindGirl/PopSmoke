import { Language } from '../types';

export const i18n_EN = {
  // ============ App Level / Common ============
  appTitle: 'SMOKE',
  appSubtitle: 'CONTROLLER',
  tracker: 'TRACKER',
  notificationSuccess: 'Success!',
  notificationError: 'Error!',
  notificationInfo: 'Info',
  notificationWarning: 'Warning!',
  
  // ============ Navigation ============
  analysis: 'ANALYSIS',
  settings: 'SETTINGS',
  history: 'HISTORY',
  
  // ============ Dashboard ============
  lastRecord: 'LAST RECORD',
  smokeButton: 'SMOKE',
  smokes: 'SMOKES',
  clickToLog: 'Click the button below to log',
  todayLogs: "TODAY'S LOGS",
  noRecords: 'No records yet',
  recentLogs: 'RECENT LOGS',
  last24Hours: 'The last 24 hours of activity',
  currentGaugeLevel: "Today's Smoking Count",
  smokeUnit: 'Smokes',
  consecutiveDaysWithinLimit: 'Consecutive Days Within Limit',
  days: 'days',
  
  // ============ Time Periods ============
  lateNight: 'Late Night',
  earlyMorning: 'Early Morning',
  morning: 'Morning',
  noon: 'Noon',
  afternoon: 'Afternoon',
  evening: 'Evening',
  
  // ============ Settings - Threshold ============
  dailyLimit: 'Daily Limit',
  warningLimit: 'Warning Level',
  thresholdSettings: 'Threshold Settings',
  
  // ============ Settings - Theme & Language ============
  themeColor: 'Theme Color',
  language: 'Language',
  customHex: 'Custom Hex',
  customColor: 'Custom Color',
  theme: 'Theme',
  
  // ============ Dashboard - Messages ============
  warningMsg: 'WARNING: Approaching Daily Limit!',
  limitReachedMsg: 'DAILY LIMIT REACHED!',
  
  // ============ Analysis - Export ============
  totalCigarettes: 'Total Cigarettes',
  exportCSV: 'Export CSV',
  csvNote: 'Contains Date, Time, and Monthly grouping tags.',
  noDataToExport: 'No data to export for this month.',
  export: 'Export',
  exportSuccess: 'Export successful',
  exportFailed: 'Export failed',
  
  // ============ Analysis - Statistics ============
  weeklyFreq: 'Weekly',
  monthlyFreq: 'Monthly',
  thisWeek: 'This Week',
  weekData: 'Week',
  monthData: 'Month',
  weekTotal: 'This Week Total',
  mostSmokedDay: 'Most Smoked Day',
  monthTotal: 'This Month Total',
  lastMonthTotal: 'Last Month Total',
  mostSmokedDate: 'Most Smoked Date',
  
  // ============ Calendar / Weekdays ============
  day: 'th',
  mon: 'Mon',
  tue: 'Tue',
  wed: 'Wed',
  thu: 'Thu',
  fri: 'Fri',
  sat: 'Sat',
  sun: 'Sun',
  
  // ============ Common Actions ============  
  more: 'MORE / EDIT',
  back: 'BACK',
  next: 'NEXT',
  prev: 'PREV',
  delete: 'DEL',
  edit: 'EDIT',
  rename: 'RENAME',
  reset: 'RESET',
  close: 'CLOSE',
  hide: 'HIDE',
  save: 'SAVE',
  cancel: 'CANCEL',
  confirm: 'CONFIRM',
  confirmDelete: 'Are you sure you want to delete this record?',
  editTime: 'Edit Time',
  never: 'Never',
  view: 'View',
  about: 'About',
  blog: 'Blog',
  github: 'GitHub',
  stats: 'Stats',
  data: 'Data',
  hour: 'HOUR',
  minute: 'MINUTE',
  count: 'Count',
  
  // ============ History Page ============
  clearLocalData: 'Clear Local Data',
  currentMonth: 'Current Month',
  prevMonth: 'Prev Month',
  nextMonth: 'Next Month',
  confirmClearData: 'Confirm Clear Data',
  cancelClearData: 'Cancel Clear Data',
  clearDataWarning: 'Warning!',
  clearDataMessage: 'Are you sure you want to clear all local data? This action is irreversible!',
  clearData: 'Clear Data',
  clearDataConfirm: 'This will clear all smoking logs. This action cannot be undone. Are you sure?',
  yesClear: 'Yes, Clear',
  loadMore: 'Load More',
  
  // ============ Storage / Error ============
  storageError: 'Storage Error',
  storageErrorHint: 'Please go to Settings and clear some old data to free up space.',
  goToSettings: 'Go to Settings',
  dismiss: 'Dismiss',
  
  // ============ History - Filters & Sort ============
  filtersAndSort: 'Filters & Sorting',
  startDate: 'Start Date',
  endDate: 'End Date',
  minCount: 'Min Count',
  maxCount: 'Max Count',
  sortByDate: 'Sort by Date',
  sortByCount: 'Sort by Count',
  resetFilters: 'Reset Filters',
  noMatchingRecords: 'No matching records',
  totalRecords: 'Total Records',
  filteredRecords: 'Filtered Records',
  dateRange: 'Date Range',
  averagePerDay: 'Average Per Day',
  prevPage: 'Prev Page',
  nextPage: 'Next Page',
  
  // ============ API Management - Feishu ============
  feishuApi: 'Feishu API',
  feishuApiSettings: 'Feishu API Settings',
  feishuDataSync: 'Feishu Data Sync',
  apiUrl: 'API URL',
  syncFromFeishu: 'Sync from Feishu',
  syncToFeishu: 'Sync to Feishu',
  startSync: 'Start Sync',
  syncing: 'Syncing...',
  targetTableName: 'Target Table Name',
  accessKey: 'Access Key',
  accessKeyDialogTitle: 'Authentication Password',
  accessKeyDialogMessage: 'Please enter Feishu API write access key:',
  submitToFeishu: 'Submit to Feishu',
  submitting: 'Submitting...',
  startSubmit: 'Start Submit',
  syncFromSupabaseOnly: 'Sync with Supabase',
  syncDiffSummary: 'Sync Summary',
  cloudOnlyRecords: 'Cloud Only Records',
  confirmSync: 'Confirm Sync',
  localRecords: 'Local records',
  cloudRecords: 'Cloud records',
  localOnly: 'Local only',
  cloudOnly: 'Cloud only',
  conflicts: 'Conflicts',
  upload: 'Upload',
  download: 'Download',
  uploadOnly: 'Upload Only',
  downloadOnly: 'Download Only',
  foundNewRecords: 'Found {count} new records',
  foundConflictingRecords: '{count} conflicting records',
  needUploadRecords: 'Need to upload {count} records',
  needDownloadRecords: 'Need to download {count} records',
  uploadCount: 'Upload {count}',
  downloadCount: 'Download {count}',
  syncCompleted: 'Sync with {source} completed: {message}',
  syncWithFeishuCompleted: 'Sync with Feishu completed',
  syncWithSupabaseCompleted: 'Sync with Supabase completed',
  
  // ============ API Management - Cloud Sync ============
  cloudSync: 'Cloud Sync',
  cloudSyncing: 'Syncing...',
  loginFirst: 'Please login first to sync',
  syncSuccess: 'Sync successful',
  syncFailed: 'Sync failed',
  noNewRecordsToSync: 'No new records to sync',
  uploaded: 'Uploaded',
  downloaded: 'Downloaded',
  records: 'records',
  
  // ============ API Management - Feishu Sync Messages ============
  feishuSyncSuccess: 'Successfully synced from Feishu API',
  feishuSyncToStorage: 'Successfully synced from Feishu API to localStorage',
  feishuSyncToCloud: 'Successfully synced from Feishu API and uploaded to Supabase',
  syncedRecordsFromFeishu: 'Synced {newCount} new records from Feishu (Total: {totalCount}, Duplicates: {duplicateCount})',
  
  // ============ API Management - Import/Export Count ============
  importedCount: 'Imported Count',
  totalCount: 'Total Count',
  submittedCount: 'Submitted Count',
  failedCount: 'Failed Count',
  
  // ============ Auth - Sign In/Up ============
  signIn: 'Sign In to Supabase',
  signUp: 'Sign Up',
  signOut: 'Sign Out',
  email: 'Email',
  password: 'Password',
  passwordPlaceholder: 'Enter your password',
  noAccount: "Don't have an account? Sign Up",
  haveAccount: 'Already have an account? Sign In',
  loading: 'Loading...',
  initializing: 'Initializing...',
  connecting: 'Connecting...',
  authenticating: 'Authenticating...',
  resetting: 'Resetting...',
  restoring: 'Restoring...',
  skipLogin: 'Skip login, use local mode',
  verifyEmail: 'Registration successful! Please check your email and click the verification link.',
  localModeHint: 'Currently using local mode. Sign in to sync data to cloud.',
  
  // ============ Auth - Account Management ============
  accountManagement: 'Account Management',
  uploadAvatar: 'Upload Avatar',
  uploading: 'Uploading...',
  emailPlaceholder: 'Enter your email',
  updateEmail: 'Update Email',
  phone: 'Phone Number',
  phonePlaceholder: 'Enter your phone number',
  updatePhone: 'Update Phone',
  
  // ============ Auth - Password Reset ============
  resetPassword: 'Reset Password',
  currentPassword: 'Current Password',
  newPassword: 'New Password',
  confirmPassword: 'Confirm Password',
  passwordMinLength: 'Password must be at least 6 characters',
  passwordMismatch: 'Passwords do not match',
  passwordResetSuccess: 'Password reset successful',
  passwordResetFailed: 'Password reset failed',
  currentPasswordIncorrect: 'Current password is incorrect',
  signOutConfirm: 'Are you sure you want to sign out?',
  
  // ============ Cloud Data Dialog ============
  cloudDataFound: 'Cloud Data Found',
  cloudDataFoundMessage: 'We found {count} records in your cloud storage. Would you like to download them?',
  downloadFromCloud: 'Download from Cloud',
  skipDownload: 'Skip',
  loginRequired: 'Login Required',
  loginRequiredMessage: 'Your Supabase API is configured but you are not logged in. Please log in to access your cloud data.',
  loginNow: 'Login Now',
  selectDataSource: 'Select Data Source',
  selectDataSourceMessage: 'Please select which data source to get data from:',
  
  // ============ API Management Page ============
  apiManagement: 'API INF',
  api: 'API INF',
  saveApiSettings: 'Save',
  securityPassword: 'Security Password',
  enterPassword: 'Enter Security Password',
  selectSyncSource: 'Select Sync Source',
  firstLaunchSyncMessage: 'Welcome! We detected you have cloud sync configured, but no local data yet. Please select which data source to retrieve your history from:',
  feishu: 'Feishu',
  supabase: 'Supabase',
  storageMethod: 'Storage Method',
  dataManagement: 'Data Management',
  storageUsage: 'Storage Usage',
  clearOldData: 'Clear Local Data',
  systemLog: 'System Logs',
  apiUrlDisplay: 'Saved API Settings',
  apiConfigRequired: 'API Configuration Required',


  deleteApiSettings: 'Delete API Settings',
  confirmDeleteApiSettings: 'Are you sure you want to delete the saved API settings?',
  renameApiSettings: 'Rename API Settings',
  resetApiSettings: 'Reset API Settings',
  enterNewPassword: 'Enter new security password for API settings:',
  apiSettingsVerifyPassword: 'Verify security password in saved API settings to modify',
  viewSavedApiSettings: 'View Saved API Settings',
  
  // ============ Login Restore ============
  previousLoginFound: 'Previous Login Found',
  previousLoginFoundMessage: 'We found a previous login record. Do you want to restore your login status? You need to verify your security password first.',
  restoreLogin: 'Restore Login',
  continueLocalMode: 'Continue Local Mode',
  enterSecurityPasswordForRestore: 'Enter your security password to restore login status',
  
  // ============ DatePicker ============
  clear: 'Clear',
  today: 'Today',
  selectDate: 'Select date',
  selectStartDate: 'Select start date',
  selectEndDate: 'Select end date',
  
  // ============ Months ============
  january: 'Jan',
  february: 'Feb',
  march: 'Mar',
  april: 'Apr',
  may: 'May',
  june: 'Jun',
  july: 'Jul',
  august: 'Aug',
  september: 'Sep',
  october: 'Oct',
  november: 'Nov',
  december: 'Dec',
  
  // ============ Weekdays Short ============
  sunday: 'Su',
  monday: 'Mo',
  tuesday: 'Tu',
  wednesday: 'We',
  thursday: 'Th',
  friday: 'Fr',
  saturday: 'Sa',
  
  // ============ Color Picker ============
  presetColors: 'Preset Colors',
  
  // ============ Font Attribution ============
  fontAttribution: 'This app uses HarmonyOS Sans font, provided by Huawei Device Co., Ltd.',
  
  // ============ API Management - Anon Key ============
  anonKey: 'Anon Key',
  
  // ============ Analysis Page - Operation Log ============
  operationLog: 'Operation Log',
  feishuPasswordRequired: 'Feishu API settings are encrypted. Please enter password to view.',
  apiPasswordRequired: 'API settings are encrypted. Please enter password to view.',
  passwordError: 'Password incorrect. Please try again.',
  
  // ============ Settings - Messages ============
  dataCleared: 'Data Cleared',
  
  // ============ Settings - Error Messages ============
  bucketNotExist: 'Storage bucket does not exist, please contact administrator',
  permissionDenied: 'Permission denied, please check bucket settings',
  avatarUploadSuccess: 'Avatar uploaded successfully',
  avatarUploadFailed: 'Avatar upload failed',
  enterEmail: 'Please enter email address',
  emailBindSuccess: 'Email bound successfully, please check your email for verification',
  emailBindFailed: 'Email binding failed',
  passwordSameAsCurrent: 'New password cannot be the same as current password',
  passwordRequirements: 'Password requirements: at least 6 characters',
  signOutFailed: 'Sign out failed',
  
  // ============ Settings - Storage Info ============
  used: 'Used',
  mb: 'MB',
  
  // ============ Settings - Language Names ============
  chinese: 'Chinese',
  japanese: 'Japanese',
  korean: 'Korean',
  
  // ============ Operation Log Component ============
  noOperationRecords: 'No operation records',
  showRecent5: 'Show recent 5',
  showAll: 'Show all',
  clearAll: 'Clear all',
  createRecord: 'Create record',
  updateRecord: 'Update record',
  deleteRecord: 'Delete record',
  
  // ============ External Link Warning ============
  externalLinkWarningTitle: 'External Link',
  externalLinkWarningMessage: 'You are about to visit an external website:',
  
  // ============ Sync - Field Update ============
  fieldsToUpdate: 'Fields to update',
  localDataNewerThanFeishu: 'Local data is newer than Feishu, please back up local data first',
  localDataNewerThanCloud: 'Local data is newer than cloud, please upload local data first',
  externalLinkContinue: 'Continue',
  externalLinkCancel: 'Cancel',
  
  // ============ Error Boundary ============
  somethingWentWrong: 'Something went wrong',
  unexpectedError: 'An unexpected error occurred',
  tryAgain: 'Try Again',
  refreshPage: 'Refresh Page',
  errorDetails: 'Error Details',
  
  // ============ Supabase Guide ============
  howToGetSupabaseApi: 'How to get Supabase API credentials?',
  supabaseGuideTitle: 'Supabase Setup Guide',
  whatIsSupabase: 'What is Supabase?',
  supabaseDescription: 'Supabase is an open-source Firebase alternative that provides PostgreSQL database, authentication, real-time subscriptions, and more.',
  supabaseUseCase: 'Using Supabase allows your smoking records to sync across multiple devices and be securely backed up in the cloud.',
  supabaseRequirements: 'What you need:',
  supabaseEmail: 'An email address (for registering a Supabase account)',
  supabaseTime: 'About 5-10 minutes to complete configuration',
  supabaseStep1: 'Step 1: Register Supabase Account',
  supabaseStep1Desc1: 'Visit Supabase website:',
  supabaseStep1Desc2: 'Click "Start your project" or "Sign In" in the top right corner',
  supabaseStep1Desc3: 'Register/login with GitHub account or email',
  supabaseTip: '💡 Tip: Supabase offers a free plan that is sufficient for personal use.',
  supabaseStep2: 'Step 2: Create New Project',
  supabaseStep2Desc1: 'After logging in, click the "New project" button',
  supabaseStep2Desc2: 'Select or create an organization',
  supabaseStep2Desc3: 'Fill in project information:',
  supabaseStep2Desc4: 'Name: Project name (e.g., popsmoke-tracker)',
  supabaseStep2Desc5: 'Database Password: Set a database password (remember it)',
  supabaseStep2Desc6: 'Region: Choose the server region closest to you',
  supabaseStep2Desc7: 'Click "Create new project" to create the project',
  supabaseNote: '⏳ Note: Project creation takes 1-2 minutes, please be patient.',
  supabaseStep3: 'Step 3: Get API Credentials',
  supabaseStep3Desc1: 'After project creation, go to the project Dashboard',
  supabaseStep3Desc2: 'Click "Project Settings" in the left menu',
  supabaseStep3Desc3: 'Select the "API" tab',
  supabaseStep3Desc4: 'Find the following information in the "Project API keys" section:',
  supabaseProjectUrl: 'Project URL:',
  supabaseAnonKey: 'anon public:',
  supabaseStep4: 'Step 4: Configure in This App',
  supabaseStep4Desc1: 'Copy "Project URL" to the <strong>SUPABASE_URL</strong> field in this app',
  supabaseStep4Desc2: 'Copy "anon public" key to the <strong>SUPABASE_ANON_KEY</strong> field in this app',
  supabaseStep4Desc3: 'Click the save button to complete configuration',
  supabaseAfterConfig: '✅ After configuration, you can:',
  supabaseSyncDevices: 'Sync smoking records across different devices',
  supabaseCloudBackup: 'Cloud backup to prevent data loss',
  supabaseCloudSync: 'Use cloud sync to download/upload data',
  supabasePrev: 'Previous',
  supabaseNext: 'Next',
  supabaseComplete: 'Complete'
};

export type I18nTranslations = typeof i18n_EN;
export type I18nEN = typeof i18n_EN;
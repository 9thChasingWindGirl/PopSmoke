import { Language } from '../types';

export const i18n_KO = {
  // ============ App Level / Common ============
  appTitle: '흡연',
  appSubtitle: '관리',
  tracker: '기록',
  notificationSuccess: '성공！',
  notificationError: '오류！',
  notificationInfo: '정보',
  notificationWarning: '경고！',
  
  // ============ Navigation ============
  analysis: '분석',
  settings: '설정',
  history: '기록',
  
  // ============ Dashboard ============
  lastRecord: '마지막 기록',
  smokeButton: '흡연',
  clickToLog: '아래 버튼을 클릭하여 기록',
  todayLogs: '오늘의 기록',
  noRecords: '오늘 기록 없음',
  recentLogs: '최근 기록',
  last24Hours: '최근 24시간 활동',
  currentGaugeLevel: '오늘의 흡연 수',
  smokeUnit: '개',
  consecutiveDaysWithinLimit: '연속 달성 일수',
  days: '일',
  smokes: '개',
  
  // ============ Time Periods ============
  lateNight: '심야',
  earlyMorning: '새벽',
  morning: '오전',
  noon: '점심',
  afternoon: '오후',
  evening: '저녘',
  
  // ============ Settings - Threshold ============
  dailyLimit: '일일 한도',
  warningLimit: '경고 수준',
  thresholdSettings: '임계값 설정',
  
  // ============ Settings - Theme & Language ============
  themeColor: '테마 색상',
  language: '언어 / Language',
  customHex: '사용자 지정 색상',
  customColor: '사용자 지정 색상',
  theme: '테마',
  
  // ============ Dashboard - Messages ============
  warningMsg: '경고: 일일 한도에 근접!',
  limitReachedMsg: '일일 한도 도달!',
  
  // ============ Analysis - Export ============
  totalCigarettes: '총 담배 수',
  exportCSV: 'CSV 내보내기',
  csvNote: '날짜, 시간 및 월별 그룹 태그가 포함됩니다.',
  noDataToExport: '이번 달에 내보낼 데이터가 없습니다.',
  export: '내보내기',
  exportSuccess: '내보내기 성공',
  exportFailed: '내보내기 실패',
  
  // ============ Analysis - Statistics ============
  weeklyFreq: '주간',
  monthlyFreq: '월간',
  thisWeek: '이번 주',
  weekData: '주 데이터',
  monthData: '월 데이터',
  weekTotal: '이번 주 합계',
  mostSmokedDay: '가장 많이 흡연한 요일',
  monthTotal: '이번 달 합계',
  lastMonthTotal: '지난 달 합계',
  mostSmokedDate: '가장 많이 흡연한 날',
  
  // ============ Calendar / Weekdays ============
  day: '일',
  mon: '월',
  tue: '화',
  wed: '수',
  thu: '목',
  fri: '금',
  sat: '토',
  sun: '일',
  
  // ============ Common Actions ============
  more: '더보기 / 편집',
  back: '뒤로',
  next: '다음',
  prev: '이전',
  delete: '삭제',
  edit: '편집',
  rename: '이름 변경',
  reset: '재설정',
  close: '닫기',
  hide: '숨기기',
  save: '저장',
  cancel: '취소',
  confirm: '확인',
  confirmDelete: '이 기록을 삭제하시겠습니까?',
  editTime: '시간 편집',
  never: '없음',
  view: '보기',
  about: '정보',
  blog: '블로그',
  github: 'GitHub',
  stats: '통계',
  data: '데이터',
  hour: '시',
  minute: '분',
  count: '개수',
  
  // ============ History Page ============
  clearLocalData: '로컬 데이터 지우기',
  currentMonth: '현재 월',
  prevMonth: '이전 월',
  nextMonth: '다음 월',
  confirmClearData: '데이터 지우기 확인',
  cancelClearData: '취소',
  clearDataWarning: '경고!',
  clearDataMessage: '모든 로컬 데이터를 지우시겠습니까? 이 작업은 되돌릴 수 없습니다!',
  clearData: '데이터 지우기',
  clearDataConfirm: '이 작업은 모든 흡연 기록을 지웁니다. 이 작업은 되돌릴 수 없습니다. 계속하시겠습니까?',
  yesClear: '예, 지우기',
  loadMore: '더 불러오기',
  
  // ============ Storage / Error ============
  storageError: '저장소 오류',
  storageErrorHint: '설정으로 이동하여 오래된 데이터를 삭제하고 공간을 확보하세요.',
  goToSettings: '설정으로',
  dismiss: '닫기',
  
  // ============ History - Filters & Sort ============
  filtersAndSort: '필터 및 정렬',
  startDate: '시작 날짜',
  endDate: '종료 날짜',
  minCount: '최소 수',
  maxCount: '최대 수',
  sortByDate: '날짜순 정렬',
  sortByCount: '수량순 정렬',
  resetFilters: '필터 재설정',
  noMatchingRecords: '일치하는 기록 없음',
  totalRecords: '총 기록 수',
  filteredRecords: '필터링된 기록 수',
  dateRange: '날짜 범위',
  averagePerDay: '일일 평균',
  prevPage: '이전 페이지',
  nextPage: '다음 페이지',
  
  // ============ API Management - Feishu ============
  feishuApi: 'Feishu API',
  feishuApiSettings: 'Feishu API 설정',
  feishuDataSync: 'Feishu 데이터 동기화',
  apiUrl: 'API URL',
  syncFromFeishu: 'Feishu에서 동기화',
  syncToFeishu: 'Feishu로 동기화',
  startSync: '동기화 시작',
  syncing: '동기화 중...',
  targetTableName: '대상 테이블 이름',
  accessKey: '접근 키',
  accessKeyDialogTitle: '인증 비밀번호',
  accessKeyDialogMessage: 'Feishu API 쓰기 접근 키를 입력하세요:',
  submitToFeishu: 'Feishu로 제출',
  submitting: '제출 중...',
  startSubmit: '제출 시작',
  syncFromSupabaseOnly: 'Supabase와 동기화',
  syncDiffSummary: '동기화 요약',
  cloudOnlyRecords: '클라우드 전용 레코드',
  confirmSync: '동기화 확인',
  localRecords: '로컬 레코드',
  cloudRecords: '클라우드 레코드',
  localOnly: '로컬 전용',
  cloudOnly: '클라우드 전용',
  conflicts: '충돌',
  upload: '업로드',
  download: '다운로드',
  uploadOnly: '업로드만',
  downloadOnly: '다운로드만',
  foundNewRecords: '{count}개의 새 레코드를 찾았습니다',
  foundConflictingRecords: '{count}개의 충돌 레코드',
  needUploadRecords: '{count}개의 레코드를 업로드해야 합니다',
  needDownloadRecords: '{count}개의 레코드를 다운로드해야 합니다',
  uploadCount: '{count}개 업로드',
  downloadCount: '{count}개 다운로드',
  syncCompleted: '{source}와의 동기화가 완료되었습니다：{message}',
  syncWithFeishuCompleted: 'Feishu와의 동기화가 완료되었습니다',
  syncWithSupabaseCompleted: 'Supabase와의 동기화가 완료되었습니다',
  
  // ============ API Management - Cloud Sync ============
  cloudSync: '클라우드 동기화',
  cloudSyncing: '동기화 중...',
  loginFirst: '동기화하려면 먼저 로그인하세요',
  syncSuccess: '동기화 성공',
  syncFailed: '동기화 실패',
  noNewRecordsToSync: '동기화할 새 레코드가 없습니다',
  uploaded: '업로드됨',
  downloaded: '다운로드됨',
  records: '개',
  
  // ============ API Management - Feishu Sync Messages ============
  feishuSyncSuccess: 'Feishu API에서 동기화 성공',
  feishuSyncToStorage: 'Feishu API에서 로컬 스토리지로 동기화 성공',
  feishuSyncToCloud: 'Feishu API에서 Supabase로 동기화 성공',
  syncedRecordsFromFeishu: 'Feishu에서 {newCount}개의 새 레코드를 동기화했습니다（총：{totalCount}개，중복：{duplicateCount}개）',
  
  // ============ API Management - Import/Export Count ============
  importedCount: '가져온 수',
  totalCount: '총 수',
  submittedCount: '제출 성공 수',
  failedCount: '제출 실패 수',
  
  // ============ Auth - Sign In/Up ============
  signIn: '로그인到Supabase',
  signUp: '가입',
  signOut: '로그아웃',
  email: '이메일',
  password: '비밀번호',
  passwordPlaceholder: '비밀번호 입력',
  noAccount: '계정이 없으신가요? 가입',
  haveAccount: '이미 계정이 있으신가요? 로그인',
  loading: '로딩 중...',
  initializing: '초기화 중...',
  connecting: '연결 중...',
  authenticating: '인증 중...',
  resetting: '초기화 중...',
  restoring: '복원 중...',
  skipLogin: '로그인 건너뛰기, 로컬 모드 사용',
  verifyEmail: '가입 성공! 이메일을 확인하고 확인 링크를 클릭하세요.',
  localModeHint: '현재 로컬 모드 사용 중. 로그인하여 클라우드에 데이터 동기화',
  
  // ============ Auth - Account Management ============
  accountManagement: '계정 관리',
  uploadAvatar: '아바타 업로드',
  uploading: '업로드 중...',
  emailPlaceholder: '이메일 입력',
  updateEmail: '이메일 업데이트',
  phone: '전화번호',
  phonePlaceholder: '전화번호 입력',
  updatePhone: '전화번호 업데이트',
  
  // ============ Auth - Password Reset ============
  resetPassword: '비밀번호 재설정',
  currentPassword: '현재 비밀번호',
  newPassword: '새 비밀번호',
  confirmPassword: '비밀번호 확인',
  passwordMinLength: '비밀번호는 6자 이상이어야 합니다',
  passwordMismatch: '비밀번호가 일치하지 않습니다',
  passwordResetSuccess: '비밀번호 재설정 성공',
  passwordResetFailed: '비밀번호 재설정 실패',
  currentPasswordIncorrect: '현재 비밀번호가 올바르지 않습니다',
  signOutConfirm: '로그아웃 하시겠습니까?',
  
  // ============ Cloud Data Dialog ============
  cloudDataFound: '클라우드 데이터 발견',
  cloudDataFoundMessage: '클라우드 스토리지에 {count}개의 기록이 있습니다. 다운로드하시겠습니까?',
  downloadFromCloud: '클라우드에서 다운로드',
  skipDownload: '건너뛰기',
  loginRequired: '로그인 필요',
  loginRequiredMessage: 'Supabase API가 구성되었지만 로그인되지 않았습니다. 클라우드 데이터에 액세스하려면 로그인하세요.',
  loginNow: '지금 로그인',
  
  // ============ API Management Page ============
  apiManagement: 'API 관리',
  api: 'API',
  saveApiSettings: '저장',
  securityPassword: '보안 비밀번호',
  enterPassword: '보안 비밀번호 입력',
  selectSyncSource: '동기화 소스 선택',
  firstLaunchSyncMessage: '환영합니다! 클라우드 동기화가 설정되어 있지만 로컬 데이터가 없습니다. 기록을 가져올 데이터 소스를 선택하세요:',
  feishu: 'Feishu',
  supabase: 'Supabase',
  storageMethod: '스토리지 방식',
  dataManagement: '데이터 관리',
  storageUsage: '스토리지 사용량',
  clearOldData: '로컬 데이터 지우기',
  systemLog: '시스템 로그',
  apiUrlDisplay: '저장된 API 설정',
  apiConfigRequired: 'API 설정 필요',
  syncFromFeishuOnly: 'Feishu에서 동기화',

  deleteApiSettings: 'API 설정 삭제',
  confirmDeleteApiSettings: '저장된 API 설정을 삭제하시겠습니까?',
  renameApiSettings: 'API 설정 이름 변경',
  resetApiSettings: 'API 설정 재설정',
  enterNewPassword: 'API 설정의 새 보안 비밀번호를 입력하세요:',
  apiSettingsVerifyPassword: '저장된 API 설정에서 보안 비밀번호를 확인하여 수정하세요',
  viewSavedApiSettings: '저장된 API 설정 보기',
  
  // ============ Login Restore ============
  previousLoginFound: '이전 로그인 기록 발견',
  previousLoginFoundMessage: '이전 로그인 기록이 발견되었습니다. 로그인 상태를 복원하시겠습니까? 먼저 보안 비밀번호를 확인해야 합니다.',
  restoreLogin: '로그인 복원',
  continueLocalMode: '로컬 모드 계속',
  enterSecurityPasswordForRestore: '로그인 상태를 복원하려면 보안 비밀번호를 입력하세요',
  
  // ============ DatePicker ============
  clear: '지우기',
  today: '오늘',
  selectDate: '날짜 선택',
  selectStartDate: '시작 날짜 선택',
  selectEndDate: '종료 날짜 선택',
  
  // ============ Months ============
  january: '1월',
  february: '2월',
  march: '3월',
  april: '4월',
  may: '5월',
  june: '6월',
  july: '7월',
  august: '8월',
  september: '9월',
  october: '10월',
  november: '11월',
  december: '12월',
  
  // ============ Weekdays Short ============
  sunday: '일',
  monday: '월',
  tuesday: '화',
  wednesday: '수',
  thursday: '목',
  friday: '금',
  saturday: '토',
  
  // ============ Color Picker ============
  presetColors: '프리셋 색상',
  
  // ============ Font Attribution ============
  fontAttribution: '본 앱은 Huawei Device Co., Ltd.에서 제공하는 HarmonyOS Sans 글꼴을 사용합니다.',
  
  // ============ API Management - Anon Key ============
  anonKey: '익명 키',
  
  // ============ Analysis Page - Operation Log ============
  operationLog: '작업 로그',
  feishuPasswordRequired: 'Feishu API 설정이 암호화되어 있습니다. 복호화하려면 비밀번호를 입력하세요.',
  apiPasswordRequired: 'API 설정이 암호화되어 있습니다. 복호화하려면 비밀번호를 입력하세요.',
  passwordError: '비밀번호가 올바르지 않습니다. 다시 시도해주세요.',
  
  // ============ Settings - Messages ============
  dataCleared: '데이터가 삭제되었습니다',
  
  // ============ Settings - Error Messages ============
  bucketNotExist: '스토리지 버킷이 존재하지 않습니다. 관리자에게 문의하세요',
  permissionDenied: '권한이 거부되었습니다. 버킷 설정을 확인하세요',
  avatarUploadSuccess: '아바타가 성공적으로 업로드되었습니다',
  avatarUploadFailed: '아바타 업로드 실패',
  enterEmail: '이메일 주소를 입력하세요',
  emailBindSuccess: '이메일이 성공적으로 바인딩되었습니다. 인증 이메일을 확인하세요',
  emailBindFailed: '이메일 바인딩 실패',
  passwordSameAsCurrent: '새 비밀번호는 현재 비밀번호와 같을 수 없습니다',
  passwordRequirements: '비밀번호 요구사항: 6자 이상',
  signOutFailed: '로그아웃 실패',
  
  // ============ Settings - Storage Info ============
  used: '사용됨',
  mb: 'MB',
  
  // ============ Settings - Language Names ============
  chinese: '중국어',
  japanese: '일본어',
  korean: '한국어',
  
  // ============ Operation Log Component ============
  noOperationRecords: '작업 기록 없음',
  showRecent5: '최근 5개 표시',
  showAll: '모두 표시',
  clearAll: '모두 지우기',
  createRecord: '기록 생성',
  updateRecord: '기록 수정',
  deleteRecord: '기록 삭제',
  
  // ============ External Link Warning ============
  externalLinkWarningTitle: '외부 링크',
  externalLinkWarningMessage: '외부 웹사이트를 방문하려고 합니다:',
  externalLinkContinue: '계속',
  externalLinkCancel: '취소',
  
  // ============ Sync - Field Update ============
  fieldsToUpdate: '필드 업데이트',
  localDataNewerThanFeishu: '로컬 데이터가 Feishu보다 최신입니다. 먼저 로컬 데이터를 백업하세요',
  localDataNewerThanCloud: '로컬 데이터가 클라우드보다 최신입니다. 먼저 로컬 데이터를 업로드하세요',
  
  // ============ Error Boundary ============
  somethingWentWrong: '오류가 발생했습니다',
  unexpectedError: '예기치 않은 오류가 발생했습니다',
  tryAgain: '다시 시도',
  refreshPage: '페이지 새로고침',
  errorDetails: '오류 세부 정보',
  
  // ============ Supabase Guide ============
  howToGetSupabaseApi: 'Supabase API 자격 증명을 어떻게 얻나요?',
  supabaseGuideTitle: 'Supabase 설정 가이드',
  whatIsSupabase: 'Supabase란?',
  supabaseDescription: 'Supabase는 PostgreSQL 데이터베이스, 인증, 실시간 구독 등을 제공하는 오픈소스 Firebase 대안입니다.',
  supabaseUseCase: 'Supabase를 사용하면 흡연 기록을 여러 기기에서 동기화하고 클라우드에 안전하게 백업할 수 있습니다.',
  supabaseRequirements: '필요한 것:',
  supabaseEmail: 'Supabase 계정을注册하는 데 사용할 이메일 주소',
  supabaseTime: '설정 완료까지 약 5~10분',
  supabaseStep1: '1단계: Supabase 계정 등록',
  supabaseStep1Desc1: 'Supabase 웹사이트 방문:',
  supabaseStep1Desc2: '오른쪽 상단의 "Start your project" 또는 "Sign In" 클릭',
  supabaseStep1Desc3: 'GitHub 계정 또는 이메�로 등록/로그인',
  supabaseTip: '💡 팁: Supabase는 개인 사용에 충분한 무료 플랜을 제공합니다.',
  supabaseStep2: '2단계: 새 프로젝트 만들기',
  supabaseStep2Desc1: '로그인 후 "New project" 버튼 클릭',
  supabaseStep2Desc2: '조직 선택 또는 만들기',
  supabaseStep2Desc3: '프로젝트 정보 입력:',
  supabaseStep2Desc4: 'Name: 프로젝트 이름 (예: popsmoke-tracker)',
  supabaseStep2Desc5: 'Database Password: 데이터베이스 비밀번호 설정 (기억하세요)',
  supabaseStep2Desc6: 'Region: 가장 가까운 서버 지역 선택',
  supabaseStep2Desc7: '"Create new project"를 클릭하여 프로젝트 만들기',
  supabaseNote: '⏳ 참고: 프로젝트 생성에는 1~2분이 소요되므로 잠시 기다려주세요.',
  supabaseStep3: '3단계: API 자격 증명 가져오기',
  supabaseStep3Desc1: '프로젝트 생성 후 프로젝트 대시보드로 이동',
  supabaseStep3Desc2: '왼쪽 메뉴의 "Project Settings" 클릭',
  supabaseStep3Desc3: '"API" 탭 선택',
  supabaseStep3Desc4: '"Project API keys" 섹션에서 다음 정보를 찾으세요:',
  supabaseProjectUrl: 'Project URL:',
  supabaseAnonKey: 'anon public:',
  supabaseStep4: '4단계: 이 앱에서 구성',
  supabaseStep4Desc1: '"Project URL"을 이 앱의 <strong>SUPABASE_URL</strong> 필드에 복사',
  supabaseStep4Desc2: '"anon public" 키를 이 앱의 <strong>SUPABASE_ANON_KEY</strong> 필드에 복사',
  supabaseStep4Desc3: '저장 버튼을 클릭하여 설정 완료',
  supabaseAfterConfig: '✅ 설정 후 다음과 같은 것이 가능합니다:',
  supabaseSyncDevices: '다른 기기에서 흡연 기록 동기화',
  supabaseCloudBackup: '데이터 손실을 방지하는 클라우드 백업',
  supabaseCloudSync: '클라우드 동기화를 사용하여 데이터 다운로드/업로드',
  supabasePrev: '이전',
  supabaseNext: '다음',
  supabaseComplete: '완료'
};

export type I18nKO = typeof i18n_KO;

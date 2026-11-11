#!/usr/bin/env node
/**
 * 从 web 端的 i18n_*.ts 生成 Android 的 PopStrings.kt。
 *
 * 为什么生成而不是手抄：四个页面加起来要用到两百多个键、四种语言，
 * 手抄必然漏译，而且 web 端文案一改，原生端会悄悄落后。
 * 生成出来的文件顶部有 DO NOT EDIT，改文案请改 i18n_*.ts。
 *
 * 用法：
 *   node tools/gen-strings.mjs          生成
 *   node tools/gen-strings.mjs --check  只校验是否与源一致（CI 用）
 */
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const OUT = resolve(
  ROOT,
  'android-native/app/src/main/java/com/popsmoke/tracker/i18n/PopStrings.kt',
);

const LANGS = [
  { code: 'EN', file: 'i18n/i18n_EN.ts', exportName: 'i18n_EN' },
  { code: 'ZH', file: 'i18n/i18n_ZH.ts', exportName: 'i18n_ZH' },
  { code: 'JA', file: 'i18n/i18n_JP.ts', exportName: 'i18n_JP' },
  { code: 'KO', file: 'i18n/i18n_KO.ts', exportName: 'i18n_KO' },
];

/** 把 TS 文件里的对象字面量取出来求值 —— 内容全是纯数据，没有 import 依赖。 */
function loadTable(relPath, exportName) {
  const src = readFileSync(resolve(ROOT, relPath), 'utf8');
  const start = src.indexOf('{', src.indexOf(`export const ${exportName}`));
  // 从第一个 { 开始做括号配对，忽略字符串内的括号
  let depth = 0;
  let inString = null;
  let end = -1;
  for (let i = start; i < src.length; i++) {
    const ch = src[i];
    if (inString) {
      if (ch === '\\') i++;
      else if (ch === inString) inString = null;
      continue;
    }
    if (ch === "'" || ch === '"') {
      inString = ch;
    } else if (ch === '{') depth++;
    else if (ch === '}') {
      depth--;
      if (depth === 0) {
        end = i;
        break;
      }
    }
  }
  if (end < 0) throw new Error(`${relPath}: 找不到对象字面量的结尾`);
  const literal = src.slice(start, end + 1);
  // eslint-disable-next-line no-new-func
  return new Function(`return (${literal});`)();
}

const tables = LANGS.map((l) => ({ ...l, table: loadTable(l.file, l.exportName) }));

/**
 * 原生独有文案：web 端没有对应句子。
 *
 * 主要是无障碍朗读（TalkBack）和「本批尚未接入」这类只在原生存在的状态。
 * 语言缺失时自动回落英文（下面的合并逻辑），所以不必四种语言都齐。
 */
const NATIVE_ONLY = {
  comingSoon: {
    EN: 'This screen arrives in a later batch',
    ZH: '此屏将在后续批次接入',
    JA: 'この画面は次のバッチで実装されます',
    KO: '이 화면은 다음 배치에서 구현됩니다',
  },
  cdThemeColor: {
    EN: 'Change theme color',
    ZH: '切换主题颜色',
    JA: 'テーマ色を変更',
    KO: '테마 색상 변경',
  },
  cdLanguage: {
    EN: 'Change language',
    ZH: '切换语言',
    JA: '言語を変更',
    KO: '언어 변경',
  },
  cdRecentLogs: {
    EN: "View today's logs",
    ZH: '查看今日记录',
    JA: '今日の記録を見る',
    KO: '오늘의 기록 보기',
  },
  cdBackToDashboard: {
    EN: 'Back to dashboard',
    ZH: '返回仪表盘',
    JA: 'ダッシュボードに戻る',
    KO: '대시보드로 돌아가기',
  },
  cdDeleteRecord: {
    EN: 'Delete record',
    ZH: '删除记录',
    JA: '記録を削除',
    KO: '기록 삭제',
  },
  cdRecordNow: {
    EN: 'Log a smoke now',
    ZH: '立即记录一支',
    JA: '今すぐ記録する',
    KO: '지금 기록하기',
  },
  cdExpandMonth: {
    EN: 'Expand month',
    ZH: '展开月份',
    JA: '月を展開',
    KO: '월 펼치기',
  },
  cdCollapseMonth: {
    EN: 'Collapse month',
    ZH: '收起月份',
    JA: '月を折りたたむ',
    KO: '월 접기',
  },
  cdOpenDatePicker: {
    EN: 'Open date picker',
    ZH: '打开日期选择',
    JA: '日付選択を開く',
    KO: '날짜 선택 열기',
  },
  cdDecrease: {
    EN: 'Decrease',
    ZH: '减少',
    JA: '減らす',
    KO: '감소',
  },
  cdIncrease: {
    EN: 'Increase',
    ZH: '增加',
    JA: '増やす',
    KO: '증가',
  },
  cdEditRecord: {
    EN: 'Edit record time',
    ZH: '修改记录时间',
    JA: '記録時刻を編集',
    KO: '기록 시간 수정',
  },
  // 操作日志（Web 版把这些词写死在 PopOperationLog.tsx 里，没有进 i18n）
  opCreate: {
    EN: 'Record added',
    ZH: '新增记录',
    JA: '記録を追加',
    KO: '기록 추가',
  },
  opUpdate: {
    EN: 'Time updated',
    ZH: '修改时间',
    JA: '時刻を変更',
    KO: '시간 수정',
  },
  opDelete: {
    EN: 'Record deleted',
    ZH: '删除记录',
    JA: '記録を削除',
    KO: '기록 삭제',
  },
  opClear: {
    EN: 'Records cleared',
    ZH: '清空记录',
    JA: '記録を消去',
    KO: '기록 비우기',
  },
  opSync: {
    EN: 'Backup imported',
    ZH: '导入备份',
    JA: 'バックアップを読み込み',
    KO: '백업 가져오기',
  },
  opSystem: {
    EN: 'System',
    ZH: '系统',
    JA: 'システム',
    KO: '시스템',
  },
  opPause: {
    EN: 'PAUSE',
    ZH: '暂停',
    JA: '一時停止',
    KO: '일시정지',
  },
  opResume: {
    EN: 'RESUME',
    ZH: '继续',
    JA: '再開',
    KO: '계속',
  },
  opClearLog: {
    EN: 'CLEAR',
    ZH: '清空',
    JA: 'クリア',
    KO: '지우기',
  },
  opEntries: {
    EN: 'entries',
    ZH: '条记录',
    JA: '件',
    KO: '항목',
  },
  opLive: {
    EN: 'LIVE',
    ZH: '实时',
    JA: 'ライブ',
    KO: '라이브',
  },
  opPaused: {
    EN: 'PAUSED',
    ZH: '已暂停',
    JA: '一時停止中',
    KO: '일시정지됨',
  },
  opEmpty: {
    EN: 'No operation records',
    ZH: '暂无操作记录',
    JA: '操作記録なし',
    KO: '작업 기록 없음',
  },
  // 备份 / 恢复
  backupSection: {
    EN: 'Backup & Restore',
    ZH: '备份与恢复',
    JA: 'バックアップと復元',
    KO: '백업 및 복원',
  },
  backupExport: {
    EN: 'Export backup (JSON)',
    ZH: '导出备份（JSON）',
    JA: 'バックアップを書き出す（JSON）',
    KO: '백업 내보내기(JSON)',
  },
  backupImport: {
    EN: 'Import backup',
    ZH: '导入备份',
    JA: 'バックアップを読み込む',
    KO: '백업 가져오기',
  },
  backupNote: {
    EN: 'A JSON file with every record and setting. Import it after switching or reinstalling.',
    ZH: '包含全部记录与设置的 JSON 文件；换机或重装后可完整恢复。',
    JA: '全記録と設定を含む JSON ファイル。機種変更や再インストール後に復元できます。',
    KO: '모든 기록과 설정이 담긴 JSON 파일입니다. 기기 변경이나 재설치 후 복원하세요.',
  },
  backupExported: {
    EN: 'Exported',
    ZH: '已导出',
    JA: '書き出しました',
    KO: '내보냈습니다',
  },
  backupInvalid: {
    EN: 'This file is not a valid backup',
    ZH: '这不是有效的备份文件',
    JA: '有効なバックアップではありません',
    KO: '유효한 백업 파일이 아닙니다',
  },
  importModeTitle: {
    EN: 'Import mode',
    ZH: '导入方式',
    JA: '読み込み方法',
    KO: '가져오기 방식',
  },
  importModeMessage: {
    EN: 'Merge keeps current records and adds the missing ones. Replace swaps everything, settings included.',
    ZH: '合并会保留现有记录并补齐缺失的记录；覆盖会用备份替换全部记录与设置。',
    JA: '統合は現在の記録を残して不足分を追加します。置き換えは設定を含めてすべて入れ替えます。',
    KO: '병합은 현재 기록을 유지하고 빠진 기록을 추가합니다. 교체는 설정까지 전부 바꿉니다.',
  },
  importMerge: {
    EN: 'Merge',
    ZH: '合并',
    JA: '統合',
    KO: '병합',
  },
  importReplace: {
    EN: 'Replace all',
    ZH: '覆盖全部',
    JA: 'すべて置き換え',
    KO: '전부 교체',
  },
  backupImported: {
    EN: 'Imported',
    ZH: '已导入',
    JA: '読み込みました',
    KO: '가져왔습니다',
  },
  // 云端账户与同步（原生版新增：Web 把登录做成整页弹窗，原生收进设置页一张卡）
  cloudAccount: {
    EN: 'Cloud account',
    ZH: '云端账户',
    JA: 'クラウドアカウント',
    KO: '클라우드 계정',
  },
  cloudAccountHint: {
    EN: 'Sign in with Supabase to sync limits and theme across devices.',
    ZH: '用 Supabase 账户登录，同步每日上限、阈值与主题。',
    JA: 'Supabase でログインすると上限・しきい値・テーマを端末間で同期します。',
    KO: 'Supabase로 로그인하면 일일 한도·경고 기준·테마를 기기 간에 동기화합니다.',
  },
  cloudNotConfigured: {
    EN: 'Supabase is not configured yet. Add the URL and anon key on the API page.',
    ZH: '尚未配置 Supabase。请到接口页填写地址与 anon key。',
    JA: 'Supabase が未設定です。API ページで URL と anon key を入力してください。',
    KO: 'Supabase가 설정되지 않았습니다. API 페이지에서 URL과 anon key를 입력하세요.',
  },
  cloudSettingsUp: {
    EN: 'Upload settings',
    ZH: '上传设置',
    JA: '設定をアップロード',
    KO: '설정 업로드',
  },
  cloudSettingsDown: {
    EN: 'Restore settings',
    ZH: '从云端恢复',
    JA: '設定を復元',
    KO: '설정 복원',
  },
  cloudConflictTitle: {
    EN: 'Settings differ',
    ZH: '设置不一致',
    JA: '設定が一致しません',
    KO: '설정이 다릅니다',
  },
  cloudConflictMessage: {
    EN: 'Cloud settings differ from this device. Which one should win?',
    ZH: '云端保存的设置与本地不同，用哪一份？',
    JA: 'クラウドの設定がこの端末と異なります。どちらを使いますか？',
    KO: '클라우드 설정이 이 기기와 다릅니다. 어느 쪽을 사용할까요?',
  },
  cloudUseCloud: {
    EN: 'Use cloud',
    ZH: '使用云端',
    JA: 'クラウドを使う',
    KO: '클라우드 사용',
  },
  cloudKeepLocal: {
    EN: 'Keep local',
    ZH: '保留本地',
    JA: 'ローカルを残す',
    KO: '로컬 유지',
  },
  feishuBackupNow: {
    EN: 'Back up to Feishu',
    ZH: '备份到飞书',
    JA: 'Feishu にバックアップ',
    KO: 'Feishu로 백업',
  },
  feishuRestoreNow: {
    EN: 'Restore from Feishu',
    ZH: '从飞书恢复',
    JA: 'Feishu から復元',
    KO: 'Feishu에서 복원',
  },
  feishuSyncHint: {
    EN: 'Only records missing from the sheet are uploaded; duplicates are skipped by id.',
    ZH: '只上传表里还没有的记录，按 id 去重，不会重复写入。',
    JA: 'シートにない記録だけをアップロードします（id で重複を判定）。',
    KO: '시트에 없는 기록만 업로드하며 id로 중복을 걸러냅니다.',
  },
  loginAction: {
    EN: 'Sign in',
    ZH: '登录',
    JA: 'ログイン',
    KO: '로그인',
  },
  emailUnverified: {
    EN: 'Confirmation email sent. Verify it, then sign in.',
    ZH: '验证邮件已发送，请先到邮箱确认再登录。',
    JA: '確認メールを送信しました。メールを確認してからログインしてください。',
    KO: '확인 메일을 보냈습니다. 메일 확인 후 로그인하세요.',
  },
  cloudUploaded: {
    EN: '{n} uploaded',
    ZH: '上传 {n} 条',
    JA: '{n} 件アップロード',
    KO: '{n}건 업로드',
  },
  cloudSkipped: {
    EN: '{n} already there',
    ZH: '已存在 {n} 条',
    JA: '{n} 件は既存',
    KO: '{n}건 기존',
  },
  cloudFailedCount: {
    EN: '{n} failed',
    ZH: '失败 {n} 条',
    JA: '{n} 件失敗',
    KO: '{n}건 실패',
  },
  cloudRestoredCount: {
    EN: '{n} new records',
    ZH: '新增 {n} 条记录',
    JA: '{n} 件の新しい記録',
    KO: '새 기록 {n}건',
  },
  cloudUpToDate: {
    EN: 'Already up to date',
    ZH: '已是最新',
    JA: 'すでに最新です',
    KO: '이미 최신입니다',
  },
  cloudDownloaded: {
    EN: '{n} downloaded',
    ZH: '下载 {n} 条',
    JA: '{n} 件ダウンロード',
    KO: '{n}건 다운로드',
  },
  syncNothing: {
    EN: 'Already up to date',
    ZH: '已是最新',
    JA: 'すでに最新です',
    KO: '이미 최신입니다',
  },
  syncPendingCount: {
    EN: '{n} pending',
    ZH: '待同步 {n} 条',
    JA: '保留中 {n} 件',
    KO: '대기 {n}건',
  },
  syncNow: {
    EN: 'Sync Now',
    ZH: '立即同步',
    JA: '今すぐ同期',
    KO: '지금 동기화',
  },
  syncQueue: {
    EN: 'Sync Queue',
    ZH: '同步队列',
    JA: '同期キュー',
    KO: '동기화 큐',
  },
  syncQueueEmpty: {
    EN: 'No operations in queue',
    ZH: '队列为空',
    JA: 'キューに操作なし',
    KO: '큐에 작업 없음',
  },
  syncAll: {
    EN: 'Sync All',
    ZH: '全部同步',
    JA: '全て同期',
    KO: '전체 동기화',
  },
  syncOne: {
    EN: 'Sync',
    ZH: '同步',
    JA: '同期',
    KO: '동기화',
  },
  syncRemove: {
    EN: 'Remove',
    ZH: '移除',
    JA: '削除',
    KO: '제거',
  },
  syncClearQueue: {
    EN: 'Clear',
    ZH: '清空',
    JA: 'クリア',
    KO: '비우기',
  },
  syncEntries: {
    EN: 'entries',
    ZH: '条记录',
    JA: 'エントリー',
    KO: '항목',
  },
  syncPendingState: {
    EN: 'PENDING',
    ZH: '待同步',
    JA: '保留中',
    KO: '대기중',
  },
  syncFailedState: {
    EN: 'FAILED',
    ZH: '失败',
    JA: '失敗',
    KO: '실패',
  },
  syncProcessing: {
    EN: 'PROCESSING',
    ZH: '处理中',
    JA: '処理中',
    KO: '처리중',
  },
  syncReady: {
    EN: 'READY',
    ZH: '就绪',
    JA: '準備完了',
    KO: '준비됨',
  },
  syncPending: {
    EN: 'Pending',
    ZH: '待同步',
    JA: '保留中',
    KO: '대기',
  },
  syncClearConfirm: {
    EN: 'Clear the whole queue?',
    ZH: '确定清空整个队列吗？',
    JA: 'キューを全てクリアしますか？',
    KO: '큐를 모두 비우시겠습니까?',
  },
  syncHasFailedHint: {
    EN: 'Some changes failed to sync — retry from the queue below',
    ZH: '有改动同步失败，可在下方队列里重试',
    JA: '同期に失敗した変更があります。下のキューから再試行できます',
    KO: '동기화에 실패한 변경이 있습니다. 아래 큐에서 다시 시도하세요',
  },
  syncReadyHint: {
    EN: 'All local changes are synced',
    ZH: '本地改动已全部同步',
    JA: 'ローカルの変更はすべて同期済みです',
    KO: '로컬 변경이 모두 동기화되었습니다',
  },
  syncRemoveConfirm: {
    EN: 'Remove this operation from the queue?',
    ZH: '确定从队列中移除这条操作吗？',
    JA: 'この操作をキューから削除しますか？',
    KO: '이 작업을 큐에서 제거하시겠습니까?',
  },
  signedInAs: {
    EN: 'Signed in',
    ZH: '已登录',
    JA: 'ログイン中',
    KO: '로그인됨',
  },
  issueNetwork: {
    EN: 'Network unavailable. Check your connection.',
    ZH: '网络不可用，请检查连接。',
    JA: 'ネットワークに接続できません。',
    KO: '네트워크에 연결할 수 없습니다.',
  },
  issueCredentials: {
    EN: 'Wrong email or password.',
    ZH: '邮箱或密码不正确。',
    JA: 'メールアドレスまたはパスワードが違います。',
    KO: '이메일 또는 비밀번호가 틀렸습니다.',
  },
  issueEmailNotConfirmed: {
    EN: 'Email not confirmed yet.',
    ZH: '邮箱尚未验证。',
    JA: 'メールが未確認です。',
    KO: '이메일이 아직 확인되지 않았습니다.',
  },
  issueSessionExpired: {
    EN: 'Session expired. Sign in again.',
    ZH: '登录已过期，请重新登录。',
    JA: 'セッションが切れました。再ログインしてください。',
    KO: '세션이 만료되었습니다. 다시 로그인하세요.',
  },
  issueServer: {
    EN: 'Server error. Try again later.',
    ZH: '服务端出错，请稍后再试。',
    JA: 'サーバーエラーです。後でもう一度お試しください。',
    KO: '서버 오류입니다. 잠시 후 다시 시도하세요.',
  },
  issueTableMissing: {
    EN: 'Table not found. Check the Supabase schema.',
    ZH: '找不到数据表，请检查 Supabase 表结构。',
    JA: 'テーブルが見つかりません。Supabase のスキーマを確認してください。',
    KO: '테이블을 찾을 수 없습니다. Supabase 스키마를 확인하세요.',
  },
  issueBadResponse: {
    EN: 'Unexpected response from the server.',
    ZH: '服务端返回了无法识别的响应。',
    JA: 'サーバーの応答を解釈できません。',
    KO: '서버 응답을 해석할 수 없습니다.',
  },
  issueNoConfig: {
    EN: 'API is not configured.',
    ZH: '尚未配置 API。',
    JA: 'API が未設定です。',
    KO: 'API가 설정되지 않았습니다.',
  },
  issueWeakPassword: {
    EN: 'Password needs at least 6 characters.',
    ZH: '密码至少 6 位。',
    JA: 'パスワードは 6 文字以上必要です。',
    KO: '비밀번호는 6자 이상이어야 합니다.',
  },
  issueAlreadyRegistered: {
    EN: 'This email is already registered.',
    ZH: '该邮箱已注册。',
    JA: 'このメールは登録済みです。',
    KO: '이미 등록된 이메일입니다.',
  },
  issueMissingFields: {
    EN: 'Fill in both email and password.',
    ZH: '请填写邮箱和密码。',
    JA: 'メールとパスワードを入力してください。',
    KO: '이메일과 비밀번호를 입력하세요.',
  },
  issueNoCloudSettings: {
    EN: 'No settings saved in the cloud yet.',
    ZH: '云端还没有保存过设置。',
    JA: 'クラウドに設定がまだありません。',
    KO: '클라우드에 저장된 설정이 없습니다.',
  },
  issueRateLimited: {
    EN: 'Too many attempts. Wait a moment.',
    ZH: '尝试过于频繁，请稍后再试。',
    JA: '試行が多すぎます。少し待ってください。',
    KO: '시도가 너무 많습니다. 잠시 후 다시 시도하세요.',
  },
  issueUnknown: {
    EN: 'Something went wrong.',
    ZH: '出了点问题。',
    JA: '問題が発生しました。',
    KO: '문제가 발생했습니다.',
  },
};

// 合并原生独有键：按语言分别注入
for (const entry of tables) {
  for (const [key, byLang] of Object.entries(NATIVE_ONLY)) {
    const value = byLang[entry.code];
    if (value !== undefined && entry.table[key] === undefined) {
      entry.table[key] = value;
    }
  }
}

// EN 是基准：所有键以它为准，其余语言缺失时回落到 EN（运行时行为，不是静默失败）
const keys = Object.keys(tables[0].table);

// Kotlin 硬关键字做属性名要加反引号
const HARD_KEYWORDS = new Set([
  'as', 'break', 'class', 'continue', 'do', 'else', 'false', 'for', 'fun', 'if',
  'in', 'interface', 'is', 'null', 'object', 'package', 'return', 'super', 'this',
  'throw', 'true', 'try', 'typealias', 'typeof', 'val', 'var', 'when', 'while',
]);

function propName(key) {
  return HARD_KEYWORDS.has(key) ? `\`${key}\`` : key;
}

function kotlinString(value) {
  return `"${String(value)
    .replace(/\\/g, '\\\\')
    .replace(/"/g, '\\"')
    .replace(/\$/g, '\\$')
    .replace(/\r?\n/g, '\\n')}"`;
}

const lines = [];
const out = (s = '') => lines.push(s);

out('package com.popsmoke.tracker.i18n');
out('');
out('// DO NOT EDIT —— 本文件由 tools/gen-strings.mjs 从 i18n/i18n_*.ts 生成。');
out('// 改文案请改 web 端的 i18n 源文件，然后跑 `node tools/gen-strings.mjs`。');
out('//');
out('// 缺失的键在运行时回落到英文，而不是抛异常或显示空白：');
out('// 宁可露出一句英文，也不要在界面上留一个洞。');
out('');
out(`val PopStringKeyCount = ${keys.length}`);
out('');

// ---- 各语言表 ----
tables.forEach(({ code, table }) => {
  out(`private val ${code}_TABLE: Map<String, String> = mapOf(`);
  keys.forEach((k) => {
    const value = table[k];
    if (value === undefined) return; // 未翻译的键直接不写，运行时回落
    out(`    ${kotlinString(k)} to ${kotlinString(value)},`);
  });
  out(')');
  out('');
});

// ---- 访问类 ----
out('/** 类型安全的文案访问器：每个键都是一个只读属性，拼错编译期就报错。 */');
out('class PopStrings(private val table: Map<String, String>) {');
out('    /** 取文案；本语言缺失时回落英文，仍缺失则返回键名（截图里一眼能看见）。 */');
out('    operator fun get(key: String): String = table[key] ?: EN_TABLE[key] ?: key');
out('');
keys.forEach((k) => {
  out(`    val ${propName(k)}: String get() = get(${kotlinString(k)})`);
});
out('}');
out('');
out('val EN = PopStrings(EN_TABLE)');
out('val ZH = PopStrings(ZH_TABLE)');
out('val JA = PopStrings(JA_TABLE)');
out('val KO = PopStrings(KO_TABLE)');
out('');
out('fun popStrings(language: String): PopStrings = when (language) {');
out('    "zh" -> ZH');
out('    "ja" -> JA');
out('    "ko" -> KO');
out('    else -> EN');
out('}');
out('');
out('/** 时段标签的键（见 DayPart.labelKey）→ 本地化文案。 */');
out('fun PopStrings.dayPartLabel(key: String): String = when (key) {');
out('    "lateNight" -> lateNight');
out('    "earlyMorning" -> earlyMorning');
out('    "morning" -> morning');
out('    "noon" -> noon');
out('    "afternoon" -> afternoon');
out('    else -> evening');
out('}');
out('');

const content = lines.join('\n');

// 未翻译键的提醒（不阻断，但要说清）
const missing = tables
  .slice(1)
  .map(({ code, table }) => {
    const gaps = keys.filter((k) => table[k] === undefined);
    return gaps.length ? `${code} 缺 ${gaps.length} 个键（回落英文）` : null;
  })
  .filter(Boolean);

if (process.argv.includes('--check')) {
  const current = existsSync(OUT) ? readFileSync(OUT, 'utf8') : '';
  if (current !== content) {
    console.error('[gen-strings] PopStrings.kt 与 i18n 源不一致，请重新生成');
    process.exit(1);
  }
  console.log(`[gen-strings] 一致（${keys.length} 键 × ${LANGS.length} 语言）`);
} else {
  writeFileSync(OUT, content);
  console.log(`[gen-strings] 已生成 ${keys.length} 键 × ${LANGS.length} 语言`);
}

if (missing.length) console.warn('[gen-strings] ' + missing.join('；'));

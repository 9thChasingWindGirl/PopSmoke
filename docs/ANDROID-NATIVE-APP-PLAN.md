> ⚠️ **本文档已被 `NATIVE-REWRITE-PLAN.md`（原生重写方案）取代，仅作存档。**
> 路线已改为「Kotlin + Compose 原生实现 + 鸿蒙 ArkTS 预留」，不再走 Capacitor 容器。
> 保留价值：§1.2 的 CDN / 字体 / 签名问题清单，对**继续维护 Web 版**仍然适用。

# PopSmoke 安卓 App 方案 v1（原生容器 + GitHub Actions 编译）—— 已废弃

> 版本：v1 · 针对仓库当前状态（`0.4.30-beta`，Capacitor 8 / targetSdk 36 / AGP 8.13.0）
> 目标：产出一个可安装、可上架、离线可用、编译全在 GitHub Actions 完成的安卓 App，且**前端视觉零改动**。

---

## 0. 一页结论

| 项 | 结论 |
|---|---|
| 技术路线 | **方案 A：Capacitor 8 原生容器 + 现有 React 前端**（原生 Shell 承载 Web 资产，原生插件做数据/文件/分享/生命期）。不建议全量 Compose 重写。 |
| 为什么 | UI 是高度定制化的波普艺术视觉（4px 黑描边 + 硬阴影 + Bangers 字体 + 翻转卡 + 半环仪表 + 旋转标签），Compose 逐像素复刻成本远高于收益，且必然出现视觉漂移；而现有 `services/` 里 3000+ 行业务逻辑（存储适配、双云同步、加密、队列）几乎全部要重写。 |
| 「无损」的定义 | 不是"看起来差不多"，而是**以当前 CDN 渲染结果为视觉基线，重构后截图 diff = 0**。基线先固化，再改代码（见 §4.3）。 |
| 最大风险 | 当前 `index.html` 用 **Tailwind Play CDN + Google Fonts CDN** 出样式。APK 内 WebView 一旦离线或访问不到 CDN（国内尤其），**整个 App 会变成无样式纯文本**。这是本方案必须最先解决的事，不是优化项。 |
| 次高风险 | ① `android/.../values/colors.xml` 缺失但 `styles.xml` 引用了 `@color/colorPrimary` → **AAPT 资源链接必然报错，当前 CI 构建跑不通**；② release 无签名配置；③ versionCode 恒定 1。 |
| 交付物 | 可安装 APK（debug/beta）+ 已签名 APK/AAB（stable）+ 视觉回归报告，全部由 Actions 产出。 |

---

## 1. 现状体检

### 1.1 前端设计资产（这是要"无损保留"的东西）

**设计令牌**（`styles/designSystem.ts`）

- 色：`primary.black #000` / `white` / `paper #fdfbf7` / `background #f0f0f0`；6 个主题色 `#FFD700 #FF6B6B #4ECDC4 #C7F464 #A78BFA #FF9F43`，运行时可由用户改色（`settings.themeColor` 直接注入 inline style，且用 `isLightColor()` 动态决定文字黑白）。
- 描边：默认 4px 纯黑，细 2px，粗 6px；圆角基本为 0（局部 `rounded-2xl`）。
- 阴影：`4px 4px 0 0 #000`（默认）/ `2px 2px`（hover）/ `8px 8px`（大卡）/ `1px 1px`；外加 inset 高光 `inset 0 4px 0 rgba(255,255,255,.4), inset 0 -4px 0 rgba(0,0,0,.1)`。
- 字体：`display` = **Bangers**（英文/数字）+ Noto Sans SC/JP/KR 900；`body` = **HarmonyOS Sans**（本地字体）+ 系统 CJK 兜底。
- 动效：`slideIn / popBlast / slideDown / slideUp / shake`，另有 `animate-pulse`、`animate-bounce`。
- 版式：body 为 `#f0f0f0` + 20px 网点 `radial-gradient` 背景；`html` 根字号按断点 14/15/16px；顶部固定 60px 导航（桌面展开 5 个 tab，移动端折叠成底部 tab 栏）；内容区 `max-w-4xl` 居中。

**组件与页面**

- 5 个页面：`PopDashboard`（半环 SVG 仪表 + 3D 翻转卡 + BOOM 记录按钮）、`PopAnalysis`（Recharts 图表 + CSV 导出）、`PopHistory`、`PopAPI`（Supabase/飞书配置与同步）、`PopSettings`。
- 26 个 UI 组件：`PopCard / PopButton / PopNav / PopDropdown / PopColorPicker / PopDatePicker / PopTimePicker / PopSelect / PopConfirm / PopPrompt / PopNotification / PopLoading / PopAuthModal / PopCloudDataDialog / PopSystemLog / PopSyncQueue / PopOperationLog / PopSupabaseGuide / PopUpdateDialog / PopPasswordResetDialog / PopExternalLinkWarning / PopStorageErrorDialog / PopForm / PopInput / PageContainer`。
- 状态：zustand（`authStore / appStore / dataStore`）+ 自定义 hooks（`useInitialization / useDataSync / useCloudData`）。

**结论**：视觉几乎全部由 Tailwind 工具类（含大量任意值如 `max-w-[420px]`、`shadow-[2px_2px_0px...]`、`[&::-webkit-scrollbar]:hidden`、`group-active:translate-x-1`）+ 少量 inline style 构成。这意味着：**只要 Tailwind 的生成结果一致，视觉就一致**。这就是「无损」的可工程化切入点。

### 1.2 技术底座与阻塞项

| 级别 | 问题 | 证据 | 影响 |
|---|---|---|---|
| **P0** | 样式靠 CDN 运行时生成 | `index.html:10` `<script src="https://cdn.tailwindcss.com">`；仓库内 **0 个 `.css` 文件**，`@tailwindcss/vite` 装了但 `vite.config.ts` 根本没用 | 离线/被墙 → 全站无样式；首屏还要等 JIT 编译，冷启动白屏 |
| **P0** | 缺 `res/values/colors.xml` | `styles.xml` 引用 `@color/colorPrimary / colorPrimaryDark / colorAccent`，全仓库无 `colors.xml` | `processDebugResources` 直接失败，**CI 现在编不出包** |
| **P0** | release 未签名 | `app/build.gradle` 无 `signingConfigs` | `assembleRelease` 产出 `*-unsigned.apk`，装不上；上架也不行 |
| **P0** | versionCode 恒定 1 | workflow 从不传 `-PVERSION_CODE`，gradle 回落 `?: 1` | 覆盖安装失败，Play 拒绝同 code |
| **P1** | 字体走 Google Fonts CDN | `index.html:13` | Bangers 丢失 → 标题全塌成系统字体，波普味消失 |
| **P1** | 24MB 字体塞进 `public/fonts` | `HarmonyOS_Sans_SC_*.ttf` 各 8.1MB × 3 | APK 白胖 24MB，且全部 load |
| **P1** | 状态栏高度硬编码 24 | `PopNav.tsx:56` `setStatusBarHeight(24)` + `MainActivity.onResume` 强制 edge-to-edge | Android 15/16 上刘海/挖孔机型标题栏被裁切 |
| **P1** | 无硬件返回键处理 | 未装 `@capacitor/app` | 打开弹窗/二级页按返回 → 直接退出 App |
| **P1** | splash 主题缺 `postSplashScreenTheme` | `styles.xml` `Theme.SplashScreen` 仅设 `android:background` | 启动可能卡在 splash 主题或闪白；且 `capacitor.config.ts` 里的 `SplashScreen` 配置是**死配置**（没装 `@capacitor/splash-screen`） |
| **P1** | 启动图标是 Capacitor 占位图 | `mipmap-*/ic_launcher.png` 是 Capacitor 默认青色网格；真正的品牌图标躺在 `app-icons/Android/*.png` 没接进去 | 桌面显示成模板 App |
| **P1** | CI 没有 Supabase 环境变量 | `.env.local` 被 gitignore，workflow 无 env 注入 | 打出来的包 Supabase URL 为空 → 认证/同步直接不可用 |
| **P2** | 飞书接口 CORS | 全部走浏览器 `fetch`（`services/api/clients/FeishuClient.ts`），WebView origin 为 `https://localhost` | 第三方接口不带 CORS 头就被拦，需走原生 HTTP |
| **P2** | CSV 导出到 `Directory.Documents` | `PopAnalysis.tsx:494` | Android 11+ 分区存储下写公共 Documents 大概率失败 |
| **P2** | `splits.abi` 只有 APK | `build.gradle` | 上架 Play 需要 AAB；targetSdk 36 需确认 16KB page size |
| **P2** | workflow 仅 `workflow_dispatch` | `android-build.yml` | README 宣称"推送 main 触发"，实际不会 |

---

## 2. 路线选择

| | A. Capacitor 原生容器（推荐） | B. Kotlin + Compose 全原生 | C. 混合（A + 关键页原生） |
|---|---|---|---|
| 前端设计保留度 | **100%**（同一份代码同一套 CSS） | 0%，需逐像素重画 | 90%+，被原生化的页会漂移 |
| 工作量 | 3～4 人日（含流水线） | 25～40 人日 | 10～15 人日 |
| 业务逻辑复用 | 100% | 0%（`storageAdapter` 1906 行 / `apiService` 1241 行要重写） | 70% |
| 后续维护 | Web 与 App 同源，改一次全生效 | 双端双写，长期分裂 | 三份代码，最贵 |
| 性能 | WebView 级别（本 App 无重动画/长列表压力，够用） | 最佳 | 局部最佳 |
| 风险 | 中低（问题都在配置层，可枚举） | 高（视觉漂移不可控，等于重做产品） | 中高 |

**建议 A**，并在 A 的框架内用"原生插件"补齐体验（SQLite、文件、分享、触感、返回键、SystemBars），未来若某页确实需要原生性能（如超长历史列表），再单独用 C 的方式替换单页，而非整体重写。

---

## 3. 目标架构

```
┌─────────────────────────── Android App (com.popsmoke.tracker) ───────────────────────────┐
│  MainActivity (BridgeActivity)                                                            │
│    └ WebView  ──加载──►  https://localhost  (assets/public，全部离线内置)                  │
│         │                                                                                 │
│         │  同源 HTML/CSS/JS（视觉 = 现状，构建期编译 Tailwind，字体自托管）                │
│         │                                                                                 │
│    Capacitor Bridge ──┬─ @capacitor-community/sqlite   → 本地库（已用，保留）              │
│                       ├─ @capacitor/preferences        → 设置/令牌（已用）                 │
│                       ├─ @capacitor/filesystem         → 导出 CSV/日志（改造目录策略）     │
│                       ├─ @capacitor/share              → 分享文件（已用）                 │
│                       ├─ @capacitor/core SystemBars    → 边到边/安全区（替代硬编码 24px）  │
│                       ├─ @capacitor/app   【新增】      → 硬件返回键 / 前后台 / 深链       │
│                       ├─ @capacitor/haptics【新增】     → 记录时触感反馈                   │
│                       ├─ @capacitor/keyboard【新增】    → 键盘遮挡（输入框/时间选择器）    │
│                       └─ @capacitor/browser【新增】     → 重置密码等外部页                 │
└───────────────────────────────────────────────────────────────────────────────────────────┘
        │ 原生 HTTP（CapacitorHttp，绕过 CORS）              │ HTTPS
        ▼                                                    ▼
   飞书多维表格 API                                    Supabase (Auth + Postgres + Storage)
```

---

## 4. 「无损」工程：四件事

### 4.1 把 Tailwind 从 CDN 搬到构建期（P0）

**原则：锁定与 CDN 完全一致的版本与配置，而不是升级。**

`cdn.tailwindcss.com` 提供的是 **Tailwind v3.4.x 的 Play CDN（浏览器内 JIT）**。因此：

1. 安装 `tailwindcss@3.4.17`（固定版本，不用 v4）。当前 devDeps 里的 `tailwindcss@4` + `@tailwindcss/vite` 会导致两件事：v4 是 CSS-first 配置、且新增了 `perspective-*` / `rotate-y-*` / 任意 `underline-offset-6` 等能力 → **原本是空操作的类会突然生效，翻转卡与下划线偏移都会变样**。v4 迁移留给后续独立排期。
2. `tailwind.config.js` 逐项搬运 `index.html` 里的内联 config（fontFamily / colors.paper / boxShadow / animation / keyframes / plugins）。**只搬不加**。
3. `index.html` 里那两个 `<script>`（CDN + `tailwind.config`）删除，`<link>` Google Fonts 删除。
4. 新建 `styles/tailwind.css`，承载 `@tailwind base/components/utilities` **以及**现在 `<style>` 块里的全部内容：`@font-face`、body 网点背景、根字号媒体查询、`.scrollbar-*`、`.no-scroll`、`.responsive-container`、`.text-responsive-*`、`.icon-responsive`、`range` 样式。**逐字搬迁，不重写**。
5. `index.tsx` 顶部 `import './styles/tailwind.css'`。

**断点必须保持"未定义"状态**：`designSystem.ts` 写着 `xs:320 / sm:600 / md:840`，但 CDN 实际用的是 Tailwind 默认值（sm 640 / md 768 / lg 1024）。也就是说当前页面上 **`xs:` 前缀的类全部是空操作**，`sm:` 生效在 640px 而非 600px。若按 designSystem 去配置断点，**布局会在 600～640px 区间跳变、`xs:` 类集体激活 → 视觉直接变了**。

- 本期：`screen` 保持 Tailwind 默认，`xs` 不定义（与现状逐字节一致）。
- 清理这些死类、统一断点，单独立项，用截图 diff 逐条验收。

同理这些当前是"死类"，保持死着：`text-wrap balance`（v3 无 `text-wrap`）、`hover:underline-offset-6`（v3 默认只有 0/1/2/4/8）、`perspective-1000`、`rotate-y-180`（`transform-style-3d` / `backface-hidden` 由自定义 plugin 提供，保留）。

参考配置：

```js
// tailwind.config.js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html', './index.tsx', './App.tsx', './types.ts', './constants.ts',
    './components/**/*.{ts,tsx}', './styles/**/*.ts', './hooks/**/*.ts',
    './services/**/*.ts', './stores/**/*.ts', './utils/**/*.ts', './i18n/**/*.ts', './event/**/*.ts',
  ],
  theme: {
    extend: {
      fontFamily: {
        display: ['Bangers', '"Noto Sans SC"', '"Noto Sans JP"', '"Noto Sans KR"', 'cursive'],
        body: ['"HarmonyOS Sans"', '"HarmonyOS Sans SC"', '"Noto Sans SC"', '"Noto Sans JP"', '"Noto Sans KR"', 'sans-serif'],
      },
      colors: { paper: '#fdfbf7' },
      boxShadow: {
        pop: '4px 4px 0px 0px rgba(0,0,0,1)',
        'pop-hover': '2px 2px 0px 0px rgba(0,0,0,1)',
        'pop-lg': '8px 8px 0px 0px rgba(0,0,0,1)',
      },
      animation: {
        'slide-in': 'slideIn 0.3s ease-out forwards',
        'pop-blast': 'popBlast 0.6s ease-out forwards',
        'slide-down': 'slideDown 0.4s ease-out forwards',
        'slide-up': 'slideUp 0.3s ease-in forwards',
        shake: 'shake 0.5s ease-in-out',
      },
      keyframes: {
        slideIn: { '0%': { opacity: '0', transform: 'translateY(10px) scale(0.98)' }, '100%': { opacity: '1', transform: 'translateY(0) scale(1)' } },
        popBlast: {
          '0%': { opacity: '0', transform: 'translate(-50%,-50%) scale(0.5) rotate(-15deg)' },
          '50%': { opacity: '1', transform: 'translate(-50%,-50%) scale(1.2) rotate(15deg)' },
          '100%': { opacity: '0', transform: 'translate(-50%,-50%) scale(1.5) rotate(0deg)' },
        },
        slideDown: { '0%': { opacity: '0', transform: 'translateY(-100%)' }, '100%': { opacity: '1', transform: 'translateY(0)' } },
        slideUp: { '0%': { opacity: '1', transform: 'translateY(0)' }, '100%': { opacity: '0', transform: 'translateY(-100%)' } },
        shake: {
          '0%,100%': { transform: 'translateX(0)' },
          '10%,30%,50%,70%,90%': { transform: 'translateX(-5px)' },
          '20%,40%,60%,80%': { transform: 'translateX(5px)' },
        },
      },
    },
  },
  plugins: [
    function ({ addUtilities }) {
      addUtilities({ '.transform-style-3d': { 'transform-style': 'preserve-3d' }, '.backface-hidden': { 'backface-visibility': 'hidden' } });
    },
  ],
};
```

注意 `componentPatterns.ts` / `componentStyles.ts` 里是**字符串常量里的类名**，所以 `content` 必须包含 `styles/**/*.ts`，否则大批卡片/仪表样式会被 purge 掉（这是这类项目最常见的"重构后变丑"原因）。

### 4.2 字体自托管 + 瘦身（P1）

| 字体 | 现状 | 方案 |
|---|---|---|
| Bangers（标题/数字，波普灵魂） | Google CDN | 下载 woff2 放 `public/fonts/`，`@font-face` + `font-display: swap` 自托管 |
| Noto Sans SC/JP/KR 900 | Google CDN | **移除**。中文标题用已自带的 `HarmonyOS_Sans_SC_Black` 承接；日/韩回落系统 CJK（Android 自带 Noto CJK），视觉差异仅在日韩语标题字形 |
| HarmonyOS Sans R/B/Black（拉丁 body） | 本地 145KB × 3 | 保留 |
| HarmonyOS Sans SC R/B/Black | 本地 8.1MB × 3 = 24MB | 只保留 SC_Black（标题用），并用 `fonttools` 子集化到「i18n 全量字符串 + 数字 + 标点」字符集，预计 8MB → 1MB 内；body 走系统 CJK |

用户自由输入的备注文字走 `font-body`，天然由系统字体兜底，不存在子集化缺字问题。

体积预期：APK 从 ~40MB 降到 ~12MB 量级。

### 4.3 视觉基线冻结 + 截图回归门禁（这是"无损"的判定方法）

**顺序很重要：先固化基线，再动代码。**

1. **抓基线**：在改动前的 `main` 上跑 `pnpm dev`，用 Playwright 对 5 个页面 × 3 个视口（`390×844@3x` 手机、`768×1024` 平板、`1280×800` 桌面）× 4 种语言 × 2 种主题色，截图存 `tests/visual/baseline/`。
   - 注入固定数据（IndexedDB/SQLite 都 mock 成固定 20 条记录），`page.clock` 固定时间，关闭 `animation/transition`，等待 `fonts.ready`。
2. **改代码**（§4.1、§4.2）。
3. **对比**：`expect(page).toHaveScreenshot({ maxDiffPixelRatio: 0 })`。**基线必须来自 CDN 版本**，这样"无损"就从主观判断变成 CI 里的一条红线。
4. 这条 job 在日常 PR 也跑，之后任何人不小心删了 `styles/**` 的扫描范围，都会被拦下。

### 4.4 边到边与安全区（P1，Android 15/16 必做）

Capacitor 8 **已移除** `android.adjustMarginsForEdgeToEdge`，改为内置 **SystemBars** 核心插件；Android 15（SDK 35）强制边到边，Android 16（SDK 36）取消退出选项。当前项目是 `MainActivity.onResume()` 里手写 `setDecorFitsSystemWindows(false)` + `PopNav` 里 `statusBarHeight = 24` 硬编码，属于典型的"能跑但会错位"。

改造：

```ts
// capacitor.config.ts
const config: CapacitorConfig = {
  appId: 'com.popsmoke.tracker',
  appName: 'PopSmoke Tracker',
  webDir: 'dist',
  plugins: {
    SystemBars: {
      insetsHandling: 'css',            // 注入 --safe-area-inset-*，兼容旧 WebView
      initialViewportFitValueHint: 'cover',
    },
  },
};
```

- 删除 `MainActivity.onResume()` 里整段状态栏 hack，回归纯净 `BridgeActivity`。
- `PopNav` 删除 `statusBarHeight` 状态，改用 CSS：`padding-top: env(safe-area-inset-top, 0px)`；底部 tab 栏已有 `env(safe-area-inset-bottom)`，保留。
- `index.html` 的 viewport 已含 `viewport-fit=cover`，保持。
- 图标亮/暗用 `SystemBars.setStyle()` 按主题色亮度切换（`isLightColor` 已有，可直接复用）。

---

## 5. 原生层改造清单

### 5.1 依赖增补

```bash
pnpm add @capacitor/app @capacitor/haptics @capacitor/keyboard @capacitor/browser @capacitor/splash-screen
```

`@capacitor/status-bar` 与 `SystemBars` 功能重叠，可保留但统一改走 `SystemBars`，避免两套 insets 打架。

### 5.2 代码接线

| 能力 | 落点 | 行为 |
|---|---|---|
| 硬件返回键 | `App.tsx` 挂 `App.addListener('backButton')` | 优先级：关闭弹窗 → 非 Dashboard 页回 Dashboard → 连续两次返回才退出 |
| 触感 | `handleAddLog` 成功后 `Haptics.impact({ style: 'MEDIUM' })`；超限时 `notification({ type: 'WARNING' })` | 记录"按下去有反馈"，这是 WebView App 最容易被吐槽的点 |
| 键盘 | `Keyboard.setResizeMode({ mode: 'native' })` | 避免底部 tab 栏顶住输入框 |
| 启动图 | 装 `@capacitor/splash-screen`，配合主题 `postSplashScreenTheme` | 消除白闪，splash 用品牌色 |
| 外部链接 | 把 `PopExternalLinkWarning` 放行的链接改由 `Browser.open()` | 避免 WebView 内跳走导致回不来 |

### 5.3 资源修复（P0/P1 合并）

1. **新建 `android/app/src/main/res/values/colors.xml`**（当前缺失导致构建失败）：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="colorPrimary">#FFD700</color>
    <color name="colorPrimaryDark">#000000</color>
    <color name="colorAccent">#FF6B6B</color>
</resources>
```

2. `styles.xml` 补齐 splash 主题（当前只有 `android:background`，AndroidX SplashScreen 缺 `postSplashScreenTheme` 会异常）：

```xml
<style name="AppTheme.NoActionBarLaunch" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">#FFD700</item>
    <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher_foreground</item>
    <item name="postSplashScreenTheme">@style/AppTheme.NoActionBar</item>
</style>
```

3. **替换启动图标**：`app-icons/Android/{mdpi..xxxhdpi}.png` → `res/mipmap-*/ic_launcher.png`(48/72/96/144/192) 与 `ic_launcher_round.png`；再生成 adaptive icon 前景（108dp 安全区内留 66dp 主体）与背景色，加 Android 13+ 的 `<monochrome>` 单色层；`Play Store.png` 保留用于上架。
4. `AndroidManifest.xml`：
   - `android:enableOnBackInvokedCallback="true"`（Android 13+ 预测性返回）
   - `android:windowSoftInputMode="adjustResize"`（键盘不遮输入）
   - `android:supportsRtl="true"` 已有，`android:label` 改为 `PopSmoke Tracker`
   - `allowBackup` 建议 `false`（本地 SQLite 里有明文行为数据，避免云备份泄漏）
5. `strings.xml`：`app_name` / `title_activity_main` 改成 `PopSmoke Tracker`（现在是小写 `popsmoke tracker`，桌面图标下面会显得不专业）。

### 5.4 数据与网络

- **SQLite 已就绪**：`storageAdapter.ts` 的 `SQLiteAdapter` 是原生实现，`getStorageAdapter()` 已按 `Capacitor.isNativePlatform()` 分流，无需改动。上线前建议补一次「首次启动建表 + 老版本 Web 数据迁移（IndexedDB→SQLite）」的显式用例。当前是 Web 端用 IndexedDB、App 端用 SQLite，**两端数据不互通**——如果用户先在网页用再装 App，会看到空数据。方案：App 首启提供"从云端恢复"引导（Supabase 同步已具备）。
- **CORS**：把 `fetch` 换成 `CapacitorHttp`（`@capacitor/core` 内置，`CapacitorHttp.request`），由原生发起请求绕过 WebView 同源限制。最小侵入做法：在 `storageAdapter`/`apiService` 之上包一层 `httpClient.ts`，Web 走 `fetch`、原生走 `CapacitorHttp`，业务代码不动。
- **CSV 导出**：`Directory.Documents` 在 Android 11+ 分区存储下不可直接写。改为写 `Directory.Cache`，写入后 `Share.share({ url })` 让用户"保存到文件/发送给谁"，同时在 `file_paths.xml` 补 `<cache-path>`（已有 `my_cache_images`，path 为 `.`，可复用）。

---

## 6. 构建与签名

### 6.1 `app/build.gradle` 关键改动

```gradle
android {
    ...
    def versionCodeValue = (project.findProperty("VERSION_CODE") ?: "1").toInteger()
    def versionNameValue = project.findProperty("VERSION_NAME") ?: "1.0"
    defaultConfig {
        versionCode versionCodeValue
        versionName versionNameValue
    }
    signingConfigs {
        release {
            if (System.getenv("KEYSTORE_PATH")) {
                storeFile file(System.getenv("KEYSTORE_PATH"))
                storePassword System.getenv("KEYSTORE_PASSWORD")
                keyAlias System.getenv("KEY_ALIAS")
                keyPassword System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig System.getenv("KEYSTORE_PATH") ? signingConfigs.release : signingConfigs.debug
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    // 保留 ABI 拆分出 APK，同时新增 AAB 供上架
    bundle { language { enableSplit = false } }
}
```

要点：

- **缺失 keystore 时自动回落 debug 签名**，保证 fork 的仓库不配 secret 也能出可安装包；配了 secret 就出正式签名包。
- **R8 打开**（现在 `minifyEnabled false`）需在 `proguard-rules.pro` 保留 Capacitor 桥与插件的反射入口；一次跑通后可省 40% 体积。
- **16KB page size**：AGP ≥ 8.5.1 默认对未压缩 `.so` 做 16KB 对齐，本项目 AGP 8.13.0 满足；需在 CI 里验证 `@capacitor-community/sqlite` 带的 `.so` 是否已对齐（Play 自 2025-11-01 起强制，宽限至 2026-05-31）。
- 版本号来源统一到 `package.json.version`（`0.4.30-beta` → `versionName 0.4.30`，`versionCode` 用 CI 的递增序号或时间戳）。

---

## 7. GitHub Actions 流水线

### 7.1 作业图

```
push tag / workflow_dispatch
        │
        ├─ job: web-visual  ── pnpm build + Playwright 截图 ──► 与 baseline diff=0 才放行
        │
        └─ job: android (needs: web-visual)
              ├ setup node22 + pnpm (frozen-lockfile)
              ├ pnpm build            (注入 VITE_SUPABASE_*)
              ├ npx cap sync android  (同步 web 资产 + 插件工程)
              ├ setup-java 21 + gradle cache
              ├ 解码 keystore（无 secret 则跳过）
              ├ ./gradlew assembleDebug / assembleRelease bundleRelease
              ├ 产物校验：aapt2 badging + apksigner verify + 16KB 对齐检查
              ├ upload-artifact（唯一命名 build-<run_id>）
              └ job: release（needs: android）→ 创建/覆盖 GitHub Release + changelog
```

### 7.2 骨架

```yaml
name: Android Build

on:
  workflow_dispatch:
    inputs:
      channel: { type: choice, options: [beta, stable], default: beta }
      create_release: { type: boolean, default: true }
  push:
    tags: ['v*']

concurrency:
  group: android-${{ github.ref }}
  cancel-in-progress: true

env:
  VITE_SUPABASE_URL: ${{ secrets.VITE_SUPABASE_URL }}
  VITE_SUPABASE_ANON_KEY: ${{ secrets.VITE_SUPABASE_ANON_KEY }}

jobs:
  visual:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
      - uses: actions/setup-node@v4
        with: { node-version: 22, cache: pnpm }
      - run: pnpm install --frozen-lockfile
      - run: pnpm exec playwright install --with-deps chromium
      - run: pnpm build
      - run: pnpm test:visual          # 与 tests/visual/baseline 对比
      - uses: actions/upload-artifact@v4
        if: failure()
        with: { name: visual-diff-${{ github.run_id }}, path: tests/visual/diff/ }

  build:
    needs: visual
    runs-on: ubuntu-latest
    env:
      VERSION: ${{ github.event.inputs.channel == 'stable' && github.ref_name || 'beta' }}
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: pnpm/action-setup@v4
      - uses: actions/setup-node@v4
        with: { node-version: 22, cache: pnpm }
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21, cache: gradle }

      - run: pnpm install --frozen-lockfile
      - run: pnpm build
      - run: pnpm exec cap sync android

      - name: Decode keystore
        if: ${{ secrets.KEYSTORE_BASE64 != '' }}
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > android/release.keystore

      - name: Build
        run: |
          cd android && chmod +x gradlew
          VCODE=$(( $(date +%s) / 60 ))
          VNAME=$(node -p "require('../package.json').version.replace(/-.*$/,'')")
          ARGS="-PVERSION_CODE=$VCODE -PVERSION_NAME=$VNAME"
          if [ "${{ github.event.inputs.channel }}" = "stable" ]; then
            ./gradlew $ARGS assembleRelease bundleRelease
          else
            ./gradlew $ARGS assembleDebug
          fi
        env:
          KEYSTORE_PATH: ${{ secrets.KEYSTORE_BASE64 != '' && 'release.keystore' || '' }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

      - name: Verify
        run: |
          SDK=$ANDROID_HOME/build-tools/36.0.0
          APK=$(find android/app/build/outputs/apk -name "*.apk" | head -1)
          $SDK/aapt2 dump badging "$APK" | head -3
          $SDK/apksigner verify --print-certs "$APK"
          unzip -l "$APK" | grep -c "lib/" || true   # 确认 .so 与 16KB 对齐

      - uses: actions/upload-artifact@v4
        with: { name: build-${{ github.run_id }}, path: output/*, retention-days: 14 }
```

### 7.3 Secrets 清单

| Secret | 用途 | 必需 |
|---|---|---|
| `VITE_SUPABASE_URL` / `VITE_SUPABASE_ANON_KEY` | 构建期注入，否则云端功能为空 | 是 |
| `KEYSTORE_BASE64` | 发布签名（`base64 -w0 release.keystore`） | stable 必需 |
| `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` | 同上 | stable 必需 |
| `PAT_TOKEN` | 创建 Release / 删旧 tag | 是 |

### 7.4 现有 workflow 要修的 6 处

1. 从不传 `-PVERSION_CODE/-PVERSION_NAME` → 版本号恒 1。
2. `assembleRelease` 无签名 → 产物装不上（本次重点）。
3. 无 Gradle 缓存 → 每次全量下载 AGP/SDK，5～8 分钟浪费。
4. `pnpm install` 未加 `--frozen-lockfile` → CI 与本地依赖可能不一致。
5. 无产物校验（签名/包名/versionCode/16KB 对齐）。
6. 仅 `workflow_dispatch`，与 README 描述不符；建议加 `push: tags: v*`。

---

## 8. 分阶段计划

| 阶段 | 内容 | 涉及文件 | 验收（DoD） |
|---|---|---|---|
| **P0 构建可跑通**（0.5 天） | 补 `colors.xml`；补 splash 主题；图标替换；验证 `assembleDebug` 成功 | `res/values/*`, `mipmap-*` | CI 产出可安装 APK，桌面图标为品牌图 |
| **P0 样式落地**（1 天） | Tailwind 构建期化 + 字体自托管 + 截图基线 + 回归门禁 | `index.html`, `tailwind.config.js`, `styles/tailwind.css`, `index.tsx`, `package.json`, `tests/visual/*` | 断网飞行模式下打开 App，视觉与 Web 完全一致；diff = 0 |
| **P1 体验原生**（1 天） | SystemBars insets、返回键、触感、键盘、splash、分享导出 | `capacitor.config.ts`, `MainActivity.java`, `PopNav.tsx`, `App.tsx`, `file_paths.xml` | 刘海机上标题栏不裁切；弹窗按返回只关弹窗；输入框不被键盘遮 |
| **P1 数据与网络**（0.5 天） | `httpClient` 双通道（fetch/CapacitorHttp）、CSV 导出改 Cache+Share | `services/httpClient.ts`(新), `PopAnalysis.tsx`, `PopSystemLog.tsx` | 飞书同步在 App 内可用；导出可保存/分享 |
| **P2 发布工程**（0.5 天） | 签名配置、versionCode/Name 注入、R8、AAB、产物校验、Release 流程 | `app/build.gradle`, `proguard-rules.pro`, `android-build.yml` | stable 出已签名 APK + AAB；覆盖安装版本号递增正常 |
| **P3 可选增强** | v4 迁移与死类清理、Web→SQLite 首启迁移引导、通知提醒、桌面小组件 | — | 逐项独立验收 |

---

## 9. 验收清单（"无损"与"原生"两部分）

**设计无损**

- [ ] 5 页面 × 3 视口 × 4 语言 × 2 主题色 截图 diff = 0（对比 CDN 基线）
- [ ] 断网/飞行模式冷启动，字体、网点背景、硬阴影、翻转卡全部正常
- [ ] `font-display` 英文数字仍是 Bangers，中文标题仍是 900 粗黑
- [ ] 弹窗、通知、时间选择器、颜色选择器的动效曲线与时长与 Web 一致
- [ ] APK 内无任何远程样式/字体请求（可用 `adb logcat` 或 WebView 检查网络）

**原生合格**

- [ ] 刘海/挖孔/手势导航三种机型上，顶部标题与底部 tab 不被遮挡
- [ ] 硬件返回键行为符合预期，长列表滚动不与返回手势冲突
- [ ] 记录一次吸烟有触感反馈，超限有强提醒
- [ ] 杀进程重进，数据与设置不丢
- [ ] 安卓 12～16 各一档机型冒烟通过
- [ ] `apksigner verify` 通过；`versionCode` 单调递增；Play Console 无 16KB 警告

---

## 10. 风险登记

| 风险 | 影响 | 应对 |
|---|---|---|
| Tailwind 扫描范围漏掉 `.ts` 里的类名 | 卡片/仪表样式丢失 | 截图回归门禁必拦；`content` 显式列 `styles/**/*.ts` |
| 字体子集化漏字 | 某语言标题变豆腐块 | 子集化脚本以 i18n 全量字符串为输入，CI 校验「所有 i18n key 的字形均存在」 |
| `@capacitor-community/sqlite` 的 `.so` 未 16KB 对齐 | Play 上架被拒 | CI 增加对齐检查；必要时升级插件版本 |
| R8 混淆破坏 Capacitor 桥 | 运行时白屏/插件失效 | 先保持 `minifyEnabled false` 出包验证，再单独一轮开 R8 并回归 |
| Web 与 App 数据不互通 | 用户装 App 后看不到旧数据 | App 首启"从云端恢复"引导（Supabase 同步已有） |
| 国内网络下 Supabase 可达性 | 登录/同步慢 | 保持离线优先，同步失败可重试（`PopSyncQueue` 已有队列） |

---

## 11. 需要你确认的三件事

1. **路线确认**：按方案 A（Capacitor 原生容器，前端零改动）推进？若你确实要求「Kotlin + Compose 全原生重写」，视觉必然重画，请明确优先级是"原生"还是"无损"——两者不可兼得。
2. **Tailwind 版本策略**：先用 `v3.4.17` 锁定与 CDN 一致（保证无损），v4 迁移另排期。是否接受？
3. **发布渠道**：只发 GitHub Release 侧载 APK，还是也要上 Google Play（需要 AAB + 16KB 对齐 + 隐私政策）？国内应用商店是否需要单独出包？

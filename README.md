# POP SMOKE - 吸烟记录追踪应用

一个现代化的跨平台吸烟记录追踪应用，采用波普艺术风格设计，支持 Web 和 Android 平台。

## 功能特性

### 核心功能
- 🔐 **用户认证**：支持 Supabase 账户注册、登录、登出
- 📊 **数据存储**：Web 端使用 IndexedDB，Android 端使用 SQLite
- ☁️ **云端同步**：支持 Supabase 和飞书双平台数据同步
- 🌍 **多语言支持**：英语、中文、日语、韩语
- 🎨 **主题定制**：多种预设主题颜色 + 自定义颜色
- 📱 **跨平台**：Web + Android 原生应用

### 数据管理
- 📈 **数据可视化**：吸烟数据统计和分析图表
- 📋 **历史记录**：查看、筛选、管理历史吸烟记录
- 📤 **飞书集成**：支持与飞书多维表格数据同步
- 🔄 **操作日志**：记录所有数据操作，便于追踪

### 设计特色
- 🎭 **波普艺术风格**：独特的视觉设计语言
- ✨ **交互动效**：按钮、卡片、弹窗动画效果
- 📐 **响应式布局**：适配桌面和移动设备

## 技术栈

| 类别 | 技术 |
|------|------|
| 前端框架 | React 19 + TypeScript |
| 构建工具 | Vite 6 |
| 样式方案 | Tailwind CSS 4 |
| 跨平台 | Capacitor 8 |
| 数据可视化 | Recharts 3 |
| 后端服务 | Supabase (认证 + 数据库) |
| 数据存储 | IndexedDB (Web) / SQLite (Android) |

## 项目结构

```
popsmoking/
├── components/              # React 组件
│   ├── pages/              # 页面组件
│   │   ├── PopDashboard.tsx    # 主仪表盘
│   │   ├── PopAnalysis.tsx     # 数据分析
│   │   ├── PopHistory.tsx      # 历史记录
│   │   ├── PopAPI.tsx          # API 管理
│   │   └── PopSettings.tsx     # 设置页面
│   └── ui/                 # UI 组件库
│       ├── PopButton.tsx       # 按钮
│       ├── PopCard.tsx         # 卡片容器
│       ├── PopColorPicker.tsx  # 颜色选择器
│       ├── PopDatePicker.tsx   # 日期选择器
│       ├── PopDropdown.tsx     # 下拉菜单
│       ├── PopInput.tsx        # 输入框
│       ├── PopNotification.tsx # 通知组件
│       ├── PopOperationLog.tsx # 操作日志
│       ├── PopCloudDataDialog.tsx # 云端数据对话框
│       ├── PopSupabaseGuide.tsx   # Supabase 配置引导
│       └── PopExternalLinkWarning.tsx # 外部链接警示
├── services/               # 服务层
│   ├── authService.ts          # 认证服务
│   ├── authStorageAdapter.ts   # 认证存储适配器
│   ├── avatarCacheService.ts   # 头像缓存服务
│   ├── feishuService.ts        # 飞书 API 服务
│   ├── storageAdapter.ts       # 存储适配器
│   ├── supabase.ts             # Supabase 客户端
│   ├── supabaseStorageService.ts # Supabase 存储服务
│   ├── syncStateManager.ts     # 同步状态管理
│   └── systemLogService.ts     # 系统日志服务
├── styles/                 # 样式系统
│   ├── designSystem.ts         # 设计系统定义
│   ├── componentStyles.ts      # 组件样式
│   └── themePresets.ts         # 主题预设
├── i18n/                   # 国际化
│   ├── i18n_ZH.ts              # 中文
│   ├── i18n_EN.ts              # 英文
│   ├── i18n_JP.ts              # 日文
│   └── i18n_KO.ts              # 韩文
├── types/                  # TypeScript 类型定义
│   ├── auth.ts                 # 认证类型
│   ├── storage.ts              # 存储类型
│   └── logs.ts                 # 日志类型
├── utils/                  # 工具函数
│   ├── logUtils.ts             # 日志工具
│   ├── storageUtils.ts         # 存储工具
│   └── errors/                 # 错误处理
├── constants/              # 常量定义
├── android/                # Android 原生项目
├── public/                 # 静态资源
│   └── fonts/                  # HarmonyOS Sans 字体
└── .github/                # GitHub 配置
    └── workflows/              # CI/CD 工作流
```

## 快速开始

### 前提条件

- Node.js >= 22.0.0
- pnpm >= 10.0.0
- Supabase 项目账号

### 本地运行

1. **克隆项目**：
   ```bash
   git clone https://github.com/your-username/popsmoking.git
   cd popsmoking
   ```

2. **安装依赖**：
   ```bash
   pnpm install
   ```

3. **配置环境变量**：
   创建 `.env.local` 文件：
   ```env
   VITE_SUPABASE_URL=your-supabase-url
   VITE_SUPABASE_ANON_KEY=your-supabase-anon-key
   ```

4. **运行开发服务器**：
   ```bash
   pnpm dev
   ```

### 构建生产版本

```bash
# 构建 Web 应用
pnpm build

# 预览构建结果
pnpm preview
```

### Android 构建

```bash
# 同步 Capacitor
pnpm exec cap sync android

# 构建 APK（需要 Android Studio 或命令行工具）
cd android
./gradlew assembleDebug
```

## Supabase 数据库配置

### smoke_logs 表

| 字段名 | 数据类型 | 约束 | 描述 |
|--------|----------|------|------|
| `id` | `text` | `primary key` | 记录 ID |
| `user_id` | `text` | `references auth.users(id)` | 用户 ID |
| `timestamp` | `bigint` | `not null` | 记录时间戳 |
| `record_date` | `text` | `not null` | 记录日期 |
| `record_time` | `text` | | 记录时间 |
| `record_index` | `integer` | | 当日序号 |
| `created_at` | `timestamp` | `default now()` | 创建时间 |

### user_settings 表

| 字段名 | 数据类型 | 约束 | 描述 |
|--------|----------|------|------|
| `id` | `text` | `primary key` | 设置 ID |
| `user_id` | `text` | `unique` | 用户 ID |
| `dailyLimit` | `integer` | `not null` | 每日上限 |
| `warningLimit` | `integer` | `not null` | 警告阈值 |
| `themeColor` | `text` | `not null` | 主题颜色 |
| `language` | `text` | `not null` | 语言设置 |

### avatars 存储桶

用于存储用户头像图片，需要设置为公开访问。

## 数据同步

### Supabase 同步
1. 在设置页面配置 Supabase API
2. 登录账户后自动同步数据

### 飞书同步
1. 在 API 管理页面配置飞书 API URL
2. 选择同步方向（上传/下载）
3. 设置日期范围和目标表格
4. 执行同步操作

## 多语言支持

| 语言 | 代码 | 状态 |
|------|------|------|
| 中文 | `zh` | ✅ 完整支持 |
| English | `en` | ✅ 完整支持 |
| 日本語 | `ja` | ✅ 完整支持 |
| 한국어 | `ko` | ✅ 完整支持 |

## 主题预设

| 颜色 | 名称 | 色值 |
|------|------|------|
| 🟡 黄色 | Pop Yellow | `#FFD700` |
| 🔴 红色 | Pop Red | `#FF6B6B` |
| 🔵 青色 | Pop Cyan | `#4ECDC4` |
| 🟢 浅绿 | Pop Green | `#C7F464` |
| 🟣 紫色 | Pop Purple | `#A78BFA` |
| 🟠 橙色 | Pop Orange | `#FF9F43` |

## CI/CD

项目使用 GitHub Actions 自动构建 Android APK：

- 触发条件：推送到 `main` 分支
- 构建产物：`PopSmoke-APK`

## 部署

### Web 部署

支持部署到以下平台：
- Vercel
- Netlify
- GitHub Pages
- Cloudflare Pages

### Android 部署

1. 构建 APK：
   ```bash
   cd android
   ./gradlew assembleRelease
   ```

2. 签名发布（需要配置签名密钥）

## 开发规范

- **命名规范**：组件使用 `Pop` 前缀
- **类型安全**：TypeScript 严格模式
- **样式系统**：统一的设计系统和组件样式
- **国际化**：所有文本支持多语言

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

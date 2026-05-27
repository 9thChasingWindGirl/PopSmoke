# 🚀 PopSmoke HarmonyOS 鸿蒙化路线图

## 分支说明

- **main**: 保留现有 Web + Android 代码，稳定版本
- **harmony**: HarmonyOS 鸿蒙化开发分支

---

## 📋 开发计划

### 阶段一：工程搭建 (Week 1-2)

#### 1.1 创建鸿蒙工程结构
```
harmonyos/
├── entry/                          # 主模块
│   ├── src/main/ets/
│   │   ├── entryability/           # 入口 Ability
│   │   ├── pages/                  # 页面
│   │   │   ├── Index.ets           # 首页/仪表盘
│   │   │   ├── Analysis.ets        # 数据分析
│   │   │   ├── History.ets         # 历史记录
│   │   │   ├── Settings.ets        # 设置
│   │   │   └── AdvancedSettings.ets # 高级设置(隐藏)
│   │   ├── services/               # 服务层
│   │   │   ├── auth/               # 认证服务
│   │   │   │   ├── HarmonyOSAuthService.ets
│   │   │   │   ├── HuaweiCloudAuthService.ets
│   │   │   │   └── LegacyAuthService.ets
│   │   │   ├── storage/            # 存储服务
│   │   │   │   ├── LocalStorageService.ets
│   │   │   │   ├── HuaweiCloudDBService.ets
│   │   │   │   └── LegacyAPIService.ets
│   │   │   └── sync/               # 同步服务
│   │   │       └── SyncManager.ets
│   │   ├── components/             # 组件
│   │   │   ├── ui/                 # UI 组件
│   │   │   └── charts/             # 图表组件
│   │   ├── models/                 # 数据模型
│   │   │   ├── SmokeLog.ets
│   │   │   ├── AppSettings.ets
│   │   │   └── User.ets
│   │   └── utils/                  # 工具函数
│   └── resources/                  # 资源文件
├── features/                       # 特性模块
└── build-profile.json5             # 构建配置
```

#### 1.2 配置文件
- `build-profile.json5`: 构建配置
- `module.json5`: 模块配置
- `hvigorfile.ts`: 构建脚本

### 阶段二：核心功能迁移 (Week 3-5)

#### 2.1 数据模型转换
将 TypeScript 类型转换为 ArkTS:

```typescript
// 原 TypeScript (types.ts)
interface SmokeLog {
  id: string;
  user_id: string;
  record_date: string;
  record_time: string;
  timestamp: number;
}

// 新 ArkTS (models/SmokeLog.ets)
@Observed
class SmokeLog {
  id: string = '';
  userId: string = '';
  recordDate: string = '';
  recordTime: string = '';
  timestamp: number = 0;
  
  constructor(init?: Partial<SmokeLog>) {
    if (init) {
      Object.assign(this, init);
    }
  }
}
```

#### 2.2 存储层实现

**本地存储 (默认)**
```typescript
// services/storage/LocalStorageService.ets
import { preferences } from '@kit.ArkData';

export class LocalStorageService {
  private context: Context;
  private pref: preferences.Preferences;
  
  async initialize(context: Context): Promise<void> {
    this.context = context;
    this.pref = preferences.getPreferencesSync(context, {
      name: 'popsmoke_storage'
    });
  }
  
  async saveLogs(logs: SmokeLog[]): Promise<void> {
    this.pref.putSync('logs', JSON.stringify(logs));
    await this.pref.flush();
  }
  
  async getLogs(): Promise<SmokeLog[]> {
    const data = this.pref.getSync('logs', '[]') as string;
    return JSON.parse(data).map((item: object) => new SmokeLog(item));
  }
}
```

**华为云数据库 (可选)**
```typescript
// services/storage/HuaweiCloudDBService.ets
import { cloudData } from '@kit.CloudFoundationKit';

export class HuaweiCloudDBService {
  private cloudDB: cloudData.CloudDBZone;
  
  async initialize(): Promise<void> {
    // 配置在 agconnect-services.json 中，用户无感知
    this.cloudDB = await cloudData.openCloudDBZone({
      zoneName: 'popsmoke_zone'
    });
  }
  
  async saveLogs(logs: SmokeLog[]): Promise<void> {
    await this.cloudDB.executeUpsert('smoke_logs', logs);
  }
  
  async getLogs(userId: string): Promise<SmokeLog[]> {
    const query = this.cloudDB.query('smoke_logs')
      .equalTo('user_id', userId)
      .orderByDesc('timestamp');
    return await query.execute();
  }
}
```

#### 2.3 认证服务

**鸿蒙账号登录（推荐）**
```typescript
// services/auth/HarmonyOSAuthService.ets
import { authentication } from '@kit.AccountKit';

export class HarmonyOSAuthService {
  async signInWithHuaweiId(): Promise<AuthResult> {
    const auth = new authentication.HuaweiIDAuthentication();
    const result = await auth.login({
      scope: ['openid', 'profile']
    });
    
    return {
      userId: result.openId,
      displayName: result.displayName,
      avatarUrl: result.avatarUrl,
      isAuthenticated: true
    };
  }
  
  async signInAnonymous(): Promise<AuthResult> {
    // 匿名登录，无需用户操作
    const token = await authentication.getAnonymousToken();
    return {
      userId: `anon_${token.substring(0, 8)}`,
      isAuthenticated: false
    };
  }
}
```

### 阶段三：UI 重构 (Week 6-8)

#### 3.1 页面结构

**首页/仪表盘 (pages/Index.ets)**
```typescript
@Entry
@Component
struct Index {
  @State logs: SmokeLog[] = [];
  @State todayCount: number = 0;
  @State settings: AppSettings = new AppSettings();
  
  build() {
    Column() {
      // 波普艺术风格标题
      PopHeader({ title: 'POPSMOKE', themeColor: this.settings.themeColor });
      
      // 今日统计卡片
      TodayStatsCard({
        count: this.todayCount,
        limit: this.settings.dailyLimit,
        themeColor: this.settings.themeColor
      });
      
      // 记录按钮
      PopRecordButton({
        onClick: () => this.handleRecord(),
        themeColor: this.settings.themeColor
      });
      
      // 底部导航
      PopNavBar({
        currentIndex: 0,
        onTabChange: (index) => this.switchTab(index)
      });
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#f5f5f5');
  }
  
  private handleRecord() {
    // 记录吸烟数据
    const log = new SmokeLog({
      id: generateUUID(),
      timestamp: Date.now(),
      recordDate: formatDate(new Date()),
      recordTime: formatTime(new Date())
    });
    
    this.logs.unshift(log);
    this.todayCount++;
    
    // 播放 POW 动画
    this.showPopEffect();
    
    // 保存数据
    this.saveLogs();
  }
}
```

#### 3.2 图表组件

使用鸿蒙 Canvas 自研图表替代 Recharts:

```typescript
// components/charts/BarChart.ets
@Component
export struct BarChart {
  @Prop data: ChartData[];
  @Prop themeColor: string = '#FFD700';
  
  private context: CanvasRenderingContext2D;
  
  build() {
    Canvas(this.context)
      .width('100%')
      .height(200)
      .onReady(() => {
        this.drawChart();
      });
  }
  
  private drawChart() {
    const ctx = this.context;
    const width = 300;
    const height = 200;
    
    // 清空画布
    ctx.clearRect(0, 0, width, height);
    
    // 绘制柱状图
    const barWidth = width / this.data.length * 0.6;
    const maxValue = Math.max(...this.data.map(d => d.value));
    
    this.data.forEach((item, index) => {
      const x = (index + 0.5) * (width / this.data.length) - barWidth / 2;
      const barHeight = (item.value / maxValue) * (height - 40);
      const y = height - barHeight - 20;
      
      // 绘制柱子
      ctx.fillStyle = this.themeColor;
      ctx.fillRect(x, y, barWidth, barHeight);
      
      // 绘制标签
      ctx.fillStyle = '#333';
      ctx.font = '12px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(item.label, x + barWidth / 2, height - 5);
    });
  }
}
```

### 阶段四：高级功能 (Week 9-10)

#### 4.1 隐藏的高级设置

```typescript
// pages/AdvancedSettings.ets
@Entry
@Component
struct AdvancedSettings {
  @State showSupabaseConfig: boolean = false;
  @State showFeishuConfig: boolean = false;
  
  build() {
    Column() {
      Text('高级同步设置')
        .fontSize(20)
        .fontWeight(FontWeight.Bold)
        .margin(16);
      
      Text('警告：这些设置仅供高级用户使用')
        .fontSize(12)
        .fontColor('#ff4444')
        .margin({ bottom: 16 });
      
      // Supabase 配置（折叠）
      CollapsePanel({
        title: 'Supabase 配置',
        isOpen: this.showSupabaseConfig,
        onToggle: () => this.showSupabaseConfig = !this.showSupabaseConfig
      }) {
        SupabaseConfigForm();
      };
      
      // 飞书配置（折叠）
      CollapsePanel({
        title: '飞书多维表格配置',
        isOpen: this.showFeishuConfig,
        onToggle: () => this.showFeishuConfig = !this.showFeishuConfig
      }) {
        FeishuConfigForm();
      };
      
      // 数据迁移工具
      DataMigrationTool();
    }
    .padding(16);
  }
}
```

#### 4.2 数据迁移

```typescript
// services/migration/DataMigrationService.ets
export class DataMigrationService {
  async migrateFromSupabase(supabaseConfig: SupabaseConfig): Promise<MigrationResult> {
    // 1. 从 Supabase 导出数据
    const supabaseData = await this.exportFromSupabase(supabaseConfig);
    
    // 2. 转换数据格式
    const migratedData = this.convertDataFormat(supabaseData);
    
    // 3. 导入到华为云
    await this.importToHuaweiCloud(migratedData);
    
    return {
      success: true,
      migratedCount: migratedData.length,
      message: `成功迁移 ${migratedData.length} 条记录`
    };
  }
  
  async migrateFromFeishu(feishuConfig: FeishuConfig): Promise<MigrationResult> {
    // 类似流程...
  }
}
```

---

## 🔧 开发环境配置

### 1. 安装 DevEco Studio
```bash
# 下载地址: https://developer.harmonyos.com/cn/develop/deveco-studio
# 安装后配置 SDK
```

### 2. 项目初始化
```bash
# 在 harmony 分支下
cd /workspace

# 创建鸿蒙工程目录
mkdir -p harmonyos

# 使用 DevEco Studio 创建项目
# 或命令行创建
hvigor create --project harmonyos --template emptyAbility
```

### 3. 依赖配置
在 `harmonyos/entry/oh-package.json5` 中添加:
```json
{
  "dependencies": {
    "@kit.AccountKit": "^1.0.0",
    "@kit.ArkData": "^1.0.0",
    "@kit.CloudFoundationKit": "^1.0.0",
    "@kit.ConnectivityKit": "^1.0.0"
  }
}
```

---

## 📱 功能对照表

| 功能 | Web/Android (main) | HarmonyOS (harmony) |
|------|-------------------|---------------------|
| 用户认证 | Supabase Auth | 鸿蒙账号 / 匿名 |
| 数据存储 | IndexedDB / SQLite | preferences / 华为云DB |
| 云端同步 | Supabase / 飞书 | 华为云 (自动) |
| 数据图表 | Recharts | Canvas 自研 |
| 本地通知 | 未实现 | 鸿蒙通知服务 |
| 多语言 | i18n 文件 | 鸿蒙资源文件 |
| 主题系统 | CSS 变量 | ArkUI 样式 |

---

## ✅ 检查清单

### 开发前准备
- [ ] 安装 DevEco Studio
- [ ] 配置鸿蒙 SDK
- [ ] 注册华为开发者账号
- [ ] 创建华为云项目
- [ ] 配置 AppGallery Connect

### 开发阶段
- [ ] 工程结构搭建
- [ ] 数据模型转换
- [ ] 存储层实现
- [ ] 认证服务接入
- [ ] UI 页面重构
- [ ] 图表组件开发
- [ ] 多语言适配
- [ ] 主题系统实现

### 测试阶段
- [ ] 单元测试
- [ ] 真机测试
- [ ] 性能测试
- [ ] 兼容性测试

### 发布阶段
- [ ] 华为应用市场审核
- [ ] 文档更新
- [ ] 用户引导

---

## 📚 参考文档

- [HarmonyOS 开发文档](https://developer.harmonyos.com/)
- [ArkTS 语言指南](https://developer.harmonyos.com/cn/docs/documentation/doc-guides-V3/arkts-get-started-0000001430240458-V3)
- [ArkUI 框架](https://developer.harmonyos.com/cn/docs/documentation/doc-guides-V3/arkui-overview-0000001430600265-V3)
- [华为云数据库](https://developer.huawei.com/consumer/cn/doc/development/AppGallery-connect-Guides/agc-clouddb-introduction)

---

## 💬 讨论

如有问题，请在 Discussion 中交流。

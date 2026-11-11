# PopSmoke 原生重写方案（Android 先行 · 鸿蒙预留）

> 本文档取代 v1 的「Web 套壳」方案（见 `ANDROID-NATIVE-APP-PLAN.md`，仅保留其中 Web 侧 CDN 问题清单作参考）。
> 前提：不做 WebView 容器，用原生 UI 框架重新实现界面；`styles/` 全局样式系统是设计真源。

---

## 一、先对齐一件关键的事：`styles/` 到底是什么

你的判断是对的——主要样式确实集中在这一个目录里，它是这个项目的**设计中枢**：

| 文件 | 内容 | 在原生重写里的角色 |
|---|---|---|
| `designSystem.ts` | 颜色、描边宽度、阴影、圆角、字号台阶、间距、动效时长与曲线、层级、断点 | **设计令牌（tokens）**，直接可搬 |
| `componentStyles.ts` | 按钮/卡片/输入/弹窗/通知/仪表盘/翻转卡/导航云的"组件配方" | **组件规格书**，逐条对应原生组件 |
| `componentPatterns.ts` | 容器、栅格、按钮组、表单、文字层级的版式组合 | **布局规则**，对应原生布局 |
| `themePresets.ts` | 6 套主题色 + 多语言名称 | **主题系统**，原生侧做成可切换 Theme |

但有一个技术细节必须说清，否则整个迁移会走偏：

> **这些文件里存的是 Tailwind 的"类名字符串"**（例如 `"bg-paper border-4 border-black shadow-pop-lg p-6 relative"`）。浏览器认识它们，Kotlin 不认识。

所以原生实现不是"把这堆字符串搬到 Android"，而是：

**把这些字符串里隐含的设计决策，抽成一份跨平台数据 → 再由 Android / 鸿蒙各自生成原生样式代码。**

这一步做对了，后面两端都是顺着走；做错了，就会变成"每个页面各写各的"，风格必然散掉。

### 顺带发现的三处「文档与实现不一致」，需要你拍板

| 项 | `styles/` 里写的 | 页面实际渲染的 | 影响 |
|---|---|---|---|
| 断点 | `xs:320 / sm:600 / md:840` | Tailwind 默认 `sm:640 / md:768`；`xs:` 前缀**全部未生效** | 原生侧按哪个定适配策略 |
| 圆角 | `borders.radius: { none: 0, default: 0 }` | 卡片实际用了 `rounded-2xl`（16px） | 卡片到底是直角还是 16px 圆角 |
| 字号台阶 | `responsiveFont.h1.xs = 1.5rem`(24px) | 仪表盘数值实际用 40~64px 的 inline style | 原生字阶是否要新增超大号 |

我的建议：**原生以「设计意图 + 你的审美判断」为准，而不是以当前实现的意外为准**。既然不要求像素级复制，这正是顺手把这几处理顺的机会。

---

## 二、目标形态

| 平台 | 技术栈 | 说明 |
|---|---|---|
| Android | **Kotlin + Jetpack Compose**（纯 Compose，不用 XML 布局） | 声明式 UI，与 React 心智最接近，动效/手势能力强 |
| HarmonyOS | **ArkTS + ArkUI**（声明式） | 与 Compose 写法高度相似（`@Component` / `build()` / 状态驱动），令牌与规范可复用 |
| 共用 | `design/tokens.json` + `design/interactions.md` | 唯一真源，两端由脚本生成原生常量 |

**一个重要的取舍：不使用 Material Design 组件库。**

Material 3 的视觉语言（柔和阴影、大圆角、色板体系、涟漪反馈）与波普风（0 圆角、4px 纯黑描边、无模糊硬投影、高饱和大色块、按下位移）是**结构性冲突**的。套 M3 会陷入"到处覆盖默认样式"的泥潭，最后既不 Material 也不波普。

正确做法：**只使用 Compose 的底座能力**（`Layout` / `Modifier` / `Animation` / `Canvas` / `Gesture`），自建 `PopTheme` 与全套 `Pop*` 组件。这与你现有 `components/ui/Pop*` 的命名体系正好一一对应。

---

## 三、核心章节：把「风格」翻译成原生规则

这是重写的手艺所在。波普风之所以"有味道"，靠的是几条很硬的几何规则，它们必须逐条在原生里重建，否则会变成"配色像、气质不像"。

| 设计语言 | Web 里的实现 | 原生怎么做 | 坑 |
|---|---|---|---|
| **4px 纯黑硬描边** | `border-4 border-black` | `Modifier.border(4.dp, Black)` | Compose 的 border **不占布局尺寸**，而 CSS `border-box` 占。要用内边距补偿，否则内容会挤 |
| **无模糊硬投影** | `box-shadow: 4px 4px 0 0 #000` | **不能用** `Modifier.shadow()`（那是模糊 elevation）。用「偏移的纯黑矩形层」垫在下面，或 `drawBehind` 直接画 | 这是最容易做错的一条。用了 elevation 就立刻失去波普味 |
| **按下位移** | `active:translate-x-[2px]` + 阴影同步缩到 2px | `collectIsPressedAsState()` 驱动 `animateDpAsState`，同时把阴影偏移从 4 动画到 2 | 阴影必须**跟着缩**，才有"被按下去"的错觉；只动元素不动阴影会飘 |
| **旋转标签** | `transform: rotate(-2deg)` | `Modifier.rotate(-2f)` | 旋转不改变布局占位，父容器要留溢出空间，否则边缘被裁 |
| **3D 翻转卡**（仪表盘↔今日记录） | `rotateY(180deg)` + `backface-visibility: hidden` | `graphicsLayer { rotationY = angle; cameraDistance = 12 * density }`，两面按角度条件绘制 | `cameraDistance` 用默认值会透视过度变形，必须给 12×density |
| **半环仪表** | SVG `stroke-dasharray` + 1s 过渡 | `Canvas` + `drawArc(startAngle = -180f, sweepAngle = 180f * pct)` + `StrokeCap.Round` + `animateFloatAsState(1000ms)` | 端点圆角、描边宽度、底色灰 `#E5E7EB` 三个值要和 token 对齐 |
| **网点背景** | `radial-gradient(#000 1px, transparent 1px)` / 20px | `drawBehind` 循环 `drawCircle`，或做一张 20×20 的可平铺贴图 | 平铺贴图更省电，且不会在滚动时抖动 |
| **高饱和主题色** | 运行时 `settings.themeColor` 注入 inline style | `PopTheme` 里用 `CompositionLocal` 提供 `accent`，颜色变化带动全树重组 | 文字黑白由 `isLightColor()` 决定，这套逻辑要原样搬（HSL 亮度阈值） |
| **动效曲线** | CSS `keyframes` + `cubic-bezier` | `tween(300, easing = CubicBezierEasing(0.4f,0f,0.2f,1f))`；弹入用 `spring(dampingRatio = 0.5f)` | 时长/曲线必须来自 token，不能各页手写 |
| **字体** | Bangers + HarmonyOS Sans | `res/font/` + `FontFamily(Bangers, HarmonySans)`；Bangers 需单独调行高与字距 | Bangers 是 OFL 协议可商用，但字形偏高，`lineHeight` 要压 |

**最关键的三个组件（决定"像不像"）：**

1. `PopCard` —— 硬描边 + 硬阴影 + 纸色底 + 可选左上角黑色标题贴片（旋转 -1°）
2. `PopButton` —— 硬描边 + 硬阴影 + 按下位移 + 大写字母 + 字距加宽
3. `SemiCircleGauge` —— 半环 + 中心超大数字（这个数字是整个 App 的视觉锚点）

这三个做完，整个 App 的"气质"就定了。其余页面是它们的不同组合。

---

## 四、设计令牌：唯一真源

新建 `design/tokens.json`，作为 Android / 鸿蒙 /（未来）Web 三方共用的数据源：

```jsonc
{
  "brand": {
    "ink": "#000000",
    "paper": "#FDFBF7",
    "canvas": "#F0F0F0",
    "white": "#FFFFFF"
  },
  "themes": {
    "gold":   { "accent": "#FFD700", "onAccent": "#000000" },
    "coral":  { "accent": "#FF6B6B", "onAccent": "#000000" },
    "teal":   { "accent": "#4ECDC4", "onAccent": "#000000" },
    "lime":   { "accent": "#C7F464", "onAccent": "#000000" },
    "purple": { "accent": "#A78BFA", "onAccent": "#000000" },
    "orange": { "accent": "#FF9F43", "onAccent": "#000000" }
  },
  "border": { "hairline": 2, "regular": 4, "heavy": 6, "color": "#000000", "radius": 0, "radiusCard": 16 },
  "shadow": { "xs": 1, "sm": 2, "md": 4, "lg": 8, "blur": 0, "color": "#000000" },
  "type": {
    "display": ["Bangers", "HarmonySans", "SystemCJK"],
    "body":    ["HarmonySans", "SystemCJK"],
    "scale": {
      "h1":   [24, 30, 36, 48],
      "h2":   [20, 24, 30, 36],
      "h3":   [16, 18, 20, 24],
      "body": [14, 14, 16, 16],
      "small":[12, 12, 14, 14],
      "gauge":[40, 56, 64]
    },
    "tracking": { "display": 0.02, "label": 0.10 }
  },
  "spacing": [4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80, 96],
  "motion": {
    "fast": 150, "normal": 300, "slow": 500, "gauge": 1000, "blast": 600,
    "easing": {
      "standard": [0.4, 0.0, 0.2, 1.0],
      "bounce":   [0.68, -0.55, 0.265, 1.55]
    }
  },
  "haptics": { "tap": "light", "record": "medium", "warning": "heavy" }
}
```

配套一个生成脚本（`pnpm gen:tokens`）：

```
design/tokens.json
      ├─► android/.../ui/theme/PopTokens.kt      (Compose: Color/Dp/TextStyle/AnimationSpec)
      └─► harmony/.../common/PopTokens.ets       (ArkTS: ResourceColor/number/curves)
```

**为什么值得多花这一步**：你提到已经做了多款可替换主题——在原生侧，"主题"就等于一组 token。以后要加第 7、8 套主题，只改 JSON，两端自动同步，不用动任何页面代码。这是把"能换主题"变成"结构性能力"的关键。

> 如果你愿意，把你那几套主题的样式（CSS 或截图）给我，我可以把它们一并抽象成 token preset，连 "不同主题换一套描边/阴影配方" 这种结构性变化也能覆盖。

---

## 五、功能模块的原生映射（工作量的真话）

| 现有 Web 实现 | 原生替代 | 难度 |
|---|---|---|
| `App.tsx` 里 `switch (view)` 切页 | Compose 单 Activity + `AnimatedContent` / Navigation | 低 |
| `zustand`（authStore / appStore / dataStore） | `ViewModel` + `StateFlow`，或一个 `AppState` 单例 | 低 |
| `storageAdapter`（1906 行，Web 用 IndexedDB / App 用 SQLite） | **Room**（Room 就是 SQLite 封装，天生原生） | 中（但要重写仓储层，逻辑可照抄） |
| `@capacitor/preferences` | **DataStore** | 低 |
| `@supabase/supabase-js` | **supabase-kt**（官方 KMP SDK，Auth + Postgrest + Storage 齐全） | 中 |
| 飞书同步（`FeishuClient`，走 fetch） | **Ktor Client**（原生无 CORS 概念，反而更简单） | 低 |
| Recharts 图表（`PopAnalysis`） | **自绘 Canvas**（推荐）或 Vico | **高**：最费工，但自绘能把图表也做成波普风 |
| `PopDatePicker` / `PopTimePicker`（滚轮） | Compose `LazyColumn` + `snapFlingBehavior` 做滚轮 | **高**：惯性吸附的手感要调 |
| 4 语言 / 394 个文案键（`i18n/*.ts`） | Android `strings.xml`（或 Compose Multiplatform Resources） | 低（可脚本生成） |
| CSV 导出（Filesystem + Share） | **SAF**（`ACTION_CREATE_DOCUMENT`）+ `FileProvider` | 低，且比原来更规范 |
| 操作日志 / 系统日志（`PopSystemLog`） | 保留数据结构，UI 原生化 | 低 |
| 版本检查 / 更新提示 | Play In-App Update API | 低 |
| 触感、返回键、安全区 | 系统原生能力，不再需要插件桥 | 低（且体验直接变好） |

**总判断**：业务逻辑（同步、加密、队列、去重、统计口径）全部可以"翻译"而不必"重新设计"，真正的成本集中在 **UI 还原** 和 **图表/滚轮选择器** 这两块。

---

## 六、鸿蒙：现实约束与节奏

必须诚实说明一件事，避免后面踩坑：

1. **HarmonyOS NEXT 不兼容 Android APK**。它不是"再打一个包"，而是要用 ArkTS 重写一遍 UI。好消息是 ArkUI 的声明式写法与 Compose 非常接近，你的 token 与交互规范可以直接复用，**逻辑层是第二次翻译而不是第二次设计**。
2. **GitHub Actions 不能官方编译鸿蒙**。`hvigor` 构建依赖 DevEco Studio 的 SDK 与签名体系，SDK 的获取与授权需你自行确认；社区有一些自建 runner 的做法，但稳定性和合规性都不适合作为主链路。
   - 短期：鸿蒙包在本地 DevEco Studio 出（这条流水线只能手工跑）。
   - 中期：如果你确实需要 CI，可以自建一台 Windows/Mac runner 装 DevEco，再挂到 Actions（可行，但要额外维护）。
   - Android 侧则完全可以用 GitHub Actions 全自动出包，没有限制。
3. 另有华为的 **ArkUI-X**（一套代码跨 Android/iOS/鸿蒙），但生态与文档偏薄，作为"未来可选项"而非首选。

**所以建议的节奏是**：Android 完整落地 → 抽稳 token 与交互规范 → 鸿蒙移植。反过来做（先鸿蒙）会让两边的设计规范都还没定型就开始分叉。

---

## 七、分阶段路线（每阶段可独立验收、可随时停）

| 阶段 | 内容 | 产出 | 验收标准 |
|---|---|---|---|
| **0 · 设计契约**（2–3 天） | 提炼 `tokens.json`；写 `interactions.md`（按压/弹窗/页面切换/触感规范）；列 26 个组件的原生规格；定 5 个关键页的还原基准图 | 三个文档 + 一个 JSON | 你能指着一页说"就照这个做" |
| **1 · Android 骨架 + 仪表盘**（5–7 天） | `PopTheme`、6 个原子组件（Card/Button/Input/Modal/Notification/Badge）、半环仪表、3D 翻转卡、BOOM 按钮、Room 建库、本地记录增删改 | 可安装的 App，能记录、能看今日数据 | **这一屏做完就能判断"味道对不对"**，不对就停下来调 |
| **2 · 其余页面**（5–7 天） | History、Analysis（自绘图表）、Settings（主题/语言/限额）、API（Supabase/飞书配置与同步） | 功能对齐现有 Web 版 | 5 个页面全部可用 |
| **3 · 云端**（3–5 天） | supabase-kt 登录/注册/登出、双向同步、飞书同步、操作日志 | 数据能与云端一致 | 换机登录能看到自己的数据 |
| **4 · 打磨与发布**（3 天） | 动效逐帧校准、触感、无障碍（字号放大/对比度）、图标与启动图、签名、AAB、GitHub Actions | 可上架的 AAB + APK | 真机三档机型冒烟通过 |
| **5 · 鸿蒙移植**（8–12 天） | ArkTS 重写 UI + 复用 token/规范；本地 DevEco 出包 | HAP | 两端并排看是同一个产品 |

合计：**Android 约 18–25 人日；鸿蒙 +8–12 人日。** 这是重写，不是包装，工作量必须按重写算。

---

## 八、怎么保证"风格高度一致"而不是"重画一遍"

你放宽了像素级要求，这是对的——原生控件有自己的字体渲染、行高、触摸反馈，逐像素对齐是给自己找麻烦。但"风格一致"必须可验证，否则最后会变成"感觉不太一样但说不上哪不对"。

用三个维度的**硬指标**来定：

| 维度 | 指标 | 判定方式 |
|---|---|---|
| **令牌一致** | 色值、描边宽度、阴影偏移、圆角、字号、字距、动效时长与曲线，全部来自同一个 JSON | 代码里不允许出现裸数值（用 `PopTokens.*`），CI 可加 lint 拦 |
| **几何一致** | 描边 4、阴影偏移 4、按下位移 2、标题旋转 -1°~-2° 的比例关系不变 | 组件级单测 + 截图并排 |
| **节奏一致** | 弹窗 300ms、仪表 1000ms、按压 150ms、弹入有回弹 | 动效规范表逐条对照 |

**验收方法（简单有效）**：阶段 1 结束时，把 Web 版的 Dashboard 与 App 版的 Dashboard 并排截图，用五个问题过一遍——

1. 第一眼的气质是同一个东西吗？
2. 描边、阴影的"厚度感"一致吗？
3. 数字够不够大、够不够抢眼？
4. 按下去的手感（位移 + 触感）对味吗？
5. 动画的节奏是"脆"还是"软"，和原来一样吗？

五个都过，就继续；有一个不对，先修再往下走。这比像素 diff 更适合你现在的目标。

---

## 九、必须提前接受的三个代价

1. **代码会分叉**。原生 App 落地后，Web 版与 App 版就是两套代码，新功能要写两遍（加上鸿蒙是三遍）。建议：**App 成为主线产品，Web 版冻结为"演示 / 分享 / 免安装试用"版本**，只做必要维护。这是做原生必须付的成本。
2. **图形与选择器最费工**。Recharts 那几张图和两个滚轮选择器，是这次迁移里最不像"照抄"的部分，需要留足时间。
3. **需要你能跑起来并发验收**。Android Studio 跑模拟器，DevEco Studio 跑鸿蒙模拟器。AI 可以把代码写完，但"味道对不对"只有你能拍板，所以每一阶段都要留一次真机/模拟器确认。

---

## 十、下一步，三个选项

| 选项 | 做什么 | 适合什么情况 |
|---|---|---|
| **A（推荐）** | 先做「阶段 0 设计契约」：产出 `tokens.json` + 交互规范 + 组件规格表 | 想先看清全貌、把地基打对再动手 |
| **B** | 直接做「阶段 1」：搭 Compose 工程 + PopTheme + 仪表盘那一屏 | 想尽快看到"原生版本长什么样"，用结果做决策 |
| **C** | 先出「26 个组件 → 原生组件」的逐条对照表 | 想先评估工作量与难度分布 |

我建议 **B**——因为你对"味道对不对"的敏感度最高，与其先写文档，不如先把最能代表风格的那一屏做出来。一屏出来，后面就是复制粘贴式的扩张。

---

## 附：与 v1 方案的关系

v1（`ANDROID-NATIVE-APP-PLAN.md`）里的 Capacitor 容器方案不再作为主线。但其中 §1.2 的清单对**继续维护 Web 版**仍然有效，尤其是：

- `index.html` 依赖 `cdn.tailwindcss.com` 运行时生成样式（仓库内 0 个 `.css` 文件）→ 离线或访问不到 CDN 时整站无样式。**只要 Web 版还在对外提供，这条就必须修**。
- Google Fonts CDN 提供 Bangers 标题字体，同样需要自托管。
- `android/` 目录（Capacitor 工程）在原生重写后可以删除，但建议先保留一个版本，作为功能对照。

——

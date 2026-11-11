# PopSmoke 组件对照表：Web → 原生

> 配套文档：`NATIVE-REWRITE-PLAN.md`
> 范围：`components/` 下全部组件 —— UI 组件 25 个 + 通用组件 2 个（`ErrorBoundary` / `LoadingSpinner`），共 27 个。
> 说明：`docs/ANDROID-NATIVE-APP-PLAN.md` 里写的"26 个"是估算，以此表为准。

---

## 一、裁决原则（按你的要求：原生最优优先，其次才是自定义样式）

每一个组件都按下面三层依次判断，**能落在上一层就绝不往下走**：

| 层级 | 判定 | 做法 | 例子 |
|---|---|---|---|
| **L1** | 平台原生控件能满足**交互意图** | 直接用平台控件，只替换视觉皮肤 | 下拉菜单用 `DropdownMenu`（拿到外部点击关闭、无障碍、键盘） |
| **L2** | 平台有行为但外观不符 | 用 `foundation` 层自建外观，**复用平台行为** | 输入框用 `BasicTextField`（拿到 IME、选区把手、自动填充），不用 Material 的 `TextField` |
| **L3** | 平台无对应 | 完全自绘 | 半环仪表、HSV 取色盘、滚轮选择器 |

### 有一条底线：这些不许为了视觉而复刻

复刻 CSS 是手段，不是目的。下面这些必须走平台原生，视觉让路：

- **返回键 / 返回手势** —— 每个弹窗、每个二级页都要能被返回键关掉
- **IME 避让** —— 输入框被键盘顶住时页面要自动让位（`imePadding()` + `adjustResize`）
- **焦点顺序与自动填充** —— 邮箱/密码要能被密码管理器识别
- **TalkBack 语义** —— 图标按钮必须有 `contentDescription`
- **手势导航边缘冲突** —— 底部 tab 栏不能和系统返回手势抢区域
- **生命周期与超时** —— 网络请求要在 `onStop` 后安全取消，不能像 Web 那样"页面还在就算活着"

---

## 二、27 个组件逐条对照

难度：★ 低（半天内）· ★★ 中（1 天）· ★★★ 高（2–3 天）· ★★★★ 很高（3 天以上）

### A. 基础与版式（4 个）

| # | 组件 | 现在做什么 | 原生方案 | 层级 | 难度 | 备注 |
|---|---|---|---|---|---|---|
| 1 | `PageContainer` | 页面外壳：限宽居中、上方留 80px 给顶栏、下方留 80px + 安全区给 tab 栏 | `Scaffold` + `Box(Modifier.widthIn(max = …))` + `windowInsetsPadding` | L2 | ★ | 现在用写死的 80px 避让导航，原生改用 insets 后不再需要猜数值 |
| 2 | `PopCard` | 波普卡片：`bg-paper` + 4px 黑边 + 8px 硬阴影；可选左上角黑色标题贴片（旋转 -1°） | `PopSurface`（见 §3）+ 可选 `PopCornerTag` | L3 | ★ | **最高复用价值的组件**，全站都用它 |
| 3 | `PopButton` | 三种变体（primary 主题色 / secondary 白 / danger 红）+ 4px 描边 + 硬阴影 + 按下位移 2px | `PopPressable`（见 §3）+ variant 参数 | L3 | ★ | 按下时阴影要同步从 4 缩到 2，见主方案第三节 |
| 4 | `PopInput` | 4px 描边输入框，三种状态（默认/错误/成功），粗黑字体 | `BasicTextField` + 装饰层（`decorationBox`）；`KeyboardOptions` 指定类型 | L2 | ★★ | **不要用 M3 `TextField`**，它自带下划线/容器色，覆盖成本高于自建 |

### B. 对话框族（11 个 → 合并为 1 个基元 + 11 种内容）

这是这次重写最值得做的一次收敛。现状是 11 个组件各自用 `createPortal` 画遮罩、画白卡、画描边标题、画两个按钮，结构几乎一样，只是内容不同。

| # | 组件 | 现在做什么 | 原生方案 | 层级 | 难度 | 备注 |
|---|---|---|---|---|---|---|
| 5 | `PopConfirm` | 确认框：题型标题（成功绿/错误红/警告黄/信息蓝）+ 消息 + 确认/取消 | `PopDialog { title, message, actions }` | L2 | ★ | |
| 6 | `PopPrompt` | 单输入框弹窗（可密码） | `PopDialog` + 输入槽，键盘回车确认 | L2 | ★ | |
| 7 | `PopSelect` | 多选项竖排按钮弹窗 | `PopDialog` + 选项列表 | L2 | ★ | |
| 8 | `PopForm` | 多字段表单弹窗（Enter 跳到下一个） | `PopDialog` + 字段列表，`imeAction = Next/Done` | L2 | ★★ | 原生自带"下一项"焦点流转，比手写 `getElementById` 稳 |
| 9 | `PopNotification` | **其实是模态框**（全屏遮罩 + 白卡 + OK 按钮），不是 toast | 二选一：① 作模态 → 并入 `PopDialog`；② 作轻提示 → 自绘底部浮层（类似 Snackbar 位置） | L2 | ★ | 建议按 ①：现在的交互本来就是"必须点 OK 才能继续" |
| 10 | `PopExternalLinkWarning` | 跳外链前警告，右上角有旋转 6° 的 POP SMOKE 贴片 | `PopDialog` + 旋转贴片装饰 | L2 | ★ | 原生侧点击外链直接走 `Intent.ACTION_VIEW`，这个警告框其实可以省掉 |
| 11 | `PopAuthModal` | 整页居中的登录/注册卡（非遮罩，是独立满屏） | **独立页面**（Navigation 目标），不是弹窗 | L2 | ★★ | 邮箱/密码要接 `Autofill`，密码管理器才认 |
| 12 | `PopPasswordResetDialog` | 设新密码（带强度提示、两次输入校验） | 独立页面 + 深链接力（Supabase recovery link → `intent-filter`） | L2 | ★★ | Web 现在靠邮箱链接回站，原生必须配深链，否则流程断 |
| 13 | `PopStorageErrorDialog` | 存储不足提示卡 | `PopDialog` 单按钮版 | L2 | ★ | |
| 14 | `PopCloudDataDialog` | 四模式对话框（下载/登录/同步差异/选择）+ 进出场动画 | 拆分：登录→页面；同步差异→`PopDialog` + 可滚动差异清单 | L2 | ★★ | 一个组件塞四种模式，原生拆开更好维护 |
| 15 | `PopUpdateDialog` | 新版本提示 + 更新日志 + 跳转 | **改用 Play In-App Update**（原生上架后不需要自己做）；侧载包保留自绘版 | L1 | ★★ | 这是原生化后能直接"删掉"的一个功能 |

基础弹窗基元建议一行代码就能用：

```kotlin
PopDialog(
    title = "删除这条记录？",
    tone = PopTone.Warning,
    onDismiss = { … },
) {
    PopDialogActions(
        confirm = "删除" to { … },
        cancel = "取消" to { … },
    )
}
```

它内部负责：遮罩 + 白卡 + 描边标题 + 入场缩放动画 + **返回键关闭** + **点击遮罩关闭** + 无障碍对话框语义。

### C. 选择器（4 个，最费工的一块）

| # | 组件 | 现在做什么 | 原生方案 | 层级 | 难度 | 备注 |
|---|---|---|---|---|---|---|
| 16 | `PopTimePicker` | 双列滚轮（时/分），40px 行高、`snap-y mandatory` 吸附、中央黄色高亮条 | `LazyColumn` + `rememberSnapFlingBehavior` + 中央固定高亮条 + 选中项反色 | L3 | ★★★ | **手感是重点**：惯性衰减、吸附回中、点击跳转的动画曲线都要调。建议抽成通用 `PopWheelColumn`，日期也复用它 |
| 17 | `PopDatePicker` | 自绘月历弹出层（7 列网格、今天高亮黄、选中反黑、上月/下月、清除/今天） | 自绘月历（`LazyVerticalGrid(7)`）+ `PopDialog` 承载 | L3 | ★★ | 不用系统 `DatePickerDialog`：它的外观改不动，且波普风需要 4px 描边格子 |
| 18 | `PopColorPicker` | 色块预览 + Hex 输入 + 可展开面板：HSV 面板、色相条、RGB 三输入 | `Canvas` + `pointerInput` 自绘 HSV 面板与色相条；RGB 用三格输入 | L3 | ★★★ | 拖拽手感 + 面板绘制是难点；建议先做"预设色块网格"版本，HSV 面板二期 |
| 19 | `PopDropdown` | 自定义下拉（触发器 + 浮层，靠 document 点击外部关闭） | `DropdownMenu` + 自定义 `DropdownMenuItem` 皮肤 | **L1** | ★★ | 直接用平台组件，**外部点击关闭、返回键、无障碍全部白拿**，比 Web 的 `mousedown` 监听可靠 |

### D. 结构化面板（4 个）

| # | 组件 | 现在做什么 | 原生方案 | 层级 | 难度 | 备注 |
|---|---|---|---|---|---|---|
| 20 | `PopNav` | 顶栏（POP**SMOKE** 双色标题 + 主题色方块 + 语言下拉）+ 移动端底部 5 tab；桌面展开成顶部横向导航 | `PopScaffold`：顶栏 + 底部 `Row` 自绘 tab；选中态颜色切换；`haptic` | L2 | ★★ | 现在硬编码 `statusBarHeight = 24`，原生改用 insets，刘海机不再裁切 |
| 21 | `PopOperationLog` | 终端风操作日志：黑标题条 + 等宽字体行 + 暂停/清空 + 底部计数 | `LazyColumn` + 等宽字体 + 自动滚到底（可暂停） | L2 | ★★ | 自动滚动要在用户手动上滑时暂停，这个细节 Web 版已有，别丢 |
| 22 | `PopSyncQueue` | 同步队列：逐条状态 + 单条重试 + 全部重试 + 清空（三个确认） | 同上 + 批量操作状态机 | L2 | ★★ | 队列本身逻辑复用 `PopSyncQueue` 背后的 service |
| 23 | `PopSystemLog` | 系统日志大窗（最大 90vh）：级别筛选 + 搜索 + 导出 + 深色终端区 | 独立页面（更适合大屏信息密度）+ 筛选 chips + 搜索框 + SAF 导出 | L2 | ★★ | 从"弹窗"改成"页面"，是因为它信息量大且要搜索，弹窗里憋屈 |

### E. 页面级与杂项（5 个）

| # | 组件 | 现在做什么 | 原生方案 | 层级 | 难度 | 备注 |
|---|---|---|---|---|---|---|
| 24 | `PopSupabaseGuide` | 五步图文引导（含代码块、链接、黄色/蓝色提示条） | `HorizontalPager` 分步引导 或 单页滚动 + 步骤锚点 | L2 | ★ | 内容照搬，代码块用等宽字体 + 可复制 |
| 25 | `PopLoading` | 全屏加载：旋转圆环 + POP**SMOKE** 标识 + 状态文案（七种状态各一种颜色） | 自绘环形进度（`Canvas` + `rotate`），**不用** `CircularProgressIndicator`（它太 Material） | L3 | ★★ | 七种状态色建议保留，是挺好的一层信息表达 |
| 26 | `LoadingSpinner` | 三种尺寸的旋转圆环（内联用） | 同 #25 的小尺寸版本，抽成一个 Composable | L3 | ★ | 和 #25 合并实现 |
| 27 | `ErrorBoundary` | React 错误边界，兜底白屏 | Compose 无直接对应：`runCatching` + 全局 `CoroutineExceptionHandler` + 崩溃页 | L2 | ★★ | 顺带接入崩溃上报（Firebase Crashlytics 或自建） |

---

## 三、先造 6 个公共基元（其余组件都是它们的组合）

这是控制"风格一致"的真正手段：把波普风的几何规则**只写一次**，之后所有组件自动继承。

| 基元 | 职责 | 关键实现 |
|---|---|---|
| `PopSurface` | 纸色底 + 4px 黑描边 + 硬阴影（offset 可配 1/2/4/8） | `drawBehind` 画偏移黑色矩形，**禁用** `Modifier.shadow` |
| `PopPressable` | 按下位移 2px + 阴影同步收缩 + 触感 | `collectIsPressedAsState` → `animateDpAsState`；阴影 offset 与位移联动 |
| `PopText` | display/body 两套字体 + 字阶 + 字距（display 加宽 0.02em） | 从 `PopTokens` 取，页面里不许写裸 `fontSize` |
| `PopDialog` | 遮罩 + 白卡 + 描边标题 + 入场动画 + 返回键/遮罩关闭 | 用 `Dialog`（窗口级），拿到焦点管理与无障碍；内容用槽传入 |
| `PopWheelColumn` | 滚轮列：吸附、惯性、中央高亮、点击跳转 | `LazyColumn` + `rememberSnapFlingBehavior`；时间与日期共用 |
| `PopScaffold` | 顶栏 + 内容区 + 底部 tab + insets 处理 | `Scaffold` + `windowInsetsPadding` + `imePadding` |

有了这 6 个，27 个组件的实际编码量会明显低于表面上看起来的数字 —— 因为它们是"装配"而不是"从零画"。

---

## 四、三个重点攻坚项（决定成败）

### 1. 滚轮选择器（`PopTimePicker` / `PopDatePicker`）

Web 版靠 CSS `scroll-snap` 白拿了吸附效果，原生要自己实现：

- 惯性衰减曲线要与系统一致（用平台的 `FlingBehavior`，别自己写物理）
- 松手后必须**精确吸附到整行**，否则"23:59"会变成"23:58"
- 中央高亮条是**固定不动**的（现在就是这么做的），滚的是内容
- 点击某一项 → 平滑滚到中间，而不是瞬间跳

这是全项目最容易"看起来像但手感不对"的地方，建议单独留 1 天专门调手感。

### 2. HSV 取色盘（`PopColorPicker`）

`Canvas` 画饱和度/亮度渐变面板 → 叠加 `pointerInput` 拖拽 → 实时换算 HSV → 输出 Hex。色相条同理。真正的坑是**拖到边缘时要跟手且不越界**，以及面板绘制的性能（建议把渐变面板缓存成一次绘制，不要每帧重算）。

建议分两步：先做"6 个预设色块 + 自定义 Hex 输入"（够用且半天完成），HSV 面板放二期。

### 3. 图表（`PopAnalysis`，不在组件表内但最费工）

Recharts 的三类图（趋势/分布/对比）建议**全部自绘 `Canvas`**，理由：

- 第三方图表库的默认视觉与波普风冲突，覆盖成本高
- 自绘能把 4px 描边、硬阴影、黑底标签这些元素带进图表，形成统一的视觉语言
- 数据量小（个人吸烟记录，最多几千条），性能无压力

这是唯一一个"自绘反而更省事"的地方。

---

## 五、建造顺序（按依赖排，不要跳）

```
第 1 批（基元层）  PopTokens → PopSurface → PopPressable → PopText → PopDialog → PopScaffold
                    ↓
第 2 批（原子层）  PopCard / PopButton / PopInput / PopLoading / LoadingSpinner / ErrorBoundary
                    ↓
第 3 批（仪表盘一屏）半环仪表 + 3D 翻转卡 + BOOM 按钮 + 日志列表   ←── 到这里就能判断"味道对不对"
                    ↓
第 4 批（对话框族）PopDialog 的 11 种内容装配（半天到一天，因为基元已就绪）
                    ↓
第 5 批（选择器）  PopWheelColumn → PopTimePicker → PopDatePicker → PopColorPicker
                    ↓
第 6 批（面板与页面）PopNav / PopOperationLog / PopSyncQueue / PopSystemLog / PopSupabaseGuide
                    ↓
第 7 批（图表）    自绘趋势图 / 分布图 / 对比图
```

**第 3 批是关键节点**：仪表盘那一屏出来了，你就能判断整条路线对不对。如果味道不对，损失只有前面两批的工作量。

---

## 六、主题资产怎么给我

你说有多套可替换主题，请按下面方式放，我来抽成 token preset：

```
design/themes-source/
  ├─ <主题名>/
  │    ├─ theme.css          # 或 .scss，只需颜色/描边/阴影相关的部分
  │    ├─ screenshot.png     # 该主题下的一张页面截图（有就最好）
  │    └─ notes.md           # 一句话说明它跟默认主题的差别
  └─ ...
```

我需要的**最小信息**只有四条：

1. **主色**（accent）是什么，深色文字还是浅色文字压在上面
2. 是否改变了**描边宽度**（有些主题可能更细/更粗）
3. 是否改变了**阴影**（偏移量/是否变成实心块）
4. 是否改变了**字体**或**圆角**

只有 1 变 → 就是普通 preset（改个色的事）；2/3/4 也变 → 属于"结构性主题"，token 结构要留出 `receipt` 槽位，我在生成器里处理。

---

## 七、这张表能推出的三个结论

1. **真正需要"从零画"的只有 6 个**（卡片、按钮、输入框、滚轮、取色盘、图表）。其余 21 个是"装配 + 复用平台行为"，工作量比看起来小。
2. **能删掉的功能有 2 个**：外链警告（原生直接走 Intent）、应用内更新提示（改用 Play In-App Update）。
3. **能合并的有 5 个**：5 个弹窗变体合并为 1 个 `PopDialog`。合并之后，未来改一次弹窗样式，全站 11 处一起变 —— 这个收益 Web 版靠 CSS 类名拿到了，原生版靠组合拿到了，效果一样。

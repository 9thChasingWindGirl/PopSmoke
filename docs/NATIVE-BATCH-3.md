# 原生第三批：把「看着像」补成「用起来是」

> 承接 `NATIVE-BATCH-1.md`（仪表盘）、`NATIVE-BATCH-2.md`（其余四页）。
> 前两批解决的是视觉还原与结构；这一批只做一件事：**上一批明确列出的功能缺口，逐条补上**。
> 视觉仍然无损，但重点从"像素对不对"转向"点下去有没有反应、反应对不对"。

## 这一批做了什么

| 缺口（第二批遗留） | 这一批的处理 |
|---|---|
| 顶栏两键「点击循环切换」 | 改为真正的浮层：色块弹 280dp 取色面板（预设色 + 色号输入），语言键弹四项菜单（选中浅灰底） |
| 记录行不能改时间 | 新增 `PopTimeWheel`（时/分双列滚轮、吸附滚动）+ 编辑键（黄底铅笔）；跨天编辑重算当天序号 |
| 没有操作日志 | 新增数据层 `OpLog` + 分析页终端卡（黑头 / 等宽行 / 灰状态栏 / 暂停 / 清空） |
| 没有备份与恢复 | 设置页可导出 JSON 备份到文件，也可用系统文件选择器导回（合并 / 覆盖两种模式） |
| 记录没有触觉反馈 | 记录与删除确认加 `HapticFeedbackType.LongPress` |

顺带补齐的底层改动：

- `StoreCodec`：JSON 编解码从 `LocalStore` 抽出来，本地文件与备份文件共用同一套格式与版本号（`v: 2`）。
  旧文件（无 `opLogs`）照常读入，缺字段取默认值 —— 升级不该让用户丢数据。
- `StoreState.opLogs`：操作日志落盘（最多 100 条），重启后仍然在。
- `LogRepository`：`record / delete / updateLogTime / clearLogs / importState` 全部在写操作处顺手记日志。
  Web 版是在 `App.tsx` 里手动补 `addOperationLog`，漏一条路径就少一条日志；原生把这件事收进仓库，调用方不用记得。
- 操作日志只存**结构化字段**（类型、时间、序号、条数），文案在渲染时按当前语言拼。
  Web 版存的是拼好的英文句子，切语言后历史日志不会跟着变。

## 四处「照着写会错」的地方

### 1. `Popup(alignment = Alignment.TopEnd)` 不是右对齐

这是这批唯一一个**截图抓不到、只能靠读源码发现**的坑。

`AlignmentOffsetPositionProvider` 的算法是：把浮层的左上角放到锚点区域内 `alignment` 算出的点上。
`TopEnd` 在锚点区域里的坐标是 `(anchorWidth, 0)`，也就是**锚点右上角** ——
浮层会从色块右边缘开始往右铺，整个跑到屏幕外。

正确做法是自定义 `PopupPositionProvider`：

```kotlin
val x = anchorBounds.right - popupContentSize.width     // 右边缘对齐
val y = anchorBounds.bottom + offsetPx                  // 落在锚点下方
```

再把它夹进窗口范围（否则窄屏上 280dp 的面板会顶出去）。Paparazzi 抓不到 Popup（独立窗口），
所以这条只能靠代码审读 + 真机确认；面板本体单独有截图用例 `topbar_theme_menu_zh`。

### 2. 菜单面板不能交给内容自适应宽度

`PopMenuItem` 里是 `fillMaxWidth()`（选中态底色要铺满整行）。如果面板宽度由内容决定，
`fillMaxWidth` 会一路吃到父级最大宽度 —— 实测语言菜单变成一条横贯全屏的白条。

所以语言菜单显式给 `width = PopLayout.MenuMinWidth (80dp)`、`padding = 0`，
对齐 Web 的 `min-w-[80px]` + 菜单项自带 `px-4 py-2`。取色面板是固定 280dp，不受影响。

### 3. 滚轮高亮条要用 `matchParentSize`，不是 `fillMaxWidth`

高亮条横贯两列滚轮（含中间冒号）。用 `fillMaxWidth` 会铺到弹窗内边距，
右端比「分」列多出一截，看起来像整体放歪了。`matchParentSize` 拿到的是**整行滚轮的实际宽度**，
再用 `drawBehind` 把 40dp 高的色带画在垂直居中位置。

滚轮本身：`contentPadding = (128 - 40) / 2 = 44dp` 才能让选中行停在正中；
选中判定取「离视口中心最近的一行」，而不是 `firstVisibleItemIndex`（后者在惯性滚动中途会跳变，数字会闪）。

### 4. 设置页引了文件选择器，Paparazzi 就炸

`rememberLauncherForActivityResult` 要求 `LocalActivityResultRegistryOwner`，而 JVM 渲染环境没有 Activity，
`settings_page_en` 直接抛 `IllegalStateException`。

测试里补了一个空实现的 registry（只实现 `onLaunch`；`onRegister` 是具体方法、不能 override）。
这是"给测试环境补一个宿主"，不是给生产代码加分支 —— 生产代码不该为测试让步。

## 与 Web 版刻意的差异（新增）

| 位置 | Web | 原生 | 原因 |
|---|---|---|---|
| 操作日志前缀色 | `text-green-400` 等浅色写在白底 | 同色系 500/600 档 | 浅绿/浅蓝在白底上对比度不足 3:1，终端字更小，读不清 |
| 记录行编辑键 | 28px | 视觉 24.5dp / 触摸 48dp | 与删除键同一套无障碍底线 |
| 取色面板 | canvas HSV 渐变 | 预设色块 + 色号输入 | 手机上小面板里拖色相条点不准；两条主路径都保留 |
| 导入备份 | 无（走云同步） | SAF 选文件 + 合并/覆盖 | 本地应用没有云端，文件就是唯一的迁移通道 |

## 明确没做的

1. **云端同步 / 账号体系**：Supabase 鉴权、飞书多维表读写、差异合并。这一批没碰，
   分析页的同步按钮仍旧跳到 API 页并提示"未配置"。它是独立一批（网络 + 加密 + 冲突解决）。
2. **系统日志**（Web 的 `PopSystemLog`）：那是给开发者看的运行日志，本地应用没有服务层，
   留着空壳比没有更误导。操作日志已经把"数据发生了什么"讲清楚了。
3. **头像**：没有登录就没有头像。

## 验证

```
node tools/gen-tokens.mjs --check      # 令牌生成物一致（新增 themePanelWidth/menuMinWidth/timeWheel*）
node tools/gen-strings.mjs --check     # 文案生成物一致（431 键 × 4 语言）
gradle :app:testDebugUnitTest          # 视觉基准 + 仓库行为测试
gradle :app:assembleDebug              # APK 8.0MB
```

新增的 6 条仓库行为测试（`LogRepositoryTest`）覆盖的是截图看不见的部分：

| 用例 | 钉住的规则 |
|---|---|
| `record_assignsDailyIndex_andWritesCreateOp` | 当天序号从 1 递增，每条记录都有 Create 日志 |
| `editingTimeWithinSameDay_keepsIndex` | 同一天改时间不改序号 |
| `editingTimeAcrossDay_reassignsIndexInTargetDay` | 跨天改时间按目标日重算序号 |
| `deleteAndClear_writeOps` | 删除/清空各写一条日志，清空带条数 |
| `importMerge_addsOnlyMissingRecords` | 合并只补缺失 id，并记一条 Sync |
| `storeCodec_acceptsLegacyFileWithoutOpLogs` | 旧备份（无 opLogs）仍可导入 |

截图清单（16 张，`app/src/test/snapshots/images/`）：

`front_en_default_theme` / `front_zh_teal_theme` / `front_ja_limit_reached` / `front_en_empty` /
`full_screen_with_chrome_en` / `full_screen_with_warning_zh` /
`back_flipped_logs_zh` / `back_flipped_logs_full_zh` /
`history_page_zh` / `analysis_week_en` / `settings_page_en` / `api_page_zh` /
**`time_picker_dialog_zh`** / **`operation_log_zh`** / **`topbar_theme_menu_zh`** / **`topbar_language_menu_zh`**

## 下一步建议

1. **同步批次**：先定"哪些字段是同步键"（`id` 已用 UUID，可直接做幂等合并），再接 Supabase。
2. **长按记录行**：呼出"编辑时间 / 删除"的快捷菜单，比两个小键更接近手机习惯（小键保留给发现性）。
3. **操作日志上限**：现在是 100 条 + 全量落盘；记录密集时可以改成环形缓冲 + 单独文件。

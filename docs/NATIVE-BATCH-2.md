# 原生第二批：其余四个页面与组件层

> 承接 `NATIVE-BATCH-1.md`（仪表盘一屏）。这一批把 Analysis / History / API / Settings
> 四个页面全部落地，并补齐它们依赖的组件。仍以 Compose 自绘为主，不使用 Material 组件。

## 这一批做了什么

| 层 | 新增 |
|---|---|
| 数据 | `AppUiState`（全应用单一状态）、`PopViewModel`（替代 `DashboardViewModel`）、`feature/stats/LogStats.kt`（分组/排序/周月统计/CSV）、`ApiConfigStore` + `SecureStore`（Keystore AES-GCM） |
| 组件 | `PopInput`、`PopSlider`、`PopSelect`、`PopColorPicker`、`PopDatePickerField`、`PopToast`、`PopLoading`、`PopStatTile`、`PopAccordionRow`、`PopBarChart` |
| 页面 | `HistoryScreen`、`AnalysisScreen`、`SettingsScreen`、`ApiScreen` |
| 骨架 | `PopApp` 五页路由（`PopTab`）+ `PopActions`；返回键从子页回记录页 |
| 文案 | `tools/gen-strings.mjs` 从 `i18n_*.ts` 生成 `PopStrings.kt`（406 键 × 4 语言） |

## 三处「照着做会错」的地方

### 1. 背面必须走真实翻转态渲染

上一批的 `back_logs_list_zh` 用例把背面板单独 `fillMaxSize` 渲染，截出来的背面被拉成整屏高，
和主区域对不上 —— 那既不是真实效果，也量不出「背面是否与正面等大」。

现在 `DashboardScreen(initialFlipped = true)` 进入真实翻转态，背面由正面高度约束。
实测正面与背面的卡片上边界都在 **y=217**（同一坐标系），完全重合。
满额 10 条时背面内部滚动，不撑大卡片、不顶开底栏。

### 2. `PopButton` 的内层必须和外层同宽

`PopPressable` 的阴影层是 `matchParentSize`。当外层拿到 `fillMaxWidth`、内层只包住文字时，
**阴影会从右边和下边漏出来，变成一条黑带**。历史页的红色「重置筛选」上肉眼可见，
像素采样也证实：内容 51→144，阴影一直到 419。

修法：`PopButton(..., fillWidth = true)` 内层补 `fillMaxWidth()`。

### 3. 卡片标题标签必须被卡片宽度约束

标题是绝对定位的黑色标签。只给 `padding` 时，长标题（「查看已保存的 API 设置」）
会直接顶出屏幕右边。改成 `Row(fillMaxWidth + padding)` 包一个 `weight(1f, fill = false)` 的 Box，
超长自动省略。

## 与 Web 版刻意的差异（都是手机上的正确做法）

| 位置 | Web | 原生 | 原因 |
|---|---|---|---|
| 日期选择 | 三列滚轮 | 月历弹窗 | 选筛选区间要翻三列 + 等惯性吸附；日历一点就到 |
| 下拉选择 | 浮层 + document 点击关闭 | 就地展开 | 浮层会盖住手指，长表单里还会被键盘顶飞 |
| 排序选中态 | `scale-105 font-bold` | 主题色反色底板 | 放大 5% 在手机上几乎看不出来 |
| 图表 | Recharts | 全自绘 Canvas | 第三方库的圆角/细线/无描边与波普风冲突，改不动 |
| 列表 | 全量渲染 | LazyColumn | 长历史不必要地测量全部行 |
| 删除按钮 | 24.5px | 视觉 24.5dp / 触摸 48dp | 手指点不准 |

## 明确没做的（下一批）

1. **云端同步**：Supabase 鉴权、飞书多维表读写、差异合并与冲突解决。这一批只把配置
   用 Keystore 加密存下来，分析页的同步按钮点了会跳到 API 页并提示"未配置"，
   而不是做一个点了没反应的假按钮。
2. **顶栏的两个键**：仍是「点一下循环切换」，Web 版是弹菜单。等设置页的选择器抽成
   通用弹层后再接。
3. **记录行的时间编辑**：需要先有时间选择器（Web 是三列滚轮）。日历弹窗已经能复用到日期部分。
4. **操作日志卡**：依赖同步与导出事件，等同步批次一起做。
5. **头像 / 账号体系**：没有登录就没有头像，整块留空。

> 2–4 已在 `NATIVE-BATCH-3.md` 处理：顶栏改成浮层菜单、记录行可改时间（滚轮选择器）、
> 分析页补上操作日志终端卡，另加备份导出/导入。1 与 5 仍留待同步批次。

## 验证

```
node tools/gen-tokens.mjs --check      # 令牌生成物一致
node tools/gen-strings.mjs --check     # 文案生成物一致（406 键 × 4 语言）
gradle :app:verifyPaparazziDebug       # 9 张基准截图逐像素比对
node tools/png-probe.mjs <png> --row Y --col X   # 任意点/行/列取色，定位偏移
```

截图清单（`app/src/test/snapshots/images/`）：
`front_en_default_theme` / `front_zh_teal_theme` / `front_ja_limit_reached` / `front_en_empty` /
`full_screen_with_chrome_en` / `full_screen_with_warning_zh` /
`back_flipped_logs_zh` / `back_flipped_logs_full_zh` /
`history_page_zh` / `analysis_week_en` / `settings_page_en` / `api_page_zh`

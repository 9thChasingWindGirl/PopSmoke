# 第 1–3 批落地：原生骨架 + 仪表盘一屏

> 对应 `docs/COMPONENT-MAPPING.md` 第五节建造顺序的第 1、2、3 批。
> 目标不是"能跑"，而是**先验证味道**：这一屏把所有硬规则都用上了，看着不对，后面 24 个组件就不用写了。

## 一、结论先说

| 项 | 结果 |
|---|---|
| 编译 | 通过（AGP 8.7.3 / Gradle 8.9 / Kotlin 2.0.21 / JDK 17） |
| 渲染 | 通过（Paparazzi 在 JVM 里渲染出 6 张 PNG，不需要设备） |
| 安装包 | `android-native/app/build/outputs/apk/debug/app-debug.apk`，**7.6MB** |
| Material 依赖 | 0。全部只依赖 Compose 底座（ui / foundation / animation） |
| 令牌来源 | `design/tokens.json` 单一真源，`PopTokens.kt` 由脚本生成，CI 有 --check 红线 |

包名 `com.popsmoke.tracker.debug`（debug 后缀），**可以和旧的 Web 套壳版同时装在一台机器上并排对比**。

```bash
adb install -r android-native/app/build/outputs/apk/debug/app-debug.apk
```

## 二、交付物

```
design/tokens.json                 # 唯一真源：颜色/描边/阴影/间距/字阶/动效/主题预设
tools/gen-tokens.mjs               # 生成器（node，零依赖），--check 供 CI 校验
tools/png-probe.mjs                # 截图像素探针，用于客观核对色值与边界
android-native/
  app/src/main/java/com/popsmoke/tracker/
    design/     PopTokens.kt(生成) PopTheme.kt PopColorParse.kt
    ui/foundation/  popShadow PopPressable PopDisplay/PopBody PopIcon PopDotGrid PopTapTarget
    ui/atoms/   PopCard PopButton PopIconButton PopTag PopDialog
    ui/chrome/  PopTopBar PopBottomBar
    feature/dashboard/  DashboardScreen SemiCircleGauge FlipCard LogList DashboardViewModel DashboardUiState
    data/       Models LocalStore LogRepository TimeText
    i18n/       PopStrings（en/zh/ja/ko，值逐条取自 i18n_*.ts）
  app/src/test/  DashboardScreenshotTest.kt + snapshots/images/*.png
.github/workflows/android-native.yml
```

已实现的组件（对照表编号）：1 `PageContainer`→骨架、2 `PopCard`、3 `PopButton`、5 `PopConfirm`→`PopDialog`、9 `PopNotification`→同基元、`PopNav` 的移动端顶栏+底栏、以及仪表盘这一屏自带的半环仪表 / 翻转卡 / 记录行。

## 三、六张基准截图

`android-native/app/src/test/snapshots/images/`：

| 文件 | 看什么 |
|---|---|
| `full_screen_with_chrome_en.png` | 整屏：顶栏 POP/SMOKE、纸面点阵、卡片、底栏五页签 |
| `front_en_default_theme.png` | 正面 · 英文 · 橙（默认色 #F97316） |
| `front_zh_teal_theme.png` | 正面 · 中文 · 青绿（验证"换主题=换一个 accent"） |
| `front_ja_limit_reached.png` | 正面 · 日文 · 达上限（红弧 + 底部红条） |
| `front_en_empty.png` | 正面 · 零记录（空弧、`--:--`、金色主题） |
| `back_logs_list_zh.png` | 背面 · 今日记录列表（行高/图标块/删除键比例） |

## 四、三处刻意的偏离（都有理由，不是为了省事）

1. **仪表进度弧的投影改硬阴影**。Web 是 `drop-shadow(0 4px 8px color/50)`（模糊）。整站规则是零模糊，单独一个模糊阴影会像贴上去的；改成偏移 4dp、透明度 0.28 的硬偏移。
2. **删除键触摸目标 48dp**，视觉仍是 24.5dp。Web 版那个 24.5px 的删除键手指点不准，这是对照表里"视觉让路"的底线。
3. **本批不做输入类交互**：记录行的"编辑时间"按钮暂未绘制（依赖第 5 批的滚轮选择器）；顶栏的色块和语言按钮暂时是"点一下循环切换 6 套预设 / 4 种语言"，正式版接 `PopColorPicker` 与下拉。这样点任何地方都有真实反应，不会有死按钮。

## 五、一个真实缺陷的复盘

第一版渲染出来，整张卡片内部是黑的。

原因：`PopCard` 的 modifier 链写成了 `background().border().popShadow()`。
Compose 的绘制类 modifier **按链顺序执行**，`drawBehind` 排在 `background` 后面，于是 8dp 的黑色阴影层被画在了卡片**上面**，把纸白底整片盖掉。

正确顺序是 `popShadow() → background() → border()`：阴影先画，被卡片本身盖住，只露出偏移出去的那条"厚边"。

值得记一笔的是：**肉眼描述没有发现这个问题**（看图的人说"黑色像背景"），是 `tools/png-probe.mjs` 扫第 300 行像素、发现卡片内部是 `#000000` 而不是 `#FDFBF7` 才定位到的。这就是为什么"看着像"不算验收。

## 六、视觉门禁怎么用

```bash
# 1. 令牌与生成物是否一致（CI 已挂）
node tools/gen-tokens.mjs --check

# 2. 重录基准图（有意改动视觉时）
cd android-native && gradle :app:recordPaparazziDebug

# 3. 比对（改代码但不想改视觉时；有像素差异即失败）
cd android-native && gradle :app:verifyPaparazziDebug

# 4. 探针：查任意点的色值 / 整行扫描边界
node tools/png-probe.mjs <png> --at 200,775 --row 300
```

`verifyPaparazziDebug` 目前还没挂进 CI 做阻塞项：截图是在 Windows 录制、CI 在 Linux 跑，我需要先确认跨平台的栅格化结果是否逐像素一致，再把它从"参考"升级成"拦截"。

## 七、体积账

7.6MB 里字体是主要成分：Bangers 91KB + HarmonyOS Sans 三个字重各约 145KB。

**没有打包中文 SC 字体**（单份 8MB，三份 24MB）。Bangers 和 HarmonyOS Sans 都没有 CJK 字形，Android 会按字形回落到系统 Noto Sans CJK —— 和 Web 版用的 Noto Sans SC 同源，观感一致。截图里的中文可以直接验证这一点。

## 八、下一步（第 4 批）

按依赖顺序：

1. `PopInput`（`BasicTextField` + 装饰层）+ `PopDialog` 的内容槽版本 → 装配登录、设置、表单三处；
2. `PopWheelColumn` → `PopTimePicker` / `PopDatePicker`，把记录行的"编辑时间"打开；
3. `PopColorPicker`（先做 6 预设 + 自定义 Hex，HSV 面板二期）。

## 九、还没验证的部分（如实说明）

- **真机运行未验证**。本机没有 Android 设备也没有模拟器：编译通过 + JVM 渲染通过，能证明代码正确、Composition 正常，但启动路径（`MainActivity` → ViewModel → 文件读写 → 边到边 inset）没有在真机上跑过。第一次安装如果崩，把 `adb logcat` 发我。
- 横屏、平板宽屏未做适配验证（布局是流式的，但没实测）。
- 阈值提示条的脉冲/跳动动画只验证了终态，动画节奏要真机看。

---

# 第一轮修正（对齐 Web 版尺寸与位置）

首屏交付后收到反馈："元素尺寸不一致、位置偏移（语言键变小、仪表区被拉长、提示条挤掉底栏）"。
排查出四处**根因**，都不是"调数值"能解决的，全部改在规则层。

## 1. 卡片被拉满 → 仪表区跟着变长

Web 版的卡片高度是**内容高度**：`PageContainer` 是 `min-h-screen + flex items-center`，
中间那层 `h-full` 的父级高度是 auto，百分比高度解析成 auto，所以卡片只是"内容那么大"的一块，垂直居中漂浮。

原生第一版把卡片写成 `fillMaxSize()`、仪表区写 `weight(1f)`，等于把整块剩余高度塞给仪表区。
实测卡片高 789dp，Web 版只有约 425dp —— 空出来的近 400dp 全在仪表弧上下，所以"仪表区变长了"。

改法：
- `FlipCard` 改成**正面驱动高度**、背面 `matchParentSize()`（等价于 Web 版 back 面 `absolute inset-0`）；
- 仪表区去掉 `weight(1f)`，恢复固定 105dp；
- 卡片放进 `Box(weight(1f), contentAlignment = Center)` 里居中。

## 2. 提示条挤掉底栏

提示条原本和卡片区在同一个 `Column` 里抢高度，卡片又是 `fillMaxSize`，两者一起把区域撑破。
现在提示条是 `Column` 里**先被测量**的固定高度子项，卡片区是 `weight(1f)`；
底栏在更外层（`PopApp`）、高度固定，从结构上就不可能被挤走。
新增截图 `full_screen_with_warning_zh` 专门钉住这条红线。

## 3. 语言键 22×22 → 28×28

Web 版是 `w-8 h-8` 的固定方块（2rem = 28dp），文字 `text-xs` 居中。
原生第一版用 `padding` 撑开，实测 22dp 见方、字号还大一号，和左边 28dp 的色块并排就明显不齐。
改成 `PopLayout.Chip = 28.dp` + `PopTypeScale.Xs` 居中。

## 4. 所有带描边的组件都小一圈（最隐蔽的一处）

Compose 的 `Modifier.border` **不参与测量**，描边压在 padding 上；
CSS 的 `border` 是盒子的一部分（`box-sizing: border-box`）。
于是每个"内容撑开 + 有描边"的组件都比 Web 版小 **2×描边宽**：
卡片少 8dp、贴纸少 4dp、按钮少 8dp、列表行少 4dp。
单看都像"差一点"，叠一屏就成了累计位置偏移。

新增 `Modifier.popBorder(width, color, shape)` = `border(...).padding(width)`，语义等同 CSS。
**只有内容撑开的元素用它**；已经定死尺寸的（28×28 色块、42×42 图标键、40×40 图标块）
CSS 的 border-box 里描边本来就在尺寸之内，继续用 `border` 才是对的 —— 这条写在函数注释里了。

## 5. 行高：Compose 默认比 Tailwind 矮 10%~15%

Web 每档字号自带配套行高（`text-xl` → 1.75rem，不是 1.4 倍），原生默认按字体度量算。
新增 `type.lineHeight` 令牌 + `PopLineHeight.of(size)`。

踩坑记录：**只设 `TextStyle.lineHeight` 是无效的**。把标签行高改成 40sp，
截图里标签高度一个像素都没变；必须同时写
`LineHeightStyle(Alignment.Center, Trim.None)` 才是 CSS 的 half-leading 语义。

## 验证

```bash
node tools/gen-tokens.mjs --check          # 令牌与 JSON 一致
gradle :app:recordPaparazziDebug           # 7 张基准图
node tools/png-probe.mjs <png> --col 200   # 新增：整列扫描，量高度/间距/偏移
```

`--col` 是这轮加的：横向位置能靠 `--row` 看，但"位置偏移"是**竖向**的，
只能一列一列读出来。这一轮所有结论都是靠它量出来的，不是看图看出来的。

实测结果（1dp ≈ 1.17px，Pixel 5）：

| 元素 | Web 版 | 修正前 | 修正后 |
|---|---|---|---|
| 卡片总高 | ~425dp | 789dp | 414dp |
| 语言键 | 28×28 | ~22×22 | 28×28 |
| 贴纸高 | 46dp | 40dp | 45.4dp |
| BOOM 按钮高 | 99dp | 88dp | ~97dp |
| 卡片区被拉满 | 否 | 是 | 否 |
| 提示条挤底栏 | 否 | 是 | 否 |


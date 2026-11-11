# 第六批：真机反馈修复

上一批交的是 debug 包，真机跑出来的问题比截图能暴露的多得多。这一批只做一件事：
把用户报的五条逐条钉死。

## 1. 记录一次之后无法再记（最严重）

现象：记一条后界面失去响应，重开 App 也一样；清掉本地数据能恢复，但只恢复一次。

这一条不能只靠猜，所以把整条写入链路按"哪里可能永久卡住"过了一遍，改了四处：

| 位置 | 原来的问题 | 现在 |
|---|---|---|
| `LocalStore.save` | 固定用 `popsmoke.json.tmp`，两次保存交错会互相踩；而且**先删主文件再 rename**，中间被杀就丢数据 | 唯一临时文件；`renameTo` 自带覆盖，不再先删；rename 失败退回复制 |
| `LogRepository.commit` | `store.save` 抛异常会顺着协程往上掀，**而收集 `state` 的协程一死，界面就永久不跟随数据** | 落盘失败一律吃掉，只补一条 System 操作日志 |
| `LogRepository` 写路径 | 六个写方法各自「读状态→算→提交」，并发时后写覆盖先写 | 统一走 `Mutex`；每个写方法进锁后先 `ensureLoaded()` |
| `PopViewModel` 收集器 | `buildUi` 抛一次异常，collect 直接终止，之后点什么都没反应 | 单帧 `runCatching`，失败退回上一帧 |

第三行那个 `ensureLoaded` 还顺手修掉一个更阴的问题：App 启动瞬间点记录，
写操作会基于空状态算出「1 支」并落盘，把磁盘上的历史记录整个覆盖掉。

**坦白说**：这四条都是"让冻结不可能发生"的加固，不是对某个确定根因的定点修复。
如果真机上还复现，现在至少会在操作日志里留下一条 System 记录，可以据此继续定位。

顺带把记录按钮的触觉反馈从 `LongPress` 换成轻振 —— 前者在 Android 上是实打实的
500ms 长振，点一下麻半秒，本身就像卡住了。

## 2. 状态栏预留过多、不自适应

顶栏原来是 `height(60 + inset)` 再 `padding(top = inset)`：高度被钉死，
系统字体放大（fontScale 1.3/1.5）时文字会被裁；inset 取的是 `statusBars`，不含挖孔。

改成 `padding(top = inset) + heightIn(min = 60)`，让内容自己撑开；
inset 改取 `safeDrawing`（含刘海/挖孔）。`border` 移到 padding 之前，
否则那条 4dp 黑边会画在状态栏下方，顶上多出一段白边。

**底栏不能跟着改**——它的页签是 `fillMaxHeight()`，一旦最大高度不受限就会撑满剩余空间，
把内容区压成 0（这一版实测过：内容一片空白、页签文字跑到屏幕中间）。底栏保持固定高度。

## 3. 色盘缺失

之前只有 6 个预设色块 + 色号输入框，没有可拖的色盘。现在补上：

- 饱和度 × 明度面板（120dp 横条）+ 色相条，都带指示器
- **拖动时只改本地预览，松手才回调**。回调一路走到写文件，每帧一次全量落盘会让手指跟不上
- 面板做成横条而不是正方形：它要装进顶栏那个 280dp 浮层，正方形会把底部预设色块顶出窗口——
  "看不到色盘"很可能就是这么来的

新增两个令牌 `svPanelHeightPx` / `hueBarHeightPx`，走 tokens.json 生成，没有手改 `PopTokens.kt`。

## 4. 翻转卡片掉帧

三个来源，逐个处理：

- **背面之前全程参与组合**。它是 LazyColumn，700ms 翻转里每帧都在测量和绘制。
  改成只在 `rotation >= 90°` 后组合 —— 90° 时两面都侧立不可见，切换看不出来
- **page 底纹逐点绘制**：一屏 1080×2400 约 6500 个 `drawCircle`。改成把一格烘焙成
  20×20 位图，用 `ImageShader(TileMode.Repeated)` 平铺，整屏一次 `drawRect`
- `cameraDistance` 从 graphicsLayer 的 lambda 里挪到组合期，不再每帧 `Dp.toPx()`

## 5. 关于栏点击无响应

`LinkRow` 只有文字和边框，**没有 onClick**。现在接上：

- 博客 `https://blog.whysoserious.dpdns.org/`
- GitHub `https://github.com/9thChasingWindGirl`

先弹"即将访问外部网站"确认（沿用 Web 版 `PopExternalLinkWarning` 的语义，文案复用已有的
`externalLinkWarningTitle/Message/Continue/Cancel`），确认后用 `ACTION_VIEW` 交给系统浏览器。

## 验证

- 57 条单测全绿
- 22 张截图基准全部重录（底纹实现换了，像素必然变），逐张看过：整屏布局、卡片、设置页色盘均正常
- CI 三轮全绿，`verifyPaparazziDebug` 在 Linux 上同样通过（跨平台渲染一致这一点仍然成立）
- APK 8.10 MB

## 未做

- 设置页 "CLOUD ACCOUNT" 卡里出现两个文案相同的 "API INF" 按钮（账户卡的"去配置"+ 设置页的接口管理），
  视觉上像重复，需要和 Web 版对一下再决定留哪个
- 记录卡死的真机复现：加固后如果仍复现，请把操作日志里那条 System 记录的时间点发我

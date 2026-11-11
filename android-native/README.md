# PopSmoke · Android 原生工程

Kotlin + Jetpack Compose 重写，**不使用 Material 组件库**（M3 的柔和阴影、大圆角、涟漪与波普风结构性冲突）。

## 构建

本机需要 JDK 17 + Android SDK 35。本仓库不提交 gradle wrapper 二进制，CI 由 `gradle/actions/setup-gradle` 提供固定版本。

```bash
# 有 wrapper 时
./gradlew :app:assembleDebug
# 没有 wrapper 时（用本地安装的 Gradle 8.9）
gradle :app:assembleDebug
```

`local.properties` 里写 `sdk.dir=...`（已在 .gitignore）。

## CI

`.github/workflows/android-native.yml` 在 PR 与 main 推送（触及 `android-native/**`、`design/`、`tools/`、`i18n/`）时运行：

1. `gen-tokens --check` / `gen-strings --check`：生成物与源不一致直接失败
2. `assembleDebug` 出包并上传 artifact
3. `verifyPaparazziDebug` 比对基准图，**这是硬门槛**

顺序是"先出包再比对"：截图挂了 run 会标红，但包已经在 artifact 里，不用白等一轮。基准图在 Windows 录制、CI 在 Linux 渲染，实测逐像素一致，所以这里不需要放宽。

取 CI 产物：

```bash
gh run list --limit 3                                   # 找 run id
gh run download <run-id> -n popsmoke-debug-<run_number> -D out
```

artifact 保留 7 天（仓库上限）。`release` job 只在手动触发时跑，需要 `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` 四个 Secret，缺一个就报错退出，不会产出装不上的未签名包。

## 令牌

所有色值、描边、阴影、间距、字阶、动效都来自仓库根的 `design/tokens.json`，由脚本生成 `design/PopTokens.kt`：

```bash
node tools/gen-tokens.mjs           # 生成
node tools/gen-tokens.mjs --check   # 校验（CI 用）
```

**不要在页面代码里写裸数值**。需要新数值时先加到 JSON，再重新生成。

注意 `base.rem = 14`：Web 版在 `max-width: 640px` 时 `html { font-size: 14px }`，所以手机上 1rem = 14px。令牌里的 rem 值都会乘以 14 生成 dp/sp，直接改用 16 会让整屏比 Web 版大 14%。

## 截图测试（视觉回归）

```bash
gradle :app:recordPaparazziDebug   # 重录基准图 → app/src/test/snapshots/images
gradle :app:verifyPaparazziDebug   # 比对，有差异即失败
```

渲染在 JVM 里完成（layoutlib），不需要模拟器。探针工具可查任意像素：

```bash
node ../tools/png-probe.mjs app/src/test/snapshots/images/<file>.png --at 200,775 --row 300
```

## 目录

```
design/        令牌（生成）+ 主题（accent / onAccent）
ui/foundation/ 绘制与交互基元：popShadow、PopPressable、PopDisplay/PopBody、PopIcon、PopDotGrid
ui/atoms/      PopCard / PopButton / PopTag / PopDialog
ui/chrome/     顶栏、底栏
feature/       按屏组织，一屏一包
data/          模型、本地存储、仓库
data/cloud/    HTTP 传输、Supabase 账户、飞书备份、会话加密存储
data/sync/     上行队列（记录改动的待推送操作），独立于 StoreState 落盘
i18n/          四语言文案（值取自 Web 版 i18n_*.ts）
```

## 云端

两条通道分工不同，不要合并成一个"云同步"开关：

| 通道 | 职责 | 表 / 端点 |
|------|------|-----------|
| Supabase | 账户与配置（上限、阈值、主题、语言） | GoTrue `/auth/v1/*`、PostgREST `app_settings` |
| 飞书 | 记录备份 | 自建中继 `GET/POST {url}`，落到多维表格 |

网络层只有 `HttpURLConnection` + `org.json`，没有 OkHttp/Ktor。会话（access/refresh token）用 Keystore AES-GCM 加密落盘，见 `data/cloud/SessionStore.kt`。


## 两条硬约束

1. **屏幕绘制顺序**：`popShadow()` 必须写在 `background()` 之前。Compose 的绘制类 modifier 按链顺序执行，顺序反了阴影会盖在内容上（卡片刻度会整片变黑）。
2. **阴影零模糊**：不用 `Modifier.shadow`。硬阴影 = 同形状纯色层 + 位移，见 `ui/foundation/PopModifiers.kt`。

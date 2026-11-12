// 版本集中在此，app 模块不重复声明。
// AGP 8.7.3 要求 Gradle >= 8.9；Compose 编译器由 Kotlin 2.0 的 compose 插件提供，不再需要 composeOptions。
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("app.cash.paparazzi") version "1.3.5" apply false
}

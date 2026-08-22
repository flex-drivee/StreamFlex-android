// 📂 ROOT build.gradle.kts
plugins {
    // 1. Android Application Plugin
    id("com.android.application") version "8.13.2" apply false

    // 2. Kotlin Plugin
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
}
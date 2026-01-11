// 📂 ROOT build.gradle.kts
plugins {
    // 1. Android Application Plugin
    id("com.android.application") version "8.13.2" apply false

    // 2. Kotlin Plugin (Using 2.0.0)
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false

    // 3. ⚡ THE FIX: Add the new Compose Compiler Plugin explicitly
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}
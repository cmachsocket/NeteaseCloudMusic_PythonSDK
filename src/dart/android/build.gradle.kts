// musiclibrary plugin (FFI) Android 端 build 脚本
//
// 2026-08-25 改为预编译 .so 嵌入:不再现场编译 MusicLibrary。
//
// 职责现在只剩:
//   1. 把 src/main/jniLibs/<abi>/ 下的预编译 .so 打包进 APK
//   2. AGP 默认 jniLibs 自动处理,这里不再声明 externalNativeBuild / prefab / dependencies
//
// .so 由 ../MusicLibrary 的 scripts/build-android.js 预编译产出 (NDK r29,4 ABI:
// arm64-v8a / x86_64 / armeabi-v7a / x86)。build-android.js 跟 release zip 流程都用
// 同一份脚本,保证 APK 里跟 GitHub release 装的库行为一致。
//
// 7 个 .so 全部以预编译形式塞进 jniLibs/:
//   libncm_music_api / libkugou_music_api / libengine / libqjs / libcurl / libssl / libcrypto
// (curl 跟 openssl 来自 com.android.ndk.thirdparty: curl:7.85.0-beta-1 / openssl:1.1.1l-beta-1,
// 之前 commit cab211c 想用 prefab runtime inject 路径走 libssl,但 plugin 没 externalNativeBuild
// 时 prefab 不会自动转写 .so 到 APK;改回预编译嵌入更直接,跟其他 .so 同等待遇)。

plugins {
    id("com.android.library")
}
repositories {
    google()
    mavenCentral()
}
android {
    namespace = "com.example.musiclibrary"
    // 跟随宿主 app 的 compileSdk。Flutter plugin loader 会注入
    // `flutter.compileSdkVersion` extension 到 plugin module,跟 path_provider_android
    // / sqflite_android 一样的写法。一定要 ≤ 宿主 app 的 compileSdk。
    // GitHub Actions 跑 .github/workflows/android-build.yml 时 SDK 由 action 自动下载,
    // action 默认下 build-tools 36.0.0 + platforms android-36,必须严格 compileSdk = 36。
    // 别因为本地装不上 36 就改 37 (2026-08-25 cab211c 错了一次)。
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 23
        // 默认 ABI 过滤 (全 ABI,跟 build-android.js --all 对齐)
        // ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }
}

// 2026-08-25: 不再需要 prefab / curl / openssl dependency,7 个 .so 全部作为预编译
// 产物打入 APK。libssl / libcrypto 走 jniLibs 嵌入而不是 prefab (prefab 在 plugin
// 没 externalNativeBuild 时不会自动转写 .so 到 APK)。
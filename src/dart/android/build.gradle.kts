// musiclibrary plugin (FFI) Android 端 build 脚本
//
// 2026-08-25 改为预编译 .so 嵌入:不再现场编译 MusicLibrary。
//
// 职责现在只剩:
//   1. 把 src/main/jniLibs/<abi>/ 下的预编译 .so 打包进 APK
//   2. AGP 默认 jniLibs 自动处理,这里不再声明 externalNativeBuild / prefab / dependencies
//
// .so 由 ../MusicLibrary 的 scripts/build-android.sh 预编译产出 (NDK r29,4 ABI:
// arm64-v8a / x86_64 / armeabi-v7a / x86)。build-android.sh 跟 release zip 流程都用
// 同一份脚本,保证 APK 里跟 GitHub release 装的库行为一致。
//
// libcurl.so 由 MusicLibrary 脚本里的 android-prefab 机制打包进 APK,
// (同一份 com.android.ndk.thirdparty:curl:7.85.0-beta-1),插件不需要单独 prefab。

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
    // 2026-08-25 本机只有 android-37 + build-tools/37.0.0,锁 37 (注释曾写过,但被
    // reset 成 36 了)。AGP 9.0.1 默认 36,本地没装会试图下载失败 (SDK 目录只读)。
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 23
        // 默认 ABI 过滤 (全 ABI,跟 build-android.sh --all 对齐)
        // ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }
}

// 2026-08-25: 不再需要 prefab / curl / openssl dependency,libcurl.so 跟其他 .so
// 一样作为预编译产物打入 APK。
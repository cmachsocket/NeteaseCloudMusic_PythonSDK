// musiclibrary plugin (FFI) Android 端 build 脚本
//
// 职责:
//   1. 通过 NDK 编译 ncm_music_api.so (网易云/酷狗 API 共享库)
//   2. 通过 Maven prefab 引入 libcurl 给 native 端使用
//   3. 声明 plugin bundle 哪些 .so 给 Flutter 工具链打包进 APK
//
// 依赖的 sibling submodule: ../../../../MusicLibrary/ (C/JS/QuickJS native 源码)
//
// 见: src/dart/android/src/main/CMakeLists.txt

plugins {
    id("com.android.library")
}

android {
    buildFeatures {
        prefab = true
    }
    namespace = "com.example.musiclibrary"
    // 跟随宿主 app 的 compileSdk。Flutter plugin loader 会注入
    // `flutter.compileSdkVersion` extension 到 plugin module,跟 path_provider_android
    // / sqflite_android 一样的写法。一定要 ≤ 宿主 app 的 compileSdk
    // (AGP 不允许 plugin 比 host 编更新, 否则 :app:checkDebugAarMetadata 会 fail
    // 报 'Dependency :musiclibrary requires ... compile against version 37 or later',
    // 2026-08-22 CI 复现)。
    compileSdk = flutter.compileSdkVersion
    // buildToolsVersion 不写,跟 AGP 默认走(AGP 9.0.1 默认 36.0.0)。

    // 跟随宿主 app 的 ndkVersion(主项目 gradle.properties: android.ndkVersion=29.0.14206865)
    ndkVersion = "29.0.14206865"

    // 调用 plugin 自己的 CMakeLists.txt (cmake_minimum_required=3.21)
    externalNativeBuild {
        cmake {
            path = file("src/main/CMakeLists.txt")
            // 不指定 version —— 让 AGP 走默认选择 (SDK cmake/ 或 PATH),
            // CI runner SDK 一般预装 cmake;3.22.1+。
            // 之前写死 '3.21.0' 导致 [CXX1300] CMake '3.21.0' was not found in SDK,
            // 因为 SDK Manager 包的安装路径 / 版本会随 runner 时间点变化
            // (2026-08-22 CI 复现)。
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 23
    }

    // ABI 过滤按需开(默认全架构)
    // ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
}

// ========== libcurl via Maven Prefab ==========
// AGP 8.0+ 默认 prefab=true,可以直接 find_package(CURL CONFIG) 拿到 .so + headers。
// Google 官方 NDK 提供的 prefab 包(自动传递 openssl 依赖)。
dependencies {
    implementation("com.android.ndk.thirdparty:curl:7.85.0-beta-1")
}
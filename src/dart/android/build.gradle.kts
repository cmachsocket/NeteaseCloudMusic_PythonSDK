// musiclibrary plugin (FFI) Android 端 build 脚本
//
// 职责:
//   1. 通过 NDK 编译 ncm_music_api.so (网易云/酷狗 API 共享库)
//   2. 通过 Maven prefab 引入 libcurl 给 native 端使用
//   3. 声明 plugin bundle 哪些 .so 给 Flutter 工具链打包进 APK
//
// 依赖的 sibling submodule: ../../../../../MusicLibrary/ (C/JS/QuickJS native 源码)
//
// 见: src/dart/android/src/main/CMakeLists.txt

plugins {
    id("com.android.library")
}

android {
    // prefab=true: AGP 8.0+ 默认 true, 显式声明以防老版本默认关掉。
    // prefab 注入 curl::curl IMPORTED INTERFACE target, native 端直接 link。
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
        // 用 ndk.stl 设 c++_shared, 跟 prefab 的 libcurl (shared STL) 一致。
        // 不能用 externalNativeBuild.cmake.arguments("-DANDROID_STL=c++_shared")——
        // AGP 9 newDsl 下 arguments() 不被 Kotlin DSL 推断到
        // (CI 2026-08-23 "Unresolved reference 'arguments'" 复现)。
        // ndk { stl = "c++_shared" } 是官方推荐用法, 类型安全且 AGP 9 兼容。
        ndk {
            stl = "c++_shared"
        }
    }

    // ABI 过滤按需开(默认全架构)
    // ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
}

// ========== libcurl via Maven Prefab ==========
// AGP prefab=true (上面 buildFeatures 里开), Google 官方 NDK 提供的 prefab 包
// com.android.ndk.thirdparty:curl 自动生成 curl::curl target 给 native 端 link,
// prefab.json 已经声明 openssl 依赖传递, 不要额外 implementation openssl
// (会撞版本, 不同 prefab 包的 openssl 可能 ABI 不一致)。
dependencies {
    implementation("com.android.ndk.thirdparty:curl:7.85.0-beta-1")
    implementation("com.android.ndk.thirdparty:openssl:1.1.1l-beta-1")
}
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
    // 2026-08-23 重构:放弃 AGP prefab 引入 libcurl,改为手动链 libcurl.so。
    // libcurl.so + curl headers 手工抽出在 src/main/jniLibs/<abi>/ + src/main/jniIncludes/curl/。
    // prefab=false 避免 AGP 触发 prefab CLI / --stl 校验 (它不接受 plugin 的 ndk.stl 设置)。
    buildFeatures {
        prefab = false
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
        // 2026-08-23 重构:不再走 prefab, ndk.stl 这个 deprecated 设置不再需要。
        // CMakeLists 里 set(ANDROID_STL c++_shared) 只影响 CMake toolchain 端,
        // 跟 prefab CLI 无关 (prefab=false)。
        // ndk { stl = "c++_shared" } 删了也没影响。
    }

    // ABI 过滤按需开(默认全架构)
    // ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
}

// 2026-08-23 重构: 不再依赖 AGP prefab 拉 libcurl, libcurl.so 手工抽出在
// src/main/jniLibs/<abi>/, AGP 默认会打包进 APK。
// 之前 implementation 了 com.android.ndk.thirdparty:curl + openssl prefab 包,
// 删了。openssl prefab 也在删掉列表。
// 运行时 libssl.so / libcrypto.so / libz.so 由 Android system image 提供, 不需要
// 打包到 APK。
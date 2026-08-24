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
repositories {
    google()
    mavenCentral()
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
    // 2026-08-24 shared_build 适配: 锁 compileSdk=37, 跟宿主 app + 本地 SDK 已装的
    // platforms/android-37.0 + build-tools/37.0.0 对齐。
    // flutter.compileSdkVersion (Flutter 3.47 默认 36) 在 AGP 9.0.1 下会触发
    // 下载 build-tools;36.0.0 + platforms;android-36, 而 /opt/android-sdk 无写权限。
    compileSdk = 37
    // AGP 9.x 默认 build-tools 36.0.0, 本地没装。显式锁 37.0.0 (本地有)。
    buildToolsVersion = "37.0.0"
    // buildToolsVersion 不写,跟 AGP 默认走(AGP 9.0.1 默认 36.0.0)。

    // 跟随宿主 app 的 ndkVersion(主项目 gradle.properties: android.ndkVersion=29.0.14206865)
    ndkVersion = "29.0.14206865"

    // 调用 plugin 自己的 CMakeLists.txt (cmake_minimum_required=3.21)
    externalNativeBuild {
        cmake {
            path = file("src/main/CMakeLists.txt")
            // 2026-08-24 shared_build 适配: SDK 没装 cmake, local.properties 的 cmake.dir
            // 指向 plugin 内部 .cmake-sdk/4.4.2 (symlink 到系统 /usr/bin/cmake 4.4.2,
            // AGP 要求目录名跟 cmake --version 主版本一致)。
            version = "4.4.2"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 23
        // arguments 是 defaultConfig.externalNativeBuild.cmake 下的属性
        // (ExternalNativeCmakeOptions),不是模块级 cmake block 的。
        // 模块级 android.externalNativeBuild.cmake 只接 path / version。
        // 之前 shared_build 在 defaultConfig 嵌套了 externalNativeBuild 是语法错误,
        // 这里走正确的 defaultConfig.externalNativeBuild.cmake 路径。
        externalNativeBuild {
            cmake {
                arguments.add("-DANDROID_STL=c++_shared")
            }
        }
    }

    // ABI 过滤按需开(默认全架构)
    // ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
}

// ========== libcurl via Maven Prefab ==========
// AGP 8.0+ 默认 prefab=true,可以直接 find_package(CURL CONFIG) 拿到 .so + headers。
// Google 官方 NDK 提供的 prefab 包(自动传递 openssl 依赖)。
dependencies {
    implementation("com.android.ndk.thirdparty:curl:7.85.0-beta-1")
    implementation("com.android.ndk.thirdparty:openssl:1.1.1l-beta-1")
}
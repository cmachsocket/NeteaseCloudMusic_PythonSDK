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
    namespace = "com.example.musiclibrary"
    // 跟随宿主 app 的 compileSdk(主项目用 flutter.compileSdkVersion=37)
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    // 跟随宿主 app 的 ndkVersion(主项目 gradle.properties: android.ndkVersion=29.0.14206865)
    ndkVersion = "29.0.14206865"

    // 调用 plugin 自己的 CMakeLists.txt
    externalNativeBuild {
        cmake {
            path = file("src/main/CMakeLists.txt")
            version = "3.21.0"
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
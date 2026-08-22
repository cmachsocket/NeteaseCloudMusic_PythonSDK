# musiclibrary Android 端 native build
#
# 只 build ncm_music_api 这一个共享库,其他(engine/kugou_music_api)用 option 关掉。
# libcurl 通过 AGP prefab 从 Maven 依赖里引入(NDK r29 不自带 libcurl)。
#
# 路径关系(plugin 在 src/dart/android/src/main/):
#   宿主项目根: ../../../../../
#   MusicLibrary 主项目: ../../../../MusicLibrary/
#   quickjs submodule: ../../../../MusicLibrary/quickjs/
cmake_minimum_required(VERSION 3.21)
project(musiclibrary_android LANGUAGES C)

# ========== 定位 MusicLibrary ==========
# CMAKE_CURRENT_SOURCE_DIR = NeteaseCloudMusic_PythonSDK/src/dart/android/src/main
# 需要 6 个 ../ 才到 flutter_netease_music 根 (NeteaseCloudMusic_PythonSDK 是同级)
# 之前写 4 个 ../ 只回到 NeteaseCloudMusic_PythonSDK, 报找不到 (2026-08-22 CI 复现)
get_filename_component(MUSICLIBRARY_ROOT
    "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../MusicLibrary"
    ABSOLUTE)

if(NOT EXISTS "${MUSICLIBRARY_ROOT}/CMakeLists.txt")
    message(FATAL_ERROR
        "MusicLibrary/CMakeLists.txt not found at ${MUSICLIBRARY_ROOT}.\n"
        "Make sure MusicLibrary git submodule is initialized:\n"
        "  git submodule update --init --recursive")
endif()

message(STATUS "musiclibrary: using MusicLibrary at ${MUSICLIBRARY_ROOT}")

# ========== libcurl via Prefab ==========
# AGP 8.0+ 默认开启 prefab。Google 官方包 com.android.ndk.thirdparty:curl 在
# plugin 的 build.gradle.kts 通过 implementation 引入,AGP 会生成 .cxx/.../prefab/modules/curl
# 并提供 `curl::curl` IMPORTED INTERFACE target (注意: prefab 不提供
# curlConfig.cmake / curl-config.cmake, 所以 find_package(curl CONFIG) 总是
# 失败 —— 2026-08-22 CI [CXX1429] 复现)。
# 直接用 prefab 注入的 curl::curl target, 不要 find_package。
# prefab 依赖的 openssl 也会自动传递。
if(NOT TARGET curl::curl)
    message(FATAL_ERROR "curl::curl target not provided by AGP prefab. "
        "检查 plugin build.gradle.kts 是否 implementation 了 "
        "'com.android.ndk.thirdparty:curl:7.85.0-beta-1'")
endif()

# ========== 子库选择 ==========
# Android 端只需要 ncm_music_api,关闭 engine / kugou 减少 APK 体积
# (engine 会被 ncm_music_api 间接依赖,所以这里仍然保留 ON;
#  kugou 完全不需要,关掉)
set(MUSICLIBRARY_BUILD_ENGINE ON CACHE BOOL "" FORCE)
set(MUSICLIBRARY_BUILD_KUGOU  OFF CACHE BOOL "" FORCE)
set(MUSICLIBRARY_BUILD_NCM    ON  CACHE BOOL "" FORCE)

# ========== 引入 MusicLibrary 主 CMakeLists ==========
add_subdirectory(${MUSICLIBRARY_ROOT}
                 ${CMAKE_CURRENT_BINARY_DIR}/musiclibrary_build)

# ========== link libcurl 给 engine_config (Android 平台需要) ==========
# MusicLibrary 主 CMakeLists 里只在 UNIX AND NOT ANDROID / WIN32 链接 curl,
# 所以 Android 平台默认 curl 是断的。这里手动补上。
# prefab 模块导出的是 "curl::curl" target,会自动传递依赖 (openssl)。
target_link_libraries(engine_config INTERFACE curl::curl)
target_link_libraries(engine           PRIVATE curl::curl)

# ========== 输出 ncm_music_api.so 给 Flutter 工具链打包 ==========
# 设置统一的输出目录,跟其他 plugin 的 native lib 输出保持一致
set_target_properties(ncm_music_api PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/output)

# 暴露给 flutter 工具链:声明 plugin bundle 了哪些 native lib
set(musiclibrary_bundled_libraries
    $<TARGET_FILE:ncm_music_api>
    PARENT_SCOPE)

# Android 15+ 需要 16KB page size 对齐
if(ANDROID)
    target_link_options(ncm_music_api PRIVATE "-Wl,-z,max-page-size=16384")
endif()
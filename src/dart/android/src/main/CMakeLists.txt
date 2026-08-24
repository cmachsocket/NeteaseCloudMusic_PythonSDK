# musiclibrary Android 端 native build
#
# 只 build ncm_music_api 这一个共享库,其他(engine/kugou_music_api)用 option 关掉。
#
# 2026-08-23 重构:放弃 AGP prefab (com.android.ndk.thirdparty:curl:7.85.0-beta-1)
# 直接 prefab 引入 libcurl 的方案,改用手动链 libcurl.so:
#   - prefab CLI --stl 在 AGP 9.x 下不接受 plugin ndk.stl 设置, 永远传 c++_static
#   - 但 libcurl prefab abi.json 要求 c++_shared, 触发 [CXX1212] "static STL
#     but requires shared STL" 错
#   - 不走 prefab CLI → 没这个校验 → STL 选择完全由 plugin 自己控
#   - libcurl.so / libssl.so / libcrypto.so / libz.so 在 runtime 由 Android
#     system image 提供 (dlopen),plugin 不需要打包 ssl/crypto/z 到 APK
#     (注意 libcurl.so 本体是 plugin 自带的,需要打包进 APK jniLibs/)
#
# 路径关系(plugin 在 src/dart/android/src/main/):
#   宿主项目根: ../../../../../
#   MusicLibrary 主项目: ../../../../MusicLibrary/
#   quickjs submodule: ../../../../MusicLibrary/quickjs/

cmake_minimum_required(VERSION 3.21)

# STL = c++_shared (跟 libcurl prefab 一致, prefab CLI 不参与了所以这条只影响
# CMake toolchain 端, 跟 AGP 内部 --stl 决策无关)
set(ANDROID_STL c++_shared)

# 必须启 CXX —— engine / kugou / ncm 是 C++, 只启 C 会让 CMake toolchain
# 不初始化 C++ runtime, 即使 -DANDROID_STL 传了也 link 不到 libc++_shared.so。
project(musiclibrary_android LANGUAGES C CXX)

# ========== 定位 MusicLibrary ==========
# CMAKE_CURRENT_SOURCE_DIR = NeteaseCloudMusic_PythonSDK/src/dart/android/src/main
# 需要 6 个 ../ 才到 flutter_netease_music 根 (NeteaseCloudMusic_PythonSDK 是同级)
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

# ========== libcurl 手动链 (替代 prefab) ==========
# libcurl.so / headers 来自 com.android.ndk.thirdparty:curl:7.85.0-beta-1 prefab AAR,
# 之前手动抽出放在 src/main/jniLibs/<abi>/libcurl.so + src/main/jniIncludes/curl/。
# plugin build.gradle.kts 已经 implementation 删了 curl prefab 依赖, jniLibs/
# 会被 AGP 自动打包进 APK。
#
# 注意:
#   - libcurl.so 依赖 libssl.so / libcrypto.so / libz.so, 但这些都是 Android
#     runtime 提供 (system image), 不需要打包
#   - 这里只 link libcurl.so 本体
set(PLUGIN_JNI_LIBS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/jniLibs")
set(PLUGIN_JNI_INCLUDES_DIR "${CMAKE_CURRENT_SOURCE_DIR}/jniIncludes")

message(STATUS "musiclibrary: PLUGIN_JNI_LIBS_DIR=${PLUGIN_JNI_LIBS_DIR}")
message(STATUS "musiclibrary: ANDROID_ABI=${ANDROID_ABI}")
message(STATUS "musiclibrary: looking for libcurl.so in ${PLUGIN_JNI_LIBS_DIR}/${ANDROID_ABI}/")
file(GLOB _test_curl_files "${PLUGIN_JNI_LIBS_DIR}/${ANDROID_ABI}/*")
message(STATUS "musiclibrary: glob result: ${_test_curl_files}")

# Android NDK toolchain 把 CMAKE_FIND_ROOT_PATH_MODE_LIBRARY 设成 ONLY,
# 导致 find_library 不搜 PATHS 传的绝对路径, 只搜 CMAKE_FIND_ROOT_PATH (=NDK)。
# 手动设 BOTH 让 find_library 也搜 PATHS (jniLibs/ 本地目录)。
# (2026-08-23 验证: 不设这个 find_library 会报 CURL_LIB-NOTFOUND, 但 file(GLOB)
#  能找到 —— 表面是 NDK toolchain 限了 find_library 的 search root)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY BOTH)

find_library(CURL_LIB
    NAMES curl
    PATHS "${PLUGIN_JNI_LIBS_DIR}/${ANDROID_ABI}"
    NO_DEFAULT_PATH
    REQUIRED)

message(STATUS "musiclibrary: libcurl at ${CURL_LIB}")

# ========== 子库选择 ==========
# Android 端只需要 ncm_music_api,关闭 engine / kugou 减少 APK 体积
set(MUSICLIBRARY_BUILD_ENGINE ON CACHE BOOL "" FORCE)
set(MUSICLIBRARY_BUILD_KUGOU  OFF CACHE BOOL "" FORCE)
set(MUSICLIBRARY_BUILD_NCM    ON  CACHE BOOL "" FORCE)

# ========== 引入 MusicLibrary 主 CMakeLists ==========
add_subdirectory(${MUSICLIBRARY_ROOT}
                 ${CMAKE_CURRENT_BINARY_DIR}/musiclibrary_build)

# ========== link libcurl 给 engine (Android 平台需要) ==========
# MusicLibrary 主 CMakeLists 里只在 UNIX AND NOT ANDROID / WIN32 链接 curl,
# 所以 Android 平台默认 curl 是断的。这里手动补上。
# 不能用 INTERFACE 库 (engine_config 是 INTERFACE,不能 link 一个非 target 的
# 东西作为 INTERFACE 依赖),所以直接 link 到 engine。
target_include_directories(engine PRIVATE "${PLUGIN_JNI_INCLUDES_DIR}")
target_link_libraries(engine PRIVATE ${CURL_LIB})

# ========== 输出 ncm_music_api.so 给 Flutter 工具链打包 ==========
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
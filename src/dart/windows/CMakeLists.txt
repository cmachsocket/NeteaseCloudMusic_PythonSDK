cmake_minimum_required(VERSION 3.14)
project(musiclibrary_windows LANGUAGES CXX)

# musiclibrary plugin: 纯 FFI 绑定,无 C/C++ 代码,只承载预编译 .dll。
#
# **native lib 来源**:
#   从 MusicLibrary GitHub Release 下载 windows-x64/windows-x86/windows-arm64
#   预编译 zip (脚本: ../fetch_multi_release.ps1 win64:<target> 等),然后 cp 到
#   native_libraries/<arch>/。App 仓库不再需要自己 fetch。
#
# **打包**:
#   通过 `musiclibrary_windows_bundled_libraries` + PARENT_SCOPE 把 .dll 路径列表
#   交给 Flutter 工具链 (`generated_plugins.cmake`)。app 根 CMake 自动 install 到
#   bundle 目录(exe 同级,Windows 进程找 .dll 用 PATH + exe 同目录,后者优先)。
#
# **arch 过滤**:
#   windows app 根 CMake 的 `install(FILES "${PLUGIN_BUNDLED_LIBRARIES}" ...)` 会
#   把列表里所有文件全装到同一 bundle。如果同时塞 x86 + x64 + arm64 的 .dll,
#   64位 exe 启动时会加载错的 arch 崩掉。所以这里按当前 build arch 筛:
#     - Visual Studio generator → CMAKE_VS_PLATFORM_NAME ("x64"/"Win32"/"ARM64")
#     - Ninja / Makefile        → CMAKE_SYSTEM_PROCESSOR ("AMD64"/"x86"/"ARM64")
#   没匹配 → 不 install (build 仍继续,只是 SDK 加载会失败 —— 比错装崩好)。

if(MSVC)
  set(_musiclibrary_arch "${CMAKE_VS_PLATFORM_NAME}")
else()
  set(_musiclibrary_arch "${CMAKE_SYSTEM_PROCESSOR}")
endif()

# 映射:VS 用 "x64"/"Win32"/"ARM64";system 用 "AMD64"/"x86"/"ARM64"
# 统一到我们的目录名 (x64 / x86 / arm64)
if(_musiclibrary_arch STREQUAL "x64" OR _musiclibrary_arch STREQUAL "AMD64")
  set(_musiclibrary_target_arch "x64")
elseif(_musiclibrary_arch STREQUAL "Win32" OR _musiclibrary_arch STREQUAL "x86"
       OR _musiclibrary_arch STREQUAL "i686" OR _musiclibrary_arch STREQUAL "i386")
  set(_musiclibrary_target_arch "x86")
elseif(_musiclibrary_arch STREQUAL "ARM64")
  set(_musiclibrary_target_arch "arm64")
else()
  set(_musiclibrary_target_arch "")
  message(WARNING "musiclibrary plugin: 未识别的 windows arch '${_musiclibrary_arch}',"
    "native .dll 不会被 install, SDK 加载会失败。支持 arch: x64, x86, arm64")
endif()

set(musiclibrary_native_libs "")
if(_musiclibrary_target_arch)
  file(GLOB musiclibrary_native_libs
    "${CMAKE_CURRENT_SOURCE_DIR}/native_libraries/${_musiclibrary_target_arch}/*.dll")
endif()

# Flutter 工具链 (`flutter_tools/lib/src/flutter_plugins.dart`) 对 FFI plugin
# 用 `${<pubspec.name>_bundled_libraries}` 引用,本 plugin pubspec.name == musiclibrary,
# 所以变量名固定是 `musiclibrary_bundled_libraries`(linux/windows 都一样)。
set(musiclibrary_bundled_libraries
  "${musiclibrary_native_libs}"
  PARENT_SCOPE
)
cmake_minimum_required(VERSION 3.10)
project(musiclibrary_linux LANGUAGES C)

# musiclibrary plugin: 纯 FFI 绑定,无 C/C++ 代码,只承载预编译 .so。
#
# **native lib 来源**:
#   从 MusicLibrary GitHub Release 下载 linux-x64 预编译 zip (脚本:
#   ../fetch_multi_release.sh linux64:<target>),然后 cp 到
#   native_libraries/。App 仓库不再需要自己 fetch。
#
# **打包**:通过 `*_bundled_libraries` + PARENT_SCOPE 把 .so 路径列表交给
# Flutter 工具链,app 根 CMake 自动 install 到 bundle/lib/。
#   (生成机制见 flutter_tools/lib/src/flutter_plugins.dart,生成的
#   generated_plugins.cmake 会追加这些路径到 PLUGIN_BUNDLED_LIBRARIES。)

# Flutter plugin 名硬约束 (生成机制依赖此命名)
set(PLUGIN_NAME "musiclibrary_plugin")

# 给 flutter 一个空库,否则 add_library(${PLUGIN_NAME} SHARED ...) 找不到 target。
# 用 INTERFACE library —— 不实际编译任何产物,只为了占位 plugin target。
add_library(${PLUGIN_NAME} INTERFACE)

# 收集所有 native .so (libengine.so + libncm_music_api.so)
set(musiclibrary_native_libs "")
file(GLOB musiclibrary_native_libs
  "${CMAKE_CURRENT_SOURCE_DIR}/native_libraries/*.so")

# Flutter 工具链 (`flutter_tools/lib/src/flutter_plugins.dart`) 对 FFI plugin
# 用 `${<pubspec.name>_bundled_libraries}` 引用,本 plugin pubspec.name == musiclibrary,
# 所以变量名固定是 `musiclibrary_bundled_libraries`(linux/windows 都一样)。
# PARENT_SCOPE 让 `add_subdirectory` 的 caller (generated_plugins.cmake) 能读到。
set(musiclibrary_bundled_libraries
  "${musiclibrary_native_libs}"
  PARENT_SCOPE
)
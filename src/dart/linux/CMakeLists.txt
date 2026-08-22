cmake_minimum_required(VERSION 3.10)
project(musiclibrary_linux LANGUAGES C)

# Stub: 实现在 linux/native_libraries/ 里 (由 app 根 CMakeLists.txt install)
# 这个 plugin 本身不编译任何代码,只为了让 flutter run 不报 "musiclibrary/linux 不存在"。
#
# FFI 加载的 .so (libengine.so + libncm_music_api.so) 在
# /home/cmach_socket/projects/flutter_netease_music/linux/native_libraries/

# 必须给 Flutter 一个空库,否则 add_library(musiclibrary_plugin SHARED ...) 找不到 target
add_library(musiclibrary_plugin SHARED dummy.c)

# 给 flutter generated_plugins.cmake 一个空 list
set(musiclibrary_bundled_libraries "")
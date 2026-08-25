#!/bin/bash
# 下载 MusicLibrary GitHub release 的预编译 .zip,解压到指定目录。
#
# 用法: ./fetch_multi_release.sh <arch>:<target-dir> [<arch>:<target-dir> ...]
# 例:
#   ./fetch_multi_release.sh win64:/tmp/win64 linux64:/tmp/linux64
#   ./fetch_multi_release.sh android64:/tmp/android64
#
# Android 走独立路径 (2026-08-25):解包后 copy 到 plugin 的 jniLibs,
# 因为 Android plugin 不再现场编译 MusicLibrary。
# TARGET 目录对 Android 来说只是个临时缓存,copy 完就删。

set -e

# 解析脚本自身位置,确定 plugin jniLibs 根
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYSDK_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
PLUGIN_JNI_ROOT="$PYSDK_ROOT/src/dart/android/src/main/jniLibs"

if [[ $# -eq 0 ]]; then
  echo "用法: $0 <arch>:<target-dir> [<arch>:<target-dir> ...]"
  echo
  echo "支持: win64, win32, winarm, linux64, linuxarm, macos64, macosarm, android64, android64x86, androidarm, androidx86"
  exit 1
fi

for arg in "$@"; do
  ARCH="${arg%%:*}"
  TARGET="${arg#*:}"

  case $ARCH in
    win64) PATTERN="windows-x64" ;;
    win32) PATTERN="windows-x86" ;;
    winarm) PATTERN="windows-arm64" ;;
    linux64) PATTERN="linux-x64" ;;
    linuxarm) PATTERN="linux-arm64" ;;
    macos64) PATTERN="macos-x64" ;;
    macosarm) PATTERN="macos-arm64" ;;
    # 2026-08-25 Android NDK 预编译 (跟着 MusicLibrary 的 build-android.sh 同步)
    android64)     PATTERN="android-arm64-v8a";   ABI_DIR="arm64-v8a"   ;;
    android64x86)  PATTERN="android-x86_64";      ABI_DIR="x86_64"      ;;
    androidarm)    PATTERN="android-armeabi-v7a"; ABI_DIR="armeabi-v7a" ;;
    androidx86)    PATTERN="android-x86";         ABI_DIR="x86"         ;;
    *) echo "未知架构: $ARCH"; continue ;;
  esac

  mkdir -p "$TARGET"

  # 获取 Release 资源列表并模糊匹配 zip 文件名
  RELEASE_JSON=$(curl -s -H "Accept: application/vnd.github.v3+json" "https://api.github.com/repos/2061360308/MusicLibrary/releases/latest")
  ZIP_NAME=$(echo "$RELEASE_JSON" | grep -oP '"name":\s*"'"$PATTERN"'.*?\.zip"' | head -n1 | sed 's/"name":\s*"//;s/"//')
  if [ -z "$ZIP_NAME" ]; then
    echo "未找到 $ARCH 对应的 zip 文件"
    continue
  fi
  ZIP_URL=$(echo "$RELEASE_JSON" | grep -A 10 "$ZIP_NAME" | grep 'browser_download_url' | head -n1 | sed 's/.*"browser_download_url":\s*"//;s/"//')
  if [ -z "$ZIP_URL" ]; then
    echo "未找到 $ARCH zip 的下载链接"
    continue
  fi

  echo "下载 $ZIP_NAME 到 $TARGET ..."
  curl -L -o "$TARGET/$ZIP_NAME" "$ZIP_URL"

  echo "解压 $ZIP_NAME ..."
  unzip -o "$TARGET/$ZIP_NAME" -d "$TARGET"

  # Android: copy .so 到 plugin jniLibs/<abi>/,然后清掉临时
  if [[ "$ARCH" == android* ]]; then
    DEST="${PLUGIN_JNI_ROOT}/${ABI_DIR}"
    mkdir -p "$DEST"
    echo "同步到 plugin jniLibs: $DEST"
    find "$TARGET/lib" -maxdepth 2 -type f -name "*.so" -exec cp -v {} "$DEST/" \;
    rm -rf "$TARGET"
    echo "已嵌入到 plugin jniLibs (不再现场编译 MusicLibrary)"
  else
    echo "清理 lib 目录 ..."
    find "$TARGET/lib" -type f ! -name '*.dll' ! -name '*.so' ! -name '*.dylib' -delete
  fi

  # 删除 zip 文件
  rm -f "$TARGET/$ZIP_NAME"
  echo "已删除 $ZIP_NAME"

  echo "$ARCH 完成"
  echo
done

echo "全部完成"
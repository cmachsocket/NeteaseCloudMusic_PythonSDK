#!/usr/bin/env python3
"""
sync-android-libs.py
把 MusicLibrary 预编译的 Android .so 同步到 plugin 的 jniLibs,替代原来的现场编译。

默认从 MusicLibrary/build/android/dist 拿 ABI 目录,
写到 ../src/dart/android/src/main/jniLibs/<abi>/。

用法:
    python3 scripts/sync-android-libs.py                    # 同步所有 dist 下的 ABI
    python3 scripts/sync-android-libs.py arm64-v8a x86_64    # 只同步指定 ABI
    python3 scripts/sync-android-libs.py --clean            # 先清空 jniLibs 再同步

依赖 MusicLibrary 跑过:
    cd ../MusicLibrary && ANDROID_NDK_HOME=... ./scripts/build-android.sh --all
"""
import argparse
import shutil
import sys
from pathlib import Path

# 路径:
#   scripts/sync-android-libs.py  →  NeteaseCloudMusic_PythonSDK/scripts/
#   3 个 ../ 到 NeteaseCloudMusic_PythonSDK/
#   跟 MusicLibrary 平级,所以 MusicLibrary 是 ../MusicLibrary
SCRIPT_DIR = Path(__file__).resolve().parent
PYSDK_DIR = SCRIPT_DIR.parent
MUSICLIBRARY_DIST = PYSDK_DIR.parent / "MusicLibrary" / "build" / "android" / "dist"
JNI_LIBS = PYSDK_DIR / "src" / "dart" / "android" / "src" / "main" / "jniLibs"

EXPECTED_LIBS = (
    "libncm_music_api.so",
    "libkugou_music_api.so",   # 2026-08-25: Android 改为一起 build (不关 KUGOU)
    "libengine.so",
    "libqjs.so",
    "libcurl.so",
    # 2026-08-25: libcurl.so 运行时依赖 libssl / libcrypto (NDK prefab openssl 包)。
    # prefab 模块拆成 modules/ssl/ + modules/crypto/ 两个, 但 aar 是同一个 openssl 包。
    "libssl.so",
    "libcrypto.so",
)
SUPPORTED_ABIS = ("arm64-v8a", "x86_64", "armeabi-v7a", "x86")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        "abis",
        nargs="*",
        help=f"要同步的 ABI(默认全部)。支持: {', '.join(SUPPORTED_ABIS)}",
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="同步前先清空 jniLibs",
    )
    parser.add_argument(
        "--source",
        type=Path,
        default=MUSICLIBRARY_DIST,
        help=f"MusicLibrary dist 路径(默认: {MUSICLIBRARY_DIST})",
    )
    args = parser.parse_args()

    if not args.source.exists():
        print(f"❌ 找不到 dist 目录: {args.source}", file=sys.stderr)
        print("   先跑:", file=sys.stderr)
        print("   cd ../MusicLibrary && ANDROID_NDK_HOME=... ./scripts/build-android.sh --all", file=sys.stderr)
        return 1

    abis = args.abis or sorted(p.name for p in args.source.iterdir() if p.is_dir())
    unknown = [a for a in abis if a not in SUPPORTED_ABIS]
    if unknown:
        print(f"❌ 不支持的 ABI: {unknown};支持: {SUPPORTED_ABIS}", file=sys.stderr)
        return 1

    if args.clean and JNI_LIBS.exists():
        print(f"[sync] cleaning {JNI_LIBS}")
        shutil.rmtree(JNI_LIBS)

    JNI_LIBS.mkdir(parents=True, exist_ok=True)

    for abi in abis:
        src_dir = args.source / abi
        if not src_dir.exists():
            print(f"⚠️  dist 缺 {abi}: {src_dir},跳过", file=sys.stderr)
            continue

        missing = [lib for lib in EXPECTED_LIBS if not (src_dir / lib).exists()]
        if missing:
            print(f"❌ {abi} 缺: {missing}", file=sys.stderr)
            return 1

        dst = JNI_LIBS / abi
        dst.mkdir(parents=True, exist_ok=True)
        for lib in EXPECTED_LIBS:
            shutil.copy2(src_dir / lib, dst / lib)
            print(f"  {abi}/{lib}")

    print(f"\n✅ 同步完成 → {JNI_LIBS}")
    print(f"   ABI: {abis}")
    print(f"   文件: {len(abis) * len(EXPECTED_LIBS)} 个 .so")
    return 0


if __name__ == "__main__":
    sys.exit(main())
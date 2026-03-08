# flutter_music_library

Flutter FFI plugin bindings for the prebuilt MusicLibrary native binaries.

## Current plugin scope

- Implemented as a Flutter plugin (`ffiPlugin`) for `windows`.
- Uses prebuilt native binaries, no C/C++ source compilation in this package.
- Binaries are bundled via `windows/CMakeLists.txt`.

## Prebuilt binaries location

Windows DLLs are stored in:

`windows/third_party/musiclib`

Expected files:

- `engine.dll`
- `kugou_music_api.dll`
- `ncm_music_api.dll`
- `libcurl.dll`
- `zlib1.dll`

## How loading works

At runtime, Dart FFI loads by library name (for example `engine.dll`).
Flutter tooling reads `windows/CMakeLists.txt` and copies bundled DLLs into the app output directory, so dependencies are discoverable.

## Install in another Flutter app

```yaml
dependencies:
  flutter_music_library:
    git:
      url: https://github.com/<you>/<repo>.git
      path: flutter_music_library
```

Or local path:

```yaml
dependencies:
  flutter_music_library:
    path: ../flutter_music_library
```

## Usage

```dart
import 'package:flutter_music_library/music_library.dart';

final kugou = KuGouMusicApi(
  env: KugouProcessEnv(platform: KugouPlatform.lite),
);
final response = kugou.albumDetail('10729818');
print(response.status);
kugou.dispose();
```

## Notes

- Call `dispose()` explicitly when done.
- If you need Android/iOS/macOS/Linux publishing, add platform binaries and platform build packaging rules similarly.

import 'dart:ffi';

import 'package:ffi/ffi.dart';

import 'common.dart';
import 'core.dart';

class NeteaseCloudMusicApi {
  NeteaseCloudMusicApi({
    NcmProcessEnv? env,
    String? libraryDir,
  })  : _engine = EngineBindings(libraryDir: libraryDir),
        _bindings = NcmBindings(libraryDir: libraryDir),
        _env = env ?? NcmProcessEnv() {
    _engine.ensureInitialized();

    _contextManager = NcmContextManager(bindings: _bindings);
    _nativeEnv = _env.toNative();
    _contextManager.init(_nativeEnv.pointer);
    _ctx = _contextManager.takeContext();
  }

  final EngineBindings _engine;
  final NcmBindings _bindings;
  late final NcmContextManager _contextManager;
  final NcmProcessEnv _env;
  late final NcmEnvHandle _nativeEnv;
  late final Pointer<JSContext> _ctx;

  bool _destroyed = false;

  MusicResponse request(
    String path, {
    String cookie = '',
    NcmProcessEnv? env,
    Map<String, dynamic>? query,
  }) {
    final useEnv = env ?? _env;
    final envHandle = useEnv == _env ? _nativeEnv : useEnv.toNative();

    final pathPtr = path.toNativeUtf8();
    final cookiePtr = cookie.toNativeUtf8();
    final paramsPtr = encodeQuery(query ?? const <String, dynamic>{}).toNativeUtf8();

    try {
      final responsePtr = _bindings.request(
        _ctx,
        pathPtr,
        cookiePtr,
        paramsPtr,
        envHandle.pointer,
      );
      return parseFfiResponse(responsePtr, _engine);
    } finally {
      calloc.free(pathPtr);
      calloc.free(cookiePtr);
      calloc.free(paramsPtr);
      if (!identical(envHandle, _nativeEnv)) {
        envHandle.dispose();
      }
    }
  }

  String generateRandomCnIp() {
    return _bindings.generateRandomCnIp(_ctx, _engine);
  }

  String generateAnonymousToken() {
    return _bindings.generateAnonymousToken(_ctx, _engine);
  }

  MusicResponse topSong({String cookie = '', NcmProcessEnv? env, int? type}) {
    return request(
      '/top/song',
      cookie: cookie,
      env: env,
      query: <String, dynamic>{'type': type},
    );
  }

  void dispose() {
    if (_destroyed) {
      return;
    }

    _engine.destroyContext(_ctx);
    _nativeEnv.dispose();
    _contextManager.destroy();
    _engine.dispose();
    _destroyed = true;
  }
}

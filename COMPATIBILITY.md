# Compatibility Guide

This document explains the Android versions and CPU architectures the app
supports, how runtime binaries are resolved, and what to do if you need a
custom build for a non-default ABI.

---

## Android Version Support

| Android version | API | Support |
| --- | --- | --- |
| Android 15 | 35 | Full |
| Android 14 | 34 | Full |
| Android 13 | 33 | Full |
| Android 12 / 12L | 31-32 | Full |
| Android 11 | 30 | Full |
| Android 10 | 29 | Full |
| Android 9 | 28 | Full |
| Android 8.0 / 8.1 | 26-27 | Minimum supported |
| Android 7.x and below | 25 and below | Not supported |

---

## CPU Architecture Support

Our runtimes are split into two categories:

### 1. Python & QuickJS Runtimes
- **Support**: **Multi-architecture (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`)**
- **Details**: Since the app utilizes the official `youtubedl-android` dependency wrapper, CPython 3.12 and QuickJS standard libraries/executables (`libpython.so`, `libpython.zip.so`, and `libqjs.so`) are pre-compiled and native-bundled for all architectures out-of-the-box. This ensures zero tracebacks or environment crashes across emulators and 32-bit devices.

### 2. FFmpeg Runtime
- **Primary Shipped Target**: `arm64-v8a`
- **Details**: The custom-compiled FFmpeg binary is fetched automatically from our [Packages Repository](https://github.com/iam-sandipmaity/video-downloader-packages) during build time. For non-arm64 devices, custom builds are required or alternate ABIs can be fetched from the package builder release history.

| Component | `arm64-v8a` | `armeabi-v7a` | `x86_64` | `x86` |
| --- | --- | --- | --- | --- |
| **Python / QuickJS** | Yes (Built-in) | Yes (Built-in) | Yes (Built-in) | Yes (Built-in) |
| **FFmpeg** | Yes (Default) | No (Custom Build) | No (Custom Build) | No (Custom Build) |

---

## How Runtime Resolution Works

### yt-dlp & Python
`yt-dlp` runs using the Python bytecode interpreter provided by the `youtubedl-android` library. On app startup:
- The library initializes Python and sets up standard module mappings.
- The app can safely update the `yt-dlp` script dynamically in-app through the **Updates** screen.

### FFmpeg
FFmpeg is resolved in this order:
1. Local app-packaged native binary (`libffmpeg.so` located inside the APK's `jniLibs/<abi>/` directory)
2. In-app downloaded overlay/updates runtime package
3. Bundled legacy executables (e.g. `libffmpeg_exec.so`)
4. Copied executable fallback from `assets/ffmpeg/<abi>/ffmpeg`

---

## Public Download Location

Default public save root:

```text
Download/LocalDownloader/
```

The root folder and media-specific folders can also be changed from Settings.

On older Android versions with storage restrictions or denied legacy storage
permission, the app may fall back to an app-owned external directory.

---

## Building For Another ABI

If you need support for a custom CPU architecture (such as an Intel emulator `x86_64` or older `armeabi-v7a` phone):

1. Compile the custom `libffmpeg.so` for your target ABI using the compiler scripts in the [Packages Builder Repository](https://github.com/iam-sandipmaity/video-downloader-packages).
2. Place the compiled `libffmpeg.so` under `app/src/main/jniLibs/<abi>/libffmpeg.so`.
3. Build the APK normally. The Gradle builder will pack the native library into the APK.

Example layout for `x86_64`:
```text
app/src/main/jniLibs/x86_64/libffmpeg.so
```

Example layout for `armeabi-v7a`:
```text
app/src/main/jniLibs/armeabi-v7a/libffmpeg.so
```

If you are packaging a fuller embedded runtime instead of a single fallback
executable, the app can also use `libffmpeg.so` and related runtime-support
artifacts when present.

---

## How To Check Your ABI

### Option 1: device info app

Use any device-info style app and look for `CPU ABI`.

### Option 2: ADB

```bash
adb shell getprop ro.product.cpu.abi
```

### Option 3: device specs

If your device uses a recent Snapdragon, Dimensity, Tensor, or Exynos chipset,
it is almost certainly `arm64-v8a`.

---

## Android Behavior Notes

Some Android versions require specific platform handling that the app already
implements:

| Android | Requirement | Status |
| --- | --- | --- |
| 14+ | declared foreground service type | Done |
| 13+ | notification runtime permission | Done |
| 10+ | public download/storage handling adjustments | Done |
| 8-9 | legacy external storage permission path | Done |

---

## Build Advice

If you only want the standard app:

```bash
gradle :app:assembleDebug
```

If you want to test a custom ABI path, verify:

- the fallback asset exists for that ABI
- the packaged native file is present
- the runtime is executable when copied
- the app logs show the expected runtime path being selected

---

## Practical Recommendation

For most users and releases, stay with:

- Android 8+
- `arm64-v8a`
- default public download folders

Only use custom ABI builds when you specifically need emulator, Intel, or older
32-bit ARM support.

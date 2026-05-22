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

Default shipped target:

- `arm64-v8a`

This covers the large majority of modern Android phones and tablets.

Other ABIs are possible through custom builds, but are not packaged by default
in the normal release flow.

| ABI | Typical use | Shipped by default |
| --- | --- | --- |
| `arm64-v8a` | modern phones and tablets | Yes |
| `armeabi-v7a` | older 32-bit ARM devices | No |
| `x86_64` | emulators and some Intel devices | No |
| `x86` | older emulators | No |

---

## How Runtime Resolution Works

### yt-dlp

`yt-dlp` runs through the embedded `youtubedl-android` runtime and can be
updated in-app through the Updates screen.

### FFmpeg

FFmpeg is resolved in this order:

1. managed overlay package downloaded by the app
2. embedded runtime package if available
3. bundled `libffmpeg_exec.so`
4. copied executable fallback from `assets/ffmpeg/<abi>/ffmpeg`

This layered approach exists because device/runtime behavior differs across
vendors, Android versions, and ABI packaging styles.

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

If you need support for an ABI that is not packaged by default:

1. clone the repository
2. add compatible FFmpeg artifacts for that ABI
3. place the raw fallback binary under `assets/ffmpeg/<abi>/ffmpeg`
4. place the packaged native fallback under `jniLibs/<abi>/libffmpeg_exec.so`
5. build the APK normally

Example layout for `x86_64`:

```text
app/src/main/assets/ffmpeg/x86_64/ffmpeg
app/src/main/jniLibs/x86_64/libffmpeg_exec.so
```

Example layout for `armeabi-v7a`:

```text
app/src/main/assets/ffmpeg/armeabi-v7a/ffmpeg
app/src/main/jniLibs/armeabi-v7a/libffmpeg_exec.so
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

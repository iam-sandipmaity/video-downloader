# Compatibility Guide

This document explains which Android versions and CPU architectures are supported, how the binary system works internally, and how to build a custom APK for unsupported devices.

---

## Android Version Support

| Android Version | API Level | Released | Support |
|---|---|---|---|
| Android 15 | 35 | 2024 | ✅ Full (target SDK) |
| Android 14 | 34 | 2023 | ✅ Full |
| Android 13 | 33 | 2022 | ✅ Full |
| Android 12 / 12L | 31–32 | 2021–2022 | ✅ Full |
| Android 11 | 30 | 2020 | ✅ Full |
| Android 10 | 29 | 2019 | ✅ Full |
| Android 9 (Pie) | 28 | 2018 | ✅ Full |
| Android 8.0 / 8.1 (Oreo) | 26–27 | 2017 | ✅ Minimum supported |
| Android 7.x (Nougat) | 24–25 | 2016 | ❌ Not supported |
| Android 6 and below | ≤ 23 | ≤ 2015 | ❌ Not supported |

> **Coverage:** ~90% of active Android devices worldwide.

---

## CPU Architecture Support

Every Android device has a CPU architecture. The app packages pre-built binaries for specific architectures:

| ABI | Architecture | Real devices | Shipped by default |
|---|---|---|---|
| **arm64-v8a** | 64-bit ARM | All modern phones & tablets (Snapdragon, Dimensity, Exynos, …) sold since ~2015 | ✅ Yes |
| **armeabi-v7a** | 32-bit ARM | Old budget phones (2012–2018) | ⚠️ No (< 1% of devices) |
| **x86_64** | 64-bit Intel | Android emulators, Intel Chromebooks, rare Intel tablets | ⚠️ No (dev use only) |
| **x86** | 32-bit Intel | Emulators only — no real phones | ❌ Dead |

### How to check your CPU architecture

**Option 1 — Settings:**
> Settings → About phone → Processor (or CPU info)
> - Contains "ARM64" / "Cortex-A" / "Kryo" / "Exynos" → you are **arm64-v8a** ✅

**Option 2 — ADB (developer):**
```bash
adb shell getprop ro.product.cpu.abi
```

**Option 3 — App:**
Install any "Device Info" app from the Play Store and look for "CPU ABI".

---

## How the Binary System Works

When you open the app:

- `yt-dlp` runs through the embedded `youtubedl-android` runtime
- `ffmpeg` follows the bundled binary resolution chain below

```
Step 1: Check nativeLibraryDir
        (/data/app/<package>/lib/<abi>/)
        ↓ found? → use it directly (fastest, always executable)
        ↓ not found?

Step 2: Check assets/<tool>/<abi>/<binary>
        Matches device's Build.SUPPORTED_ABIS list in order
        ↓ found? → copy to app internal storage → chmod +x → use it
        ↓ not found?

Step 3: FAIL with clear error message listing what was searched
```

The `nativeLibraryDir` path (Step 1) is populated from `jniLibs/` during APK installation — Android extracts them automatically. The `assets/` fallback (Step 2) is there for cases where `.so` extraction fails or is blocked.

---

## Building for a Non-Standard Architecture

If your device uses **x86_64**, **armeabi-v7a**, or another ABI not bundled in the default release, follow these steps to build your own APK.

### Step 1 — Fork / clone the repository

```bash
git clone https://github.com/your-username/video-downloader
cd video-downloader
```

### Step 2 — Obtain compatible binaries

You need an `ffmpeg` binary compiled for your target ABI.

#### ffmpeg

| ABI | Download source |
|---|---|
| x86_64 | [ffmpeg-android-maker](https://github.com/Javernaut/ffmpeg-android-maker) or [termux packages](https://packages.termux.dev/apt/termux-main/binary-x86_64/) |
| armeabi-v7a | Same sources, pick `arm` flavour |

Make sure the binary is **statically linked** (no shared lib dependencies) and has **execute permission**.

### Step 3 — Place the binaries

Depending on your ABI, place files in the following paths:

#### For x86_64

```
app/src/main/assets/ffmpeg/x86_64/ffmpeg

app/src/main/jniLibs/x86_64/libffmpeg_exec.so   ← rename ffmpeg → libffmpeg_exec.so
```

#### For armeabi-v7a

```
app/src/main/assets/ffmpeg/armeabi-v7a/ffmpeg

app/src/main/jniLibs/armeabi-v7a/libffmpeg_exec.so
```

---

## Building with GitHub Actions (Recommended)

The easiest way to get a compiled APK without setting up Android Studio locally:

### Step 1 — Fork the repo on GitHub

Click **Fork** at the top-right of the repository page.

### Step 2 — Add your binary files

Upload your ABI-specific binaries to the correct paths in your fork (see Step 3 above).  
You can do this via the GitHub web UI: navigate to the folder → "Add file" → "Upload files".

### Step 3 — Enable GitHub Actions

1. Go to your forked repository
2. Click the **Actions** tab
3. Click **"I understand my workflows, go ahead and enable them"**

### Step 4 — Trigger a build

Either:
- Push any commit to the repository, **or**
- Go to **Actions → Android Build → Run workflow** (manual trigger)

### Step 5 — Download your APK

1. Go to **Actions** tab
2. Click the latest **Android Build** workflow run
3. Scroll to **Artifacts** at the bottom
4. Download **`app-debug-apk`**

The APK inside is compiled with your binaries and will work on your device.

---

## Version-specific Runtime Behaviour

Some Android versions need special handling that is already implemented in the app:

| Android | Requirement | Status |
|---|---|---|
| 14+ (API 34+) | `foregroundServiceType` must be declared | ✅ Done |
| 13+ (API 33+) | `POST_NOTIFICATIONS` runtime permission | ✅ Done |
| 10+ (API 29+) | `requestLegacyExternalStorage` for public Downloads | ✅ Done |
| 8–9 (API 26–28) | `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion=28` | ✅ Done |

---

## Download Folder

Files are saved to:

```
/sdcard/Download/LocalDownloader/
```

This folder is visible in the system **Files** app and any file manager.  
On Android 8–9 with storage permission denied, the app falls back to:

```
/sdcard/Android/data/com.localdownloader/files/Download/
```

(Visible in Files app under "Internal storage → Android → data → …")

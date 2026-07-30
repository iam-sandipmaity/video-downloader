# Lessons Learned & Historical Context

This document captures historical pivots, key resolutions, and optimization metrics to prevent regression errors on future development tasks.

---

## 1. yt-dlp Wrapper Architecture Pivot
- **The Issue**: Early versions attempted to dynamically package a custom-compiled Python 3.11.9 static runtime and QuickJS engine directly via executable shell calls. This introduced severe startup tracebacks, dynamic linker errors on Android 10+ namespace rules, and CPU architecture mismatches.
- **The Solution**: Replaced the custom CPython wrapper pipeline with the official dependency:
  `io.github.junkfood02.youtubedl-android:library:0.18.1`
  This wrapper encapsulates stable native binaries, resolving execution permission issues and restoring stable packaging.

## 2. ProGuard Obfuscation Safeguards
- **The Issue**: Minified nightly releases crashed on startup with `ClassNotFoundException` due to package shrinking.
- **The Solution**: Explicit rules must be enforced in the obfuscation files to prevent shrinking of core dependency namespaces. Keep rules must protect:
  1. `com.yausername.youtubedl_android` and `com.yausername.youtubedl` (preventing obfuscation package name `rxo` crashes).
  2. `org.apache.commons.compress` and `org.tukaani.xz` (resolving compression and extraction errors on package bundles).

## 3. UI Thread Recomposition Throttle
- **The Issue**: Fast fragment download updates (such as high-speed HLS downloads) flooded the main threat with progress notifications, causing severe interface freezes and audio/video sync issues in the UI.
- **The Solution**: Added a **250ms time throttle** inside the progress update pipeline of `DownloadEngine` / `DownloadWorker`. This bundles rapid events and ensures Jetpack Compose does not trigger excessive rendering updates.

## 4. Room Migration Caveats
- **The Issue**: Database upgrade schema mismatches caused app crashes on SQLite upgrades.
- **The Solution**: Specify column constraints precisely. For instance, when adding columns like `is_in_vault` in Migration 4→5, specify them as `NOT NULL DEFAULT 0` rather than allowing null values, and match the schema mapping in `@ColumnInfo(defaultValue = "0")` in the data entity model.

## 5. MediaStore Security Leakage
- **The Issue**: Moving downloads into the private vault kept metadata and cache files visible inside external gallery apps and Google Photos due to indexing.
- **The Solution**: Programmatically delete moved files from the system `MediaStore` cache when placing them in private storage. Conversely, re-insert them into the public media registry when moving them back out of the vault.

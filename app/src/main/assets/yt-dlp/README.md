# yt-dlp asset fallback placement

Primary packaging (recommended):

- `app/src/main/jniLibs/arm64-v8a/libyt_dlp.so`
- `app/src/main/jniLibs/x86_64/libyt_dlp.so`

Asset fallback (used only when no native lib is found):

- `yt-dlp/arm64-v8a/yt-dlp`
- `yt-dlp/x86_64/yt-dlp`
- optional: `yt-dlp/armeabi-v7a/yt-dlp`

The app now prefers `nativeLibraryDir` binaries (more reliable on Android versions that block execute from `files/`), then falls back to copying from assets.

# FFmpeg asset fallback placement

Primary packaging (recommended):

- `app/src/main/jniLibs/arm64-v8a/libffmpeg_exec.so`
- `app/src/main/jniLibs/x86_64/libffmpeg_exec.so`

Asset fallback (used only when no native lib is found):

- `ffmpeg/arm64-v8a/ffmpeg`
- `ffmpeg/x86_64/ffmpeg`
- optional: `ffmpeg/armeabi-v7a/ffmpeg`

The app now prefers `nativeLibraryDir` binaries (more reliable on Android versions that block execute from `files/`), then falls back to copying from assets.

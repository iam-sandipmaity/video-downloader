# Standalone Player Module

This module packages the in-app player as a separate Android application without changing the existing downloader app.

Build it with:

```bash
gradle :playerapp:assembleDebug
```

Key pieces:

- `playerapp/src/main/java/com/localplayer/MainActivity.kt` handles launcher and external open intents
- `playerapp/src/main/java/com/localplayer/ui/StandalonePlayerApp.kt` provides the simple open-media flow
- `playerapp/src/main/java/com/localplayer/ui/screens/PlayerScreen.kt` contains the copied player UI and gestures
- `playerapp/src/main/java/com/localplayer/viewmodel/PlayerViewModel.kt` contains standalone playback state and Media3 control logic

When you are ready to move this into its own repository, `playerapp/` can be used as the starting point.

# Product Roadmap & Issue Backlog

This document outlines upcoming feature updates, active issues, and architectural improvements planned for the Local Video Downloader.

---

## 🐛 Known Issues & Backlog

1. **Troubleshooting Logs Enrichment**:
   - The diagnostic report captures basic system details, but lacks dynamic memory stats (such as JVM heap allocations or device storage categories).
2. **Video Player Rotation Layout Shifts**:
   - Pinch-to-zoom coordinates can lose tracking references during rapid device screen rotations.
3. **Queue Sorting Performance**:
   - Heavy item list sorting updates (50+ items) in the queue screen can occasionally block Compose list rendering.

---

## 🗺️ Feature Roadmap

### Phase 1: Vault Biometrics Integration
- Add Fingerprint/Face Unlock support via Android Biometric Prompt API.
- Keep PIN credentials as secure fallback when hardware keys are absent or fail.

### Phase 2: Dynamic Subtitle Import & Translation
- Enable manual imports of external `.srt` or `.vtt` file paths directly into local download logs.
- Integrate open-source translation pipelines to translate sidecar files on the fly.

### Phase 3: Advanced Playlist & Scheduled Downloads
- Add options to queue download tasks with delayed execution triggers.
- Support recurring checks for designated channel URLs or playlists.

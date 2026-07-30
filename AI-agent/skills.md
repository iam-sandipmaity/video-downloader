# Agent Operational Skills & Tooling

Use this guide to run compilations, analyze tasks, and verify the codebase layout when performing modifications.

---

## Gradle Build Commands

Always run validation tasks from the root directory of the repository:

### 1. Compile and Build Debug Package
Checks Kotlin compilation, Hilt injections, and compiles the debug APK:
```bash
./gradlew assembleDebug
```

### 2. Run Verification Checks
Executes all local unit tests:
```bash
./gradlew test
```

### 3. Lint Validation
Checks code style rules (ktlint / lint):
```bash
./gradlew lint
```

### 4. Clean Build Cache
Cleans compiled files, build directories, and cache folders if compilation errors persist:
```bash
./gradlew clean
```

---

## Log Analysis & Diagnostics

When debugging runtime errors on an emulator or real device:

### 1. ADB Logging
Filter output logs for this application package:
```bash
adb logcat -s "DownloadWorker" "FormatViewModel" "PlayerViewModel" "LocalDownloader"
```

### 2. Sanity Checks on Room Database
Ensure that changes to Room entities do not break local schema files. If modified, verify generated schema files under:
`app/schemas/com.localdownloader.data.persistence.AppDatabase/`

---

## CI Build Simulation

The GitHub Action triggers builds based on environment flags defined in `.github/workflows/master.yml`. To test build environments:
- Modify `gradle.properties` version configurations.
- Toggle local build parameters using environment properties on your testing terminal if needed.

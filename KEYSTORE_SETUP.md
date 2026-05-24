# Keystore Setup

Use this guide to create, wire, verify, and protect the signing keystores used by this project.

This repo supports two signing identities:

- `internal-debug.jks`
  Used for stable debug builds.
- `release.jks`
  Used for release APKs and GitHub release workflows.

Do not commit keystores or live passwords to Git. This repo already ignores:

- `keystore.properties`
- `*.jks`
- `*.keystore`

## What This Project Expects

Local Gradle signing reads from:

- environment variables
- Gradle properties
- `keystore.properties`

Relevant keys in [app/build.gradle.kts](app/build.gradle.kts):

- `INTERNAL_DEBUG_STORE_FILE`
- `INTERNAL_DEBUG_STORE_PASSWORD`
- `INTERNAL_DEBUG_KEY_ALIAS`
- `INTERNAL_DEBUG_KEY_PASSWORD`
- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

GitHub Actions also expects these repository secrets:

- `INTERNAL_DEBUG_KEYSTORE_BASE64`
- `INTERNAL_DEBUG_STORE_PASSWORD`
- `INTERNAL_DEBUG_KEY_ALIAS`
- `INTERNAL_DEBUG_KEY_PASSWORD`
- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

## 1. Create A Local Signing Folder

Create a private folder that stays outside version control.

Recommended layout:

```text
.signing/
  internal-debug.jks
  release.jks
```

PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path .signing
```

## 2. Generate The Internal Debug Keystore

Use `keytool` from JDK 17 or newer.

PowerShell:

```powershell
keytool -genkeypair `
  -v `
  -keystore .signing/internal-debug.jks `
  -alias internal-debug `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000 `
  -storetype PKCS12 `
  -dname "CN=Video Downloader Internal Debug, OU=Android, O=Video Downloader, L=Kolkata, ST=West Bengal, C=IN"
```

You will be prompted for:

- keystore password
- key password

Recommended:

- keep the alias simple: `internal-debug`
- use a strong unique password
- you may use the same store/key password, but separate passwords are also fine

## 3. Generate The Release Keystore

PowerShell:

```powershell
keytool -genkeypair `
  -v `
  -keystore .signing/release.jks `
  -alias release `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000 `
  -storetype PKCS12 `
  -dname "CN=Video Downloader Release, OU=Android, O=Video Downloader, L=Kolkata, ST=West Bengal, C=IN"
```

Important:

- this release keystore becomes your update identity
- if you lose it, future app updates will not install over already released builds
- back it up immediately after creation

## 4. Create `keystore.properties`

Create [keystore.properties](keystore.properties) in the repo root.

Template:

```properties
INTERNAL_DEBUG_STORE_FILE=.signing/internal-debug.jks
INTERNAL_DEBUG_STORE_PASSWORD=replace-with-debug-store-password
INTERNAL_DEBUG_KEY_ALIAS=internal-debug
INTERNAL_DEBUG_KEY_PASSWORD=replace-with-debug-key-password

RELEASE_STORE_FILE=.signing/release.jks
RELEASE_STORE_PASSWORD=replace-with-release-store-password
RELEASE_KEY_ALIAS=release
RELEASE_KEY_PASSWORD=replace-with-release-key-password
```

Notes:

- use real values only on your machine
- do not paste this file into issues, chats, PRs, or commits
- relative paths like `.signing/release.jks` work fine for local builds

## 5. Verify The Keystores

Verify both files before trying a build.

Internal debug:

```powershell
keytool -list -v -keystore .signing/internal-debug.jks
```

Release:

```powershell
keytool -list -v -keystore .signing/release.jks
```

Check that:

- the alias exists
- the certificate is valid
- the SHA-256 fingerprint is visible

## 6. Local Build Check

If Gradle is installed locally:

```powershell
gradle :app:assembleDebug
gradle :app:assembleRelease
```

Expected behavior:

- debug build uses `internalDebugStable`
- release build uses `releaseStable`

If signing values are missing, Gradle falls back to unsigned/default behavior for that build type where applicable.

## 7. Convert Keystores To GitHub Secrets

GitHub Actions does not upload `.jks` files directly. It expects base64 text.

PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path '.signing/release.jks')))
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path '.signing/internal-debug.jks')))
```

Paste each output as one line into GitHub repository secrets.

## 8. Add GitHub Repository Secrets

Repository path:

1. Open the GitHub repo
2. Go to `Settings`
3. Open `Secrets and variables`
4. Click `Actions`
5. Add these secrets

Debug secrets:

- `INTERNAL_DEBUG_KEYSTORE_BASE64`
- `INTERNAL_DEBUG_STORE_PASSWORD`
- `INTERNAL_DEBUG_KEY_ALIAS`
- `INTERNAL_DEBUG_KEY_PASSWORD`

Release secrets:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Mapping:

- `INTERNAL_DEBUG_KEYSTORE_BASE64`
  Base64 of `.signing/internal-debug.jks`
- `RELEASE_KEYSTORE_BASE64`
  Base64 of `.signing/release.jks`
- all password and alias secrets
  Match the values from `keystore.properties`

## 9. Understand Which Workflow Uses Which Secrets

In [.github/workflows/android-build.yml](.github/workflows/android-build.yml):

- `build`
  Uses debug signing for `:app:assembleDebug` when debug secrets exist
- `release-nightly`
  Publishes the debug APK artifact from the `build` job
- `release-main`
  Requires release signing secrets and builds `:app:assembleRelease`
- `release-tag`
  Requires release signing secrets and builds `:app:assembleRelease`

In short:

- debug signing affects nightly/debug builds
- release signing affects stable release APKs

## 10. Common Failure Cases

### `Cannot recover key`

Usually means one of these is wrong:

- alias
- store password
- key password
- keystore base64 copied from the wrong file

Most common mistake:

- using `release.jks` values in `INTERNAL_DEBUG_*`
- or using `internal-debug.jks` values in `RELEASE_*`

### `Missing release signing secrets`

The GitHub workflow did not receive:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

### Debug build fails but release works

Usually the `INTERNAL_DEBUG_*` secrets or local debug entries are wrong.

### Release build fails but debug works

Usually the `RELEASE_*` secrets or local release entries are wrong.

## 11. Backup Rules

Back up these three things together:

- `.signing/release.jks`
- `.signing/internal-debug.jks`
- `keystore.properties`

Best practice:

- keep one offline backup
- keep one encrypted backup in a separate safe location
- never rely only on the machine that generated the keystore

## 12. If A Keystore Is Exposed

Treat it as compromised if you ever:

- commit it
- upload it publicly
- paste its full base64 in a public issue/chat
- share the passwords with the wrong people

What to do:

1. generate a new keystore
2. update `keystore.properties`
3. replace all related GitHub secrets
4. use the new keystore for future unreleased builds

Important:

- if the exposed keystore was already used for public release builds, rotating it will break upgrade continuity for those already-installed releases
- only rotate a public release keystore after understanding that tradeoff

## 13. Safe Release Checklist

Before pushing release-related changes:

- verify `git status` does not show `keystore.properties`
- verify `.signing/` files are not staged
- verify no passwords are present in docs or screenshots
- confirm release secrets exist in GitHub Actions
- confirm the release keystore is backed up

## 14. Minimal Example Summary

If you just want the shortest working path:

1. create `.signing/`
2. generate `internal-debug.jks`
3. generate `release.jks`
4. create `keystore.properties`
5. verify with `keytool -list -v`
6. base64 both `.jks` files
7. add all 8 GitHub secrets
8. run a debug build
9. run a release build
10. back everything up

## Related Docs

- [README.md](README.md)
- [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [.github/workflows/android-build.yml](.github/workflows/android-build.yml)

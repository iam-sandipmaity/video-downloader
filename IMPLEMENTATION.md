# YouTube Auth Bundle Flow

## Goal

Add a practical YouTube authentication fallback for long-form downloads that fail with `HTTP Error 403: Forbidden`.

The current preferred flow is:

- user runs the desktop helper on a computer
- helper captures cookies plus PO token from the user's own YouTube session
- helper writes one `auth_bundle.json`
- user imports that single file in the Android app
- the app stores cookies/token locally and uses them only for YouTube auth fallback retries

## Why this approach

Android cannot realistically mirror desktop `--cookies-from-browser` behavior in a reliable, portable way.
The fastest path is:

1. capture auth data where the browser session already exists
2. package it into one portable bundle
3. keep the Android downloader local-first
4. use auth-specific retry only for blocked YouTube downloads

Manual cookies import and manual PO token entry still remain as an advanced fallback inside the app.

## Scope

## Desktop helper flow

Add a desktop helper under `tools/youtube-auth-helper/`:

- launches a persistent Chromium profile with Playwright
- lets the user log into YouTube manually
- listens for `youtubei/v1/player` requests
- captures `serviceIntegrityDimensions.poToken`
- exports cookies in Netscape format
- writes:
  - `cookies.txt`
  - `po_token.txt`
  - `auth_bundle.json`

The important part is that `auth_bundle.json` now embeds:

- `cookiesContent`
- `poToken`
- `poTokenClientHint`

That gives the Android app a true one-file import workflow without embedding Node.js or browser automation inside the APK.

### Settings

Add to app settings:

- `youtubeAuthEnabled`
- `youtubeCookiesPath`
- `youtubePoToken`

Expose them in the Settings screen with:

- a recommended `Import auth bundle` flow
- status card showing whether auth fallback is ready
- toggle for enabling YouTube auth fallback
- manual cookies import button
- manual PO token field
- token client hint dropdown for advanced/manual setup

### Storage

When the user imports `auth_bundle.json`:

- decode the JSON
- copy `cookiesContent` into app-private storage under `files/auth/youtube-cookies.txt`
- save the copied absolute path plus token/client hint in settings
- enable YouTube auth fallback automatically

This avoids depending on temporary cache paths or content URIs at download time.

### Download options

Pass through per-download auth fields:

- `youtubeAuthEnabled`
- `youtubeCookiesPath`
- `youtubePoToken`

This ensures resumed jobs keep the same auth state that existed when they were queued.

### yt-dlp integration

Only for YouTube auth retries:

- add `--cookies <path>` if the file exists
- use auth extractor args matching the stored client hint:
  - `youtube:player_client=default,web;po_token=web.gvs+<TOKEN>`
  - or `youtube:player_client=default,mweb;po_token=mweb.gvs+<TOKEN>`

The token is applied only during the dedicated auth fallback path, not during every normal download attempt.

### Retry order

Current order becomes:

1. selected format
2. analyzed extractor-args fallback
3. mp4 adaptive fallback
4. YouTube auth fallback using imported cookies + PO token
5. YouTube safe mode muxed fallback

## Notes

- This flow does **not** auto-generate tokens on Android.
- This flow does **not** parse cookies from installed browsers on Android.
- The desktop helper uses the user's own session and keeps the actual download local on-device.
- This MVP is expected to improve long-form YouTube reliability, but some formats may still remain unavailable if the token/session pair is invalid or expired.

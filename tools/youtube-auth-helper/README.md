# YouTube Auth Helper

Desktop helper for generating the Android app's one-file YouTube auth import bundle.

The helper still writes `cookies.txt` and `po_token.txt` for inspection, but the main file you should move to your phone is:

- `auth_bundle.json`

## Requirements

- Node.js 20+
- npm

## Setup

```bash
cd tools/youtube-auth-helper
npm install
```

## Usage

```bash
npm start -- --target-url "https://www.youtube.com/watch?v=aqz-KE-bpKQ"
```

Optional flags:

- `--output-dir <dir>`
- `--profile-dir <dir>`
- `--headless`

## Flow

1. The helper opens a persistent Chromium profile using Playwright.
2. Log into YouTube in that browser if needed.
3. Play the loaded video or another YouTube video.
4. The helper watches `youtubei/v1/player` requests and captures the first `serviceIntegrityDimensions.poToken` it sees.
5. The helper exports YouTube/Google cookies in Netscape format.
6. It writes:
   - `output/cookies.txt`
   - `output/po_token.txt`
   - `output/auth_bundle.json`
7. `auth_bundle.json` embeds:
   - `cookiesContent`
   - `poToken`
   - `poTokenClientHint`

## App import

In the Android app:

1. Open `Settings`
2. Open the `YouTube Auth` section
3. Tap `Import auth bundle`
4. Select `auth_bundle.json`
5. Save settings if needed and retry the YouTube download

## Manual fallback

If you do not want to use the one-file bundle import, you can still:

1. Import `cookies.txt`
2. Paste the contents of `po_token.txt`
3. Select the matching token client hint
4. Save settings

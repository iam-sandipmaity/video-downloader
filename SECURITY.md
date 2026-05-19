# Security Policy

## Supported Versions

Security fixes are currently targeted at the latest release line and the `main` branch.

| Version | Supported |
| ------- | --------- |
| `main`  | Yes       |
| `1.7.x` | Yes       |
| `< 1.7` | No        |

If you are running an older release, please upgrade to the latest available version before reporting a vulnerability unless the issue prevents upgrading safely.

## Reporting a Vulnerability

Please report security issues privately. Do not open a public issue with exploit details, proof-of-concept code, secrets, or private user data.

Preferred path:

1. Use GitHub's Private Vulnerability Reporting / repository security advisory flow for this repository if it is enabled.
2. If private reporting is not available, open a minimal GitHub issue asking for a secure contact path without including technical exploit details.

Please include:

- affected version or commit
- device / Android version if relevant
- clear reproduction steps
- impact assessment
- logs, screenshots, or proof-of-concept material only when necessary

Good examples of in-scope reports for this project include:

- arbitrary file read/write
- command injection or unsafe process execution
- insecure update or runtime replacement flows
- APK signing, install, or release integrity issues
- WebView / JavaScript bridge abuse
- cookie, token, or local credential exposure
- path traversal, export, or storage permission bypasses

## Response Expectations

- Initial acknowledgement target: within 5 business days
- Status update target: at least every 14 days while the report is being triaged or fixed
- Fixes are usually prepared on `main` first and then included in the next supported release

Reports may be closed without a security fix if they are:

- already known
- out of scope
- not reproducible
- best handled as a regular bug report instead of a vulnerability

## Disclosure Guidance

Please wait for a fix or maintainer confirmation before publicly disclosing details. Coordinated disclosure helps protect users who may still be on supported versions.

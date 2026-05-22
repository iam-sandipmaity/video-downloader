# Security Policy

## Supported Versions

Security fixes are targeted at the latest maintained app line and the current
main development branch.

| Version | Supported |
| ------- | --------- |
| `main` | Yes |
| stable `1.7.2` and the current `1.7.2.x` prerelease line | Yes |
| older `1.7.1.x` and below | No |

If you are on an older release, please upgrade before reporting a vulnerability
unless the issue prevents safe upgrading.

Current release posture:

- `1.7.2` is the stable public baseline right now
- new in-progress work can ship first as `1.7.2.0`, `1.7.2.1`, and similar patch-line builds
- prerelease labels such as `-alpha`, `-beta`, or `-test` may be used on that `1.7.2.x` line while validation is still happening
- once that work is considered complete and stable, the next stable public release becomes `1.7.3`

## Reporting a Vulnerability

Please report security issues privately.

Do not post public exploit details, tokens, cookies, private logs, or proof of
concept payloads in a normal issue.

Preferred path:

1. Use GitHub Private Vulnerability Reporting or a repository security advisory
   if it is enabled.
2. If private reporting is not available, open a minimal issue asking for a
   secure contact route without posting the sensitive details publicly.

Please include:

- affected version or commit
- device model and Android version when relevant
- clear reproduction steps
- expected impact
- logs or screenshots only when they are actually needed

## Examples of In-Scope Issues

- arbitrary file read or write
- command injection
- unsafe runtime replacement or update behavior
- APK signing or release integrity issues
- WebView or JavaScript bridge abuse
- cookie, token, or credential exposure
- path traversal or storage-permission bypass

## Response Expectations

- acknowledgement target: within 5 business days
- status update target: every 14 days while triage is active
- fixes usually land on the main maintained branch first and then roll into the
  next supported release

## Out of Scope or Non-Security Reports

Reports may be closed or redirected when they are:

- already known
- not reproducible
- primarily a normal bug instead of a vulnerability
- based on unsupported old versions

## Disclosure Guidance

Please wait for a fix or maintainer approval before publishing full details.
Coordinated disclosure helps protect users who are still on supported builds.

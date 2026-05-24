# Privacy, Security, And Responsible Use

This page summarizes the app's privacy and security posture in practical terms.

## Privacy Summary

Video Downloader is designed as a local-first Android app.

That means the normal product direction is:

- links are analyzed on-device
- downloads happen on-device
- conversion and compression happen on-device
- saved media stays on your device unless you choose to share it
- the project does not require its own account system

## What The App May Process

Depending on which features you use, the app may handle:

- pasted or shared links
- format and naming choices
- cookies you explicitly save
- YouTube access or session recovery data you explicitly generate
- local media files selected for conversion or compression
- local logs used for troubleshooting

## What The App Is Not Trying To Be

The project is not designed to:

- upload your downloads to a project-owned cloud
- require a central user account
- process downloads through a project-run relay service
- force server-side conversion for normal use

## Third-Party Requests

The app does contact third-party services when you ask it to do real work.

Examples:

- the media site you pasted
- upstream media delivery endpoints
- GitHub endpoints for app or runtime updates

Those requests are part of the download and update flows themselves.

## Cookies And Session Data

Cookies are powerful and should be treated carefully.

Important points:

- cookie and session data stays on-device unless you export or share it
- authenticated requests can expose private account access if mishandled
- you should only import cookies when you understand what they represent

## Logs

Logs can be extremely useful for bug reports, but they can also contain
sensitive information such as:

- URLs
- file paths
- runtime error messages
- site-specific request context

Best practice:

- review logs before sharing them publicly
- never post cookies or tokens in a public issue

## Security Posture

Current security direction includes ongoing hardening around:

- runtime update integrity
- storage handling
- WebView trust boundaries
- sensitive data exposure
- file-sharing scope

Security fixes target the latest maintained line rather than very old releases.

## Reporting Security Issues

Please report security problems privately if possible.

Do not post:

- active tokens
- cookies
- exploit payloads
- sensitive logs

Preferred details:

- affected version or commit
- Android version and device model when relevant
- reproduction steps
- impact summary

## Responsible Use

You are responsible for:

- the links you download
- whether you import authenticated cookies
- whether you share private logs or downloaded media
- following the laws and platform terms that apply in your region

## Canonical Policy Docs

- Privacy policy:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/PRIVACY.md`
- Security policy:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/SECURITY.md`
- Vulnerability hardening notes:
  `https://github.com/iam-sandipmaity/video-downloader/tree/main/docs`

## Related Wiki Pages

- [Settings, Cookies, and YouTube Access](Settings-Cookies-and-YouTube-Access.md)
- [Troubleshooting](Troubleshooting.md)
- [Updates, Runtimes, and Compatibility](Updates-Runtimes-and-Compatibility.md)

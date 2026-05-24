# Settings, Cookies, And YouTube Access

This page explains the app's main settings areas and when the access tools are
actually worth using.

## Settings Hub

The app uses a dedicated settings hub instead of a single overloaded page.

Common areas include:

- appearance
- download defaults
- folders and storage
- notifications
- access and network
- about and support

## Appearance

Appearance settings help you adjust how the app feels without changing download
logic.

Common examples:

- theme style
- accent choices
- contrast behavior
- app language

Language support keeps expanding through Android resource-based localization.

## Download Defaults

This is where you shape the most common download behavior before each task
starts.

Useful defaults can include:

- video naming template
- audio naming template
- preferred output behavior
- subtitle defaults
- thumbnail embedding defaults
- concurrency preferences

This section is worth revisiting if you keep making the same choices manually.

## Folders And Storage

Storage settings matter because the app writes real files into public or
app-owned areas depending on device behavior and permissions.

Things to pay attention to:

- public root folder path
- media-specific folders
- whether your device or Android version changes storage behavior

Default public root:

```text
Download/LocalDownloader/
```

## Notifications

Notification controls help you decide how much queue and completion feedback
you want from the app.

Useful when:

- you want download completion notices
- you do not want noisy failure or cancellation notices
- you use background audio playback

## Access And Network

This section is important for reliability.

Common controls include:

- Wi-Fi-only downloading
- cellular permission behavior
- cookies access
- YouTube access entry points

If you use limited mobile data, this should be one of the first settings pages
you review.

## Cookies

Cookies are optional, but they can be very useful when a site:

- requires sign-in
- exposes better formats only to authenticated users
- fails anonymous requests

Best practice:

- only save what you actually need
- understand that cookies can represent active account sessions
- delete outdated or unnecessary cookies

## YouTube Access

The YouTube access tools are for tougher YouTube-specific cases where a normal
request path is not enough.

Use them when:

- YouTube requests fail despite an updated runtime
- anonymous access is no longer enough
- you need better request context for harder videos

Do not treat this as mandatory setup for every install. Many people will only
need it occasionally.

## Help And Support Areas

The app also includes support-style surfaces for:

- updates
- help
- about and credits
- logs or diagnostics

That means many recovery steps can happen inside the app before you need to
open a GitHub issue.

## Practical Advice

- Start simple. Only enable advanced access tools when a site actually needs
  them.
- Revisit defaults after a week of normal use and remove repetitive manual
  choices.
- If a site suddenly behaves worse, check Updates and cookies before assuming
  the app's base logic is broken.

## Related Pages

- [Updates, Runtimes, and Compatibility](Updates-Runtimes-and-Compatibility.md)
- [Troubleshooting](Troubleshooting.md)
- [FAQ](FAQ.md)

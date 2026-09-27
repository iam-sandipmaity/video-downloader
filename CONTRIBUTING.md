# Contributing

Thanks for contributing to Video Downloader.

The current stable public release baseline is `1.7.5.1`. Most work improves one of
these areas:

- download compatibility and extractor regressions
- queue, runtime, or media-processing bugs
- translation quality and locale completeness
- documentation accuracy
- test coverage and internal logic hardening

## Branching & Release Model

We use a **3-tier branch structure**:

```
Feature / Fix Branch  ──►  nightly (Staging)  ──►  main (Production Stable)
```

- **Target `nightly` for all Pull Requests**: Feature branches and bug fixes should branch off `nightly` and target `nightly` in Pull Requests.
- **Nightly Releases**: Merges to `nightly` trigger automated nightly APK builds published under the rolling `nightly` pre-release tag.
- **Stable Releases**: Merges from `nightly` into `main` trigger production releases after testing and version bumps.

For full details and maintainer release checklists, see [docs/BRANCHING_STRATEGY.md](docs/BRANCHING_STRATEGY.md).

## Before You Start

Please read these first:

- [README.md](README.md)
- [docs/BRANCHING_STRATEGY.md](docs/BRANCHING_STRATEGY.md)
- [SECURITY.md](SECURITY.md)
- [COMPATIBILITY.md](COMPATIBILITY.md)
- [docs/development.md](docs/development.md)
- [docs/architecture.md](docs/architecture.md)

## Good First Contribution Types

- fix a broken download flow for a supported site
- improve queue, history, or runtime recovery behavior
- tighten app copy, docs, or translation quality
- add tests around existing logic
- clean up stale docs or release notes

## Things To Avoid In Unreviewed PRs

- large navigation redesigns
- changing the package name or signing assumptions
- replacing downloader/runtime architecture without prior discussion
- bundling secrets, keystores, or personal cookies
- committing generated logs, APKs, or temporary test artifacts

## Development Setup

```bash
git clone https://github.com/iam-sandipmaity/video-downloader
cd video-downloader
git checkout nightly
./gradlew :app:assembleStandardDebug
```

If you are working on a custom FFmpeg ABI or runtime packaging scenario, also
review [COMPATIBILITY.md](COMPATIBILITY.md).

## Pull Request Expectations

Try to keep pull requests focused and reviewable.

A good PR usually includes:

- a short summary of the user-facing problem
- the solution approach
- screenshots or recordings for UI changes
- testing notes
- any risks, follow-up work, or intentionally untouched areas

## Testing Guidance

Before opening a PR, run what is practical for the change:

```bash
./gradlew :app:assembleStandardDebug
./gradlew :app:testStandardDebugUnitTest
```

If you cannot run one of those locally, say so in the PR.

For download-related work, also note:

- tested site(s)
- media type (single video, audio, playlist, subtitles, etc.)
- whether cookies or YouTube access were involved

## Translation Contributions

Translation updates can be sent through
[Hosted Weblate](https://hosted.weblate.org/projects/local-video-downloader/android-app-strings/)
or by editing Android resource files directly in a pull request.

When adding or improving a locale manually:

1. update the locale `strings.xml`
2. preserve placeholders like `%1$d`, `%1$s`, and formatting markup
3. keep technical labels accurate when they should remain untranslated
4. check plural blocks as well as plain strings
5. if a Weblate or manual update translates a key that also exists in a
   locale `strings_lint_fillins.xml`, remove that fallback entry from the
   fill-in file in the same pull request

Please avoid machine-translated bulk locale updates without review.

## Security Notes

Do not open a public issue or PR for a live security problem with exploit
details or private user data. Follow [SECURITY.md](SECURITY.md) instead.

## Style Notes

- keep changes narrow and intentional
- preserve the local-first, on-device execution model
- prefer clarity over cleverness in docs and code
- keep user-facing text concise and practical

## Maintainer Review

Maintainers may ask for:

- narrower scope
- clearer testing evidence
- follow-up cleanup in a separate PR
- wording changes for docs, UX, or translations

That is normal and meant to keep the repository stable and easier to maintain.

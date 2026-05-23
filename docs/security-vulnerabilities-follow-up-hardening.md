# Security Vulnerability Follow-Up Hardening

## Summary

This document captures the lower-severity follow-up findings that remained
after the initial transport, storage, and updater fixes.

All issues below were reviewed and remediated on `2026-05-23`.

## Findings

### VDL-SEC-2026-002: Mutable GitHub Action Tags in CI

- Severity: `Medium`
- CWE: `CWE-494` Download of Code Without Integrity Check
- Affected area: GitHub Actions workflows

#### Risk

The CI workflows referenced actions by mutable major-version tags such as
`@v6`, `@v5`, and `@v3`.

That creates a supply-chain risk because the referenced action code can change
without a corresponding workflow change in this repository.

#### Affected Files

- `.github/workflows/android-build.yml`
- `.github/workflows/cleanup.yml`

#### Remediation

- pinned all GitHub Actions to immutable full commit SHAs
- added explicit `permissions: contents: read` to the build job

#### Validation

After the fix, the workflows no longer depend on mutable action tags.

### VDL-SEC-2026-003: Over-Broad Local Preview WebView Access

- Severity: `Low`
- CWE: `CWE-200` Exposure of Sensitive Information to an Unauthorized Actor
- Affected area: external HTML and web archive preview

#### Risk

The local preview WebView allowed `file://` access for all web preview files,
including plain HTML where direct file access was not required.

That unnecessarily widened the trust boundary for externally supplied preview
content.

#### Affected Files

- `app/src/main/java/com/localdownloader/ui/screens/ExternalPreviewScreen.kt`

#### Remediation

- restricted file access to `.mhtml` and `.mht` archive previews only
- switched normal HTML and XHTML previews to `loadDataWithBaseURL`
- blocked secondary navigations
- kept JavaScript disabled and mixed-content blocked
- enabled Safe Browsing where supported

#### Validation

After the fix, ordinary HTML previews no longer require direct file access.

### VDL-SEC-2026-004: WebView Auth Flow Hardening Gaps

- Severity: `Low`
- CWE: `CWE-749` Exposed Dangerous Method or Function
- Affected area: PO-token generation and embedded auth WebViews

#### Risk

The PO-token flow already needed JavaScript, but the WebView still benefitted
from tighter local access and navigation restrictions.

The risky native bridge was already removed earlier in this remediation wave,
but the WebView configuration itself still had room for hardening.

#### Affected Files

- `app/src/main/java/com/localdownloader/utils/YoutubePoTokenGenerator.kt`
- `app/src/main/java/com/localdownloader/ui/screens/CookiesScreen.kt`
- `app/src/main/java/com/localdownloader/ui/screens/YoutubeAuthScreen.kt`

#### Remediation

- disabled file and content access in the PO-token WebView
- blocked file-URL escalation and mixed content
- blocked unexpected navigations
- enabled Safe Browsing on supported Android versions
- kept the cookie/login WebViews locked down to the minimum required access

#### Validation

After the fix, the PO-token WebView no longer exposes a native JS bridge and
no longer carries unnecessary local-access permissions.

### VDL-SEC-2026-005: Persistent Completed Download Debug Traces

- Severity: `Low`
- CWE: `CWE-922` Insecure Storage of Sensitive Information
- Affected area: download history persistence

#### Risk

Even after secret redaction, completed downloads could still keep sanitized
internal debug traces in persistent task history.

That preserved extra operational detail longer than necessary for successful
downloads.

#### Affected Files

- `app/src/main/java/com/localdownloader/data/DownloadRepositoryImpl.kt`
- `app/src/main/java/com/localdownloader/worker/DownloadWorker.kt`
- `app/src/main/java/com/localdownloader/data/persistence/DownloadTaskEntity.kt`

#### Remediation

- cleared `debugTrace` for completed downloads
- added startup cleanup for already-persisted completed task traces
- kept traces only where they still provide value for non-terminal failure and
  recovery scenarios

#### Validation

After the fix, successful downloads do not retain persistent internal traces in
history storage.

## Audit Note

This follow-up hardening document was added after the static security review on
`2026-05-23`.

# Future Plan: Maintenance Roadmap

This document reflects the project's current direction after the `1.7.2`
stabilization work.

The app is no longer in a broad "figure out the product shape" phase. The main
UI, navigation, queue surfaces, settings structure, and access tools are now
considered stable enough that future work should mostly improve reliability,
compatibility, and internal quality.

---

## What Is Considered Stable For Now

These parts are expected to stay structurally stable unless a real usability
problem or bug forces change:

- Home / Browse primary flow
- Downloads library
- More hub
- Queue and History layout direction
- Settings hub and subpages
- Cookies and YouTube access entry points
- Help / Updates / App log placement
- Converter and Compressor tool positioning

This does not mean the screens are frozen forever. It means the repo should not
default to redesigning them for the sake of redesign.

---

## Highest Priority Work

### 1. Download Compatibility Maintenance

This is the most important ongoing track.

Focus areas:

- extractor breakage from upstream site changes
- YouTube recovery reliability
- playlist edge cases
- metadata and subtitle download regressions
- FFmpeg post-processing compatibility

### 2. Queue And Runtime Reliability

Focus areas:

- scheduling consistency
- retry and resume correctness
- runtime-update safety
- maintaining compile pipelines and NDK configurations in the custom Packages Builder repository
- clearer recovery signals when tasks fail
- stronger diagnostics without noisy logs

### 3. Translation Quality

The app now supports a larger locale set, so the next translation work should
be quality-focused:

- wording review
- consistency review
- missing new-string backfill as the app evolves
- plural and placeholder correctness

### 4. Test Coverage

The repository still needs stronger automated protection around:

- queue logic
- runtime selection
- updates and install gating
- cookie and YouTube access flows
- media-tool validation

---

## Medium Priority Work

### 1. CI And Build Hygiene

- reduce native strip warning noise
- keep SDK/AGP/Kotlin lines current
- preserve release/install compatibility

### 2. Documentation Accuracy

- keep README aligned with the real product state
- update implementation docs when runtime behavior changes
- keep compatibility docs accurate when ABI/runtime packaging shifts

### 3. Smaller UX Fixes

Allowed and useful:

- compactness improvements
- copy clarity
- better empty states
- alignment and spacing fixes
- recovery/action discoverability fixes

Not the default priority:

- large navigation resets
- full visual restyling of already-stable areas

---

## Lower Priority / Optional Work

These are possible later, but not the current repo priority:

- new media-tool presets beyond the existing practical set
- major UI experiments
- broad feature expansion unrelated to download reliability
- exotic ABI packaging improvements beyond current realistic user demand

---

## Release Posture

For the near future, new app updates are expected to happen mainly when one of
these is true:

- a download issue appears
- a runtime/update path needs fixing
- a regression is found
- translation or docs need correction
- internal logic needs reshaping for reliability

That is the intended posture of the current line: stable user-facing structure,
active maintenance underneath it.

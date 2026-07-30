# UI/UX & Design Guidelines

We maintain a premium, state-of-the-art Material Design 3 experience that feels responsive, alive, and secure.

---

## 1. Jetpack Compose & Theming Guidelines

1. **No Ad-hoc Styling**: Never hardcode colors or text sizes directly in composables. Always reference our theme tokens:
   - Use `MaterialTheme.colorScheme.primary`, `surfaceContainerLow`, etc.
   - Use `MaterialTheme.typography.titleMedium`, `bodyMedium`, etc.
2. **Support Dynamic Dark Mode**: Verify all customized components render correctly on both light and dark backgrounds.
3. **Strings Localization**: Never use hardcoded text strings in UI layouts. Always declare them in [strings.xml](file:///home/error/prj/video-downloader/app/src/main/res/values/strings.xml) and pull them via `stringResource(id)`.

---

## 2. Vault Security UI Guidelines

1. **Session Locks**: The private vault contains private data. Navigating away from vault-related screens must trigger active session termination. Always lock on `onBack` and when routing leaves secure composables.
2. **PIN Setup Screen Layout**:
   - Enforce digit constraints (4–8 characters).
   - Display real-time error helper feedback directly under confirm input fields (such as "PINs do not match" or "PIN must be at least 4 digits") using `isError` flags.
   - Force a numeric password layout for input.
   - Keep the proceed/save buttons disabled until validation states succeed.

---

## 3. Media Player UX

1. **Vinyl Animation Deck**: The music player screen uses custom canvas-drawn visuals and vinyl rotation sweeps for active tracks. Keep transitions smooth and ensure tonearm rotation maps cleanly to play/pause state transitions.
2. **Memory-Safe Navigation**: Navigating next/previous in playlist tracks must reuse/recycle routes. Pop the active track player from the backstack using `popUpTo(currentRoute) { inclusive = true }` to prevent memory leaks.
3. **Graceful Failures**: If a selected media path becomes unreachable or missing:
   - Centered error alerts must be displayed.
   - Back actions must be immediately visible to allow immediate recovery.

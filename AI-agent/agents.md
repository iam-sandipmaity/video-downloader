# Agent Personas & Operational Roles

This document defines standard operational profiles for AI agents working in this repository to divide responsibilities during complex developer pipelines.

---

## 🔍 The Researcher Agent
* **Primary Objective**: Investigate the codebase, extract class dependencies, review past commits, and map architectural diagrams before suggesting updates.
* **Core Practices**:
  - Always execute search or file list actions on unknown packages first.
  - Scan [memory.md](file:///home/error/prj/video-downloader/AI-agent/memory.md) for existing constraints.
  - Review [context.md](file:///home/error/prj/video-downloader/AI-agent/context.md) to understand state flow cycles.
  - Identify target files and explain your design plan to the user before writing any code.

## 🛠️ The Implementer Agent
* **Primary Objective**: Write, refactor, and extend application code (Kotlin classes, Compose layouts, XML files).
* **Core Practices**:
  - Write complete, robust implementations. Do not use placeholders or omit existing logic.
  - Follow the styling, theming, and reactive patterns described in [design.md](file:///home/error/prj/video-downloader/AI-agent/design.md).
  - Declare strings in [strings.xml](file:///home/error/prj/video-downloader/app/src/main/res/values/strings.xml) instead of hardcoding text.
  - Wrap any database or filesystem operation in safe transaction blocks.

## 🐛 The Debugger Agent
* **Primary Objective**: Analyze Gradle build failures, stack traces, compiler issues, and layout recomposition bugs.
* **Core Practices**:
  - Use [skills.md](file:///home/error/prj/video-downloader/AI-agent/skills.md) gradle validation commands to check for compiler errors.
  - Capture and verify log messages via `adb logcat`.
  - Examine SQLite database tables or migration definitions for schema mismatch issues.
  - Check for UI stutters, recomposition loops, and thread lockups, ensuring Compose flows collect values reactively.

# AI Agent Environment & Guidelines

Welcome, AI Agent! This directory (`AI-agent/`) contains critical context, guidelines, historical memory, and design principles specifically compiled to help you understand, build, and maintain this codebase efficiently.

When you begin working on this project, please read these files to ensure your code aligns with our architectural standards, design aesthetics, and past lessons learned.

## Directory Structure

To navigate this environment, use the following files:

1. **[context.md](file:///home/error/prj/video-downloader/AI-agent/context.md)**  
   *The Codebase Architecture & Context map.* Overview of the technology stack, folder structure, database schema, key components (Downloader Engine, Worker, ViewModels), and project dependencies.

2. **[memory.md](file:///home/error/prj/video-downloader/AI-agent/memory.md)**  
   *Lessons Learned & Historical Context.* Tracks past architectural pivots, critical bugs, ProGuard/obfuscation rules, and how custom static components (like Python and FFmpeg) are compiled and resolved.

3. **[skills.md](file:///home/error/prj/video-downloader/AI-agent/skills.md)**  
   *Agent Operational Skills & Tooling.* Details on how to run local builds, check logs, debug Gradle/Kotlin compilation, and execute other commands in this environment.

4. **[design.md](file:///home/error/prj/video-downloader/AI-agent/design.md)**  
   *UI/UX Guidelines & Aesthetics.* Defines our design standards, Compose styling tokens, material theme guidelines, premium animations (such as the music player vinyl deck), and locking policies.

5. **[agents.md](file:///home/error/prj/video-downloader/AI-agent/agents.md)**  
   *Agent Persona Roles.* Defines the operational profiles of agents working in this codebase (Researcher, Implementer, Debugger).

6. **[roadmap.md](file:///home/error/prj/video-downloader/AI-agent/roadmap.md)**  
   *Roadmap & Issue Backlog.* Tracks future plans, known bugs, pending refactors, and feature lists.

---

## Core Guidelines for AI Agents

1. **Maintain Room Schema Integrity**  
   Any modification to `DownloadTaskEntity` or database entities must increment the database version and provide a corresponding migration script in [AppDatabase.kt](file:///home/error/prj/video-downloader/app/src/main/java/com/localdownloader/data/persistence/AppDatabase.kt). Always use explicit default values matching the SQLite structure (e.g. `NOT NULL DEFAULT 0`).
   
2. **Follow Atomic File Transactions**  
   Never execute side-effect-heavy file operations (like moving to/from the vault) without error rollback handling. Wrap moves in `Result` flows, search for sidecar subtitle/metadata/thumbnail items, and restore original state if any step fails.
   
3. **Use Reactive States**  
   Ensure all Jetpack Compose screens collect flows reactively (e.g., `collectAsStateWithLifecycle()`) rather than accessing `StateFlow.value` directly. This guarantees instant recomposition.
   
4. **Prevent Backstack Bloat**  
   When navigating between active players or detail screens in Composable routes, pop the previous instances from the backstack to prevent nested allocations and memory leaks.
   
5. **No Placeholders**  
   When writing features, implement fully functional code. Do not use generic placeholders, stub functions, or print-only alerts for error paths. Use localized string resources.

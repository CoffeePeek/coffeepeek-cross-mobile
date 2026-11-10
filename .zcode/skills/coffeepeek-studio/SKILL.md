---
name: coffeepeek-studio
description: Work on the CoffeePeek Kotlin Multiplatform project through Android Studio MCP and IDE tooling. Use whenever a task in this repo involves inspecting or navigating code, refactoring, Gradle builds, run configurations or Build Variants (directSandbox, directDebug, playDebug), device runs, or IDE diagnostics — even if the user doesn't mention Android Studio.
---

# CoffeePeek Android Studio workflow

Use this skill for tasks that depend on Android Studio/IntelliJ project understanding or its configured MCP. Read the repository's current `AGENTS.md` first; it owns architecture, testing, device, and authorization rules. Read a module-specific `AGENTS.md` when entering a module if one exists.

## Select the correct project and source

- For each IDE/MCP call that accepts a project path, pass the actual absolute path of the current CoffeePeek checkout. Do not assume the IDE's active project or reuse a path from a different worktree.
- Inspect Git status before edits when repository state matters. Preserve staged, unstaged, and untracked work; don't assume it belongs to the current task.
- Use IDE project navigation/tree and module/dependency inspection for project structure. Inspect `settings.gradle.kts`, relevant feature Gradle files, source sets, API, DI and navigation before structural changes.
- Read `feature/README.md`, `feature/LEGACY_EXIT_PLAN.md`, and the owning feature plan when migration state affects the task.

## Navigate code with IDE semantics

- Prefer symbol search for declarations and call hierarchy for callers/callees. Use text or regex search for literal/config/resource searches, not as a substitute for a semantic call graph.
- Read the exact declaration and relevant callers before changing contracts. Use IDE quick documentation/type info when the declaration or overload is ambiguous.
- Use semantic rename refactoring for Kotlin symbols. Reformat and run inspections only on files in scope.
- Report which files were inspected. A clean IDE inspection of selected files is not a project-wide result.

## Build and run safely

- Before running the app, read `composeApp/ANDROID_RUN_MODES.md` and inspect the available run configurations. The generic `composeApp` configuration follows the selected Build Variant; its name does not prove that it is Sandbox.
- Use `directSandbox` for isolated fake-data UI checks only when the user asked for a run/check. Use `directDebug`/`playDebug` only for a requested live-backend/device check. Never use Release for routine tests.
- Prefer an exact Gradle task for the requested variant over a generic app run configuration. Shell Gradle commands use `--max-workers=6`; never run concurrent Gradle builds. Report the exact task/configuration and result.
- Before a device operation, identify the agreed device, model, serial and lock state. Explicitly target that device. Do not uninstall, clear data, replace a newer/incompatible APK, or change device security settings.
- Don't launch a run configuration just because it exists. Inspect its selected variant, package and target first; don't silently change the user's Studio variant.
- Never create, restore, or run automated Compose UI, screenshot, or instrumented UI tests. Follow `AGENTS.md` for permitted unit/contract tests and required compilation gates.

## After edits

- Use scoped IDE diagnostics where available, then compile affected modules/app as required by `AGENTS.md`. Never claim a build, device check, or preview ran unless the tool actually returned evidence.
- Business UI and state belong to `feature/<name>/impl`; `composeApp` is application composition. Reuse `core/design-system` rather than making parallel shared components.
- Preserve the user's existing Git changes. Do not commit, push, merge, rebase, switch branches, or discard edits through IDE VCS actions unless the request explicitly calls for it.

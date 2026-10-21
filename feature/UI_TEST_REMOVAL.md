# Migration UI test removal — 2026-10-20

At the user's request, remove the agent-authored Android UI suites from every
open migration checkpoint (#43, #95, #99, #101, #102, #104 and #109).

Scope: design-system and feature Compose UI suites, their test-only Activities
and manifests, and the agent-authored Favorites destination, Shop form host and
history composition/route UI suites in composeApp. Remove unused feature UI-test
dependencies and CI assembly tasks for those suites. Production screens, paired
previews, Sandbox, unit tests, database instrumentation and other contributors'
application UI tests are not part of this deletion.

Earlier dated UI test counts in migration plans, READMEs and PR descriptions are
historical evidence only: the deleted suites are no longer available to rerun.
Use the remaining unit/contract tests and builds, plus explicitly scoped manual
Sandbox checks for UI verification. This deletion does not constitute a new
visual, device or live-backend acceptance run.

The removal is committed in each checkpoint and propagated parent-first without
rewriting published history. Deleted sources remain recoverable from Git.

## Verification at the Roasters tip — 2026-10-20

Source: `9e78663534001ad90f85aa8b3e3f00b440d06120`, followed only by this
documentation update. Compared with the original Roasters tip `ec3944db`,
50 UI-only files were deleted: 36 suite files, six standalone test Activities,
and eight manifests. No production source, unit test, Sandbox fixture or
database instrumentation source changed. All seven checkpoints have no
remaining design-system/feature instrumented UI sources from these suites,
and each child contains its updated parent.

One sequential Gradle invocation (`--max-workers=6 --no-parallel`) succeeded
in 4m 50s: Direct Debug, Play Debug and Direct Sandbox APKs; all three packaged
DEX checks; Sandbox manifest isolation; Direct/Play application instrumentation
and core/database instrumentation compilation. All 30 unit-test tasks executed:
1785 test executions, zero failures/errors/skips, no retries. Direct and Play
include duplicate common cases; this is not a count of unique tests.

IDE diagnostics reported no errors in the changed dependency DSL, design-system
build file or application build file. The initial Roasters Gradle-file inspection
was unavailable because the IDE excluded that file; it is not counted as a
successful inspection. Existing AGP/KMP and preview deprecation warnings remain.

No device tests, installation, visual review or live-backend acceptance were
performed for this removal. Ordinary CoffeePeek and the installed Sandbox were
not updated. Earlier feature/device acceptance gates remain separate.

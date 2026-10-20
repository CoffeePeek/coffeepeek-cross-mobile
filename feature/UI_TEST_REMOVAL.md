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

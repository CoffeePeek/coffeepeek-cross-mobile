# Published menu revision — 2026-10-27

Changing a published menu price failed with HTTP 400 because the client omitted
`payload.menu.expectedInputRevision`. The editor now carries the revision read
with the menu through its snapshot, state, draft and POST/PUT payload.

The current develop reader uses the existing legacy Shop DTO/domain mapper and
the Contributions composition adapter. Those retain `inputRevision`; no catalog
reader migration or unrelated UI changes are included. Only a missing menu gets
revision zero. Missing/negative revisions fail before uploads or submission.
Editing an existing request and retrying preserve its original observed revision.

Verification: Windows, JDK 17, base develop `e385cc3e`; integration branch
`codex/migration-menu-revision-develop`, isolated from the map working tree.

- Contributions domain/data/impl: 17/21/66 tests; legacy data: 70 tests;
  complete application DirectDebug unit suite: 830 tests. Total 1004,
  zero failures/errors/skips. Includes HTTP request, DTO/domain mapper,
  composition adapter, editor revision/retry and invalid revision regressions.
- DirectDebug and DirectSandbox APK assembly, both packaged DEX runtime class
  checks and Sandbox isolation passed. Gradle runs used `--max-workers=6`.
- No device installation, live account writes, automated UI/instrumentation
  tests or iOS/Xcode verification. Existing deprecation warnings remain.

# Database infrastructure

Reviewed against the current source on 2026-11-27. `core/database` owns the bundled
SQLite driver configuration, `CoffeePeekDatabase`, schema migrations, settings DAO
and key-value `SettingRepository`, with Android and iOS factories. The established
`com.coffeepeek.room` package names do not indicate a surviving `modules/room`.

The current Room schema is version 3 and contains `setting_table`. Migrations
`MIGRATION_1_2` and `MIGRATION_2_3` preserve settings while retiring the old local
brew journal. Exported history lives in `schemas/`; KSP runs for common metadata,
Android and both Native targets. There is no destructive migration fallback.

Core does not depend on feature entities/DAOs. Future feature persistence must
preserve that direction; do not import a feature into core to assemble a schema.
Room is exported because the public builder contract exposes it; bundled SQLite
and concrete settings storage remain implementation details.

## Non-UI persistence tests

`src/androidInstrumentedTest` contains isolated V1/V2 Room fixtures and
`ApplicationDatabaseMigrationTest`. They verify the bundled driver, read/write,
close/reopen, valid/missing migration behavior, shipped schema upgrades and
settings preservation. They use unique test-database names and clean only their
own fixtures. These are database contracts, not Compose/UI/screenshot tests.

Build-only checks:

```shell
./gradlew :core:database:assembleDebug :core:database:assembleDebugAndroidTest --max-workers=6
```

Executing `:core:database:connectedDebugAndroidTest` requires an explicitly
selected device and inspection of the test target/setup first. Compilation does
not establish that the tests ran. Do not clear application data or reinstall the
normal application to repair a test environment. No database test or device run
was performed for this documentation refresh.

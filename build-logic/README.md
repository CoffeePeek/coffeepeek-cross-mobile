# Build infrastructure

This is an included Gradle build, not an application/KMP runtime module.
It owns the module catalog and settings registration; application source sets
must never depend on it. The `utilities` subproject exposes the module catalog
and shared `Config` to buildSrc scripts. Task helpers remain in buildSrc.

`Modules.kt` contains actual paths and `Modules.all` registers them through
`com.coffeepeek.modules`. Add a new path and include it in `all`; the settings
plugin rejects duplicates and missing module build files. Paths and dependencies
remain unchanged during this migration.

The separate `conventions` included build owns repeated KMP library setup. It is
loaded for project plugins, not from the settings plugin classpath, so its AGP
and Kotlin Gradle dependencies do not conflict with legacy module plugins.
Its conventions are:

- `com.coffeepeek.kmp.android-library` configures Android library + KMP, Java 17,
  JVM target, shared SDK defaults and the `coffeepeekModule` namespace/resource
  settings.
- `com.coffeepeek.kmp.shared-library` adds the Android, iOS device and iOS
  simulator targets for platform-independent modules.
- `com.coffeepeek.kmp.android-compose-library` configures Android Compose and
  its compiler with the API 37 compile SDK needed by Navigation 3.

Feature build files declare which convention applies, set `android.namespace`
and (when the module owns Android resources) `android.resourcePrefix`, then list
only their dependencies. The Compose convention derives the generated resource
package from `android.namespace`. The former `coffeepeekModule` setters remain
available for already migrated core scripts; new feature scripts use Android's
native DSL.

Project scripts import `com.coffeepeek.buildlogic.module` and the relevant
`api`, `implementation`, and `testImplementation` helpers from buildSrc:

```kotlin
implementation(project(module.core.network))
implementation(project(module.legacy.domain))
```

These helpers map to `commonMainApi`, `commonMainImplementation`, and
`commonTestImplementation`. Plain Gradle `implementation` in a KMP Android
module instead targets Android's `main` configuration and would silently leave
shared/iOS source sets without the dependency. Android-only dependencies use
`androidImplementation`; instrumented-test dependencies use
`androidInstrumentedTestImplementation`. Standard `debugImplementation` and
plugin-specific configurations such as `ksp` are not remapped.

Core references are available but not automatically added to the application.
Gradle's generated `projects` accessors also remain available. Do not create a
second manually maintained module catalog or a runtime umbrella core module.

An included build supplies settings plugins before buildSrc is available and
isolates build-only code from runtime modules; a runtime Kotlin package cannot
serve this responsibility. The buildSrc bridge shares the same compiled catalog
instead of copying constants. No new dependency versions or platform targets are
introduced by the catalog.

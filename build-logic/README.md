# Build infrastructure

Reviewed against source on 2026-11-27. This included Gradle build owns settings/module registration and conventions; runtime modules never depend on it.

- `utilities/src/main/kotlin/com/coffeepeek/buildlogic/Modules.kt`: the one module catalog. `ModulesSettingsPlugin` checks duplicate paths and missing build files.
- `utilities/src/main/kotlin/com/coffeepeek/config/Config.kt`: JVM target and Android SDK/application defaults.
- `conventions/`: project plugins loaded separately from the settings plugin classpath.
- Root `buildSrc/`: dependency/task helpers using the catalog rather than a second copy.

## Conventions

| Plugin | Responsibility |
| --- | --- |
| `com.coffeepeek.kmp.android-library` | Android/KMP library setup, JVM 17 and SDK defaults |
| `com.coffeepeek.kmp.shared-library` | Android plus iOS device/simulator targets |
| `com.coffeepeek.kmp.android-compose-library` | Compose compiler/resources for selected targets |
| `com.coffeepeek.kmp.shared-android-compose-library` | shared/Native logic with Compose compiler restricted to Android |
| `com.coffeepeek.function-body-limits` | 40/20 body limits with frozen existing-debt baseline |

Check the owning module's plugins and source sets before changing its convention. Shared Compose UI needs runtime/compiler support on every compiled target; Android-only UI must not pull Android implementation dependencies into Native.

KMP scripts use `com.coffeepeek.buildlogic.module` plus buildSrc's `api`, `implementation`, `testImplementation` helpers for `commonMain` / `commonTest`. Android-specific dependencies use `androidApi` / `androidImplementation`. Plain Android `implementation` does not automatically expose a dependency to common/iOS. Namespace/resources use the module's declared Android DSL and applicable convention settings.

## Verification

Run Gradle sequentially with `--max-workers=6`:

```shell
./gradlew -p build-logic :build-logic-utils:test --max-workers=6
./gradlew verifyFunctionBodyLimits --max-workers=6
```

`generateFunctionBodyLimitsBaseline` is for shrinking/regenerating debt after refactors, never adding new violations by hand. Debug app assembly runs [packaged DEX checks](ANDROID_RUNTIME_PACKAGING.md); Sandbox also verifies isolation. Convention-based test configuration is not an instruction to create/run UI suites; repository [verification policy](../docs/VERIFICATION_FLOW.md) still applies.

An included build is needed because settings plugins load before buildSrc. It does not justify a runtime umbrella module or another manually maintained module list. Dependency versions remain in `gradle/libs.versions.toml`.

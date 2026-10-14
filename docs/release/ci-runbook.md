# CoffeePeek CI/CD

## Configuration

Prepared on 2026-10-14 using the mobile-cicd skill. All pipelines remain in
GitHub Actions; Xcode Cloud is not configured by this change.

| Event | Android | iOS |
|---|---|---|
| PR to `main` or `develop` | Debug builds, unit tests and lint when Android/shared code changes | Unsigned Simulator build when iOS/shared code changes |
| Push to `develop` | Signed direct APK to Firebase for Android/shared changes | Release archive to TestFlight for iOS/shared changes |
| Push to `main` | Signed direct APK to the website and Play AAB for Android/shared changes | Release archive to TestFlight for iOS/shared changes |
| Manual workflow dispatch | Runs the selected workflow even without matching file changes | Runs the selected workflow even without matching file changes |

Release destinations, signing secrets, build numbers and upload serialization
are unchanged. TestFlight still uses `API_BASE_URL_MAIN` for both branches.
The Simulator workflow no longer runs automatically on push.

## Change detection

PR filters are in `.github/filters/mobile.yml`, read by the reusable
`mobile-changes.yml` workflow. Shared source sets, module build files, build
logic, version catalogs and Gradle wrapper changes run both platforms. Android
source sets run Android; iOS/Apple/native source sets and Swift run iOS.
Documentation and design assets do not cause application builds.

Both PR workflows always start, with change detection on Linux. The existing
`build-apk` and `build` check names are preserved as final result jobs. An
irrelevant PR gets a successful check without compiling the application. A
failed detector or a failed/cancelled required build fails its final result job.
No `pull_request_target` event or signing secrets are used for PR builds.

Release workflows use GitHub's push path filters. Keep their filters consistent
with the PR filters when adding source sets or build inputs. A manual dispatch
bypasses path filtering, and is useful for rerunning a release after changing
repository variables or secrets.

## Caching

- All Gradle builds use `gradle/actions/setup-gradle@v6` with its default enhanced
  cache provider. Jobs on `main` and `develop` can save updated Gradle state.
  PRs and other branches only read Gradle state. There is no overlapping
  `setup-java` Gradle cache.
- `.github/actions/setup-ios-caches` caches `~/.konan`, including downloaded
  Kotlin/Native tools and dependencies. Keys include OS, architecture, Xcode,
  Kotlin, target and Gradle configuration. Restore prefixes never cross Xcode,
  Kotlin or target boundaries.
- SwiftPM source packages use `$RUNNER_TEMP/SourcePackages`, explicitly passed
  to both Simulator builds and archives. Keys include OS, architecture, Xcode
  and `Package.resolved`. DerivedData, archives and signing assets are excluded.
- The cache actions may save dependency caches in a PR's merge-ref scope;
  these are not visible to release jobs. Gradle PR caching remains read-only.
- Configuration-cache entries are not transferred between runners: no
  `GRADLE_ENCRYPTION_KEY` is provisioned. Signed Android builds retain their
  existing `--no-configuration-cache` setting.
- The first run populates the new caches. A missing cache is safe and causes a
  normal build. To invalidate dependency caches, increment `konan-v1` or
  `swiftpm-v1` in the composite action; do not delete signing secrets.

Full Android checkout history is retained because `versionCode` and
`versionName` currently depend on the Git commit count.

## Measurement

The audit sampled the latest 80 `develop` runs and selected successful runs.
These are elapsed workflow times, including queue/setup, not billed minutes:

| Workflow | Successful samples | Baseline median |
|---|---:|---:|
| Android CI | 18 | 4m 23s |
| Android Release | 21 | 8m 45s |
| iOS build verification | 13 | 13m 18s |
| iOS TestFlight | 14 | 15m 52s |

After the changes reach GitHub, compare at least five completed warm-cache
builds per platform. Record cache restore/save messages, compilation/archive
duration and total workflow duration separately. Check that a docs-only PR
does not allocate a Mac, that platform-specific PRs build the right platform,
and that a shared-code PR builds both. Compare total runner usage separately
from elapsed time: removing parallel duplicate builds saves compute without
necessarily reducing the duration of a release.

## Existing release guides

- `docs/IOS_SETUP.md`: Apple signing, App Store Connect and TestFlight.
- `docs/FIREBASE_CD.md`: Android development distribution.
- `docs/ANDROID_APK_CD.md`: signed APK publication and rollback.
- `docs/GOOGLE_PLAY_CD.md`: Play internal testing.

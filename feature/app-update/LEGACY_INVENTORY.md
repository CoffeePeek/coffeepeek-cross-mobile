# App update — legacy-first inventory and plan — 2026-10-21

Method: the rule in [ANDROID_REFACTORING_PLAN.md](../ANDROID_REFACTORING_PLAN.md#legacy-first-parity-gate-mandatory-for-every-owner), format as in
[../settings/LEGACY_INVENTORY.md](../settings/LEGACY_INVENTORY.md). Read-only analysis of the legacy `composeApp/.../feature/appupdate` code on
branch `feature/appupdate-refactoring` (stacked on #118): no code changed, Gradle not run, nothing tried on a device. Every row is **open** until the
owner confirms it and ends as **preserved**, **improved (how)** or **intentionally removed (why, with approval)**. Test files are listed by name; their
contents were not audited row by row.

Why this owner exists: Settings left only an `UpdateCheckPort` for it, and the legacy session/legacy-module cleanup needs the next owner written
against `SessionReader`/ports from day one ([../SESSION_REPLACEMENT_PLAN.md](../SESSION_REPLACEMENT_PLAN.md)). App update does not read the session.

There is **no `feature/app-update` module yet**; this directory holds only this file.

## Scope and sizes (legacy, lines)

| Area | Lines |
|---|---|
| `domain`: `AppUpdate` (policy rules) 14, `UpdateInstaller` + `InstallationState/Stage` + `isCompatibleUpdate` 42 | 56 |
| `data/AppUpdateRepositoryImpl` (HTTP + dismissed version) | 48 |
| `ui/AppUpdateState` (check/dismiss state machine) 85, `PlayUpdateRetry` 31, `AppUpdatePrompt` (dialog card) 248, `UpdateInstallationEffect` expect 6 | 370 |
| Android: `PlayUpdateInstaller` 121, `ApkUpdateInstaller` 202, `AndroidUpdateInstaller` 12, `UpdateInstallationEffect.android` (snackbar + launchers) 149, `UpdateInstallerModule.android` | ~500 |
| iOS: `StoreUpdateInstaller` 21, effect 6, `UpdateInstallerModule.ios` | ~35 |
| Sandbox: `SandboxUpdateInstaller` (never installs), `SandboxUpdatePolicy` fixture from the Settings work | ~30 |
| Tests today: `AppUpdateStateTest` 107, `PlayUpdateRetryTest` 94, `UpdateInstallationTest` 42, `AppUpdateTest` 45 (commonTest); the update case of `AndroidSettingsPortsTest`; `toUpdate` validation in `SandboxApplicationBackendTest`. **No test** for `AppUpdatePrompt`, the Play/APK installers or the Android effect | ~290 |

## (1) Inventory of behaviour to keep

### A. Policy (domain/data)

| # | Behaviour | Source |
|---|---|---|
| A1 | `AppUpdate(versionCode, minVersionCode, url)`: required = current < min; available = current < versionCode; prompt = available and (required or manual or dismissed != versionCode) | `AppUpdate` |
| A2 | Response validation: `isSuccess`, `min >= 0`, `version >= min`, https URL, non-blank host, no user/password; anything else is "no update" | `toUpdate` |
| A3 | Fetch `GET /api/v1/app-version?platform=<android\|ios>&channel=<apk\|play>` (no channel on iOS), `Cache-Control: no-store`, 10 s timeout, non-200 is "no update", through the unauthenticated client | `AppUpdateRepositoryImpl` |
| A4 | Dismissed version stored in settings key `dismissed_app_version`, read once per process and kept in memory; dismiss is ignored for a required update; a failed write is swallowed | `AppUpdateRepositoryImpl`, `AppUpdateState.dismiss` |
| A5 | Unknown current version (`AppConfig.versionCode == null`): no fetch, never prompts | `AppUpdateState` |

### B. Check state machine

| # | Behaviour |
|---|---|
| B1 | `check(manual)` is ignored while a check runs; sets `checking`; result `UpdateUiState(update, showPrompt, manual, checkFailed, message)`; manual only: message "Установлена актуальная версия" or "Не удалось проверить обновления. Попробуйте позже."; automatic failures are silent; a timeout counts as a failure, any other cancellation is rethrown |
| B2 | `takeNativePrompt(transfer)` consumes `showPrompt` once and returns the update only if the installer is `Available` and has no error (or the check was manual) |
| B3 | `needsPlayFallback`: on the Play channel the custom dialog is shown only for a required update whose native transfer failed or progressed |
| B4 | Automatic check on every `ON_RESUME` of the root; manual check from Settings (`UpdateCheckPort`) |

### C. Installer contract and rules

| # | Behaviour |
|---|---|
| C1 | `InstallationStage` Available/Starting/Downloading/Verifying/Ready/Installing, `progress`, `busy`, `canCancel`, `error` |
| C2 | `isCompatibleUpdate`: same package, expected version, newer than installed, signers present; multiple signers must be equal, a single signer must be in the downloaded signing history |

### D. Android Play channel (`PlayUpdateInstaller`)

D1 flexible update, immediate when required; D2 refuse when Play's `availableVersionCode` < `minVersionCode` ("Обязательное обновление пока недоступно…"); D3 resume an update already downloaded or developer-triggered; D4 DOWNLOADED means Ready (flexible) or Installing (immediate); D5 consent cancel: required shows "Чтобы продолжить, установите обязательное обновление через Google Play.", optional returns to Available; D6 `completeUpdate` failure returns to Ready with an error; D7 other failures use the fixed Russian messages; D8 `refresh()` on resume re-reads Play state.

### E. Android APK channel (`ApkUpdateInstaller`, flavor flag `APK_UPDATES_ENABLED`)

E1 https-only URL; E2 `DownloadManager` download with a visible notification, state persisted in `SharedPreferences("app_update_download")` (id, version, minimum, url), polled every 500 ms; E3 verify package, version and signers with C2 before Ready and again before install; E4 FileProvider install intent, with the "unknown sources" settings detour and automatic resume after the user grants it ("Разрешите CoffeePeek устанавливать обновления…"); E5 cleanup of the download and prefs on failure/cancel/already-installed; E6 `canCancel` while downloading.

### F. Prompt dialog (`AppUpdatePrompt`, common)

F1 full-width dialog card with an icon (coffee, check when Ready, progress while busy), titles and bodies per stage (strings kept verbatim), buttons "Позже" / "Скачать" (or "App Store" on iOS) / "Повторить" / "Установить" / "Отмена"; F2 required: no "Позже", no back/outside dismiss, no swipe; optional: swipe up beyond 64 dp dismisses; F3 hidden state remembered per version (`rememberSaveable`), re-shown on a new prompt or when Ready; F4 light and dark previews.

### G. Android installation effect (Play channel, non-APK)

G1 registers the activity-result launchers for Play consent and the APK install intent; G2 `installer.refresh()` on resume; G3 on the Play channel the prompt appears as a snackbar: "Обновление загружено. Перезапустите приложение для установки." with "Перезапустить", errors with "Повторить"; G4 a required downloaded update installs automatically; G5 retry = `retryPlayUpdate`: wait for any check, manual re-check, start the native flow, 30 s timeout; G6 dismissing the snackbar dismisses the update (optional only); G7 snackbar sits above the floating navigation.

### H. iOS

H1 `StoreUpdateInstaller` opens the URL in the browser; failure shows "Не удалось открыть App Store."; H2 no installation effect.

### I. Wiring and environment

I1 `KoinApp`: repository over the plain client, `AppUpdateState`; per-platform `UpdateInstallerModule` (APK/Play on Android by `BuildConfig.APK_UPDATES_ENABLED`, Store on iOS); I2 `App.kt` composes `AppUpdatePrompt` on both platforms; I3 `SettingsScreen` (legacy) and `AndroidUpdateCheckPort` read `AppUpdateState`; I4 manifest: `FileProvider` (`${applicationId}.fileprovider`) and `REQUEST_INSTALL_PACKAGES` (removed in the Sandbox manifest); Play `app-update` dependency; I5 Sandbox: `SandboxUpdateInstaller` never downloads, installs or binds Play, and the Sandbox root does **not** compose `App.kt`, so the prompt is unreachable there; I6 no analytics events exist for updates.

## (2) Parity risks and gaps

1. Only the pure policy/state/retry logic has tests; the dialog, both installers and the effect have none, and Play/APK flows cannot run offline.
2. Required-update behaviour is not testable in the Sandbox today (no prompt host). Fixing that is a precondition for a visual check of Required/Available/Failure.
3. The Play channel never shows the dialog unless the native flow fails (B3); easy to break when the dialog moves.
4. The APK path writes files, shares a FileProvider authority and needs a permission declared in the app manifest; moving code must not change authority, prefs name, download folder or manifest entries.
5. Existing installs keep `dismissed_app_version` and the `app_update_download` prefs: keys must stay.
6. `PlayUpdateInstaller` reads `BuildConfig.VERSION_CODE` of the app module; a library cannot, so the version must come through a port.

## (3) Target design (proposal, pending the decisions below)

Layout `feature/app-update/{api,domain,data,impl}`, package `com.coffeepeek.feature.appupdate`, resource prefix `appupdate_`, no dependency on `modules/*`.

- **domain**: `AppUpdate` rules, `InstallationState/Stage`, `isCompatibleUpdate`, `UpdateCheckResult`.
- **api**: ports `AppUpdateEnvironment` (versionCode, platform, channel, apkUpdatesEnabled, fileProvider authority), `DismissedUpdateStore` (read/write the dismissed version), `UpdateLinkOpener` (iOS store link), `AppUpdateChecker` (what Settings needs: observe `UpdateCheckState`, `check(manual)`), Android entries (`AppUpdateEntry` host composable slot) and the `AndroidUpdateInstaller` contract.
- **data**: HTTP repository (`/api/v1/app-version`, validation A2, timeout A3) on the unauthenticated client from `core/network`.
- **impl**: typed MVI view model replacing `AppUpdateState` (A1-B4), prompt UI on `core/design-system` (F), Play and APK installers plus the effect (D, E, G) in `androidMain`; iOS keeps legacy through a Legacy renderer (see Q4).
- **app**: Koin wiring, adapters for the ports (`AppConfig`, Room settings, `OpenInBrowser`, `BuildConfig`), `AppUpdateRenderer` replacing the direct `AppUpdatePrompt()` call in `App.kt` (Legacy default for iOS), `AndroidUpdateCheckPort` delegating to the new checker, Sandbox host.

## (4) Plan and PR slicing (at most 40 files per PR, one layer group per PR)

| Step | Content | Expected files |
|---|---|---|
| 0 | Sandbox: compose the update host in the Sandbox root so the existing fixtures (`Available`/`Required`/`Failure`/`UpToDate`) are visible; extend Sandbox tests | ~8 |
| 1 | Parity tests on the legacy code for the gaps in section (2) that unit tests can reach (response validation table, `shouldPrompt` matrix, takeNativePrompt/needsPlayFallback, retry outcomes, installer state machine through fakes) | ~6 |
| 2 | `api` + `domain` + `data` modules, ports, repository and its tests; adapters for the ports in the app | ~30 |
| 3a | `impl`: view model, state, prompt UI, entries, tests | ~25 |
| 3b | `impl` Android installers and effect (Play, APK), `AndroidUpdateInstaller` wiring, moved manifest/Gradle dependency | ~20 |
| 4 | App connection: renderer in `App.kt`, Koin, Settings adapter, Legacy default for iOS | ~15 |
| 5 | Retroactive audit of the migrated path | docs only |

## (5) Owner decisions needed

| # | Question | Recommendation |
|---|---|---|
| Q1 | Name `feature/app-update`, package `com.coffeepeek.feature.appupdate`, prefix `appupdate_` | Yes |
| Q2 | Play/APK installers live in `impl/androidMain` (with the Play `app-update` dependency) and get version/flavor/FileProvider authority through `AppUpdateEnvironment` | Yes; keeps authority, prefs name and download folder unchanged |
| Q3 | Dismissed version stays in the app's Room settings behind `DismissedUpdateStore`, key `dismissed_app_version` preserved | Yes (as theme/city) |
| Q4 | iOS keeps the legacy prompt and `StoreUpdateInstaller` through a Legacy renderer until iOS migrates | Yes |
| Q5 | Sandbox root hosts the update prompt so Required/Available/Failure can be checked (installer stays the non-installing fake) | Yes |
| Q6 | Texts verbatim, the three `update_*` strings and the dialog texts move into `appupdate_` resources | Yes |
| Q7 | No analytics events (none exist today) | Yes |
| Q8 | Prompt visuals preserved (card, icon, buttons, swipe), only the building blocks move to design-system | Yes |

## Step 0 result — Sandbox hosts the update prompt (2026-10-21)

- `SandboxScreen` composes `AppUpdatePrompt(updates)` next to `AuthorBlockingHost` (outside the re-keyed Navigator) and calls `updates.check()` when the update chip or
  the «Заполнить»/«Очистить» reset changes the policy. No production code changed; the installer stays the non-installing fake (decision Q5).
- New unit test `SandboxApplicationBackendTest.updatePromptStateFollowsTheFixturePoliciesThroughTheRealRepository` drives the real `AppUpdateRepositoryImpl` and
  `AppUpdateState` through every fixture: up to date (no prompt); available (prompt, not required, «Позже» persists across a fresh state, a manual check shows it again);
  required (prompt, dismiss ignored); failure (silent when automatic, failure message when manual); manual up to date message.
- Verified: `SandboxApplicationBackendTest` 18, 0 failures; `assembleDirectSandbox` passes.
- **Device check, Pixel 7 / Android 17 (`com.coffeepeek.sandbox` only, updated in place, regular app untouched), launch extras `sandbox_update`:**
  *Available*: the Play-channel snackbar appears with the blocked-installer message («Внешние приложения отключены в Sandbox») and «Повторить», as the legacy flow
  does when the native step fails; *Required*: the blocking card «Обновите CoffeePeek / Установите новую версию, чтобы продолжить.» appears over the app with the same
  error and «Повторить», no «Позже»; *Failure*: no prompt at launch (silent), and the manual check from Settings shows «Не удалось проверить обновления…» under the row.
- **Still not exercised:** dark theme and large font on the card, back/outside-tap/swipe on the Required card, the real Play and APK installers, an iOS run, TalkBack.

## Step 1 result — parity tests on the legacy code (2026-10-21, branch `feature/appupdate-parity-tests`)

12 new tests, no production change. Together with the existing `AppUpdateStateTest` 5, `AppUpdateTest` 2, `PlayUpdateRetryTest` 5, `UpdateInstallationTest` 2 they pin rows A-C of the inventory:

- `AppUpdateCheckBehaviourTest` (commonTest, 8): manual check answers «Установлена актуальная версия» / «Не удалось проверить обновления. Попробуйте позже.» while an automatic check stays silent
  (B1); an error or a timeout is a failed check, not a crash (B1); an unconfigured version never fetches and a manual check reports a failure (A5); a second check while one runs is ignored (B1);
  the dismissed version is read once per process, a stored older dismissal does not hide a newer version and a failed write still hides the prompt in memory (A4); a required update cannot be
  dismissed and keeps prompting (A4); the stages that count as busy (C1); policy host validation (A2).
- `AppUpdateRepositoryTest` (androidUnitTest, 4, real time because of the 10 s timeout): request is `GET /api/v1/app-version` with `platform`/`channel` from `AppConfig` and `Cache-Control: no-store`
  (A3); 204, `isSuccess=false` and an http URL give no update; error statuses (404/503) and an unreadable body throw (the state turns them into a failed check); the dismissed version is stored under
  `dismissed_app_version`, an unreadable value reads as none (A4).
- **Characterized quirks (kept for the migration, not endorsed):** Ktor accepts `https:///app.apk` and `https://` as having a host, so the policy URL check lets them through; the shared client
  throws on non-2xx instead of returning, so the repository's `status != 200 -> null` branch only covers 2xx answers other than 200; a manual check with an unconfigured version reports a *failure*.
- Still not covered by unit tests (device/instrumented only): `PlayUpdateInstaller`, `ApkUpdateInstaller`, `AppUpdatePrompt`, the Android effect, `StoreUpdateInstaller` (inventory D-H).
- Verified: Direct and Play unit runs of the new classes, 0 failures; iOS test compilation passes.

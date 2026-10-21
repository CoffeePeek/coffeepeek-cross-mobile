# Auth — legacy-first inventory and plan — 2026-10-21

Method: the rule in [ANDROID_REFACTORING_PLAN.md](../ANDROID_REFACTORING_PLAN.md#legacy-first-parity-gate-mandatory-for-every-owner), format as in
[../app-update/LEGACY_INVENTORY.md](../app-update/LEGACY_INVENTORY.md). Read-only analysis of the legacy `composeApp`, `modules/data` and `modules/network` code on branch `feature/auth-refactoring`
(stacked on #125): no code changed, Gradle not run, nothing tried on a device. Every row is **open** until the owner confirms it and ends as **preserved**, **improved (how)** or **intentionally removed
(why, with approval)**. Files not read line by line are marked "skimmed". Test files are listed by name; their bodies were read only where stated.

Why this owner now: Auth is the main writer of the session (`login`, `googleLogin`, `appleLogin`, `logout`) and the entry every other owner points at (`Navigator.navigate(Screen.Auth)` has about 30 call
sites, most of them already in migrated adapters). It is the next step of [../SESSION_REPLACEMENT_PLAN.md](../SESSION_REPLACEMENT_PLAN.md) section 4 "Writers".

There is **no `feature/auth` module yet**; this directory holds only this file.

## Scope and sizes (legacy, lines)

| Area | Lines |
|---|---|
| `ui/screen/auth`: `AuthScreen` 161, `AuthViewModel` 100, `AuthLayout` 407 (scaffold, mascots, stepper, footer), `AuthFields` 199, `SocialSignInButtons` 169 | 1036 |
| `ui/screen/auth/registr`: `RegisterScreen` 452, `RegisterViewModel` 211 | 663 |
| `feature/passwordrecovery`: screen 64, view model 61, repository + impl 34 | 159 |
| `auth/`: `AppleAuth` (common 37, android 14, ios 53), `GoogleAuth` (common 7, android 95, ios 53), `GoogleSignInButton`, `GoogleSignInResult`, `SocialLoginError`, `SessionRealtimeManager` 206 | ~480 |
| `utils/ValidationUtils` (email, password, login error; also phone/URL/Instagram for other forms), `analytics/SignInAnalytics`, `ui/component/GuestAuthCard` 91 | ~230 |
| Legacy layers: `modules/domain` `AuthRepository` 14, `modules/data` `AuthRepositoryImpl` 85, `modules/network` `AuthService` (login, google, apple challenge/login, register, email check, logout, refresh) | ~300 |
| Tests today: `AppleLoginFlowTest` 8, `AppleLoginPersistenceTest` (data) 5, `RegisterAnalyticsConsentTest` 3, `PasswordRecoveryViewModelTest` 4, `PasswordRecoveryRepositoryTest` 3, `SocialLoginErrorTest`, `PasswordValidationTest`, `AuthServiceTest` (network). **No test** for `AuthViewModel` (login), `RegisterViewModel` steps and validation, `localizedLoginError`, `validateEmailRequired`, `AuthRepositoryImpl.login/googleLogin/logout`, or any screen | ~600 |

## (1) Inventory of behaviour to keep

### A. Email login (`AuthScreen`, `AuthViewModel`)

| # | Behaviour |
|---|---|
| A1 | Fields: Email and Password (placeholder `name@example.com`, leading icons, password visibility handling in `AuthFields`); email is trimmed, password is not |
| A2 | Validation on submit: email required (`Введите email`) and format (`Введите корректный email`); password required (`Введите пароль`) and, for the login screen, at least 6 characters (`Пароль должен содержать как минимум 6 символов`); both field errors are shown at once and nothing is sent while either is set |
| A3 | Editing clears the login error; a field error is re-validated live only after it has been shown once |
| A4 | Request errors are mapped by `localizedLoginError`: credentials-like words (`unauthorized`, `invalid credential`, `wrong password`, `user not found`, `неверн`...) → `Неверный email или пароль`; network-like words → `Не удалось подключиться. Проверьте интернет-соединение`; anything else → `Не удалось войти. Проверьте email и пароль`; shown inline above the buttons |
| A5 | Success: `AuthRepository.login` saves the session; if the user id differs from the stored one, local user data is cleared first (`UserSessionCleaner`); then analytics `sign_in_completed` (method `password`, entry `login`) with identity sync and completion of a pending password sign-up; then `Navigator.navigate(Screen.Main)` |
| A6 | While the request runs the legacy base class shows the global loading overlay (`LoadingHandler`), clears it in `finally`; a cancelled coroutine shows nothing |
| A7 | «Забыли пароль?» opens `ForgotPassword(email)` with the typed email; footer «Создать аккаунт» replaces the screen with Register (`popThenNavigate`); the close button pops (`closeAuth`) |
| A8 | Social buttons and the "or" divider are shown only when Apple is supported or Google is configured |

### B. Social sign-in

| # | Behaviour |
|---|---|
| B1 | Google (Android): `GoogleSignIn` activity-result launcher with the web client id from `BuildConfig`; the buttons are hidden when it is blank (`isGoogleSignInConfigured`); `SIGN_IN_CANCELLED` is silent; known status codes get Russian developer-facing messages (DEVELOPER_ERROR, SIGN_IN_FAILED, others with the code) |
| B2 | Google token → `AuthRepository.googleLogin` → same persistence as A5 (including clearing local data when the user changes) → analytics method `google` (entry `login` or `register`) → Main |
| B3 | Errors go through `socialLoginError`: account already exists, unverified email, network, generic; provider diagnostic codes (`GSI-…`, `ASI-…` with optional domain / OAuth error) are appended as «(код …)» |
| B4 | Apple (iOS only; `AppleAuth.android` is unsupported): challenge → native authorization → exchange; failures carry stage markers `[ASI-CH]`, `[ASI-X]`, `[ASI-P]` (persistence); cancellation propagates silently; a double tap is ignored (`appleSigningIn`); unlike password and Google, **Apple login does not clear local user data when the user changes** (legacy asymmetry) |
| B5 | iOS Google goes through a Swift bridge (`IosGoogleAuthBridge`); the Apple button is an expect/actual composable |

### C. Registration (`RegisterScreen`, `RegisterViewModel`)

| # | Behaviour |
|---|---|
| C1 | Three steps with a stepper (Email → Details → Success, mascot laptop then happy); back from Details returns to Email (and clears the password error), from Email/Success pops |
| C2 | Email step: format validation, then `isEmailTaken`; a taken email shows the error text stored under the string key `email_no_exist` (the key name reads the opposite way); a failed check shows the global error with the default message |
| C3 | Details step: name (required), password (min **8**, live re-validation after the first error), terms checkbox (required, with Terms and Privacy links from `LegalUrls`), optional analytics checkbox kept separate from the terms (saved with `setConsent` just before `register`; no SDK setup before login) |
| C4 | Submit validation marks the field errors and reports **one** global message. The checks run in the order terms, password, email, name and each failure overwrites the message, so the **last failing check wins** (name beats email beats password beats terms); valid → consent saved → `register` → the password sign-up is remembered for analytics → Success step |
| C5 | Success step shows the email and «go to login» |
| C6 | Social sign-up buttons on the first step use entry `register` (B2, B4) |

### D. Password recovery

| # | Behaviour |
|---|---|
| D1 | `POST /api/Users/password/forgot` with `{ "email": trimmed-lowercased }`; non-2xx or `isSuccess=false` is a failure; cancellation is preserved |
| D2 | Two steps: Email (info text, field, «Отправить письмо» / «Подождите…» while busy) and EmailSent (explains the web flow, «Вернуться ко входу», «Указать другой email»); edits are ignored while busy; invalid email shows the error without a request; failure shows «Не удалось отправить письмо…» and can be retried |
| D3 | The reset itself happens on the web; `Screen.ResetPassword` (deep link) redirects to the login screen; «Вернуться ко входу» opens Auth clearing the stack to Main |

### E. Sign-out and session end (neighbours, not all owned by Auth)

| # | Behaviour | Proposed owner |
|---|---|---|
| E1 | `AuthRepository.logout`: reads the refresh token, **local cleanup first and authoritative**, remote logout best-effort, cleanup failure reported as the result; used by the profile (legacy `ProfileViewModel` and the Android own-profile renderer) | Auth (domain `SignOut`) |
| E2 | `BaseViewModel.requireAuthSession` clears an inactive session | session/legacy base class |
| E3 | `SessionRealtimeManager`: SignalR `ForceLogout` revocation, reconnect every 5 s, started by `CoffeePeekApplication` | stays app/session-owned |

### F. Entry points and guests

| # | Behaviour |
|---|---|
| F1 | About 30 call sites open Auth/Register through `Navigator.navigate(Screen.Auth/Register)` (Shop, Feed, Profile, Settings, Favorites, Community, author blocking, check-in tab); migrated owners already do it from app adapters. Navigation stays app-owned |
| F2 | `GuestAuthCard` (login/register prompt) is an app component used by Shop, Profile, Settings (as a slot), blocked authors; analytics `auth_gate_shown` through `Analytics.gate(feature)` |
| F3 | After a successful login the user always lands on `Main`, not back where the gate was shown |
| F4 | On iOS the Auth, Register and ForgotPassword controllers extend under the home indicator (native shell) |

### G. Wiring and shared pieces

| # | Behaviour |
|---|---|
| G1 | `AuthRepository` comes from `DataModule` (`AuthRepositoryImpl(authService, sessionRepository, userSessionCleaner)`); `AuthViewModel`, `RegisterViewModel` and `PasswordRecoveryViewModel` are Koin ViewModels |
| G2 | `ValidationUtils` also serves other forms (optional email/phone/URL/Instagram); `LoadingHandler`/`ErrorHandler` are app singletons used by the legacy base class |
| G3 | Sandbox replaces the repository with `SandboxAuthRepository`: any email/password succeeds locally, Google/Apple fail with "disabled", registration always succeeds, no email is ever "taken"; the fake backend has no auth routes |

## (2) Parity risks and gaps

1. No unit test pins the login view model, the registration step/validation logic, `localizedLoginError`, the email/password validators or `AuthRepositoryImpl` login/Google/logout; only Apple and recovery are covered.
2. The registration error priority (C4) is order-dependent and surprising; it must be characterized before any rewrite.
3. B4: Apple login skips local-data clearing while the other two clear it; iOS keeps legacy, so this only matters if Android ever gets Apple.
4. Login success lands on `Main` regardless of where the user came from (F3); changing it is a product decision, not a refactoring one.
5. The legacy flow relies on global overlays (A6, C2): a migrated feature cannot use app singletons, so something local has to replace them.
6. Google and Apple need real accounts and Google Cloud / Apple configuration; nothing here is testable offline, so they stay an authorized live gate.
7. The Sandbox cannot exercise failures (wrong password, offline, 5xx, taken email, recovery failure) because auth never reaches the fake backend.
8. `SessionRepository`-typed code in `AuthRepositoryImpl` and `UserSessionCleaner` (clears legacy favorites) means the data layer cannot leave `modules/data` without a cleaner port.

## (3) Target design (proposal, pending the decisions below)

`feature/auth/{api,domain,data,impl}`, package `com.coffeepeek.feature.auth`, resource prefix `auth_`, no dependency on `modules/*` or `composeApp`.

- **domain**: `Credentials` rules (email, password, registration name/terms), `AuthFailure` mapping (the `localizedLoginError` and `socialLoginError` rules as pure functions), `SignInResult`, repositories `AuthRepository` (login, google, register, email taken, sign out, recovery request).
- **api**: ports `LocalUserDataCleaner` (what `UserSessionCleaner` does today), `AuthAnalytics` (sign-in completed, sign-up remembered/completed, consent), `GoogleSignInConfig`, `AuthExternalActions` (terms/privacy links); Android entries `LoginEntry`, `RegisterEntry`, `PasswordRecoveryEntry` taking navigation callbacks; `SignOut` for the profile.
- **data**: HTTP on `core/network` (`/api/Tokens`, `/api/Tokens/google/login`, `/api/Users`, email check, `/api/Users/password/forgot`, logout) writing the session through `core/session` `SessionStore`; Apple challenge endpoints stay in `modules/network` for the iOS legacy path.
- **impl**: typed MVI view models (login, register steps, recovery), screens on `core/design-system`, Google launcher in `androidMain`.
- **app**: renderers with Legacy defaults for iOS, Koin wiring, adapters for the ports (analytics, cleaner, config, external links), Navigator callbacks.

## (4) Plan and PR slicing (at most 40 files per PR, one layer group per PR)

| Step | Content | Expected files |
|---|---|---|
| 0 | Sandbox: serve the auth routes in the fake backend and let the Sandbox use the real repository, with scenario chips (login: success / wrong password / offline / server error; register: email taken / failure; recovery: success / failure); Sandbox tests | ~12 |
| 1 | Parity tests on the legacy code for the gaps in section (2): login VM, registration steps and the C4 priority, validators and `localizedLoginError`, `AuthRepositoryImpl` login/Google/logout | ~8 |
| 2 | `api` + `domain` + `data`, ports and app adapters, tests | ~35 |
| 3a | `impl`: login and password recovery (view models, screens, entries, tests) | ~30 |
| 3b | `impl`: registration (three steps) | ~25 |
| 3c | `impl` androidMain: Google sign-in launcher | ~10 |
| 4 | App connection: renderers, Koin, Navigator callbacks, Legacy default for iOS | ~15 |
| 5 | Retroactive audit | docs only |

## (5) Owner decisions needed

| # | Question | Recommendation |
|---|---|---|
| Q1 | One owner `feature/auth` including registration and password recovery, package `com.coffeepeek.feature.auth`, prefix `auth_` | Yes |
| Q2 | The data layer writes the session through `core/session` `SessionStore` (no legacy `SessionRepository`) | Yes (step 4 "Writers" of the session plan) |
| Q3 | `UserSessionCleaner` stays legacy behind a `LocalUserDataCleaner` port until legacy favorites go away | Yes |
| Q4 | Sandbox runs the real repository against new auth routes of the fake backend, with failure chips; Google/Apple stay blocked | Yes |
| Q5 | Android Google sign-in lives in `impl/androidMain` (Play services dependency moves with it), configured through `GoogleSignInConfig`; Apple stays iOS/legacy | Yes |
| Q6 | iOS keeps the legacy auth screens through Legacy renderers until iOS migrates | Yes |
| Q7 | After login keep landing on `Main` (parity); "return to where the gate was shown" becomes a separate product change | Yes, keep parity |
| Q8 | Replace the global loading overlay and global error dialog by a loader on the primary button and inline errors (migrated features cannot use app singletons) | Yes, as a deliberate improvement; confirm |
| Q9 | Keep the registration error priority exactly as is (C4), write it down as a test first | Yes; the priority can be fixed later with approval |
| Q10 | `SessionRealtimeManager` and `requireAuthSession` stay outside Auth; Auth only provides `SignOut` for the profile | Yes |
| Q11 | Analytics events (`sign_in_completed`, `sign_up_completed`, `auth_gate_shown`, consent) keep names and properties, behind `AuthAnalytics` | Yes |
| Q12 | Texts verbatim (incl. both password minimums: 6 for login, 8 for registration) into `auth_` resources; `GuestAuthCard` stays an app component | Yes |

## Step 0 result — Sandbox account fixtures (2026-10-21, branch `feature/auth-sandbox`)

- The fake backend serves `POST /api/Tokens` (login), `DELETE /api/Tokens` (logout), `/api/Tokens/google/*` and `/api/Tokens/apple/*` (blocked, 503), `POST /api/Users` (register), `GET /api/Users/exists` and
  `POST /api/Users/password/forgot`. `SandboxAuthRepository` is deleted: the Sandbox now runs the real `AuthRepositoryImpl`, `AuthService` and `PasswordRecoveryRepositoryImpl` against the fake backend.
- New chip row «Вход и регистрация» and launch extra `sandbox_auth`: `Works` (default), `WrongPassword` (401 «Invalid credentials»), `EmailTaken`, `RegistrationFailure` (400), `RecoveryFailure` (500).
  Offline and Server error from the scenario chips also reach sign-in, so the offline and 5xx login cases need no extra chip. `reset` returns the policy to `Works`.
- The login answers with a JWT whose subject is the Sandbox account (`11111111-1111-4111-8111-111111111111`), so the local session and the signed-in one share a user id.
- Test: `SandboxApplicationBackendTest.accountFixturesAnswerTheRealAuthServiceAndRecoveryRepository` (the class has 19 tests, 0 failures) drives the real `AuthService` and recovery repository through every policy and the scenarios,
  and checks that the wrong-password message maps to «Неверный email или пароль» through the legacy `localizedLoginError`.
- Verified: `SandboxApplicationBackendTest` 19, `assembleDirectSandbox`, the device run below.
- Device check, Pixel 7 / Android 17, `com.coffeepeek.sandbox` only, data cleared per run, guest mode, launch extra `sandbox_auth` (screens opened from Settings → «Войти»):
  - *Works*: empty/invalid fields show «Введите корректный email» and «Пароль должен содержать как минимум 6 символов» together (A2); valid credentials sign in, the app lands on the search tab, the Profile tab appears and the guest card is gone (A5, F3).
  - *WrongPassword*: inline «Неверный email или пароль» above «Забыли пароль?» (A4).
  - *Offline* (scenario chip, with the account policy on `Works`): inline «Не удалось войти. Проверьте email и пароль», i.e. the generic text, not the network text of `localizedLoginError` (see finding 2 below).
  - *EmailTaken*: registration step 1 shows «Этот Email уже занят» under the field (C2).
  - *RegistrationFailure*: the global dialog «Что-то пошло не так» with the **raw server message** («Registration rejected») and «Понятно».
  - *Works* registration: reaches «Проверьте почту — Мы отправили ссылку на new@example.com. Перейдите по ссылке чтобы активировать аккаунт. Ссылка действует 10 минут.» with «На страницу входа» (C5).
  - *RecoveryFailure*: «Не удалось отправить письмо. Проверьте соединение и попробуйте снова.» under the field; *Works*: the EmailSent text with «Вернуться ко входу» and «Указать другой email» (D2).
  - Social buttons and the "or" divider are hidden because Google is not configured in the Sandbox (A8).

### Findings from the device run (add to the inventory rows above)

1. **C7 — the registration submit is gated.** On the Details step the primary button looks disabled and ignores taps until the name is filled, the password has 8+ characters and the terms are accepted; the "one global message" validation of C4 is therefore not reachable from the UI. It must be characterized with the view model, and the gating kept.
2. **A4 note — offline mapping.** An offline request (an `IOException` in the Sandbox) reaches the user as «Не удалось войти. Проверьте email и пароль»: the exception text carries none of the words `localizedLoginError` treats as network. A real offline failure may word its message differently; step 1 should pin down what `AuthService` produces for an I/O failure before the mapping is rewritten.
3. **C8 — password strength meter.** Under the registration password there is a three-segment meter with a label («средний» for `password1`).
4. **C9 — global error dialog.** The legacy base class shows the raw server message (`ApiException.message`) in a dialog with a mascot titled «Что-то пошло не так» and a «Понятно» button; the migrated owner must decide what to show (see Q8).
5. **A9 — system password manager.** After a successful sign-in and after a successful registration Android offers to save the password in Google Password Manager. The prompt is a system dialog and was dismissed with ✕ (nothing was saved); it comes from the autofill framework, so the migrated fields should keep the same autofill hints (username/email and password) or consciously change them.
6. **Layout (owner request, recovery screen).** On «Восстановление пароля» (no mascot) the card starts right under the wordmark and almost touches the close button and the CoffeePeek logo. The card needs a gap below the header; the migrated screen must keep that gap, and the legacy scaffold gets the same fix on its own branch.

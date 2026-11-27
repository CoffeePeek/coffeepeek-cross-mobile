# Home check-ins

Reviewed against `develop` on 2026-11-27. The feature is active on Android and iOS;
older foundation/activation and UI-fix ledgers are superseded by this guide.

## Ownership and entries

`api/domain/data/impl` own Home preparation read/write contracts, validation,
detail/editor MVI, the Shop/Home creation chooser and guest onboarding. Public
entries include `HomeCheckInDetailEntry`, `HomeCheckInEditorEntry`,
`CheckInCreatePickerEntry` and `GuestCheckInOnboardingEntry`. Android uses shared
Compose; iOS uses native editor/detail presentation over the same Kotlin owners.
Application composition owns navigation, the account-bound destination lifetime,
configured transport, photos, brew-method catalogue and analytics adapters.

Global creation and personal-history edit reuse these entries. A destination
retains its draft across its supported recreation and clears private data on
account replacement/final close. A same-account token renewal or recoverable
failure preserves the draft. Pending saves prevent conflicting dismissal or
replacement; accepted saves refresh/close once. Stale results cannot target a
replacement account. The chosen creation flow receives one consent-aware tracker,
so chooser recomposition does not duplicate attempt/start/created events.

## Form and transport contracts

- The backend uses `/api/v1/check-ins/home`, its detail/calendar routes and
  `/api/Photos/check-in` upload preparation. Data owns DTOs, signed upload handling
  and response/error mapping. Signed upload transport is credential-free; it
  does not inherit API authorization headers.
- Bean selection is mutually exclusive: catalogue slug, supported legacy product
  ID during edit, custom bean name or shelf bag. `HomeCheckInBean` is the narrow
  cross-feature input. A new creation must not submit a legacy product ID.
- The editor supports preparation date/time with offset, brew method, drink or
  custom drink, optional dose/duration/rating/note and ordered visit photos.
  `HomePreparationValidation.kt` is the source of limits: note 1000 characters,
  custom bean 250, custom drink 1–100, rating 1–5 and at most five valid,
  nonduplicate image attachments. Date, dose and duration validation is shared.
- Brew methods come from the application `BrewMethodCatalog` adapter under
  `composeApp/.../di/catalog`, not from a nonexistent catalogue feature module.
  Preserve saved methods/options missing from a refreshed catalogue. Existing
  edits cannot replace photos through the current write contract; do not infer
  upload support for edit from the create UI.
- Photos remain ordinary ordered gallery photos. Retired drink-photo ML cutouts,
  extraction/selection/retry/skip steps and extra PNG uploads must not return.
  Preview/detail/original representations use the media contract, not fabricated
  file URLs. [Shelf](../shelf/README.md) has its own scanner capability.
- Known rejections retain editable state. Uncertain delivery is explicit and
  blocks blind repeated submission; the client cannot promise server-wide
  idempotency. Coroutine cancellation is propagated.

## Presentation and lifecycle

Publication controls yield to the keyboard; native notes/numeric accessories,
date selection, photo-source choice and editor use the existing shared platform
ports. Native picker/camera handoffs wait for source-sheet dismissal and retain
their destination owner. Cancelling a picker returns to the form. Delayed source,
photo, success or onboarding callbacks after disposal must be ignored.
Detail edit/delete and saved/unconfirmed actions keep their existing guards.
Native modal host lifetime is independent of incidental SwiftUI recomposition.

## Source and verification

Inspect [API](api/src/commonMain), [validation](domain/src/commonMain),
[transport](data/src/commonMain) and [MVI/native sessions](impl/src/commonMain).
Application history adapters are under `composeApp/.../di/checkinhistory`;
global/native creation adapters are under `composeApp/.../di/homecheckin`.
Native views are under `iosXtool/CoffeePeek/Sources/CoffeePeek/HomeCheckInNative*`.

Unit/contract suites cover validation, read/write mapping, photo delivery,
editor/detail state and native-session lifetime. For code changes execute those
suites, affected app contracts, Android assembly/DEX checks and relevant Native
CI selectors. Manual Sandbox/native acceptance must separately cover both themes,
large text, accessibility, date/time, keyboard/footer, ordered photos, picker
cancel/retake, guest onboarding, modal/back-swipe cancellation, failure recovery
and account replacement during pending work. Real CRUD/upload/analytics acceptance
requires an authorized test setup. This docs review ran no app or device tests.

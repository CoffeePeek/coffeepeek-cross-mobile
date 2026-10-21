# Contributions: legacy inventory (step 0 of the owner migration)

Date: 2026-10-21. Base: `feature/community-app-connection` (stack tip). Read-only pass: nothing is changed in code.
Everything below was read line by line unless marked "skimmed". Android is migrated first; iOS and shared keep the legacy
screens behind `Legacy…Renderer` defaults (see `feature/LEGACY_REMOVAL.md`).

## (1) What "Contributions" means in this app

Everything a user *sends for moderation*: new coffee shops, new roasters, and suggested changes to a shop, plus the list
of what was sent and its status.

| Piece | Files (all under `composeApp/src/commonMain/kotlin/com/coffeepeek/admin/ui/screen`) | Lines |
|---|---|---|
| My contributions list (3 kinds × 3 statuses) | `contributions/MyContributionsScreen.kt` 339, `MyContributionsViewModel.kt` 174 | 513 |
| Add shop wizard (5 steps), success screen, map picker | `addshop/AddShopScreen.kt` 836, `AddShopViewModel.kt` 498, `AddShopSuccessScreen.kt` 198, `PickShopLocationDialog.kt` 341 | 1873 |
| Suggest a shop change (section list, editor for 8 sections, request detail) | `shopchange/SuggestShopChangeScreen.kt` 84 + VM 36, `ShopChangeEditorScreen.kt` 513 + VM 339, `ShopChangeRequestDetailScreen.kt` 445 + VM 39, `ShopChangeLabels.kt` 32 | 1488 |
| Add roaster form (lives in the roaster package) | `roaster/AddRoasterScreen.kt` 486, `AddRoasterViewModel.kt` 157 | 643 |
| Shared submission UI | `ui/component/SubmissionStatusContent.kt` (sending / error / success shells), `ui/component/PhotoAttachmentsSection.kt` | not counted |
| **Not Contributions** (the old map assigned `review` here) | `review/ReviewReportScreen.kt` 59 + VM 63 (report of a shop review or a check-in), `review/ConsumedDrinkField.kt` 93 (legacy review form) | 215 |

Tests today: `composeApp/src/commonTest/.../ui/submission/SubmissionViewModelTest.kt` (5: shop draft kept on failure and retry,
roaster result kept and no resubmit, created/updated change stays until the user leaves, menu photos without drinks, thrown
errors stop the loader in all three forms), `androidUnitTest/.../contributions/MyContributionsUiStateTest.kt` (2: tab
visibility), instrumented `ShopMenuEditorTest` (not run: no UI tests are added or run). No test for schedule conversion
(`localSchedulesToUtc` in `modules/data`), photo upload, or the list mapping.

## (2) Entry points (who opens what)

| From | To | Note |
|---|---|---|
| Own profile top menu («Правки кофеен», «Отправленные кофейни», «Отправленные обжарщики») | `MyContributions(kind)` | migrated Profile calls `onOpenContributions(ProfileContribution)`, `AndroidOwnProfileScreenRenderer` maps it to the legacy route |
| Settings «Добавить кофейню» / «Добавить обжарщика» | `AddShop` / `AddRoaster` | migrated Settings (`AndroidSettingsScreenRenderer`); **no session check** here or in the screens, a guest sees the form and the server rejects it |
| Discovery (`FeedScreen` `AddMissingShopCard`) | `AddShop` | legacy Discovery, also used by iOS |
| Shop detail «Предложить правку» | `SuggestShopChange(shopId)` | migrated Shop emits `ShopDetailEffect.SuggestChange`; a guest is asked to sign in first (checked in the Shop view model) |
| `AddShop` step 4 «Добавить обжарщика» | `AddRoaster` | form state is kept while the roaster form is open (view model is route scoped) |
| `SuggestShopChange` → `ShopChangeEditor(shopId, section, requestId)` → `ShopChangeRequestDetail` | | the editor pops itself and opens the detail after a successful send |
| Contributions list item | `ShopDetail(slug)` or `ShopChangeRequestDetail(id)` | roasters have no link (`ModerationRoasterDto` carries no published id) |
| `ShopChangeRequestDetail` | `ShopDetail(shopId)` | |

Analytics: only the page-type mapping in `analytics/AnalyticsVisibility.kt` (`MyContributions`, `ShopChangeRequestDetail` →
`contributions`; `SuggestShopChange`, `ShopChangeEditor`, `AddShop` → `shop_edit`). The view models send no events; `AddRoaster`
has no page type (falls into `other`).

## (3) Behaviour to preserve (rules)

### A. My contributions
- A1 Three kinds with titles and empty texts: `Отправленные кофейни / Вы ещё не добавляли кофейни`, `Отправленные обжарщики / Вы ещё не добавляли обжарщиков`, `Мои правки / Вы ещё не отправляли правки`.
- A2 The first page (size 20) of **all three** statuses is loaded in parallel; tabs exist only for statuses that have items, fixed order Опубликовано / На модерации / Отклонено, label `«<title> · <totalCount>»`. Any failing request fails the whole screen with its message (default `Ошибка загрузки`) and a retry.
- A3 Per-tab paging: `hasMore = currentPage < totalPages`, load more when the last visible item is within 3 of the end, one load at a time, a failed page only clears the spinner.
- A4 Item mapping: shop → title name, subtitle address, meta `Скоро появится в каталоге` when approved without a published shop, rejected reason, target `ShopDetail(slug)` only when published; roaster → name, about (3 lines), reason, no target; change → section title, summary (see A5), local date-time, reason, target detail.
- A5 Change summaries: description (or `Описание очищено`), contacts `Телефон, почта, сайт и Instagram`, photos `Фото: N сохранено · M добавлено`, counts for tags/roasters/equipment/brew methods, menu `Меню: N позиций · M новых фото`, else the section hint.
- A6 Card look: icon badge per kind/section, status pill (Готово / Проверяется / Отклонено), red «Почему отклонено» block, footer `Открыть в каталоге` / `Посмотреть заявку` only with a target.
- A7 The selected tab survives rotation (`rememberSaveable`), falls back to the first visible tab.

### B. Add shop wizard
- B1 Steps BASIC → CONTACTS → PHOTOS → FEATURES → SCHEDULE with a dots header, back on the first step leaves the form, slide animation by direction.
- B2 BASIC: name required ≤55 (input is cut at 55), address required, city required; a single available city is chosen automatically, otherwise the city is guessed from the address text; description, price range 1–4 (`Cheap/Moderate/Expensive/Luxury`).
- B3 Address: typing an address clears the coordinates; **current location** (permission effect, `getLastKnownLocation`, reverse geocode) fills the address only when it is empty unless forced; hints for no location / no address / denied; **map picker** dialog returns point + address and may set the city.
- B4 CONTACTS: optional phone, e-mail, site, Instagram with the shared validators; the step cannot continue with an error.
- B5 PHOTOS: shop photos up to 10, menu photos up to 4, remove by index, silently ignores extra.
- B6 FEATURES: multi-select equipment, beans, roasters, brew methods from the catalogs; the roaster block links to `AddRoaster`.
- B7 SCHEDULE: unified open/close with 30-minute steppers (clamped to 00:00–23:59), presets, closed days; optional per-day schedule seeded from the unified one; submitted as seven days (Sunday = 0) converted from local to UTC in data.
- B8 Catalogs load first (loader, error + `Повторить`); submit is blocked while loading, invalid, submitting or done, and uses a compare-and-set so a double tap sends once.
- B9 Submitting shows a status screen; a failure shows `Кофейня не отправлена` with the message plus `Введённые данные остались в форме.` and actions (retry / back to the form); the message is cut before `Expected type` and defaults to `Ошибка отправки. Попробуйте выйти и войти снова.`; cancellation clears the spinner and is rethrown.
- B10 Success screen shows name, city, address and the first photo; `onGoHome` pops back.
- B11 Data: shop and menu photos are uploaded to presigned URLs in parallel (menu limited to 4), then one `createShop` request with the contact object only when any contact is filled.

### C. Suggest a shop change
- C1 `SuggestShopChange` shows the shop title and eight section rows (Фотографии, Контакты, Описание, Особенности, Обжарщики, Оборудование, Меню, Методы заваривания) with hints, each opens the editor.
- C2 The editor loads catalogs, shop details and the drink catalog, prefilling the chosen section from the current shop; with a `requestId` it also loads the existing request and overlays its payload (edit of a pending/rejected request); any load failure shows the message.
- C3 Per-section validation: description ≤1000; contacts via validators; photos ≤10 in total (retained + new + already uploaded); menu photos ≤4; other sections always valid. Submitting needs not loading, not submitting and no earlier result.
- C4 Photos: retained photos can be removed, new ones added up to the limit; already uploaded photos of an earlier request are kept (no second upload); menu: availability per drink (Present/Absent/Unknown) and a price (comma allowed).
- C5 Create or update (`PUT` when editing) uploads new photos first (menu limited to 4), sends the payload of the chosen section only; failure keeps the form with `Не удалось отправить заявку` or the server message.
- C6 After success the editor pops and opens `ShopChangeRequestDetail(id)`; the detail shows the hero (section, status, date), the rejection card, the payload summary per section and a button to the shop.

### D. Add roaster
- D1 Name required ≤100, about optional, city and address are a pair (one without the other is an error: `Для адреса выберите город` / `Для выбранного города укажите адрес`), Instagram accepts a handle or a URL (normalized to `https://instagram.com/<handle>`), site is a URL, up to 5 photos.
- D2 City catalog loads first (a failure shows `Не удалось загрузить города` but the form stays usable only without a city).
- D3 Submit is guarded like the shop (compare-and-set, no resubmit after a result); the success content shows the server message and whether the address was validated; a failure keeps the form (`Не удалось отправить обжарщика на модерацию`).

## (4) What the legacy code depends on (to be replaced by ports)

| Dependency | Used for | Plan |
|---|---|---|
| `ShopRepository` (`getCatalogs`, `getShopDetails`, `getMenuDrinks`, `getMyShopSubmissions`, `createShop`) | catalogs, prefill, list, create | reads of details/catalogs/drinks already have migrated owners (`feature/shop` data); Contributions gets its own **write + list** repository and reads what it needs through a narrow port, not the legacy interface |
| `RoasterRepository` (`submitRoaster`, `getMyRoasterSubmissions`) | roaster form and list | write/list part moves to Contributions data (the roaster migration left them here on purpose) |
| `ShopChangeRequestRepository` (`create`, `getMine`, `getById`, `update`) | all change screens | moves as is, own models |
| `PhotoRepository` presigned upload (`uploadShopPhotos`, `uploadMenuPhotos`, `uploadRoasterPhotos`) | all three forms | needs a small uploader in Contributions data (same two-step flow: request URLs, PUT bytes, keep storage keys); the Shop owner has its own for check-ins, a shared one is the future "core media/upload" candidate (see the local shared-extraction plan), not part of this migration |
| `PlatformLocation`, `LocationPermissionEffect`, `CoffeeMap`, `PlatformMapControlButton` | current location, map picker | app-owned host slots (location + map are Discovery/core concerns); Contributions asks for `pickLocation` and `currentAddress` through ports |
| `PhotoAttachmentsSection`, `PickedImage`, `ImagePicker`, `MAX_*_PHOTOS` | photo pick and previews | app-owned picker slot (platform), limits become constants of the feature |
| `validateOptional*`, `utcIsoToLocalDateTime`, `Navigator`, `BaseViewModel`, legacy Cp design system | validation, dates, navigation, UI | validators move into `domain`, navigation becomes callbacks, UI on `core/design-system` |
| `SessionRepository` | none inside these screens (guards missing, see open question Q2) | port `ContributionsAccount` only if Q2 adds guards |

## (5) Findings worth deciding before the code moves

1. **No guest guard** for `AddShop`/`AddRoaster`: a guest can fill the whole wizard and fails at the end (the shop error hint says to sign out and in). Shop already guards `SuggestChange`.
2. **Raw error text**: shop submit and list show `error.message` (with a string cut `Expected type`), roaster and change forms show `message` or a default. Migrated owners show the server message or a default only.
3. **Hard-coded Russian strings** in every screen and view model (labels, hints, errors): they move to string resources.
4. **Menu photos limit** lives twice (`MAX_MENU_PHOTOS` 4 in the UI and `take(4)` in data); extra photos are dropped silently in data.
5. **`ShopChangeEditor` can send an unchanged payload** (tags, roasters, equipment, brew methods are always valid) and an empty-cleared description is a valid change (`Описание очищено`): confirm this is intended.
6. **No drafts**: the wizard state lives in the route-scoped view model; process death loses it (same as legacy today).
7. **Schedule conversion** (`localSchedulesToUtc`) has no test; the wizard sends seven days even when the user touched nothing (09:00–21:00).
8. **Roasters in the list have no link**, approved roasters are indistinguishable from published ones.
9. `review/ReviewReportScreen` and `ConsumedDrinkField` are used by the legacy Shop review flow and legacy check-in report, not by Contributions.

## (6) Decisions to confirm (recommendation first)

| # | Question | Recommendation |
|---|---|---|
| Q1 | Module shape | one owner `feature/contributions/{api,domain,data,impl}` covering list, add shop, add roaster, change suggestions (shared forms, photo sending and status shells) |
| Q2 | Guest behaviour | add a guard: the entries ask for sign-in through a port (`onSignIn`) instead of showing the form to a guest; the list and details already need a session |
| Q3 | Review report / consumed drink field | out of scope, stay legacy until iOS migrates Shop (`feature/LEGACY_REMOVAL.md` row corrected) |
| Q4 | Error texts | server message or a resource default, never the raw transport message |
| Q5 | Location and map | app-owned slots (current address, pick on map) so the owner does not own a map SDK; the picker stays the existing `CoffeeMap` dialog on Android |
| Q6 | Photo upload | a minimal uploader in Contributions data now; the shared uploader is a later extraction |
| Q7 | Entry API | entries: `ContributionsListEntry(kind)`, `AddShopEntry`, `AddRoasterEntry`, `SuggestChangeEntry(shop)`, `ChangeEditorEntry(shop, section, request)`, `ChangeRequestDetailEntry(id)`; callbacks for shop, sign-in, add roaster, back |
| Q8 | Slicing | inventory (this PR), Sandbox fixtures, parity tests, api+domain+data, impl list+detail, impl add shop, impl add roaster+changes, app connection + device check, audit — each ≤40 files |

## (7) Plan and PR slicing (each step on its own stacked branch)

0. Sandbox: fake `moderation/shops|roasters|…/mine` lists (all statuses, paging, rejected reasons), `/api/ShopChangeRequests` create, update, get and `/mine`, `shops` create, `roasters` submit, photo upload URLs, failure fixtures; unit tests like the feed ones.
1. Parity tests on the legacy code for the gaps listed in (1) and (5): list mapping/summary, schedule conversion, wizard step rules, change payload per section, roaster rules.
2. `api + domain + data` (own models, repositories, uploader, mappers, tests).
3a. `impl`: list, change request detail. 3b. `impl`: add shop wizard. 3c. `impl`: add roaster + change editor.
4. App connection (renderers/entries, ports, Koin, Profile/Settings/Discovery/Shop callers) and a Pixel 7 Sandbox check.
5. Retroactive audit and the legacy map update (nothing is deleted before iOS migrates).

## (8) Not checked in this pass

Live API contracts (paths and DTOs of `moderation/*`, `shop-change-requests`), image upload to the real storage, camera, real location and map, iOS behaviour, large font, TalkBack. They stay explicit gates for the later steps.

## Step 0 result (Sandbox fixtures)

- **Added**: `SandboxContributionsBackend` (`ModerationShops`, `ModerationRoasters`, `ShopChangeRequests`: lists by status with paging and filters, create/submit with validation, read and edit of a change request, edit refused for an approved one), equipment/bean/shop-tag catalogs, the `SandboxContributions` modes (`Seeded`, `Paged`, `Empty`, `ListFailure`, `SubmitFailure`), the controls row «Мои вклады» and the launch extra `sandbox_contributions`; `SANDBOX.md` describes them.
- **Tests**: `SandboxApplicationBackendTest` +1 (25 in total) runs the real `ShopApiService`, `RoasterApiService` and `ShopChangeRequestApiService` against the fixtures: lists per status, published vs waiting shops, rejected reasons, filters, submissions appearing as pending, server messages for invalid input, address/city pairing for roasters, edit rules, catalogs, paging (47 items, 3 pages), empty and failure modes, reset.
- **Device check (Pixel 7, Sandbox, dark theme)**: profile menu -> «Отправленные кофейни» shows the three tabs with counts, the published shop with «Открыть в каталоге», the waiting one with «Скоро появится в каталоге», and the rejected one with the red reason block (legacy screen, nothing migrated yet).
- **Not covered**: the real moderation decisions, photo key checks, real map/location, the wizard, roaster and change forms on a device (the step 1 and later checks).


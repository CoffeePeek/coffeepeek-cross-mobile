# Community (the «Лента» tab) — legacy-first inventory and plan — 2026-10-22

Method: the rule in [ANDROID_REFACTORING_PLAN.md](../ANDROID_REFACTORING_PLAN.md#legacy-first-parity-gate-mandatory-for-every-owner), format as in [../auth/LEGACY_INVENTORY.md](../auth/LEGACY_INVENTORY.md).
Read-only analysis of the legacy code on `feature/community-inventory` (stacked on #136): no code changed, Gradle not run, nothing tried on a device. `CommunityViewModel`, `CommunityScreen`,
`TimelineCheckInCard`, `EditCheckInSheet`, `FeedRepositoryImpl`, `CheckInRepositoryImpl`, `CheckInTimelineContentImpl` were read line by line; the three test files were only listed by test name.
Every row is **open** until the owner confirms it and ends as **preserved**, **improved (how)** or **intentionally removed (why, with approval)**.

There is **no `feature/community` module yet**; this directory holds only this file. Not to be confused with `ui/screen/feed` (`FeedViewModel`, the search/discovery tab): that is the **Discovery** owner.

## Scope and sizes (legacy, lines)

| Area | Lines |
|---|---|
| `feature/community/ui`: `CommunityViewModel` 428, `CommunityScreen` 206, `TimelineCheckInCard` 74, `EditCheckInSheet` 62 | 770 |
| Shared presentation used by it: `feature/checkin/{api,ui}/CheckInTimelineContent` 34 + 82, `ui/component/CheckInActions` 40, `ReviewContent` pieces (`CheckInHeader`, `CheckInHeading`, `HelpfulButton`, `ReviewPhotoStrip`, `SavedDrinkLabel`, `ReviewRatingCards`, `ReviewTextInput`), `ConsumedDrinkField`, `FullScreenImageDialog`, `SwipeDismissModalBottomSheet`, `LoadErrorContent`, `CoffeePeekPullToRefresh`, `PrefetchListPhotos` | ~(skimmed) |
| Legacy layers: `modules/domain` `FeedRepository` (+`FeedFilters`, `FeedItem`, `FeedPage`, `FeedLoadException`) and `CheckInRepository` (`getMyCheckIns`, `updateCheckIn`, `setVisibility`, `setHelpful`, `report`, also `createCheckIn`/date range used by other owners); `modules/data` `FeedRepositoryImpl` 26, `CheckInRepositoryImpl` 105; `modules/network` `FeedApiService`, `CheckInApiService` | ~200 |
| Tests today (names only): `CommunityViewModelTest` 5 (guest/logout, edit keeps drink + pending version, failed save keeps draft, pagination retry/dedupe, late response of a previous account), `PublicCommunityViewModelTest` 9 (anonymous order, cursor retry, filters reset and stale response, initial session vs chosen timeline, invalid cursor, canonical owner and pending version, votes/own vote/report draft, blocking waits for ownership ×2), `TimelineCheckInPresentationTest` 2, `FeedRepositoryContractTest` (data), `AuthorVisibilityFeedTest` | ~520 |

## Who uses it

- Android: `MainScreen.kt` `composable<CommunityTab> { CommunityScreen(onCreateCheckIn = navigate(CreateCheckIn)) }`. iOS: `PlatformRootNavigation.ios.kt` tab 2 calls the same composable. Koin: `factory { CommunityViewModel(get() ×5, visibility) }` (`KoinApp.kt`).
- It reuses already migrated owners through their legacy-compatible seams: the check-in creation sheet (`CheckInCreateFlow`, `CheckInCreateFlowRenderer`), the Home editor route `EditHomeCheckIn(id)`, the author-blocking api (`AuthorVisibility`, `blockableAuthor`/`CheckInActions`), `UserProfile`, `ShopDetail`, `Auth` routes.
- `CheckInTimelineContent` / `CheckInActions` are shared with the legacy public profile (`PublicProfileCards`, kept for iOS) and `ReviewContent` (legacy Shop); the migrated owners (history, shop, profile) have their own cards.

## (1) Inventory of behaviour to keep

### A. Timeline load and paging

| # | Behaviour |
|---|---|
| A1 | Public feed (default): `FeedRepository.getFeed(20, cursor, filters)`; items keep the **server's publication order**, a page is appended by id without reordering (existing items are updated in place), `publishedAt` kept per item; `hasMore` = `nextCursor != null` |
| A2 | Mine timeline: `CheckInRepository.getMyCheckIns(page, 20)`, `hasMore = currentPage < totalPages`; allowed for a signed-in user only (a guest asking for it gets the sign-in request) |
| A3 | Initial state `isLoggedIn = null` shows the loader until the first session emission; a session change (login, logout, other account) cancels every job, resets the whole state with a new `sessionGeneration`, keeps `filters`, falls back to Public when logged out, and reloads; late answers for a previous account or generation are dropped (`isCurrent`, generation checks everywhere) |
| A4 | Pull to refresh = reset load (`FEED_ACTION_CLICKED refresh`, trigger `gesture`); auto-load-more when the last visible item is within 3 of the end (`load_more`, trigger `scroll`); retry button: first-page failure refreshes, page>1 failure loads more (`retry`), except `restartPagination` (invalid cursor) which restarts from the first page with the same filters and a different label «Обновить ленту» |
| A5 | Failure text `Не удалось загрузить ленту`; `FeedLoadException.restartPagination` comes from the data layer; loading/loading-more/empty states; empty text `Пока нет опубликованных чек-инов.` |
| A6 | Author blocking: Public timeline waits for the visibility snapshot (`visibility.refresh()`, failure → `Не удалось загрузить блокировки. Повторите попытку.`), hides items of blocked authors (`hides(authorId, slug)`), and when the snapshot changes reloads from scratch (dropping the first emission) |
| A7 | Own items: `owns(checkIn)` = signed in and (Mine timeline or the item's author slug equals the canonical own slug from `users.refreshProfile()`); `canBlockAuthor` only on Public, guests may open the sign-in prompt, signed-in users only after the own slug is known and for others' items |
| A8 | Scroll resets to the top and the photo preview closes when the generation changes; the Home editor opened from an item refreshes the list on `ON_RESUME` |

### B. Item actions

| # | Behaviour |
|---|---|
| B1 | Helpful (like): only on Public, not for own items, blocked while any mutation/edit/report is open; guest → `Analytics.gate("feed_like")` + sign-in; request `setHelpful(id, !current)`; result sets the **authoritative** `helpfulCount`/`isHelpfulByCurrentUser`; failure → snackbar with the message or `Не удалось изменить отметку`; analytics `FEED_ITEM_INTERACTED like/unlike` |
| B2 | Report (Public, not own): guest → sign-in; bottom sheet «Пожаловаться на чекин», text 1..2000 characters (input is cut at 2000), error `Опишите проблему: от 1 до 2000 символов`, submit → `report(id, text)`, success → snackbar `Жалоба отправлена`, failure keeps the draft and shows the message |
| B3 | Visibility (own): Public → Private hides it via `setVisibility`; a private own item shows «Опубликовать»; the updated item replaces the card; a Public item that becomes private or not `Approved` **leaves the public feed** and stays in Mine; failure → snackbar `Не удалось изменить видимость` |
| B4 | Edit (own shop check-in): bottom sheet with ratings (coffee/service/place), text (≤1000), consumed drink (`ConsumedDrinkField`, drinks from `shops.getConsumedDrinks()`, retry on error `Не удалось загрузить напитки`); validation by `validateCheckInContent`; save → `updateCheckIn`; success snackbar `Чек-ин сохранён и отправлен на проверку. Он доступен в профиле, в разделе «Чекины»` on Public, `Чек-ин сохранён` on Mine; failure keeps the draft and shows the message or `Не удалось сохранить чек-ин`; the saved drink name stays when the drink is unchanged; analytics through `CheckInTracker(Visit, Diary, edit = true)` (editor step, field changes note/rating/drink, submit, validation, saved, failed, cancel) |
| B5 | Edit of a **home** check-in opens the Home editor route (`EditHomeCheckIn(id)`), not the sheet |
| B6 | Only one mutation at a time (`isMutating`); timeline, filter, refresh and load-more are blocked while mutating or while a sheet is open; each mutation cancels the running load |
| B7 | Author name/avatar → `UserProfile(slug, name, avatar)` when the slug is known; shop name → `ShopDetail(shopId)` (`itemAction("shop")`); photo → full-screen viewer (`itemAction("photo")`) |

### C. Presentation

| # | Behaviour |
|---|---|
| C1 | List: heading «Лента», cards separated by dividers, `contentType` home/shop, keys by id, photo thumbnails prefetch (first 3 per item), bottom padding = floating-nav clearance + 88 dp |
| C2 | Card (`CheckInTimelineContent`): header (author, own mark, actions), shop heading with rating or home recipe (bean name + `method · dose г · time с · taste/5`), note, photo strip (detail urls, thumbnails), supporting text (publication label only on Mine; rejection reason for own rejected items), footer (helpful button + count, private «Опубликовать», saved drink label); the helpful count is shown on Public or when > 0 |
| C3 | Publication labels: Приватный / На проверке / Опубликован / Отклонён / Публичный / Статус проверки неизвестен |
| C4 | Floating create button (`CheckInCreateButton`): guest → `CHECK_IN_START_ATTEMPTED source=global_create` + gate + sign-in; signed in → `CheckInCreateFlow` (shop → `CreateCheckIn` route, home saved → refresh) |
| C5 | Analytics: `FEED_VIEWED` (first page count, public only), `analyticsExposure` for the list and per card kind (`feed_home_check_in`/`feed_shop_check_in`), `FEED_ITEM_INTERACTED` (like, unlike, shop, photo; `author` and `home_detail` are allowed values but **not emitted** today), `FEED_ACTION_CLICKED` (refresh, load_more, retry) |

## (2) Gaps and quirks found by reading

1. **Mine timeline and filters are unreachable from the UI.** `selectTimeline` and `setFilters` have no caller in `CommunityScreen`; only the tests (and `needsLogin` for Mine) exercise them. The screen is Public feed only. Decision needed (Q3).
2. `itemAction("author")` and `home_detail` are never called although the analytics schema allows them (author click navigates without an event).
3. All user texts are hard-coded Russian strings in the view model (`Не удалось загрузить ленту`, snackbar texts); the migrated owner moves them to Android resources with typed message kinds (testable, as in Auth).
4. `CommunityViewModel` depends on legacy `SessionRepository` (`observeSession`, `peekSession`, `isActiveSession`), `ShopRepository.getConsumedDrinks`, `UserRepository.refreshProfile` and the global `Analytics`; a migrated module must take ports for each.
5. `CheckInTimelineContent` is shared with legacy screens that stay for iOS: the migrated card must be a copy in `impl`, not a move.
6. `CheckInRepositoryImpl` mixes five owners' methods (`createCheckIn` Shop/Home, date-range read History, `getMyCheckIns` paging Community/History, edit/visibility/helpful/report). Shop already has its own helpful-vote backend (`ShopCheckInVoteBackend`); Community needs a smaller data layer for feed + mine + edit + visibility + helpful + report rather than the shared repository.
7. Sandbox already serves `/api/v1/feed`, `/api/v1/check-ins/mine`, helpful/report/visibility/update routes, protected photos (history/profile fixtures); there is **no Community-specific fixture set** (several pages, cursor failures, own vs others, pending/rejected items, blocked author in the feed, offline/error scenarios for the feed).

## (3) Target structure

`feature/community/{api,domain,data,impl}`, package `com.coffeepeek.feature.community`, prefix `community_`.

- **domain**: models (`TimelineItem` or reuse of a feed check-in model owned by this feature, `TimelineKind`, `FeedFilters`), `CommunityRepository` (feed page, my page, update, set visibility, set helpful, report), pure rules (`owns`, `canBlockAuthor`, edit validation message, publication state, withdrawn-from-public rule), failure kinds (`restartPagination`).
- **data**: `createCommunityRepository(client, ...)` on the authenticated client; DTOs/mappers for feed items and check-ins (the file-url resolution of `FileUrlResolver` becomes a port); contract tests ported from `FeedRepositoryContractTest` and the check-in repository tests.
- **api**: ports `CommunityAnalytics` (events above), `CommunityAccount` (signed-in key flow, canonical own slug), `ConsumedDrinks` (drink options), `CommunityAuthors` (visibility snapshot + block action, adapters over the author-blocking api), `CommunityEntry` (Android Compose entry with callbacks: `onSignIn`, `onOpenAuthor`, `onOpenShop`, `onEditHome(id)`, `onCreateShopCheckIn`, `createFlow` slot, `photoViewer` slot, `bottomClearance`).
- **impl**: typed MVI view model (`CommunityState/Action/Event`), screen, card, edit sheet, report sheet on `core/design-system`, strings.
- **app**: `CommunityScreenRenderer` with `LegacyCommunityScreenRenderer` default (iOS), Android renderer, adapters (session, analytics, drinks via legacy `ShopRepository`, visibility via `AuthorVisibility`), `MainScreen` uses the renderer.

## (4) Decisions to confirm (recommendations first)

| # | Question | Recommendation |
|---|---|---|
| Q1 | One owner `feature/community` for feed + own timeline logic, own data layer instead of the shared `CheckInRepository` | Yes |
| Q2 | Session through `SessionReader` ports (no legacy `SessionRepository`), identical per-account generation logic | Yes (same as Auth/Settings) |
| Q3 | Mine timeline and filters have no UI today | Carry the view-model logic and tests (parity), **do not add UI**; remove only with approval later |
| Q4 | Card: a new card in `impl` on design-system (copy of the legacy look), legacy `CheckInTimelineContent` stays for iOS/legacy profile | Yes |
| Q5 | Photo viewer, check-in create flow and Home editor stay app-owned (slots/callbacks), as in Home/History | Yes |
| Q6 | Texts move to `community_` resources verbatim; typed message kinds in state | Yes (improvement, testable) |
| Q7 | Emit `author` and `home_detail` item events | Keep parity (do not emit) unless the analytics owner asks |
| Q8 | Analytics names/properties unchanged behind `CommunityAnalytics` (incl. exposure and `FEED_VIEWED` trackers staying app-side as slots/modifiers) | Yes |

Decisions Q1–Q8 accepted as recommended (2026-10-22).

## (5) Plan and PR slicing (each step on its own stacked branch, ≤40 files, logically grouped)

| Step | Content | ~Files |
|---|---|---|
| 0 | Sandbox: Community fixtures (pages and cursors, own/others/pending/rejected, blocked author, failure and offline chips, launch extra `sandbox_community`) + device check of the legacy feed | ~8 |
| 1 | Parity tests on the legacy code for the gaps: refresh/load-more triggers, retry vs restart, visibility snapshot waits/failures, helpful guest gate, report limits, visibility toggle and withdrawal, home vs shop edit, analytics events | ~4 |
| 2 | `api` + `domain` + `data` (+ contract tests) | ~25 |
| 3a | `impl`: timeline list, card, load/refresh/paging, helpful, author/shop/photo callbacks | ~25 |
| 3b | `impl`: edit sheet, report sheet, visibility, create button | ~15 |
| 4 | App connection: renderer + Koin + adapters, device parity check on Pixel 7 (dark/light, large font, guest/signed-in, blocked author, offline/error) | ~12 |
| 5 | Retroactive audit and `LEGACY_REMOVAL.md` update | ~3 |

Open gates after the owner: real feed against the live backend, protected photo delivery, iOS keeps the legacy screen until it migrates.

## Step 0 result (Sandbox fixtures and device check of the legacy feed)

- **Fixtures** (`SandboxFeed`, launch extra `sandbox_feed`, chip row «Лента»): `Single` (default, unchanged: the visible public fixtures in one page), `Paged` (45 generated public posts of the blockable friend and two other authors, every fifth a home preparation, behind an offset cursor, page size honoured), `PageFailsOnce` (the first request for a later page answers 503, then recovers), `InvalidCursor` (every later page answers 400, so the app must restart from the first page), `Empty`, `Failure` (500). Blocking the friend hides their posts on every page; helpful/report routes accept generated posts. `reset()` returns the feed to `Single`; the scenario chips (offline, 5xx) still override everything.
- **Tests**: `SandboxApplicationBackendTest` +1 through the real `FeedApiService` (default page, three pages with unique ids and a home post, block/unblock across pages, votes and reports on generated posts, failing-once page recovers, invalid cursor sets `restartPagination`, empty, failure, reset). The class passes (20 tests).
- **Device (Pixel 7, legacy feed, dark theme)**: `Paged` renders the first page and loads the following ones while scrolling up to post 45; `PageFailsOnce` stops after page 1 with the shared error block and «Попробовать снова» loads the next page; `Failure` on the first page shows the same shared block («Упс, что-то пошло не так», «Сервис временно недоступен…») — the legacy text «Не удалось загрузить ленту» from the view model is **not shown** by this screen, the shared `LoadErrorContent` text is (note for the migrated owner: keep the shared look and wording).
- **Not checked on a device**: `InvalidCursor` and `Empty` visuals, large font, light theme, own-item actions in the feed.

## Step 1 result (parity tests on the legacy code)

`CommunityParityGapsTest` (9 tests, commonTest, stable over three full reruns; Direct and Play unit tests and the iOS test compile pass) pins what the 14 older tests did not reach:

- **Feed actions and analytics (A4, C5)**: `refresh("gesture")`, `loadMore` (`load_more`/`scroll`), `retry` (`retry`/`button`), no event for a refresh without a trigger, no second `load_more` event when the retry of page 2 goes through `loadMore`, no feed events on the personal timeline; item events `like`/`unlike` with `check_in_kind` shop|home, `shop`, `photo`.
- **Guest (B1)**: a guest's like gives `auth_gate_shown feed_like` and the sign-in request, `loginHandled` clears it, selecting the personal timeline as a guest requests sign-in and stays Public.
- **Blocks snapshot (A6)**: with a not-ready snapshot the public feed asks `refresh()` first; its failure shows `Не удалось загрузить блокировки. Повторите попытку.` and requests no posts; a retry after success loads them. **Quirk found**: the snapshot change published by that refresh also restarts the load, so the posts may be requested twice (the test accepts 1 or 2; the migrated owner should request once).
- **First-page failure (A5)**: `Не удалось загрузить ленту`, `failedPage = 1`, no restart flag; retry refreshes.
- **Report (B2)**: own post cannot be reported, input is cut at 2000, a blank text gives `Опишите проблему: от 1 до 2000 символов` and no request, a 2000-character text is sent as is, success clears the draft, closes the sheet and shows `Жалоба отправлена`.
- **Visibility (B3, B6)**: only own posts toggle; while a mutation runs refresh, filters, timeline, edit and another mutation are ignored; failure keeps the post and shows the server message (or `Не удалось изменить видимость`); success makes an own public post private and it leaves the public feed.
- **Edit (B4)**: ignored for another author's post and while the report sheet is open; cancel closes the editor.
- **Labels (C3)**: all six publication labels.

Still not covered by tests (UI only, to be covered by the migrated screen's tests and the device pass): the home-vs-shop edit branch (B5, a screen decision), load-more threshold (A4), photo preview and scroll reset (A8), analytics exposure modifiers.

## Step 2 result (api + domain + data)

- **Modules** `feature/community/{api,domain,data}` (`module.feature.community.*` in `Modules.kt`); no dependency on any legacy module. `impl` comes in 3a/3b.
- **domain**: `CommunityPost` (own model: author reference with opaque id and slug, photos in three sizes, drink, home preparation, visibility and moderation), `FeedFilters`/`FeedPage`/`MyPostsPage`/`HelpfulVote`/`PostEdit`, `FeedLoadException(restartPagination)`, typed `CommunityFailure(kind, serverMessage)`, `CommunityRepository` (feed, my posts, update, visibility, helpful, report). Pure rules in `CommunityRules.kt`: `validateEdit` (note, rating, drink in the legacy order), `validateReport`, `publicationLabel`, `isWithdrawnFromPublic`, `appendTimeline`, `ownsPost`, `canBlockAuthor`.
- **api** (ports, commonMain): `CommunityAccount` (account key flow, current key, own canonical slug), `ConsumedDrinks`, `CommunityAnalytics` (feed actions with trigger, item interactions, guest like gate, the edit funnel). Author blocking is not re-wrapped: `impl` takes `AuthorVisibility` and `AuthorBlockActions` from the author-blocking api (decision Q5/Q8).
- **data**: `createCommunityRepository(client, apiBaseUrl)` on the authenticated client. Own DTOs and mapper (photo order, server-issued URLs only, feed/detail variants in the legacy format order, empty feed variant stays empty, home kind with or without recipe, tolerant numbers), `CommunityBackend` with explicit status handling; user-facing texts are **not** in this layer (the screen maps `FailureKind` and `serverMessage` to resources, as the legacy screen did: server message first, else the default text).
- **Behaviour kept from the legacy layers**: `Cache-Control: no-store` on the feed, page size 1..100 and filter length 1..100 checked before the request, `restartPagination` only for a 400 with a cursor, helpful as PUT/DELETE without a body with the authoritative count, report needs a 201 with data and a trimmed text of 1..2000 characters, edit/report validated before any request.
- **Tests**: domain 8 (rules), data 16 (11 repository contract, 5 mapper); Android unit tests and iOS test compilation pass.
- **Left for 3a**: the legacy `CheckInRepositoryImpl.getMyCheckIns` page semantics are reproduced (`hasMore = page < totalPages`); the app adapters (account from the session, drinks from the legacy `ShopRepository`, analytics) are written in step 4.

## Step 3a result (impl: timeline, card, helpful, blocking)

- **api (Android)**: `CommunityEntry.Content(onSignIn, onOpenAuthor, onOpenShop, bottomClearance, floatingAction)` and `CommunityHost`, the slots the app lends the screen: authenticated image loading (with the high-quality upgrade), the relative date, brew method names, photo prefetch, the full-screen photo viewer, exposure and feed-viewed analytics. Defaults keep previews and tests free of app services. The check-in create button is the `floatingAction` slot (decision Q5).
- **impl** (`feature/community/impl`, prefix `community_`): `CommunityViewModel` (typed MVI) — one account at a time (a session change resets everything, a late answer for another account or generation is dropped), public feed with cursor paging in the server's order, personal timeline by page, refresh/load more/retry with their analytics, filters and timeline selection (logic kept, no UI — Q3), author-blocking snapshot applied on arrival and on change (a block cancels the running load, invalidates the cursor and reloads; the cursor-invalidating snapshot of the model's *own* block-list refresh no longer restarts the load a second time — the legacy double request is gone), like with the authoritative count and the guest gate, one mutation at a time.
- **UI** (`CommunityScreen`, `PostCard`): list with pull to refresh (core), loader, the shared error block (same words and mascot; both load errors and the block-list error render it, as before), empty text, load more near the end, photo preview closed on a generation change, scroll reset on a generation change, snackbar for notices; card with author header (tap opens the profile), shop or home recipe, note, 1–3+ photo layout, like button with accessible labels, drink label, overflow menu (3a: block author; edit, hide and report come with 3b).
- **Improvements**: a failed vote shows the server's own message or the default text, never the raw transport message the legacy list could show; texts are Android resources.
- **Tests**: `CommunityViewModelTest` 18 (guest feed and gates, session change and late answers, cursor retry, refresh, invalid cursor, filters and late response, first-page failure, personal timeline, ownership/blocking visibility, votes, mutation guard, item analytics, one block-list refresh and one feed request, failed block list, block during load more, blocked authors dropped). Android build of the module, iOS compile of main and tests pass.
- **Not covered**: device parity (step 4), the edit/report/visibility flows (3b), photo viewer and image loading (app slots).

## Step 3b result (impl: edit, report, visibility)

- **ViewModel** (`CommunityViewModel`, now with `ConsumedDrinks`): edit (own posts only; not while a mutation runs or a report is open; drinks loaded on open with retry; field-change analytics note → rating → drink; submit/rejected/saved/failed/cancelled funnel; a draft is kept on failure with the server's message or the default text; an invalid form is rejected before any request with a typed `EditIssue`), hide/publish (`ToggleVisibility`; an own public post made private leaves the public feed and is replaced in place elsewhere; failure keeps the post and shows the server message or the default), report (guest asks to sign in, own posts cannot be reported, input cut at 2000, blank rejected, success clears the draft and shows a notice, failure keeps the draft and shows the server message or the default). One mutation at a time; refresh, filters, timeline and like are ignored while an editor or report sheet is open; a session change closes the sheets and drops late saves.
- **UI**: edit sheet (ratings with the three photo cards, note, drink field with the "other" custom name, error line, save/cancel), report sheet, full overflow menu (block, edit, hide, report; only what the viewer may do), publication label and rejection reason under own posts, «Опубликовать» for own private posts, home posts open the Home editor through `onEditHome` and the list reloads when the user returns. The create button stays the app's `floatingAction` slot, which now receives `CommunityFloating` (signed-in state, session generation to close its own sheet, and `refresh`).
- **Improvements**: a failed save, vote, hide or report shows the server's own message or the default text — never a raw transport message; texts are Android resources.
- **Tests**: `CommunityEditTest` 14 (editor guards and drinks, field analytics, rejection before the request, saving with trimmed values and the review notice, in-place replacement on the personal timeline, failures, catalog retry, session change, hide and publish, report flows, ignored actions); with the 18 timeline tests the module has 32. Android build and iOS compile of main and tests pass.
- **Not covered**: device parity (step 4), image loading and the photo viewer (app slots).

## Step 4 result (app connection) and step 5 audit

- **Wiring**: `CommunityScreenRenderer` (commonMain) with `LegacyCommunityScreenRenderer` as the default (iOS keeps the legacy feed, `MainScreen` asks the renderer); Android binds `AndroidCommunityScreenRenderer` over `CommunityEntry` with the app-owned pieces: authenticated image loading (`CpImage` with the high-quality upgrade), the relative date, brew method names, photo prefetch, the full-screen photo viewer, exposure and feed-viewed analytics, the snackbar look lifted above the floating navigation, and the legacy create button with the global creation flow (a guest goes to sign in, the flow closes with the account scope and refreshes the list after a home check-in is saved). Koin gained `CommunityRepository` (authenticated client), `CommunityAccount` (`SessionCommunityAccount`: user id else token of an active session, own canonical slug from the refreshed profile), `ConsumedDrinks` (`ShopCommunityDrinks`) and `CommunityAnalytics` (`AppCommunityAnalytics`: same events and properties, the diary edit flow tracker).
- **Tests**: `CommunityAdaptersTest` 7 (account keys, own slug, drinks mapping, feed events, edit funnel, failure stage without leaking details, events before an editor is opened). Direct unit tests, Android and iOS compile, `assembleDirectSandbox` pass.
- **Device check (Pixel 7, Sandbox, dark theme)**: the feed renders like the legacy list; the own post's menu shows «Редактировать» and «Скрыть» (card-style menu); the edit sheet shows ratings, note, drink and buttons; saving shows the "sent for review" notice. **Found and fixed**: the notice was drawn behind the floating navigation (the default snackbar) — the app now lends its snackbar and lifts it above the navigation; photo tiles keep a loader underneath, so a photo that is not loaded yet (a fresh list after leaving the tab) never shows an empty hole.
- **Not checked on a device**: like, report and block flows, hide/publish, the home editor round trip, the create flow, large font, light theme, TalkBack, the loader of a slow photo (Sandbox images load instantly).
- **Audit**: nothing of the legacy feed is deleted. `CommunityScreen`, `CommunityViewModel`, `TimelineCheckInCard`, `EditCheckInSheet` stay for iOS and shared; `CheckInTimelineContent`/`CheckInActions` stay for the legacy public profile and review cards. Check-in repository methods used only by the feed (`getMyCheckIns(page)`, `updateCheckIn`, `setVisibility`, `setHelpful`, `report`) are still used by other legacy code paths until their owners move.
- **Follow-up (owner request)**: the same photo-loader underlay should be applied to the other places that show photos (shop, profile, history, favorites cards); done for the feed first.


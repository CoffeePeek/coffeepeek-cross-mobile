# iOS shop and roaster migration

The first checkpoint uses SwiftUI for the existing `ShopReviews` route entered
from the shop's full check-in action. It renders the aggregate and sample
ratings, public check-in cards, helpful votes, reporting, author links,
blocking, guest preview and sign-in gates. The existing shop ViewModel still
owns loading, votes, result rollback and navigation events. The existing
browsing lease and host ViewModel discard state and in-flight data when the
account or author visibility changes. The server still sends one full list.

Thumbnail loading and the existing full-screen album remain behind Kotlin's
session-aware media APIs. The album still uses the shared Compose photo
controller until the native viewer can be wired here. The iOS screen analytics
stay with the native route controller. The Android renderer stays in place.

The shop detail screen and roaster detail screen are separate checkpoints.
Their heaviest pieces are the shop header/menu/actions and QR handoff, and the
roaster offers/map/related shops. Keep the existing native gallery, report form
and coffee detail when wiring those screens.

The iOS workflow compiles the app and runs the existing shop list, account lease,
and author-blocking tests. On a device or simulator check both themes and
languages, VoiceOver, guest preview, the full album, vote failure, block/report
presentation and returning from sign-in. Verify account switching while a
request is pending without using a real account for mutations.

## Native check-in list navigation — 2026-11-25

Source: `dce8989e5` plus `codex/migration-ios-shop-filter-parity` working changes.
The active `ShopReviews` destination now exposes the existing root UIKit navigation
bar. Its child hosting controller supplies the localized title; the root controller
owns the compact navigation item and the system back button. SwiftUI no longer draws
another header or embeds another navigation stack at runtime. The root delegate
still owns swipe-back, saved-stack updates and bar visibility when leaving the list.
List state, media/session observation, actions and route-owned analytics are unchanged.

Paired light/dark and large-text previews use fake list state and a SwiftUI navigation
container for preview chrome; they do not exercise the runtime UIKit parent. Swift 6
syntax parsing, both native launcher registration contracts, Android assembly/DEX
checks and body limits passed. Full iOS metadata was blocked by the unchanged Room
constructor generation error; the initial failure and successful subsequent Android
checks are recorded in [the report verification](../shop-report/README.md#native-form-visual-parity--2026-11-25).
No device, IDE preview rendering or manual iPhone check ran. Native build, back-button
and edge-swipe behavior, status-bar/safe-area placement, theme and large text remain
device/simulator acceptance gates.

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

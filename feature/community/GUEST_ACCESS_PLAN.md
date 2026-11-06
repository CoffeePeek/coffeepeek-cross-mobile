# Guest access in Community and Search

This slice starts from `develop` at `3374e91bd428faccee46f900bf853a818adb0470`.

- Community shows one readable check-in and a faded teaser when a second is available. Guests can refresh the preview. The sign-in button and teaser open the existing login screen.
- Root navigation redirects guests from public profiles and their check-in lists, including public profile links. Discovery sends guests to sign-in for advanced filters and when opening a roaster from its search surfaces. Name search and public quick filters remain available.
- The shared Compose entries serve Android and iOS. Existing signed-in feed pagination, public shop navigation and the check-in shop picker keep their existing paths.

## Verification

- Android CI assembles both Debug flavors, checks packaged runtime classes, runs unit tests and runs lint. The focused regressions cover guest search and public profile routing.
- The feed includes paired light and dark previews. Manual verification uses `directSandbox` on an agreed device. Check the sticky button, long posts, large text, screen reader focus, Back from login, and signed-in pagination.
- Local verification on 2026-11-06 was limited to source and wiring review. The Windows checkout could not fetch the baseline. No app was installed or launched. Live backend and iOS device acceptance remain separate.

## Map labels and auth layout

- The Android and iOS MapLibre adapters use the existing OpenFreeMap vector source. Only subway stations appear as an M badge, from zoom 12, matching the default city camera and the lowest zoom at which the tiles contain stations. The basemap handles collisions. Manual review should cover Minsk below zoom 12 and at zooms 12 through 16, light and dark styles, zones and coffee pins.
- Login and registration draw behind the system bars. The close button stays below the top inset and the scroll content can clear the bottom inset or keyboard. Check login, all registration steps, back navigation, short windows and large fonts in Sandbox.
- The guest-only revision `3a464e49` passed [Android CI](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/42181028869), including both Debug flavors, unit tests and lint. The map and auth changes require a fresh build on both platforms.

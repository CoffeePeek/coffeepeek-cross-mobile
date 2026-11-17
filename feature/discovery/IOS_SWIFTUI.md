# SwiftUI Discovery

## Advanced filters

The advanced filter button in both the Discovery search surface and the
expanded map opens the same SwiftUI panel on iOS. The app captures the native
factory before creating the tab's child controller; the Discovery feature
continues to own the search model, localized catalog labels, the price scale,
and the draft. A confirmed draft sends one ApplyFilters event and keeps the
latest city. The close button, UIKit swipe dismissal, and VoiceOver escape drop
the draft. Catalog retry uses the existing search event. Images use the app's
existing catalog media loader and cancel when the row or panel leaves.

This checkpoint keeps MapLibre and its existing Kotlin actions. The search
header, results for shops and roasters, quick filters, map controls, selected
shop and zone cards, and the Discovery root are still Compose. SwiftUI uses a
menu for the same price stops and keeps the localized labels and selection
state. The existing Android sheet remains unchanged.

## Verification

The iOS CI runs the app build, Discovery draft tests and the existing search
ViewModel tests. The draft cases cover cancel without writing the original
selection, current-city preservation on apply, reset, and invalid intent
handling. Light and dark previews use fixed fixtures with no backend.

Manual simulator acceptance still needs presentation from search and expanded
map, one apply, dismissal by button and swipe, switching city, catalog retry,
scrolling, VoiceOver and large type in both supported app languages.

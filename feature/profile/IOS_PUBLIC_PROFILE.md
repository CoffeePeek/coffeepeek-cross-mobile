# Public profile on iOS

The author profile and its full check-in list now use SwiftUI through the
existing UIKit route controller. The Kotlin adapter owns a destination-scoped
PublicProfileViewModel. It uses the same follow, visibility, snapshot, paging,
public activity and connection-list rules as the shared screen. SwiftUI reuses
the own-profile image and check-in components. The existing route still owns
screen analytics, deep links, share, moderation confirmation and navigation.

The feature-owned shelf preview, featured collection and full-screen photo
viewer remain embedded pending their separate migrations. Profile editing,
avatar selection and deletion status are the next review slice for this block.

Verification covers both launcher registrations, SwiftUI resources for Russian
and Belarusian, the shared Kotlin profile and activity tests and new Native
projection tests for account replacement, block readiness and photos. Manual
iPhone verification remains for public and guest deep links, dynamic type,
VoiceOver, theme and locale changes, account transitions, follow/blocks, native
navigation gestures, embedded views and the full-list pagination.

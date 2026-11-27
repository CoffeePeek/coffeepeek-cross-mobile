# Roasters

Reviewed against `develop` on 2026-11-27. The Android-only activation and shared
iOS migration plans are superseded by this guide; their evidence remains in Git.

## Ownership and runtime

The populated `api/domain/data/impl` modules own roaster discovery, full-list
search and detail. Shared Kotlin owns state, repositories, favorites behavior
and request lifetimes. Android renders Compose; iOS renders the native discovery
and detail screens through `NativeRoasterListSession` and
`NativeRoasterDetailSession`. It is no longer correct to describe native iOS as
an unconfigured legacy fallback.

Application composition supplies account/favorite adapters, the existing
settings-backed catalogue cache, image/viewer, navigation, browser, haptics and
analytics. Public `RoasterDiscoveryEntry`, `RoasterDetailEntry`,
`PublicRoasterCatalog` and `PublicRoasterDetails` are the integration surfaces.
The detail implementation consumes Business Ownership through that feature's
API. Neither domain nor UI imports another feature's domain/data implementation.

## Data and state contracts

- The backend reads `/api/roasters` and `/api/roasters/{slug}`. The existing
  `roaster_catalog_v1` cache is application-adapted; do not introduce another
  catalogue writer or silently reset persisted settings.
- Search/filter/favorite criteria apply to the catalogue. Discovery preview and
  expanded list are presentations of the existing feature, not independently
  invented endpoint contracts. Returning from a full list must revoke that
  presentation's callbacks and avoid leaving its rows over the preview.
- Hearts use the authoritative server favorites source. A membership observation
  failure reports an error and preserves the last successful keys; a later
  success recovers. Guests request sign-in. Cancellation remains cancellation.
- Shop photo/detail enrichment crosses the existing narrow application ports.
  Missing photos use the configured placeholder; no new photo URL is guessed
  from an unrelated storage key.
- Root/session ownership guards navigation and image callbacks after disposal or
  account replacement. Native rendering does not take ownership of repositories
  or the authentication session.

## Source and verification

Inspect [API](api/src/commonMain), [data](data/src/commonMain),
[shared presentation/native sessions](impl/src/commonMain), and application
assembly under `composeApp/.../di/roaster`. Native views are in
`iosXtool/CoffeePeek/Sources/CoffeePeek/Roaster*Screen.swift` and share the current
native navigation/media infrastructure.

Existing domain/data/MVI/native-session unit and contract tests cover query
results, favorite failures/recovery and session lifetimes. For a behavioral
change run the affected suites and platform build gates, including the Android
packaged DEX check and the selected Native suites. Manual Sandbox acceptance
must cover list expand/return and interrupted transitions, error/retry, combined
filters, favorite synchronization/guest gates, placeholder detail, photo viewer,
links, both themes, narrow windows, large text and accessibility. Follow
[run modes](../../composeApp/ANDROID_RUN_MODES.md); use in-app Sandbox reset where
appropriate, never uninstall or clear app data to make a check pass.
No device/build verification was performed by this documentation-only review.

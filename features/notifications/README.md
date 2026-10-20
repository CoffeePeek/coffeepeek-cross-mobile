# Notifications

Owns enabled-by-default notification categories, device registration, post-check-in
permission coordination and foreground activity reporting. There is no settings UI.
The application composition consumes `api` and supplies the authenticated HTTP client,
session repository and native device port. Domain has no platform or transport imports;
DTOs and HTTP endpoints stay internal to data. Shared presentation coordinates session
changes; platform SDKs remain in Android/iOS application boundaries. Application DI
wires successful shop/home creation callbacks to the feature API. Native code requests
OS permission once and respects a previous denial.
No other feature may depend on internals. A separate module follows the existing
author-blocking feature boundary and prevents native Firebase dependencies leaking into
the shared business code. The legacy modules have no dependency on this feature.

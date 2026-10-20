# Notifications

Owns notification preferences, device registration and foreground activity reporting.
The application composition consumes `api` and supplies the authenticated HTTP client,
session repository and native device port. Domain has no platform or transport imports;
DTOs and HTTP endpoints stay internal to data. Shared presentation coordinates session
changes and exposes state; platform SDKs remain in Android/iOS application boundaries.
No other feature may depend on internals. A separate module follows the existing
author-blocking feature boundary and prevents native Firebase dependencies leaking into
the shared business code. The legacy modules have no dependency on this feature.

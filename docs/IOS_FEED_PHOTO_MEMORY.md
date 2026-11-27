# iOS image memory and viewer lifetime

Current diagnostic guide, reviewed 2026-11-27. The original feed stutter/termination
report and investigation remain in Git history. Without a current crash/jetsam
report or process profile, memory pressure is a hypothesis, not a confirmed leak.
Do not reuse a historical TestFlight build's measurements as current acceptance.

## Active source owners

- `core/media` owns transfer, access permissions, bounded caches and platform
  decoding; [its guide](../core/media/README.md) is the photo protocol reference.
- Shared Compose `IosImageBitmapDecoder` uses ImageIO downsampling before bitmap
  allocation (default edge 2048), serialized native work and cancellation checks.
  `ImageBitmapMemoryCache` limits retained pixels/entry count. This is not a bound
  on the entire process, active painters, GPU textures or decoder temporaries.
- The active app viewer is SwiftUI in `CoffeePeekNativePhotoViewer.swift`, through
  `composeApp/iosMain` media adapters and `IosPhotoPresentation`. Its UIImage
  loading/access lifetime must be reviewed independently of the Compose decoder.
  Do not claim the Compose cache budget caps all active SwiftUI images.
- Protected images observe session/access expiry, clear invalid frames and reject
  late completions. Cache bytes and decoded images are never permission to show
  a previously private photo after revocation/account change.

## Diagnostic workflow

Record source/version/build, signed configuration, device/OS and exact sequence
before profiling: open/dismiss repeatedly, swipe photos, scroll, background/resume
and leave the feature. Inspect live allocations/resident memory and a fresh crash/
jetsam report using the platform tools. Keep screenshots and sanitized reports
local; do not publish private photos/account identifiers or full diagnostics.

Review cancellation of preview/original tasks, observation handles and native
controller lifetime. Distinguish encoded bytes, decoded RGBA, temporary decode
buffers, active UI image references and GPU memory. Width × height × 4 estimates
pixel storage only; a small compressed file can decode to much more memory.

Keep HTTP channel consumption inside the response scope. Do not add buffering or
thread/flow boundaries around a raw channel without updating its ownership.
Speculative work is bounded and pauses under the viewer; original loading remains
current-photo work. Any concurrency/cache tuning needs measured evidence.

## Acceptance

Existing non-UI regressions cover cache/access clocks, cancellation, source
identity and stale callbacks. Executed Native tests/full app linking are separate
from a Windows compile. Long-scroll/viewer profiling, corrupt/EXIF/alpha inputs,
rapid dismissal, background/resume and real protected-access revocation remain
explicit manual gates on the signed artifact. No app/device/profiling or new test
execution was performed for this documentation update.

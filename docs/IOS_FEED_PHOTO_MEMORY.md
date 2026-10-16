# iOS feed photo memory investigation

Date: 2026-10-16. Reported device: iPhone 13, iOS 26.6.2, TestFlight 41.1.
Reproduction: open a photo in the feed, dismiss it, then scroll; severe stuttering
eventually ends in termination. No fresh crash or jetsam report was available
for this incident. Memory pressure is a supported hypothesis, not a confirmed
termination reason or a measured leak.

## Build and source evidence

[TestFlight run 41, attempt 1](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/38887973940)
built `e07446f3bfc11a7c4e16fcd94da637802a3230ff`, whose second parent is
`dc4add26`. It includes the earlier profile, feed visibility, email editing and
keyboard sheet fixes. This report investigates an additional path.

The pinned dependencies are Kamel 1.0.8 and Compose Multiplatform 1.12.1.
Inspection of their cached source artifacts established:

- The non-Android bitmap decoder constructs a Skia image from the complete
  encoded file. Compose's conversion then allocates a bitmap at the original
  dimensions. Kamel's `maxBitmapDecodeSize` is ignored on this platform.
- Kamel's default bitmap cache retains up to 100 entries, without a byte budget.
  The application shares that cache across its native root controllers and the
  photo viewer. Dismissing a photo therefore does not evict its decoded pixels.
- The viewer eagerly composes one page on either side of the selected photo.
- The iOS custom URL fetcher first collected a response into a byte array, before
  the decoder collected the channel into another byte array.
- Normal Compose Dialog dismissal releases its graphics layer and closes the
  overlay scene, cancelling its scene job. No application-owned controller or
  rendering-loop leak was demonstrated in this path. Dialog transitions remain
  enabled.

An anonymous, header-only inspection of three public feed photos found:

| Dimensions | Encoded size reported by API | Estimated decoded RGBA pixels |
| --- | ---: | ---: |
| 1440 x 2560 | 1,032,087 bytes | 14.06 MiB |
| 1440 x 2560 | 522,069 bytes | 14.06 MiB |
| 3213 x 5712 | 3,166,016 bytes | 70.01 MiB |

These are `width * height * 4` calculations, not measured process memory. Native
decoder, CPU bitmap, GPU texture and closing-frame resources can overlap.
No photo content, user identifiers or private API responses are stored here.

## Changes

Application composition installs an iOS bitmap decoder after Kamel's defaults,
so both the normal URL and custom URL paths use it. ImageIO creates an
orientation-correct thumbnail with a maximum edge of 2048 pixels before the
RGBA buffer is allocated. Small images retain their dimensions. Alpha is
preserved; failures do not fall back to allocating the original-size bitmap.
One decoder operation runs at a time, and cancellation is checked around native
work. Temporary CF/CG objects are explicitly released.

The estimated pixels of the 3213 x 5712 sample become 9 MiB at 1152 x 2048;
a square image is at most 16 MiB. These bounds cover returned RGBA bitmaps,
not the application's total footprint or the native decoder's temporary memory.
The display limit does not alter uploaded files; deep zoom has less source
detail than an original-resolution decode.

The shared iOS bitmap cache now has a 48 MiB pixel budget and a 100-entry cap,
using access-order eviction. Eviction drops references without disposing pixels
still used by a painter. Active painters, Skia/Metal caches and temporary buffers
are outside this budget. GIF animations and SVG/vector resources use separate
Kamel decoders/caches and are outside the bitmap policy.

The pager no longer eagerly retains an additional offscreen page on each side;
normal swipe-direction prefetch remains available. The iOS custom URL fetcher
passes the HTTP channel directly to the decoder. Its `flow.emit` stays inside
Ktor's response scope until downstream decoding finishes. Do not insert a
buffer, `channelFlow` or `flowOn` at this raw-channel boundary without changing
response ownership.

## Automated verification

Completed on Windows:

- 255 application unit tests passed (62 suites, no failures or skipped tests),
  including 10 byte-cache regression tests and concurrent updates.
- Kotlin main and test sources compiled for iOS Arm64 and iOS Simulator Arm64.
  The 8 new native decoder tests and 4 HTTP-stream tests were compiled, not run.
- Both production-viewer Android UI tests passed on the Pixel 8 API 37 emulator:
  initial middle-page loading without eager neighbors, next-page swipe, and
  12 pending-load dismissals with cancellation and working underlying scrolling
  and buttons. These do not measure iOS memory or rendering performance.
- Independent source review found no additional actionable defect in resource
  ownership, Kamel decoder registration, cache concurrency or stream lifetime.
- No Gradle dependencies, domain/data boundaries or module graph changed.

The successful Gradle invocation ran these tasks:

```powershell
.\gradlew.bat :composeApp:testDirectDebugUnitTest :composeApp:compileTestKotlinIosArm64 :composeApp:compileTestKotlinIosSimulatorArm64 :composeApp:connectedDirectDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.coffeepeek.admin.ui.component.FullScreenImageDialogTest' --offline --console=plain
```

## Real-device verification

Use the same iPhone 13, feed data, build mode and thermal conditions for before
and after captures. Avoid a debugger when collecting a termination report.

1. In Xcode Instruments, record Allocations and Time Profiler while scrolling
   for 30 seconds, opening/dismissing the same photo 20 times, scrolling again,
   then repeating with at least 10 different large photos.
2. Mark the initial feed, open viewer, immediately after dismissal and 5 seconds
   after dismissal. Track process physical footprint, live allocations,
   ImageIO/Skia/Metal allocations, main-thread stalls and frame responsiveness.
3. Repeat portrait/landscape photos, transparent cutouts, rotated/mirrored JPEGs,
   rapid dismissal during loading, paging, pinch zoom and double tap. Check
   visual orientation and alpha on device. Include a long feed scroll after the
   cycles to exercise cache eviction.
4. Inspect Xcode Memory Graph and the Leaks instrument for retained controllers,
   Compose scenes and decoders. A reachable cache or GPU resource can consume
   memory without being reported as a leak. Look for a bounded steady state
   after caches warm; do not require an exact return to the initial footprint.
5. If termination recurs, collect the matching `CoffeePeek*.ips` or
   `JetsamEvent*.ips` from Settings > Privacy & Security > Analytics & Improvements
   > Analytics Data. Compare its timestamp, app build, exception or termination
   reason with the capture. Symbolicate an app crash with the matching dSYM.

Windows can compile Kotlin iOS source/tests but cannot run ImageIO/UIKit, link
the app or establish that native/GPU allocations stop growing. Shared Android
UI tests validate composition and cancellation only. Device profiling remains
necessary before calling this incident resolved.

Apple references:
[ImageIO downsampling](https://developer.apple.com/documentation/imageio/cgimagesourcecreatethumbnailatindex(_:_:_:)),
[thumbnail size](https://developer.apple.com/documentation/imageio/kcgimagesourcethumbnailmaxpixelsize),
[thumbnail orientation](https://developer.apple.com/documentation/imageio/kcgimagesourcecreatethumbnailwithtransform),
[gathering memory evidence](https://developer.apple.com/documentation/xcode/gathering-information-about-memory-use),
[reducing memory use](https://developer.apple.com/documentation/xcode/making-changes-to-reduce-memory-use),
[crash and diagnostic logs](https://developer.apple.com/documentation/xcode/acquiring-crash-reports-and-diagnostic-logs).

# P4: check-in photo variants in KMP

Requires the additive backend contract from CoffeePeek/coffeepeek-backend PRs #346–#349.
This branch starts from develop (613c7872); it does not include the unrelated local iOS merge.

Feed/shop/profile cards use feed previews, calendars use thumb, and a tap opens detail. Original URLs and gallery order stay in the domain model. The viewer exposes an explicit original action; on a pending manifest only a user opening the viewer can load an original. Legacy responses without variants keep their previous behavior. Home gallery and drink cutout descriptors are handled separately, preserving PNG alpha. JPEG/PNG is preferred for platform compatibility, with WebP as a supported fallback.

Protected check-in images have no HTTP or decoded bitmap cache reuse. Each new display checks access again. An active composition discards its previous resource every 20 seconds and on session changes before requesting again. Offline/error responses do not keep the previous painter. These are conservative no-store semantics, not a positive TTL or conditional-GET cache. Keep backend positive-cache rollout disabled until real-device acceptance.

At most four image transfers run simultaneously. Duplicate protected transfers share in-flight work within an opaque session scope; no completed bytes are retained by the coordinator. Leaving the last subscriber cancels the transfer. iOS variants may temporarily share their bounded encoded bytes; original iOS downloads still stream into the existing bounded decoder. The existing iOS decode mutex is retained until profiling. Android server variants skip decode/re-encode and go directly to the decoder; legacy originals retain EXIF handling.

Validation commands:

```sh
./gradlew :modules:network:testDebugUnitTest :modules:data:testDebugUnitTest :features:home-checkin:testDebugUnitTest :composeApp:testDirectDebugUnitTest
./gradlew :composeApp:compileKotlinIosArm64
```

Remaining release gates: signed iOS/Android device runs, background/resume and rapid account switch, private/home access and both block directions against the real Gateway/Caddy, corrupt/EXIF/alpha cases, long-scroll memory, and download/decode/render measurements at 5 Mbit/s and 100 ms RTT. Windows KMP compilation does not prove an Xcode link or device behavior. Positive TTL/304 byte reuse and final concurrency tuning remain separate measured changes. No production flags are changed.

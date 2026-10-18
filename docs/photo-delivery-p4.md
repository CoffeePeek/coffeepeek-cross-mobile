# P4: check-in photo variants in KMP

Requires the additive backend contract from CoffeePeek/coffeepeek-backend PRs #346–#349.
This branch starts from develop (613c7872); it does not include the unrelated local iOS merge.

Feed/shop/profile cards use feed previews, calendars use thumb, and a tap opens detail. Original URLs and gallery order stay in the domain model. The viewer exposes an explicit original action; on a pending manifest only a user opening the viewer can load an original. Legacy responses without variants keep their previous behavior. Home gallery and drink cutout descriptors are handled separately, preserving PNG alpha. JPEG/PNG is preferred for platform compatibility, with WebP as a supported fallback.

Protected immutable check-in variants use an application memory cache of encoded bytes (48 entries / 12 MiB, LRU). Its key includes the full source/recipe/purpose/format URL and an opaque rotating session scope. A response must explicitly permit private storage; `no-store` evicts bytes and ETag, while `max-age=0` may retain them only for online revalidation. After expiry, `If-None-Match` can restore the exact retained bytes only on an authorized 304 with a matching ETag and valid new policy. Denials, network failures, malformed metadata, missing candidates and unexpected 304 responses never show stale bytes.

Fresh reuse is capped by server `max-age`, 20 seconds and JWT expiry. RFC 9111 Date/Age accounting includes the entire request/body transfer time; wall and monotonic deadlines also account for residence and decoding. Missing/unreadable Date grants no positive reuse. A usable positive grant that expires before completion cannot display. An online no-store/zero-freshness response may authorize the current presentation until request start + 20 seconds, capped by JWT, without granting a later fresh hit. A new 304 uses its own Date/Age/policy; old observations cannot renew permission. Unknown/expired authenticated JWTs fail closed. Anonymous requests conservatively always revalidate online.

Display permission is separate from cached bytes. The protected painter is removed at its actual permission deadline before reauthorization; it also clears on session/token changes, logout, local photo-affecting mutations and backgrounding. Resume checks both clocks before rendering. Kamel's decoded caches are explicitly bypassed, including the custom iOS bitmap cache. Original/private/home/moderation responses keep no-store behavior. Keep backend positive-cache rollout disabled until real-device acceptance.

At most four image transfers run simultaneously. Duplicate variants share in-flight work within one session scope. Leaving the last subscriber cancels the transfer, including queued work; cancelled or invalidated responses cannot publish later or overwrite a replacement. Variant bodies stream with the backend's 8 MiB limit. Original iOS downloads still stream into the existing bounded decoder. The iOS decode mutex is retained until profiling. Android server variants skip decode/re-encode and go directly to the decoder; legacy originals retain EXIF handling.

Protected transfers use a dedicated authenticated Ktor client without `HttpCache`, plus request `no-cache, no-store` and explicit Darwin URL cache bypass. The request directive excludes lower transport caches; the separate application byte cache follows response policy. Ktor's HTTP cache otherwise intercepts manual 304 responses and can throw `InvalidCacheStateException`. API and image clients share refresh rotation coordination bound to the rejected request's actual bearer credential, including delayed 401 responses. An account switch never retries the old operation with another account. Other images retain their existing transport.

Validation commands:

```sh
./gradlew :modules:network:testDebugUnitTest :modules:data:testDebugUnitTest :features:home-checkin:testDebugUnitTest :composeApp:testDirectDebugUnitTest
./gradlew :composeApp:compileKotlinIosArm64
```

Unit coverage includes Date/Age/response delay, zero/no-store policy, missing Date, repeated/new 304 metadata, JWT expiry, clock changes, LRU/byte limits, denied/offline loads, simultaneous subscribers, uncancellable old responses, evicted candidates, native cache exclusion and shared/delayed refresh rotation. Remaining release gates: signed iOS/Android device runs, background/resume and rapid account switch, private/home access and both block directions against the real Gateway/Caddy, corrupt/EXIF/alpha cases, long-scroll memory, and download/decode/render measurements at 5 Mbit/s and 100 ms RTT. Windows KMP compilation does not prove an Xcode link or device behavior. Final concurrency tuning remains a measured change. No production flags are changed.

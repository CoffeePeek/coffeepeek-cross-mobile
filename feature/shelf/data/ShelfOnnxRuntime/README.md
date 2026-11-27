# iOS ONNX Runtime dependency

Reviewed against `Package.swift` on 2026-11-27. The shelf scanner uses ONNX Runtime
C 1.24.2 and the matching local Objective-C bindings. `ShelfOnnxRuntime` exports
the existing `OnnxRuntimeBindings` module and supplies the Apple framework/C++
linker settings. The source and binary licenses/checksums are recorded in
[THIRD_PARTY.md](THIRD_PARTY.md); preserve `LICENSE.onnxruntime`.

The active binary target is the immutable
[CoffeePeek dependency release](https://github.com/CoffeePeek/coffeepeek-cross-mobile/releases/tag/ios-onnxruntime-1.24.2).
It mirrors the original Microsoft archive without changing binary bytes:

- Original binary: `https://download.onnxruntime.ai/pod-archive-onnxruntime-c-1.24.2.zip`
- SHA-256: `f7100a992d2a8135168c8afd831e6a58b465349101982aa58b3e11d36e600b54`
- Original package revision: `b7fb7f7dea8a2469e6335d95a61b8f36d0dc83b2`

The mirror replaced an upstream TLS hostname failure; SwiftPM still verifies
TLS and that same checksum. The earlier Git source mirror is no longer an
active SwiftPM dependency. Do not restore its checkout instructions, disable
certificate/checksum verification or publish different bytes under this pin.
Source and binary updates must use matching versions and preserve provenance.

macOS verification/TestFlight builds use this package normally. Windows xcross
UI builds retain their scanner limitation; see [iOS setup](../../../../docs/IOS_SETUP.md).
Python/static checks or Kotlin metadata compilation do not certify SwiftPM
resolution, Apple framework linking, camera/scanner runtime or signing.
This documentation-only review did not download, resolve or execute the runtime.

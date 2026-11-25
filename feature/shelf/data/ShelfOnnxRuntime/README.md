# iOS ONNX Runtime dependency

The scanner uses the unmodified ONNX Runtime C 1.24.2 archive and the existing
Objective-C bindings. Version, binary bytes and upstream SHA-256 stay pinned.

The upstream `download.onnxruntime.ai` endpoint failed TLS hostname validation
on the macOS verification runner on 2026-11-25. The retained original archive was
checked against Microsoft's published checksum and copied to the project's
[dependency release](https://github.com/CoffeePeek/coffeepeek-cross-mobile/releases/tag/ios-onnxruntime-1.24.2).
SwiftPM still verifies TLS and the same checksum before using it.

- Original URL: `https://download.onnxruntime.ai/pod-archive-onnxruntime-c-1.24.2.zip`
- Upstream package revision: `b7fb7f7dea8a2469e6335d95a61b8f36d0dc83b2`
- SHA-256: `f7100a992d2a8135168c8afd831e6a58b465349101982aa58b3e11d36e600b54`
- Original MIT license remains inside the archive.

Do not disable certificate/checksum verification or substitute a different binary
under this version. Local Windows xcross UI builds retain their existing scanner
limitation; macOS verification and TestFlight build the scanner normally.

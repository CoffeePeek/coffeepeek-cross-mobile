# ONNX Runtime source pin

This local Swift package adds the Apple framework and C++ linker settings needed
by the shelf scanner's `OnnxRuntimeBindings` import. It keeps ONNX Runtime 1.24.2.

The Microsoft SwiftPM repository returned HTTP 404 on 2026-11-19. The source URL
therefore uses the GuLP-AAC mirror, pinned to the full original commit
`b7fb7f7dea8a2469e6335d95a61b8f36d0dc83b2` from the application's existing
`Package.resolved`. Both the manifest and application lock file use that revision.
The mirror is a transport for the same source tree. Other package pins are unchanged.

The binary remains Microsoft's archive at
`https://download.onnxruntime.ai/pod-archive-onnxruntime-c-1.24.2.zip`, with SHA-256
`f7100a992d2a8135168c8afd831e6a58b465349101982aa58b3e11d36e600b54`.
The source retains Microsoft's MIT license.

Verification on Windows, 2026-11-19, from `88cbfc634` with the source-pin patch:
the pinned commit fetched with object integrity checking and passed `git fsck`;
the official 52,138,597-byte binary archive matched its SHA-256; the manifest and
lock file agreed; 21 iOS tooling and 51 release tooling tests passed. The first
tooling run had three Git Bash failures caused by Windows sandbox access denial;
the permission-scoped retry passed all tests. Original diagnostics remain local.
Direct Debug Android assembly, packaged runtime-class verification and the
function body limit check also passed. Gradle reused unaffected compiled outputs.

Full SwiftPM resolution, Apple compilation, signing and TestFlight upload require
the macOS CI archive run. Local Python checks do not certify those gates.

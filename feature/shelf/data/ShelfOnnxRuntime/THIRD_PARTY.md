# ONNX Runtime 1.24.2

`Sources/OnnxRuntimeBindings` contains the unmodified Objective-C inference
bindings from Microsoft's official archive, with its test directory omitted.
The license is preserved in `LICENSE.onnxruntime`.

- Source archive — https://download.onnxruntime.ai/pod-archive-onnxruntime-objc-1.24.2.zip
- Source archive SHA-256 — `ab102b30f15c44b08dc6fc32683abeafd216ff6bcabdf74fbac37fbb19d80eed`
- Binary archive — https://download.onnxruntime.ai/pod-archive-onnxruntime-c-1.24.2.zip
- Binary archive SHA-256 — `f7100a992d2a8135168c8afd831e6a58b465349101982aa58b3e11d36e600b54`

`Package.swift` uses the same runtime version and exports the same
`OnnxRuntimeBindings` module. Updates must use matching versions of the source
and binary archives.

// swift-tools-version: 5.9
import PackageDescription

// ONNX Runtime 1.24.2 for the shelf scanner's iosMain. The local Objective-C bindings and the official binary archive
// retain the OnnxRuntimeBindings module while avoiding a Git checkout of the upstream Swift package.
let package = Package(
    name: "ShelfOnnxRuntime",
    platforms: [.iOS(.v15)],
    products: [.library(name: "ShelfOnnxRuntime", targets: ["ShelfOnnxRuntime"])],
    targets: [
        .binaryTarget(
            name: "onnxruntime",
            url: "https://download.onnxruntime.ai/pod-archive-onnxruntime-c-1.24.2.zip",
            checksum: "f7100a992d2a8135168c8afd831e6a58b465349101982aa58b3e11d36e600b54"
        ),
        .target(
            name: "OnnxRuntimeBindings",
            dependencies: ["onnxruntime"],
            cxxSettings: [.define("SPM_BUILD")]
        ),
        .target(
            name: "ShelfOnnxRuntime",
            dependencies: ["OnnxRuntimeBindings"],
            linkerSettings: [
                .linkedFramework("Foundation"),
                .linkedFramework("CoreML"),
                .linkedLibrary("objc"),
                .linkedLibrary("c++"),
            ]
        ),
    ],
    cxxLanguageStandard: .cxx17
)

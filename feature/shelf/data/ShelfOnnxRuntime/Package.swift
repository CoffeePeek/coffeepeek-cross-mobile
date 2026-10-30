// swift-tools-version: 5.9
import PackageDescription

// ONNX Runtime for the shelf scanner's iosMain. The runtime's own package declares no linker settings, so its Objective-C
// bindings and the CoreML execution provider would leave Foundation, CoreML and the Objective-C runtime unresolved.
let package = Package(
    name: "ShelfOnnxRuntime",
    platforms: [.iOS(.v15)],
    products: [.library(name: "ShelfOnnxRuntime", targets: ["ShelfOnnxRuntime"])],
    dependencies: [
        // Pinned: the Swift package lags the Android AAR.
        .package(url: "https://github.com/microsoft/onnxruntime-swift-package-manager.git", exact: "1.24.2"),
    ],
    targets: [
        .target(
            name: "ShelfOnnxRuntime",
            dependencies: [.product(name: "onnxruntime", package: "onnxruntime-swift-package-manager")],
            linkerSettings: [
                .linkedFramework("Foundation"),
                .linkedFramework("CoreML"),
                .linkedLibrary("objc"),
                .linkedLibrary("c++"),
            ]
        ),
    ]
)

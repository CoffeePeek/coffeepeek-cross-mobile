// swift-tools-version: 5.9
import PackageDescription

// ONNX Runtime for the shelf scanner's iosMain. The runtime's own package declares no linker settings, so its Objective-C
// bindings and the CoreML execution provider would leave Foundation, CoreML and the Objective-C runtime unresolved.
let package = Package(
    name: "ShelfOnnxRuntime",
    platforms: [.iOS(.v15)],
    products: [.library(name: "ShelfOnnxRuntime", targets: ["ShelfOnnxRuntime"])],
    dependencies: [
        // The Microsoft repository returns 404. Pin the mirror to the original 1.24.2 commit from Package.resolved.
        .package(
            url: "https://github.com/GuLP-AAC/onnxruntime-swift-package-manager.git",
            revision: "b7fb7f7dea8a2469e6335d95a61b8f36d0dc83b2"
        ),
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

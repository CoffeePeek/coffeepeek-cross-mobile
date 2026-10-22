// swift-tools-version: 5.10

import PackageDescription
import Foundation

let packageDirectory = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
let composeFrameworkDirectory = packageDirectory
    .deletingLastPathComponent()
    .deletingLastPathComponent()
    .appendingPathComponent("composeApp/build/bin/iosArm64/debugFramework")
    .path

let package = Package(
    name: "CoffeePeek",
    platforms: [
        .iOS("18.0"),
        .macOS(.v14),
    ],
    products: [
        // An xtool project should contain exactly one library product,
        // representing the main app.
        .library(
            name: "CoffeePeek",
            targets: ["CoffeePeek"]
        ),
    ],
    dependencies: [
        .package(path: "../../iosApp/KotlinMultiplatformLinkedPackage"),
        .package(url: "https://github.com/google/GoogleSignIn-iOS", from: "10.0.0"),
        .package(
            url: "https://github.com/maplibre/maplibre-gl-native-distribution",
            exact: "6.29.0"
        ),
    ],
    targets: [
        .target(
            name: "CoffeePeek",
            dependencies: [
                .product(name: "KotlinMultiplatformLinkedPackage", package: "KotlinMultiplatformLinkedPackage"),
                .product(name: "GoogleSignIn", package: "GoogleSignIn-iOS"),
                .product(name: "MapLibre", package: "maplibre-gl-native-distribution"),
            ],
            swiftSettings: [
                .unsafeFlags(["-F", composeFrameworkDirectory], .when(platforms: [.iOS])),
            ],
            linkerSettings: [
                .unsafeFlags(
                    ["-F", composeFrameworkDirectory, "-framework", "ComposeApp"],
                    .when(platforms: [.iOS])
                ),
            ]
        ),
    ]
)

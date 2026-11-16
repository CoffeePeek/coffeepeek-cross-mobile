// swift-tools-version: 5.9
import PackageDescription
import class Foundation.ProcessInfo
let xcrossBuild = ProcessInfo.processInfo.environment["COFFEEPEEK_XCROSS"] == "1"
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: (xcrossBuild ? [] : [.package(path: "subpackages/_feature_shelf_data")]) + [
    .package(path: "subpackages/com_posthog_posthog_kmp_0_5_1")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: (xcrossBuild ? [] : [.product(name: "_feature_shelf_data", package: "_feature_shelf_data")]) + [
        .product(name: "com_posthog_posthog_kmp_0_5_1", package: "com_posthog_posthog_kmp_0_5_1")
      ]
    )
  ]
)

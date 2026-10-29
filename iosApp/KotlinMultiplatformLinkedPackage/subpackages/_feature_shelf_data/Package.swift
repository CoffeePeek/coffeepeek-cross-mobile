// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_shelf_data",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "_feature_shelf_data",
      type: .none,
      targets: ["_feature_shelf_data"]
    )
  ],
  dependencies: [
    .package(
      path: "../../../../feature/shelf/data/ShelfOnnxRuntime"
    )
  ],
  targets: [
    .target(
      name: "_feature_shelf_data",
      dependencies: [
        .product(
          name: "ShelfOnnxRuntime",
          package: "ShelfOnnxRuntime"
        )
      ]
    )
  ]
)

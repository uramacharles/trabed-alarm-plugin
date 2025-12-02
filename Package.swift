// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TrabedAlarmPlugin",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "TrabedAlarmPlugin",
            targets: ["AlarmPluginPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "AlarmPluginPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/AlarmPluginPlugin"),
        .testTarget(
            name: "AlarmPluginPluginTests",
            dependencies: ["AlarmPluginPlugin"],
            path: "ios/Tests/AlarmPluginPluginTests")
    ]
)
import SwiftUI
import ComposeApp

@main
struct CoffeePeekApp: App {
    init() {
        IosNativeMapRegistry.shared.provider = MapLibreMapProvider()
        IosDrinkPhotoRegistry.shared.provider = NativeDrinkPhotoProvider()
    }

    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                .ignoresSafeArea(.container, edges: .all)
                .ignoresSafeArea(.keyboard)
        }
    }
}

private struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

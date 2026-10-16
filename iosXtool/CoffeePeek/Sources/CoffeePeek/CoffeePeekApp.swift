import SwiftUI
import ComposeApp
import GoogleSignIn

@main
struct CoffeePeekApp: App {
    init() {
        IosNativeMapRegistry.shared.provider = MapLibreMapProvider()
        IosDrinkPhotoRegistry.shared.provider = NativeDrinkPhotoProvider()
        SocialSignInProvider.shared.configure()
    }

    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                .ignoresSafeArea(.container, edges: .all)
                .ignoresSafeArea(.keyboard)
                .onOpenURL { url in
                    if !GIDSignIn.sharedInstance.handle(url) {
                        _ = MainViewControllerKt.handleAppLink(link: url.absoluteString)
                    }
                }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    if let url = activity.webpageURL {
                        _ = MainViewControllerKt.handleAppLink(link: url.absoluteString)
                    }
                }
        }
    }
}

private struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

import SwiftUI
import GoogleSignIn
import ComposeApp

@main
struct CoffeePeekApp: App {
    init() {
        IosNativeMapRegistry.shared.provider = MapLibreMapProvider()
        SocialSignInProvider.shared.configure()
    }

    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                // Compose owns status/navigation bar insets. Keeping the host view
                // inside SwiftUI's safe area applies those insets twice and leaves
                // the system regions outside the current screen background.
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

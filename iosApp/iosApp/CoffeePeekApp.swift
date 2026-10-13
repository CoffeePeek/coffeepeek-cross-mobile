import SwiftUI
import GoogleSignIn
import ComposeApp

@main
struct CoffeePeekApp: App {
    init() {
        IosNativeMapRegistry.shared.provider = MapLibreMapProvider()
        configureGoogleSignIn()
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
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }

    private func configureGoogleSignIn() {
        let info = Bundle.main.infoDictionary ?? [:]
        let iosClientID = (info["GIDClientID"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let webClientID = (info["GIDServerClientID"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let reversedClientID = (info["CoffeePeekGoogleReversedClientID"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let configured = !iosClientID.isEmpty
            && !webClientID.isEmpty
            && !reversedClientID.isEmpty
            && reversedClientID != "coffeepeek-google-signin-disabled"

        if configured {
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(
                clientID: iosClientID,
                serverClientID: webClientID
            )
        }

        IosGoogleAuthBridge.shared.configure(
            isConfigured: configured,
            signIn: { completion in
                let completionBox = KotlinCompletionBox(completion)
                guard let presenter = UIApplication.shared.topViewController else {
                    completionBox.call(idToken: nil, error: "Не удалось открыть Google Sign-In")
                    return
                }
                GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                    completionBox.call(
                        idToken: result?.user.idToken?.tokenString,
                        error: error?.localizedDescription
                    )
                }
            },
            signOut: {
                GIDSignIn.sharedInstance.signOut()
            }
        )
    }
}

private struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private final class KotlinCompletionBox: @unchecked Sendable {
    private let completion: (String?, String?) -> KotlinUnit

    init(_ completion: @escaping (String?, String?) -> KotlinUnit) {
        self.completion = completion
    }

    func call(idToken: String?, error: String?) {
        _ = completion(idToken, error)
    }
}

private extension UIApplication {
    var topViewController: UIViewController? {
        let root = connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController

        var current = root
        while let presented = current?.presentedViewController {
            current = presented
        }
        return current
    }
}

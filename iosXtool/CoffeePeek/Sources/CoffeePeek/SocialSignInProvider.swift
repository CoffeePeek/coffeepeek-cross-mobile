import AuthenticationServices
import ComposeApp
import GoogleSignIn
import UIKit

@MainActor
private final class SocialSignInCompletion<Value> {
    private var callback: ((String?, String?) -> Value)?
    init(_ callback: @escaping (String?, String?) -> Value) { self.callback = callback }
    func finish(token: String?, error: String?) {
        let completion = callback
        callback = nil
        _ = completion?(token, error)
    }
}

/// Kotlin dispatches this UI port and request-specific cancellation to Main.
@MainActor
final class SocialSignInProvider: NSObject, @MainActor IosAppleAuthProvider {
    @MainActor static let shared = SocialSignInProvider()
    private let ui = SocialSignInUI()

    @MainActor func configure() { ui.configure(provider: self) }

    func signIn(nonce: String, state: String, completion: @escaping (String?, String?) -> Void) {
        ui.signIn(nonce: nonce, state: state, completion: SocialSignInCompletion(completion))
    }
    func cancel(state: String) { ui.cancelApple(state: state) }
}

@MainActor
private final class SocialSignInUI: NSObject,
    ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    private var appleController: ASAuthorizationController?
    private var appleCompletion: SocialSignInCompletion<Void>?
    private var appleWindow: UIWindow?
    private var expectedState: String?
    private var googleInProgress = false
    // Public NSError contract: kGIDSignInErrorCodeCanceled = -5.
    // SDK 10 imports its NS_ERROR_ENUM differently from earlier SDK versions.
    private static let googleCancellationCode = -5

    func configure(provider: SocialSignInProvider) {
        IosAppleAuthRegistry.shared.provider = appleSignInEnabled ? provider : nil
        let info = Bundle.main.infoDictionary ?? [:]
        func value(_ key: String) -> String {
            (info[key] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        }
        let clientID = value("GIDClientID")
        let serverID = value("GIDServerClientID")
        let reversedID = value("CoffeePeekGoogleReversedClientID")
        let urlTypes = info["CFBundleURLTypes"] as? [[String: Any]] ?? []
        let schemes = urlTypes.flatMap { $0["CFBundleURLSchemes"] as? [String] ?? [] }
        let valid = clientID.hasSuffix(".apps.googleusercontent.com")
            && serverID.hasSuffix(".apps.googleusercontent.com")
            && reversedID == clientID.split(separator: ".").reversed().joined(separator: ".")
            && schemes.contains(reversedID)
        if valid {
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID, serverClientID: serverID)
        }
        IosGoogleAuthBridge.shared.configure(isConfigured: valid, signIn: { [weak self] completion in
            guard let self else {
                _ = completion(nil, "Не удалось открыть окно Google")
                return
            }
            self.signInWithGoogle(SocialSignInCompletion(completion))
        }, signOut: { GIDSignIn.sharedInstance.signOut() })
    }

    private var appleSignInEnabled: Bool {
        guard let value = Bundle.main.object(forInfoDictionaryKey: "CoffeePeekAppleSignInEnabled") else { return true }
        return value as? Bool == true
    }

    func signIn(nonce: String, state: String, completion: SocialSignInCompletion<Void>) {
        guard appleController == nil, !googleInProgress else {
            completion.finish(token: nil, error: "Вход уже выполняется [ASI-B]")
            return
        }
        guard let window = Self.presentationWindow else {
            completion.finish(token: nil, error: "Не удалось открыть окно Apple [ASI-W]")
            return
        }
        appleCompletion = completion
        appleWindow = window
        expectedState = state
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.email]
        request.nonce = nonce
        request.state = state
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        appleController = controller
        controller.performRequests()
    }

    func cancelApple(state: String) {
        guard expectedState == state else { return }
        appleController?.cancel()
        finishApple(token: nil, error: "APPLE_SIGN_IN_CANCELLED")
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        // Captured before presenting so scene changes cannot switch the authorization anchor.
        appleWindow ?? ASPresentationAnchor()
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard controller === appleController else { return }
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            finishApple(token: nil, error: "Apple не вернул данные для входа. Попробуйте ещё раз. [ASI-C]")
            return
        }
        guard credential.state == expectedState else {
            finishApple(token: nil, error: "Не удалось проверить ответ Apple. Попробуйте ещё раз. [ASI-S]")
            return
        }
        guard let data = credential.identityToken,
              let token = String(data: data, encoding: .utf8), !token.isEmpty else {
            finishApple(token: nil, error: "Apple не вернул данные для входа. Попробуйте ещё раз. [ASI-T]")
            return
        }
        finishApple(token: token, error: nil)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        guard controller === appleController else { return }
        let cancelled = (error as? ASAuthorizationError)?.code == .canceled
        if cancelled {
            finishApple(token: nil, error: "APPLE_SIGN_IN_CANCELLED")
            return
        }
        let nativeError = error as NSError
        let domain = Self.safeErrorDomain(nativeError.domain)
        NSLog("Apple Sign-In failed: domain=%@ code=%ld", domain ?? "<invalid>", nativeError.code)
        let domainSuffix = domain.map { " D:\($0)" } ?? ""
        finishApple(token: nil, error: "Не удалось войти через Apple. Попробуйте ещё раз. [ASI-E\(nativeError.code)\(domainSuffix)]")
    }

    private func finishApple(token: String?, error: String?) {
        let completion = appleCompletion
        appleCompletion = nil
        appleController = nil
        appleWindow = nil
        expectedState = nil
        completion?.finish(token: token, error: error)
    }

    private func signInWithGoogle(_ completion: SocialSignInCompletion<KotlinUnit>) {
        guard let presenter = beginGoogle(completion) else { return }
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { [weak self] result, error in
            let token = result?.user.idToken?.tokenString
            let receivedResult = result != nil
            // GIDSignIn.h guarantees Main, but its callback omits that annotation.
            // Assert that SDK contract; keep the non-Sendable SDK result local.
            MainActor.assumeIsolated {
                self?.googleInProgress = false
                if let error { completion.finish(token: nil, error: Self.googleFailure(error as NSError)) }
                else if receivedResult { self?.completeGoogle(token, completion: completion) }
                else { completion.finish(token: nil, error: "Не удалось войти через Google. Попробуйте ещё раз. [GSI-R]") }
            }
        }
    }

    private func beginGoogle(_ completion: SocialSignInCompletion<KotlinUnit>) -> UIViewController? {
        guard !googleInProgress, appleController == nil else {
            completion.finish(token: nil, error: "Вход уже выполняется")
            return nil
        }
        guard var presenter = Self.presentationWindow?.rootViewController else {
            completion.finish(token: nil, error: "Не удалось открыть окно Google")
            return nil
        }
        while let presented = presenter.presentedViewController { presenter = presented }
        googleInProgress = true
        return presenter
    }

    private func completeGoogle(_ token: String?, completion: SocialSignInCompletion<KotlinUnit>) {
        if let token, !token.isEmpty {
            completion.finish(token: token, error: nil)
        } else {
            NSLog("Google Sign-In failed: missing ID token")
            completion.finish(token: nil, error: "Не удалось войти через Google. Попробуйте ещё раз. [GSI-T]")
        }
    }

    private static func googleFailure(_ error: NSError) -> String {
        if error.domain == kGIDSignInErrorDomain && error.code == googleCancellationCode {
            return "Вход через Google отменён"
        }
        let domain = safeErrorDomain(error.domain)
        NSLog("Google Sign-In failed: domain=%@ code=%ld", domain ?? "<invalid>", error.code)
        let domainSuffix = domain.map { " D:\($0)" } ?? ""
        let oauthSuffix = safeOAuthErrorIdentifier(error).map { " O:\($0)" } ?? ""
        return "Не удалось войти через Google. Попробуйте ещё раз. [GSI-E\(error.code)\(domainSuffix)\(oauthSuffix)]"
    }

    private static func safeErrorDomain(_ domain: String) -> String? {
        let allowed = CharacterSet(charactersIn: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._-")
        guard !domain.isEmpty, domain.count <= 80,
              domain.unicodeScalars.allSatisfy({ allowed.contains($0) }) else { return nil }
        return domain
    }

    private static func safeOAuthErrorIdentifier(_ error: NSError) -> String? {
        guard error.domain == "org.openid.appauth.oauth_token",
              let response = error.userInfo["OIDOAuthErrorResponseErrorKey"] as? [String: Any],
              let identifier = response["error"] as? String,
              let first = identifier.unicodeScalars.first,
              CharacterSet.letters.contains(first),
              identifier.count <= 40 else { return nil }
        let allowed = CharacterSet(charactersIn: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_")
        guard identifier.unicodeScalars.allSatisfy({ allowed.contains($0) }) else { return nil }
        return identifier
    }

    private static var presentationWindow: UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }
            .flatMap(\.windows).first(where: \.isKeyWindow)
    }
}

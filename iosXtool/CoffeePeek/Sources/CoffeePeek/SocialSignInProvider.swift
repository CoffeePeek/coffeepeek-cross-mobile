import AuthenticationServices
import ComposeApp
import GoogleSignIn
import UIKit

/// Shared by the Xcode and xtool app entry points. All provider UI runs on the main queue.
final class SocialSignInProvider: NSObject, IosAppleAuthProvider,
    ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    static let shared = SocialSignInProvider()
    private var appleController: ASAuthorizationController?
    private var appleCompletion: (@Sendable (String?, String?) -> Void)?
    private var appleWindow: UIWindow?
    private var expectedState: String?
    private var googleInProgress = false
    // Public NSError contract: kGIDSignInErrorCodeCanceled = -5.
    // SDK 10 imports its NS_ERROR_ENUM differently from earlier SDK versions.
    private static let googleCancellationCode = -5

    func configure() {
        IosAppleAuthRegistry.shared.provider = self
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
            DispatchQueue.main.async { self?.signInWithGoogle(completion) }
        }, signOut: {
            DispatchQueue.main.async { GIDSignIn.sharedInstance.signOut() }
        })
    }

    func signIn(nonce: String, state: String, completion: @escaping @Sendable (String?, String?) -> Void) {
        guard appleController == nil, !googleInProgress else {
            completion(nil, "Вход уже выполняется [ASI-B]")
            return
        }
        guard let window = Self.presentationWindow else {
            completion(nil, "Не удалось открыть окно Apple [ASI-W]")
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

    func cancel() {
        DispatchQueue.main.async { [weak self] in
            if #available(iOS 16.0, *) { self?.appleController?.cancel() }
            self?.finishApple(token: nil, error: "APPLE_SIGN_IN_CANCELLED")
        }
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
        completion?(token, error)
    }

    private func signInWithGoogle(_ completion: @escaping (String?, String?) -> KotlinUnit) {
        guard !googleInProgress, appleController == nil else {
            _ = completion(nil, "Вход уже выполняется")
            return
        }
        guard var presenter = Self.presentationWindow?.rootViewController else {
            _ = completion(nil, "Не удалось открыть окно Google")
            return
        }
        while let presented = presenter.presentedViewController { presenter = presented }
        googleInProgress = true
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { [weak self] result, error in
            self?.googleInProgress = false
            if let error = error as NSError? {
                let cancelled = error.domain == kGIDSignInErrorDomain && error.code == Self.googleCancellationCode
                if cancelled {
                    _ = completion(nil, "Вход через Google отменён")
                    return
                }
                let domain = Self.safeErrorDomain(error.domain)
                NSLog("Google Sign-In failed: domain=%@ code=%ld", domain ?? "<invalid>", error.code)
                let domainSuffix = domain.map { " D:\($0)" } ?? ""
                let oauthErrorSuffix = Self.safeOAuthErrorIdentifier(error).map { " O:\($0)" } ?? ""
                _ = completion(nil, "Не удалось войти через Google. Попробуйте ещё раз. [GSI-E\(error.code)\(domainSuffix)\(oauthErrorSuffix)]")
            } else if result == nil {
                NSLog("Google Sign-In failed: missing result")
                _ = completion(nil, "Не удалось войти через Google. Попробуйте ещё раз. [GSI-R]")
            } else if let token = result?.user.idToken?.tokenString, !token.isEmpty {
                _ = completion(token, nil)
            } else {
                NSLog("Google Sign-In failed: missing ID token")
                _ = completion(nil, "Не удалось войти через Google. Попробуйте ещё раз. [GSI-T]")
            }
        }
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

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
            completion(nil, "Вход уже выполняется")
            return
        }
        guard let window = Self.presentationWindow else {
            completion(nil, "Не удалось открыть окно Apple")
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
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              credential.state == expectedState,
              let data = credential.identityToken,
              let token = String(data: data, encoding: .utf8), !token.isEmpty else {
            finishApple(token: nil, error: "Apple не вернул данные для входа. Попробуйте ещё раз.")
            return
        }
        finishApple(token: token, error: nil)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        guard controller === appleController else { return }
        let cancelled = (error as? ASAuthorizationError)?.code == .canceled
        finishApple(token: nil, error: cancelled ? "APPLE_SIGN_IN_CANCELLED" : "Не удалось войти через Apple. Попробуйте ещё раз.")
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
                _ = completion(nil, cancelled ? "Вход через Google отменён" : "Не удалось войти через Google. Попробуйте ещё раз.")
            } else {
                _ = completion(result?.user.idToken?.tokenString, nil)
            }
        }
    }

    private static var presentationWindow: UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }
            .flatMap(\.windows).first(where: \.isKeyWindow)
    }
}

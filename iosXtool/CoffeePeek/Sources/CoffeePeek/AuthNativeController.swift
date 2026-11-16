import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeAuthController(screen: IosAuthScreen) -> UIViewController {
    switch screen {
    case let login as IosLogin:
        return AuthHostingController(screen: login, rootView: AuthStateHost(
            screen: login,
            currentState: login.currentState,
            observeState: login.observeState,
            observeEvent: login.observeSignedIn
        ) { state, strings in
            LoginScreen(state: state, strings: strings, actions: LoginScreenActions(
                emailChanged: login.emailChanged, passwordChanged: login.passwordChanged,
                submit: login.submit, forgotPassword: login.forgotPassword, register: login.register,
                back: login.back, apple: login.appleAvailable ? login.signInWithApple : nil,
                google: login.googleAvailable ? login.signInWithGoogle : nil
            ))
        })
    case let recovery as IosPasswordRecovery:
        return AuthHostingController(screen: recovery, rootView: AuthStateHost(
            screen: recovery, currentState: recovery.currentState, observeState: recovery.observeState
        ) { state, strings in
            PasswordRecoveryScreen(state: state, strings: strings, actions: PasswordRecoveryActions(
                emailChanged: recovery.emailChanged, submit: recovery.submit, back: recovery.back,
                login: recovery.login, anotherEmail: recovery.requestAnotherEmail
            ))
        })
    default:
        preconditionFailure("Unsupported iOS authentication route")
    }
}

private final class AuthHostingController<Content: View>: UIHostingController<Content> {
    private let screen: IosAuthScreen

    init(screen: IosAuthScreen, rootView: Content) {
        self.screen = screen
        super.init(rootView: rootView)
    }

    @MainActor required dynamic init?(coder: NSCoder) {
        return nil
    }

    deinit {
        screen.close()
    }
}

private struct AuthStateHost<Snapshot, Content: View>: View {
    let screen: IosAuthScreen
    let currentState: () -> Snapshot
    let observeState: (@escaping (Snapshot) -> Void) -> IosAuthObservation
    let observeEvent: (() -> IosAuthObservation)?
    let content: (Snapshot, NativeAuthStrings) -> Content

    @State private var snapshot: Snapshot
    @State private var languageTag: String
    @State private var observations: [IosAuthObservation] = []

    init(
        screen: IosAuthScreen,
        currentState: @escaping () -> Snapshot,
        observeState: @escaping (@escaping (Snapshot) -> Void) -> IosAuthObservation,
        observeEvent: (() -> IosAuthObservation)? = nil,
        @ViewBuilder content: @escaping (Snapshot, NativeAuthStrings) -> Content
    ) {
        self.screen = screen
        self.currentState = currentState
        self.observeState = observeState
        self.observeEvent = observeEvent
        self.content = content
        _snapshot = State(initialValue: currentState())
        _languageTag = State(initialValue: screen.languageTag())
    }

    var body: some View {
        content(snapshot, NativeAuthStrings(languageTag: languageTag))
            .onAppear(perform: beginObservation)
            .onDisappear(perform: endObservation)
    }

    private func beginObservation() {
        guard observations.isEmpty else { return }
        snapshot = currentState()
        languageTag = screen.languageTag()
        observations = [
            observeState { [value = $snapshot] state in value.wrappedValue = state },
            screen.observeLanguage { [value = $languageTag] tag in value.wrappedValue = tag }
        ]
        if let observeEvent { observations.append(observeEvent()) }
    }

    private func endObservation() {
        observations.forEach { $0.cancel() }
        observations = []
    }
}

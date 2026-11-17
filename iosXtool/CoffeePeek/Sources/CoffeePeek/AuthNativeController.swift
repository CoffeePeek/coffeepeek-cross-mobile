import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeAuthController(screen: IosAuthScreen) -> UIViewController {
    switch screen {
    case let login as IosLogin: return makeLoginController(login)
    case let registration as IosRegister: return makeRegisterController(registration)
    case let recovery as IosPasswordRecovery: return makeRecoveryController(recovery)
    case let checkIn as IosCheckInAuthFlow: return CheckInAuthController(flow: checkIn)
    default: preconditionFailure("Unsupported iOS authentication route")
    }
}

@MainActor
private func makeLoginController(_ login: IosLogin) -> UIViewController {
    AuthHostingController(screen: login, rootView: AuthStateHost(
        screen: login, currentState: login.currentState, observeState: login.observeState,
        observeEvent: login.observeSignedIn
    ) { state, strings in
        LoginScreen(state: state, strings: strings, actions: LoginScreenActions(
            emailChanged: login.emailChanged, passwordChanged: login.passwordChanged,
            submit: login.submit, forgotPassword: login.forgotPassword, register: login.register,
            back: login.back, apple: login.appleAvailable ? login.signInWithApple : nil,
            google: login.googleAvailable ? login.signInWithGoogle : nil
        ))
    })
}

@MainActor
private func makeRegisterController(_ registration: IosRegister) -> UIViewController {
    AuthHostingController(screen: registration, rootView: AuthStateHost(
        screen: registration, currentState: registration.currentState,
        observeState: registration.observeState, observeEvent: registration.observeSignedIn
    ) { state, strings in
        RegisterScreen(state: state, strings: strings, actions: RegisterScreenActions(
            nameChanged: registration.nameChanged, emailChanged: registration.emailChanged,
            passwordChanged: registration.passwordChanged, termsChanged: registration.termsChanged,
            analyticsChanged: registration.analyticsChanged, submit: registration.submit,
            login: registration.login, back: registration.back,
            openTerms: registration.openTerms, openPrivacy: registration.openPrivacy,
            apple: registration.appleAvailable ? registration.signInWithApple : nil,
            google: registration.googleAvailable ? registration.signInWithGoogle : nil
        ))
    })
}

@MainActor
private func makeRecoveryController(_ recovery: IosPasswordRecovery) -> UIViewController {
    AuthHostingController(screen: recovery, rootView: AuthStateHost(
        screen: recovery, currentState: recovery.currentState, observeState: recovery.observeState
    ) { state, strings in
        PasswordRecoveryScreen(state: state, strings: strings, actions: PasswordRecoveryActions(
            emailChanged: recovery.emailChanged, submit: recovery.submit, back: recovery.back,
            login: recovery.login, anotherEmail: recovery.requestAnotherEmail
        ))
    })
}

/// The outer UIKit route owns the shop handoff. Its children keep their drafts as steps change.
private final class CheckInAuthController: UIViewController {
    private let flow: IosCheckInAuthFlow
    private var observation: IosAuthObservation?
    private var login: UIViewController?
    private var registration: UIViewController?
    private var recovery: (screen: IosPasswordRecovery, controller: UIViewController)?
    private var displayed: UIViewController?

    init(flow: IosCheckInAuthFlow) {
        self.flow = flow
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { return nil }

    override func viewDidLoad() {
        super.viewDidLoad()
        show(flow.currentScreen())
        observation = flow.observeScreen { [weak self] screen in self?.show(screen) }
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        // The parent remains in its navigation stack when a sheet covers it or a tab is changed.
        if parent?.isMovingFromParent == true || parent?.navigationController == nil {
            flow.close()
        }
    }

    deinit {
        observation?.cancel()
        flow.close()
    }

    private func controller(for screen: IosAuthScreen) -> UIViewController {
        switch screen {
        case let screen as IosLogin:
            if let login { return login }
            let controller = makeLoginController(screen)
            login = controller
            return controller
        case let screen as IosRegister:
            if let registration { return registration }
            let controller = makeRegisterController(screen)
            registration = controller
            return controller
        case let screen as IosPasswordRecovery:
            if let recovery, recovery.screen === screen { return recovery.controller }
            let controller = makeRecoveryController(screen)
            recovery = (screen, controller)
            return controller
        default:
            preconditionFailure("Unsupported check-in authentication step")
        }
    }

    private func show(_ screen: IosAuthScreen) {
        let next = controller(for: screen)
        let previous = displayed
        guard next !== previous else { return }
        let visible = view.window != nil
        previous?.willMove(toParent: nil)
        if visible { previous?.beginAppearanceTransition(false, animated: false) }
        previous?.view.removeFromSuperview()
        if visible { previous?.endAppearanceTransition() }
        previous?.removeFromParent()
        addChild(next)
        if visible { next.beginAppearanceTransition(true, animated: false) }
        next.view.frame = view.bounds
        next.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(next.view)
        if visible { next.endAppearanceTransition() }
        next.didMove(toParent: self)
        displayed = next
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

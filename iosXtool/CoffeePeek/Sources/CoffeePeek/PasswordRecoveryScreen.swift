import ComposeApp
import SwiftUI

struct PasswordRecoveryActions {
    var emailChanged: (String) -> Void = { _ in }
    var submit: () -> Void = {}
    var back: () -> Void = {}
    var login: () -> Void = {}
    var anotherEmail: () -> Void = {}
}

struct PasswordRecoveryScreen: View {
    let state: IosPasswordRecoveryState
    let strings: NativeAuthStrings
    let actions: PasswordRecoveryActions

    @State private var email: String
    @FocusState private var emailFocused: Bool

    init(state: IosPasswordRecoveryState, strings: NativeAuthStrings, actions: PasswordRecoveryActions) {
        self.state = state
        self.strings = strings
        self.actions = actions
        _email = State(initialValue: state.email)
    }

    var body: some View {
        NativeAuthPage(
            title: strings.text("auth_recovery_title"),
            subtitle: strings.text(state.emailSent ? "auth_recovery_sent" : "auth_recovery_info"),
            strings: strings,
            closeIcon: true,
            back: actions.back
        ) {
            if state.emailSent {
                NativeAuthSubmit(title: strings.text("auth_recovery_back_to_login"), action: actions.login)
                Button(strings.text("auth_recovery_another_email"), action: actions.anotherEmail)
                    .frame(maxWidth: .infinity, minHeight: 44)
            } else {
                recoveryForm
            }
        }
    }

    private var recoveryForm: some View {
        VStack(alignment: .leading, spacing: 14) {
            NativeAuthField(title: strings.text("auth_email_label"), error: state.emailError.map(strings.message)) {
                NativeAuthEmailInput(strings: strings, email: emailBinding)
                    .focused($emailFocused)
                    .submitLabel(.send)
                    .onSubmit(submit)
            }
            .disabled(state.busy)
            if state.failed {
                NativeAuthError(text: strings.text("auth_recovery_failed"))
            }
            NativeAuthSubmit(
                title: strings.text(state.busy ? "auth_recovery_busy" : "auth_recovery_send"),
                busy: state.busy,
                action: submit
            )
        }
    }

    private var emailBinding: Binding<String> {
        Binding(get: { email }, set: { updated in
            email = updated
            actions.emailChanged(updated)
        })
    }

    private func submit() {
        guard !state.busy else { return }
        emailFocused = false
        actions.submit()
    }
}

#Preview("Light") {
    PasswordRecoveryScreen(
        state: IosPasswordRecoveryState(
            email: "name@example.com", busy: false, emailSent: false, emailError: nil, failed: false
        ),
        strings: NativeAuthStrings(languageTag: "ru"), actions: PasswordRecoveryActions()
    )
    .preferredColorScheme(.light)
}

#Preview("Dark") {
    PasswordRecoveryScreen(
        state: IosPasswordRecoveryState(
            email: "name@example.com", busy: false, emailSent: true, emailError: nil, failed: false
        ),
        strings: NativeAuthStrings(languageTag: "be"), actions: PasswordRecoveryActions()
    )
    .preferredColorScheme(.dark)
}

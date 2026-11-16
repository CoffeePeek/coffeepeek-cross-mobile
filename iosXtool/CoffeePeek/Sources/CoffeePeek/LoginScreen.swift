import ComposeApp
import SwiftUI

struct LoginScreenActions {
    var emailChanged: (String) -> Void = { _ in }
    var passwordChanged: (String) -> Void = { _ in }
    var submit: () -> Void = {}
    var forgotPassword: (String) -> Void = { _ in }
    var register: () -> Void = {}
    var back: () -> Void = {}
    var apple: (() -> Void)? = nil
    var google: (() -> Void)? = nil
}

struct LoginScreen: View {
    private enum Field { case email, password }

    let state: IosLoginState
    let strings: NativeAuthStrings
    let actions: LoginScreenActions

    @State private var email: String
    @State private var password = ""
    @State private var showingPassword = false
    @FocusState private var focus: Field?

    init(state: IosLoginState, strings: NativeAuthStrings, actions: LoginScreenActions) {
        self.state = state
        self.strings = strings
        self.actions = actions
        _email = State(initialValue: state.email)
    }

    var body: some View {
        NativeAuthPage(
            title: strings.text("auth_login_title"), subtitle: nil, strings: strings, back: actions.back
        ) {
            credentialFields
            if let error = state.failure {
                NativeAuthError(text: strings.message(error))
            }
            NativeAuthSubmit(title: strings.text("auth_login"), busy: state.busy, action: submit)
            NativeAuthFooter(
                prompt: strings.text("auth_login_no_account"), actionTitle: strings.text("auth_login_register"),
                enabled: !state.busy, action: actions.register
            )
            if actions.apple != nil || actions.google != nil {
                socialOptions
            }
        }
    }

    private var credentialFields: some View {
        VStack(alignment: .leading, spacing: 24) {
            NativeAuthField(title: strings.text("auth_email_label"), error: state.emailError.map(strings.message)) {
                NativeAuthEmailInput(strings: strings, email: emailBinding, contentType: .username)
                    .focused($focus, equals: .email)
                    .submitLabel(.next)
                    .onSubmit { focus = .password }
            }
            passwordField
        }
        .disabled(state.busy)
    }

    private var passwordField: some View {
        NativeAuthField(
            title: strings.text("auth_password_label"), error: state.passwordError.map(strings.message),
            labelActionTitle: strings.text("auth_forgot_password"), labelAction: { actions.forgotPassword(email) }
        ) {
            HStack(spacing: 0) {
                passwordInput
                Button { showingPassword.toggle() } label: {
                    Label(
                        strings.text(showingPassword ? "auth_hide_password" : "auth_show_password"),
                        systemImage: showingPassword ? "eye.slash" : "eye"
                    )
                    .labelStyle(.iconOnly)
                    .font(.system(size: 22, weight: .regular))
                    .frame(minWidth: 44, minHeight: 44).contentShape(Rectangle())
                }
                .buttonStyle(.plain).foregroundStyle(CoffeePeekNativeStyle.foreground)
            }
        }
    }

    private var passwordInput: some View {
        Group {
            if showingPassword {
                TextField(strings.text("auth_password_label"), text: passwordBinding,
                          prompt: Text(strings.text("auth_password_label")).foregroundStyle(CoffeePeekNativeStyle.secondary))
            } else {
                SecureField(strings.text("auth_password_label"), text: passwordBinding,
                            prompt: Text(strings.text("auth_password_label")).foregroundStyle(CoffeePeekNativeStyle.secondary))
            }
        }
        .textContentType(.password)
        .textInputAutocapitalization(.never)
        .autocorrectionDisabled()
        .focused($focus, equals: .password)
        .submitLabel(.go)
        .onSubmit(submit)
    }

    private var socialOptions: some View {
        VStack(spacing: 10) {
            HStack(spacing: 12) {
                Rectangle().fill(CoffeePeekNativeStyle.border).frame(height: 0.5)
                Text(strings.text("auth_or"))
                    .font(CoffeePeekNativeStyle.font(13, relativeTo: .footnote))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
                Rectangle().fill(CoffeePeekNativeStyle.border).frame(height: 0.5)
            }
            .fixedSize(horizontal: false, vertical: true)
            if let apple = actions.apple {
                Button(action: apple) {
                    Label(strings.text("auth_sign_in_apple"), systemImage: "apple.logo")
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
            }
            if let google = actions.google {
                Button(action: google) {
                    Text(strings.text("auth_sign_in_google"))
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
            }
        }
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .disabled(state.busy)
    }

    private var emailBinding: Binding<String> {
        Binding(get: { email }, set: { updated in
            email = updated
            actions.emailChanged(updated)
        })
    }

    private var passwordBinding: Binding<String> {
        Binding(get: { password }, set: { updated in
            password = updated
            actions.passwordChanged(updated)
        })
    }

    private func submit() {
        guard !state.busy else { return }
        focus = nil
        actions.submit()
    }
}

#Preview("Light") {
    LoginScreen(
        state: IosLoginState(email: "", emailError: nil, passwordError: nil, failure: nil, busy: false),
        strings: NativeAuthStrings(languageTag: "ru"), actions: LoginScreenActions()
    )
    .preferredColorScheme(.light)
}

#Preview("Dark") {
    LoginScreen(
        state: IosLoginState(email: "name@example.com", emailError: nil, passwordError: nil, failure: nil, busy: false),
        strings: NativeAuthStrings(languageTag: "be"), actions: LoginScreenActions(apple: {}, google: {})
    )
    .preferredColorScheme(.dark)
}

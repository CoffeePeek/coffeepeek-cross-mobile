import ComposeApp
import SwiftUI

struct RegisterScreenActions {
    var nameChanged: (String) -> Void = { _ in }
    var emailChanged: (String) -> Void = { _ in }
    var passwordChanged: (String) -> Void = { _ in }
    var termsChanged: (Bool) -> Void = { _ in }
    var analyticsChanged: (Bool) -> Void = { _ in }
    var submit: () -> Void = {}
    var login: () -> Void = {}
    var back: () -> Void = {}
    var openTerms: () -> Void = {}
    var openPrivacy: () -> Void = {}
    var apple: (() -> Void)? = nil
    var google: (() -> Void)? = nil
}

struct RegisterScreen: View {
    private enum Field { case email, name, password }
    let state: IosRegisterState
    let strings: NativeAuthStrings
    let actions: RegisterScreenActions
    @State private var email: String
    @State private var name: String
    @State private var password = ""
    @State private var showingPassword = false
    @FocusState private var focus: Field?

    init(state: IosRegisterState, strings: NativeAuthStrings, actions: RegisterScreenActions) {
        self.state = state
        self.strings = strings
        self.actions = actions
        _email = State(initialValue: state.email)
        _name = State(initialValue: state.name)
    }

    var body: some View {
        NativeAuthPage(title: strings.text("auth_register_title"), subtitle: nil,
                       strings: strings, back: actions.back) {
            if state.success { successContent } else { form }
        }
        .onChange(of: state.success) { _, success in
            if success {
                password = ""
                focus = nil
            }
        }
    }

    private var form: some View {
        VStack(alignment: .leading, spacing: 20) {
            fields.disabled(state.busy)
            RegisterConsent(
                strings: strings,
                termsAccepted: Binding(get: { state.termsAccepted }, set: { actions.termsChanged($0) }),
                analyticsAccepted: Binding(get: { state.analyticsAccepted }, set: { actions.analyticsChanged($0) }),
                termsError: state.termsError.map(strings.message),
                openTerms: actions.openTerms, openPrivacy: actions.openPrivacy
            )
            .disabled(state.busy)
            if let error = state.failureText {
                NativeAuthError(text: error)
            } else if let error = state.failure {
                NativeAuthError(text: strings.message(error))
            }
            NativeAuthSubmit(title: strings.text("auth_register_submit"), busy: state.busy,
                             enabled: state.canSubmit, action: submit)
            NativeAuthFooter(prompt: strings.text("auth_already_have_account"),
                             actionTitle: strings.text("auth_login"), accented: true,
                             enabled: !state.busy, action: actions.login)
            if actions.apple != nil || actions.google != nil { socialOptions }
        }
    }

    private var fields: some View {
        VStack(alignment: .leading, spacing: 18) {
            NativeAuthField(title: strings.text("auth_email_label"), error: state.emailError.map(strings.message)) {
                NativeAuthEmailInput(strings: strings, email: emailBinding, contentType: .username)
                    .focused($focus, equals: .email)
                    .submitLabel(.next)
                    .onSubmit { focus = .name }
            }
            NativeAuthField(title: strings.text("auth_name_label"), error: state.nameError.map(strings.message)) {
                TextField(strings.text("auth_name_label"), text: nameBinding,
                          prompt: Text(strings.text("auth_name_placeholder"))
                            .foregroundStyle(CoffeePeekNativeStyle.secondary))
                    .textContentType(.name)
                    .textInputAutocapitalization(.words)
                    .focused($focus, equals: .name)
                    .submitLabel(.next)
                    .onSubmit { focus = .password }
            }
            NativeAuthField(title: strings.text("auth_password_label"), error: state.passwordError.map(strings.message),
                            hint: strings.text("auth_register_password_placeholder")) {
                HStack {
                    passwordInput
                    passwordVisibility
                }
            }
        }
    }

    private var passwordInput: some View {
        Group {
            if showingPassword {
                TextField(strings.text("auth_password_label"), text: passwordBinding,
                          prompt: Text(strings.text("auth_password_label"))
                            .foregroundStyle(CoffeePeekNativeStyle.secondary))
            } else {
                SecureField(strings.text("auth_password_label"), text: passwordBinding,
                            prompt: Text(strings.text("auth_password_label"))
                                .foregroundStyle(CoffeePeekNativeStyle.secondary))
            }
        }
        .textContentType(.newPassword)
        .textInputAutocapitalization(.never)
        .autocorrectionDisabled()
        .focused($focus, equals: .password)
        .submitLabel(.done)
        .onSubmit(submit)
    }

    private var passwordVisibility: some View {
        Button { showingPassword.toggle() } label: {
            Label(strings.text(showingPassword ? "auth_hide_password" : "auth_show_password"),
                  systemImage: showingPassword ? "eye.slash" : "eye")
                .labelStyle(.iconOnly)
                .frame(minWidth: 44, minHeight: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var successContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text(strings.text("auth_register_success_title"))
                .font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
                .accessibilityAddTraits(.isHeader)
            Text(strings.registrationSuccess(email: state.email))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
                .fixedSize(horizontal: false, vertical: true)
            NativeAuthSubmit(title: strings.text("auth_go_to_login"), action: actions.login)
        }
    }

    private var socialOptions: some View {
        VStack(spacing: 10) {
            Text(strings.text("auth_or"))
                .font(CoffeePeekNativeStyle.font(13)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            if let apple = actions.apple {
                CoffeePeekAppleSignInButton(action: apple)
            }
            if let google = actions.google {
                CoffeePeekGoogleSignInButton(title: strings.text("auth_sign_in_google"), action: google)
            }
        }
        .disabled(state.busy)
    }

    private var emailBinding: Binding<String> {
        Binding(get: { email }, set: { value in
            email = value
            actions.emailChanged(value)
        })
    }

    private var nameBinding: Binding<String> {
        Binding(get: { name }, set: { value in
            name = value
            actions.nameChanged(value)
        })
    }

    private var passwordBinding: Binding<String> {
        Binding(get: { password }, set: { value in
            password = value
            actions.passwordChanged(value)
        })
    }

    private func submit() {
        guard !state.busy, state.canSubmit, !state.success else { return }
        focus = nil
        actions.submit()
    }
}

private var registerPreviewState: IosRegisterState {
    IosRegisterState(name: "", email: "", termsAccepted: false, analyticsAccepted: true,
                     nameError: nil, emailError: nil, passwordError: nil, termsError: nil,
                     failure: nil, failureText: nil, busy: false, canSubmit: false, success: false)
}

#Preview("Register Light") {
    RegisterScreen(state: registerPreviewState, strings: NativeAuthStrings(languageTag: "ru"),
                   actions: RegisterScreenActions()).preferredColorScheme(.light)
}

#Preview("Register Dark") {
    RegisterScreen(state: registerPreviewState, strings: NativeAuthStrings(languageTag: "be"),
                   actions: RegisterScreenActions()).preferredColorScheme(.dark)
}

import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeUsernameSelectionController(source: IosUsernameSelection) -> UIViewController {
    UIHostingController(rootView: UsernameSelectionNativeScreen(source: source))
}

@MainActor
private final class UsernameSelectionNativeModel: ObservableObject {
    let source: IosUsernameSelection
    @Published var state: IosUsernameSelectionState
    @Published var language: String
    private var observation: IosAuthObservation?
    private var languageObservation: IosAuthObservation?

    init(source: IosUsernameSelection) {
        self.source = source
        state = source.currentState()
        language = source.languageTag()
        observation = source.observeState { [weak self] in self?.state = $0 }
        languageObservation = source.observeLanguage { [weak self] in self?.language = $0 }
    }

    isolated deinit {
        observation?.cancel()
        languageObservation?.cancel()
    }
}

private struct UsernameSelectionNativeScreen: View {
    @StateObject private var model: UsernameSelectionNativeModel

    init(source: IosUsernameSelection) {
        _model = StateObject(wrappedValue: UsernameSelectionNativeModel(source: source))
    }

    var body: some View {
        UsernameSelectionNativeContent(state: model.state, strings: NativeAuthStrings(languageTag: model.language),
            edit: model.source.edit, submit: model.source.submit, retry: model.source.retry, signOut: model.source.signOut)
    }
}

private struct UsernameSelectionNativeContent: View {
    let state: IosUsernameSelectionState
    let strings: NativeAuthStrings
    let edit: (String) -> Void
    let submit: () -> Void
    let retry: () -> Void
    let signOut: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                Text(strings.text("auth_username_title"))
                    .font(CoffeePeekNativeStyle.font(28, weight: "Bold")).accessibilityAddTraits(.isHeader)
                Text(strings.text("auth_username_description"))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
                if state.editing { editor }
                else if state.failed {
                    NativeAuthError(text: strings.text("auth_username_unavailable"))
                    NativeAuthSubmit(title: strings.text("auth_username_retry"), busy: false, enabled: true, action: retry)
                } else { ProgressView().frame(maxWidth: .infinity) }
                Button(strings.text("auth_username_sign_out"), action: signOut)
                    .buttonStyle(.plain).frame(minHeight: 44)
            }
            .font(CoffeePeekNativeStyle.font(17)).frame(maxWidth: 460)
            .padding(24).frame(maxWidth: .infinity)
        }
        .background(CoffeePeekNativeStyle.background).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .interactiveDismissDisabled()
    }

    private var editor: some View {
        VStack(alignment: .leading, spacing: 16) {
            NativeAuthField(title: strings.text("auth_username_label"),
                error: state.errorKey.map(strings.text), hint: strings.text("auth_username_rules")) {
                TextField(strings.text("auth_username_label"), text: Binding(get: { state.username }, set: edit))
                    .textContentType(.username).textInputAutocapitalization(.never).autocorrectionDisabled()
                    .submitLabel(.continue).onSubmit(submit).disabled(state.busy)
            }
            Text(strings.text("auth_username_link_notice"))
                .font(CoffeePeekNativeStyle.font(13, relativeTo: .footnote))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
            NativeAuthSubmit(title: strings.text("auth_username_continue"), busy: state.busy,
                enabled: state.canSubmit, action: submit)
        }
    }
}

#Preview("Username selection light") {
    UsernameSelectionNativeContent(state: IosUsernameSelectionState(username: "coffee_lover", editing: true,
        busy: false, failed: false, canSubmit: true, errorKey: nil), strings: NativeAuthStrings(languageTag: "ru"),
        edit: { _ in }, submit: {}, retry: {}, signOut: {}).preferredColorScheme(.light)
}

#Preview("Username selection dark") {
    UsernameSelectionNativeContent(state: IosUsernameSelectionState(username: "coffee_lover", editing: true,
        busy: false, failed: false, canSubmit: true, errorKey: "auth_username_taken"), strings: NativeAuthStrings(languageTag: "be"),
        edit: { _ in }, submit: {}, retry: {}, signOut: {}).preferredColorScheme(.dark)
}

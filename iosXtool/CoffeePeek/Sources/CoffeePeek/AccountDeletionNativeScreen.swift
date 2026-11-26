import ComposeApp
import SwiftUI

struct AccountDeletionNativeScreen: View {
    let state: IosAccountDeletionState
    let strings: OwnProfileStrings
    let send: () -> Void
    let dismissError: () -> Void
    let back: () -> Void

    var body: some View {
        SettingsNativePage(title: strings["profile_deletion_title"], backLabel: strings["profile_back"], back: back) {
            if state.revoked { Color.clear }
            else if state.loading { CoffeePeekNativeLoader(label: strings["profile_loading"], size: 32, showsLabel: true).frame(maxWidth: .infinity, minHeight: 180) }
            else { confirmation }
        }
        .alert(strings["profile_error_title"], isPresented: errorPresented) {
            Button(strings["profile_ok"], action: dismissError)
        } message: { Text(state.serverError ?? strings[state.errorKey ?? "profile_deletion_send_failed"]) }
    }

    private var confirmation: some View {
        VStack(spacing: 20) {
            Text(strings[state.requested ? "profile_deletion_sent_title" : "profile_deletion_confirm_title"])
                .font(CoffeePeekNativeStyle.font(26, weight: "Bold", relativeTo: .title))
                .accessibilityAddTraits(.isHeader)
            Text(String(format: strings[state.requested ? "profile_deletion_sent_subtitle" : "profile_deletion_confirm_subtitle"],
                        state.email.isEmpty ? strings["profile_deletion_confirm_no_email"] : state.email))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
            if state.requested {
                Text(strings["profile_deletion_wait_hint"]).font(.footnote).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
            CoffeePeekNativeActionButton(title: strings[sendKey], busy: state.sending, enabled: state.canSend, action: send)
            CoffeePeekNativeActionButton(title: strings["profile_close"], primary: false, action: back)
        }
        .multilineTextAlignment(.center)
        .padding(.vertical, 32)
        .frame(maxWidth: 480)
        .frame(maxWidth: .infinity)
    }

    private var sendKey: String {
        if !state.requested { return "profile_deletion_send" }
        return state.resendAvailable ? "profile_deletion_resend" : "profile_deletion_resend_wait"
    }

    private var errorPresented: Binding<Bool> {
        Binding(get: { !state.revoked && (state.errorKey != nil || state.serverError != nil) },
                set: { if !$0 { dismissError() } })
    }
}

@MainActor
private func deletionPreview(_ language: String, requested: Bool) -> some View {
    AccountDeletionNativeScreen(state: IosAccountDeletionState(email: "fixture@example.invalid", loading: false,
        sending: false, requested: requested, canSend: !requested, resendAvailable: false, revoked: false,
        errorKey: nil, serverError: nil), strings: OwnProfileStrings(languageTag: language, table: "NativeAccountDeletion"),
        send: {}, dismissError: {}, back: {})
}

#Preview("Account deletion light") { deletionPreview("ru", requested: false).preferredColorScheme(.light) }
#Preview("Account deletion dark") { deletionPreview("be", requested: false).preferredColorScheme(.dark) }
#Preview("Deletion pending light") { deletionPreview("ru", requested: true).preferredColorScheme(.light) }
#Preview("Deletion pending dark") { deletionPreview("be", requested: true).preferredColorScheme(.dark) }

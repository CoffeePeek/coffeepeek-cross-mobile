import ComposeApp
import SwiftUI

struct EditProfileNativeContent: View {
    let state: IosEditProfileState
    let strings: EditProfileNativeStrings
    let actions: EditProfileNativeActions
    @State private var photoSources = false
    @State private var deleteConfirmation = false

    var body: some View {
        VStack(spacing: 0) {
            header
            if state.revoked { Color.clear }
            else if state.loading { ProgressView(strings["profile_loading"]).frame(maxHeight: .infinity) }
            else if state.loadFailed { loadFailure }
            else { form }
        }
        .font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea()).tint(CoffeePeekNativeStyle.accent)
        .confirmationDialog(strings["profile_edit_photo_title"], isPresented: $photoSources, titleVisibility: .visible) {
            Button(strings["profile_edit_gallery"]) { actions.choosePhoto(false) }
            Button(strings["profile_edit_camera"]) { actions.choosePhoto(true) }
            Button(strings["profile_cancel"], role: .cancel) {}
        }
        .alert(strings["profile_edit_confirm_username_title"], isPresented: usernameConfirmation) {
            Button(strings["profile_edit_confirm_username_action"], action: actions.confirmUsername)
            Button(strings["profile_cancel"], role: .cancel, action: actions.cancelUsername)
        } message: { Text(strings.format("profile_edit_confirm_username_message", state.usernameToConfirm ?? "")) }
        .alert(strings[state.errorTitleKey ?? "profile_edit_error_title"], isPresented: errorPresented) {
            Button(strings["profile_edit_error_dismiss"], action: actions.dismissError)
        } message: { Text(errorMessage) }
        .confirmationDialog(strings["profile_edit_delete_title"], isPresented: $deleteConfirmation, titleVisibility: .visible) {
            Button(strings["profile_edit_delete_confirm"], role: .destructive, action: actions.deleteAccount)
            Button(strings["profile_cancel"], role: .cancel) {}
        } message: { Text(strings["profile_edit_delete_message"]) }
    }

    private var header: some View {
        CoffeePeekNativeNavigationHeader(title: strings["profile_edit_title"],
            backLabel: strings["profile_back"], back: actions.back).disabled(state.saving)
    }

    private var form: some View {
        ScrollView {
            VStack(spacing: 24) {
                EditProfileNativeAvatar(state: state, strings: strings, load: actions.loadImage) { photoSources = true }
                    .frame(maxWidth: .infinity).padding(.top, 12)
                EditProfileNativeFields(state: state, strings: strings, actions: actions)
                Button(action: actions.save) {
                    Group {
                        if state.saving { ProgressView(strings["profile_edit_saving"]) }
                        else { Text(strings["profile_edit_save"]) }
                    }.frame(maxWidth: .infinity).frame(minHeight: 48)
                }.buttonStyle(.plain).font(CoffeePeekNativeStyle.font(16, weight: "SemiBold"))
                    .foregroundStyle(.black).background(CoffeePeekNativeStyle.accent, in: Capsule())
                    .opacity(state.canSave || state.saving ? 1 : 0.4).disabled(!state.canSave)
                Button(strings["profile_edit_delete_account"], role: .destructive) { deleteConfirmation = true }
                    .buttonStyle(.plain).foregroundStyle(.red).frame(minHeight: 44).disabled(state.saving)
            }.padding(.horizontal, 20).padding(.bottom, 32)
        }.scrollDismissesKeyboard(.interactively)
    }

    private var loadFailure: some View {
        VStack(spacing: 16) {
            Text(strings["profile_load_failed"])
            Button(strings["profile_retry"], action: actions.retry)
        }.frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    private var usernameConfirmation: Binding<Bool> {
        Binding(get: { state.usernameToConfirm != nil }, set: { _ in })
    }
    private var errorPresented: Binding<Bool> {
        Binding(get: { state.errorMessageKey != nil }, set: { if !$0 { actions.dismissError() } })
    }
    private var errorMessage: String {
        if state.errorMessageKey == "profile_edit_error_username_limit", let raw = state.retryAtUtc,
           let date = ISO8601DateFormatter().date(from: raw) ?? fractionalDate(raw) {
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: strings.languageTag)
            formatter.dateStyle = .medium
            formatter.timeStyle = .short
            return strings.format("profile_edit_error_username_retry_at", formatter.string(from: date))
        }
        return strings[state.errorMessageKey ?? "profile_edit_error_title"]
    }
    private func fractionalDate(_ raw: String) -> Date? {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.date(from: raw)
    }
}

var editProfilePreviewState: IosEditProfileState {
    IosEditProfileState(username: "coffee_lover", about: "Люблю кофе и новые места", avatarUrl: nil, avatar: nil,
        loading: false, saving: false, canSave: true, revoked: false, photoLoading: false,
        usernameErrorKey: nil, aboutErrorKey: nil, usernameToConfirm: nil,
        errorTitleKey: nil, errorMessageKey: nil, retryAtUtc: nil, loadFailed: false)
}

#Preview("Edit profile light") {
    EditProfileNativeContent(state: editProfilePreviewState, strings: .init(languageTag: "ru"), actions: .init())
}
#Preview("Edit profile dark") {
    EditProfileNativeContent(state: editProfilePreviewState, strings: .init(languageTag: "be"), actions: .init()).preferredColorScheme(.dark)
}

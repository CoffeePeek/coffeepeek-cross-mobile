import ComposeApp
import SwiftUI
import UIKit

struct EditProfileNativeFields: View {
    let state: IosEditProfileState
    let strings: EditProfileNativeStrings
    let actions: EditProfileNativeActions
    @State private var username: String
    @State private var about: String
    @FocusState private var focus: Field?
    private enum Field { case username, about }

    init(state: IosEditProfileState, strings: EditProfileNativeStrings, actions: EditProfileNativeActions) {
        self.state = state
        self.strings = strings
        self.actions = actions
        _username = State(initialValue: state.username)
        _about = State(initialValue: state.about)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 24) {
            field(title: "profile_edit_username_label", issue: state.usernameErrorKey, count: username.utf16.count, limit: 64) {
                TextField(strings["profile_edit_username_placeholder"], text: $username)
                    .textContentType(.nickname).textInputAutocapitalization(.words)
                    .submitLabel(.next).focused($focus, equals: .username)
                    .onSubmit { focus = .about }
                    .padding(.horizontal, 16).frame(minHeight: 48)
            }
            field(title: "profile_edit_about_label", issue: state.aboutErrorKey, count: about.utf16.count, limit: 600) {
                TextField(strings["profile_edit_about_placeholder"], text: $about, axis: .vertical)
                    .lineLimit(4...8).textInputAutocapitalization(.sentences)
                    .focused($focus, equals: .about).padding(16)
            }
        }
        .disabled(state.saving)
        .onChange(of: username) { _, value in
            let limited = limitedText(value, to: 64)
            if limited != value { username = limited }
            if limited != state.username { actions.username(limited) }
        }
        .onChange(of: about) { _, value in
            let limited = limitedText(value, to: 600)
            if limited != value { about = limited }
            if limited != state.about { actions.about(limited) }
        }
        .onChange(of: state.username) { _, value in if value != username { username = value } }
        .onChange(of: state.about) { _, value in if value != about { about = value } }
        .onChange(of: state.saving) { _, saving in if saving { focus = nil } }
        .onChange(of: state.usernameToConfirm) { _, value in if value != nil { focus = nil } }
    }

    private func limitedText(_ value: String, to limit: Int) -> String {
        var result = value
        while result.utf16.count > limit { result.removeLast() }
        return result
    }

    private func field<Content: View>(title: String, issue: String?, count: Int, limit: Int,
                                      @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings[title]).font(CoffeePeekNativeStyle.font(14, weight: "Medium"))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
            content().font(CoffeePeekNativeStyle.font(16)).textFieldStyle(.plain)
                .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(issue == nil ? CoffeePeekNativeStyle.border : .red))
                .accessibilityLabel(strings[title])
            HStack(alignment: .top) {
                if let issue { Text(strings[issue]).foregroundStyle(.red) }
                Spacer(minLength: 8)
                Text("\(count)/\(limit)").foregroundStyle(CoffeePeekNativeStyle.secondary)
            }.font(CoffeePeekNativeStyle.font(12))
        }
    }
}

struct EditProfileNativeAvatar: View {
    let state: IosEditProfileState
    let strings: EditProfileNativeStrings
    let load: (String, @escaping (UIImage?) -> Void) -> IosEditProfileObservation?
    let onPick: () -> Void
    @State private var remote: UIImage?
    @State private var observation: IosEditProfileObservation?
    private var image: UIImage? { state.avatar.flatMap { UIImage(data: $0 as Data) } ?? remote }

    var body: some View {
        Button(action: onPick) {
            ZStack(alignment: .bottomTrailing) {
                ZStack {
                    Circle().fill(CoffeePeekNativeStyle.accent.gradient)
                    if let image { Image(uiImage: image).resizable().scaledToFill() }
                    else { Text(String(state.username.prefix(2)).uppercased()).font(CoffeePeekNativeStyle.font(30, weight: "Bold")) }
                    if state.photoLoading { ProgressView().tint(.black) }
                }.frame(width: 96, height: 96).clipShape(Circle())
                Image(systemName: "camera").font(.system(size: 16)).frame(width: 32, height: 32)
                    .background(CoffeePeekNativeStyle.surface, in: Circle())
                    .overlay(Circle().strokeBorder(CoffeePeekNativeStyle.border))
            }
        }.buttonStyle(.plain).disabled(state.saving || state.photoLoading)
            .accessibilityLabel(strings["profile_edit_change_photo"])
            .onAppear(perform: observeImage)
            .onChange(of: state.avatarUrl) { _, _ in observeImage() }
            .onDisappear { observation?.cancel(); observation = nil }
    }
    private func observeImage() {
        observation?.cancel()
        observation = nil
        remote = nil
        if let url = state.avatarUrl { observation = load(url) { remote = $0 } }
    }
}

#Preview("Edit fields light") {
    EditProfileNativeFields(state: editProfilePreviewState, strings: .init(languageTag: "ru"), actions: .init()).padding()
}
#Preview("Edit fields dark") {
    EditProfileNativeFields(state: editProfilePreviewState, strings: .init(languageTag: "ru"), actions: .init()).padding().preferredColorScheme(.dark)
}

#Preview("Edit avatar light") {
    EditProfileNativeAvatar(state: editProfilePreviewState, strings: .init(languageTag: "ru"), load: { _, _ in nil }, onPick: {}).padding()
}
#Preview("Edit avatar dark") {
    EditProfileNativeAvatar(state: editProfilePreviewState, strings: .init(languageTag: "ru"), load: { _, _ in nil }, onPick: {}).padding().preferredColorScheme(.dark)
}

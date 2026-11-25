import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeBlockedAuthorsController(
    authors: IosBlockedAuthors,
    languageTag: String,
    onBack: @escaping () -> KotlinUnit,
    onLogin: @escaping () -> KotlinUnit,
    onRegister: @escaping () -> KotlinUnit
) -> UIViewController {
    UIHostingController(rootView: BlockedAuthorsHost(
        authors: authors,
        strings: BlockedAuthorsStrings(languageTag: languageTag),
        onBack: { _ = onBack() },
        onLogin: { _ = onLogin() },
        onRegister: { _ = onRegister() }
    ))
}

private struct BlockedAuthorsHost: View {
    let authors: IosBlockedAuthors
    let strings: BlockedAuthorsStrings
    let onBack: () -> Void
    let onLogin: () -> Void
    let onRegister: () -> Void
    @State private var state: IosBlockedAuthorsState
    @State private var observation: IosBlockedAuthorsObservation?
    @State private var alert: BlockedAuthorsAlert?

    init(authors: IosBlockedAuthors, strings: BlockedAuthorsStrings, onBack: @escaping () -> Void,
         onLogin: @escaping () -> Void, onRegister: @escaping () -> Void) {
        self.authors = authors
        self.strings = strings
        self.onBack = onBack
        self.onLogin = onLogin
        self.onRegister = onRegister
        _state = State(initialValue: authors.currentState())
    }

    var body: some View {
        BlockedAuthorsScreen(state: state, strings: strings, onBack: onBack,
            onLogin: onLogin, onRegister: onRegister, onRefresh: authors.refresh,
            onUnblock: { authors.requestUnblock(authorId: $0) })
            .alert(item: $alert, content: makeAlert)
            .onAppear(perform: startObserving)
            .onDisappear {
                observation?.cancel()
                observation = nil
                alert = nil
            }
    }

    private func startObserving() {
        guard observation == nil else { return }
        state = authors.currentState()
        alert = BlockedAuthorsAlert(state: state)
        observation = authors.observe { [state = $state, alert = $alert] snapshot in
            state.wrappedValue = snapshot
            alert.wrappedValue = BlockedAuthorsAlert(state: snapshot)
        }
        authors.refresh()
    }

    private func makeAlert(_ item: BlockedAuthorsAlert) -> Alert {
        if item.login {
            return Alert(title: Text(strings.text("author_blocking_login_title")),
                message: Text(strings.text("author_blocking_login_message")),
                primaryButton: .default(Text(strings.text("author_blocking_login"))) {
                    authors.loginHandled()
                    onLogin()
                }, secondaryButton: .cancel(Text(strings.text("author_blocking_cancel")), action: authors.loginHandled))
        }
        return promptAlert(item)
    }

    private func promptAlert(_ item: BlockedAuthorsAlert) -> Alert {
        let operation = item.blocking ? "block" : "unblock"
        let title = item.failed ? strings.text("author_blocking_save_failed")
            : strings.authorTitle("author_blocking_\(operation)_title", name: item.name)
        let message = strings.text(item.failed ? "author_blocking_save_failed_message" : "author_blocking_\(operation)_message")
        let label = Text(strings.text(item.failed ? "author_blocking_retry" : "author_blocking_\(operation)"))
        let confirmAction = { if let prompt = item.prompt { authors.confirm(expectedPrompt: prompt) } }
        let cancelAction = { if let prompt = item.prompt { authors.dismiss(expectedPrompt: prompt) } }
        let confirm: Alert.Button = item.blocking && !item.failed
            ? .destructive(label, action: confirmAction) : .default(label, action: confirmAction)
        return Alert(title: Text(title), message: Text(message), primaryButton: confirm,
            secondaryButton: .cancel(Text(strings.text("author_blocking_cancel")), action: cancelAction))
    }
}

private struct BlockedAuthorsAlert: Identifiable {
    let login: Bool
    let name: String
    let blocking: Bool
    let failed: Bool
    let prompt: IosAuthorBlockPrompt?
    var id: String { prompt?.id ?? "login" }

    init?(state: IosBlockedAuthorsState) {
        if state.needsLogin {
            login = true
            name = ""
            blocking = false
            failed = false
            prompt = nil
        } else if let prompt = state.prompt, !prompt.working {
            login = false
            name = prompt.name
            blocking = prompt.blocking
            failed = prompt.failed
            self.prompt = prompt
        } else {
            return nil
        }
    }
}

private struct BlockedAuthorsScreen: View {
    let state: IosBlockedAuthorsState
    let strings: BlockedAuthorsStrings
    let onBack: () -> Void
    let onLogin: () -> Void
    let onRegister: () -> Void
    let onRefresh: () -> Void
    let onUnblock: (String) -> Void

    var body: some View {
        List {
            Section {
                Text(strings.text("author_blocking_description"))
                    .foregroundStyle(.secondary)
            } header: {
                Text(strings.text("author_blocking_heading"))
                    .accessibilityAddTraits(.isHeader)
            }
            if state.signedIn { authorList } else { guest }
        }
        .listStyle(.insetGrouped)
        .refreshable { onRefresh() }
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .overlay { workingOverlay }
    }

    private var header: some View {
        CoffeePeekNativeNavigationHeader(title: strings.text("author_blocking_title"),
            backLabel: strings.text("author_blocking_back"), back: onBack)
    }

    @ViewBuilder private var authorList: some View {
        if state.loading {
            Section { CoffeePeekNativeLoader(label: strings.text("author_blocking_loading"), size: 32, showsLabel: true) }
        }
        if let error = state.error {
            Section {
                Text(strings.text("author_blocking_list_failed")).font(.headline)
                Text(error).foregroundStyle(.secondary)
                Button(strings.text("author_blocking_retry"), action: onRefresh)
            }
        }
        if state.ready && !state.loading && state.error == nil && state.items.isEmpty {
            Section {
                Label(strings.text("author_blocking_empty_title"), systemImage: "eye.slash")
                Text(strings.text("author_blocking_empty_message")).foregroundStyle(.secondary)
            }
        }
        Section {
            ForEach(state.items, id: \.id) { author in
                BlockedAuthorRow(author: author, strings: strings,
                    enabled: state.ready && state.prompt?.working != true,
                    onUnblock: { onUnblock(author.id) })
            }
        }
    }

    private var guest: some View {
        Section {
            Text(strings.text("author_blocking_guest_message"))
            Button(strings.text("author_blocking_login"), action: onLogin)
            Text(strings.text("author_blocking_no_account")).foregroundStyle(.secondary)
            Button(strings.text("author_blocking_register"), action: onRegister)
        }
    }

    @ViewBuilder private var workingOverlay: some View {
        if let prompt = state.prompt, prompt.working {
            ZStack {
                Color.black.opacity(0.15).ignoresSafeArea()
                CoffeePeekNativeLoader(label: strings.text(prompt.blocking ? "author_blocking_blocking" : "author_blocking_unblocking"), size: 32, showsLabel: true)
                    .padding(24)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
                    .accessibilityAddTraits(.updatesFrequently)
            }
        }
    }
}

private struct BlockedAuthorRow: View {
    let author: IosBlockedAuthor
    let strings: BlockedAuthorsStrings
    let enabled: Bool
    let onUnblock: () -> Void
    @Environment(\.sizeCategory) private var sizeCategory

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                avatar
                VStack(alignment: .leading, spacing: 4) {
                    Text(author.name).font(.headline)
                    if let slug = author.slug {
                        Text("@\(slug)").font(.subheadline).foregroundStyle(.secondary)
                    }
                }
                Spacer(minLength: 0)
                if !sizeCategory.isAccessibilityCategory { unblock }
            }
            if sizeCategory.isAccessibilityCategory { unblock }
        }
        .padding(.vertical, 4)
    }

    private var unblock: some View {
        Button(strings.text("author_blocking_unblock"), action: onUnblock)
            .buttonStyle(.borderless)
            .disabled(!enabled)
            .frame(minHeight: 44)
            .accessibilityLabel(strings.authorTitle("author_blocking_unblock_author", name: author.name))
    }

    private var avatar: some View {
        AsyncImage(url: author.avatarUrl.flatMap(URL.init(string:))) { phase in
            if let image = phase.image {
                image.resizable().scaledToFill()
            } else {
                Text(String(author.name.prefix(1))).font(.headline)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.secondary.opacity(0.12))
            }
        }
        .frame(width: 44, height: 44)
        .clipShape(Circle())
        .accessibilityHidden(true)
    }
}

struct BlockedAuthorsStrings {
    private let bundle: Bundle

    init(languageTag: String) {
        let localization = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = localization.flatMap { Bundle(url: $0) } ?? .main
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "BlockedAuthors")
    }

    func authorTitle(_ key: String, name: String) -> String {
        String(format: text(key), name)
    }
}

private func blockedAuthorsPreviewState(signedIn: Bool) -> IosBlockedAuthorsState {
    IosBlockedAuthorsState(items: signedIn ? [IosBlockedAuthor(id: "preview", name: "Анна", slug: "anna", avatarUrl: nil)] : [],
        signedIn: signedIn, ready: true, loading: false, error: nil, prompt: nil, needsLogin: false)
}

#Preview("Light") {
    BlockedAuthorsScreen(state: blockedAuthorsPreviewState(signedIn: true), strings: BlockedAuthorsStrings(languageTag: "ru"),
        onBack: {}, onLogin: {}, onRegister: {}, onRefresh: {}, onUnblock: { _ in })
        .preferredColorScheme(.light)
}

#Preview("Dark, guest") {
    BlockedAuthorsScreen(state: blockedAuthorsPreviewState(signedIn: false), strings: BlockedAuthorsStrings(languageTag: "be"),
        onBack: {}, onLogin: {}, onRegister: {}, onRefresh: {}, onUnblock: { _ in })
        .preferredColorScheme(.dark)
}

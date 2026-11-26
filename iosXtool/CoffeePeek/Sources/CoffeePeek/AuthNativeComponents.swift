import ComposeApp
import SwiftUI
import UIKit

#Preview("Auth glass navigation light") {
    NativeAuthPage(title: "Вход", subtitle: nil, strings: NativeAuthStrings(languageTag: "ru"), back: {}) {
        Color.clear.frame(height: 80)
    }.preferredColorScheme(.light)
}
#Preview("Auth glass navigation dark") {
    NativeAuthPage(title: "Уваход", subtitle: nil, strings: NativeAuthStrings(languageTag: "be"), back: {}) {
        Color.clear.frame(height: 80)
    }.preferredColorScheme(.dark)
}

struct NativeAuthStrings {
    private let bundle: Bundle
    private let locale: Locale

    init(languageTag: String) {
        let localization = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = localization.flatMap { Bundle(url: $0) } ?? .main
        locale = Locale(identifier: languageTag)
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "NativeAuth")
    }

    func message(_ message: IosAuthMessage) -> String {
        if message.minimumLength > 0 {
            return format(message.key, message.minimumLength)
        }
        let primary = text(message.key)
        guard let code = message.diagnosticCode else { return primary }
        let domain = message.diagnosticDomain.map { format("auth_social_diag_domain", $0) } ?? ""
        let oauth = message.diagnosticOAuth.map { format("auth_social_diag_oauth", $0) } ?? ""
        return format("auth_social_diag", primary, code, domain, oauth)
    }

    func registrationSuccess(email: String) -> String {
        format("auth_register_success_subtitle", email)
    }

    private func format(_ key: String, _ values: CVarArg...) -> String {
        String(format: text(key), locale: locale, arguments: values)
    }
}

struct NativeAuthPage<Content: View>: View {
    let title: String
    let subtitle: String?
    let strings: NativeAuthStrings
    var closeIcon = false
    let back: () -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if let subtitle {
                    Text(subtitle).foregroundStyle(CoffeePeekNativeStyle.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                content()
            }
            .frame(maxWidth: 480)
            .padding(.horizontal, 24)
            .padding(.top, 24)
            .padding(.bottom, 28)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .font(CoffeePeekNativeStyle.font(17))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .safeAreaInset(edge: .top, spacing: 0) { header }
    }

    private var header: some View {
        HStack(spacing: 8) {
            Button(action: back) {
                Label(
                    strings.text(closeIcon ? "auth_close" : "auth_back"),
                    systemImage: closeIcon ? "xmark" : "chevron.backward"
                )
                .labelStyle(.iconOnly)
                .font(.system(size: 22, weight: .regular))
                .frame(minWidth: 44, minHeight: 44)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain).shopDetailGlassCircle()
            Text(title).font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
                .frame(maxWidth: .infinity).multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true).accessibilityAddTraits(.isHeader)
            Color.clear.frame(width: 44, height: 1).accessibilityHidden(true)
        }
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .padding(.horizontal, 12).padding(.vertical, 8)
        .background(CoffeePeekNativeStyle.background)
    }
}

struct NativeAuthField<Content: View>: View {
    let title: String
    let error: String?
    var hint: String? = nil
    var labelActionTitle: String? = nil
    var labelAction: (() -> Void)? = nil
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            label
            content()
                .font(CoffeePeekNativeStyle.font(17))
                .padding(.horizontal, 16)
                .frame(minHeight: 48)
                .background(CoffeePeekNativeStyle.background, in: Capsule())
                .overlay {
                    Capsule()
                        .strokeBorder(error == nil ? CoffeePeekNativeStyle.border : .red, lineWidth: 1)
                        .allowsHitTesting(false)
                }
            if let error {
                NativeAuthError(text: error)
            } else if let hint {
                Text(hint).font(CoffeePeekNativeStyle.font(13, relativeTo: .footnote))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
        }
    }

    private var label: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(title).font(CoffeePeekNativeStyle.font(15))
                Spacer(minLength: 0)
                labelButton
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(CoffeePeekNativeStyle.font(15))
                labelButton
            }
        }
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
    }

    @ViewBuilder private var labelButton: some View {
        if let labelActionTitle, let labelAction {
            Button(action: labelAction) {
                Text(labelActionTitle).underline().font(CoffeePeekNativeStyle.font(15))
            }
            .buttonStyle(.plain).foregroundStyle(CoffeePeekNativeStyle.accent)
            .frame(minHeight: 44).contentShape(Rectangle())
            .padding(.vertical, -12)
        }
    }
}

struct NativeAuthError: View {
    let text: String

    var body: some View {
        Text(text)
            .font(CoffeePeekNativeStyle.font(13, relativeTo: .footnote))
            .foregroundStyle(.red)
            .onAppear { announce() }
            .onChange(of: text) { _, _ in announce() }
    }

    private func announce() {
        UIAccessibility.post(notification: .announcement, argument: text)
    }
}

struct NativeAuthSubmit: View {
    let title: String
    var busy = false
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if busy { CoffeePeekNativeLoader(color: .black, size: 24).accessibilityHidden(true) }
                Text(title)
                    .font(CoffeePeekNativeStyle.font(17, weight: "SemiBold"))
            }
            .foregroundStyle(Color.black.opacity(enabled && !busy ? 1 : 0.65))
            .frame(maxWidth: .infinity, minHeight: 48)
            .padding(.horizontal, 16)
            .background(CoffeePeekNativeStyle.accent.opacity(enabled && !busy ? 1 : 0.48), in: Capsule())
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .disabled(busy || !enabled)
    }
}

struct NativeAuthFooter: View {
    let prompt: String
    let actionTitle: String
    var accented = false
    var enabled = true
    let action: () -> Void

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 8) {
                promptText
                actionButton
            }
            .fixedSize(horizontal: true, vertical: false)
            VStack(spacing: 0) {
                promptText
                actionButton
            }
        }
        .frame(maxWidth: .infinity).padding(.top, 8)
    }

    private var promptText: some View {
        Text(prompt).font(CoffeePeekNativeStyle.font(15))
            .foregroundStyle(CoffeePeekNativeStyle.secondary)
            .fixedSize(horizontal: false, vertical: true)
    }

    private var actionButton: some View {
        Button(action: action) {
            Text(actionTitle).underline(accented)
                .font(CoffeePeekNativeStyle.font(17, weight: "SemiBold"))
                .fixedSize(horizontal: false, vertical: true)
                .frame(minHeight: 44)
        }
        .buttonStyle(.plain)
        .foregroundStyle(accented ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.foreground)
        .disabled(!enabled)
    }
}

struct NativeAuthEmailInput: View {
    let strings: NativeAuthStrings
    @Binding var email: String
    var contentType: UITextContentType = .emailAddress

    var body: some View {
        TextField(
            strings.text("auth_email_label"), text: $email,
            prompt: Text(strings.text("auth_email_placeholder")).foregroundStyle(CoffeePeekNativeStyle.secondary)
        )
        .textContentType(contentType)
        .keyboardType(.emailAddress)
        .textInputAutocapitalization(.never)
        .autocorrectionDisabled()
    }
}

private struct AuthComponentsPreview: View {
    let strings: NativeAuthStrings

    var body: some View {
        NativeAuthPage(title: strings.text("auth_login_title"), subtitle: nil, strings: strings, back: {}) {
            NativeAuthField(title: strings.text("auth_email_label"), error: strings.text("auth_email_invalid")) {
                NativeAuthEmailInput(strings: strings, email: .constant("example"))
            }
            NativeAuthSubmit(title: strings.text("auth_login"), action: {})
            NativeAuthFooter(prompt: strings.text("auth_login_no_account"),
                             actionTitle: strings.text("auth_login_register"), action: {})
        }
    }
}

#Preview("Components Light") {
    AuthComponentsPreview(strings: NativeAuthStrings(languageTag: "ru")).preferredColorScheme(.light)
}

#Preview("Components Dark") {
    AuthComponentsPreview(strings: NativeAuthStrings(languageTag: "be")).preferredColorScheme(.dark)
}

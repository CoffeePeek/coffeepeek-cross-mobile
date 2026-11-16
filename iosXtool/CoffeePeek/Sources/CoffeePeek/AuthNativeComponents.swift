import ComposeApp
import SwiftUI
import UIKit

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
                Image("LaunchLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 58, height: 58)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 8) {
                    Text(title).font(.largeTitle.bold()).accessibilityAddTraits(.isHeader)
                    if let subtitle {
                        Text(subtitle).foregroundStyle(.secondary)
                    }
                }
                content()
            }
            .frame(maxWidth: 480)
            .padding(.horizontal, 24)
            .padding(.top, 8)
            .padding(.bottom, 28)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color(uiColor: .systemGroupedBackground))
        .safeAreaInset(edge: .top, spacing: 0) {
            HStack {
                Button(action: back) {
                    Label(
                        strings.text(closeIcon ? "auth_close" : "auth_back"),
                        systemImage: closeIcon ? "xmark" : "chevron.backward"
                    )
                        .labelStyle(.iconOnly)
                        .frame(minWidth: 44, minHeight: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                Spacer()
            }
            .padding(.horizontal, 12)
            .background(Color(uiColor: .systemGroupedBackground))
        }
    }
}

struct NativeAuthField<Content: View>: View {
    let title: String
    let error: String?
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.subheadline.weight(.semibold))
            content()
                .padding(.horizontal, 14)
                .frame(minHeight: 50)
                .background(.background, in: RoundedRectangle(cornerRadius: 12))
                .overlay {
                    RoundedRectangle(cornerRadius: 12)
                        .strokeBorder(error == nil ? Color.secondary.opacity(0.24) : .red, lineWidth: 1)
                        .allowsHitTesting(false)
                }
            if let error {
                NativeAuthError(text: error)
            }
        }
    }
}

struct NativeAuthError: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.footnote)
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
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if busy { ProgressView().tint(.white).accessibilityHidden(true) }
                Text(title)
                    .font(.body.weight(.semibold))
            }
            .frame(maxWidth: .infinity, minHeight: 44)
        }
        .buttonStyle(.borderedProminent)
        .disabled(busy)
    }
}

struct NativeAuthEmailInput: View {
    let strings: NativeAuthStrings
    @Binding var email: String
    var contentType: UITextContentType = .emailAddress

    var body: some View {
        TextField(
            strings.text("auth_email_label"), text: $email,
            prompt: Text(strings.text("auth_email_placeholder"))
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
        }
    }
}

#Preview("Components Light") {
    AuthComponentsPreview(strings: NativeAuthStrings(languageTag: "ru")).preferredColorScheme(.light)
}

#Preview("Components Dark") {
    AuthComponentsPreview(strings: NativeAuthStrings(languageTag: "be")).preferredColorScheme(.dark)
}

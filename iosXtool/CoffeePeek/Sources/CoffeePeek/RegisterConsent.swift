import SwiftUI

struct RegisterConsent: View {
    let strings: NativeAuthStrings
    @Binding var termsAccepted: Bool
    @Binding var analyticsAccepted: Bool
    let termsError: String?
    let openTerms: () -> Void
    let openPrivacy: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 24) {
            HStack(alignment: .top, spacing: 8) {
                consentToggle(termsLabel, selected: $termsAccepted)
                Text(legalText)
                    .font(CoffeePeekNativeStyle.font(13))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 8)
                    .environment(\.openURL, OpenURLAction(handler: openLegalURL))
            }
            if let termsError { NativeAuthError(text: termsError) }
            HStack(alignment: .top, spacing: 8) {
                consentToggle(strings.text("auth_analytics_title"), selected: $analyticsAccepted)
                VStack(alignment: .leading, spacing: 6) {
                    Text(strings.text("auth_analytics_title"))
                        .font(CoffeePeekNativeStyle.font(15))
                    Text(strings.text("auth_analytics_description"))
                        .font(CoffeePeekNativeStyle.font(13))
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                }
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 8)
            }
        }
    }

    private func consentToggle(_ label: String, selected: Binding<Bool>) -> some View {
        Toggle(isOn: selected) {
            Image(systemName: selected.wrappedValue ? "checkmark.square.fill" : "square")
                .symbolRenderingMode(.palette)
                .foregroundStyle(selected.wrappedValue ? Color.black : CoffeePeekNativeStyle.secondary,
                                 CoffeePeekNativeStyle.accent)
                .font(.system(size: 21))
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .toggleStyle(.button)
        .buttonStyle(.plain)
        .tint(CoffeePeekNativeStyle.accent)
        .accessibilityLabel(label)
    }

    private var termsLabel: String {
        ["auth_terms_prefix", "auth_terms_link", "auth_terms_and", "auth_privacy_link"]
            .map(strings.text).joined(separator: " ")
    }

    private var legalText: AttributedString {
        var terms = AttributedString(strings.text("auth_terms_link"))
        terms.link = URL(string: "https://coffeepeek.by/terms")
        terms.foregroundColor = CoffeePeekNativeStyle.accent
        var privacy = AttributedString(strings.text("auth_privacy_link"))
        privacy.link = URL(string: "https://coffeepeek.by/privacy")
        privacy.foregroundColor = CoffeePeekNativeStyle.accent
        return AttributedString(strings.text("auth_terms_prefix") + " ") + terms
            + AttributedString(" " + strings.text("auth_terms_and") + " ") + privacy
    }

    private func openLegalURL(_ url: URL) -> OpenURLAction.Result {
        switch url.absoluteString {
        case "https://coffeepeek.by/terms": openTerms()
        case "https://coffeepeek.by/privacy": openPrivacy()
        default: return .systemAction
        }
        return .handled
    }
}

#Preview("Consent Light") {
    RegisterConsent(strings: NativeAuthStrings(languageTag: "ru"), termsAccepted: .constant(false),
                    analyticsAccepted: .constant(true), termsError: nil, openTerms: {}, openPrivacy: {})
        .padding(24).background(CoffeePeekNativeStyle.background).preferredColorScheme(.light)
}

#Preview("Consent Dark") {
    RegisterConsent(strings: NativeAuthStrings(languageTag: "be"), termsAccepted: .constant(true),
                    analyticsAccepted: .constant(false), termsError: nil, openTerms: {}, openPrivacy: {})
        .padding(24).background(CoffeePeekNativeStyle.background).preferredColorScheme(.dark)
}

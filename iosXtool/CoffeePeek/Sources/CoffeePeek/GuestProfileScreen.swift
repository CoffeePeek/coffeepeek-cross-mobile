import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeGuestProfileController(profile: IosGuestProfile) -> UIViewController {
    GuestProfileHostingController(profile: profile)
}

private final class GuestProfileHostingController: UIHostingController<GuestProfileHost> {
    private let profile: IosGuestProfile

    init(profile: IosGuestProfile) {
        self.profile = profile
        super.init(rootView: GuestProfileHost(profile: profile))
    }

    @MainActor required dynamic init?(coder: NSCoder) { return nil }

    isolated deinit { profile.close() }
}

private struct GuestProfileHost: View {
    let profile: IosGuestProfile
    @State private var languageTag: String
    @State private var observation: IosGuestProfileObservation?

    init(profile: IosGuestProfile) {
        self.profile = profile
        _languageTag = State(initialValue: profile.languageTag())
    }

    var body: some View {
        GuestProfileScreen(strings: NativeProfileStrings(languageTag: languageTag), login: profile.login, register: profile.register)
            .onAppear(perform: observeLanguage)
            .onDisappear {
                observation?.cancel()
                observation = nil
            }
    }

    private func observeLanguage() {
        guard observation == nil else { return }
        languageTag = profile.languageTag()
        observation = profile.observeLanguage { [language = $languageTag] tag in
            language.wrappedValue = tag
        }
    }
}

private struct NativeProfileStrings {
    private let bundle: Bundle

    init(languageTag: String) {
        let localization = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = localization.flatMap { Bundle(url: $0) } ?? .main
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "NativeProfile")
    }
}

private struct GuestProfileScreen: View {
    let strings: NativeProfileStrings
    let login: () -> Void
    let register: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                CoffeePeekNativeRootHeader(title: strings.text("profile_title")) { EmptyView() }
                CoffeePeekGuestAccountCard(
                    prompt: strings.text("guest_prompt"), loginTitle: strings.text("guest_login"),
                    noAccountPrompt: strings.text("guest_no_account"), registerTitle: strings.text("guest_register"),
                    login: login, register: register
                )
            }
            .padding(.horizontal, 16).padding(.bottom, 16).padding(.bottom, 100)
        }
        .font(CoffeePeekNativeStyle.font(17))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
    }
}

#Preview("Guest profile light") {
    GuestProfileScreen(strings: NativeProfileStrings(languageTag: "ru"), login: {}, register: {})
        .preferredColorScheme(.light)
}

#Preview("Guest profile dark") {
    GuestProfileScreen(strings: NativeProfileStrings(languageTag: "be"), login: {}, register: {})
        .preferredColorScheme(.dark)
}

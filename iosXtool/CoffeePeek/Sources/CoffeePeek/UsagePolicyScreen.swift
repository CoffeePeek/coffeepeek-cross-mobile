import ComposeApp
import SwiftUI

struct UsagePolicyScreen: View {
    let state: IosUsagePolicyState
    let strings: SettingsNativeStrings
    let setConsent: (Bool) -> Void
    let openTerms: () -> Void
    let openPrivacy: () -> Void
    let back: () -> Void

    var body: some View {
        SettingsNativePage(title: strings.text("settings_policy_title"), strings: strings, back: back) {
            if state.available {
                Section(strings.text("settings_policy_section")) {
                    Toggle(isOn: Binding(get: { state.accepted }, set: setConsent)) {
                        SettingsNativeLabel(
                            title: strings.text("settings_policy_analytics_title"),
                            description: strings.text(state.syncFailed ? "settings_analytics_sync_failed" : "settings_policy_analytics_text")
                        )
                    }
                }
            }
            Section {
                SettingsNativeRow(
                    title: strings.text("settings_policy_terms_title"),
                    description: strings.text("settings_policy_terms_description"),
                    systemImage: "doc.text", navigates: true, action: openTerms
                )
                SettingsNativeRow(
                    title: strings.text("settings_policy_privacy_title"),
                    description: strings.text("settings_policy_privacy_description"),
                    systemImage: "shield", navigates: true, action: openPrivacy
                )
            } header: {
                if !state.available { Text(strings.text("settings_policy_section")) }
            }
        }
    }
}

#Preview("Usage policy light") {
    UsagePolicyScreen(
        state: IosUsagePolicyState(available: false, accepted: false, syncFailed: false),
        strings: SettingsNativeStrings(languageTag: "ru"), setConsent: { _ in }, openTerms: {}, openPrivacy: {}, back: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Usage policy dark") {
    UsagePolicyScreen(
        state: IosUsagePolicyState(available: true, accepted: false, syncFailed: true),
        strings: SettingsNativeStrings(languageTag: "be"), setConsent: { _ in }, openTerms: {}, openPrivacy: {}, back: {}
    )
    .preferredColorScheme(.dark)
}

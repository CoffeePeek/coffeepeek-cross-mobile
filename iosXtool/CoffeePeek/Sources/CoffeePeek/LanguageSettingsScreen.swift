import ComposeApp
import SwiftUI

struct LanguageSettingsScreen: View {
    let state: IosLanguageSettingsState
    let strings: SettingsNativeStrings
    let select: (String) -> Void
    let retry: () -> Void
    let back: () -> Void

    var body: some View {
        SettingsNativePage(title: strings.text("settings_language_title"), strings: strings, back: back) {
            SettingsNativeStyle.heading(strings.text("settings_language_heading"),
                                        description: strings.text("settings_language_description"))
            SettingsNativeStyle.section {
                languageOption("ru", label: "settings_language_russian")
                SettingsNativeStyle.divider
                languageOption("be", label: "settings_language_belarusian")
            }
            if state.saving {
                CoffeePeekNativeLoader(label: strings.text("settings_language_saving"), size: 32, showsLabel: true)
            } else if state.saveFailed {
                SettingsNativeStyle.section {
                    SettingsRetryRow(
                        message: strings.text("settings_language_save_error"),
                        retry: strings.text("settings_retry"),
                        action: retry
                    )
                }
            }
        }
    }

    private func languageOption(_ tag: String, label: String) -> some View {
        SettingsSelectionRow(title: strings.text(label), selected: state.selectedTag == tag) { select(tag) }
            .disabled(state.saving)
    }
}

#Preview("Language light") {
    LanguageSettingsScreen(
        state: IosLanguageSettingsState(selectedTag: "ru", saving: false, saveFailed: false),
        strings: SettingsNativeStrings(languageTag: "ru"), select: { _ in }, retry: {}, back: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Language dark") {
    LanguageSettingsScreen(
        state: IosLanguageSettingsState(selectedTag: "be", saving: false, saveFailed: true),
        strings: SettingsNativeStrings(languageTag: "be"), select: { _ in }, retry: {}, back: {}
    )
    .preferredColorScheme(.dark)
}

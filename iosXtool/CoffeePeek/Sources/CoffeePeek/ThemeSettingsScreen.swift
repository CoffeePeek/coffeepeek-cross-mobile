import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeThemeSettingsController(
    settings: IosThemeSettings,
    languageTag: String,
    onBack: @escaping () -> KotlinUnit
) -> UIViewController {
    UIHostingController(rootView: ThemeSettingsHost(
        settings: settings,
        strings: ThemeSettingsStrings(languageTag: languageTag),
        onBack: { _ = onBack() }
    ))
}

private struct ThemeSettingsHost: View {
    let settings: IosThemeSettings
    let strings: ThemeSettingsStrings
    let onBack: () -> Void
    @State private var selected: ThemeMode
    @State private var observation: IosThemeObservation?

    init(settings: IosThemeSettings, strings: ThemeSettingsStrings, onBack: @escaping () -> Void) {
        self.settings = settings
        self.strings = strings
        self.onBack = onBack
        _selected = State(initialValue: settings.currentMode())
    }

    var body: some View {
        ThemeSettingsScreen(selected: selected, strings: strings, onSelect: { settings.selectMode(mode: $0) }, onBack: onBack)
            .onAppear(perform: observeMode)
            .onDisappear {
                observation?.cancel()
                observation = nil
            }
    }

    private func observeMode() {
        guard observation == nil else { return }
        selected = settings.currentMode()
        observation = settings.observeMode { [selection = $selected] mode in
            selection.wrappedValue = mode
        }
    }
}

private struct ThemeSettingsScreen: View {
    let selected: ThemeMode
    let strings: ThemeSettingsStrings
    let onSelect: (ThemeMode) -> Void
    let onBack: () -> Void

    var body: some View {
        SettingsNativePage(title: strings.text("theme.title"), backLabel: strings.text("theme.back"), back: onBack) {
            SettingsNativeStyle.heading(strings.text("theme.heading"), description: strings.text("theme.description"))
            SettingsNativeStyle.section {
                option(.system, title: strings.text("theme.system"), icon: "desktopcomputer")
                SettingsNativeStyle.divider
                option(.light, title: strings.text("theme.light"), icon: "sun.max")
                SettingsNativeStyle.divider
                option(.dark, title: strings.text("theme.dark"), icon: "moon")
            }
        }
    }

    private func option(_ mode: ThemeMode, title: String, icon: String) -> some View {
        SettingsSelectionRow(title: title, selected: mode == selected, systemImage: icon) { onSelect(mode) }
    }
}

private struct ThemeSettingsStrings {
    private let bundle: Bundle

    init(languageTag: String) {
        let localization = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = localization.flatMap { Bundle(url: $0) } ?? .main
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "ThemeSettings")
    }
}

#Preview("Light") {
    ThemeSettingsScreen(selected: .system, strings: ThemeSettingsStrings(languageTag: "ru"), onSelect: { _ in }, onBack: {})
        .preferredColorScheme(.light)
}

#Preview("Dark") {
    ThemeSettingsScreen(selected: .dark, strings: ThemeSettingsStrings(languageTag: "be"), onSelect: { _ in }, onBack: {})
        .preferredColorScheme(.dark)
}

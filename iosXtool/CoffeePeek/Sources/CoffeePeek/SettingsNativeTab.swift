import ComposeApp
import SwiftUI

struct SettingsNativeTab: View {
    let state: IosSettingsTabState
    let strings: SettingsNativeStrings
    let open: (IosSettingsAction) -> Void
    let checkForUpdates: () -> Void
    let setPushEnabled: (Bool) -> Void
    let refreshNotifications: () -> Void
    let switchAccount: () -> Void

    var body: some View {
        List {
            Text(strings.text("settings_tab_title"))
                .font(.largeTitle.bold())
                .accessibilityAddTraits(.isHeader)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            if state.showGuestCard { guestSection }
            if state.hasBusinesses { businessSection }
            if state.showAddRows { addSection }
            preferencesSection
            otherSection
            if state.updateChecksEnabled { updateSection }
            Text(strings.version(state.version))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .listRowBackground(Color.clear)
        }
        .listStyle(.insetGrouped)
    }

    private var guestSection: some View {
        Section {
            Text(strings.text("settings_guest_prompt"))
                .foregroundStyle(.secondary)
            Button(strings.text("settings_guest_login")) { open(.signIn) }
            Button(strings.text("settings_guest_register")) { open(.register) }
        }
    }

    private var businessSection: some View {
        Section(strings.text("ownership_account")) {
            SettingsNativeRow(
                title: strings.text(state.businessMode ? "ownership_switch_personal" : "ownership_switch_business"),
                systemImage: "person.crop.circle", action: switchAccount
            )
        }
    }

    private var addSection: some View {
        Section(strings.text("settings_section_add")) {
            SettingsNativeRow(
                title: strings.text("settings_add_shop"), description: strings.text("settings_add_shop_description"),
                systemImage: "plus.circle", navigates: true
            ) { open(.addShop) }
            SettingsNativeRow(
                title: strings.text("settings_add_roaster"), description: strings.text("settings_add_roaster_description"),
                systemImage: "cup.and.saucer", navigates: true
            ) { open(.addRoaster) }
        }
    }

    private var preferencesSection: some View {
        Section(strings.text("settings_section_settings")) {
            SettingsNativeRow(
                title: strings.text("settings_city_row"), description: strings.text("settings_city_row_description"),
                systemImage: "location", value: state.city ?? strings.text("settings_city_not_selected"), navigates: true
            ) { open(.city) }
            SettingsNativeRow(
                title: strings.text("settings_theme_row"), description: strings.text("settings_theme_row_description"),
                systemImage: themeImage, value: strings.text(themeValue), navigates: true
            ) { open(.theme) }
            SettingsNativeRow(
                title: strings.text("settings_language_title"), description: strings.text("settings_language_row_description"),
                systemImage: "globe", value: strings.text(languageValue), navigates: true
            ) { open(.language) }
            notificationRows
        }
    }

    @ViewBuilder
    private var notificationRows: some View {
        let preference = state.notifications
        let title = strings.text("settings_notifications_title")
        let description = strings.text(preference.descriptionKey)
        if preference.guest {
            SettingsNativeRow(title: title, description: description, systemImage: "bell", navigates: true) { open(.signIn) }
        } else {
            Toggle(isOn: Binding(get: { preference.checked }, set: setPushEnabled)) {
                SettingsNativeLabel(title: title, description: description)
            }
            .disabled(!preference.enabled)
            if let failure = preference.failureKey {
                if preference.canRetry {
                    SettingsRetryRow(
                        message: strings.text(failure), retry: strings.text("notifications_retry_settings"), action: refreshNotifications
                    )
                } else {
                    Text(strings.text(failure)).font(.footnote).foregroundStyle(.secondary)
                }
            }
        }
    }

    private var otherSection: some View {
        Section {
            SettingsNativeRow(
                title: strings.text("settings_support"), description: strings.text("settings_support_description"),
                systemImage: "lifepreserver", navigates: true
            ) { open(.support) }
            SettingsNativeRow(
                title: strings.text("settings_policy_row"), systemImage: "lock.shield", navigates: true
            ) { open(.usage) }
            SettingsNativeRow(title: strings.text("settings_share"), systemImage: "square.and.arrow.up") { open(.share) }
            if state.canRateApp {
                SettingsNativeRow(title: strings.text("settings_rate_app"), systemImage: "star") { open(.rate) }
            }
        } header: {
            Text(strings.text("settings_section_other"))
        } footer: {
            Text(strings.text("settings_section_other_description"))
        }
    }

    private var updateSection: some View {
        Section(strings.text("settings_section_update")) {
            SettingsNativeRow(
                title: strings.text(state.checkingUpdates ? "settings_update_checking" : "settings_update_check"),
                description: state.updateAvailable ? strings.text("settings_update_available") : strings.version(state.version),
                systemImage: "arrow.down.circle", busy: state.checkingUpdates, action: checkForUpdates
            )
        }
    }

    private var languageValue: String {
        state.languageTag == "be" ? "settings_language_belarusian" : "settings_language_russian"
    }

    private var themeValue: String {
        switch state.theme {
        case .dark: "settings_theme_value_dark"
        case .light: "settings_theme_value_light"
        default: "settings_theme_value_system"
        }
    }

    private var themeImage: String {
        switch state.theme {
        case .dark: "moon"
        case .light: "sun.max"
        default: "circle.lefthalf.filled"
        }
    }
}

private func settingsPreview(guest: Bool, languageTag: String) -> some View {
    SettingsNativeTab(
        state: IosSettingsTabState(
            showGuestCard: guest, showAddRows: true, city: guest ? nil : "Мінск", theme: guest ? .system : .dark,
            languageTag: languageTag, checkingUpdates: false, updateAvailable: false, updateChecksEnabled: true,
            version: "1.0", canRateApp: true, hasBusinesses: !guest, businessMode: false,
            notifications: IosNotificationSettingsState(
                guest: guest, checked: !guest, enabled: !guest, descriptionKey: "settings_notifications_description",
                failureKey: nil, canRetry: false
            )
        ),
        strings: SettingsNativeStrings(languageTag: languageTag), open: { _ in }, checkForUpdates: {},
        setPushEnabled: { _ in }, refreshNotifications: {}, switchAccount: {}
    )
}

#Preview("Settings light") {
    settingsPreview(guest: true, languageTag: "ru").preferredColorScheme(.light)
}

#Preview("Settings dark") {
    settingsPreview(guest: false, languageTag: "be").preferredColorScheme(.dark)
}

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
    var headerTopInset: CGFloat = 0

    var body: some View {
        ScrollView {
            content
                .padding(.horizontal, 16)
                .padding(.bottom, 100)
        }
        .ignoresSafeArea(.container, edges: .top)
        .settingsAppearance()
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 16) {
            CoffeePeekNativeRootHeader(title: strings.text("settings_tab_title")) { EmptyView() }
                .padding(.top, headerTopInset)
            if state.showGuestCard { guestSection }
            if state.hasBusinesses { businessSection }
            if state.showAddRows { addSection }
            preferencesSection
            otherSection
            if state.updateChecksEnabled { updateSection }
            Text(strings.version(state.version))
                .font(CoffeePeekNativeStyle.font(13, relativeTo: .footnote))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }

    private var guestSection: some View {
        CoffeePeekGuestAccountCard(
            prompt: strings.text("settings_guest_prompt"), loginTitle: strings.text("settings_guest_login"),
            noAccountPrompt: strings.text("settings_guest_no_account"), registerTitle: strings.text("settings_guest_register"),
            login: { open(.login) }, register: { open(.registration) }
        )
    }

    private var businessSection: some View {
        SettingsNativeStyle.section(strings.text("ownership_account")) {
            SettingsNativeRow(
                title: strings.text(state.businessMode ? "ownership_switch_personal" : "ownership_switch_business"),
                systemImage: "business", navigates: true, action: switchAccount
            )
        }
    }

    private var addSection: some View {
        SettingsNativeStyle.section(strings.text("settings_section_add")) {
            SettingsNativeRow(
                title: strings.text("settings_add_shop"), description: strings.text("settings_add_shop_description"),
                systemImage: "plus", navigates: true
            ) { open(.shop) }
            SettingsNativeStyle.divider
            SettingsNativeRow(
                title: strings.text("settings_add_roaster"), description: strings.text("settings_add_roaster_description"),
                systemImage: "bean", navigates: true
            ) { open(.roaster) }
        }
    }

    private var preferencesSection: some View {
        SettingsNativeStyle.section(strings.text("settings_section_settings")) {
            SettingsNativeRow(
                title: strings.text("settings_city_row"), description: strings.text("settings_city_row_description"),
                systemImage: "location", value: state.city ?? strings.text("settings_city_not_selected"), navigates: true
            ) { open(.city) }
            SettingsNativeStyle.divider
            SettingsNativeRow(
                title: strings.text("settings_theme_row"), description: strings.text("settings_theme_row_description"),
                systemImage: "theme", value: strings.text(themeValue), navigates: true
            ) { open(.theme) }
            SettingsNativeStyle.divider
            SettingsNativeRow(
                title: strings.text("settings_language_title"), description: strings.text("settings_language_row_description"),
                systemImage: "globe", value: strings.text(languageValue), navigates: true
            ) { open(.language) }
            SettingsNativeStyle.divider
            notificationRows
        }
    }

    @ViewBuilder
    private var notificationRows: some View {
        let preference = state.notifications
        let title = strings.text("settings_notifications_title")
        let description = strings.text(preference.descriptionKey)
        if preference.guest {
            SettingsNativeRow(title: title, description: description, systemImage: "bell", navigates: true) { open(.login) }
        } else {
            SettingsNativeLabel.toggle(title, description: description, icon: "bell",
                                       isOn: Binding(get: { preference.checked }, set: { setPushEnabled($0) }))
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
        SettingsNativeStyle.section(strings.text("settings_section_other"),
                                    description: strings.text("settings_section_other_description")) {
            SettingsNativeRow(
                title: strings.text("settings_support"), description: strings.text("settings_support_description"),
                systemImage: "text.bubble", navigates: true
            ) { open(.support) }
            SettingsNativeStyle.divider
            SettingsNativeRow(
                title: strings.text("settings_policy_row"), systemImage: "lock.shield", navigates: true
            ) { open(.usage) }
            SettingsNativeStyle.divider
            SettingsNativeRow(title: strings.text("settings_share"), systemImage: "square.and.arrow.up") { open(.share) }
            if state.canRateApp {
                SettingsNativeStyle.divider
                SettingsNativeRow(title: strings.text("settings_rate_app"), systemImage: "star") { open(.rate) }
            }
        }
    }

    private var updateSection: some View {
        SettingsNativeStyle.section(strings.text("settings_section_update")) {
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

}

@MainActor
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
    settingsPreview(guest: false, languageTag: "ru").preferredColorScheme(.light)
}

#Preview("Settings dark") {
    settingsPreview(guest: false, languageTag: "be").preferredColorScheme(.dark)
}

#Preview("Settings guest light") {
    settingsPreview(guest: true, languageTag: "ru").preferredColorScheme(.light)
}

#Preview("Settings guest dark") {
    settingsPreview(guest: true, languageTag: "be").preferredColorScheme(.dark)
}

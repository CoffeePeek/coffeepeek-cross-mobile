import ComposeApp
import SwiftUI

struct CitySettingsScreen: View {
    let state: IosCitySettingsState
    let strings: SettingsNativeStrings
    let select: (String) -> Void
    let retry: () -> Void
    let back: () -> Void

    var body: some View {
        SettingsNativePage(title: strings.text("settings_city_title"), strings: strings, back: back) {
            Section {
                if state.loading {
                    ProgressView(strings.text("settings_loading"))
                } else if state.failed || state.cities.isEmpty {
                    SettingsRetryRow(
                        message: strings.text(state.failed ? "settings_city_error" : "settings_city_empty"),
                        retry: strings.text("settings_retry"),
                        action: retry
                    )
                } else {
                    ForEach(state.cities, id: \.id) { city in
                        SettingsSelectionRow(
                            title: city.name, selected: city.id == state.selectedId, systemImage: "location"
                        ) { select(city.id) }
                    }
                }
            } header: {
                Text(strings.text("settings_city_heading"))
            } footer: {
                Text(strings.text("settings_city_description"))
            }
        }
    }
}

#Preview("Cities light") {
    CitySettingsScreen(
        state: IosCitySettingsState(
            cities: [IosSettingsCity(id: "minsk", name: "Минск"), IosSettingsCity(id: "brest", name: "Брест")],
            selectedId: "minsk", loading: false, failed: false
        ),
        strings: SettingsNativeStrings(languageTag: "ru"), select: { _ in }, retry: {}, back: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Cities dark") {
    CitySettingsScreen(
        state: IosCitySettingsState(cities: [], selectedId: nil, loading: false, failed: true),
        strings: SettingsNativeStrings(languageTag: "be"), select: { _ in }, retry: {}, back: {}
    )
    .preferredColorScheme(.dark)
}

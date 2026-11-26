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
            SettingsNativeStyle.heading(strings.text("settings_city_heading"),
                                        description: strings.text("settings_city_description"))
            SettingsNativeStyle.section {
                if state.loading {
                    CoffeePeekNativeLoader(label: strings.text("settings_loading"), size: 32, showsLabel: true)
                        .padding(16)
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
                        if city.id != state.cities.last?.id { SettingsNativeStyle.divider }
                    }
                }
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

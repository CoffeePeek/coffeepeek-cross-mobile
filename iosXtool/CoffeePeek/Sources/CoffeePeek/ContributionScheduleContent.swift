import SwiftUI

extension ContributionsStrings {
    func sectionTitle(_ section: String) -> String { text("section_\(sectionSuffix(section))") }
    func sectionHint(_ section: String) -> String { text("hint_\(sectionSuffix(section))") }
    private func sectionSuffix(_ value: String) -> String { value == "BrewMethods" ? "brew_methods" : value.lowercased() }

    func fieldTitle(_ key: String, roaster: Bool) -> String {
        if roaster && key == "name" { return text("roaster_name_label") }
        if roaster && key == "description" { return text("roaster_about_label") }
        return text("shop_\(key)_label")
    }

    func groupTitle(_ key: String) -> String {
        key == "tags" ? text("section_tags") : text("shop_\(key == "brewMethods" ? "brew_methods" : key)")
    }

    func issue(_ issue: String, roaster: Bool) -> String {
        switch issue {
        case "NameBlank": text("shop_error_name_blank")
        case "NameTooLong": format(roaster ? "roaster_error_name_too_long" : "shop_error_name_too_long", roaster ? 100 : 55)
        case "AddressBlank", "AddressMissing": text("shop_error_address_blank")
        case "CityMissing": text("shop_error_city_missing")
        case "DescriptionTooLong": format("shop_error_name_too_long", 1000)
        default: text("error_\(issue.lowercased())")
        }
    }

    func locationHint(_ value: String) -> String {
        text(value == "PermissionDenied" ? "shop_location_denied" : value == "NoAddress" ? "shop_location_no_address" : "shop_location_not_found")
    }

    func media(_ suffix: String) -> String {
        let language = languageTag.split(separator: "-").first.map(String.init) ?? "ru"
        let bundle = Bundle.main.path(forResource: language, ofType: "lproj").flatMap(Bundle.init(path:)) ?? .main
        return bundle.localizedString(forKey: "coffeepeek_photo_source_\(suffix)", value: nil, table: "NativeContributions")
    }
}

struct ContributionScheduleContent: View {
    let state: ContributionFormSnapshot
    let strings: ContributionsStrings
    let onAction: (String, Int, Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text(strings.text("shop_schedule_intro")).foregroundStyle(CoffeePeekNativeStyle.secondary)
            Toggle(strings.text("shop_per_day"), isOn: Binding(get: { state.perDay }, set: { onAction("perDay", 0, $0 ? 1 : 0) }))
                .tint(CoffeePeekNativeStyle.accent)
            if state.perDay {
                ForEach(state.days) { day in
                    VStack(spacing: 12) {
                        Toggle(strings.text("shop_day_\(day.day)"), isOn: Binding(get: { !day.closed }, set: { onAction("dayOpen", day.day, $0 ? 1 : 0) }))
                            .tint(CoffeePeekNativeStyle.accent)
                        if !day.closed {
                            timeRow("shop_opening", value: day.open) { onAction("dayOpening", day.day, $0) }
                            timeRow("shop_closing", value: day.close) { onAction("dayClosing", day.day, $0) }
                        }
                    }.padding(16).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
                }
            } else {
                timeRow("shop_opening", value: state.open) { onAction("open", 0, $0) }
                timeRow("shop_closing", value: state.close) { onAction("close", 0, $0) }
                presets
                Text(strings.format("shop_work_days_count", 7 - state.closedDays.count)).font(.headline)
                ForEach([1, 2, 3, 4, 5, 6, 0], id: \.self) { day in
                    Toggle(strings.text("shop_day_\(day)"), isOn: Binding(get: { !state.closedDays.contains(day) }, set: { _ in onAction("closed", day, 0) }))
                        .tint(CoffeePeekNativeStyle.accent)
                }
            }
        }
    }

    private var presets: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings.text("shop_presets")).font(.headline)
            ForEach([8, 9, 10], id: \.self) { hour in
                let open = String(format: "%02d:00", hour)
                let close = String(format: "%02d:00", hour + 12)
                Button { onAction("preset", hour + 12, hour) } label: {
                    HStack {
                        Text(strings.format("shop_preset_label", open, close)); Spacer()
                        if state.open == open && state.close == close { Image(systemName: "checkmark") }
                    }.frame(minHeight: 44)
                }
            }
        }
    }

    private func timeRow(_ title: String, value: String, change: @escaping (Int) -> Void) -> some View {
        HStack {
            Text(strings.text(title)).frame(maxWidth: .infinity, alignment: .leading)
            Button { change(-15) } label: { Image(systemName: "minus").frame(width: 44, height: 44) }
                .accessibilityLabel(strings.text("shop_earlier"))
            Text(value).monospacedDigit().frame(minWidth: 54)
            Button { change(15) } label: { Image(systemName: "plus").frame(width: 44, height: 44) }
                .accessibilityLabel(strings.text("shop_later"))
        }
    }
}

private func scheduleSample() -> some View {
    ContributionScheduleContent(state: ContributionFormSnapshot(kind: "AddShop", signedIn: 1, loading: false,
        open: "09:00", close: "21:00", days: [], closedDays: [0]),
        strings: ContributionsStrings(languageTag: "ru"), onAction: { _, _, _ in })
        .padding(16).background(CoffeePeekNativeStyle.background)
}
#Preview("Contribution schedule light") { scheduleSample().preferredColorScheme(.light) }
#Preview("Contribution schedule dark") { scheduleSample().preferredColorScheme(.dark) }


import ComposeApp
import SwiftUI

struct ShelfNativeBagForm: View {
    let add: IosShelfAddState
    let strings: ShelfNativeStrings
    let card: UIImage?
    let send: (ShelfNativeAddIntent) -> Void
    @State private var dateShown = false
    @State private var roasted = Date()

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 16) {
                if let image = card {
                    Image(uiImage: image).resizable().scaledToFit().frame(width: 110, height: 150).accessibilityHidden(true)
                }
                VStack(alignment: .leading, spacing: 12) {
                    Text(strings["shelf_form_ready"]).font(.headline)
                    Button(strings["shelf_retake_camera"]) { send(.command("camera")) }
                }
            }
            if !add.hints.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text(strings["shelf_form_hint"]).font(.headline)
                    ForEach(add.hints, id: \.self) { Text(strings.reason($0)) }
                }.padding(12).background(CoffeePeekNativeStyle.accent.opacity(0.12), in: RoundedRectangle(cornerRadius: 12))
            }
            coffeeField
            TextField(strings["shelf_form_roaster"], text: Binding(get: { add.roaster }, set: { send(.roaster($0)) }))
                .textFieldStyle(.roundedBorder).accessibilityLabel(strings["shelf_form_roaster"])
            weights
            traits
            roastDate
            if add.saving { ProgressView() }
            Button(strings["shelf_form_save"]) { send(.save(false)) }
                .buttonStyle(.borderedProminent).foregroundStyle(.black).frame(maxWidth: .infinity)
            Button(strings["shelf_form_later"]) { send(.save(true)) }.frame(maxWidth: .infinity)
        }
        .disabled(add.saving)
        .task(id: add.landing) { if add.landing { send(.command("landed")) } }
    }

    private var coffeeField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings["shelf_form_coffee"]).font(.headline)
            TextField(strings["shelf_form_coffee_hint"], text: Binding(get: { add.query }, set: { send(.query($0)) }))
                .textFieldStyle(.roundedBorder).autocorrectionDisabled()
            if add.searching { ProgressView() }
            if add.searchFailed { Text(strings["shelf_form_coffee_failed"]).font(.footnote).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            ForEach(add.options, id: \.slug) { option in
                Button { send(.coffee(option.slug)) } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(option.title).font(.headline)
                        Text(option.roaster).font(.subheadline).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    }.frame(maxWidth: .infinity, alignment: .leading).padding(8)
                }.buttonStyle(.plain)
            }
        }
    }

    private var weights: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 65))], spacing: 8) {
            ForEach([200, 250, 500, 1000] as [Int], id: \.self) { grams in
                let selected = add.grams?.intValue == Int32(grams)
                Button(String(format: strings["shelf_weight"], grams)) {
                    send(.weight(selected ? nil : grams))
                }
                .buttonStyle(.bordered).tint(selected ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.secondary)
                .accessibilityAddTraits(selected ? .isSelected : [])
            }
        }
    }
    private var traits: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                trait("Beans", key: "shelf_grind_beans", selected: add.grind) { send(.grind($0)) }
                trait("Ground", key: "shelf_grind_ground", selected: add.grind) { send(.grind($0)) }
            }
            HStack {
                trait("Espresso", key: "shelf_roast_espresso", selected: add.purpose) { send(.purpose($0)) }
                trait("Filter", key: "shelf_roast_filter", selected: add.purpose) { send(.purpose($0)) }
            }
        }
    }
    private func trait(_ value: String, key: String, selected: String?, choose: @escaping (String?) -> Void) -> some View {
        Button(strings[key]) { choose(selected == value ? nil : value) }
            .buttonStyle(.bordered).tint(selected == value ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.secondary)
            .accessibilityAddTraits(selected == value ? .isSelected : [])
    }

    private var roastDate: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings["shelf_form_roasted"]).font(.subheadline)
            HStack {
                Button(add.roastedOn ?? strings["shelf_form_roasted_pick"]) {
                    roasted = Self.formatter.date(from: add.roastedOn ?? add.today) ?? Date()
                    dateShown = true
                }
                if add.roastedOn != nil { Button(strings["shelf_form_roasted_clear"]) { send(.date(nil)) } }
            }
        }
        .sheet(isPresented: $dateShown) {
            VStack(spacing: 16) {
                DatePicker(strings["shelf_form_roasted"], selection: $roasted,
                    in: ...(Self.formatter.date(from: add.today) ?? Date()), displayedComponents: .date)
                    .datePickerStyle(.graphical)
                Button(strings["shelf_date_done"]) { send(.date(Self.formatter.string(from: roasted))); dateShown = false }
            }.padding().presentationDetents([.medium, .large])
        }
    }
    private static var formatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }
}

private func shelfFormPreview() -> some View {
    let state = IosShelfAddState(step: "Form", scanner: "Ready", downloadBytes: 0, downloadDone: 0,
        downloadTotal: 0, frame: [], stages: [], failure: nil, query: "Santa Rosa", roaster: "Coffee Roaster",
        grams: KotlinInt(int: 250), grind: "Beans", purpose: "Filter", roastedOn: nil, today: "2026-11-18",
        searching: false, searchFailed: false, options: [], saving: false, duplicate: false, landing: false, hints: [], message: nil)
    return ScrollView { ShelfNativeBagForm(add: state, strings: ShelfNativeStrings("ru"), card: nil, send: { _ in }).padding() }
        .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
}
#Preview("Add bag form light") { shelfFormPreview().preferredColorScheme(.light) }
#Preview("Add bag form dark") { shelfFormPreview().preferredColorScheme(.dark) }

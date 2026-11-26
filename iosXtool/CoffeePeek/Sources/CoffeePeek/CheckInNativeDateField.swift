import SwiftUI

struct CheckInNativeDateField: View {
    let title: String
    let value: String
    let today: String
    let label: String
    let done: String
    let cancel: String
    let changed: (String) -> Void
    var style: CheckInNativeFormStyle = .standard
    var error: String? = nil
    @State private var presented = false
    @State private var selection = Date()

    var body: some View {
        Button {
            selection = date(value) ?? Date()
            presented = true
        } label: {
            if style == .home {
                HomeCheckInNativeSelection(label: label, symbol: "calendar", error: error)
            } else {
                HStack {
                    Text(label)
                    Spacer()
                    Image(systemName: "calendar").accessibilityHidden(true)
                }
                .padding(16).frame(minHeight: 48)
                .background(CoffeePeekNativeStyle.surface, in: Capsule())
                .overlay(Capsule().stroke(CoffeePeekNativeStyle.border))
            }
        }
        .buttonStyle(.plain).accessibilityLabel(title).accessibilityValue(label)
        .sheet(isPresented: $presented) {
            NavigationStack {
                DatePicker(title, selection: $selection, in: ...(date(today) ?? Date()), displayedComponents: .date)
                    .datePickerStyle(.graphical).padding()
                    .tint(CoffeePeekNativeStyle.accent)
                    .navigationTitle(title).navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button(cancel) { presented = false } }
                        ToolbarItem(placement: .confirmationAction) {
                            Button(done) {
                                let selected = civilDate(selection)
                                if selected != value { changed(selected) }
                                presented = false
                            }
                        }
                    }
            }.presentationDetents([.medium, .large])
        }
    }

    private func date(_ value: String) -> Date? {
        let parts = value.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return Calendar(identifier: .gregorian).date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2], hour: 12))
    }

    private func civilDate(_ date: Date) -> String {
        let parts = Calendar(identifier: .gregorian).dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", parts.year ?? 1970, parts.month ?? 1, parts.day ?? 1)
    }
}

#Preview("Check-in date light") {
    CheckInNativeDateField(title: "Дата", value: "2026-11-17", today: "2026-11-17", label: "17 ноября", done: "Готово", cancel: "Отмена", changed: { _ in })
        .padding().preferredColorScheme(.light)
}

#Preview("Check-in date dark") {
    CheckInNativeDateField(title: "Дата", value: "2026-11-17", today: "2026-11-17", label: "17 ноября", done: "Готово", cancel: "Отмена", changed: { _ in })
        .padding().preferredColorScheme(.dark)
}

#Preview("Home date light") { homeDatePreview().preferredColorScheme(.light) }
#Preview("Home date dark") { homeDatePreview().preferredColorScheme(.dark) }

@MainActor
private func homeDatePreview() -> some View {
    let strings = CheckInNativeStrings(languageTag: "ru", table: "NativeHomeCheckIn")
    return CheckInNativeField(title: strings["home_check_in_prepared_date"], style: .home) {
        CheckInNativeDateField(title: strings["home_check_in_choose_date"], value: "2026-11-17", today: "2026-11-17",
            label: "17 ноября 2026", done: strings["home_check_in_done"], cancel: strings["home_check_in_cancel"],
            changed: { _ in }, style: .home)
    }.padding(16).background(CoffeePeekNativeStyle.background)
}

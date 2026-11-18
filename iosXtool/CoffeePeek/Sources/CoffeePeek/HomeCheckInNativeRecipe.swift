import ComposeApp
import SwiftUI

struct HomeCheckInNativeRecipe: View {
    let state: IosHomeCheckInState
    let strings: CheckInNativeStrings
    let actions: HomeCheckInNativeActions
    @State private var expanded: Bool?
    private var hasRecipe: Bool {
        state.draft.methodCode != nil || state.draft.beanName != nil || state.draft.shelfBagId != nil ||
        !state.draft.dose.isEmpty || state.draft.durationSeconds != nil
    }
    private var recipeError: Bool { state.issues.fields.contains { ["Method", "Bean", "Dose", "Duration"].contains($0.field) } }

    var body: some View {
        DisclosureGroup(isExpanded: Binding(get: { expanded ?? (hasRecipe || recipeError) }, set: { expanded = $0 })) {
            VStack(alignment: .leading, spacing: 12) {
                method
                bean
                CheckInNativeField(title: strings["home_check_in_dose_label"], error: error("Dose")) {
                    CheckInNativeInput(title: strings["home_check_in_dose_description"], placeholder: strings["home_check_in_dose_hint"],
                        value: Binding(get: { state.draft.dose }, set: { actions.dose(String($0.prefix(8))) }), keyboard: .decimalPad)
                }
                HomeCheckInNativeDuration(seconds: state.draft.durationSeconds?.intValue, error: error("Duration"),
                                          strings: strings, changed: actions.duration)
            }.padding(.top, 12)
        } label: {
            Text(strings["home_check_in_recipe_title"]).font(CoffeePeekNativeStyle.font(17, weight: "Semibold"))
                .frame(minHeight: 48)
        }
        .tint(CoffeePeekNativeStyle.foreground).disabled(!state.status.canEdit)
        .onChange(of: state.status.saveAttempt) { _, _ in if recipeError { expanded = true } }
        .onChange(of: recipeError) { _, failed in if failed { expanded = true } }
    }

    private var method: some View {
        CheckInNativeField(title: strings["home_check_in_brew_method"], error: error("Method")) {
            Menu {
                Button(strings["home_check_in_omit"]) { actions.method(nil) }
                ForEach(state.catalog.methods, id: \.id) { method in
                    Button { actions.method(method.id) } label: {
                        if state.draft.methodCode == method.id { Label(method.name ?? method.id, systemImage: "checkmark") }
                        else { Text(method.name ?? method.id) }
                    }
                }
            } label: {
                HomeCheckInNativeSelection(label: methodLabel, symbol: "cup.and.saucer")
            }.disabled(state.catalog.methodsLoading).accessibilityLabel(strings["home_check_in_method_action"])
            if state.catalog.methodsLoading { ProgressView(strings["home_check_in_methods_loading"]) }
            if state.catalog.methodsIssueKey != nil {
                Button(strings["home_check_in_methods_retry"], action: actions.retryMethods)
            }
        }
    }

    private var methodLabel: String {
        guard let code = state.draft.methodCode else { return strings["home_check_in_choose_method"] }
        return state.catalog.methods.first { $0.id == code }?.name ??
            (code == "other" ? strings["home_check_in_method_other"] : code)
    }

    private var bean: some View {
        CheckInNativeField(title: strings["home_check_in_bean"], error: error("Bean")) {
            Button(action: actions.openBeans) {
                HomeCheckInNativeSelection(label: state.draft.beanName ?? strings["home_check_in_choose_bean"], symbol: "leaf")
            }.buttonStyle(.plain)
            if state.draft.shelfBagId != nil, let url = state.draft.shelfBagCardUrl {
                HStack(spacing: 12) {
                    HomeCheckInNativeRemoteImage(url: url, load: actions.loadImage)
                        .frame(width: 56, height: 72).clipShape(RoundedRectangle(cornerRadius: 4))
                    Toggle(isOn: Binding(get: { state.draft.showShelfBag }, set: actions.showBag)) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(strings["home_check_in_show_bag_title"])
                            Text(strings["home_check_in_show_bag_hint"]).font(CoffeePeekNativeStyle.font(13))
                                .foregroundStyle(CoffeePeekNativeStyle.secondary)
                        }
                    }.tint(CoffeePeekNativeStyle.accent)
                }
            }
        }
    }
    private func error(_ field: String) -> String? {
        state.issues.fields.first { $0.field == field }.map { strings[$0.messageKey] }
    }
}

struct HomeCheckInNativeSelection: View {
    let label: String
    let symbol: String
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol).accessibilityHidden(true)
            Text(label).frame(maxWidth: .infinity, alignment: .leading)
            Image(systemName: "chevron.down").accessibilityHidden(true)
        }.padding(16).frame(minHeight: 48)
            .background(CoffeePeekNativeStyle.surface, in: Capsule())
            .overlay(Capsule().stroke(CoffeePeekNativeStyle.border))
            .foregroundStyle(CoffeePeekNativeStyle.foreground)
    }
}

struct HomeCheckInNativeDuration: View {
    let seconds: Int?
    let error: String?
    let strings: CheckInNativeStrings
    let changed: (Int?) -> Void
    @State private var minutesText = ""
    @State private var secondsText = ""
    @State private var lastEmitted: Int?
    var body: some View {
        CheckInNativeField(title: strings["home_check_in_duration"], error: error) {
            HStack(spacing: 12) {
                number("home_check_in_duration_minutes", accessibility: "home_check_in_duration_minutes_description",
                    value: $minutesText, maximum: 10080)
                number("home_check_in_duration_seconds", accessibility: "home_check_in_duration_seconds_description",
                    value: $secondsText, maximum: 59)
            }
        }.onAppear { synchronize() }.onChange(of: seconds) { _, _ in if seconds != lastEmitted { synchronize() } }
    }
    private func number(_ label: String, accessibility: String, value: Binding<String>, maximum: Int) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(strings[label]).font(CoffeePeekNativeStyle.font(13))
            CheckInNativeInput(title: strings[accessibility], placeholder: "0", value: Binding(get: { value.wrappedValue }, set: {
                let digits = String($0.filter(\.isNumber).prefix(8))
                guard digits.isEmpty || (Int(digits) ?? maximum + 1) <= maximum else { return }
                value.wrappedValue = digits
                emit()
            }), keyboard: .numberPad)
        }
    }
    private func synchronize() {
        minutesText = seconds.map { $0 / 60 }.flatMap { $0 > 0 ? String($0) : nil } ?? ""
        secondsText = seconds.map { $0 % 60 }.flatMap { $0 > 0 ? String($0) : nil } ?? ""
        lastEmitted = seconds
    }
    private func emit() {
        lastEmitted = minutesText.isEmpty && secondsText.isEmpty ? nil : (Int(minutesText) ?? 0) * 60 + (Int(secondsText) ?? 0)
        changed(lastEmitted)
    }
}

#Preview("Home recipe light") {
    HomeCheckInNativeRecipe(state: homeCheckInNativePreviewState(), strings: homeCheckInNativePreviewStrings("ru"), actions: .init())
        .padding().background(CoffeePeekNativeStyle.background).preferredColorScheme(.light)
}
#Preview("Home recipe dark") {
    HomeCheckInNativeRecipe(state: homeCheckInNativePreviewState(), strings: homeCheckInNativePreviewStrings("be"), actions: .init())
        .padding().background(CoffeePeekNativeStyle.background).preferredColorScheme(.dark)
}

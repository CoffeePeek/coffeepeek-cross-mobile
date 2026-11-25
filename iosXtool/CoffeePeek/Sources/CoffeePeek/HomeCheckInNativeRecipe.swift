import ComposeApp
import SwiftUI

struct HomeCheckInNativeRecipe: View {
    let state: IosHomeCheckInState
    let strings: CheckInNativeStrings
    let actions: HomeCheckInNativeActions
    @State private var expanded: Bool?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private var isExpanded: Bool { expanded ?? (hasRecipe || recipeError) }
    private var hasRecipe: Bool {
        state.draft.methodCode != nil || state.draft.beanName != nil || state.draft.shelfBagId != nil ||
        !state.draft.dose.isEmpty || state.draft.durationSeconds != nil
    }
    private var recipeError: Bool { state.issues.fields.contains { ["Method", "Bean", "Dose", "Duration"].contains($0.field) } }

    var body: some View {
        DisclosureGroup(isExpanded: Binding(get: { isExpanded }, set: { expanded = $0 })) {
            recipeFields.padding(.top, 12).transition(reduceMotion ? .identity : .opacity)
        } label: {
            Text(strings["home_check_in_recipe_title"]).font(CoffeePeekNativeStyle.font(17, weight: "Semibold"))
                .frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
                .contentShape(Rectangle())
        }
        .disclosureGroupStyle(.automatic)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: isExpanded)
        .tint(CoffeePeekNativeStyle.foreground).disabled(!state.status.canEdit)
        .onChange(of: state.status.saveAttempt) { _, _ in if recipeError { expanded = true } }
        .onChange(of: recipeError) { _, failed in if failed { expanded = true } }
    }

    private var recipeFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            method
            bean
            CheckInNativeField(title: strings["home_check_in_dose_label"], error: error("Dose"), style: .home) {
                CheckInNativeInput(title: strings["home_check_in_dose_description"], placeholder: strings["home_check_in_dose_hint"],
                    value: Binding(get: { state.draft.dose }, set: { actions.dose(String($0.prefix(8))) }),
                    keyboard: .decimalPad, style: .home, error: error("Dose"))
            }
            HomeCheckInNativeDuration(seconds: state.draft.durationSeconds?.intValue, error: error("Duration"),
                                      strings: strings, changed: actions.duration)
        }
    }

    private var method: some View {
        CheckInNativeField(title: strings["home_check_in_brew_method"], error: error("Method"), style: .home) {
            Menu {
                Button(strings["home_check_in_omit"]) { actions.method(nil) }
                ForEach(state.catalog.methods, id: \.id) { method in
                    Button { actions.method(method.id) } label: {
                        if state.draft.methodCode == method.id { Label(method.name ?? method.id, systemImage: "checkmark") }
                        else { Text(method.name ?? method.id) }
                    }
                }
            } label: {
                HomeCheckInNativeSelection(label: methodLabel, symbol: "cup.and.saucer", error: error("Method"))
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
        CheckInNativeField(title: strings["home_check_in_bean"], error: error("Bean"), style: .home) {
            Button(action: actions.openBeans) {
                HomeCheckInNativeSelection(label: state.draft.beanName ?? strings["home_check_in_choose_bean"], symbol: "leaf", error: error("Bean"))
            }.buttonStyle(.plain)
            if state.draft.shelfBagId != nil, let url = state.draft.shelfBagCardUrl {
                HStack(spacing: 12) {
                    HomeCheckInNativeRemoteImage(url: url, load: actions.loadImage)
                        .frame(width: 56, height: 72).clipShape(RoundedRectangle(cornerRadius: 4))
                    Toggle(isOn: Binding(get: { state.draft.showShelfBag }, set: { actions.showBag($0) })) {
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
    var error: String? = nil
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: symbol).font(.system(size: 18))
                .foregroundStyle(CoffeePeekNativeStyle.secondary).accessibilityHidden(true)
            Text(label).font(CoffeePeekNativeStyle.font(15)).frame(maxWidth: .infinity, alignment: .leading)
            Image(systemName: "chevron.up.chevron.down").font(.system(size: 16))
                .foregroundStyle(CoffeePeekNativeStyle.secondary).accessibilityHidden(true)
        }.padding(.horizontal, 14).padding(.vertical, 12).frame(minHeight: 44)
            .background(error == nil ? CoffeePeekNativeStyle.foreground.opacity(0.04) : CoffeePeekNativeStyle.error.opacity(0.05),
                        in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(error == nil ? CheckInNativeFormStyle.homeOutline : CoffeePeekNativeStyle.error))
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
        CheckInNativeField(title: strings["home_check_in_duration"], error: error, style: .home) {
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
            CheckInNativeInput(title: strings[accessibility], placeholder: maximum == 59 ? "00" : "0", value: Binding(get: { value.wrappedValue }, set: {
                let digits = String($0.filter(\.isNumber).prefix(8))
                guard digits.isEmpty || (Int(digits) ?? maximum + 1) <= maximum else { return }
                value.wrappedValue = digits
                emit()
            }), keyboard: .numberPad, style: .home, error: error)
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

private struct HomeRecipeDisclosurePreview: View {
    @State private var showError = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                HomeCheckInNativeRecipe(state: homeRecipeDisclosurePreviewState(showError: showError),
                    strings: homeCheckInNativePreviewStrings("ru"), actions: .init())
                Button("Показать ошибку дозы") { showError = true }
            }.padding(16)
        }
        .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
    }
}

private func homeRecipeDisclosurePreviewState(showError: Bool) -> IosHomeCheckInState {
    let base = homeCheckInNativePreviewState()
    let draft = IosHomeCheckInDraft(preparedOn: "2026-11-25", dateLabel: "25.11.2026", methodCode: nil,
        dose: "", durationSeconds: nil, tasteScore: nil, note: "", drinkName: "", isPublic: nil,
        beanName: nil, shelfBagId: nil, showShelfBag: false, shelfBagCardUrl: nil,
        photos: [], existingPhotoUrls: [], customBeanName: nil)
    let fields = showError ? [IosHomeCheckInFieldIssue(field: "Dose", messageKey: "home_check_in_error_dose")] : []
    return IosHomeCheckInState(draft: draft, status: base.status, catalog: base.catalog,
        issues: IosHomeCheckInIssues(loadKey: nil, saveKey: nil, fields: fields), saved: nil)
}

#Preview("Recipe disclosure light") { HomeRecipeDisclosurePreview().preferredColorScheme(.light) }
#Preview("Recipe disclosure dark") { HomeRecipeDisclosurePreview().preferredColorScheme(.dark) }
#Preview("Recipe disclosure large text") {
    HomeRecipeDisclosurePreview().preferredColorScheme(.dark).environment(\.dynamicTypeSize, .accessibility2)
}

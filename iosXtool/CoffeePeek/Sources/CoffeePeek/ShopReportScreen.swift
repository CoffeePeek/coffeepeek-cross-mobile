import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShopReportController(
    report: IosShopReport,
    languageTag: String,
    shopTitle: String,
    onBack: @escaping () -> KotlinUnit
) -> UIViewController {
    ShopReportHostingController(report: report, languageTag: languageTag, shopTitle: shopTitle,
                                onBack: { _ = onBack() })
}

@MainActor
private final class ShopReportHostingController: UIHostingController<ShopReportHost> {
    private let report: IosShopReport

    init(report: IosShopReport, languageTag: String, shopTitle: String, onBack: @escaping () -> Void) {
        self.report = report
        super.init(rootView: ShopReportHost(report: report, strings: ShopReportStrings(languageTag: languageTag),
                                           shopTitle: shopTitle, onBack: onBack))
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("Use init(report:languageTag:shopTitle:onBack:)") }

    deinit {
        let owner = report
        DispatchQueue.main.async { owner.close() }
    }
}

private struct ShopReportHost: View {
    let report: IosShopReport
    let strings: ShopReportStrings
    let shopTitle: String
    let onBack: () -> Void
    @State private var state: ShopReportViewState
    @State private var observation: IosShopReportObservation?

    init(report: IosShopReport, strings: ShopReportStrings, shopTitle: String, onBack: @escaping () -> Void) {
        self.report = report
        self.strings = strings
        self.shopTitle = shopTitle
        self.onBack = onBack
        _state = State(initialValue: ShopReportViewState(report.currentState()))
    }

    var body: some View {
        ShopReportScreen(state: state, strings: strings, shopTitle: shopTitle,
                         selectCategory: { report.selectCategory(category: $0) },
                         changeDescription: { report.changeDescription(description: $0) },
                         submit: { report.submit() }, onBack: onBack)
            .onAppear(perform: observe)
            .onDisappear {
                observation?.cancel()
                observation = nil
            }
    }

    private func observe() {
        guard observation == nil else { return }
        state = ShopReportViewState(report.currentState())
        observation = report.observe { [binding = $state] snapshot in
            binding.wrappedValue = ShopReportViewState(snapshot)
        }
    }
}

private struct ShopReportViewState {
    var category: String?
    var description = ""
    var submitting = false
    var submitted = false
    var unconfirmed = false
    var error: String?
    var sessionEnded = false

    init(category: String? = nil, description: String = "", submitting: Bool = false,
         submitted: Bool = false, unconfirmed: Bool = false, error: String? = nil) {
        self.category = category
        self.description = description
        self.submitting = submitting
        self.submitted = submitted
        self.unconfirmed = unconfirmed
        self.error = error
    }

    init(_ snapshot: IosShopReportState) {
        category = snapshot.selectedCategory
        description = snapshot.descriptionText
        submitting = snapshot.isSubmitting
        submitted = snapshot.isSubmitted
        unconfirmed = snapshot.deliveryUnconfirmed
        error = snapshot.error
        sessionEnded = snapshot.sessionEnded
    }
}

private struct ShopReportScreen: View {
    let state: ShopReportViewState
    let strings: ShopReportStrings
    let shopTitle: String
    let selectCategory: (String) -> Void
    let changeDescription: (String) -> Void
    let submit: () -> Void
    let onBack: () -> Void
    @FocusState private var descriptionFocused: Bool

    private var title: String {
        shopTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? strings.text("shop_report_shop_fallback") : shopTitle
    }

    var body: some View {
        Form {
            Group {
                if state.sessionEnded {
                    message("shop_report_session_ended", button: "shop_report_back")
                } else if state.unconfirmed {
                    message("shop_report_unconfirmed", button: "shop_report_back")
                } else if state.submitted {
                    success
                } else {
                    formFields
                }
            }.listRowBackground(CoffeePeekNativeStyle.surface)
        }
        .scrollContentBackground(.hidden)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .foregroundStyle(CoffeePeekNativeStyle.foreground).tint(CoffeePeekNativeStyle.accent)
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button(strings.text("shop_report_done")) { descriptionFocused = false }
            }
        }
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Button(action: onBack) {
                Label(strings.text("shop_report_back"), systemImage: "chevron.backward")
                    .labelStyle(.iconOnly)
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(Rectangle())
            }
            Text(strings.text("shop_report_title"))
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .background(CoffeePeekNativeStyle.background)
    }

    private var formFields: some View {
        Group {
            Section {
                Text(title).font(.headline)
                Text(strings.text("shop_report_prompt")).foregroundStyle(.secondary)
            }
            Section {
                ForEach(ShopReportCategory.all, id: \.id) { category in
                    categoryButton(category)
                }
            }
            if state.category == "Other" { descriptionField }
            submission
        }
    }

    private func categoryButton(_ category: ShopReportCategory) -> some View {
        Button { selectCategory(category.id) } label: {
            HStack {
                Text(strings.text(category.key)).foregroundStyle(.primary)
                Spacer(minLength: 12)
                if state.category == category.id {
                    Image(systemName: "checkmark").accessibilityHidden(true)
                }
            }
            .contentShape(Rectangle())
        }
        .disabled(state.submitting)
        .accessibilityAddTraits(state.category == category.id ? .isSelected : [])
    }

    private var descriptionField: some View {
        Section {
            TextEditor(text: Binding(get: { state.description }, set: changeDescription))
                .frame(minHeight: 112)
                .focused($descriptionFocused)
                .disabled(state.submitting)
                .accessibilityLabel(strings.text("shop_report_description_label"))
        } header: {
            Text(strings.text("shop_report_description_label"))
        } footer: {
            Text(strings.text("shop_report_description_limit"))
        }
    }

    private var submission: some View {
        Section {
            if let error = state.error, let key = ShopReportStrings.errorKeys[error] {
                Text(strings.text(key)).foregroundStyle(.red)
            }
            if state.submitting {
                ProgressView(strings.text("shop_report_submitting"))
            } else {
                Button(strings.text("shop_report_submit")) {
                    descriptionFocused = false
                    submit()
                }
                .disabled(state.category == nil)
            }
        }
    }

    private var success: some View {
        Section {
            Text(strings.text("shop_report_thanks")).font(.headline)
            Text(String(format: strings.text("shop_report_will_review"), title))
                .foregroundStyle(.secondary)
            Button(strings.text("shop_report_done"), action: onBack)
        }
    }

    private func message(_ key: String, button: String) -> some View {
        Section {
            Text(strings.text(key)).foregroundStyle(.secondary)
            Button(strings.text(button), action: onBack)
        }
    }
}

private struct ShopReportCategory {
    let id: String
    let key: String

    static let all = [
        Self(id: "OutdatedMenu", key: "shop_report_outdated_menu"),
        Self(id: "ShopClosed", key: "shop_report_shop_closed"),
        Self(id: "IncorrectAddress", key: "shop_report_incorrect_address"),
        Self(id: "WrongOpeningHours", key: "shop_report_wrong_hours"),
        Self(id: "IncorrectPhotos", key: "shop_report_incorrect_photos"),
        Self(id: "Other", key: "shop_report_other")
    ]
}

private struct ShopReportStrings {
    private let bundle: Bundle
    static let errorKeys = ["CategoryRequired": "shop_report_category_required",
                            "DescriptionRequired": "shop_report_description_required",
                            "SubmissionFailed": "shop_report_submission_failed"]

    init(languageTag: String) {
        let localization = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
            ?? Bundle.main.url(forResource: "ru", withExtension: "lproj")
        bundle = localization.flatMap { Bundle(url: $0) } ?? .main
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "ShopReport")
    }
}

#Preview("Light") {
    ShopReportScreen(state: ShopReportViewState(category: "Other", description: "Неверный адрес"),
                     strings: ShopReportStrings(languageTag: "ru"), shopTitle: "Кофейня",
                     selectCategory: { _ in }, changeDescription: { _ in }, submit: {}, onBack: {})
        .preferredColorScheme(.light)
}

#Preview("Dark") {
    ShopReportScreen(state: ShopReportViewState(category: "ShopClosed", error: "SubmissionFailed"),
                     strings: ShopReportStrings(languageTag: "be"), shopTitle: "Кавярня",
                     selectCategory: { _ in }, changeDescription: { _ in }, submit: {}, onBack: {})
        .preferredColorScheme(.dark)
}

#Preview("Unconfirmed") {
    ShopReportScreen(state: ShopReportViewState(unconfirmed: true),
                     strings: ShopReportStrings(languageTag: "ru"), shopTitle: "Кофейня",
                     selectCategory: { _ in }, changeDescription: { _ in }, submit: {}, onBack: {})
        .preferredColorScheme(.dark)
}

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

    isolated deinit { report.close() }
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
    @ScaledMetric(relativeTo: .body) private var descriptionHeight: CGFloat = 112

    private var title: String {
        shopTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? strings.text("shop_report_shop_fallback") : shopTitle
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if state.sessionEnded {
                    message("shop_report_session_ended", button: "shop_report_back")
                } else if state.unconfirmed {
                    message("shop_report_unconfirmed", button: "shop_report_back")
                } else if state.submitted {
                    success
                } else {
                    formFields
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .font(CoffeePeekNativeStyle.font(16))
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
        CoffeePeekNativeNavigationHeader(title: strings.text("shop_report_title"),
            backLabel: strings.text("shop_report_back"), back: onBack)
    }

    private var formFields: some View {
        Group {
            Text(title)
                .font(CoffeePeekNativeStyle.font(22, weight: "Semibold", relativeTo: .title2))
                .accessibilityAddTraits(.isHeader)
            Text(strings.text("shop_report_prompt")).foregroundStyle(CoffeePeekNativeStyle.secondary)
            VStack(spacing: 0) {
                ForEach(Array(ShopReportCategory.all.enumerated()), id: \.element.id) { index, category in
                    if index > 0 {
                        Divider().overlay(CoffeePeekNativeStyle.border).padding(.horizontal, 16)
                    }
                    categoryButton(category)
                }
            }
            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 24))
            .overlay { RoundedRectangle(cornerRadius: 24).strokeBorder(CoffeePeekNativeStyle.border) }
            if state.category == "Other" { descriptionField }
            submission
        }
    }

    private func categoryButton(_ category: ShopReportCategory) -> some View {
        Button { selectCategory(category.id) } label: { categoryLabel(category) }
        .buttonStyle(.plain)
        .opacity(state.submitting ? 0.5 : 1)
        .disabled(state.submitting)
        .accessibilityAddTraits(state.category == category.id ? .isSelected : [])
    }

    private func categoryLabel(_ category: ShopReportCategory) -> some View {
        HStack(spacing: 12) {
            Text(strings.text(category.key)).foregroundStyle(CoffeePeekNativeStyle.foreground)
            Spacer(minLength: 12)
            ZStack {
                if state.category == category.id {
                    Image(systemName: "checkmark").font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(CoffeePeekNativeStyle.accent)
                }
            }
            .frame(width: 24, height: 24).accessibilityHidden(true)
        }
        .padding(.horizontal, 16).padding(.vertical, 12)
        .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
        .contentShape(Rectangle())
    }

    private var descriptionField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings.text("shop_report_description_label"))
                .font(CoffeePeekNativeStyle.font(14, weight: "Medium"))
            TextEditor(text: Binding(get: { state.description }, set: { changeDescription($0) }))
                .scrollContentBackground(.hidden)
                .frame(height: descriptionHeight).padding(12)
                .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
                .overlay {
                    RoundedRectangle(cornerRadius: 16).strokeBorder(
                        state.error == "DescriptionRequired" ? CoffeePeekNativeStyle.error : CoffeePeekNativeStyle.border)
                }
                .focused($descriptionFocused)
                .disabled(state.submitting)
                .accessibilityLabel(strings.text("shop_report_description_label"))
            Text(strings.text("shop_report_description_limit"))
                .font(CoffeePeekNativeStyle.font(12, relativeTo: .caption))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }

    private var submission: some View {
        VStack(alignment: .leading, spacing: 20) {
            if let error = state.error, let key = ShopReportStrings.errorKeys[error] {
                Text(strings.text(key)).font(CoffeePeekNativeStyle.font(12, relativeTo: .caption))
                    .foregroundStyle(CoffeePeekNativeStyle.error)
            }
            if state.submitting {
                CoffeePeekNativeLoader(label: strings.text("shop_report_submitting"), size: 32, showsLabel: true)
                    .frame(maxWidth: .infinity, minHeight: 48)
            } else {
                CoffeePeekNativeActionButton(title: strings.text("shop_report_submit"), enabled: state.category != nil) {
                    descriptionFocused = false
                    submit()
                }
            }
        }
        .padding(.top, 8)
    }

    private var success: some View {
        Group {
            Text(strings.text("shop_report_thanks"))
                .font(CoffeePeekNativeStyle.font(24, weight: "Semibold", relativeTo: .title2))
            Text(String(format: strings.text("shop_report_will_review"), title))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
            CoffeePeekNativeActionButton(title: strings.text("shop_report_done"), action: onBack)
        }
    }

    private func message(_ key: String, button: String) -> some View {
        Group {
            Text(strings.text(key)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            CoffeePeekNativeActionButton(title: strings.text(button), action: onBack)
        }
    }
}

private struct ShopReportCategory {
    let id: String
    let key: String

    static let all = [
        Self(id: "OutdatedMenu", key: "shop_report_outdated_menu"),
        Self(id: "ShopClosed", key: "shop_report_shop_closed"),
        Self(id: "WrongOpeningHours", key: "shop_report_wrong_hours"),
        Self(id: "IncorrectPhotos", key: "shop_report_incorrect_photos"),
        Self(id: "IncorrectAddress", key: "shop_report_incorrect_address"),
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

@MainActor
private func shopReportPreview(_ state: ShopReportViewState = ShopReportViewState(), languageTag: String = "ru") -> some View {
    ShopReportScreen(state: state, strings: ShopReportStrings(languageTag: languageTag), shopTitle: "CoffeePeek",
                     selectCategory: { _ in }, changeDescription: { _ in }, submit: {}, onBack: {})
}

#Preview("Report · Light") { shopReportPreview().preferredColorScheme(.light) }
#Preview("Report · Dark") { shopReportPreview().preferredColorScheme(.dark) }
#Preview("Other · Light") {
    shopReportPreview(ShopReportViewState(category: "Other", description: "Неверный адрес")).preferredColorScheme(.light)
}
#Preview("Other · Dark") {
    shopReportPreview(ShopReportViewState(category: "Other", description: "Неверный адрес")).preferredColorScheme(.dark)
}
#Preview("Error · Belarusian") {
    shopReportPreview(ShopReportViewState(category: "ShopClosed", error: "SubmissionFailed"), languageTag: "be")
        .preferredColorScheme(.dark)
}
#Preview("Submitting") {
    shopReportPreview(ShopReportViewState(category: "ShopClosed", submitting: true)).preferredColorScheme(.light)
}
#Preview("Submitted") {
    shopReportPreview(ShopReportViewState(submitted: true)).preferredColorScheme(.light)
}
#Preview("Unconfirmed") {
    shopReportPreview(ShopReportViewState(unconfirmed: true)).preferredColorScheme(.dark)
}
#Preview("Report · Large text") {
    shopReportPreview().preferredColorScheme(.dark).environment(\.dynamicTypeSize, .accessibility2)
}

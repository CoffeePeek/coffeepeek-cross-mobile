import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeContributionsController(route: IosContributionsRoute, languageTag: String) -> UIViewController {
    let strings = ContributionsStrings(languageTag: languageTag)
    if let list = route as? IosContributionsList {
        return ContributionsHostingController(route: route, content: ContributionsListHost(route: list, strings: strings))
    }
    if let detail = route as? IosChangeRequest {
        return ContributionsHostingController(route: route, content: ChangeRequestHost(route: detail, strings: strings))
    }
    if let form = route as? IosContributionForm {
        return makeContributionFormController(form: form, strings: strings)
    }
    preconditionFailure("Unknown iOS contributions route")
}

@MainActor
private final class ContributionsHostingController<Content: View>: UIHostingController<Content> {
    private let route: IosContributionsRoute

    init(route: IosContributionsRoute, content: Content) {
        self.route = route
        super.init(rootView: content)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("Use init(route:content:)") }

    deinit {
        let route = route
        DispatchQueue.main.async { route.close() }
    }
}

struct ContributionsStrings {
    let languageTag: String

    private var bundle: Bundle {
        let language = languageTag.split(separator: "-").first.map(String.init) ?? "ru"
        return Bundle.main.path(forResource: language, ofType: "lproj").flatMap(Bundle.init(path:)) ?? .main
    }

    func text(_ suffix: String) -> String {
        bundle.localizedString(forKey: "contributions_\(suffix)", value: nil, table: "NativeContributions")
    }

    func format(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: text(key), locale: Locale(identifier: languageTag), arguments: arguments)
    }
}

struct ContributionsScaffold<Content: View>: View {
    let title: String
    let backLabel: String
    let onBack: (() -> Void)?
    @ViewBuilder let content: Content

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                if let onBack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.backward")
                            .frame(width: 44, height: 44)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(backLabel)
                }
                Text(title)
                    .font(CoffeePeekNativeStyle.font(21, weight: "Bold", relativeTo: .title2))
                    .accessibilityAddTraits(.isHeader)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 4)
            content.frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
    }
}

struct ContributionsMessage: View {
    let text: String
    let actionTitle: String
    let onAction: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text(text).foregroundStyle(CoffeePeekNativeStyle.secondary)
            Button(actionTitle, action: onAction)
                .buttonStyle(.bordered)
                .tint(CoffeePeekNativeStyle.foreground)
                .frame(minHeight: 44)
        }
        .multilineTextAlignment(.center)
        .padding(24)
    }
}

struct ContributionStatusLabel: View {
    let status: String
    let strings: ContributionsStrings

    var body: some View {
        Label(strings.text("status_\(status)"), systemImage: icon)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Color.primary.opacity(0.07), in: Capsule())
    }

    private var icon: String {
        switch status {
        case "approved": "checkmark.circle"
        case "rejected": "exclamationmark.circle"
        default: "clock"
        }
    }
}

extension View {
    func contributionsCard() -> some View {
        self
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
            .overlay {
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(CoffeePeekNativeStyle.border, lineWidth: 1)
                    .allowsHitTesting(false)
            }
    }
}


private func contributionsSendingPreview() -> some View {
    ContributionsScaffold(title: "Отправка", backLabel: "Назад", onBack: nil) {
        ProgressView("Сохраняем заявку…")
    }
}
#Preview("Contributions sending light") { contributionsSendingPreview().preferredColorScheme(.light) }
#Preview("Contributions sending dark") { contributionsSendingPreview().preferredColorScheme(.dark) }

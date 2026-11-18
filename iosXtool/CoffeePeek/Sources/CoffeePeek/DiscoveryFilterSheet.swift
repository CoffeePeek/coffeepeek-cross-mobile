import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeDiscoveryFilterController(_ filters: IosDiscoveryFilters) -> UIViewController {
    let controller = UIHostingController(rootView: DiscoveryFilterHost(filters: filters))
    controller.modalPresentationStyle = .pageSheet
    if let sheet = controller.sheetPresentationController {
        sheet.detents = [.custom(identifier: .init("discovery-filters")) { $0.maximumDetentValue * 0.8 }]
        sheet.prefersGrabberVisible = true
        sheet.prefersScrollingExpandsWhenScrolledToEdge = false
        sheet.prefersEdgeAttachedInCompactHeight = true
    }
    return controller
}

@MainActor
private struct DiscoveryFilterHost: View {
    let filters: IosDiscoveryFilters
    @State private var state: IosDiscoveryFilterState
    @State private var observation: IosDiscoveryFilterObservation?

    init(filters: IosDiscoveryFilters) {
        self.filters = filters
        self._state = State(initialValue: filters.currentState())
    }

    var body: some View {
        DiscoveryFilterSheet(
            state: state,
            actions: DiscoveryFilterActions(
                reset: filters.reset, done: filters.apply, close: filters.close,
                price: { filters.price(index: $0) }, toggle: { filters.toggle(section: $0, id: $1) },
                retry: filters.retry, image: { filters.loadImage(url: $0, onImage: $1) }
            )
        )
        .onAppear {
            guard observation == nil else { return }
            state = filters.currentState()
            observation = filters.observe(onState: { state = $0 })
        }
        .onDisappear {
            observation?.cancel()
            observation = nil
        }
    }
}

private struct DiscoveryFilterActions {
    var reset: () -> Void = {}
    var done: () -> Void = {}
    var close: () -> Void = {}
    var price: (Int32) -> Void = { _ in }
    var toggle: (String, String) -> Void = { _, _ in }
    var retry: () -> Void = {}
    var image: (String, @escaping (UIImage?) -> Void) -> IosDiscoveryFilterObservation? = { _, _ in nil }
}

private struct DiscoveryFilterSheet: View {
    let state: IosDiscoveryFilterState
    let actions: DiscoveryFilterActions

    var body: some View {
        VStack(spacing: 0) {
            header
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 18) {
                    catalogStatus
                    priceSection
                    ForEach(state.sections, id: \.id) { section in
                        DiscoveryFilterSectionView(section: section, actions: actions)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            }
        }
        .padding(.top, 16)
        .background(CoffeePeekNativeStyle.surface)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .accessibilityAction(.escape, actions.close)
    }

    private var header: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 10) {
                resetButton
                Spacer(minLength: 0)
                title
                Spacer(minLength: 0)
                doneButton
                closeButton
            }
            VStack(spacing: 4) {
                title
                HStack {
                    resetButton
                    Spacer(minLength: 0)
                    doneButton
                    closeButton
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 4)
    }

    private var title: some View {
        Text(state.title)
            .font(CoffeePeekNativeStyle.font(17, weight: "Bold", relativeTo: .headline))
            .accessibilityAddTraits(.isHeader)
    }

    private var resetButton: some View {
        Button(state.reset, action: actions.reset)
            .disabled(!state.canReset)
            .tint(CoffeePeekNativeStyle.foreground)
            .frame(minHeight: 44)
    }

    private var doneButton: some View {
        Button(action: actions.done) {
            Text(state.done).fontWeight(.semibold)
                .frame(minHeight: 44)
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
        }
    }

    private var closeButton: some View {
        Button(action: actions.close) {
            Image(systemName: "xmark")
                .frame(minWidth: 44, minHeight: 44)
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
        }
        .accessibilityLabel(state.close)
    }

    @ViewBuilder private var catalogStatus: some View {
        if let status = state.catalogStatus {
            HStack(alignment: .center, spacing: 10) {
                if state.isLoading { ProgressView().accessibilityHidden(true) }
                Text(status).foregroundStyle(CoffeePeekNativeStyle.secondary)
                if let retry = state.retry {
                    Button(retry, action: actions.retry)
                        .tint(CoffeePeekNativeStyle.foreground)
                        .frame(minHeight: 44)
                }
            }
        }
    }

    private var priceSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            ViewThatFits(in: .horizontal) {
                HStack {
                    Text(state.priceTitle).font(.headline).accessibilityAddTraits(.isHeader)
                    Spacer()
                    Text(selectedPrice).foregroundStyle(CoffeePeekNativeStyle.secondary)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(state.priceTitle).font(.headline).accessibilityAddTraits(.isHeader)
                    Text(selectedPrice).foregroundStyle(CoffeePeekNativeStyle.secondary)
                }
            }
            if state.prices.count > 1 {
                Slider(value: Binding(get: { Double(state.priceIndex) }, set: { actions.price(Int32($0.rounded())) }),
                       in: 0...Double(state.prices.count - 1), step: 1) {
                    Text(state.priceTitle)
                }
                .accessibilityValue(selectedPrice)
                .frame(minHeight: 44)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(CoffeePeekNativeStyle.background, in: RoundedRectangle(cornerRadius: 16))
    }

    private var selectedPrice: String {
        let index = Int(state.priceIndex)
        return state.prices.indices.contains(index) ? state.prices[index] : ""
    }
}

private struct DiscoveryFilterSectionView: View {
    let section: IosDiscoveryFilterSection
    let actions: DiscoveryFilterActions
    @State private var expanded = false

    private var choices: [IosDiscoveryFilterChoice] {
        guard !expanded else { return section.choices }
        let initial = Array(section.choices.prefix(5))
        let visibleIDs = Set(initial.map(\.id))
        return initial + section.choices.filter { $0.selected && !visibleIDs.contains($0.id) }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(section.title).font(.headline).accessibilityAddTraits(.isHeader)
            if let selectedLabel = section.selectedLabel {
                Text(selectedLabel).font(.subheadline).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
            ForEach(choices, id: \.id) { choice in
                Divider().overlay(CoffeePeekNativeStyle.border)
                choiceRow(choice)
            }
            if section.choices.count > 5 {
                Divider().overlay(CoffeePeekNativeStyle.border)
                Button(expanded ? section.collapse : section.showAll) { expanded.toggle() }
                    .tint(CoffeePeekNativeStyle.foreground)
                    .frame(minHeight: 44)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(CoffeePeekNativeStyle.background, in: RoundedRectangle(cornerRadius: 16))
    }

    private func choiceRow(_ choice: IosDiscoveryFilterChoice) -> some View {
        Button {
            actions.toggle(section.id, choice.id)
        } label: {
            HStack(spacing: 12) {
                if section.id == "Roasters" {
                    DiscoveryFilterLogo(url: choice.photoUrl, load: actions.image)
                        .id(choice.photoUrl)
                        .accessibilityHidden(true)
                } else if section.id == "BrewMethods" {
                    Image(systemName: brewSymbol(choice))
                        .font(.system(size: 16))
                        .frame(width: 28, height: 28)
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                        .accessibilityHidden(true)
                }
                Text(choice.title).frame(maxWidth: .infinity, alignment: .leading)
                if choice.selected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(CoffeePeekNativeStyle.accent)
                        .accessibilityHidden(true)
                }
            }
            .foregroundStyle(CoffeePeekNativeStyle.foreground)
            .padding(.vertical, 6)
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(choice.selected ? .isSelected : [])
    }

    private func brewSymbol(_ choice: IosDiscoveryFilterChoice) -> String {
        let method = (choice.id + " " + choice.title).lowercased()
        let matches: ([String]) -> Bool = { $0.contains { method.contains($0) } }
        if matches(["cold", "колд", "айс"]) { return "snowflake" }
        if matches(["chemex", "кемекс"]) { return "hourglass" }
        if matches(["v60", "pour", "ворон", "filter", "batch", "фільтр", "фильтр"]) { return "line.3.horizontal.decrease" }
        if matches(["press", "пресс", "прэс"]) { return "arrow.down.to.line" }
        if matches(["cezve", "turkish", "турк", "джезв", "мока", "moka"]) { return "mug" }
        return "cup.and.saucer"
    }
}

private struct DiscoveryFilterLogo: View {
    let url: String?
    let load: (String, @escaping (UIImage?) -> Void) -> IosDiscoveryFilterObservation?
    @State private var image: UIImage?
    @State private var observation: IosDiscoveryFilterObservation?

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                Image(systemName: "cup.and.saucer").foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
        }
        .frame(width: 28, height: 28)
        .background(CoffeePeekNativeStyle.surface, in: Circle())
        .clipShape(Circle())
        .onAppear {
            guard observation == nil, let url, !url.isEmpty else { return }
            observation = load(url) { image = $0 }
        }
        .onDisappear {
            observation?.cancel()
            observation = nil
        }
    }
}

private var discoveryFilterPreview: IosDiscoveryFilterState {
    IosDiscoveryFilterState(
        title: "Filters", reset: "Reset", done: "Done", close: "Close",
        priceTitle: "Price", prices: ["Any", "Low", "Medium", "High"],
        priceIndex: 2, canReset: true, catalogStatus: nil, isLoading: false, retry: nil,
        sections: [
            IosDiscoveryFilterSection(
                id: "BrewMethods", title: "Brew methods", selectedLabel: "Selected: 1",
                showAll: "Show all", collapse: "Collapse",
                choices: [
                    IosDiscoveryFilterChoice(id: "espresso", title: "Espresso", selected: true, photoUrl: nil),
                    IosDiscoveryFilterChoice(id: "batch", title: "Filter coffee", selected: false, photoUrl: nil)
                ]
            )
        ]
    )
}

#Preview("Discovery filters - Light") {
    DiscoveryFilterSheet(state: discoveryFilterPreview, actions: DiscoveryFilterActions())
        .preferredColorScheme(.light)
}

#Preview("Discovery filters - Dark and Large Type") {
    DiscoveryFilterSheet(state: discoveryFilterPreview, actions: DiscoveryFilterActions())
        .preferredColorScheme(.dark)
        .environment(\.dynamicTypeSize, .accessibility2)
}

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
                reset: filters.reset, close: filters.apply,
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
                closeButton
            }
            VStack(spacing: 4) {
                title
                HStack {
                    resetButton
                    Spacer(minLength: 0)
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
                if state.isLoading {
                    CoffeePeekNativeLoader(label: status, size: 22).accessibilityHidden(true)
                }
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
        CoffeePeekNativePriceSlider(title: state.priceTitle, prices: state.prices,
            selectedIndex: Int(state.priceIndex), select: { actions.price(Int32($0)) })
    }
}

struct CoffeePeekNativePriceSlider: View {
    let title: String
    let prices: [String]
    let selectedIndex: Int
    var showsValue = true
    let select: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ViewThatFits(in: .horizontal) {
                HStack {
                    Text(title).font(.headline).accessibilityAddTraits(.isHeader)
                    Spacer()
                    if showsValue { Text(selectedPrice).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(title).font(.headline).accessibilityAddTraits(.isHeader)
                    if showsValue { Text(selectedPrice).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                }
            }
            if prices.count > 1 {
                Slider(value: Binding(get: { Double(min(max(0, selectedIndex), prices.count - 1)) }, set: { select(Int($0.rounded())) }),
                       in: 0...Double(prices.count - 1), step: 1) {
                    Text(title)
                }
                .tint(CoffeePeekNativeStyle.accent)
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
        prices.indices.contains(selectedIndex) ? prices[selectedIndex] : ""
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
            ForEach(Array(choices.enumerated()), id: \.element.id) { index, choice in
                if index > 0 { Divider().overlay(CoffeePeekNativeStyle.border) }
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
        CoffeePeekNativeChoiceRow(title: choice.title, selected: choice.selected,
            action: { actions.toggle(section.id, choice.id) }) { choiceIcon(choice) }
    }

    @ViewBuilder private func choiceIcon(_ choice: IosDiscoveryFilterChoice) -> some View {
        if section.id == "Roasters" {
            DiscoveryFilterLogo(url: choice.photoUrl, load: actions.image)
                .id(choice.photoUrl).accessibilityHidden(true)
        } else if section.id == "BrewMethods" || section.id == "Tags" {
            Group {
                if let icon = choice.icon { Image(uiImage: icon).renderingMode(.template).resizable().scaledToFit() }
                else { Color.clear }
            }.frame(width: 22, height: 22).frame(width: 28, height: 28)
                .foregroundStyle(CoffeePeekNativeStyle.secondary).accessibilityHidden(true)
        }
    }

}

struct CoffeePeekNativeChoiceRow<Icon: View>: View {
    let title: String
    let selected: Bool
    let action: () -> Void
    @ViewBuilder let icon: Icon

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                icon
                Text(title).frame(maxWidth: .infinity, alignment: .leading)
                if selected {
                    Image(systemName: "checkmark").font(.system(size: 20))
                        .foregroundStyle(CoffeePeekNativeStyle.accent).accessibilityHidden(true)
                }
            }.foregroundStyle(CoffeePeekNativeStyle.foreground)
                .padding(.vertical, 12).frame(minHeight: 48).contentShape(Rectangle())
        }.buttonStyle(.plain).accessibilityAddTraits(selected ? .isSelected : [])
    }
}

@MainActor
private func nativeFilterControlsPreview() -> some View {
    VStack(spacing: 16) {
        CoffeePeekNativePriceSlider(title: "Цена", prices: ["Любая", "< 8 BYN", "8 BYN", "> 8 BYN"],
            selectedIndex: 2, select: { _ in })
        CoffeePeekNativeChoiceRow(title: "Обжарщик", selected: true, action: {}) {
            Image(systemName: "cup.and.saucer").frame(width: 28, height: 28)
        }
    }.padding().background(CoffeePeekNativeStyle.surface)
}
#Preview("Native filter controls light") { nativeFilterControlsPreview().preferredColorScheme(.light) }
#Preview("Native filter controls dark") { nativeFilterControlsPreview().preferredColorScheme(.dark) }

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
                    IosDiscoveryFilterChoice(id: "espresso", title: "Espresso", selected: true, photoUrl: nil, icon: nil),
                    IosDiscoveryFilterChoice(id: "batch", title: "Filter coffee", selected: false, photoUrl: nil, icon: nil)
                ]
            ),
            IosDiscoveryFilterSection(
                id: "Tags", title: "Особенности", selectedLabel: "Выбрано: 1", showAll: "Показать все (7)", collapse: "Свернуть",
                choices: [
                    IosDiscoveryFilterChoice(id: "roastery", title: "Roastery", selected: false, photoUrl: nil, icon: UIImage(systemName: "building.2")),
                    IosDiscoveryFilterChoice(id: "beans-for-sale", title: "Зерно на продажу", selected: false, photoUrl: nil, icon: UIImage(systemName: "bag")),
                    IosDiscoveryFilterChoice(id: "decaf", title: "Decaf", selected: false, photoUrl: nil, icon: UIImage(systemName: "cup.and.saucer")),
                    IosDiscoveryFilterChoice(id: "to-go", title: "To Go", selected: false, photoUrl: nil, icon: UIImage(systemName: "figure.walk")),
                    IosDiscoveryFilterChoice(id: "plant-based-milk", title: "Растительное молоко", selected: false, photoUrl: nil, icon: UIImage(systemName: "leaf")),
                    IosDiscoveryFilterChoice(id: "pet-friendly", title: "Можно с питомцами", selected: true, photoUrl: nil, icon: UIImage(systemName: "pawprint")),
                    IosDiscoveryFilterChoice(id: "outside-seating", title: "Терраса", selected: false, photoUrl: nil, icon: UIImage(systemName: "sun.max"))
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

#Preview("Discovery filters - Dark") {
    DiscoveryFilterSheet(state: discoveryFilterPreview, actions: DiscoveryFilterActions())
        .preferredColorScheme(.dark)
}

#Preview("Discovery filters - Light and Large Type") {
    DiscoveryFilterSheet(state: discoveryFilterPreview, actions: DiscoveryFilterActions())
        .preferredColorScheme(.light)
        .environment(\.dynamicTypeSize, .accessibility2)
}

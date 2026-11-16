import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCoffeeDetailController(detail: IosCoffeeDetail, languageTag: String) -> UIViewController {
    CoffeeDetailHostingController(detail: detail, strings: CoffeeDetailStrings(languageTag: languageTag))
}

private final class CoffeeDetailHostingController: UIHostingController<CoffeeDetailHost> {
    private let detail: IosCoffeeDetail

    init(detail: IosCoffeeDetail, strings: CoffeeDetailStrings) {
        self.detail = detail
        super.init(rootView: CoffeeDetailHost(detail: detail, strings: strings))
    }

    @available(*, unavailable)
    required dynamic init?(coder: NSCoder) {
        fatalError("Coffee detail requires its Kotlin destination.")
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        detail.visible(host: self)
    }

    override func viewWillDisappear(_ animated: Bool) {
        detail.hidden()
        super.viewWillDisappear(animated)
    }
}

private struct CoffeeDetailHost: View {
    @StateObject private var model: CoffeeDetailModel
    let detail: IosCoffeeDetail
    let strings: CoffeeDetailStrings

    init(detail: IosCoffeeDetail, strings: CoffeeDetailStrings) {
        self.detail = detail
        self.strings = strings
        _model = StateObject(wrappedValue: CoffeeDetailModel(detail: detail))
    }

    var body: some View {
        CoffeeDetailScreen(
            state: model.state,
            images: detail,
            strings: strings,
            onAction: model.send,
            onExposure: { section, fraction in detail.updateExposure(section: section.rawValue, fraction: fraction) }
        )
        .allowsHitTesting(model.viewer == nil)
        .accessibilityHidden(model.viewer != nil)
        .overlay {
            if let viewer = model.viewer {
                CoffeeDetailViewer(controller: viewer)
                    .id(ObjectIdentifier(viewer))
                    .ignoresSafeArea()
            }
        }
        .onAppear { model.observe() }
        .onDisappear { model.stopObserving() }
    }
}

private final class CoffeeDetailModel: ObservableObject {
    @Published private(set) var state: IosCoffeeDetailState
    @Published private(set) var viewer: UIViewController?
    private let detail: IosCoffeeDetail
    private var observation: IosCoffeeDetailObservation?

    init(detail: IosCoffeeDetail) {
        self.detail = detail
        state = detail.currentState()
    }

    deinit {
        observation?.cancel()
        detail.close()
    }

    func observe() {
        guard observation == nil else { return }
        updateState(detail.currentState())
        observation = detail.observe { [weak self] in self?.updateState($0) }
    }

    func stopObserving() {
        observation?.cancel()
        observation = nil
    }

    func send(_ action: CoffeeDetailAction) {
        switch action {
        case .retry: detail.retry()
        case .retrySimilar: detail.retrySimilar()
        case .back: detail.back()
        case .photos: detail.openPhotos()
        case .description: detail.toggleDescription()
        case .roaster: detail.openRoaster()
        case .similar(let slug): detail.openSimilar(slug: slug)
        case .seller(let index): detail.openSeller(index: Int32(index))
        case .share: detail.share()
        }
    }

    private func updateState(_ next: IosCoffeeDetailState) {
        state = next
        if !next.arePhotosOpen {
            viewer = nil
        } else if viewer == nil {
            viewer = detail.makeViewer()
        }
    }
}

private enum CoffeeDetailAction {
    case retry, retrySimilar, back, photos, description, roaster, share
    case similar(String)
    case seller(Int)
}

private struct CoffeeDetailScreen: View {
    let state: IosCoffeeDetailState
    let images: IosCoffeeDetail?
    let strings: CoffeeDetailStrings
    let onAction: (CoffeeDetailAction) -> Void
    let onExposure: (CoffeeDetailSection, Float) -> Void

    var body: some View {
        Group {
            if state.isLoading {
                ProgressView(strings.text("coffee_loading"))
            } else if state.loadFailed {
                loadError
            } else if let product = state.product {
                productContent(product)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(uiColor: .systemBackground))
        .safeAreaInset(edge: .top, spacing: 0) { header }
    }

    private var header: some View {
        HStack(spacing: 8) {
            headerButton("coffee_back", icon: "chevron.backward", action: .back)
            Spacer()
            headerButton("coffee_share", icon: "square.and.arrow.up", action: .share)
                .disabled(state.product == nil)
        }
        .padding(.horizontal, 12)
        .background(.bar)
    }

    private func headerButton(_ key: String, icon: String, action: CoffeeDetailAction) -> some View {
        Button { onAction(action) } label: {
            Label(strings.text(key), systemImage: icon)
                .labelStyle(.iconOnly)
                .frame(minWidth: 44, minHeight: 44)
                .contentShape(Rectangle())
        }
    }

    private var loadError: some View {
        VStack(spacing: 12) {
            Text(strings.text("coffee_load_failed"))
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            Button(strings.text("coffee_retry")) { onAction(.retry) }
                .buttonStyle(.borderedProminent)
        }
        .padding(16)
    }

    private func productContent(_ product: IosCoffeeProduct) -> some View {
        GeometryReader { viewport in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 24) {
                    hero(product)
                    introduction(product).padding(.horizontal, 16)
                    tastes(product).padding(.horizontal, 16)
                    about(product).padding(.horizontal, 16)
                    offers(product).padding(.horizontal, 16)
                    similar.padding(.horizontal, 16)
                }
                .padding(.bottom, 24)
            }
            .coordinateSpace(name: coffeeDetailScrollSpace)
            .onPreferenceChange(CoffeeDetailFrames.self) { frames in
                for section in CoffeeDetailSection.allCases {
                    onExposure(section, coffeeDetailFraction(frames[section], in: viewport.size))
                }
            }
        }
    }

    @ViewBuilder
    private func hero(_ product: IosCoffeeProduct) -> some View {
        let availablePhoto = product.photo?.isEmpty == false
        Button { onAction(.photos) } label: {
            CoffeeDetailImage(url: product.photo, images: images)
                .aspectRatio(1, contentMode: .fit)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!availablePhoto)
        .accessibilityLabel(strings.text(availablePhoto ? "coffee_open_photos" : "coffee_photo_missing"))
        .coffeeDetailSection(availablePhoto ? .photos : nil)
    }

    private func introduction(_ product: IosCoffeeProduct) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(product.name)
                .font(.largeTitle.bold())
                .accessibilityAddTraits(.isHeader)
            if !product.countries.isEmpty { Text(product.countries).foregroundStyle(.secondary) }
            let type = [strings.optional("coffee_kind_", product.productKind),
                        strings.optional("coffee_form_", product.productForm)].compactMap { $0 }.joined(separator: " · ")
            if !type.isEmpty { Text(type).font(.subheadline).foregroundStyle(.secondary) }
            description(product.description)
            if let roast = product.roast { CoffeeDetailField(title: strings.text("coffee_roast"), value: roast) }
            if !product.profile.isEmpty { CoffeeDetailField(title: strings.text("coffee_profile"), value: product.profile) }
            if let acidity = product.acidity { CoffeeDetailField(title: strings.text("coffee_acidity"), value: acidity) }
        }
    }

    @ViewBuilder
    private func description(_ description: String?) -> some View {
        if let description {
            CoffeeDetailDescription(text: description, expanded: state.isDescriptionExpanded,
                                    strings: strings) { onAction(.description) }
        }
    }

    @ViewBuilder
    private func tastes(_ product: IosCoffeeProduct) -> some View {
        if !product.tasteDescriptors.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                sectionTitle("coffee_taste")
                Text(product.tasteDescriptors.joined(separator: " · ")).foregroundStyle(.secondary)
            }
        }
    }

    private func about(_ product: IosCoffeeProduct) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("coffee_about")
            CoffeeDetailField(title: strings.text("coffee_country"),
                              value: product.countries.isEmpty ? strings.text("coffee_unspecified_feminine") : product.countries)
            CoffeeDetailField(title: strings.text("coffee_form"),
                              value: strings.optional("coffee_form_", product.productForm) ?? strings.text("coffee_unspecified_feminine"))
            Text(strings.text("coffee_roaster"))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .coffeeDetailSection(.roaster)
            roaster(product)
        }
    }

    private func roaster(_ product: IosCoffeeProduct) -> some View {
        Button { onAction(.roaster) } label: {
            HStack(spacing: 10) {
                CoffeeDetailImage(url: product.roasterPhoto, images: images)
                    .frame(width: 44, height: 44)
                    .clipShape(Circle())
                Text(product.roasterName).foregroundStyle(.primary)
                if product.canOpenRoaster {
                    Spacer()
                    Image(systemName: "chevron.forward").foregroundStyle(.secondary)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!product.canOpenRoaster)
    }

    private func offers(_ product: IosCoffeeProduct) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                sectionTitle("coffee_offers")
                if let checkedAt = product.checkedAt {
                    Text(strings.format("coffee_checked_at", checkedAt)).font(.footnote).foregroundStyle(.secondary)
                }
                if product.offers.isEmpty {
                    Text(strings.text("coffee_no_offers")).foregroundStyle(.secondary)
                }
            }
            .coffeeDetailSection(.offers)
            ForEach(product.offers.indices, id: \.self) { index in
                CoffeeDetailOfferCard(offer: product.offers[index], strings: strings) { onAction(.seller(index)) }
            }
        }
    }

    @ViewBuilder
    private var similar: some View {
        if state.similarLoading || state.similarFailed || !state.similar.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                sectionTitle("coffee_similar").coffeeDetailSection(.similarCoffees)
                if state.similarLoading {
                    ProgressView(strings.text("coffee_loading"))
                } else if state.similarFailed {
                    Button(strings.text("coffee_retry_similar")) { onAction(.retrySimilar) }
                } else {
                    ScrollView(.horizontal) {
                        LazyHStack(alignment: .top, spacing: 12) {
                            ForEach(state.similar, id: \.slug) { coffee in
                                CoffeeSimilarCard(coffee: coffee, images: images, strings: strings) {
                                    onAction(.similar(coffee.slug))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private func sectionTitle(_ key: String) -> some View {
        Text(strings.text(key))
            .font(.title2.bold())
            .accessibilityAddTraits(.isHeader)
    }
}

private struct CoffeeDetailViewer: UIViewControllerRepresentable {
    let controller: UIViewController
    func makeUIViewController(context: Context) -> UIViewController { controller }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private func coffeeDetailPreview(languageTag: String, failed: Bool) -> some View {
    let product = IosCoffeeProduct(
        name: "Эфиопия", photo: nil, countries: "Эфиопия", productKind: "roasted_beans",
        productForm: "whole_beans", description: "Сладкий кофе с ягодным послевкусием.",
        roast: nil, acidity: nil, profile: "", tasteDescriptors: ["Ягоды"],
        roasterName: "CoffeePeek", roasterPhoto: nil, canOpenRoaster: false, checkedAt: nil, offers: []
    )
    return CoffeeDetailScreen(
        state: IosCoffeeDetailState(product: failed ? nil : product, isLoading: false, loadFailed: failed,
                                   arePhotosOpen: false, isDescriptionExpanded: false, similar: [],
                                   similarLoading: false, similarFailed: false),
        images: nil, strings: CoffeeDetailStrings(languageTag: languageTag), onAction: { _ in }, onExposure: { _, _ in }
    )
}

#Preview("Light") { coffeeDetailPreview(languageTag: "ru", failed: false).preferredColorScheme(.light) }
#Preview("Dark") { coffeeDetailPreview(languageTag: "be", failed: true).preferredColorScheme(.dark) }

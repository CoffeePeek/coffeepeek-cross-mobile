import ComposeApp
import SwiftUI
import UIKit

struct RoasterDetailNativeHost: View {
    let source: IosRoasterDetail
    var topInset: CGFloat = 0
    var bottomInset: CGFloat = 0
    @StateObject private var model: RoasterDetailNativeModel

    init(source: IosRoasterDetail) {
        self.source = source
        _model = StateObject(wrappedValue: RoasterDetailNativeModel(source))
    }
    var body: some View {
        RoasterDetailNativeScreen(state: model.state, strings: model.strings, source: source,
            ownership: model.ownership, loader: model.loader, topInset: topInset, bottomInset: bottomInset)
            .coffeePeekPhotoViewer(model.viewer, languageTag: model.strings.languageTag, onDismiss: source.closePhoto)
            .onAppear(perform: model.start).onDisappear(perform: model.stop)
    }
}

@MainActor
private final class RoasterDetailNativeModel: ObservableObject {
    @Published var state: IosRoasterDetailState
    @Published var strings: OwnProfileStrings
    @Published var viewer: IosPhotoViewer?
    @Published var ownership: UIViewController?
    let source: IosRoasterDetail
    private var observations: [IosRoasterObservation] = []

    init(_ source: IosRoasterDetail) {
        self.source = source
        state = source.current()
        strings = OwnProfileStrings(languageTag: source.languageTag(), table: "NativeRoaster")
    }
    var loader: CoffeePeekCardImageLoader {
        { [weak self] url, receive in
            guard let source = self?.source else { return nil }
            let observation = source.loadImage(url: url, receive: receive)
            return CoffeePeekCardImageRequest(cancel: observation.cancel)
        }
    }
    func start() {
        guard observations.isEmpty else { return }
        update(source.current())
        observations = [source.observeState { [weak self] in self?.update($0) }, source.observeEvents(),
            source.observeLanguage { [weak self] in self?.strings = OwnProfileStrings(languageTag: $0, table: "NativeRoaster") }]
    }
    func stop() {
        guard viewer == nil else { return }
        observations.forEach { $0.cancel() }
        observations = []
    }
    private func update(_ next: IosRoasterDetailState) {
        if next.photo != state.photo { viewer = next.photo == nil ? nil : source.makePhotoViewer() }
        if next.id != state.id || (next.id != nil && ownership == nil) { ownership = source.makeOwnershipActions() }
        if !next.active { viewer = nil; ownership = nil }
        state = next
    }
    isolated deinit { observations.forEach { $0.cancel() } }
}

struct RoasterDetailNativeScreen: View {
    let state: IosRoasterDetailState
    let strings: OwnProfileStrings
    let source: IosRoasterDetail?
    let ownership: UIViewController?
    let loader: CoffeePeekCardImageLoader
    var topInset: CGFloat = 0
    var bottomInset: CGFloat = 0
    @State private var selectedPhoto = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        ZStack {
            if state.active {
                if let name = state.name {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) { content(name) }
                            .padding(.bottom, bottomInset + 24)
                            .coffeePeekRefreshable(isRefreshing: state.loading, label: strings["roaster_loading"]) { source?.retry() }
                    }.transition(.opacity)
                } else if state.loading {
                    CoffeePeekNativeLoader(label: strings["roaster_loading"]).transition(.opacity)
                } else { failure.transition(.opacity) }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .overlay(alignment: .top) { if state.name == nil { toolbar } }
        .overlay(alignment: .bottom) {
            if let message = notice {
                ShopDetailToastView(message: message, dismissLabel: strings["roaster_message_dismiss"], dismiss: { source?.dismissMessage() })
                    .padding(.bottom, bottomInset + 12)
            }
        }
        .font(CoffeePeekNativeStyle.font(17)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea()).tint(CoffeePeekNativeStyle.accent)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.22), value: state.name != nil)
        .onChange(of: state.photos) { _, next in selectedPhoto = min(selectedPhoto, max(0, next.count - 1)) }
    }

    private var toolbar: some View {
        HStack(spacing: 8) {
            Button { source?.back() } label: { Image(systemName: "chevron.left").frame(width: 48, height: 48) }
                .shopDetailGlassCircle()
                .accessibilityLabel(strings["roaster_detail_back"])
            Spacer()
            if state.id != nil && state.active {
                Button { source?.favorite() } label: {
                    CoffeePeekLikeIcon(selected: state.favorite).frame(width: 48, height: 48)
                }
                .shopDetailGlassCircle()
                .accessibilityLabel(strings[state.favorite ? "roaster_favorite_remove" : "roaster_favorite_add"])
                .roasterExposure("favorite:detail", source: source)
                Button { source?.share() } label: { Image(systemName: "square.and.arrow.up").frame(width: 48, height: 48) }
                    .shopDetailGlassCircle()
                    .accessibilityLabel(strings["roaster_share"])
                if let ownership { OwnProfileEmbedded(controller: ownership, drawsBackground: false).frame(width: 48, height: 48) }
            }
        }.font(.system(size: 20)).buttonStyle(.plain).padding(.horizontal, 12).padding(.top, topInset + 12)
    }

    @ViewBuilder private func content(_ name: String) -> some View {
        photos(name)
        Text(name).font(CoffeePeekNativeStyle.font(34, weight: "Bold", relativeTo: .largeTitle))
            .accessibilityAddTraits(.isHeader).padding(.horizontal, 16)
        if state.failed { failure }
        if let about = state.about, !about.isEmpty {
            section(strings["roaster_about_title"]) {
                Text(about).foregroundStyle(CoffeePeekNativeStyle.secondary).textSelection(.enabled)
            }
        }
        if let address = state.address, !address.isEmpty {
            section(strings["roaster_address_title"]) {
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: "mappin.and.ellipse").foregroundStyle(CoffeePeekNativeStyle.accent).accessibilityHidden(true)
                    Text(address).textSelection(.enabled)
                }
            }
        }
        if state.instagram != nil || state.website != nil { contacts }
        shops
    }

    private func photos(_ name: String) -> some View {
        Group {
            if state.photos.isEmpty {
                CoffeePeekPhotoPlaceholder().frame(height: 280 + topInset)
                    .accessibilityLabel(String(format: strings["roaster_photos_missing"], name))
            } else {
                TabView(selection: $selectedPhoto) {
                    ForEach(Array(state.photos.enumerated()), id: \.offset) { index, url in
                        Button { source?.photo(url: url) } label: {
                            CoffeePeekCardImage(url: url, symbol: nil, loadImage: loader)
                        }
                        .buttonStyle(.plain).tag(index)
                        .accessibilityLabel(name + ", " + String(format: strings["roaster_photo_position"], index + 1, state.photos.count))
                    }
                }.tabViewStyle(.page(indexDisplayMode: .never)).frame(height: 280 + topInset)
                    .overlay(alignment: .bottomTrailing) {
                        if state.photos.count > 1 {
                            Text(String(format: strings["roaster_photo_position"], selectedPhoto + 1, state.photos.count))
                                .font(CoffeePeekNativeStyle.font(15, weight: "Medium", relativeTo: .subheadline))
                                .foregroundStyle(.white).padding(.horizontal, 12).padding(.vertical, 4)
                                .background(.black.opacity(0.55), in: Capsule()).padding(12)
                        }
                    }
            }
        }.clipShape(UnevenRoundedRectangle(bottomLeadingRadius: 16, bottomTrailingRadius: 16))
            .overlay(alignment: .top) { toolbar }.roasterExposure("photos", source: state.photos.isEmpty ? nil : source)
    }

    private var contacts: some View {
        section(strings["roaster_links_title"]) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 12) { contactItems }
                VStack(alignment: .leading, spacing: 12) { contactItems }
            }
        }.roasterExposure("contacts", source: source)
    }

    @ViewBuilder private var contactItems: some View {
        if let instagram = state.instagram {
            CoffeePeekNativeContactButton(title: instagram, kind: "instagram") { source?.instagram() }
        }
        if let website = state.website {
            CoffeePeekNativeContactButton(title: website, kind: "website") { source?.website() }
        }
    }

    private var shops: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings["roaster_shops_title"]).font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
                .accessibilityAddTraits(.isHeader).roasterExposure("shops", source: source)
            if state.shops.isEmpty { Text(strings["roaster_shops_empty"]).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            ForEach(state.shops, id: \.slug) { shop in
                Button { source?.shop(slug: shop.slug) } label: {
                    HStack(spacing: 12) {
                        CoffeePeekCardImage(url: shop.photo, symbol: nil, loadImage: loader)
                            .frame(width: 72, height: 72).clipShape(RoundedRectangle(cornerRadius: 12))
                        Text(shop.name).frame(maxWidth: .infinity, alignment: .leading)
                            .font(CoffeePeekNativeStyle.font(17, weight: "SemiBold", relativeTo: .headline))
                            .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 2)
                        Image(systemName: "chevron.right").foregroundStyle(CoffeePeekNativeStyle.secondary).accessibilityHidden(true)
                    }.padding(8).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
                        .overlay { RoundedRectangle(cornerRadius: 16).strokeBorder(CoffeePeekNativeStyle.border).allowsHitTesting(false) }
                        .contentShape(RoundedRectangle(cornerRadius: 16))
                }.buttonStyle(.plain).accessibilityLabel(String(format: strings["roaster_open_shop"], shop.name))
            }
        }.padding(.horizontal, 16)
    }

    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(CoffeePeekNativeStyle.font(17, weight: "Semibold", relativeTo: .headline)).accessibilityAddTraits(.isHeader)
            content()
        }.padding(.horizontal, 16)
    }

    private var failure: some View {
        VStack(spacing: 12) {
            Text(strings["roaster_detail_load_failed"])
            CoffeePeekNativeActionButton(title: strings["roaster_detail_retry"]) { source?.retry() }
            CoffeePeekNativeActionButton(title: strings["roaster_detail_back"], primary: false) { source?.back() }
        }.frame(maxWidth: .infinity).padding()
    }

    private var notice: String? {
        guard state.active else { return nil }
        return state.message ?? state.messageKey.map { strings[$0] }
    }
}

@MainActor
private func roasterDetailPreview(_ language: String, stage: String = "content") -> some View {
    RoasterDetailNativeScreen(state: IosRoasterDetailState(active: true, loading: stage == "loading", failed: stage == "failed", id: "fixture",
        name: stage == "content" ? "CoffeePeek Roasters" : nil, about: "Обжариваем кофе каждый день. Эфиопия, Колумбия и сезонные лоты.",
        address: "Sandbox street, 1", instagram: "@coffeepeek", website: "coffeepeek.by", photos: ["preview-1", "preview-2"],
        shops: [IosRoasterShop(slug: "preview", name: "Кофейня с длинным названием у парка", photo: nil)],
        favorite: true, photo: nil, messageKey: nil, message: nil),
        strings: OwnProfileStrings(languageTag: language, table: "NativeRoaster"), source: nil, ownership: nil,
        loader: { _, receive in receive(nil); return nil })
}
#Preview("Roaster loading light") { roasterDetailPreview("ru", stage: "loading").preferredColorScheme(.light) }
#Preview("Roaster loading dark") { roasterDetailPreview("be", stage: "loading").preferredColorScheme(.dark) }
#Preview("Roaster error light") { roasterDetailPreview("ru", stage: "failed").preferredColorScheme(.light) }
#Preview("Roaster error dark") { roasterDetailPreview("be", stage: "failed").preferredColorScheme(.dark) }
#Preview("Roaster detail light") { roasterDetailPreview("ru").preferredColorScheme(.light) }
#Preview("Roaster detail dark") { roasterDetailPreview("be").preferredColorScheme(.dark) }

#Preview("Roaster detail accessibility light") {
    roasterDetailPreview("ru").dynamicTypeSize(.accessibility2).preferredColorScheme(.light)
}
#Preview("Roaster detail accessibility dark") {
    roasterDetailPreview("be").dynamicTypeSize(.accessibility2).preferredColorScheme(.dark)
}

import ComposeApp
import SwiftUI
import UIKit

struct RoasterDetailNativeHost: View {
    let source: IosRoasterDetail
    @StateObject private var model: RoasterDetailNativeModel

    init(source: IosRoasterDetail) {
        self.source = source
        _model = StateObject(wrappedValue: RoasterDetailNativeModel(source))
    }
    var body: some View {
        RoasterDetailNativeScreen(state: model.state, strings: model.strings, source: source,
            ownership: model.ownership, loader: model.loader)
            .coffeePeekPhotoViewer(model.viewer, languageTag: model.strings.languageTag, onDismiss: source.closePhoto)
            .onAppear(perform: model.start).onDisappear(perform: model.stop)
    }
}

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
    deinit { observations.forEach { $0.cancel() } }
}

struct RoasterDetailNativeScreen: View {
    let state: IosRoasterDetailState
    let strings: OwnProfileStrings
    let source: IosRoasterDetail?
    let ownership: UIViewController?
    let loader: CoffeePeekCardImageLoader
    @State private var selectedPhoto = 0

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if !state.active { Color.clear }
                else if let name = state.name { content(name) }
                else if state.loading { ProgressView(strings["roaster_loading"]).frame(maxWidth: .infinity, minHeight: 180) }
                else { failure }
            }.padding(.bottom, 24)
        }
        .refreshable { source?.retry() }
        .overlay(alignment: .top) { toolbar }
        .font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea()).tint(CoffeePeekNativeStyle.accent)
        .onChange(of: state.photos) { _, next in selectedPhoto = min(selectedPhoto, max(0, next.count - 1)) }
        .alert(state.message ?? state.messageKey.map { strings[$0] } ?? "", isPresented: messagePresented) {
            Button(strings["roaster_message_dismiss"]) { source?.dismissMessage() }
        }
    }

    private var toolbar: some View {
        HStack(spacing: 4) {
            Button { source?.back() } label: { Image(systemName: "chevron.left").frame(width: 48, height: 48) }
                .background(.regularMaterial, in: Circle())
                .accessibilityLabel(strings["roaster_detail_back"])
            Spacer()
            if state.id != nil && state.active {
                if let ownership { OwnProfileEmbedded(controller: ownership).frame(width: 48, height: 48) }
                Button { source?.favorite() } label: {
                    Image(systemName: state.favorite ? "heart.fill" : "heart").frame(width: 48, height: 48)
                        .foregroundStyle(state.favorite ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.foreground)
                }
                .background(.regularMaterial, in: Circle())
                .accessibilityLabel(strings[state.favorite ? "roaster_favorite_remove" : "roaster_favorite_add"])
                .roasterExposure("favorite:detail", source: source)
                Button { source?.share() } label: { Image(systemName: "square.and.arrow.up").frame(width: 48, height: 48) }
                    .background(.regularMaterial, in: Circle())
                    .accessibilityLabel(strings["roaster_share"])
            }
        }.buttonStyle(.plain).padding(.horizontal, 8).padding(.top, 8)
    }

    @ViewBuilder private func content(_ name: String) -> some View {
        photos(name)
        Text(name).font(CoffeePeekNativeStyle.font(28, weight: "Bold", relativeTo: .title))
            .accessibilityAddTraits(.isHeader).padding(.horizontal, 16)
        if state.failed { failure }
        if let about = state.about, !about.isEmpty {
            section(strings["roaster_about_title"]) { Text(about).textSelection(.enabled) }
        }
        if let address = state.address, !address.isEmpty {
            section(strings["roaster_address_title"]) { Text(address).textSelection(.enabled) }
        }
        if state.instagram != nil || state.website != nil { contacts }
        shops
    }

    private func photos(_ name: String) -> some View {
        Group {
            if state.photos.isEmpty {
                CoffeePeekPhotoPlaceholder().frame(height: 280)
                    .accessibilityLabel(String(format: strings["roaster_photos_missing"], name))
            } else {
                TabView(selection: $selectedPhoto) {
                    ForEach(Array(state.photos.enumerated()), id: \.offset) { index, url in
                        Button { source?.photo(url: url) } label: {
                            CoffeePeekCardImage(url: url, symbol: nil, loadImage: loader)
                        }
                        .buttonStyle(.plain).tag(index)
                        .accessibilityLabel(String(format: strings["roaster_photo_position"], index + 1, state.photos.count))
                    }
                }.tabViewStyle(.page(indexDisplayMode: .never)).frame(height: 280)
                    .overlay(alignment: .bottomTrailing) {
                        if state.photos.count > 1 {
                            Text(String(format: strings["roaster_photo_position"], selectedPhoto + 1, state.photos.count))
                                .font(.caption).foregroundStyle(.white).padding(8)
                                .background(.black.opacity(0.55), in: Capsule()).padding(12)
                        }
                    }
            }
        }.roasterExposure("photos", source: source)
    }

    private var contacts: some View {
        section(strings["roaster_links_title"]) {
            if let instagram = state.instagram {
                Button { source?.instagram() } label: { Label(instagram, systemImage: "camera").frame(minHeight: 44) }
            }
            if let website = state.website {
                Button { source?.website() } label: { Label(website, systemImage: "globe").frame(minHeight: 44) }
            }
        }.roasterExposure("contacts", source: source)
    }

    private var shops: some View {
        section(strings["roaster_shops_title"]) {
            if state.shops.isEmpty { Text(strings["roaster_shops_empty"]).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            ForEach(state.shops, id: \.slug) { shop in
                Button { source?.shop(slug: shop.slug) } label: {
                    HStack(spacing: 12) {
                        CoffeePeekCardImage(url: shop.photo, symbol: nil, loadImage: loader)
                            .frame(width: 64, height: 64).clipShape(RoundedRectangle(cornerRadius: 12))
                        Text(shop.name).frame(maxWidth: .infinity, alignment: .leading)
                        Image(systemName: "chevron.right").accessibilityHidden(true)
                    }.padding(12).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
                }.buttonStyle(.plain).accessibilityLabel(String(format: strings["roaster_open_shop"], shop.name))
            }
        }.roasterExposure("shops", source: source)
    }

    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(CoffeePeekNativeStyle.font(20, weight: "Semibold", relativeTo: .headline)).accessibilityAddTraits(.isHeader)
            content()
        }.padding(.horizontal, 16)
    }

    private var failure: some View {
        VStack(spacing: 12) {
            Text(strings["roaster_detail_load_failed"])
            Button(strings["roaster_detail_retry"]) { source?.retry() }.frame(minHeight: 44)
        }.frame(maxWidth: .infinity).padding()
    }
    private var messagePresented: Binding<Bool> {
        Binding(get: { state.active && (state.message != nil || state.messageKey != nil) }, set: { if !$0 { source?.dismissMessage() } })
    }
}

private func roasterDetailPreview(_ language: String) -> some View {
    RoasterDetailNativeScreen(state: IosRoasterDetailState(active: true, loading: false, failed: false, id: "fixture",
        name: "CoffeePeek", about: "Кофе каждый день", address: nil, instagram: nil, website: nil, photos: [],
        shops: [], favorite: true, photo: nil, messageKey: nil, message: nil),
        strings: OwnProfileStrings(languageTag: language, table: "NativeRoaster"), source: nil, ownership: nil,
        loader: { _, receive in receive(nil); return nil })
}
#Preview("Roaster detail light") { roasterDetailPreview("ru").preferredColorScheme(.light) }
#Preview("Roaster detail dark") { roasterDetailPreview("be").preferredColorScheme(.dark) }

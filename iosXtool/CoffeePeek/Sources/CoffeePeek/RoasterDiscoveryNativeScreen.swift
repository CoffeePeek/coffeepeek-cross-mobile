import ComposeApp
import SwiftUI

struct RoasterDiscoveryNativeHost: View {
    let source: IosRoasterDiscovery
    @StateObject private var model: RoasterDiscoveryNativeModel

    init(source: IosRoasterDiscovery) {
        self.source = source
        _model = StateObject(wrappedValue: RoasterDiscoveryNativeModel(source))
    }
    var body: some View {
        RoasterDiscoveryNativeScreen(state: model.state, search: source.search, strings: model.strings,
            source: source, loader: model.loader)
            .onAppear(perform: model.start).onDisappear(perform: model.stop)
    }
}

@MainActor
private final class RoasterDiscoveryNativeModel: ObservableObject {
    @Published var state: IosRoasterDiscoveryState
    @Published var strings: OwnProfileStrings
    let source: IosRoasterDiscovery
    private var observations: [IosRoasterObservation] = []

    init(_ source: IosRoasterDiscovery) {
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
        state = source.current()
        observations = [source.observeState { [weak self] in self?.state = $0 }, source.observeEvents(),
            source.observeLanguage { [weak self] in self?.strings = OwnProfileStrings(languageTag: $0, table: "NativeRoaster") }]
    }
    func stop() { observations.forEach { $0.cancel() }; observations = [] }
    isolated deinit { observations.forEach { $0.cancel() } }
}

struct RoasterDiscoveryNativeScreen: View {
    let state: IosRoasterDiscoveryState
    let search: Bool
    let strings: OwnProfileStrings
    let source: IosRoasterDiscovery?
    let loader: CoffeePeekCardImageLoader

    var body: some View {
        VStack(spacing: 12) {
            if state.active {
                if !state.tags.isEmpty { tags }
                if search { list } else { carousel }
                if let message = state.message ?? state.messageKey.map({ strings[$0] }) {
                    Button(message) { source?.dismissMessage() }.frame(minHeight: 44)
                }
            }
        }
        .font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background).tint(CoffeePeekNativeStyle.accent)
    }

    private var tags: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                Button(strings["roaster_tag_all"]) { source?.clearTags() }
                    .padding(12).background(CoffeePeekNativeStyle.surface, in: Capsule())
                ForEach(state.tags, id: \.id) { tag in
                    Button(tag.name) { source?.tag(id: tag.id) }
                        .padding(12).background(tag.selected ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.surface, in: Capsule())
                        .foregroundStyle(tag.selected ? .black : CoffeePeekNativeStyle.foreground)
                        .accessibilityAddTraits(tag.selected ? .isSelected : [])
                }
            }.padding(.horizontal, 16)
        }.scrollIndicators(.hidden).fixedSize(horizontal: false, vertical: true)
    }

    private var list: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                status
                ForEach(Array(state.cards.enumerated()), id: \.offset) { index, card in
                    RoasterNativeCard(card: card, strings: strings, source: source, loader: loader, compact: true)
                        .id(card.slug ?? "unaddressed:\(index)")
                }
                suggestion
            }.padding(16).padding(.bottom, CGFloat(state.bottomClearance))
        }.refreshable { source?.retry() }.roasterExposure("catalog", source: source)
    }

    private var carousel: some View {
        ScrollView(.horizontal) {
            HStack(alignment: .top, spacing: 12) {
                status
                ForEach(Array(state.cards.enumerated()), id: \.offset) { index, card in
                    RoasterNativeCard(card: card, strings: strings, source: source, loader: loader)
                        .id(card.slug ?? "unaddressed:\(index)")
                        .containerRelativeFrame(.horizontal) { width, _ in min(360, max(240, (width - 32) * 0.88)) }
                }
                if state.cards.isEmpty && !state.loading {
                    suggestion.containerRelativeFrame(.horizontal) { width, _ in max(240, width - 32) }
                }
            }.padding(.horizontal, 16)
        }.scrollIndicators(.hidden).fixedSize(horizontal: false, vertical: true)
            .roasterExposure("catalog", source: source)
    }

    @ViewBuilder private var status: some View {
        if state.loading && state.cards.isEmpty { CoffeePeekNativeLoader(label: strings["roaster_loading"]).padding(32) }
        else if state.failed { Button(strings["roaster_detail_retry"]) { source?.retry() }.frame(minHeight: 44) }
        else if state.cards.isEmpty { Text(strings["roaster_empty"]).foregroundStyle(CoffeePeekNativeStyle.secondary).padding() }
    }
    private var suggestion: some View {
        VStack(spacing: 12) {
            Text(strings["roaster_suggestion_title"]).fontWeight(.semibold)
            Text(strings["roaster_suggestion_description"]).foregroundStyle(CoffeePeekNativeStyle.secondary)
            Button(strings["roaster_suggestion_action"]) { source?.add() }.frame(minHeight: 44)
        }.multilineTextAlignment(.center).padding()
    }
}

@MainActor
private func roasterDiscoveryPreview(_ search: Bool, language: String) -> some View {
    RoasterDiscoveryNativeScreen(state: IosRoasterDiscoveryState(active: true, loading: false, failed: false,
        query: "", bottomClearance: 0, cards: [IosRoasterCard(slug: "fixture", name: "CoffeePeek", photo: nil, about: "Кофе каждый день",
        shops: 7, products: 21, favorite: false)], tags: [], messageKey: nil, message: nil), search: search,
        strings: OwnProfileStrings(languageTag: language, table: "NativeRoaster"), source: nil,
        loader: { _, receive in receive(nil); return nil })
}
#Preview("Roaster carousel light") { roasterDiscoveryPreview(false, language: "ru").preferredColorScheme(.light) }
#Preview("Roaster carousel dark") { roasterDiscoveryPreview(false, language: "be").preferredColorScheme(.dark) }
#Preview("Roaster search light") { roasterDiscoveryPreview(true, language: "ru").preferredColorScheme(.light) }
#Preview("Roaster search dark") { roasterDiscoveryPreview(true, language: "be").preferredColorScheme(.dark) }

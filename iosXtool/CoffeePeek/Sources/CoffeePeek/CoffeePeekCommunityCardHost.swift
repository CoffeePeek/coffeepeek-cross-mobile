import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCommunityCardController(card: IosCommunityCard) -> UIViewController {
    CoffeePeekCommunityHostingController(card: card)
}

private final class CoffeePeekCommunityHostingController: UIHostingController<CoffeePeekCommunityCardHost> {
    private let model: CoffeePeekCommunityCardModel

    init(card: IosCommunityCard) {
        let model = CoffeePeekCommunityCardModel(card: card)
        self.model = model
        super.init(rootView: CoffeePeekCommunityCardHost(model: model))
        view.backgroundColor = .clear
        safeAreaRegions = []
        sizingOptions = .intrinsicContentSize
    }

    @MainActor required dynamic init?(coder: NSCoder) { return nil }
}

private struct CoffeePeekCommunityCardHost: View {
    @ObservedObject var model: CoffeePeekCommunityCardModel

    var body: some View {
        Group {
            if model.state.active {
                CoffeePeekCheckInCard(data: model.presentation, loadImage: model.loadImage,
                    perform: { model.card.perform(action: $0) },
                    openPhoto: { model.card.openPhoto(index: Int32($0)) }).id(model.state.key)
            } else { Color.clear.frame(height: 1) }
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .fixedSize(horizontal: false, vertical: true)
        .background {
            GeometryReader { geometry in
                Color.clear.preference(key: CoffeePeekCommunityCardHeight.self, value: geometry.size.height)
            }
        }
        .onPreferenceChange(CoffeePeekCommunityCardHeight.self, perform: model.heightChanged)
        .onAppear(perform: model.observe)
    }
}

private struct CoffeePeekCommunityCardHeight: PreferenceKey {
    static var defaultValue: CGFloat { 0 }
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = max(value, nextValue()) }
}

private final class CoffeePeekCommunityCardModel: ObservableObject {
    let card: IosCommunityCard
    @Published private(set) var state: IosCommunityCardState
    private var observation: IosCommunityCardObservation?
    private var reportedHeight: CGFloat = 0

    init(card: IosCommunityCard) {
        self.card = card
        state = card.current()
    }

    deinit { observation?.cancel() }

    func observe() {
        guard observation == nil else { return }
        state = card.current()
        observation = card.observe { [weak self] in self?.state = $0 }
    }

    func heightChanged(_ height: CGFloat) {
        guard height.isFinite, height > 0, abs(height - reportedHeight) > 0.5 else { return }
        reportedHeight = height
        card.heightChanged(height: Double(height))
    }

    var loadImage: CoffeePeekCardImageLoader {
        { [card] url, complete in
            let observation = card.loadImage(url: url, onImage: complete)
            return CoffeePeekCardImageRequest { observation.cancel() }
        }
    }

    var presentation: CoffeePeekCheckInCardData { CoffeePeekCheckInCardData(state: state) }
}

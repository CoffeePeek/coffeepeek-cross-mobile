import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeDiscoveryController(discovery: IosDiscovery) -> UIViewController {
    let model = DiscoveryNativeModel(discovery)
    let controller = UIHostingController(rootView: DiscoveryNativeHost(model: model))
    controller.view.backgroundColor = .clear
    return controller
}

@MainActor
private struct DiscoveryNativeHost: View {
    @StateObject var model: DiscoveryNativeModel

    var body: some View {
        DiscoveryNativeScreen(state: model.state, actions: model.actions,
            map: AnyView(DiscoveryMapCanvas(discovery: model.discovery)),
            roasters: { expanded in model.roasters(expanded) }, roasterSource: model.roasterSource)
            .onAppear(perform: model.observe)
    }
}

@MainActor
private final class DiscoveryNativeModel: ObservableObject {
    let discovery: IosDiscovery
    @Published private(set) var state: IosDiscoveryState
    @Published private(set) var roasterSource: IosRoasterDiscovery?
    private var observation: IosDiscoveryObservation?
    private var roasterObservation: IosDiscoveryObservation?

    init(_ discovery: IosDiscovery) {
        self.discovery = discovery
        state = discovery.currentState()
        roasterSource = discovery.currentRoasters()
    }

    deinit { observation?.cancel(); roasterObservation?.cancel() }

    func observe() {
        guard observation == nil else { return }
        state = discovery.currentState()
        observation = discovery.observe { [weak self] in self?.state = $0 }
        roasterObservation = discovery.observeRoasters { [weak self] in self?.roasterSource = $0 }
    }

    func roasters(_ expanded: Bool) -> AnyView {
        guard let source = roasterSource, source.search == expanded else { return AnyView(Color.clear.frame(height: 0)) }
        return AnyView(RoasterDiscoveryNativeHost(source: source).id(ObjectIdentifier(source)))
    }

    var actions: DiscoveryNativeActions {
        DiscoveryNativeActions(perform: { [discovery] in discovery.perform(name: $0, id: $1) },
            query: { [discovery] in discovery.query(text: $0) },
            visible: { [discovery] in discovery.visible(name: $0, shown: $1) },
            image: { [discovery] url, completion in
                let request = discovery.loadImage(url: url, onImage: completion)
                return CoffeePeekCardImageRequest { request.cancel() }
            })
    }
}

private struct DiscoveryMapCanvas: UIViewRepresentable {
    let discovery: IosDiscovery
    @Environment(\.colorScheme) private var colorScheme

    func makeUIView(context: Context) -> UIView { discovery.createMapView() }
    func updateUIView(_ uiView: UIView, context: Context) {
        discovery.updateMapView(view: uiView, dark: colorScheme == .dark)
    }
}

struct DiscoveryNativeActions {
    var perform: (String, String) -> Void = { _, _ in }
    var query: (String) -> Void = { _ in }
    var visible: (String, Bool) -> Void = { _, _ in }
    var image: CoffeePeekCardImageLoader = { _, _ in nil }

    func call(_ action: String, _ id: String = "") { perform(action, id) }
}

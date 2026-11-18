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
            roasters: { expanded in AnyView(OwnProfileEmbedded(controller: model.roasters(expanded))) })
            .onAppear(perform: model.observe)
    }
}

@MainActor
private final class DiscoveryNativeModel: ObservableObject {
    let discovery: IosDiscovery
    @Published private(set) var state: DiscoveryIosState
    private var observation: IosDiscoveryObservation?
    private var preview: UIViewController?
    private var search: UIViewController?

    init(_ discovery: IosDiscovery) {
        self.discovery = discovery
        state = discovery.currentState()
    }

    deinit { observation?.cancel() }

    func observe() {
        guard observation == nil else { return }
        state = discovery.currentState()
        observation = discovery.observe { [weak self] in self?.state = $0 }
    }

    func roasters(_ expanded: Bool) -> UIViewController {
        if expanded {
            if let search { return search }
            let child = discovery.roastersController(expanded: true)
            search = child
            return child
        }
        if let preview { return preview }
        let child = discovery.roastersController(expanded: false)
        preview = child
        return child
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

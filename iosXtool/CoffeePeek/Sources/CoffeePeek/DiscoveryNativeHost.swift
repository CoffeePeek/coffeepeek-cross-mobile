import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeDiscoveryRouteController(route: IosDiscoveryRoute) -> UIViewController {
    let controller = UIHostingController(rootView: DiscoveryRouteHost(model: DiscoveryRouteModel(route)))
    controller.view.backgroundColor = CoffeePeekNativeStyle.nativeBackground
    return controller
}

@MainActor
private struct DiscoveryRouteHost: View {
    @StateObject var model: DiscoveryRouteModel
    var body: some View {
        Group {
            if let source = model.state.discovery {
                CheckInShopPickerContent(source: source).id(ObjectIdentifier(source)).ignoresSafeArea(.container)
            }
            else {
                VStack(spacing: 20) {
                    if model.state.failed {
                        Text(model.state.failure).multilineTextAlignment(.center)
                        Button(model.state.retry) { model.route.retry() }.buttonStyle(.bordered)
                    } else { CoffeePeekNativeLoader(label: "") }
                }.padding(24).frame(maxWidth: .infinity, maxHeight: .infinity)
                    .font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.foreground)
                    .background(CoffeePeekNativeStyle.background)
            }
        }.background(CoffeePeekNativeStyle.background.ignoresSafeArea()).onAppear(perform: model.observe)
    }
}

@MainActor
private final class DiscoveryRouteModel: ObservableObject {
    let route: IosDiscoveryRoute
    @Published private(set) var state: IosDiscoveryRouteState
    private var observation: IosDiscoveryObservation?
    init(_ route: IosDiscoveryRoute) { self.route = route; state = route.current() }
    isolated deinit { observation?.cancel(); route.close() }
    func observe() {
        guard observation == nil else { return }
        observation = route.observe { [weak self] in self?.state = $0 }
    }
}

@MainActor
func makeDiscoveryController(discovery: IosDiscovery) -> UIViewController {
    DiscoveryNativeHostingController(discovery: discovery)
}

@MainActor
private final class DiscoveryNativeHostingController: UIHostingController<DiscoveryNativeHost> {
    private let discovery: IosDiscovery
    private let visibility: CoffeePeekNativeControllerVisibility
    init(discovery: IosDiscovery) {
        self.discovery = discovery
        let visibility = CoffeePeekNativeControllerVisibility { discovery.setVisible(visible: $0) }
        self.visibility = visibility
        super.init(rootView: DiscoveryNativeHost(model: DiscoveryNativeModel(discovery), visibility: visibility))
        view.backgroundColor = CoffeePeekNativeStyle.nativeBackground
        discovery.bindPresenter { [weak self] in self }
    }
    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        discovery.bottomClearance(value: Float(view.safeAreaInsets.bottom))
        visibility.setShowing(true)
    }
    override func viewWillDisappear(_ animated: Bool) {
        visibility.setShowing(false)
        super.viewWillDisappear(animated)
    }
    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        discovery.bottomClearance(value: Float(view.safeAreaInsets.bottom))
    }
    isolated deinit { discovery.close() }
}

@MainActor
private struct DiscoveryNativeHost: View {
    @StateObject var model: DiscoveryNativeModel
    let visibility: CoffeePeekNativeControllerVisibility
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            if model.ready {
                DiscoveryNativeScreen(state: model.state, actions: model.actions,
                    map: AnyView(DiscoveryMapCanvas(discovery: model.discovery)),
                    roasters: { expanded in model.roasters(expanded) }, roasterSource: model.roasterSource)
            } else { DiscoveryNativeLoadingView() }
        }
        .onAppear {
            model.observe()
            visibility.setForeground(scenePhase == .active)
        }
        .onChange(of: scenePhase) { _, next in visibility.setForeground(next == .active) }
        .sheet(item: Binding(get: { model.filterSheet }, set: { if $0 == nil { model.dismissFilters() } }),
            onDismiss: model.dismissFilters) { sheet in
            DiscoveryFilterControllerHost(filters: sheet.filters)
                .presentationDetents([.fraction(0.8)])
                .presentationDragIndicator(.visible)
                .presentationContentInteraction(.scrolls)
                .presentationBackground(CoffeePeekNativeStyle.surface)
        }
    }
}

@MainActor
func makeCheckInShopPickerController(picker: IosCheckInShopPicker) -> UIViewController {
    UIHostingController(rootView: CheckInShopPickerHost(model: CheckInShopPickerModel(picker)))
}

@MainActor
private struct CheckInShopPickerHost: View {
    @StateObject var model: CheckInShopPickerModel

    var body: some View {
        Group {
            if let source = model.state.discovery {
                CheckInShopPickerContent(source: source).id(ObjectIdentifier(source)).ignoresSafeArea(.container)
            } else {
                CheckInShopPickerStatus(title: model.state.title, failure: model.state.failure,
                    retry: model.state.retry, close: model.state.close, failed: model.state.failed,
                    onRetry: { model.picker.retry() }, onClose: { model.picker.dismiss() })
            }
        }
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .onAppear(perform: model.observe)
        .accessibilityAction(.escape) { model.picker.back() }
    }
}

@MainActor
private final class CheckInShopPickerModel: ObservableObject {
    let picker: IosCheckInShopPicker
    @Published private(set) var state: IosCheckInShopPickerState
    private var observation: IosDiscoveryObservation?

    init(_ picker: IosCheckInShopPicker) {
        self.picker = picker
        state = picker.currentState()
    }
    isolated deinit {
        observation?.cancel()
        picker.close()
    }
    func observe() {
        guard observation == nil else { return }
        observation = picker.observe { [weak self] in self?.state = $0 }
    }
}

@MainActor
private struct CheckInShopPickerContent: UIViewControllerRepresentable {
    let source: IosDiscovery
    func makeUIViewController(context: Context) -> UIViewController { makeDiscoveryController(discovery: source) }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private struct CheckInShopPickerStatus: View {
    let title: String
    let failure: String
    let retry: String
    let close: String
    let failed: Bool
    let onRetry: () -> Void
    let onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            CoffeePeekNativeNavigationHeader(title: title, backLabel: close, back: onClose)
            VStack(spacing: 16) {
                if failed {
                    Text(failure).multilineTextAlignment(.center)
                    Button(retry, action: onRetry).buttonStyle(.bordered)
                } else { CoffeePeekNativeLoader(label: "") }
            }.padding(24).frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .font(CoffeePeekNativeStyle.font(16))
        .background(CoffeePeekNativeStyle.background)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
    }
}

#Preview("Picker unavailable light") {
    CheckInShopPickerStatus(title: "Выберите кофейню", failure: "Не удалось проверить сессию.",
        retry: "Повторить", close: "Закрыть", failed: true, onRetry: {}, onClose: {})
        .preferredColorScheme(.light)
}
#Preview("Picker unavailable dark") {
    CheckInShopPickerStatus(title: "Выберите кофейню", failure: "Не удалось проверить сессию.",
        retry: "Повторить", close: "Закрыть", failed: true, onRetry: {}, onClose: {})
        .preferredColorScheme(.dark)
}

@MainActor
private final class DiscoveryNativeModel: ObservableObject {
    let discovery: IosDiscovery
    @Published private(set) var state: IosDiscoveryState
    @Published private(set) var roasterSource: IosRoasterDiscovery?
    @Published private(set) var ready: Bool
    @Published private(set) var filterSheet: DiscoveryNativeFilterSheet?
    private var observation: IosDiscoveryObservation?
    private var roasterObservation: IosDiscoveryObservation?
    private var filterObservation: IosDiscoveryObservation?

    init(_ discovery: IosDiscovery) {
        self.discovery = discovery
        state = discovery.currentState()
        roasterSource = discovery.currentRoasters()
        ready = discovery.isReady()
        filterSheet = discovery.currentFilters().map(DiscoveryNativeFilterSheet.init)
    }

    isolated deinit {
        observation?.cancel()
        roasterObservation?.cancel()
        filterObservation?.cancel()
    }

    func observe() {
        guard observation == nil else { return }
        state = discovery.currentState()
        observation = discovery.observe { [weak self] in
            self?.state = $0
            self?.ready = self?.discovery.isReady() ?? false
        }
        roasterObservation = discovery.observeRoasters { [weak self] in self?.roasterSource = $0 }
        filterObservation = discovery.observeFilters { [weak self] in self?.filterSheet = $0.map(DiscoveryNativeFilterSheet.init) }
    }
    func dismissFilters() { discovery.applyFilters() }

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

private struct DiscoveryNativeFilterSheet: Identifiable {
    let filters: IosDiscoveryFilters
    var id: ObjectIdentifier { ObjectIdentifier(filters) }
}

private struct DiscoveryFilterControllerHost: UIViewControllerRepresentable {
    let filters: IosDiscoveryFilters
    func makeUIViewController(context: Context) -> UIViewController { makeDiscoveryFilterController(filters) }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private struct DiscoveryNativeLoadingView: View {
    var body: some View {
        CoffeePeekNativeLoader(label: "")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(CoffeePeekNativeStyle.background)
    }
}

#Preview("Discovery loading light") { DiscoveryNativeLoadingView().preferredColorScheme(.light) }
#Preview("Discovery loading dark") { DiscoveryNativeLoadingView().preferredColorScheme(.dark) }

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

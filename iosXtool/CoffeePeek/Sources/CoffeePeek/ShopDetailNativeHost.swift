import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShopDetailRouteController(route: IosShopDetailRoute) -> UIViewController {
    UIHostingController(rootView: ShopDetailRouteHost(model: ShopDetailRouteModel(route)))
}

@MainActor
private struct ShopDetailRouteHost: View {
    @StateObject var model: ShopDetailRouteModel
    var body: some View {
        Group {
            if let source = model.state.source {
                ShopDetailRouteContent(source: source).id(ObjectIdentifier(source))
            } else {
                CoffeePeekNativeRouteStatus(failed: model.state.failed, title: model.state.title, retry: model.state.retry,
                    back: model.state.back, onRetry: { model.route.retry() }, onBack: { model.route.back() })
            }
        }
        .onAppear(perform: model.observe)
        .accessibilityAction(.escape) { model.route.back() }
    }
}

@MainActor
private final class ShopDetailRouteModel: ObservableObject {
    let route: IosShopDetailRoute
    @Published private(set) var state: IosShopDetailRouteState
    private var observation: IosShopCheckInObservation?
    init(_ route: IosShopDetailRoute) {
        self.route = route
        state = route.current()
    }
    isolated deinit {
        observation?.cancel()
        route.close()
    }
    func observe() {
        guard observation == nil else { return }
        observation = route.observe { [weak self] in self?.state = $0 }
    }
}

private struct ShopDetailRouteContent: UIViewControllerRepresentable {
    let source: IosShopDetail
    func makeUIViewController(context: Context) -> UIViewController { makeShopDetailController(detail: source) }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct CoffeePeekNativeRouteStatus: View {
    let failed: Bool
    let title: String
    let retry: String
    let back: String
    let onRetry: () -> Void
    let onBack: () -> Void
    var body: some View {
        VStack(spacing: 0) {
            CoffeePeekNativeNavigationHeader(title: "", backLabel: back, back: onBack)
            VStack(spacing: 16) {
                if failed {
                    Text(title).multilineTextAlignment(.center)
                    Button(retry, action: onRetry).buttonStyle(.bordered)
                } else { CoffeePeekNativeLoader(label: "") }
            }.padding(24).frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .font(CoffeePeekNativeStyle.font(16))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background)
        .tint(CoffeePeekNativeStyle.accent)
    }
}

#Preview("Shop session failure light") {
    CoffeePeekNativeRouteStatus(failed: true, title: "Не удалось проверить сессию.", retry: "Повторить", back: "Назад",
        onRetry: {}, onBack: {}).preferredColorScheme(.light)
}
#Preview("Shop session failure dark") {
    CoffeePeekNativeRouteStatus(failed: true, title: "Не удалось проверить сессию.", retry: "Повторить", back: "Назад",
        onRetry: {}, onBack: {}).preferredColorScheme(.dark)
}

@MainActor
func makeShopDetailController(detail: IosShopDetail) -> UIViewController { ShopDetailHostingController(detail: detail) }

@MainActor
private final class ShopDetailHostingController: UIHostingController<ShopDetailNativeHost> {
    let model: ShopDetailNativeModel
    private let visibility: CoffeePeekNativeControllerVisibility

    init(detail: IosShopDetail) {
        let model = ShopDetailNativeModel(detail)
        self.model = model
        let visibility = CoffeePeekNativeControllerVisibility { detail.setVisible(shown: $0) }
        self.visibility = visibility
        super.init(rootView: ShopDetailNativeHost(model: model, visibility: visibility))
        safeAreaRegions = []
        view.backgroundColor = .clear
    }

    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        visibility.setShowing(true)
    }
    override func viewWillDisappear(_ animated: Bool) {
        visibility.setShowing(false)
        super.viewWillDisappear(animated)
    }
    isolated deinit { model.detail.close() }
    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        model.topInset = view.safeAreaInsets.top
        model.bottomInset = view.safeAreaInsets.bottom
    }
}

private struct ShopDetailNativeHost: View {
    @ObservedObject var model: ShopDetailNativeModel
    let visibility: CoffeePeekNativeControllerVisibility
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ShopDetailNativeScreen(state: model.state, actions: model.actions, topInset: model.topInset,
            bottomInset: model.bottomInset, owner: {
                if let controller = model.ownership() { AnyView(ShopDetailOwnerView(controller: controller)) }
                else { AnyView(EmptyView()) }
            }, card: { row in AnyView(checkIn(row)) })
            .onAppear {
                model.observe()
                visibility.setForeground(scenePhase == .active)
            }
            .onChange(of: scenePhase) { _, next in visibility.setForeground(next == .active) }
            .coffeePeekShopDetailOverlays(model.detail.overlays)
    }

    private func checkIn(_ row: ShopCheckInListRow) -> some View {
        ShopCheckInListCard(row: row, date: model.state.checkInDates[row.id] ?? row.date,
            strings: ShopCheckInListStrings(languageTag: model.detail.languageTag),
            actions: ShopCheckInCardActions(openAuthor: { model.actions.call("author", row.id) },
                vote: { model.actions.call("vote", row.id) }, report: { model.actions.call("report", row.id) },
                block: { model.actions.call("block", row.id) },
                photo: { model.actions.call("checkin-photo", row.id, $0) }),
            loadArtwork: { index, receive in
                if let index { return model.detail.loadPhoto(id: row.id, index: Int32(index), receive: receive) }
                return model.detail.loadAvatar(id: row.id, receive: receive)
            })
    }
}

extension View {
    @ViewBuilder
    func coffeePeekShopDetailOverlays(_ source: IosShopDetailOverlays?) -> some View {
        if let source { modifier(ShopDetailOverlaysModifier(source: source)) } else { self }
    }
}

@MainActor
private struct ShopDetailOverlaysModifier: ViewModifier {
    @StateObject private var model: ShopDetailOverlaysModel
    init(source: IosShopDetailOverlays) {
        _model = StateObject(wrappedValue: ShopDetailOverlaysModel(source))
    }
    func body(content: Content) -> some View {
        let photo = model.state.photo
        content
            .onAppear(perform: model.observe)
            .fullScreenCover(item: model.editorBinding) { item in
                ShopDetailEditorContent(editor: item.editor).interactiveDismissDisabled(true)
            }
            .coffeePeekPhotoViewer(photo?.viewer, languageTag: photo?.languageTag ?? "ru") {
                if let photo { model.source.dismissPhoto(photo: photo) }
            }
            .confirmationDialog(model.state.route?.title ?? "", isPresented: model.routeBinding, titleVisibility: .visible) {
                if let route = model.state.route {
                    ForEach(Array(route.options.enumerated()), id: \.offset) { _, option in
                        Button(option.title) { option.select() }
                    }
                    Button(route.cancel, role: .cancel) { model.source.dismissRoute() }
                }
            }
    }
}

@MainActor
private final class ShopDetailOverlaysModel: ObservableObject {
    let source: IosShopDetailOverlays
    @Published private(set) var state: IosShopDetailOverlayState
    private var observation: IosShopCheckInObservation?
    init(_ source: IosShopDetailOverlays) {
        self.source = source
        state = source.current()
    }
    isolated deinit { observation?.cancel() }
    func observe() {
        guard observation == nil else { return }
        observation = source.observe { [weak self] in self?.state = $0 }
    }
    var editorBinding: Binding<ShopDetailEditorIdentity?> {
        let presented = state.editor
        return Binding(get: { self.state.editor.map(ShopDetailEditorIdentity.init) }, set: {
            if $0 == nil, let presented { self.source.requestEditorDismiss(editor: presented) }
        })
    }
    var routeBinding: Binding<Bool> {
        Binding(get: { self.state.route != nil }, set: { if !$0 { self.source.dismissRoute() } })
    }
}

private struct ShopDetailEditorIdentity: Identifiable {
    let editor: IosShopCheckInEditor
    var id: ObjectIdentifier { ObjectIdentifier(editor) }
}

private struct ShopDetailEditorContent: UIViewControllerRepresentable {
    let editor: IosShopCheckInEditor
    func makeUIViewController(context: Context) -> UIViewController { makeShopCheckInEditorController(editor: editor) }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private struct ShopDetailOwnerView: UIViewControllerRepresentable {
    let controller: UIViewController
    func makeUIViewController(context: Context) -> UIViewController { controller }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@MainActor
private final class ShopDetailNativeModel: ObservableObject {
    let detail: IosShopDetail
    @Published private(set) var state: ShopDetailNativeData
    @Published var topInset: CGFloat = 0
    @Published var bottomInset: CGFloat = 0
    private var observation: IosShopCheckInObservation?
    private var owner: UIViewController?

    init(_ detail: IosShopDetail) {
        self.detail = detail
        state = ShopDetailNativeData(detail.currentState(), languageTag: detail.languageTag)
    }
    isolated deinit { observation?.cancel() }

    func observe() {
        guard observation == nil else { return }
        update(detail.currentState())
        observation = detail.observe { [weak self] in self?.update($0) }
    }

    private func update(_ incoming: IosShopDetailState) {
        if state.details?.id != incoming.data.details?.id || state.details?.slug != incoming.data.details?.slug { owner = nil }
        state = ShopDetailNativeData(incoming, languageTag: detail.languageTag)
    }

    func ownership() -> UIViewController? {
        if let owner { return owner }
        owner = detail.ownershipController()
        return owner
    }

    var actions: ShopDetailNativeActions {
        ShopDetailNativeActions(perform: { [detail] in detail.perform(name: $0, id: $1, index: Int32($2)) },
            dismissMessage: { [detail] in detail.dismissMessage() },
            visible: { [detail] in detail.visible(names: Array($0)) }, image: { [detail] url, receive in
                let request = detail.loadImage(url: url, onImage: receive)
                return CoffeePeekCardImageRequest { request.cancel() }
            })
    }
}

struct ShopDetailNativeActions {
    var perform: (String, String, Int) -> Void = { _, _, _ in }
    var dismissMessage: () -> Void = {}
    var visible: (Set<String>) -> Void = { _ in }
    var image: CoffeePeekCardImageLoader = { _, _ in nil }
    func call(_ name: String, _ id: String = "", _ index: Int = 0) { perform(name, id, index) }
}

struct ShopDetailNativeData {
    var languageTag = "ru"
    var active = true
    var loading = false
    var failed = false
    var loggedIn = false
    var favorite = false
    var favoriteEnabled = true
    var favoriteBusy = false
    var favoriteSelection = false
    var favoriteLabel = ""
    var scheduleExpanded = false
    var featuresExpanded = false
    var priceVisible = false
    var message: String?
    var details: IosShopDetails?
    var labels: [String: String] = [:]
    var checkInDates: [String: String] = [:]
    var rows: [ShopCheckInListRow] = []
    var hiddenCheckIns = 0
    var overall = ""
    var checkInCount = 0
    var sample = false
    var breakdown: [ShopCheckInRatingPresentation] = []
    init() {}

    init(_ state: IosShopDetailState, languageTag: String) {
        self.languageTag = languageTag
        let data = state.data
        active = state.active
        loading = data.loading
        failed = data.hasError
        loggedIn = data.loggedIn
        favorite = data.favorite
        favoriteEnabled = data.favoriteEnabled
        favoriteBusy = data.favoriteBusy
        favoriteSelection = data.favoriteSelection
        favoriteLabel = data.favoriteLabel
        scheduleExpanded = data.scheduleExpanded
        featuresExpanded = data.featuresExpanded
        priceVisible = data.priceInfoVisible
        message = data.message
        details = data.details
        labels = data.labels
        checkInDates = data.checkInDates
        rows = state.checkIns.rows.map(ShopCheckInListRow.init)
        hiddenCheckIns = Int(state.checkIns.hiddenCount)
        overall = state.checkIns.overall
        checkInCount = Int(state.checkIns.checkInCount)
        sample = state.checkIns.isRatingSample
        breakdown = state.checkIns.breakdown.map { ShopCheckInRatingPresentation(value: $0.value, label: $0.label) }
    }
}

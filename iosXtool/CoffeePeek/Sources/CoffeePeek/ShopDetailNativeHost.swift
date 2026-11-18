import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShopDetailController(detail: IosShopDetail) -> UIViewController { ShopDetailHostingController(detail: detail) }

private final class ShopDetailHostingController: UIHostingController<ShopDetailNativeHost> {
    let model: ShopDetailNativeModel

    init(detail: IosShopDetail) {
        let model = ShopDetailNativeModel(detail)
        self.model = model
        super.init(rootView: ShopDetailNativeHost(model: model))
        safeAreaRegions = []
        view.backgroundColor = .clear
    }

    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        model.topInset = view.safeAreaInsets.top
        model.bottomInset = view.safeAreaInsets.bottom
    }
}

private struct ShopDetailNativeHost: View {
    @ObservedObject var model: ShopDetailNativeModel

    var body: some View {
        ShopDetailNativeScreen(state: model.state, actions: model.actions, topInset: model.topInset,
            bottomInset: model.bottomInset, owner: {
                if let controller = model.ownership() { AnyView(ShopDetailOwnerView(controller: controller)) }
                else { AnyView(EmptyView()) }
            }, card: { row in AnyView(checkIn(row)) })
            .onAppear(perform: model.observe)
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

private struct ShopDetailOwnerView: UIViewControllerRepresentable {
    let controller: UIViewController
    func makeUIViewController(context: Context) -> UIViewController { controller }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

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
    deinit { observation?.cancel() }

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
    var details: ShopDetailIosDetails?
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

import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShopCheckInListController(list: IosShopCheckInList, languageTag: String) -> UIViewController {
    let strings = ShopCheckInListStrings(languageTag: languageTag)
    let controller = ShopCheckInListHostingController(list: list, strings: strings)
    controller.title = strings.text("shop_checkin_list_title")
    return controller
}

@MainActor
private final class ShopCheckInListHostingController: UIHostingController<ShopCheckInListHost> {
    private let visibility: CoffeePeekNativeControllerVisibility
    init(list: IosShopCheckInList, strings: ShopCheckInListStrings) {
        let visibility = CoffeePeekNativeControllerVisibility { list.setVisible(shown: $0) }
        self.visibility = visibility
        super.init(rootView: ShopCheckInListHost(list: list, strings: strings, visibility: visibility))
    }
    required dynamic init?(coder: NSCoder) { nil }
    override func viewDidAppear(_ animated: Bool) { super.viewDidAppear(animated); visibility.setShowing(true) }
    override func viewWillDisappear(_ animated: Bool) { visibility.setShowing(false); super.viewWillDisappear(animated) }
}

private struct ShopCheckInListHost: View {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var model: ShopCheckInListModel
    private let list: IosShopCheckInList
    private let strings: ShopCheckInListStrings
    let visibility: CoffeePeekNativeControllerVisibility

    init(list: IosShopCheckInList, strings: ShopCheckInListStrings, visibility: CoffeePeekNativeControllerVisibility) {
        self.list = list
        self.strings = strings
        self.visibility = visibility
        _model = StateObject(wrappedValue: ShopCheckInListModel(list: list))
    }

    var body: some View {
        ShopCheckInListScreen(state: model.state, strings: strings,
                              onRetry: { list.retry() },
                              onSignIn: { list.signIn() }, onRegister: { list.register() }) { row in
            ShopCheckInListCard(row: row, date: list.dateLabel(raw: row.date), strings: strings,
                                actions: actions(for: row), loadArtwork: { index, receive in
                if let index {
                    return list.loadPhoto(id: row.id, index: Int32(index), receive: receive)
                }
                return list.loadAvatar(id: row.id, receive: receive)
            })
        }
        .accessibilityHidden(model.viewer != nil)
        .allowsHitTesting(model.viewer == nil)
        .coffeePeekPhotoViewer(model.viewer, languageTag: strings.languageTag, onDismiss: { model.viewer = nil })
        .onAppear {
            model.start(unknownAuthor: strings.text("shop_checkins_author_unknown"))
            visibility.setForeground(scenePhase == .active)
        }
        .onDisappear { model.stop() }
        .onChange(of: scenePhase) { _, next in
            visibility.setForeground(next == .active)
        }
    }

    private func actions(for row: ShopCheckInListRow) -> ShopCheckInCardActions {
        ShopCheckInCardActions(
            openAuthor: { list.author(id: row.id) },
            vote: { list.vote(id: row.id) },
            report: { list.report(id: row.id) },
            block: { list.block(id: row.id) },
            photo: { index in list.photo(id: row.id, index: Int32(index)) }
        )
    }
}

@MainActor
private final class ShopCheckInListModel: ObservableObject {
    @Published private(set) var state: ShopCheckInListPresentation
    @Published var viewer: IosPhotoViewer?
    private let list: IosShopCheckInList
    private var observation: IosShopCheckInObservation?
    var isObserved: Bool { observation != nil }

    init(list: IosShopCheckInList) {
        self.list = list
        state = ShopCheckInListPresentation(list.currentState())
    }

    isolated deinit {
        observation?.cancel()
        list.close()
    }

    func start(unknownAuthor: String) {
        guard observation == nil else { return }
        update(list.currentState())
        observation = list.observe(onState: { [weak self] in self?.update($0) },
                                   onPhoto: { [weak self] in self?.openPhoto($0) }, unknownAuthor: unknownAuthor)
    }

    func stop() {
        guard viewer == nil else { return }
        observation?.cancel()
        observation = nil
        viewer = nil
    }

    private func update(_ incoming: IosShopCheckInListState) {
        if state.generation != incoming.generation { viewer = nil }
        state = ShopCheckInListPresentation(incoming)
    }

    private func openPhoto(_ album: IosPhotoViewer) {
        guard viewer == nil else { return }
        viewer = album
    }
}

private struct ShopCheckInListPresentation {
    var generation: Int32 = 0
    var isLoading = false
    var hasLoaded = true
    var hasLoadError = false
    var hasVoteError = false
    var observationFailed = false
    var shopTitle = ""
    var checkInCount = 0
    var overall = "0.0"
    var isRatingSample = false
    var breakdown: [ShopCheckInRatingPresentation] = []
    var rows: [ShopCheckInListRow] = []
    var showGuestPrompt = false
    var hiddenCount = 0

    init() {}

    init(_ state: IosShopCheckInListState) {
        generation = state.generation
        isLoading = state.isLoading
        hasLoaded = state.hasLoaded
        hasLoadError = state.hasLoadError
        hasVoteError = state.hasVoteError
        observationFailed = state.observationFailed
        shopTitle = state.shopTitle
        checkInCount = Int(state.checkInCount)
        overall = state.overall
        isRatingSample = state.isRatingSample
        breakdown = state.breakdown.map { ShopCheckInRatingPresentation(value: $0.value, label: $0.label) }
        rows = state.rows.map(ShopCheckInListRow.init)
        showGuestPrompt = state.showGuestPrompt
        hiddenCount = Int(state.hiddenCount)
    }
}

private struct ShopCheckInListScreen<Card: View>: View {
    let state: ShopCheckInListPresentation
    let strings: ShopCheckInListStrings
    let onRetry: () -> Void
    let onSignIn: () -> Void
    let onRegister: () -> Void
    @ViewBuilder let card: (ShopCheckInListRow) -> Card

    var body: some View {
        Group {
            if state.hasLoaded { list }
            else { status }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(CoffeePeekNativeStyle.background)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
    }

    private var list: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                Text(state.shopTitle)
                    .font(CoffeePeekNativeStyle.font(24, weight: "Bold", relativeTo: .title2))
                    .accessibilityAddTraits(.isHeader)
                ShopCheckInListRatings(overall: state.overall, count: state.checkInCount, isSample: state.isRatingSample,
                                       breakdown: state.breakdown, strings: strings)
                if state.isLoading || state.hasLoadError { status }
                if state.hasVoteError {
                    Text(strings.text("shop_checkin_list_vote_error")).foregroundStyle(.red)
                }
                if state.rows.isEmpty { Text(strings.text("shop_checkin_list_empty")).foregroundStyle(.secondary) }
                ForEach(state.rows) { card($0) }
                if state.showGuestPrompt { guestPrompt }
                ForEach(0..<state.hiddenCount, id: \.self) { _ in
                    ShopCheckInGuestPlaceholder()
                }
            }
            .padding(16)
        }
        .id(state.generation)
    }

    private var status: some View {
        VStack(spacing: 8) {
            if state.isLoading {
                CoffeePeekNativeLoader(label: strings.text(state.hasLoaded ? "shop_checkin_list_refreshing" : "shop_checkin_list_loading"), size: 32, showsLabel: true)
            } else if state.hasLoadError {
                Text(strings.text(errorMessage))
                Button(strings.text("shop_detail_retry"), action: onRetry).buttonStyle(.bordered)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(12)
    }

    private var errorMessage: String {
        if state.observationFailed { return "shop_session_unavailable" }
        return state.hasLoaded ? "shop_checkin_list_refresh_error" : "shop_checkin_list_load_error"
    }

    private var guestPrompt: some View {
        CoffeePeekGuestAccountCard(prompt: strings.text("shop_checkins_join_prompt"),
            loginTitle: strings.text("shop_reviews_sign_in"),
            noAccountPrompt: strings.text("shop_checkins_no_account"), registerTitle: strings.text("shop_reviews_register"),
            login: onSignIn, register: onRegister)
    }
}

/// Only neutral shapes are rendered; hidden check-in data never enters this view.
struct ShopCheckInGuestPlaceholder: View {
    var body: some View {
        GeometryReader { geometry in
            VStack(alignment: .leading, spacing: 12) {
                ForEach(0..<4) { index in
                    RoundedRectangle(cornerRadius: 6).fill(CoffeePeekNativeStyle.surfaceVariant)
                        .frame(width: max(0, geometry.size.width - 32) * (index == 0 ? 0.5 : 0.85), height: 16)
                }
            }.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading).padding(16)
                .blur(radius: 8)
        }
        .frame(height: 192)
        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(CoffeePeekNativeStyle.border))
        .allowsHitTesting(false).accessibilityHidden(true)
    }
}

#Preview("Guest check-in blur light") {
    ShopCheckInGuestPlaceholder().frame(width: 280).padding().preferredColorScheme(.light)
}
#Preview("Guest check-in blur dark") {
    ShopCheckInGuestPlaceholder().frame(width: 280).padding().preferredColorScheme(.dark)
}

struct ShopCheckInListStrings {
    let languageTag: String

    func text(_ key: String) -> String {
        let language = languageTag.hasPrefix("be") ? "be" : "ru"
        let bundle = Bundle.main.path(forResource: language, ofType: "lproj").flatMap(Bundle.init(path:)) ?? .main
        return bundle.localizedString(forKey: key, value: nil, table: "NativeShopCheckIns")
    }

    func format(_ key: String, _ values: CVarArg...) -> String {
        String(format: text(key), locale: Locale(identifier: languageTag), arguments: values)
    }
}

@MainActor
private func shopCheckInListPreview(guest: Bool = false) -> some View {
    var state = ShopCheckInListPresentation()
    state.shopTitle = "CoffeePeek"
    state.overall = "4.6"
    state.checkInCount = 3
    state.showGuestPrompt = guest
    state.hiddenCount = guest ? 2 : 0
    state.breakdown = [ShopCheckInRatingPresentation(value: 4.6, label: "4.6")]
    return NavigationStack {
        ShopCheckInListScreen(state: state, strings: ShopCheckInListStrings(languageTag: "ru"),
                             onRetry: {}, onSignIn: {}, onRegister: {}) { Text($0.note) }
            .navigationTitle(ShopCheckInListStrings(languageTag: "ru").text("shop_checkin_list_title"))
            .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview("Check-ins · Light") { shopCheckInListPreview().preferredColorScheme(.light) }
#Preview("Check-ins · Dark") { shopCheckInListPreview().preferredColorScheme(.dark) }
#Preview("Guest check-ins light") { shopCheckInListPreview(guest: true).preferredColorScheme(.light) }
#Preview("Guest check-ins dark") { shopCheckInListPreview(guest: true).preferredColorScheme(.dark) }
#Preview("Check-ins · Large text") {
    shopCheckInListPreview().preferredColorScheme(.dark).environment(\.dynamicTypeSize, .accessibility2)
}

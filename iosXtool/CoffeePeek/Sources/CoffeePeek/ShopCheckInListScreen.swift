import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShopCheckInListController(list: IosShopCheckInList, languageTag: String) -> UIViewController {
    UIHostingController(rootView: ShopCheckInListHost(list: list, strings: ShopCheckInListStrings(languageTag: languageTag)))
}

private struct ShopCheckInListHost: View {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var model: ShopCheckInListModel
    private let list: IosShopCheckInList
    private let strings: ShopCheckInListStrings

    init(list: IosShopCheckInList, strings: ShopCheckInListStrings) {
        self.list = list
        self.strings = strings
        _model = StateObject(wrappedValue: ShopCheckInListModel(list: list))
    }

    var body: some View {
        ShopCheckInListScreen(state: model.state, strings: strings,
                              onBack: { list.back() }, onRetry: { list.retry() },
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
        .onAppear { model.start(unknownAuthor: strings.text("shop_checkins_author_unknown")) }
        .onDisappear { model.stop() }
        .onChange(of: scenePhase) { _, next in
            if next == .active && model.isObserved { list.resume() }
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

    deinit {
        observation?.cancel()
        list.close()
    }

    func start(unknownAuthor: String) {
        guard observation == nil else { return }
        update(list.currentState())
        observation = list.observe(onState: { [weak self] in self?.update($0) },
                                   onPhoto: { [weak self] in self?.openPhoto($0) }, unknownAuthor: unknownAuthor)
        list.resume()
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
    let onBack: () -> Void
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
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
    }

    private var header: some View {
        HStack(spacing: 8) {
            Button(action: onBack) {
                Label(strings.text("shop_detail_back"), systemImage: "chevron.backward")
                    .labelStyle(.iconOnly)
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            Text(strings.text("shop_checkin_list_title"))
                .font(CoffeePeekNativeStyle.font(18, weight: "Bold", relativeTo: .headline))
                .accessibilityAddTraits(.isHeader)
            Spacer()
        }
        .padding(.horizontal, 12)
        .background(.bar)
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
                    RoundedRectangle(cornerRadius: 16).fill(CoffeePeekNativeStyle.surface)
                        .frame(height: 80)
                        .accessibilityHidden(true)
                }
            }
            .padding(16)
        }
        .id(state.generation)
    }

    private var status: some View {
        VStack(spacing: 8) {
            if state.isLoading {
                ProgressView(strings.text(state.hasLoaded ? "shop_checkin_list_refreshing" : "shop_checkin_list_loading"))
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
        VStack(spacing: 10) {
            Text(strings.text("shop_checkins_join_prompt")).multilineTextAlignment(.center)
            Button(strings.text("shop_reviews_sign_in"), action: onSignIn)
                .buttonStyle(.borderedProminent)
            Button(action: onRegister) {
                Text(strings.text("shop_checkins_no_account") + " " + strings.text("shop_reviews_register"))
                    .frame(minHeight: 44)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
    }
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

private func shopCheckInListPreview() -> some View {
    var state = ShopCheckInListPresentation()
    state.shopTitle = "CoffeePeek"
    state.overall = "4.6"
    state.checkInCount = 3
    state.breakdown = [ShopCheckInRatingPresentation(value: 4.6, label: "4.6")]
    return ShopCheckInListScreen(state: state, strings: ShopCheckInListStrings(languageTag: "ru"),
                                 onBack: {}, onRetry: {}, onSignIn: {}, onRegister: {}) {
        Text($0.note)
    }
}

#Preview("Check-ins Â· Light") { shopCheckInListPreview().preferredColorScheme(.light) }
#Preview("Check-ins Â· Dark") { shopCheckInListPreview().preferredColorScheme(.dark) }

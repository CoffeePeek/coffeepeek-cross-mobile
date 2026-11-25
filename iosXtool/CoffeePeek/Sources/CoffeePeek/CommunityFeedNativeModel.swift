import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCommunityFeedController(feed: IosCommunityFeed) -> UIViewController {
    CommunityFeedHostingController(feed: feed)
}

@MainActor
private final class CommunityFeedHostingController: UIHostingController<CommunityFeedNativeHost> {
    private let feed: IosCommunityFeed
    private let visibility: CoffeePeekNativeControllerVisibility
    init(feed: IosCommunityFeed) {
        self.feed = feed
        let visibility = CoffeePeekNativeControllerVisibility { feed.setVisible(visible: $0) }
        self.visibility = visibility
        super.init(rootView: CommunityFeedNativeHost(feed: feed, visibility: visibility))
        view.backgroundColor = .clear
    }
    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        feed.bottomClearance(value: Double(view.safeAreaInsets.bottom))
        visibility.setShowing(true)
    }
    override func viewWillDisappear(_ animated: Bool) {
        visibility.setShowing(false)
        super.viewWillDisappear(animated)
    }
    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        feed.bottomClearance(value: Double(view.safeAreaInsets.bottom))
    }
    isolated deinit { feed.close() }
}

@MainActor
private struct CommunityFeedNativeHost: View {
    let feed: IosCommunityFeed
    let visibility: CoffeePeekNativeControllerVisibility
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var model: CommunityFeedNativeModel
    init(feed: IosCommunityFeed, visibility: CoffeePeekNativeControllerVisibility) {
        self.feed = feed
        self.visibility = visibility
        _model = StateObject(wrappedValue: CommunityFeedNativeModel(feed: feed))
    }
    var body: some View {
        CommunityFeedNativeContent(state: model.presentation, actions: model.actions, headerAction: notificationAction)
            .onAppear {
                model.observe()
                visibility.setForeground(scenePhase == .active)
            }
            .onChange(of: scenePhase) { _, phase in visibility.setForeground(phase == .active) }
            .coffeePeekCommunityOverlays(feed.overlays)
    }
    private var notificationAction: AnyView? {
        feed.notifications.map { inbox in
            AnyView(CommunityNotificationButton(inbox: inbox, languageTag: feed.notificationLanguageTag)
                .frame(width: 48, height: 48).id(ObjectIdentifier(inbox)))
        }
    }
}

@MainActor
private struct CommunityNotificationButton: UIViewControllerRepresentable {
    let inbox: IosNotificationInbox
    let languageTag: String
    func makeUIViewController(context: Context) -> UIViewController { makeNotificationInboxController(inbox: inbox, languageTag: languageTag) }
    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}

@MainActor
private final class CommunityFeedNativeModel: ObservableObject {
    private let feed: IosCommunityFeed
    @Published private var state: IosCommunityFeedState
    private var observation: IosCommunityFeedObservation?
    private var refreshing = false

    init(feed: IosCommunityFeed) {
        self.feed = feed
        state = feed.current()
    }
    isolated deinit {
        observation?.cancel()
    }
    func observe() {
        guard observation == nil else { return }
        state = feed.current()
        observation = feed.observe { [weak self] in self?.state = $0 }
    }
    var actions: CommunityFeedNativeActions {
        let feed = self.feed
        return CommunityFeedNativeActions(perform: { feed.perform(action: $0) },
            refresh: { [weak self] in await self?.refresh() },
            visible: { feed.visible(key: $0, fraction: $1) })
    }
    private func refresh() async {
        guard !refreshing, !Task.isCancelled else { return }
        refreshing = true
        let completion = AsyncStream<Void>.makeStream(bufferingPolicy: .bufferingNewest(1))
        let subscription = feed.refresh { completion.continuation.finish() }
        defer {
            subscription.cancel()
            completion.continuation.finish()
            refreshing = false
        }
        for await _ in completion.stream {}
    }
    var presentation: CommunityFeedNativePresentation {
        guard state.active else { return CommunityFeedNativePresentation(active: false) }
        var result = CommunityFeedNativePresentation(active: true, revision: state.revision,
            sessionGeneration: state.sessionGeneration, title: state.title,
            loading: state.loading, refreshing: state.refreshing, loadingMore: state.loadingMore,
            canLoadMore: state.canLoadMore, guest: state.guest, interactive: state.interactive)
        result.labels = CommunityFeedNativeLabels(loading: state.loadingLabel, errorTitle: state.errorTitle,
            errorText: state.errorText, retry: state.retryLabel, empty: state.emptyLabel,
            signIn: state.signInLabel, refresh: state.refreshLabel, confirmedTitle: state.confirmedTitle,
            confirmedBody: state.confirmedBody, confirmedOpen: state.confirmedOpenLabel)
        result.confirmedIds = state.confirmedIds
        result.rows = state.cards.map { card in
            let key = card.current().key
            return CommunityFeedNativeRow(id: AnyHashable(ObjectIdentifier(card)), key: key,
                card: card, teaser: state.teaserKey == key)
        }
        result.bottomClearance = CGFloat(state.bottomClearance)
        return result
    }
}

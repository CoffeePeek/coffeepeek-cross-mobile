import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCommunityFeedController(feed: IosCommunityFeed) -> UIViewController {
    UIHostingController(rootView: CommunityFeedNativeHost(feed: feed))
}

private struct CommunityFeedNativeHost: View {
    @StateObject private var model: CommunityFeedNativeModel
    init(feed: IosCommunityFeed) {
        _model = StateObject(wrappedValue: CommunityFeedNativeModel(feed: feed))
    }
    var body: some View {
        CommunityFeedNativeContent(state: model.presentation, actions: model.actions)
            .onAppear(perform: model.observe)
    }
}

@MainActor
private final class CommunityFeedNativeModel: ObservableObject {
    private let feed: IosCommunityFeed
    @Published private var state: IosCommunityFeedState
    private var observation: IosCommunityFeedObservation?
    private var refreshObservation: IosCommunityFeedObservation?
    private var refreshContinuation: CheckedContinuation<Void, Never>?

    init(feed: IosCommunityFeed) {
        self.feed = feed
        state = feed.current()
    }
    deinit {
        observation?.cancel()
        refreshObservation?.cancel()
        refreshContinuation?.resume()
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
        guard refreshContinuation == nil else { return }
        await withTaskCancellationHandler {
            await withCheckedContinuation { continuation in
                refreshContinuation = continuation
                let subscription = feed.refresh { [weak self] in self?.finishRefresh() }
                if refreshContinuation == nil { subscription.cancel() }
                else { refreshObservation = subscription }
            }
        } onCancel: {
            Task { @MainActor [weak self] in self?.finishRefresh() }
        }
    }
    private func finishRefresh() {
        let continuation = refreshContinuation
        let subscription = refreshObservation
        refreshContinuation = nil
        refreshObservation = nil
        subscription?.cancel()
        continuation?.resume()
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

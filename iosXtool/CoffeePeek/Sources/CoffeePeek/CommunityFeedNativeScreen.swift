import SwiftUI

struct CommunityFeedNativeContent: View {
    let state: CommunityFeedNativePresentation
    let actions: CommunityFeedNativeActions
    @Environment(\.scenePhase) private var scenePhase
    @Namespace private var viewportSpace
    @State private var frames: [String: CGRect] = [:]
    @State private var viewportSize: CGSize = .zero
    @State private var reported: [String: Double] = [:]
    @State private var appeared = false
    @State private var signInHeight: CGFloat = 48
    @State private var paginationRevision: Int64?

    var body: some View {
        VStack(spacing: 0) {
            if state.active {
                header
                viewport
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .ignoresSafeArea(.container, edges: .bottom)
        .allowsHitTesting(state.active && state.interactive)
        .onAppear { appeared = true; reportVisibility() }
        .onDisappear { appeared = false; reportVisibility() }
        .onChange(of: scenePhase) { _, _ in reportVisibility() }
        .onChange(of: state.revision) { _, _ in reportVisibility() }
        .onChange(of: state.active) { _, _ in reportVisibility() }
        .onChange(of: state.interactive) { _, _ in reportVisibility() }
    }

    private var header: some View {
        HStack(spacing: 12) {
            Text(state.title).font(CoffeePeekNativeStyle.rootHeadingFont)
                .frame(maxWidth: .infinity, alignment: .leading).accessibilityAddTraits(.isHeader)
            Color.clear.frame(width: 56, height: 44).accessibilityHidden(true)
        }.frame(minHeight: 44).padding(.horizontal, 16).padding(.vertical, 12)
    }

    private var viewport: some View {
        GeometryReader { geometry in
            ZStack(alignment: .bottom) {
                ScrollViewReader { proxy in
                    ScrollView { feedList }
                        .refreshable { await actions.refresh() }
                        .accessibilityAction(named: Text(state.labels.refresh)) { actions.perform("refresh") }
                        .onChange(of: state.sessionGeneration) { _, _ in
                            frames = [:]
                            paginationRevision = nil
                            reportVisibility()
                            proxy.scrollTo("community-feed-top", anchor: .top)
                        }
                }
                if pinnedSignIn { signInButton }
            }
            .coordinateSpace(name: viewportSpace)
            .onPreferenceChange(CommunityFeedFrames.self) { frames = $0; reportVisibility() }
            .onPreferenceChange(CommunityFeedSignInHeight.self) { signInHeight = $0; reportVisibility() }
            .onAppear { viewportSize = geometry.size; reportVisibility() }
            .onChange(of: geometry.size) { _, size in viewportSize = size; reportVisibility() }
        }
    }

    private var feedList: some View {
        LazyVStack(alignment: .leading, spacing: 12) {
            Color.clear.frame(height: 0).id("community-feed-top").accessibilityHidden(true)
            ForEach(state.confirmedIds, id: \.self) { id in
                CommunityFeedConfirmedCard(id: id, labels: state.labels, perform: actions.perform).padding(.horizontal, 16)
            }
            if state.loading && state.rows.isEmpty {
                ProgressView(state.labels.loading).tint(CoffeePeekNativeStyle.foreground)
                    .frame(maxWidth: .infinity).padding(.top, 8)
            } else {
                ForEach(Array(state.rows.enumerated()), id: \.element.id) { index, row in
                    feedRow(row, index: index)
                }
                CommunityFeedNativeStatus(state: state, perform: actions.perform)
            }
        }
        .padding(.top, 8).padding(.bottom, max(0, state.bottomClearance) + 88)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func feedRow(_ row: CommunityFeedNativeRow, index: Int) -> some View {
        VStack(spacing: 12) {
            if index > 0 && !row.teaser { Divider().overlay(CoffeePeekNativeStyle.border).padding(.horizontal, 16) }
            CommunityFeedNativeRowView(row: row, signInLabel: state.labels.signIn) { actions.perform("signIn") }
        }
        .background {
            GeometryReader { geometry in
                Color.clear.preference(key: CommunityFeedFrames.self,
                    value: row.key.isEmpty ? [:] : [row.key: geometry.frame(in: .named(viewportSpace))])
            }
        }
    }

    private var pinnedSignIn: Bool { state.guest && !state.rows.isEmpty }

    private var signInButton: some View {
        Button { actions.perform("signIn") } label: {
            Text(state.labels.signIn).font(CoffeePeekNativeStyle.font(16, weight: "Semibold"))
                .multilineTextAlignment(.center).frame(maxWidth: .infinity, minHeight: 32)
        }
        .buttonStyle(.borderedProminent).buttonBorderShape(.capsule).controlSize(.large).foregroundStyle(.black)
        .background {
            GeometryReader { geometry in
                Color.clear.preference(key: CommunityFeedSignInHeight.self, value: geometry.size.height)
            }
        }
        .padding(.horizontal, 16).padding(.bottom, max(0, state.bottomClearance) + 12)
    }

    private func reportVisibility() {
        let usable = appeared && scenePhase == .active && state.active && state.interactive
        let bottom = max(0, state.bottomClearance) + (pinnedSignIn ? signInHeight + 24 : 0)
        let viewport = CGRect(origin: .zero, size: CGSize(width: viewportSize.width, height: max(0, viewportSize.height - bottom)))
        var next: [String: Double] = [:]
        if usable {
            next = frames.mapValues { visibleFraction($0, inside: viewport) }
            let feedVisible = !state.loading || !state.rows.isEmpty || !state.confirmedIds.isEmpty
            next["feed"] = feedVisible ? visibleFraction(CGRect(origin: .zero, size: viewportSize), inside: viewport) : 0
        }
        for key in Set(reported.keys).union(next.keys) {
            let fraction = next[key] ?? 0
            let previous = reported[key] ?? 0
            let thresholdChanged = (previous >= 0.1) != (fraction >= 0.1)
            let visibilityChanged = (previous > 0) != (fraction > 0)
            if abs(fraction - previous) > 0.001 || thresholdChanged || visibilityChanged { actions.visible(key, fraction) }
        }
        reported = next
        if usable { loadMoreIfNeeded(next) }
    }

    private func visibleFraction(_ bounds: CGRect, inside viewport: CGRect) -> Double {
        guard bounds.width > 0, bounds.height > 0 else { return 0 }
        let intersection = bounds.intersection(viewport)
        guard !intersection.isNull, !intersection.isEmpty else { return 0 }
        return min(1, max(0, Double(intersection.width * intersection.height / (bounds.width * bounds.height))))
    }

    private func loadMoreIfNeeded(_ visible: [String: Double]) {
        guard state.canLoadMore, !state.guest, !state.loading, !state.refreshing, !state.loadingMore,
              state.labels.errorText.isEmpty, paginationRevision != state.revision else { return }
        let tail = state.rows.suffix(3)
        guard tail.contains(where: { (visible[$0.key] ?? 0) > 0 }) else { return }
        paginationRevision = state.revision
        actions.perform("loadMore")
    }
}

private struct CommunityFeedFrames: PreferenceKey {
    static var defaultValue: [String: CGRect] { [:] }
    static func reduce(value: inout [String: CGRect], nextValue: () -> [String: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { _, latest in latest })
    }
}

private struct CommunityFeedSignInHeight: PreferenceKey {
    static var defaultValue: CGFloat { 48 }
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = max(value, nextValue()) }
}

func communityFeedPreview(error: Bool = false, guest: Bool = false) -> CommunityFeedNativePresentation {
    var state = CommunityFeedNativePresentation(title: "Лента", guest: guest)
    state.labels = .init(loading: "Загрузка", errorTitle: "Что-то пошло не так", errorText: error ? "Попробуйте ещё раз." : "",
        retry: "Повторить", empty: "Здесь пока нет записей", signIn: "Войдите, чтобы читать ленту", refresh: "Обновить",
        confirmedTitle: "Чек-ин сохранён", confirmedBody: "Он скоро появится в ленте.", confirmedOpen: "Открыть")
    if !error {
        state.rows = (0..<2).map { index in
            var card = CoffeePeekCheckInCardData(id: "preview-\(index)", name: "CoffeePeek", avatar: nil, date: "18 ноября")
            card.title = "CoffeePeek"
            card.note = "Ягодная кислотность и сладкое послевкусие."
            card.rating = 4.5
            card.ratingLabel = "4,5 из 5"
            card.likes = 12
            card.drink = "Фильтр"
            return .init(id: card.id, key: card.id, preview: card, teaser: guest && index == 1)
        }
    }
    state.bottomClearance = 88
    return state
}

#Preview("Native feed light") {
    CommunityFeedNativeContent(state: communityFeedPreview(), actions: .init()).preferredColorScheme(.light)
}
#Preview("Native feed dark") {
    CommunityFeedNativeContent(state: communityFeedPreview(), actions: .init()).preferredColorScheme(.dark)
}
#Preview("Native guest feed light") {
    CommunityFeedNativeContent(state: communityFeedPreview(guest: true), actions: .init()).preferredColorScheme(.light)
}
#Preview("Native guest feed dark") {
    CommunityFeedNativeContent(state: communityFeedPreview(guest: true), actions: .init()).preferredColorScheme(.dark)
}

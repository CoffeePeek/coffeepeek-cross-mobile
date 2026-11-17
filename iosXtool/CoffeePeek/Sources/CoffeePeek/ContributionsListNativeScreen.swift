import ComposeApp
import SwiftUI

struct ContributionsListHost: View {
    let route: IosContributionsList
    let strings: ContributionsStrings
    @State private var state: IosContributionsListState?
    @State private var observation: IosContributionObservation?

    init(route: IosContributionsList, strings: ContributionsStrings) {
        self.route = route
        self.strings = strings
    }

    var body: some View {
        ContributionsListNativeScreen(
            kind: route.kind, state: state, strings: strings, back: route.back, signIn: route.signIn,
            refresh: route.refresh, select: { route.select(status: $0) }, loadMore: { route.loadMore(status: $0) },
            open: { route.open(id: $0) }
        )
        .onAppear(perform: observe)
        .onDisappear(perform: stop)
    }

    private func observe() {
        guard observation == nil else { return }
        observation = route.observe { [binding = $state] snapshot in binding.wrappedValue = snapshot }
    }

    private func stop() {
        observation?.cancel()
        observation = nil
        state = nil
    }
}

private struct ContributionsListNativeScreen: View {
    let kind: String
    let state: IosContributionsListState?
    let strings: ContributionsStrings
    let back: () -> Void
    let signIn: () -> Void
    let refresh: () -> Void
    let select: (String) -> Void
    let loadMore: (String) -> Void
    let open: (String) -> Void

    var body: some View {
        ContributionsScaffold(title: strings.text("title_\(kind)"), backLabel: strings.text("back"), onBack: back) {
            if let state, !state.loading {
                content(state)
            } else {
                ProgressView(strings.text("loading"))
            }
        }
    }

    @ViewBuilder
    private func content(_ state: IosContributionsListState) -> some View {
        if let error = state.error {
            let needsSignIn = error == "sign_in_prompt"
            ContributionsMessage(
                text: needsSignIn ? strings.text(error) : (state.serverMessage ?? strings.text(error)),
                actionTitle: strings.text(needsSignIn ? "sign_in" : "retry"),
                onAction: needsSignIn ? signIn : refresh
            )
        } else if let selected = state.selected {
            VStack(spacing: 0) {
                tabs(state.tabs, selected: selected)
                rows(state, selected: selected)
            }
        } else {
            ScrollView {
                Text(strings.text("empty_\(kind)"))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(24)
            }
            .refreshable { refresh() }
        }
    }

    private func tabs(_ tabs: [IosContributionTab], selected: String) -> some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                ForEach(tabs, id: \.status) { tab in
                    Button {
                        select(tab.status)
                    } label: {
                        Text(strings.format("tab_label", strings.text("tab_\(tab.status)"), Int(tab.count)))
                            .font(.subheadline.weight(.semibold))
                            .padding(.horizontal, 14)
                            .frame(minHeight: 44)
                            .foregroundStyle(tab.status == selected ? Color.black : CoffeePeekNativeStyle.foreground)
                            .background(tab.status == selected ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.surface,
                                        in: Capsule())
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(tab.status == selected ? .isSelected : [])
                }
            }
            .padding(16)
        }
        .scrollIndicators(.hidden)
    }

    private func rows(_ state: IosContributionsListState, selected: String) -> some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(state.items, id: \.id) { item in
                    ContributionNativeCard(item: item, status: selected, kind: kind, strings: strings) { open(item.id) }
                }
                pageControl(state, selected: selected)
            }
            .padding(16)
        }
        .id(selected)
        .refreshable { refresh() }
    }

    @ViewBuilder
    private func pageControl(_ state: IosContributionsListState, selected: String) -> some View {
        if state.moreFailed {
            Button(strings.text("load_more_failed")) { loadMore(selected) }
                .frame(minHeight: 44)
        } else if state.loadingMore || state.hasMore {
            ProgressView(strings.text("loading"))
                .task(id: state.items.count) {
                    if state.hasMore && !state.loadingMore { loadMore(selected) }
                }
        }
    }
}

private struct ContributionNativeCard: View {
    let item: IosContributionItem
    let status: String
    let kind: String
    let strings: ContributionsStrings
    let open: () -> Void

    var body: some View {
        if item.canOpen {
            Button(action: open) { card }
                .buttonStyle(.plain)
        } else {
            card
        }
    }

    private var card: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(item.title ?? item.section.map { strings.text("section_\($0)") } ?? "")
                .font(.headline)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
            ContributionStatusLabel(status: status, strings: strings)
            if item.waiting {
                Text(strings.text("note_waiting")).font(.caption)
            } else if let date = item.date {
                Text(date).font(.caption)
            }
            if let reason = item.reason, !reason.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(strings.text("rejected_title")).font(.subheadline.weight(.semibold))
                Text(reason).font(.subheadline)
            }
            if item.canOpen {
                Label(strings.text(kind == "changes" ? "open_request" : "open_catalog"), systemImage: "arrow.right")
                    .font(.subheadline.weight(.semibold))
            }
        }
        .contributionsCard()
    }

    private var subtitle: String? {
        if let subtitle = item.subtitle { return subtitle }
        if let summary = item.summary { return strings.format(summary.key, Int(summary.count), Int(summary.extra)) }
        return item.section.map { strings.text("hint_\($0)") }
    }
}

private func contributionsListPreview() -> some View {
    ContributionsListNativeScreen(
        kind: "shops",
        state: IosContributionsListState(
            loading: false, error: nil, serverMessage: nil,
            tabs: [IosContributionTab(status: "approved", count: 1)], selected: "approved",
            items: [IosContributionItem(id: "preview", title: "Sandbox Cafe", subtitle: nil, section: nil, summary: nil,
                                       date: nil, waiting: false, reason: nil, canOpen: true)],
            hasMore: false, loadingMore: false, moreFailed: false
        ),
        strings: ContributionsStrings(languageTag: "ru"), back: {}, signIn: {}, refresh: {},
        select: { _ in }, loadMore: { _ in }, open: { _ in }
    )
}

#Preview("Contributions light") { contributionsListPreview().preferredColorScheme(.light) }
#Preview("Contributions dark") { contributionsListPreview().preferredColorScheme(.dark) }

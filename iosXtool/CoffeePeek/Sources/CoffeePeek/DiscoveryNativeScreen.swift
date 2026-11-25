import ComposeApp
import SwiftUI
import UIKit

struct DiscoveryNativeScreen: View {
    let state: IosDiscoveryState
    let actions: DiscoveryNativeActions
    let map: AnyView
    let roasters: (Bool) -> AnyView
    var roasterSource: IosRoasterDiscovery? = nil
    @FocusState private var queryFocused: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var resultsFrame: CGRect = .zero
    @State private var headerHeight: CGFloat = 0
    @State private var tailVisible = false
    @GestureState private var listDragging = false
    @State private var exposed: Set<String> = []
    @ScaledMetric(relativeTo: .body) private var searchControlHeight: CGFloat = 48

    private func text(_ key: String) -> String { state.labels[key] ?? "" }

    var body: some View {
        GeometryReader { geometry in
            screen(in: geometry.size)
        }
        .font(CoffeePeekNativeStyle.font(14))
        .background(CoffeePeekNativeStyle.background)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .allowsHitTesting(state.active)
        .onChange(of: listDragging) { _, dragging in actions.call(dragging ? "scroll-start" : "scroll-end") }
        .onChange(of: state.mapExpanded) { _, _ in queryFocused = false }
        .onChange(of: state.roastersExpanded) { _, _ in queryFocused = false }
        .accessibilityAction(.escape) { dismiss() }
        .onAppear { for feature in exposed { actions.visible(feature, true) } }
        .onDisappear { for feature in exposed { actions.visible(feature, false) } }
    }

    private var modeAnimation: Animation? { reduceMotion ? nil : .smooth(duration: 0.5, extraBounce: 0) }
    private var mapAnimation: Animation? { modeAnimation }
    private var catalogueTransition: AnyTransition {
        reduceMotion ? .identity : .offset(y: DiscoveryMapLayout.previewTravel)
    }
    private var headerTransition: AnyTransition {
        reduceMotion ? .identity : .move(edge: .top).combined(with: .opacity)
    }
    private var bottomTransition: AnyTransition {
        reduceMotion ? .identity : .move(edge: .bottom)
    }
    private var showsLanding: Bool { state.landing && !state.picker }
    private var hasQueryText: Bool { !state.query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    private var showsMapPreview: Bool { showsLanding && !state.roastersExpanded }
    private var showsQuickFilterDivider: Bool { !state.mapExpanded && !state.picker && !state.roastersExpanded }
    private var pickerListAnimation: Animation? { state.picker && !reduceMotion ? .easeInOut(duration: 0.18) : nil }
    private var pickerShopIds: [String] { (state.recent + state.shops).map(\.id) }
    private var acceptedPickerCriteria: DiscoveryPickerResultCriteria {
        DiscoveryPickerResultCriteria(query: state.pickerResultQuery, nearby: state.pickerResultNearby)
    }
    private let mapPreviewHeight = DiscoveryMapLayout.previewHeight

    private func screen(in size: CGSize) -> some View {
        ZStack(alignment: .top) {
            catalogueLayer(in: size)
                .overlayPreferenceValue(DiscoveryMapAnchors.self) { anchors in mapLayer(anchors) }
            if state.mapExpanded { expandedMap.transition(bottomTransition) }
            header.onGeometryChange(for: CGFloat.self) { $0.size.height } action: { next in
                withAnimation(headerHeight == 0 ? nil : modeAnimation) { headerHeight = next }
            }
            if showsContinueCheckIn { continueCheckIn.transition(bottomTransition) }
            notices
        }
        .frame(width: size.width, height: size.height, alignment: .top)
        .animation(mapAnimation, value: state.mapExpanded)
        .animation(modeAnimation, value: state.roastersExpanded)
        .coordinateSpace(name: "discovery")
        .onPreferenceChange(DiscoveryResultsFrame.self) { resultsFrame = $0 }
        .onPreferenceChange(DiscoveryExposureFrames.self) { frames in updateExposure(frames, size: size) }
    }

    private func catalogueLayer(in size: CGSize) -> some View {
        VStack(spacing: 0) {
            Color.clear.frame(height: headerHeight).accessibilityHidden(true)
            ZStack(alignment: .top) {
                results(catalogueOffset: state.mapExpanded ? size.height : 0).simultaneousGesture(listDrag)
                    .opacity(state.roastersExpanded ? 0 : 1)
                    .allowsHitTesting(!state.mapExpanded && !state.roastersExpanded)
                    .accessibilityHidden(state.mapExpanded || state.roastersExpanded)
                if state.roastersExpanded {
                    roasters(true)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                        .transition(catalogueTransition)
                }
            }
        }
    }

    private var header: some View {
        VStack(spacing: state.picker ? 12 : 8) {
            if state.picker { pickerHeader }
            searchHeader(expanded: state.mapExpanded)
            if !state.roastersExpanded {
                chips.transition(headerTransition)
            }
        }
        .padding(.top, 12)
        .padding(.bottom, 8)
        .background { CoffeePeekNativeStyle.background.opacity(state.mapExpanded ? 0 : 1) }
        .overlay(alignment: .bottom) {
            if showsQuickFilterDivider {
                Rectangle().fill(CoffeePeekNativeStyle.border.opacity(0.7))
                    .frame(height: 1).allowsHitTesting(false).accessibilityHidden(true)
            }
        }
    }

    private func updateExposure(_ frames: [String: DiscoveryExposureFrame], size: CGSize) {
        if !state.mapExpanded && !state.roastersExpanded { actions.call("scroll") }
        let viewport = CGRect(origin: .zero, size: size)
        let eligible = frames.filter { id, _ in
            if state.roastersExpanded { return false }
            let onMap = id == "map-full" || id == "search-map" || id == "filters-map"
            return state.mapExpanded == onMap
        }
        let visible = Set(eligible.values.filter { frame in
            let area = frame.rect.width * frame.rect.height
            let clip = frame.feature == "map" && !state.mapExpanded
                ? discoveryMapClipFrame(viewport: size, results: resultsFrame, expanded: false)
                : viewport
            let intersection = frame.rect.intersection(clip)
            return area > 0 && !intersection.isNull && intersection.width * intersection.height / area >= 0.1
        }.map(\.feature))
        for feature in exposed.subtracting(visible) { actions.visible(feature, false) }
        for feature in visible.subtracting(exposed) { actions.visible(feature, true) }
        exposed = visible
    }

    private var pickerHeader: some View {
        HStack(spacing: 12) {
            icon("chevron.left", label: text("cancel")) { actions.call("back") }
            Text(text("title")).font(CoffeePeekNativeStyle.font(20, weight: "SemiBold", relativeTo: .title3))
                .frame(maxWidth: .infinity)
            icon("xmark", label: text("close")) { actions.call("close") }
        }.padding(.horizontal, 16)
    }

    private var listDrag: some Gesture {
        DragGesture(minimumDistance: 1).updating($listDragging) { _, dragging, _ in dragging = true }
    }

    private func searchHeader(expanded: Bool) -> some View {
        let showsCancel = expanded || state.roastersExpanded || (!state.picker && (!state.landing || queryFocused))
        return HStack(spacing: 8) {
            if showsCancel {
                icon(expanded || queryFocused ? "xmark" : "chevron.left",
                     label: text(expanded ? "collapse-map" : "cancel"), glass: true) {
                    queryFocused = false
                    dismiss()
                }
                .transition(reduceMotion ? .identity : .offset(x: -12).combined(with: .opacity))
            }
            searchField(expanded: expanded)
            if !state.picker && !state.roastersExpanded { filterButton(expanded: expanded).transition(.opacity) }
        }.padding(.horizontal, 16)
            .animation(modeAnimation, value: showsCancel)
    }

    private func searchField(expanded: Bool) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(CoffeePeekNativeStyle.secondary).accessibilityHidden(true)
            searchInput
            if !state.query.isEmpty { clearSearch }
        }
        .padding(.horizontal, 12).frame(height: searchControlHeight)
        .background {
            if expanded {
                Color.clear.discoveryGlass(in: Capsule())
            } else {
                Capsule().fill(CoffeePeekNativeStyle.surface)
                    .overlay { Capsule().strokeBorder(CoffeePeekNativeStyle.border, lineWidth: 1) }
            }
        }
        .discoveryExposure("search", id: expanded ? "search-map" : "search-list")
        .roasterExposure("search", source: state.roastersExpanded ? roasterSource : nil)
    }

    private var searchPlaceholder: String {
        guard state.roastersExpanded, let source = roasterSource else { return text("search") }
        return OwnProfileStrings(languageTag: source.languageTag(), table: "NativeRoaster")["roaster_search_placeholder"]
    }

    private var searchInput: some View {
        TextField(searchPlaceholder, text: Binding(get: { state.query }, set: { actions.query($0) }))
            .textInputAutocapitalization(.never).autocorrectionDisabled().submitLabel(.search)
            .focused($queryFocused).onSubmit { queryFocused = false }
            .onChange(of: queryFocused) { _, focused in
                if focused && !state.roastersExpanded { actions.call("activate") }
            }
    }

    private var clearSearch: some View {
        Button { actions.query("") } label: { Image(systemName: "xmark.circle.fill") }
            .foregroundStyle(CoffeePeekNativeStyle.secondary).accessibilityLabel(text("clear"))
    }

    private func filterButton(expanded: Bool) -> some View {
        icon("slider.horizontal.3", label: text("filters"), glass: true) {
            queryFocused = false
            actions.call("filters")
        }
            .overlay(alignment: .topTrailing) { filterBadge }
            .discoveryExposure("filters", id: expanded ? "filters-map" : "filters-list")
    }

    @ViewBuilder private var filterBadge: some View {
        if state.activeFilterCount > 0 {
            Text(String(state.activeFilterCount)).font(CoffeePeekNativeStyle.font(10, weight: "Bold"))
                .foregroundStyle(.black).padding(4).background(CoffeePeekNativeStyle.accent, in: Circle())
        }
    }

    private var chips: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                if state.picker {
                    DiscoveryQuickFilterChip(title: text("nearby"), id: "nearby",
                        selected: !state.mapExpanded && state.chips.first(where: { $0.id == "nearby" })?.selected == true) {
                        queryFocused = false
                        actions.call("nearby")
                    }
                    DiscoveryQuickFilterChip(title: text(state.mapExpanded ? "list" : "map"),
                        id: state.mapExpanded ? "list" : "map", selected: state.mapExpanded) {
                        queryFocused = false; actions.call("map")
                    }
                } else {
                    ForEach(state.chips, id: \.id) { item in
                        DiscoveryQuickFilterChip(title: item.title, id: item.id, selected: item.selected) { actions.call(item.id) }
                    }
                }
            }.padding(.horizontal, 16).padding(.vertical, state.picker ? 0 : 4)
        }.scrollIndicators(.hidden)
    }

    private func results(catalogueOffset: CGFloat) -> some View {
        ScrollViewReader { proxy in
            ScrollView {
                resultRows(catalogueOffset: catalogueOffset)
                    .coffeePeekRefreshable(isRefreshing: state.refreshing, label: text("loading")) { actions.call("refresh") }
            }
            .scrollDismissesKeyboard(.interactively)
            .clipped()
            .transformAnchorPreference(key: DiscoveryMapAnchors.self, value: .bounds) { value, anchor in
                value.results = anchor
            }
            .background {
                GeometryReader { geometry in
                    Color.clear.preference(key: DiscoveryResultsFrame.self, value: geometry.frame(in: .named("discovery")))
                }
            }
            .onChange(of: state.scrollRevision) { _, _ in queryFocused = false; proxy.scrollTo("discovery-top", anchor: .top) }
            .onChange(of: state.query) { _, _ in
                if !state.picker { proxy.scrollTo("discovery-top", anchor: .top) }
            }
            .onChange(of: acceptedPickerCriteria) { _, _ in
                if state.picker { withAnimation(pickerListAnimation) { proxy.scrollTo("discovery-top", anchor: .top) } }
            }
            .onChange(of: showsLanding) { _, _ in
                withAnimation(modeAnimation) { proxy.scrollTo("discovery-top", anchor: .top) }
            }
        }
    }

    private func resultRows(catalogueOffset: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Color.clear.frame(height: 0).id("discovery-top")
            if state.refreshing && !state.picker {
                CoffeePeekNativeLoader(label: text("loading")).frame(maxWidth: .infinity).padding(.bottom, 16)
            }
            mapPreview.padding(.vertical, showsMapPreview ? DiscoveryMapLayout.previewSpacing : 0)
            catalogueRows(offset: catalogueOffset)
        }
        .padding(.bottom, CGFloat(state.bottomClearance) + (showsContinueCheckIn ? 72 : 20))
        .animation(modeAnimation, value: showsMapPreview)
    }

    @ViewBuilder private func catalogueRows(offset: CGFloat) -> some View {
        if state.picker {
            catalogueContent.offset(y: offset)
                .animation(pickerListAnimation, value: pickerShopIds)
                .animation(pickerListAnimation, value: state.recent.isEmpty)
        } else { catalogueContent.offset(y: offset) }
    }

    private var catalogueContent: some View {
        LazyVStack(alignment: .leading, spacing: 16) {
            if showsLanding { landingContent.transition(.identity) }
            else { listContent.transition(.identity) }
        }
    }

    private var landingContent: some View {
        Group {
            section(text("shops"), action: "activate")
                .discoveryExposure("shops", id: "shops-heading")
            searchStatus
            if !state.shops.isEmpty { shopCarousel }
            if state.shops.isEmpty && state.canSuggestShop { contribution }
            roasterPreview
        }
    }

    private var mapPreview: some View {
        Button {
            queryFocused = false
            actions.call("map")
        } label: {
            Color.clear.frame(height: showsMapPreview ? mapPreviewHeight : 0)
                .contentShape(RoundedRectangle(cornerRadius: 24))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(text("map"))
        .padding(.horizontal, 16)
        .allowsHitTesting(showsMapPreview && !state.mapExpanded)
        .accessibilityHidden(!showsMapPreview || state.mapExpanded)
        .anchorPreference(key: DiscoveryMapAnchors.self, value: .bounds) { DiscoveryMapAnchors.Value(preview: $0) }
        .discoveryExposure("map", id: "map-preview")
    }

    private func mapLayer(_ anchors: DiscoveryMapAnchors.Value) -> some View {
        GeometryReader { geometry in
            let preview = anchors.preview.map { geometry[$0] } ?? .zero
            let results = anchors.results.map { geometry[$0] } ?? .zero
            let slot = CGRect(x: preview.minX, y: preview.minY, width: preview.width, height: mapPreviewHeight)
            let frame = discoveryMapLayoutFrame(viewport: geometry.size, preview: slot, expanded: state.mapExpanded,
                previewVisible: showsMapPreview, resultsTop: results.minY)
            let clip = discoveryMapClipFrame(viewport: geometry.size, results: results, expanded: state.mapExpanded)
            let target = frame ?? CGRect(x: 0, y: 0, width: 1, height: 1)
            map.background(CoffeePeekNativeStyle.surface)
                .overlay(alignment: .topTrailing) { expandIcon }
                .frame(width: target.width, height: target.height)
                .clipShape(RoundedRectangle(cornerRadius: state.mapExpanded ? 0 : 24))
                .position(x: target.midX, y: target.midY)
                .mask(alignment: .topLeading) {
                    Rectangle().frame(width: clip.width, height: clip.height).offset(x: clip.minX, y: clip.minY)
                }
                .opacity(frame != nil ? 1 : 0)
        }
        .ignoresSafeArea(.container, edges: .vertical)
        .allowsHitTesting(state.mapExpanded)
        .accessibilityHidden(!state.mapExpanded)
        .animation(modeAnimation, value: showsMapPreview)
    }

    @ViewBuilder private var expandIcon: some View {
        if showsMapPreview && !state.mapExpanded {
            Image(systemName: "arrow.up.left.and.arrow.down.right").font(.system(size: 17, weight: .semibold))
                .frame(width: 44, height: 44).discoveryGlass(in: Circle()).padding(8)
                .accessibilityHidden(true)
        }
    }

    @ViewBuilder private var listContent: some View {
        if state.showRoasterResults {
            roasterPreview
            section(text("shops"), action: nil)
        }
        if !state.recent.isEmpty {
            section(text("recent"), action: nil)
        }
        if !state.picker && state.emptyReason != nil && state.shops.isEmpty {
            emptySearchContent
        } else {
            ForEach(state.recent + state.shops, id: \.id) { shop in shopRow(shop) }
            searchStatus
            pagination
            if state.canSuggestShop { contribution }
        }
    }

    private var roasterPreview: some View {
        Group {
            section(text("roasters"), action: "roasters")
            roasters(false)
        }
    }

    private var showsContinueCheckIn: Bool {
        state.continueCheckIn && state.active && !state.mapExpanded && !state.roastersExpanded && !queryFocused
    }

    private var continueCheckIn: some View {
        Button { actions.call("continue") } label: {
            Text(text("continue"))
                .font(CoffeePeekNativeStyle.font(15, weight: "Semibold"))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 16)
                .frame(minHeight: 44)
                .background(CoffeePeekNativeStyle.accent, in: Capsule())
                .foregroundStyle(.black)
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 16)
        .padding(.bottom, CGFloat(state.bottomClearance) + 8)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
        .ignoresSafeArea(.container, edges: .bottom)
    }

    private var shopCarousel: some View {
        ScrollView(.horizontal) {
            HStack(alignment: .top, spacing: 12) {
                ForEach(Array(state.shops.prefix(8)), id: \.id) { shop in
                    DiscoveryShopCard(shop: shop, picker: false, compact: false, labels: state.labels, actions: actions, catalogDetails: false)
                        .frame(width: 280)
                }
            }.padding(.horizontal, 16)
        }.scrollIndicators(.hidden)
    }

    private func shopRow(_ shop: IosDiscoveryShop) -> some View {
        DiscoveryShopCard(shop: shop, picker: state.picker, compact: state.picker, labels: state.labels, actions: actions)
            .padding(.horizontal, 16)
            .transition(state.picker && !reduceMotion ? .opacity : .identity)
    }

    @ViewBuilder private var searchStatus: some View {
        if state.loading && state.shops.isEmpty && state.recent.isEmpty {
            CoffeePeekNativeLoader(label: text("loading")).frame(maxWidth: .infinity).padding(20)
        } else if state.failed {
            feedback(text(state.shops.isEmpty ? "failed" : "refresh-failed"), action: "refresh", label: text("retry"))
        } else if state.picker && state.emptyReason != nil && state.shops.isEmpty && state.recent.isEmpty {
            Text(text("empty")).foregroundStyle(CoffeePeekNativeStyle.secondary).padding(.horizontal, 16)
        }
    }

    private var pagination: some View {
        VStack(spacing: 8) {
            if state.loadingMore { CoffeePeekNativeLoader(label: text("loading")) }
            if state.loadMoreFailed { feedback(text("more-failed"), action: "retry-more", label: text("retry")) }
            Color.clear.frame(height: 1).onAppear { tailVisible = true; loadMore() }
                .onDisappear { tailVisible = false }
        }
        .onChange(of: state.canLoadMore) { _, _ in loadMore() }
        .onChange(of: state.shops.count) { _, _ in loadMore() }
        .onChange(of: state.mapExpanded) { _, _ in loadMore() }
        .onChange(of: state.active) { _, _ in loadMore() }
    }

    private var contribution: some View {
        contributionCards.padding(.horizontal, 16)
    }

    private var contributionCards: some View {
        VStack(spacing: 12) {
            DiscoveryCatalogSuggestionCard(title: text("add-title"), description: text("add-body"), actionLabel: text("add")) {
                queryFocused = false
                actions.call("add")
            }
            if state.canSuggestRoaster {
                DiscoveryCatalogSuggestionCard(title: text("add-roaster-title"), description: text("add-roaster-body"),
                    actionLabel: text("add-roaster")) {
                    queryFocused = false
                    actions.call("add-roaster")
                }
            }
        }
    }

    private var emptySearchContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            if !state.allResultsEmpty { emptySearchRecovery }
            contributionCards
        }
        .padding(.horizontal, 16).padding(.vertical, 24).padding(.top, 16)
    }

    private var emptySearchRecovery: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(text("empty-title-\(state.emptyReason ?? "Catalog")"))
                .font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2)).accessibilityAddTraits(.isHeader)
            Text(text("empty-\(state.emptyReason ?? "Catalog")"))
                .font(CoffeePeekNativeStyle.font(17)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            if state.emptyReason == "Filters" {
                CoffeePeekNativeActionButton(title: text("reset")) { actions.call("reset") }
                if hasQueryText { editQueryButton(primary: false) }
            } else if hasQueryText { editQueryButton(primary: true) }
        }
    }

    @ViewBuilder private func editQueryButton(primary: Bool) -> some View {
        if primary {
            CoffeePeekNativeActionButton(title: text("edit-query")) { queryFocused = true }
        } else {
            Button(text("edit-query")) { queryFocused = true }
                .font(CoffeePeekNativeStyle.font(17, weight: "Semibold")).frame(minHeight: 48)
                .tint(CoffeePeekNativeStyle.accent)
        }
    }

    private func loadMore() { if tailVisible && state.canLoadMore && state.active && !state.mapExpanded && !state.roastersExpanded { actions.call("more") } }
    private func dismiss() {
        queryFocused = false
        actions.call(state.roastersExpanded ? "cancel-roasters" : state.mapExpanded ? "map" : state.picker ? "back" : "cancel")
    }

    private func section(_ title: String, action: String?) -> some View {
        HStack {
            Text(title).font(CoffeePeekNativeStyle.font(22, weight: "SemiBold", relativeTo: .title2))
                .accessibilityAddTraits(.isHeader)
            Spacer()
            if let action { showAllButton(action) }
        }.padding(.horizontal, 16)
    }

    private func showAllButton(_ action: String) -> some View {
        Button { actions.call(action) } label: {
            Text(text("show-all")).font(CoffeePeekNativeStyle.font(13, weight: "SemiBold"))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .padding(.horizontal, 12).padding(.vertical, 7)
                .background(CoffeePeekNativeStyle.surface, in: Capsule())
                .overlay { Capsule().strokeBorder(CoffeePeekNativeStyle.border, lineWidth: 1) }
                .frame(minHeight: 44).contentShape(Rectangle())
        }.buttonStyle(.plain)
    }

    func feedback(_ message: String, action: String, label: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(message).accessibilityAddTraits(.updatesFrequently)
            Button(label) { actions.call(action) }
        }.padding(12).frame(maxWidth: .infinity, alignment: .leading)
            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16)).padding(.horizontal, 16)
    }

    func icon(_ symbol: String, label: String, glass: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            let glyph = Image(systemName: symbol).font(.system(size: 20))
                .frame(width: searchControlHeight, height: searchControlHeight)
            if glass { glyph.discoveryGlass(in: Circle()) }
            else { glyph.background(.regularMaterial, in: Circle()) }
        }.buttonStyle(.plain).accessibilityLabel(label)
    }

    private var expandedMap: some View {
        DiscoveryMapControls(state: state, actions: actions, headerHeight: headerHeight)
            .discoveryExposure("map", id: "map-full")
    }

    @ViewBuilder private var notices: some View {
        if !state.roastersExpanded {
            VStack {
                Spacer()
                if let message = state.locationNotice {
                    HStack(alignment: .top, spacing: 12) {
                        Text(message).frame(maxWidth: .infinity, alignment: .leading)
                        if state.settingsNeeded { Button(text("settings")) { actions.call("settings") } }
                        Button { actions.call("dismiss-location") } label: { Image(systemName: "xmark") }
                            .accessibilityLabel(text("dismiss")).frame(minWidth: 44, minHeight: 44)
                    }.padding(12).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
                        .padding(.horizontal, 16)
                } else if state.favoriteFailure && !state.mapExpanded {
                    feedback(text("favorite-failed"), action: "dismiss-favorite", label: text("dismiss"))
                }
            }.padding(.bottom, CGFloat(state.bottomClearance) + (state.mapExpanded ? 16 : 76))
        }
    }
}

/// Native rendering of design-system CoffeePeekCatalogSuggestionCard.
private struct DiscoveryCatalogSuggestionCard: View {
    let title: String
    let description: String
    let actionLabel: String
    let action: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            illustration.frame(width: 132, height: 132).frame(maxWidth: .infinity).accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 8) {
                Text(title).font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
                    .foregroundStyle(CoffeePeekNativeStyle.foreground).accessibilityAddTraits(.isHeader)
                Text(description).font(CoffeePeekNativeStyle.font(17))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
            .fixedSize(horizontal: false, vertical: true).padding(.trailing, 20).padding(.top, 12)
            Button(action: action) {
                HStack(spacing: 8) {
                    Image(systemName: "plus").font(.system(size: 20)).accessibilityHidden(true)
                    Text(actionLabel).font(CoffeePeekNativeStyle.font(17, weight: "Semibold"))
                }
                .padding(.vertical, 8).frame(minHeight: 48).contentShape(Rectangle())
            }
            .buttonStyle(.plain).foregroundStyle(CoffeePeekNativeStyle.accent).padding(.top, 8)
        }
        .padding(.horizontal, 16).padding(.vertical, 24).frame(maxWidth: .infinity, alignment: .leading)
        .background(CoffeePeekNativeStyle.surfaceVariant.opacity(0.55), in: RoundedRectangle(cornerRadius: 20))
    }

    @ViewBuilder private var illustration: some View {
        if let image = Self.catalogIllustration { Image(uiImage: image).resizable().scaledToFit() }
        else { Image(systemName: "magnifyingglass").font(.system(size: 56)).foregroundStyle(CoffeePeekNativeStyle.secondaryAccent) }
    }

    private static let catalogIllustration: UIImage? = {
        let resource = "com.coffeepeek.core.designsystem.resources/drawable"
        for prefix in ["compose-resources/composeResources", "compose-resources"] {
            if let url = Bundle.main.url(forResource: "maskot_with_magnifying_glass", withExtension: "png",
                                         subdirectory: "\(prefix)/\(resource)"), let image = UIImage(contentsOfFile: url.path) {
                return image
            }
        }
        return nil
    }()
}

private struct DiscoveryQuickFilterChip: View {
    let title: String
    let id: String
    let selected: Bool
    let action: () -> Void
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: action) {
            styledCaption.frame(minWidth: 48, minHeight: 48).contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.18), value: selected)
    }

    private var caption: some View {
        HStack(spacing: 6) {
            DiscoveryChipArtwork(id: id)
                .foregroundStyle(selected ? CoffeePeekNativeStyle.onAccent : CoffeePeekNativeStyle.secondaryAccent)
            Text(title)
                .font(CoffeePeekNativeStyle.font(15, weight: "Medium", relativeTo: .callout))
                .foregroundStyle(selected ? CoffeePeekNativeStyle.onAccent : CoffeePeekNativeStyle.foreground)
        }
        .fixedSize(horizontal: true, vertical: false)
        .padding(.horizontal, 12).padding(.vertical, 10)
        .frame(minHeight: 44)
    }

    @ViewBuilder private var styledCaption: some View {
        if !selected {
            caption.discoveryGlass(in: Capsule())
        } else if #available(iOS 26.0, *) {
            caption.glassEffect(.regular.tint(CoffeePeekNativeStyle.accent).interactive(), in: Capsule())
        } else {
            caption.background(CoffeePeekNativeStyle.accent, in: Capsule())
        }
    }
}

private struct DiscoveryPickerResultCriteria: Equatable {
    let query: String?
    let nearby: Bool
}

private enum DiscoveryMapAnchors: PreferenceKey {
    struct Value {
        var preview: Anchor<CGRect>?
        var results: Anchor<CGRect>?
    }
    static var defaultValue: Value { Value() }
    static func reduce(value: inout Value, nextValue: () -> Value) {
        let next = nextValue()
        value.preview = next.preview ?? value.preview
        value.results = next.results ?? value.results
    }
}

private struct DiscoveryResultsFrame: PreferenceKey {
    static var defaultValue: CGRect { .zero }
    static func reduce(value: inout CGRect, nextValue: () -> CGRect) { value = nextValue() }
}

struct DiscoveryExposureFrame: Equatable {
    let feature: String
    let rect: CGRect
}

struct DiscoveryExposureFrames: PreferenceKey {
    static var defaultValue: [String: DiscoveryExposureFrame] { [:] }
    static func reduce(value: inout [String: DiscoveryExposureFrame], nextValue: () -> [String: DiscoveryExposureFrame]) {
        value.merge(nextValue(), uniquingKeysWith: { _, next in next })
    }
}

extension View {
    func discoveryExposure(_ feature: String, id: String) -> some View {
        background {
            GeometryReader { geometry in
                Color.clear.preference(key: DiscoveryExposureFrames.self,
                    value: [id: DiscoveryExposureFrame(feature: feature, rect: geometry.frame(in: .named("discovery")))])
            }
        }
    }
}


func discoveryPreviewState(picker: Bool = false, expanded: Bool = false, canContinue: Bool = false,
                           pickerNearby: Bool = true, emptyReason: String? = nil,
                           allResultsEmpty: Bool = false, allShops: Bool = false, allRoasters: Bool = false) -> IosDiscoveryState {
    let shop = discoveryPreviewShop()
    return IosDiscoveryState(picker: picker, active: true, query: emptyReason == "Query" || emptyReason == "Filters" ? "coffee" : "",
        landing: !picker && emptyReason == nil && !allShops,
        mapExpanded: expanded, roastersExpanded: allRoasters, showRoasterResults: false, canSuggestRoaster: allResultsEmpty,
        scrollRevision: 0, bottomClearance: picker ? 0 : 84, loading: false, refreshing: false, loadingMore: false,
        canLoadMore: false, failed: false, loadMoreFailed: false, canSuggestShop: emptyReason != nil, emptyReason: emptyReason,
        locationNotice: nil, settingsNeeded: false, favoriteFailure: false, continueCheckIn: canContinue,
        shops: emptyReason != nil ? [] : picker ? discoveryPickerPreviewShops(nearby: pickerNearby) : [shop], recent: [],
        mapShops: [shop], selectedShopId: "preview", suggestions: [],
        showSuggestions: false, searchFailed: false, searchLoading: false, selectedZone: nil, zonesVisible: true,
        heatmapVisible: false, heatmapLoading: false, heatmapFailed: false, viewportFailed: false, truncated: false,
        chips: discoveryPreviewChips(selected: picker ? (pickerNearby ? "nearby" : "") : "open-only"), activeFilterCount: 1,
        labels: ["title": "Выберите кофейню", "search": "Поиск кофеен и обжарщиков…", "shops": "Кофейни",
            "roasters": "Обжарщики", "show-all": "Показать все", "open": "ОТКРЫТО", "new": "Новая",
            "nearby": "Рядом со мной", "map": "На карте", "list": "Списком", "filters": "Фильтры",
            "continue": "Продолжить чекин"].merging(discoveryEmptyPreviewLabels) { _, next in next },
        pickerResultQuery: nil, pickerResultNearby: picker && pickerNearby, allResultsEmpty: allResultsEmpty)
}

private var discoveryEmptyPreviewLabels: [String: String] {
    ["add-title": "Не нашли нужную кофейню?",
     "add-body": "Предложите добавить её в CoffeePeek — после проверки она появится в каталоге.",
     "add": "Добавить кофейню", "add-roaster-title": "Не нашли нужного обжарщика?",
     "add-roaster-body": "Предложите добавить его в CoffeePeek — после проверки он появится в каталоге.",
     "add-roaster": "Добавить обжарщика", "empty-title-Catalog": "Кофеен пока нет",
     "empty-Catalog": "В выбранном городе пока нет кофеен в каталоге.", "empty-title-Query": "Кофейни не найдены",
     "empty-Query": "Попробуйте другое название или проверьте запрос.",
     "empty-title-Filters": "Нет кофеен с такими фильтрами",
     "empty-Filters": "Снимите фильтры или измените запрос, чтобы расширить поиск.",
     "edit-query": "Изменить запрос", "reset": "Сбросить фильтры"]
}

private func discoveryPreviewShop(id: String = "preview", title: String = "CoffeePeek Specialty",
                                  distance: String = "350 м") -> IosDiscoveryShop {
    IosDiscoveryShop(id: id, title: title, photoUrl: nil,
        address: "Тихая улица, 12", rating: "4.8", detail: nil, distance: distance, kind: "Specialty",
        tags: ["V60", "Эспрессо"], favorite: true, favoriteBusy: false,
        favoriteLabel: "Убрать из избранного", open: true, isNew: true, hours: "Открыто · 09:00 – 22:00", decoration: nil)
}

private func discoveryPickerPreviewShops(nearby: Bool) -> [IosDiscoveryShop] {
    let first = discoveryPreviewShop()
    let second = discoveryPreviewShop(id: "second", title: "Coffee Room", distance: "500 м")
    let third = discoveryPreviewShop(id: "third", title: "Puffin", distance: "800 м")
    return nearby ? [first, second, third] : [third, first]
}

private struct DiscoveryInteractionPreview: View {
    var picker = true
    private enum Mode { case landing, shops, roasters, map }
    @State private var nearby = true
    @State private var mode = Mode.landing

    var body: some View {
        DiscoveryNativeScreen(state: discoveryPreviewState(picker: picker, expanded: mode == .map, pickerNearby: nearby,
            allShops: mode == .shops, allRoasters: mode == .roasters),
            actions: DiscoveryNativeActions(perform: { name, _ in perform(name) }),
            map: AnyView(CoffeePeekNativeStyle.border),
            roasters: { _ in AnyView(CoffeePeekPhotoPlaceholder().frame(height: 160)) })
    }

    private func perform(_ name: String) {
        switch name {
        case "nearby":
            if mode != .map || !nearby { nearby.toggle() }
            mode = .landing
        case "map": mode = mode == .map ? .landing : .map
        case "activate": mode = .shops
        case "roasters": mode = .roasters
        case "cancel", "cancel-roasters": mode = .landing
        default: break
        }
    }
}

private func discoveryPreviewChips(selected: String) -> [IosDiscoveryChip] {
    [
        ("nearby", "Рядом"), ("open-only", "Открыто"), ("new", "Новые"), ("visited", "Уже был"),
        ("favorites", "Избранное"), ("Specialty", "Specialty"), ("CoffeeBar", "Кофейня"), ("Cafe", "Кафе"),
    ].map { IosDiscoveryChip(id: $0.0, title: $0.1, selected: $0.0 == selected) }
}

@MainActor
private func discoveryQuickFilterChipPreview() -> some View {
    let chips = discoveryPreviewChips(selected: "CoffeeBar")
    return VStack(alignment: .leading, spacing: 4) {
        ForEach([chips.prefix(3), chips.dropFirst(3).prefix(3), chips.suffix(2)], id: \.startIndex) { row in
            HStack(spacing: 8) {
                ForEach(Array(row), id: \.id) { item in
                    DiscoveryQuickFilterChip(title: item.title, id: item.id, selected: item.selected, action: {})
                }
            }
        }
    }
    .padding(16)
    .background(CoffeePeekNativeStyle.background)
}

#Preview("Discovery quick filters light") { discoveryQuickFilterChipPreview().preferredColorScheme(.light) }
#Preview("Discovery quick filters dark") { discoveryQuickFilterChipPreview().preferredColorScheme(.dark) }

@MainActor
private func discoveryScreenPreview(picker: Bool = false, expanded: Bool = false) -> some View {
    DiscoveryNativeScreen(state: discoveryPreviewState(picker: picker, expanded: expanded, canContinue: !picker),
        actions: DiscoveryNativeActions(), map: AnyView(CoffeePeekNativeStyle.border),
        roasters: { _ in AnyView(CoffeePeekPhotoPlaceholder().frame(height: 160)) })
}

#Preview("Discovery light") { discoveryScreenPreview().preferredColorScheme(.light) }
#Preview("Discovery dark") { discoveryScreenPreview().preferredColorScheme(.dark) }
#Preview("Discovery map light") { discoveryScreenPreview(expanded: true).preferredColorScheme(.light) }
#Preview("Discovery map dark") { discoveryScreenPreview(expanded: true).preferredColorScheme(.dark) }
#Preview("Check-in shop picker light") { discoveryScreenPreview(picker: true).preferredColorScheme(.light) }
#Preview("Check-in shop picker dark") { discoveryScreenPreview(picker: true).preferredColorScheme(.dark) }
#Preview("Discovery transitions light") { DiscoveryInteractionPreview(picker: false).preferredColorScheme(.light) }
#Preview("Discovery transitions dark") { DiscoveryInteractionPreview(picker: false).preferredColorScheme(.dark) }
#Preview("Picker transitions light") { DiscoveryInteractionPreview().preferredColorScheme(.light) }
#Preview("Picker transitions dark") { DiscoveryInteractionPreview().preferredColorScheme(.dark) }
#Preview("Picker large text") {
    DiscoveryInteractionPreview().preferredColorScheme(.dark).environment(\.dynamicTypeSize, .accessibility2)
}

@MainActor
private func discoveryEmptySearchPreview(_ reason: String = "Query", allResultsEmpty: Bool = true) -> some View {
    DiscoveryNativeScreen(state: discoveryPreviewState(emptyReason: reason, allResultsEmpty: allResultsEmpty),
        actions: DiscoveryNativeActions(), map: AnyView(CoffeePeekNativeStyle.border), roasters: { _ in AnyView(EmptyView()) })
}

#Preview("Empty search light") { discoveryEmptySearchPreview().preferredColorScheme(.light) }
#Preview("Empty search dark") { discoveryEmptySearchPreview().preferredColorScheme(.dark) }
#Preview("Filtered empty search light") { discoveryEmptySearchPreview("Filters", allResultsEmpty: false).preferredColorScheme(.light) }
#Preview("Filtered empty search dark") { discoveryEmptySearchPreview("Filters", allResultsEmpty: false).preferredColorScheme(.dark) }
#Preview("Empty shop results light") { discoveryEmptySearchPreview(allResultsEmpty: false).preferredColorScheme(.light) }
#Preview("Empty shop results dark") { discoveryEmptySearchPreview(allResultsEmpty: false).preferredColorScheme(.dark) }
#Preview("Empty catalog light") { discoveryEmptySearchPreview("Catalog", allResultsEmpty: false).preferredColorScheme(.light) }
#Preview("Empty catalog dark") { discoveryEmptySearchPreview("Catalog", allResultsEmpty: false).preferredColorScheme(.dark) }
#Preview("Empty search small window and large text") {
    discoveryEmptySearchPreview().preferredColorScheme(.dark).environment(\.dynamicTypeSize, .accessibility2)
        .frame(width: 320, height: 480)
}

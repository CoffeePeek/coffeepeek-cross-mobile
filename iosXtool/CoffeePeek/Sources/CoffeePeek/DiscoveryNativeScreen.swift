import ComposeApp
import SwiftUI

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

    private func text(_ key: String) -> String { state.labels[key] ?? "" }

    var body: some View {
        GeometryReader { geometry in
            screen(in: geometry.size)
        }
        .font(CoffeePeekNativeStyle.font(14))
        .background(CoffeePeekNativeStyle.background)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .disabled(!state.active)
        .onChange(of: listDragging) { _, dragging in actions.call(dragging ? "scroll-start" : "scroll-end") }
        .onChange(of: state.mapExpanded) { _, _ in queryFocused = false }
        .onChange(of: state.roastersExpanded) { _, _ in queryFocused = false }
        .accessibilityAction(.escape) { dismiss() }
        .onAppear { for feature in exposed { actions.visible(feature, true) } }
        .onDisappear { for feature in exposed { actions.visible(feature, false) } }
    }

    private var modeAnimation: Animation? { reduceMotion ? nil : .easeInOut(duration: 0.36) }
    private var mapAnimation: Animation? { reduceMotion ? nil : .easeInOut(duration: 0.42) }
    private var catalogueTransition: AnyTransition {
        reduceMotion ? .identity : .offset(y: 64).combined(with: .opacity)
    }
    private var showsLanding: Bool { state.landing && !state.picker }
    private var showsMapPreview: Bool { showsLanding && !state.roastersExpanded }
    private var showsQuickFilterDivider: Bool { !state.mapExpanded && !state.picker && !state.roastersExpanded }
    private let mapPreviewHeight: CGFloat = 200

    private func screen(in size: CGSize) -> some View {
        ZStack(alignment: .top) {
            catalogueLayer(in: size)
                .overlayPreferenceValue(DiscoveryMapAnchors.self) { anchors in mapLayer(anchors) }
            if state.mapExpanded { expandedMap.transition(.opacity) }
            header.onGeometryChange(for: CGFloat.self) { $0.size.height } action: { next in
                withAnimation(headerHeight == 0 ? nil : modeAnimation) { headerHeight = next }
            }
            if showsContinueCheckIn { continueCheckIn }
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
                results.simultaneousGesture(listDrag)
                    .offset(y: state.mapExpanded || state.roastersExpanded ? size.height * 0.3 : 0)
                    .opacity(state.mapExpanded || state.roastersExpanded ? 0 : 1)
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
        VStack(spacing: state.picker && !state.mapExpanded ? 12 : 8) {
            if state.picker { pickerHeader }
            searchHeader(expanded: state.mapExpanded)
            if !state.roastersExpanded {
                chips.transition(catalogueTransition)
            }
        }
        .padding(.top, state.mapExpanded ? 4 : 12)
        .padding(.bottom, state.mapExpanded ? 4 : (showsQuickFilterDivider ? 8 : 16))
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
        .padding(.horizontal, 12).frame(minHeight: 48)
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
        TextField(searchPlaceholder, text: Binding(get: { state.query }, set: actions.query))
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
                        selected: state.chips.first?.selected == true) { actions.call("nearby") }
                    DiscoveryQuickFilterChip(title: text(state.mapExpanded ? "list" : "map"),
                        id: state.mapExpanded ? "list" : "map", selected: state.mapExpanded) {
                        queryFocused = false; actions.call("map")
                    }
                } else {
                    ForEach(state.chips, id: \.id) { item in
                        DiscoveryQuickFilterChip(title: item.title, id: item.id, selected: item.selected) { actions.call(item.id) }
                    }
                }
            }.padding(.horizontal, 16).padding(.vertical, showsQuickFilterDivider ? 4 : 0)
        }.scrollIndicators(.hidden)
    }

    private var results: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    Color.clear.frame(height: 0).id("discovery-top")
                    if state.refreshing {
                        CoffeePeekNativeLoader(label: text("loading")).frame(maxWidth: .infinity).padding(.bottom, 16)
                    }
                    mapPreview.padding(.bottom, showsLanding ? 16 : 0)
                    LazyVStack(alignment: .leading, spacing: 16) {
                        if showsLanding { landingContent.transition(catalogueTransition) }
                        else { listContent.transition(catalogueTransition) }
                    }
                }.padding(.bottom, CGFloat(state.bottomClearance) + (showsContinueCheckIn ? 72 : 20))
                    .animation(modeAnimation, value: state.landing)
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
            .refreshable { actions.call("refresh") }
            .onChange(of: state.scrollRevision) { _, _ in queryFocused = false; proxy.scrollTo("discovery-top", anchor: .top) }
            .onChange(of: state.query) { _, _ in proxy.scrollTo("discovery-top", anchor: .top) }
            .onChange(of: showsLanding) { _, _ in
                withAnimation(modeAnimation) { proxy.scrollTo("discovery-top", anchor: .top) }
            }
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
            Color.clear.frame(height: showsLanding ? mapPreviewHeight : 0)
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
            let frame = discoveryMapLayoutFrame(viewport: geometry.size, preview: slot, expanded: state.mapExpanded)
            let clip = discoveryMapClipFrame(viewport: geometry.size, results: results, expanded: state.mapExpanded)
            let target = frame ?? CGRect(x: 0, y: 0, width: 1, height: 1)
            map.background(CoffeePeekNativeStyle.surface)
                .overlay(alignment: .topTrailing) { expandIcon }
                .frame(width: target.width, height: target.height)
                .clipShape(RoundedRectangle(cornerRadius: state.mapExpanded ? 0 : 24))
                .position(x: target.midX, y: target.midY)
                .offset(y: state.mapExpanded || showsMapPreview ? 0 : -24)
                .mask(alignment: .topLeading) {
                    Rectangle().frame(width: clip.width, height: clip.height).offset(x: clip.minX, y: clip.minY)
                }
                .opacity(frame != nil && (state.mapExpanded || showsMapPreview) ? 1 : 0)
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
            ForEach(state.recent, id: \.id) { shop in shopRow(shop) }
        }
        ForEach(state.shops, id: \.id) { shop in shopRow(shop) }
        searchStatus
        pagination
        if state.canSuggestShop { contribution }
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
    }

    @ViewBuilder private var searchStatus: some View {
        if state.loading && state.shops.isEmpty && state.recent.isEmpty {
            CoffeePeekNativeLoader(label: text("loading")).frame(maxWidth: .infinity).padding(20)
        } else if state.failed {
            feedback(text(state.shops.isEmpty ? "failed" : "refresh-failed"), action: "refresh", label: text("retry"))
        } else if let reason = state.emptyReason, state.shops.isEmpty && state.recent.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                Text(text(state.picker ? "empty" : "empty-\(reason)"))
                if reason == "Filters" { Button(text("reset")) { actions.call("reset") } }
            }.padding(.horizontal, 16)
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
    }

    private var contribution: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(text("add-title")).font(CoffeePeekNativeStyle.font(16, weight: "SemiBold"))
            Text(text("add-body")).foregroundStyle(CoffeePeekNativeStyle.secondary)
            Button(text("add")) { actions.call("add") }
            if state.canSuggestRoaster { Button(text("add-roaster")) { actions.call("add-roaster") } }
        }.padding(16).frame(maxWidth: .infinity, alignment: .leading)
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
            let glyph = Image(systemName: symbol).font(.system(size: 20)).frame(width: 48, height: 48)
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

private struct DiscoveryQuickFilterChip: View {
    let title: String
    let id: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            styledCaption.frame(minWidth: 48, minHeight: 48).contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
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


func discoveryPreviewState(picker: Bool = false, expanded: Bool = false, canContinue: Bool = false) -> IosDiscoveryState {
    let shop = IosDiscoveryShop(id: "preview", title: "CoffeePeek Specialty", photoUrl: nil,
        address: "Тихая улица, 12", rating: "4.8", detail: nil, distance: "350 м", kind: "Specialty",
        tags: ["V60", "Эспрессо"], favorite: true, favoriteBusy: false,
        favoriteLabel: "Убрать из избранного", open: true, isNew: true, hours: "Открыто · 09:00 – 22:00", decoration: nil)
    return IosDiscoveryState(picker: picker, active: true, query: "", landing: !picker,
        mapExpanded: expanded, roastersExpanded: false, showRoasterResults: false, canSuggestRoaster: false,
        scrollRevision: 0, bottomClearance: picker ? 0 : 84, loading: false, refreshing: false, loadingMore: false,
        canLoadMore: false, failed: false, loadMoreFailed: false, canSuggestShop: false, emptyReason: nil,
        locationNotice: nil, settingsNeeded: false, favoriteFailure: false, continueCheckIn: canContinue,
        shops: [shop], recent: [], mapShops: [shop], selectedShopId: "preview", suggestions: [],
        showSuggestions: false, searchFailed: false, searchLoading: false, selectedZone: nil, zonesVisible: true,
        heatmapVisible: false, heatmapLoading: false, heatmapFailed: false, viewportFailed: false, truncated: false,
        chips: discoveryPreviewChips(selected: picker ? "nearby" : "open-only"), activeFilterCount: 1,
        labels: ["title": "Выберите кофейню", "search": "Поиск кофеен и обжарщиков…", "shops": "Кофейни",
            "roasters": "Обжарщики", "show-all": "Показать все", "open": "ОТКРЫТО", "new": "Новая",
            "nearby": "Рядом со мной", "map": "На карте", "list": "Списком", "filters": "Фильтры",
            "continue": "Продолжить чекин"])
}

private func discoveryPreviewChips(selected: String) -> [IosDiscoveryChip] {
    [
        ("nearby", "Рядом"), ("open-only", "Открыто"), ("new", "Новые"), ("visited", "Уже был"),
        ("favorites", "Избранное"), ("Specialty", "Specialty"), ("CoffeeBar", "Кофейня"), ("Cafe", "Кафе"),
    ].map { IosDiscoveryChip(id: $0.0, title: $0.1, selected: $0.0 == selected) }
}

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

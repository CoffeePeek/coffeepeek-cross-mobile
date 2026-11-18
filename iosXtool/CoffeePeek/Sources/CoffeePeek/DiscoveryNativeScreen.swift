import ComposeApp
import SwiftUI

struct DiscoveryNativeScreen: View {
    let state: IosDiscoveryState
    let actions: DiscoveryNativeActions
    let map: AnyView
    let roasters: (Bool) -> AnyView
    @FocusState private var queryFocused: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var resultsFrame: CGRect = .zero
    @State private var tailVisible = false
    @GestureState private var listDragging = false
    @State private var exposed: Set<String> = []

    private func text(_ key: String) -> String { state.labels[key] ?? "" }

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .topLeading) {
                if state.mapExpanded {
                    map.frame(maxWidth: .infinity, maxHeight: .infinity)
                        .ignoresSafeArea(.container, edges: .vertical)
                        .transition(.asymmetric(insertion: modeTransition, removal: .identity))
                        .zIndex(1)
                }
                mainContent.simultaneousGesture(listDrag)
                    .opacity(state.mapExpanded || state.roastersExpanded ? 0 : 1)
                    .allowsHitTesting(!state.mapExpanded && !state.roastersExpanded)
                    .accessibilityHidden(state.mapExpanded || state.roastersExpanded)
                if state.mapExpanded { expandedMap.transition(modeTransition).zIndex(2) }
                if state.roastersExpanded {
                    roasters(true).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                        .background(CoffeePeekNativeStyle.background)
                        .transition(reduceMotion ? .identity : .move(edge: .trailing).combined(with: .opacity))
                        .zIndex(2)
                }
                if showsContinueCheckIn { continueCheckIn.zIndex(2) }
                notices.zIndex(3)
            }
            .frame(width: geometry.size.width, height: geometry.size.height, alignment: .topLeading)
            .animation(modeAnimation, value: state.mapExpanded)
            .animation(modeAnimation, value: state.roastersExpanded)
            .coordinateSpace(name: "discovery")
            .onPreferenceChange(DiscoveryResultsFrame.self) { resultsFrame = $0 }
            .onPreferenceChange(DiscoveryExposureFrames.self) { frames in
                if !state.mapExpanded && !state.roastersExpanded { actions.call("scroll") }
                let viewport = CGRect(origin: .zero, size: geometry.size)
                let eligible = frames.filter { id, _ in
                    if state.roastersExpanded { return false }
                    if state.mapExpanded { return id == "map-full" || id == "search-map" || id == "filters-map" }
                    return id != "map-full" && id != "search-map" && id != "filters-map"
                }
                let visible = Set(eligible.values.filter { frame in
                    let area = frame.rect.width * frame.rect.height
                    let clip = frame.feature == "map" && !state.mapExpanded
                        ? discoveryMapClipFrame(viewport: geometry.size, results: resultsFrame, expanded: false)
                        : viewport
                    let intersection = frame.rect.intersection(clip)
                    return area > 0 && !intersection.isNull && intersection.width * intersection.height / area >= 0.1
                }.map(\.feature))
                for feature in exposed.subtracting(visible) { actions.visible(feature, false) }
                for feature in visible.subtracting(exposed) { actions.visible(feature, true) }
                exposed = visible
            }
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

    private var modeAnimation: Animation? { reduceMotion ? nil : .easeInOut(duration: 0.26) }
    private var modeTransition: AnyTransition { reduceMotion ? .identity : .opacity }

    private var mainContent: some View {
        VStack(spacing: 12) {
            VStack(spacing: 12) {
                if state.picker { pickerHeader }
                searchHeader(expanded: false)
                chips(expanded: false)
            }.padding(.top, 12).padding(.bottom, 4).background(CoffeePeekNativeStyle.background)
            results
        }
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
        HStack(spacing: 8) {
            if expanded || (!state.landing && !state.picker) {
                icon(expanded ? "xmark" : "chevron.left", label: text(expanded ? "collapse-map" : "cancel"), glass: expanded) {
                    queryFocused = false
                    actions.call(expanded ? "map" : "cancel")
                }
            }
            searchField(expanded: expanded)
            if !state.picker { filterButton(expanded: expanded) }
        }.padding(.horizontal, 16)
    }

    @ViewBuilder private func searchField(expanded: Bool) -> some View {
        let field = HStack(spacing: 8) {
            Image(systemName: "magnifyingglass").foregroundStyle(CoffeePeekNativeStyle.secondary)
            searchInput
            if !state.query.isEmpty { clearSearch }
        }
        .padding(.horizontal, 12).frame(minHeight: 48)
        .discoveryExposure("search", id: expanded ? "search-map" : "search-list")
        if expanded {
            field.discoveryGlass(in: Capsule())
        } else {
            field.background(CoffeePeekNativeStyle.surface, in: Capsule())
                .overlay { Capsule().strokeBorder(CoffeePeekNativeStyle.border, lineWidth: 1).allowsHitTesting(false) }
        }
    }

    private var searchInput: some View {
        TextField(text("search"), text: Binding(get: { state.query }, set: actions.query))
            .textInputAutocapitalization(.never).autocorrectionDisabled().submitLabel(.search)
            .focused($queryFocused).onSubmit { queryFocused = false }
            .onChange(of: queryFocused) { _, focused in if focused { actions.call("activate") } }
    }

    private var clearSearch: some View {
        Button { actions.query("") } label: { Image(systemName: "xmark.circle.fill") }
            .foregroundStyle(CoffeePeekNativeStyle.secondary).accessibilityLabel(text("clear"))
    }

    private func filterButton(expanded: Bool) -> some View {
        icon("slider.horizontal.3", label: text("filters"), glass: expanded) {
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

    private func chips(expanded: Bool) -> some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                if state.picker {
                    chip(text("nearby"), selected: state.chips.first?.selected == true, glass: expanded) { actions.call("nearby") }
                    chip(text(state.mapExpanded ? "list" : "map"), selected: state.mapExpanded, glass: expanded) {
                        queryFocused = false; actions.call("map")
                    }
                } else {
                    ForEach(state.chips, id: \.id) { item in
                        chip(item.title, selected: item.selected, glass: expanded) { actions.call(item.id) }
                    }
                }
            }.padding(.horizontal, 16)
        }.scrollIndicators(.hidden)
    }

    private var results: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 16) {
                    Color.clear.frame(height: 0).id("discovery-top")
                    if state.refreshing { CoffeePeekNativeLoader(label: text("loading")).frame(maxWidth: .infinity) }
                    if state.landing && !state.picker { landingContent.transition(.identity) }
                    else { listContent.transition(.asymmetric(insertion: modeTransition, removal: .identity)) }
                }.padding(.bottom, CGFloat(state.bottomClearance) + (showsContinueCheckIn ? 72 : 20))
                    .animation(modeAnimation, value: state.landing)
            }
            .scrollDismissesKeyboard(.interactively)
            .clipped()
            .background {
                GeometryReader { geometry in
                    Color.clear.preference(key: DiscoveryResultsFrame.self, value: geometry.frame(in: .named("discovery")))
                }
            }
            .refreshable { actions.call("refresh") }
            .onChange(of: state.scrollRevision) { _, _ in queryFocused = false; proxy.scrollTo("discovery-top", anchor: .top) }
            .onChange(of: state.query) { _, _ in proxy.scrollTo("discovery-top", anchor: .top) }
        }
    }

    private var landingContent: some View {
        Group {
            mapPreview
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
            ZStack(alignment: .topTrailing) {
                CoffeePeekNativeStyle.surface
                if !state.mapExpanded && !state.roastersExpanded {
                    map.allowsHitTesting(false).accessibilityHidden(true).transition(.identity)
                }
                Image(systemName: "arrow.up.left.and.arrow.down.right").font(.system(size: 17, weight: .semibold))
                    .frame(width: 44, height: 44).discoveryGlass(in: Circle()).padding(8)
                    .accessibilityHidden(true)
            }
            .frame(height: 200)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .contentShape(RoundedRectangle(cornerRadius: 24))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(text("map"))
        .padding(.horizontal, 16)
        .discoveryExposure("map", id: "map-preview")
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

    private func chip(_ title: String, selected: Bool, glass: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            let caption = Text(title).font(CoffeePeekNativeStyle.font(13, weight: "Medium"))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .padding(.horizontal, 14).frame(minHeight: 36)
            if glass {
                caption.discoveryGlass(in: Capsule(), selected: selected)
            } else {
                caption.background(selected ? CoffeePeekNativeStyle.accent.opacity(0.22) : CoffeePeekNativeStyle.surface, in: Capsule())
                    .overlay { Capsule().stroke(selected ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.border, lineWidth: 1) }
            }
        }.buttonStyle(.plain).accessibilityAddTraits(selected ? .isSelected : [])
    }

    private var expandedMap: some View {
        DiscoveryMapControls(state: state, actions: actions,
            header: AnyView(VStack(spacing: 8) {
                if state.picker { pickerHeader }
                searchHeader(expanded: true)
                chips(expanded: true)
            }.padding(.top, 12)))
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
        chips: [IosDiscoveryChip(id: "nearby", title: "Рядом", selected: false)], activeFilterCount: 0,
        labels: ["title": "Выберите кофейню", "search": "Поиск кофеен и обжарщиков…", "shops": "Кофейни",
            "roasters": "Обжарщики", "show-all": "Показать все", "open": "ОТКРЫТО", "new": "Новая",
            "nearby": "Рядом со мной", "map": "На карте", "list": "Списком", "filters": "Фильтры",
            "continue": "Продолжить чекин"])
}

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

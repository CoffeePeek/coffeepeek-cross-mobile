import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeOwnProfileController(profile: IosOwnProfile) -> UIViewController {
    OwnProfileHostingController(profile: profile)
}

private final class OwnProfileHostingController: UIHostingController<OwnProfileHost> {
    private let profile: IosOwnProfile
    private let model: OwnProfileModel

    init(profile: IosOwnProfile) {
        self.profile = profile
        let model = OwnProfileModel(profile: profile)
        self.model = model
        super.init(rootView: OwnProfileHost(model: model))
    }

    @MainActor required dynamic init?(coder: NSCoder) { return nil }

    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        model.headerTopInset = view.safeAreaInsets.top
    }

    // SwiftUI may rebuild its state graph while UIKit retains the root tab.
    // The account lease ends only when that root controller is released.
    isolated deinit { profile.close() }
}

private struct OwnProfileHost: View {
    @ObservedObject var model: OwnProfileModel

    var body: some View {
        OwnProfileScreen(model: model)
            .allowsHitTesting(model.photoViewer == nil)
            .accessibilityHidden(model.photoViewer != nil)
            .coffeePeekPhotoViewer(model.photoViewer, languageTag: model.strings.languageTag,
                                   onDismiss: model.dismissPhotoViewer)
            .onAppear(perform: model.observe)
            .onDisappear(perform: model.stopObserving)
    }
}

private struct OwnProfileScreen: View {
    @ObservedObject var model: OwnProfileModel

    var body: some View {
        GeometryReader { geometry in
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        menu.padding(.horizontal, 16).padding(.top, model.headerTopInset).id("own-profile-top")
                        profileContent(viewport: geometry.size.height, width: geometry.size.width)
                    }
                    .padding(.bottom, 16)
                    .ownProfileCalendarAnimation(model.activity)
                }
                .accessibilityIdentifier("own-profile")
                .onChange(of: model.scrollRevision) { _, _ in proxy.scrollTo("own-profile-top", anchor: .top) }
            }
        }
        .safeAreaInset(edge: .bottom, alignment: .trailing, spacing: 0) { sectionControl }
        .ignoresSafeArea(.container, edges: [.top, .leading, .trailing])
        .font(CoffeePeekNativeStyle.font(15))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .confirmationDialog(model.strings["profile_logout_title"], isPresented: $model.logoutPresented,
                            titleVisibility: .visible) {
            Button(model.strings["profile_menu_logout"], role: .destructive, action: model.profile.logout)
            Button(model.strings["profile_cancel"], role: .cancel) {}
        } message: {
            Text(model.strings["profile_logout_message"])
        }
        .sheet(isPresented: model.connectionsPresented) {
            OwnProfileConnectionsSheet(model: model)
        }
    }

    @ViewBuilder
    private var sectionControl: some View {
        if model.state.active && model.state.identity != nil {
            switch model.selected {
            case .shelf:
                if let presenter = model.shelf as? CoffeePeekNativeShelfSettingsPresenting {
                    OwnProfileFloatingSectionControl(symbol: "gearshape",
                        label: ShelfNativeStrings(model.strings.languageTag)["shelf_settings"],
                        action: presenter.showShelfSettings)
                }
            case .collections:
                OwnProfileFloatingSectionControl(symbol: "chevron.right",
                    label: model.strings["profile_section_all_collections"],
                    action: model.profile.openCollections)
            case .checkIns:
                EmptyView()
            }
        }
    }

    private var menu: some View {
        CoffeePeekNativeRootHeader(title: model.strings["profile_title"]) {
            if model.state.active {
                Menu {
                    ownProfileEditButton(title: model.strings["profile_edit"], action: model.profile.edit)
                    ForEach(OwnProfileContribution.allCases) { contribution in
                        Button(model.strings[contribution.resource], systemImage: contribution.symbol) {
                            model.profile.openContribution(kind: contribution.rawValue)
                        }
                    }
                    Button(model.strings["profile_menu_blocked_authors"], systemImage: "eye.slash",
                           action: model.profile.openBlocked)
                    Button(model.strings["profile_menu_logout"], systemImage: "rectangle.portrait.and.arrow.right",
                           role: .destructive) { model.logoutPresented = true }
                } label: {
                    Image(systemName: "ellipsis").frame(width: 48, height: 48)
                        .background(CoffeePeekNativeStyle.surface, in: Circle())
                        .overlay {
                            Circle().strokeBorder(CoffeePeekNativeStyle.border.opacity(0.45))
                                .allowsHitTesting(false)
                        }
                }
                .accessibilityLabel(model.strings["profile_actions"])
            }
        }
        .tint(CoffeePeekNativeStyle.foreground)
    }

    @ViewBuilder
    private func profileContent(viewport: CGFloat, width: CGFloat) -> some View {
        if let identity = model.state.identity {
            OwnProfileHeader(identity: identity, strings: model.strings, profile: model.profile,
                             select: model.selectStat)
                .padding(.horizontal, 16)
            if let activity = model.activity {
                OwnProfileActivityView(state: activity, model: model, strings: model.strings, availableWidth: width - 32)
                    .padding(.horizontal, 16)
            }
            if model.state.refreshFailed {
                OwnProfileRetry(message: model.strings["profile_update_failed"], strings: model.strings,
                                retry: model.profile.refresh)
                    .padding(.horizontal, 16)
            }
            OwnProfileTabs(selection: model.selected, strings: model.strings, select: model.select, equalWidth: true)
                .background(alignment: .bottom) {
                    Rectangle().fill(CoffeePeekNativeStyle.border.opacity(0.45)).frame(height: 1)
                }
                .padding(.horizontal, 16)
            profileTab(viewport: viewport)
        } else if model.state.loading || model.state.refreshing {
            CoffeePeekNativeLoader(size: 24).frame(maxWidth: .infinity, minHeight: 130)
                .accessibilityLabel(model.strings["profile_loading"])
        } else if model.state.active {
            OwnProfileRetry(message: model.strings["profile_load_failed"], strings: model.strings,
                            retry: model.profile.refresh)
                .padding(.horizontal, 16)
        }
    }

    private var checkInsTab: some View {
        OwnProfileCheckIns(model: model)
    }

    @ViewBuilder
    private func profileTab(viewport: CGFloat) -> some View {
        switch model.selected {
        case .checkIns:
            checkInsTab
        case .collections:
            if let collections = model.collections {
                OwnProfileEmbedded(controller: collections, fitsContent: true)
                    .tint(CoffeePeekNativeStyle.foreground)
            }
        case .shelf:
            if let shelf = model.shelf {
                OwnProfileEmbedded(controller: shelf)
                    .frame(height: max(400, viewport))
            }
        }
    }
}

private struct OwnProfileFloatingSectionControl: View {
    let symbol: String
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol).font(.system(size: 20, weight: .medium)).frame(width: 48, height: 48)
        }
        .buttonStyle(.plain).shopDetailGlassCircle()
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .accessibilityLabel(label)
        .padding(.horizontal, 16).padding(.vertical, 12)
    }
}

@MainActor
private func ownProfileFloatingControlsPreview(barHeight: CGFloat) -> some View {
    let strings = OwnProfileStrings(languageTag: "ru")
    return ScrollView {
        ownProfileCollectionsPreview(strings: strings).padding(16)
    }
    .safeAreaInset(edge: .bottom, alignment: .trailing, spacing: 0) {
        OwnProfileFloatingSectionControl(symbol: "chevron.right",
            label: strings["profile_section_all_collections"], action: {})
    }
    .safeAreaInset(edge: .bottom, spacing: 0) {
        Label(strings["profile_title"], systemImage: "person.crop.circle")
            .frame(maxWidth: .infinity).frame(height: barHeight)
            .background(CoffeePeekNativeStyle.surfaceVariant)
    }
    .background(CoffeePeekNativeStyle.background).foregroundStyle(CoffeePeekNativeStyle.foreground)
}

@MainActor
private func ownProfileCollectionsPreview(strings: OwnProfileStrings) -> some View {
    VStack(spacing: 12) {
        OwnProfileTabs(selection: .collections, strings: strings, select: { _ in }, equalWidth: true)
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
            ForEach(0..<4) { _ in
                RoundedRectangle(cornerRadius: 16).fill(CoffeePeekNativeStyle.surfaceVariant).aspectRatio(1, contentMode: .fit)
            }
        }
    }
}

#Preview("Floating profile controls light") {
    ownProfileFloatingControlsPreview(barHeight: 83).preferredColorScheme(.light)
}
#Preview("Floating profile controls dark") {
    ownProfileFloatingControlsPreview(barHeight: 110).preferredColorScheme(.dark).environment(\.dynamicTypeSize, .accessibility2)
}

@MainActor
final class OwnProfileModel: ObservableObject {
    @Published private(set) var state: IosOwnProfileState
    @Published private(set) var activity: IosOwnProfileActivity?
    @Published private(set) var selected: OwnProfileTab = .checkIns
    @Published private(set) var strings: OwnProfileStrings
    @Published private(set) var photoViewer: IosPhotoViewer?
    @Published var logoutPresented = false
    @Published private(set) var scrollRevision = 0
    @Published var headerTopInset: CGFloat = 0
    let profile: IosOwnProfile
    private(set) var shelf: UIViewController?
    private(set) var collections: UIViewController?
    private var observation: IosOwnProfileObservation?
    private var calendarObservation: IosOwnProfileObservation?
    private var expanded = false

    init(profile: IosOwnProfile) {
        self.profile = profile
        state = profile.current()
        strings = OwnProfileStrings(languageTag: profile.languageTag())
    }

    isolated deinit {
        observation?.cancel()
        calendarObservation?.cancel()
    }

    var connectionsPresented: Binding<Bool> {
        Binding(get: { self.state.connections.open }, set: { if !$0 { self.profile.closeConnections() } })
    }

    func observe() {
        guard observation == nil else { return }
        update(profile.current())
        profile.resume()
        if profile.shelfRequested() { openRequestedShelf() }
        if profile.checkInsRequested() { openRequestedCheckIns() }
        if profile.collectionsRequested() { openRequestedCollections() }
        observation = profile.observe(onState: { [weak self] in self?.update($0) },
                                      onLanguage: { [weak self] in self?.strings = OwnProfileStrings(languageTag: $0) },
                                      onShelf: { [weak self] in self?.openRequestedShelf() },
                                      onCheckIns: { [weak self] in self?.openRequestedCheckIns() },
                                      onCollections: { [weak self] in self?.openRequestedCollections() })
    }

    func stopObserving() {
        guard photoViewer == nil else { return }
        observation?.cancel()
        calendarObservation?.cancel()
        observation = nil
        calendarObservation = nil
    }

    func select(_ tab: OwnProfileTab) {
        if tab == .shelf && shelf == nil { shelf = profile.makeShelf() }
        if tab == .collections && collections == nil { collections = profile.makeCollections() }
        selected = tab
    }

    func selectStat(_ stat: OwnProfileStat) {
        switch stat {
        case .checkIns: select(.checkIns)
        case .shops: profile.openContribution(kind: OwnProfileContribution.shops.rawValue)
        case .roasters: profile.openContribution(kind: OwnProfileContribution.roasters.rawValue)
        case .followers: profile.openConnections(followers: true)
        case .following: profile.openConnections(followers: false)
        case .avatar: photoViewer = profile.avatarViewer()
        }
    }

    func openPhoto(id: String, index: Int) {
        photoViewer = profile.checkInViewer(id: id, index: Int32(index))
    }

    func dismissPhotoViewer() { photoViewer = nil }

    func selectDay(_ date: String) {
        selected = .checkIns
        profile.selectDay(date: date)
    }

    func clearSelectedDay() { profile.clearSelectedDay() }

    func toggleActivity() {
        expanded.toggle()
        calendarObservation?.cancel()
        calendarObservation = nil
        observeCalendar()
    }

    private func update(_ next: IosOwnProfileState) {
        state = next
        if !next.active {
            calendarObservation?.cancel()
            calendarObservation = nil
            activity = nil
            photoViewer = nil
            logoutPresented = false
            shelf = nil
            collections = nil
            selected = .checkIns
        } else if next.identity != nil && calendarObservation == nil {
            observeCalendar()
        }
    }

    private func observeCalendar() {
        activity = profile.activity(expanded: expanded)
        calendarObservation = profile.observeActivity(expanded: expanded) { [weak self] in self?.activity = $0 }
    }

    private func openRequestedCheckIns() {
        selected = .checkIns
        profile.clearSelectedDay()
        profile.consumeCheckInsRequest()
        scrollRevision += 1
    }

    private func openRequestedShelf() {
        select(.shelf)
        profile.consumeShelfRequest()
    }

    private func openRequestedCollections() {
        select(.collections)
        profile.consumeCollectionsRequest()
    }
}

struct OwnProfileStrings {
    let languageTag: String
    private let bundle: Bundle
    private let table: String

    init(languageTag: String, table: String = "NativeOwnProfile") {
        self.languageTag = languageTag
        self.table = table
        let url = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = url.flatMap { Bundle(url: $0) } ?? .main
    }

    subscript(_ key: String) -> String {
        let label = bundle.localizedString(forKey: key, value: nil, table: table)
        if label != key || table == "NativeOwnProfile" { return label }
        return bundle.localizedString(forKey: key, value: nil, table: "NativeOwnProfile")
    }

    func count(_ key: String, _ value: Int) -> String {
        String(format: self[key], value)
    }

    func connectionCount(_ value: Int?, followers: Bool) -> String {
        guard let value else { return self[followers ? "profile_followers" : "profile_following_title"] }
        let tail = value % 100
        let form = (11...14).contains(tail) ? "many" : value % 10 == 1 ? "one" : (2...4).contains(value % 10) ? "few" : "many"
        return count("profile_connections_\(followers ? "followers" : "following")_\(form)", value)
    }
}

enum OwnProfileTab: String, CaseIterable, Identifiable {
    case checkIns = "profile_section_check_ins"
    case collections = "profile_section_favorites"
    case shelf = "profile_section_shelf"
    var id: Self { self }
}

private enum OwnProfileContribution: String, CaseIterable, Identifiable {
    case changes, shops, roasters
    var id: Self { self }
    var resource: String {
        switch self {
        case .changes: "profile_menu_shop_changes"
        case .shops: "profile_menu_submitted_shops"
        case .roasters: "profile_menu_submitted_roasters"
        }
    }
    var symbol: String {
        switch self {
        case .changes: "square.and.pencil"
        case .shops: "plus"
        case .roasters: "cup.and.saucer"
        }
    }
}

@MainActor
private func ownProfileEditButton(title: String, action: @escaping () -> Void) -> some View {
    Button(title, systemImage: "person.crop.circle", action: action)
}

@MainActor
private func ownProfileActionIconsPreview() -> some View {
    let strings = OwnProfileStrings(languageTag: "ru")
    return VStack(alignment: .leading, spacing: 16) {
        ownProfileEditButton(title: strings["profile_edit"], action: {})
        ForEach(OwnProfileContribution.allCases) { contribution in
            Label(strings[contribution.resource], systemImage: contribution.symbol)
        }
    }
    .labelStyle(.titleAndIcon).buttonStyle(.plain)
    .foregroundStyle(CoffeePeekNativeStyle.foreground)
    .padding(16).background(CoffeePeekNativeStyle.background)
}

#Preview("Profile action icons light") { ownProfileActionIconsPreview().preferredColorScheme(.light) }
#Preview("Profile action icons dark") { ownProfileActionIconsPreview().preferredColorScheme(.dark) }

struct OwnProfileSlot: Identifiable {
    let controller: UIViewController
    var id: ObjectIdentifier { ObjectIdentifier(controller) }
}

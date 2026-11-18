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
    deinit { profile.close() }
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
    @ScaledMetric(relativeTo: .body) private var collectionsHeight: CGFloat = 220

    var body: some View {
        GeometryReader { geometry in
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        menu.padding(.horizontal, 16).padding(.top, model.headerTopInset).id("own-profile-top")
                        profileContent(viewport: geometry.size.height, width: geometry.size.width)
                    }
                    .padding(.bottom, 100)
                    .ownProfileCalendarAnimation(model.activity)
                }
                .accessibilityIdentifier("own-profile")
                .onChange(of: model.scrollRevision) { _, _ in proxy.scrollTo("own-profile-top", anchor: .top) }
            }
        }
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
        .sheet(isPresented: model.followersPresented) {
            OwnProfileFollowersSheet(model: model)
        }
        .sheet(item: $model.preferences) { slot in
            VStack(spacing: 8) {
                HStack {
                    Text(model.strings["profile_collections_settings"]).font(.headline)
                    Spacer()
                    Button(model.strings["profile_cancel"]) { model.preferences = nil }
                }
                .padding()
                OwnProfileEmbedded(controller: slot.controller)
            }
            .background(CoffeePeekNativeStyle.background)
            .foregroundStyle(CoffeePeekNativeStyle.foreground)
            .tint(CoffeePeekNativeStyle.foreground)
            .presentationDetents([.medium, .large])
        }
    }

    private var menu: some View {
        HStack {
            Text(model.strings["profile_title"])
                .font(CoffeePeekNativeStyle.rootHeadingFont)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 0)
            if model.state.active {
                Menu {
                    Button(model.strings["profile_edit"], systemImage: "square.and.pencil", action: model.profile.edit)
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
            ProgressView().frame(maxWidth: .infinity, minHeight: 130)
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
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Button(model.strings["profile_section_all_collections"], action: model.profile.openCollections)
                    Spacer()
                    Button {
                        model.showPreferences()
                    } label: {
                        Image(systemName: "gearshape").frame(width: 44, height: 44)
                    }
                    .accessibilityLabel(model.strings["profile_collections_settings"])
                }
                .padding(.horizontal, 16)
                if let collections = model.collections {
                    OwnProfileEmbedded(controller: collections)
                        .frame(height: collectionsHeight)
                }
            }
            .tint(CoffeePeekNativeStyle.foreground)
        case .shelf:
            if let shelf = model.shelf {
                OwnProfileEmbedded(controller: shelf)
                    .frame(height: max(400, viewport))
            }
        }
    }
}

final class OwnProfileModel: ObservableObject {
    @Published private(set) var state: IosOwnProfileState
    @Published private(set) var activity: IosOwnProfileActivity?
    @Published private(set) var selected: OwnProfileTab = .checkIns
    @Published private(set) var strings: OwnProfileStrings
    @Published private(set) var photoViewer: IosPhotoViewer?
    @Published var preferences: OwnProfileSlot?
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

    deinit {
        observation?.cancel()
        calendarObservation?.cancel()
    }

    var followersPresented: Binding<Bool> {
        Binding(get: { self.state.followers.open }, set: { if !$0 { self.profile.closeFollowers() } })
    }

    func observe() {
        guard observation == nil else { return }
        update(profile.current())
        profile.resume()
        if profile.shelfRequested() { openRequestedShelf() }
        if profile.checkInsRequested() { openRequestedCheckIns() }
        observation = profile.observe(onState: { [weak self] in self?.update($0) },
                                      onLanguage: { [weak self] in self?.strings = OwnProfileStrings(languageTag: $0) },
                                      onShelf: { [weak self] in self?.openRequestedShelf() },
                                      onCheckIns: { [weak self] in self?.openRequestedCheckIns() })
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
        case .followers: profile.openFollowers()
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

    func showPreferences() {
        preferences = OwnProfileSlot(controller: profile.makeCollectionPreferences())
    }

    private func update(_ next: IosOwnProfileState) {
        state = next
        if !next.active {
            calendarObservation?.cancel()
            calendarObservation = nil
            activity = nil
            photoViewer = nil
            preferences = nil
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

struct OwnProfileSlot: Identifiable {
    let controller: UIViewController
    var id: ObjectIdentifier { ObjectIdentifier(controller) }
}

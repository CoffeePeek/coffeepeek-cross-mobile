import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makePublicProfileController(profile: IosPublicProfile) -> UIViewController {
    UIHostingController(rootView: PublicProfileHost(profile: profile))
}

private struct PublicProfileHost: View {
    @StateObject private var model: PublicProfileModel

    init(profile: IosPublicProfile) {
        _model = StateObject(wrappedValue: PublicProfileModel(profile: profile))
    }

    var body: some View {
        PublicProfileScreen(model: model)
            .allowsHitTesting(model.viewer == nil)
            .accessibilityHidden(model.viewer != nil)
            .coffeePeekPhotoViewer(model.viewer, languageTag: model.strings.languageTag,
                                   onDismiss: { model.viewer = nil })
            .onAppear(perform: model.observe)
            .onDisappear(perform: model.stop)
    }
}

private struct PublicProfileScreen: View {
    @ObservedObject var model: PublicProfileModel

    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 0) {
                toolbar
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 16) {
                        content(viewport: geometry.size.height)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 16)
                    .coffeePeekRefreshable(isRefreshing: model.state.loading, label: model.strings["profile_loading"]) { model.profile?.refresh() }
                }
                .accessibilityIdentifier(model.checkInsOnly ? "public-profile-check-ins" : "public-profile")
            }
        }
        .font(CoffeePeekNativeStyle.font(16))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .sheet(isPresented: model.connectionsPresented) {
            PublicProfileConnectionsSheet(model: model)
        }
    }

    private var toolbar: some View {
        CoffeePeekNativeNavigationHeader(title: model.strings[model.checkInsOnly ? "profile_section_check_ins" : "profile_title"],
            backLabel: model.strings["profile_back"], back: { model.profile?.back() })
            .overlay(alignment: .trailing) {
                toolbarActions.buttonStyle(.plain).padding(.trailing, 10)
            }
    }

    @ViewBuilder
    private var toolbarActions: some View {
        if model.checkInsOnly {
            Color.clear.frame(width: 48, height: 48)
        } else if model.state.own {
            if model.state.canShare {
                Button { model.profile?.share() } label: {
                    Image(systemName: "square.and.arrow.up").frame(width: 48, height: 48)
                }
                .accessibilityLabel(model.strings["profile_share"])
                .shopDetailGlassCircle()
            } else {
                Color.clear.frame(width: 48, height: 48)
            }
        } else if model.state.identity != nil || model.state.blockedName != nil {
            Menu {
                if model.state.canShare {
                    Button(model.strings["profile_share"], systemImage: "square.and.arrow.up") { model.profile?.share() }
                }
                if model.state.blockedName == nil {
                    Button(model.strings["profile_block_author"], systemImage: "nosign", role: .destructive) {
                        model.profile?.blockOrUnblock()
                    }
                } else {
                    Button(model.strings["profile_unblock_author"], systemImage: "eye") { model.profile?.blockOrUnblock() }
                }
            } label: {
                Image(systemName: "ellipsis").frame(width: 48, height: 48)
            }
            .accessibilityLabel(model.strings["profile_author_actions"])
            .shopDetailGlassCircle()
        } else {
            Color.clear.frame(width: 48, height: 48)
        }
    }

    @ViewBuilder
    private func content(viewport: CGFloat) -> some View {
        if !model.state.active {
            Color.clear
        } else if let blocked = model.state.blockedName {
            blockedContent(name: blocked)
        } else if let identity = model.state.identity {
            if model.checkInsOnly {
                PublicProfileRows(model: model, full: true)
            } else {
                overview(identity: identity, viewport: viewport)
            }
        } else if model.state.loading {
            CoffeePeekNativeLoader(size: 24).frame(maxWidth: .infinity, minHeight: 140)
                .accessibilityLabel(model.strings["profile_loading"])
        } else {
            OwnProfileRetry(message: model.strings[model.state.errorKey ?? "profile_load_failed"],
                strings: model.strings) { model.profile?.refresh() }
        }
    }

    private func blockedContent(name: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "eye.slash").font(.largeTitle).accessibilityHidden(true)
            Text(model.strings["profile_blocked_title"]).font(.headline)
            Text(String(format: model.strings["profile_blocked_message"], name))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
                .multilineTextAlignment(.center)
            Button(model.strings["profile_unblock"]) { model.profile?.blockOrUnblock() }
                .frame(minHeight: 44)
        }
        .frame(maxWidth: .infinity, minHeight: 175)
    }

    private func overview(identity: IosOwnProfileIdentity, viewport: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            OwnProfileHeader(identity: identity, strings: model.strings, profile: nil, select: model.selectStat,
                enabled: { $0 == .followers || $0 == .following }, imageLoader: model.loadImage)
            if !model.state.own { follow }
            PublicProfileWeek(model: model)
            if model.state.refreshFailed {
                OwnProfileRetry(message: model.strings["profile_update_failed"], strings: model.strings) {
                    model.profile?.refresh()
                }
            }
            OwnProfileTabs(selection: model.selected, strings: model.strings, select: model.select, equalWidth: true)
            selectedTab(viewport: viewport)
        }
    }

    private var follow: some View {
        VStack(alignment: .leading, spacing: 6) {
            CoffeePeekNativeActionButton(title: model.strings[model.state.following ? "profile_following" : "profile_follow"],
                enabled: model.state.followEnabled) { model.profile?.toggleFollow() }
            if model.state.followFailed {
                OwnProfileRetry(message: model.strings["profile_follow_failed"], strings: model.strings) {
                    model.profile?.toggleFollow()
                }
            }
        }
    }

    @ViewBuilder
    private func selectedTab(viewport: CGFloat) -> some View {
        switch model.selected {
        case .checkIns:
            PublicProfileRows(model: model, full: false)
        case .collections:
            if let slot = model.slot {
                OwnProfileEmbedded(controller: slot, fitsContent: true).id(ObjectIdentifier(slot))
                    .padding(.horizontal, -16)
            }
        case .shelf:
            if let slot = model.slot {
                OwnProfileEmbedded(controller: slot).id(ObjectIdentifier(slot)).frame(height: max(340, viewport * 0.75))
                    .padding(.horizontal, -16)
            }
        }
    }
}

@MainActor
final class PublicProfileModel: ObservableObject {
    let profile: IosPublicProfile?
    let checkInsOnly: Bool
    @Published private(set) var state: IosPublicProfileState
    @Published private(set) var strings: OwnProfileStrings
    @Published private(set) var selected: OwnProfileTab = .checkIns
    @Published private(set) var slot: UIViewController?
    @Published var viewer: IosPhotoViewer?
    private var observation: IosOwnProfileObservation?

    init(profile: IosPublicProfile) {
        self.profile = profile
        checkInsOnly = profile.checkInsOnly
        state = profile.current()
        strings = Self.strings(language: profile.languageTag())
    }

    fileprivate init(preview: IosPublicProfileState, language: String, checkInsOnly: Bool = false) {
        profile = nil
        self.checkInsOnly = checkInsOnly
        state = preview
        strings = Self.strings(language: language)
    }

    isolated deinit {
        observation?.cancel()
        profile?.close()
    }

    var loadImage: ProfileImageLoader {
        { [weak self] url, receive in self?.profile?.loadImage(url: url, onImage: receive) }
    }

    var connectionsPresented: Binding<Bool> {
        Binding(get: { self.state.connections.open }, set: { if !$0 { self.profile?.closeConnections() } })
    }

    func observe() {
        guard observation == nil, let profile else { return }
        update(profile.current())
        observation = profile.observe(onState: { [weak self] in self?.update($0) },
            onLanguage: { [weak self] in self?.strings = Self.strings(language: $0) })
        profile.resume()
    }

    func stop() {
        guard viewer == nil else { return }
        observation?.cancel()
        observation = nil
    }

    func select(_ tab: OwnProfileTab) {
        selected = tab
        updateSlot()
    }

    func selectStat(_ stat: OwnProfileStat) {
        if stat == .followers {
            profile?.openConnections(followers: true)
        } else if stat == .following {
            profile?.openConnections(followers: false)
        } else if stat == .avatar {
            guard viewer == nil else { return }
            viewer = profile?.makeAvatarViewer()
        }
    }

    func openPhoto(id: String, index: Int) {
        guard viewer == nil else { return }
        viewer = profile?.makeCheckInViewer(id: id, index: Int32(index))
    }

    private static func strings(language: String) -> OwnProfileStrings {
        OwnProfileStrings(languageTag: language, table: "NativePublicProfile")
    }

    private func update(_ next: IosPublicProfileState) {
        let scopeChanged = next.visibilityRevision != state.visibilityRevision || next.slug != state.slug ||
            next.active != state.active
        let slotChanged = scopeChanged || next.own != state.own
        if scopeChanged {
            viewer = nil
            if state.connections.open { profile?.closeConnections() }
        }
        state = next
        if slotChanged { updateSlot() }
    }

    private func updateSlot() {
        guard state.identity != nil else { slot = nil; return }
        switch selected {
        case .checkIns: slot = nil
        case .collections: slot = profile?.makeFeaturedCollection()
        case .shelf: slot = profile?.makeShelfPreview()
        }
    }
}

@MainActor
private func publicProfilePreview(language: String, checkInsOnly: Bool = false,
                                  pendingLike: Bool = false, failedLike: Bool = false) -> some View {
    let identity = IosOwnProfileIdentity(name: "CoffeePeek", about: "Кофе и прогулки", avatar: nil, checkIns: 6, shops: 2,
        roasters: 1, followers: 11, canOpenFollowers: true, following: KotlinInt(int: 4))
    let activity = IosPublicProfileActivity(days: [
        IosPublicProfileDay(date: "2026-11-16", today: false, count: 2, photo: nil),
        IosPublicProfileDay(date: "2026-11-17", today: true, count: 0, photo: nil),
    ], loading: false, failed: false)
    let row = IosOwnProfileCheckIn(id: "preview", name: "CoffeePeek", avatar: nil, shop: "CoffeePeek", shopSlug: "preview",
        home: false, note: "Кофе и прогулки", date: "26.11.2026", drink: nil, helpfulCount: 3, liked: false,
        score: nil, photos: [], brew: nil)
    let rows = IosPublicProfileCheckIns(rows: [row], loading: false, failed: false, more: false, ratings: [:],
        pendingLikeIds: pendingLike ? [row.id] : [], likeErrorIds: failedLike ? [row.id] : [])
    let connections = IosPublicProfileConnections(open: false, followers: true, loading: false, failed: false, more: false, people: [])
    let state = IosPublicProfileState(active: true, loading: false, errorKey: nil, refreshFailed: false, visibilityRevision: 0,
        blockedName: nil, identity: identity, slug: "coffeepeek", own: false, signedIn: true, following: false, followEnabled: true,
        followFailed: false, canShare: true, activity: activity, checkIns: rows, connections: connections)
    return PublicProfileScreen(model: PublicProfileModel(preview: state, language: language, checkInsOnly: checkInsOnly))
}

#Preview("Public profile light") {
    publicProfilePreview(language: "ru").preferredColorScheme(.light)
}

#Preview("Public profile dark") {
    publicProfilePreview(language: "be").preferredColorScheme(.dark)
}

#Preview("Public check-ins light") {
    publicProfilePreview(language: "ru", checkInsOnly: true).preferredColorScheme(.light)
}

#Preview("Public check-ins dark") {
    publicProfilePreview(language: "be", checkInsOnly: true).preferredColorScheme(.dark)
}

#Preview("Public reaction pending light") {
    publicProfilePreview(language: "ru", pendingLike: true).preferredColorScheme(.light)
}

#Preview("Public reaction pending dark") {
    publicProfilePreview(language: "be", pendingLike: true).preferredColorScheme(.dark)
}

#Preview("Public reaction failure light") {
    publicProfilePreview(language: "ru", failedLike: true).preferredColorScheme(.light)
}

#Preview("Public reaction failure dark") {
    publicProfilePreview(language: "be", failedLike: true).preferredColorScheme(.dark)
}

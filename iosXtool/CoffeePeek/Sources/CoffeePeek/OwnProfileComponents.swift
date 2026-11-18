import ComposeApp
import SwiftUI
import UIKit

enum OwnProfileStat: Equatable { case checkIns, shops, roasters, followers, avatar }

struct OwnProfileEmbedded: View {
    let controller: UIViewController

    var body: some View {
        OwnProfileControllerRepresentable(controller: controller)
            .background(CoffeePeekNativeStyle.background)
    }
}

private struct OwnProfileControllerRepresentable: UIViewControllerRepresentable {
    let controller: UIViewController
    func makeUIViewController(context: Context) -> UIViewController { controller }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct OwnProfileHeader: View {
    let identity: IosOwnProfileIdentity
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
    let select: (OwnProfileStat) -> Void
    var enabled: (OwnProfileStat) -> Bool = { _ in true }
    var imageLoader: ProfileImageLoader?
    var signedInLayout = false
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .caption) private var fourStatsWidth: CGFloat = 220
    @ScaledMetric(relativeTo: .caption) private var threeStatsWidth: CGFloat = 168

    var body: some View {
        if signedInLayout { signedInHeader } else { publicHeader }
    }

    private var signedInHeader: some View {
        VStack(alignment: .leading, spacing: 12) {
            ViewThatFits(in: .horizontal) {
                inlineIdentity(avatarSize: 88)
                inlineIdentity(avatarSize: 56)
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 12) {
                        avatar(size: 88)
                        name
                    }
                    counts
                }
            }
            bio
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func inlineIdentity(avatarSize: CGFloat) -> some View {
        HStack(alignment: .center, spacing: 12) {
            avatar(size: avatarSize)
            VStack(alignment: .leading, spacing: 8) {
                name
                counts
            }
            .frame(minWidth: identity.roasters > 0 ? fourStatsWidth : threeStatsWidth)
        }
    }

    private var publicHeader: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                avatar(size: 56)
                VStack(alignment: .leading, spacing: 4) {
                    name
                    bio
                }
            }
            counts
        }
    }

    @ViewBuilder
    private var counts: some View {
        if dynamicTypeSize.isAccessibilitySize {
            countsGrid
        } else {
            HStack(alignment: .top, spacing: signedInLayout ? 8 : 6) {
                stat(.checkIns)
                stat(.shops)
                if !signedInLayout || identity.roasters > 0 { stat(.roasters) }
                stat(.followers)
            }
        }
    }

    private var countsGrid: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                stat(.checkIns)
                stat(.shops)
            }
            HStack {
                if !signedInLayout || identity.roasters > 0 { stat(.roasters) }
                stat(.followers)
            }
        }
    }

    private var name: some View {
        Text(identity.name).font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
            .lineLimit(2)
            .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder
    private var bio: some View {
        if let about = identity.about, !about.isEmpty {
            Text(about).font(CoffeePeekNativeStyle.font(15, relativeTo: .subheadline))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }

    @ViewBuilder
    private func avatar(size: CGFloat) -> some View {
        let image = avatarImage.frame(width: size, height: size).clipShape(Circle())
        if let avatar = identity.avatar, !avatar.isEmpty {
            Button { select(.avatar) } label: { image }
                .buttonStyle(.plain)
                .accessibilityLabel(strings["profile_open_avatar"])
        } else {
            image.accessibilityHidden(true)
        }
    }

    @ViewBuilder
    private var avatarImage: some View {
        if let url = identity.avatar, !url.isEmpty {
            OwnProfileImage(url: url, symbol: "person.crop.circle",
                            loadImage: imageLoader ?? profileImageLoader(profile))
        } else {
            LinearGradient(colors: [CoffeePeekNativeStyle.color(0xD4A84B), CoffeePeekNativeStyle.accent],
                           startPoint: .topLeading, endPoint: .bottomTrailing)
                .overlay {
                    Text(String(identity.name.drop(while: { $0 == "@" }).prefix(1)).uppercased())
                        .font(CoffeePeekNativeStyle.font(34, weight: "Semibold", relativeTo: .largeTitle))
                        .foregroundStyle(CoffeePeekNativeStyle.color(0x1A1412))
                }
        }
    }

    @ViewBuilder
    private func stat(_ kind: OwnProfileStat) -> some View {
        let data = statValue(kind)
        if enabled(kind) {
            Button { select(kind) } label: { statLabel(data) }
                .buttonStyle(.plain)
                .disabled(kind == .followers && !identity.canOpenFollowers)
                .accessibilityElement(children: .combine)
        } else {
            statLabel(data).accessibilityElement(children: .combine)
        }
    }

    private func statLabel(_ data: (count: Int32, key: String)) -> some View {
        let compact = signedInLayout
        return VStack(alignment: compact ? .leading : .center, spacing: 0) {
            Text(data.count.formatted())
                .font(CoffeePeekNativeStyle.font(compact ? 22 : 18, weight: "Semibold", relativeTo: .title2))
                .lineLimit(compact ? 1 : nil)
            Text(strings[data.key])
                .font(CoffeePeekNativeStyle.font(compact ? 12 : 11, weight: "Medium", relativeTo: .caption))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
                .lineLimit(compact && !dynamicTypeSize.isAccessibilitySize ? 2 : nil)
                .fixedSize(horizontal: false, vertical: true)
        }
        .fixedSize(horizontal: !compact, vertical: false)
        .padding(.vertical, compact ? 4 : 0)
        .frame(maxWidth: .infinity, minHeight: 48, alignment: compact ? .leading : .center)
    }

    private func statValue(_ kind: OwnProfileStat) -> (count: Int32, key: String) {
        switch kind {
        case .checkIns: (identity.checkIns, "profile_stat_check_ins")
        case .shops: (identity.shops, "profile_stat_shops")
        case .roasters: (identity.roasters, "profile_stat_roasters")
        case .followers: (identity.followers, "profile_stat_followers")
        case .avatar: (0, "profile_open_avatar")
        }
    }
}

struct OwnProfileTabs: View {
    let selection: OwnProfileTab
    let strings: OwnProfileStrings
    let select: (OwnProfileTab) -> Void
    var equalWidth = false
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        if equalWidth && !dynamicTypeSize.isAccessibilitySize {
            HStack(spacing: 0) {
                ForEach(OwnProfileTab.allCases) { tab in tabButton(tab, fillsWidth: true) }
            }
        } else {
            ScrollView(.horizontal) {
                HStack(spacing: 16) {
                    ForEach(OwnProfileTab.allCases) { tab in tabButton(tab, fillsWidth: false) }
                }
            }
            .scrollIndicators(.hidden)
        }
    }

    private func tabButton(_ tab: OwnProfileTab, fillsWidth: Bool) -> some View {
        Button { select(tab) } label: {
            VStack(spacing: 8) {
                if equalWidth {
                    Text(strings[tab.rawValue])
                        .font(CoffeePeekNativeStyle.font(15, weight: selection == tab ? "Semibold" : "Regular"))
                        .foregroundStyle(selection == tab ? CoffeePeekNativeStyle.foreground : CoffeePeekNativeStyle.secondary)
                        .fixedSize(horizontal: !fillsWidth, vertical: false)
                        .frame(maxWidth: fillsWidth ? .infinity : nil)
                } else {
                    Text(strings[tab.rawValue]).fixedSize(horizontal: true, vertical: false)
                }
                Capsule().fill(selection == tab ? CoffeePeekNativeStyle.accent : .clear).frame(height: 2)
            }
            .frame(minHeight: 48, alignment: .center)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selection == tab ? .isSelected : [])
    }
}

struct OwnProfileRetry: View {
    let message: String
    let strings: OwnProfileStrings
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(message).foregroundStyle(CoffeePeekNativeStyle.secondary)
            Button(strings["profile_retry_full"], action: retry)
                .frame(minHeight: 44)
                .tint(CoffeePeekNativeStyle.foreground)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

typealias ProfileImageLoader = (String, @escaping (UIImage?) -> Void) -> IosOwnProfileObservation?

func profileImageLoader(_ profile: IosOwnProfile?) -> ProfileImageLoader {
    { url, receive in profile?.loadImage(url: url, onImage: receive) }
}

struct OwnProfileImage: View {
    @Environment(\.scenePhase) private var scenePhase
    let url: String?
    let symbol: String?
    let fits: Bool
    let fallbackUrl: String?
    let loadImage: ProfileImageLoader
    @State private var image: UIImage?
    @State private var observation: IosOwnProfileObservation?
    @State private var fallbackObservation: IosOwnProfileObservation?
    @State private var fallbackRequest: UUID?
    @State private var requested: UUID?
    @State private var hasShownPrimary = false
    @State private var usesFallback = false
    @State private var visible = false

    private struct Sources: Equatable {
        let preview: String?
        let fallback: String?
    }

    private var sources: Sources { Sources(preview: url, fallback: fallbackUrl) }

    init(url: String?, profile: IosOwnProfile?, symbol: String? = nil, fits: Bool = false) {
        self.init(url: url, symbol: symbol, fits: fits, loadImage: profileImageLoader(profile))
    }

    init(url: String?, symbol: String? = nil, fits: Bool = false, fallbackUrl: String? = nil,
         loadImage: @escaping ProfileImageLoader) {
        self.url = url
        self.symbol = symbol
        self.fits = fits
        self.fallbackUrl = fallbackUrl
        self.loadImage = loadImage
    }

    var body: some View {
        GeometryReader { geometry in
            Group {
                if let image {
                    Image(uiImage: image).resizable().aspectRatio(contentMode: fits ? .fit : .fill)
                        .frame(width: geometry.size.width, height: geometry.size.height)
                } else if let symbol {
                    CoffeePeekNativeStyle.surface.overlay {
                        Image(systemName: symbol).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    }
                } else {
                    CoffeePeekPhotoPlaceholder()
                }
            }
            .clipped()
        }
        .accessibilityHidden(true)
        .onAppear {
            visible = true
            load()
        }
        .onChange(of: sources) { _, _ in load() }
        .onChange(of: scenePhase) { _, _ in load() }
        .onDisappear {
            visible = false
            cancel()
        }
    }

    private func load() {
        cancel()
        guard visible, scenePhase == .active else { return }
        let primary = url.flatMap { $0.isEmpty ? nil : $0 } ?? fallbackUrl
        guard let primary, !primary.isEmpty else { return }
        let request = UUID()
        requested = request
        let handle = loadImage(primary) { next in
            guard requested == request, !usesFallback else { return }
            image = next
            if next != nil {
                hasShownPrimary = true
            } else if !hasShownPrimary, let fallbackUrl, !fallbackUrl.isEmpty, fallbackUrl != primary {
                usesFallback = true
                observation?.cancel()
                observation = nil
                loadFallback(fallbackUrl, request: request)
            }
        }
        if usesFallback { handle?.cancel() } else { observation = handle }
    }

    private func loadFallback(_ url: String, request: UUID) {
        let fallback = UUID()
        fallbackRequest = fallback
        fallbackObservation = loadImage(url) { next in
            if requested == request && fallbackRequest == fallback { image = next }
        }
    }

    private func cancelFallback() {
        fallbackRequest = nil
        fallbackObservation?.cancel()
        fallbackObservation = nil
    }

    private func cancel() {
        requested = nil
        image = nil
        observation?.cancel()
        cancelFallback()
        observation = nil
        hasShownPrimary = false
        usesFallback = false
    }
}

struct CoffeePeekPhotoPlaceholder: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        GeometryReader { geometry in
            let dark = colorScheme == .dark
            Text(verbatim: "COFFEEPEEK")
                .font(CoffeePeekNativeStyle.font(min(28, max(7, geometry.size.width * 0.15)), weight: "Bold"))
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .foregroundStyle(dark ? Color.black : Color.white)
                .padding(.horizontal, 2)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(dark ? Color.white : Color.black)
        }
    }
}

#Preview("Photo placeholder light") {
    CoffeePeekPhotoPlaceholder().frame(width: 150, height: 150).preferredColorScheme(.light)
}

#Preview("Photo placeholder dark") {
    CoffeePeekPhotoPlaceholder().frame(width: 150, height: 150).preferredColorScheme(.dark)
}

#Preview("Own profile header light") {
    OwnProfileHeader(identity: IosOwnProfileIdentity(name: "@CoffeePeek", about: "Кофе и прогулки", avatar: nil,
        checkIns: 13, shops: 4, roasters: 2, followers: 18, canOpenFollowers: true),
        strings: OwnProfileStrings(languageTag: "ru"), profile: nil, select: { _ in }, signedInLayout: true)
    .padding()
    .foregroundStyle(CoffeePeekNativeStyle.foreground)
    .background(CoffeePeekNativeStyle.background)
    .preferredColorScheme(.light)
}

#Preview("Own profile header dark") {
    OwnProfileHeader(identity: IosOwnProfileIdentity(name: "@CoffeePeek", about: "Кава і прагулкі", avatar: nil,
        checkIns: 13, shops: 4, roasters: 2, followers: 18, canOpenFollowers: true),
        strings: OwnProfileStrings(languageTag: "be"), profile: nil, select: { _ in }, signedInLayout: true)
    .padding()
    .foregroundStyle(CoffeePeekNativeStyle.foreground)
    .background(CoffeePeekNativeStyle.background)
    .preferredColorScheme(.dark)
}

#Preview("Profile statistics narrow light") {
    OwnProfileHeader(identity: IosOwnProfileIdentity(name: "@CoffeePeek", about: nil, avatar: nil,
        checkIns: 13, shops: 4, roasters: 0, followers: 18, canOpenFollowers: true),
        strings: OwnProfileStrings(languageTag: "ru"), profile: nil, select: { _ in }, signedInLayout: true)
        .padding(16).frame(width: 320)
        .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
        .preferredColorScheme(.light)
}

#Preview("Profile statistics large dark") {
    OwnProfileHeader(identity: IosOwnProfileIdentity(name: "@CoffeePeek", about: nil, avatar: nil,
        checkIns: 13, shops: 4, roasters: 0, followers: 18, canOpenFollowers: true),
        strings: OwnProfileStrings(languageTag: "be"), profile: nil, select: { _ in }, signedInLayout: true)
        .padding(16).frame(width: 320).dynamicTypeSize(.accessibility2)
        .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
        .preferredColorScheme(.dark)
}

#Preview("Profile tabs light") {
    OwnProfileTabs(selection: .checkIns, strings: OwnProfileStrings(languageTag: "ru"), select: { _ in }, equalWidth: true)
        .padding().background(CoffeePeekNativeStyle.background).preferredColorScheme(.light)
}

#Preview("Profile tabs dark") {
    OwnProfileTabs(selection: .collections, strings: OwnProfileStrings(languageTag: "be"), select: { _ in }, equalWidth: true)
        .padding().background(CoffeePeekNativeStyle.background).preferredColorScheme(.dark)
}

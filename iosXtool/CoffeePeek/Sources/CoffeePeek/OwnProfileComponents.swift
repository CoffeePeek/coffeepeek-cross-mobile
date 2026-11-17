import ComposeApp
import SwiftUI
import UIKit

enum OwnProfileStat: Equatable { case checkIns, shops, roasters, followers, avatar }

struct OwnProfileHeader: View {
    let identity: IosOwnProfileIdentity
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
    let select: (OwnProfileStat) -> Void
    var enabled: (OwnProfileStat) -> Bool = { _ in true }
    var imageLoader: ProfileImageLoader?
    var signedInLayout = false
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        if signedInLayout { signedInHeader } else { publicHeader }
    }

    private var signedInHeader: some View {
        VStack(alignment: .leading, spacing: 18) {
            if dynamicTypeSize.isAccessibilitySize {
                HStack(spacing: 12) {
                    avatar(size: 72)
                    name
                }
                counts
            } else {
                HStack(alignment: .center, spacing: 12) {
                    avatar(size: 84)
                    VStack(alignment: .leading, spacing: 12) {
                        name
                        counts
                    }
                }
            }
            bio
        }
        .frame(maxWidth: .infinity, alignment: .leading)
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

    private var counts: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .top, spacing: signedInLayout ? 4 : 6) {
                stat(.checkIns)
                stat(.shops)
                stat(.roasters)
                stat(.followers)
            }
            VStack(spacing: 4) {
                HStack { stat(.checkIns); stat(.shops) }
                HStack { stat(.roasters); stat(.followers) }
            }
        }
    }

    private var name: some View {
        Text(identity.name).font(CoffeePeekNativeStyle.font(21, weight: "Bold", relativeTo: .title2))
            .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder
    private var bio: some View {
        if let about = identity.about, !about.isEmpty {
            Text(about).foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }

    @ViewBuilder
    private func avatar(size: CGFloat) -> some View {
        let image = OwnProfileImage(url: identity.avatar, symbol: "person.crop.circle",
                                    loadImage: imageLoader ?? profileImageLoader(profile))
            .frame(width: size, height: size).clipShape(Circle())
        if let avatar = identity.avatar, !avatar.isEmpty {
            Button { select(.avatar) } label: { image }
                .buttonStyle(.plain)
                .accessibilityLabel(strings["profile_open_avatar"])
        } else {
            image.accessibilityHidden(true)
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
        let compact = signedInLayout && !dynamicTypeSize.isAccessibilitySize
        return VStack(alignment: compact ? .leading : .center, spacing: 3) {
            Text(data.count.formatted())
                .font(CoffeePeekNativeStyle.font(compact ? 21 : 18, weight: "Bold", relativeTo: .headline))
                .lineLimit(compact ? 1 : nil)
                .minimumScaleFactor(compact ? 0.72 : 1)
            Text(strings[data.key])
                .font(CoffeePeekNativeStyle.font(compact ? 12 : 11, relativeTo: .caption))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
                .lineLimit(compact ? 1 : nil)
                .minimumScaleFactor(compact ? 0.74 : 1)
        }
        .fixedSize(horizontal: !compact, vertical: false)
        .frame(maxWidth: .infinity, minHeight: 44, alignment: compact ? .leading : .center)
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
                        .font(CoffeePeekNativeStyle.font(16, weight: selection == tab ? "Semibold" : "Regular"))
                        .foregroundStyle(selection == tab ? CoffeePeekNativeStyle.foreground : CoffeePeekNativeStyle.secondary)
                        .fixedSize(horizontal: !fillsWidth, vertical: false)
                        .frame(maxWidth: fillsWidth ? .infinity : nil)
                } else {
                    Text(strings[tab.rawValue]).fixedSize(horizontal: true, vertical: false)
                }
                Capsule().fill(selection == tab ? CoffeePeekNativeStyle.accent : .clear).frame(height: 3)
            }
            .frame(minHeight: 44, alignment: equalWidth ? .bottom : .center)
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
    let url: String?
    let symbol: String?
    let fits: Bool
    let fallbackUrl: String?
    let loadImage: ProfileImageLoader
    @State private var image: UIImage?
    @State private var observation: IosOwnProfileObservation?
    @State private var fallbackObservation: IosOwnProfileObservation?
    @State private var requested: UUID?
    @State private var hasShownPrimary = false
    @State private var usesFallback = false

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
        .onAppear(perform: load)
        .onChange(of: sources) { _, _ in load() }
        .onDisappear { cancel() }
    }

    private func load() {
        cancel()
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
        fallbackObservation = loadImage(url) { next in
            if requested == request { image = next }
        }
    }

    private func cancel() {
        requested = nil
        observation?.cancel()
        fallbackObservation?.cancel()
        observation = nil
        fallbackObservation = nil
        hasShownPrimary = false
        usesFallback = false
        image = nil
    }
}

private struct CoffeePeekPhotoPlaceholder: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        GeometryReader { geometry in
            let dark = colorScheme == .dark
            Text(verbatim: "CFPK")
                .font(.system(size: min(28, max(7, geometry.size.width * 0.24)), weight: .black))
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
    .preferredColorScheme(.light)
}

#Preview("Own profile header dark") {
    OwnProfileHeader(identity: IosOwnProfileIdentity(name: "@CoffeePeek", about: "Кава і прагулкі", avatar: nil,
        checkIns: 13, shops: 4, roasters: 2, followers: 18, canOpenFollowers: true),
        strings: OwnProfileStrings(languageTag: "be"), profile: nil, select: { _ in }, signedInLayout: true)
    .padding()
    .preferredColorScheme(.dark)
}

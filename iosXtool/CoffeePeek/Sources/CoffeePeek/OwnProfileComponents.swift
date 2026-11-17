import ComposeApp
import SwiftUI
import UIKit

enum OwnProfileStat: Equatable { case checkIns, shops, roasters, followers, avatar }

struct OwnProfileHeader: View {
    let identity: IosOwnProfileIdentity
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
    let select: (OwnProfileStat) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                if let avatar = identity.avatar, !avatar.isEmpty {
                    Button { select(.avatar) } label: { avatarImage }
                        .buttonStyle(.plain)
                        .accessibilityLabel(strings["profile_open_avatar"])
                } else {
                    avatarImage.accessibilityHidden(true)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(identity.name).font(CoffeePeekNativeStyle.font(21, weight: "Bold", relativeTo: .title2))
                        .accessibilityAddTraits(.isHeader)
                    if let about = identity.about, !about.isEmpty {
                        Text(about).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    }
                }
            }
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .top, spacing: 6) { stat(.checkIns); stat(.shops); stat(.roasters); stat(.followers) }
                VStack(spacing: 4) {
                    HStack { stat(.checkIns); stat(.shops) }
                    HStack { stat(.roasters); stat(.followers) }
                }
            }
        }
    }

    private var avatarImage: some View {
        OwnProfileImage(url: identity.avatar, profile: profile, symbol: "person.crop.circle")
            .frame(width: 56, height: 56)
            .clipShape(Circle())
    }

    private func stat(_ kind: OwnProfileStat) -> some View {
        let data = statValue(kind)
        return Button { select(kind) } label: {
            VStack(spacing: 3) {
                Text(data.count.formatted())
                    .font(CoffeePeekNativeStyle.font(18, weight: "Bold", relativeTo: .headline))
                Text(strings[data.key])
                    .font(CoffeePeekNativeStyle.font(11, relativeTo: .caption))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
            .fixedSize(horizontal: true, vertical: false)
            .frame(maxWidth: .infinity, minHeight: 44)
        }
        .buttonStyle(.plain)
        .disabled(kind == .followers && !identity.canOpenFollowers)
        .accessibilityElement(children: .combine)
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

    var body: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 16) {
                ForEach(OwnProfileTab.allCases) { tab in
                    Button { select(tab) } label: {
                        VStack(spacing: 8) {
                            Text(strings[tab.rawValue]).fixedSize(horizontal: true, vertical: false)
                            Capsule().fill(selection == tab ? CoffeePeekNativeStyle.accent : .clear).frame(height: 3)
                        }
                        .frame(minHeight: 44)
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(selection == tab ? .isSelected : [])
                }
            }
        }
        .scrollIndicators(.hidden)
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

struct OwnProfileImage: View {
    let url: String?
    let profile: IosOwnProfile?
    let symbol: String
    var fits: Bool = false
    @State private var image: UIImage?
    @State private var observation: IosOwnProfileObservation?
    @State private var requested: String?

    var body: some View {
        GeometryReader { geometry in
            Group {
                if let image {
                    Image(uiImage: image).resizable().aspectRatio(contentMode: fits ? .fit : .fill)
                        .frame(width: geometry.size.width, height: geometry.size.height)
                } else {
                    CoffeePeekNativeStyle.surface.overlay {
                        Image(systemName: symbol).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    }
                }
            }
            .clipped()
        }
        .accessibilityHidden(true)
        .onAppear(perform: load)
        .onChange(of: url) { _, _ in load() }
        .onDisappear { cancel() }
    }

    private func load() {
        cancel()
        image = nil
        guard let url, !url.isEmpty else { return }
        requested = url
        observation = profile?.loadImage(url: url, onImage: { next in
            if requested == url { image = next }
        })
    }

    private func cancel() {
        observation?.cancel()
        observation = nil
        requested = nil
    }
}

#Preview("Own profile header light") {
    OwnProfileHeader(identity: IosOwnProfileIdentity(name: "CoffeePeek", about: "Кофе и прогулки", avatar: nil,
        checkIns: 13, shops: 4, roasters: 2, followers: 18, canOpenFollowers: true),
        strings: OwnProfileStrings(languageTag: "ru"), profile: nil, select: { _ in })
    .padding()
    .preferredColorScheme(.light)
}

#Preview("Own profile header dark") {
    OwnProfileHeader(identity: IosOwnProfileIdentity(name: "CoffeePeek", about: "Кава і прагулкі", avatar: nil,
        checkIns: 13, shops: 4, roasters: 2, followers: 18, canOpenFollowers: true),
        strings: OwnProfileStrings(languageTag: "be"), profile: nil, select: { _ in })
    .padding()
    .preferredColorScheme(.dark)
}

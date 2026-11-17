import ComposeApp
import SwiftUI

struct OwnProfileCheckIns: View {
    @ObservedObject var model: OwnProfileModel

    var body: some View {
        OwnProfileCheckInsContent(state: model.state, strings: model.strings, profile: model.profile,
            retry: model.profile.retryCheckIns, more: model.profile.moreCheckIns,
            clearDate: model.clearSelectedDay,
            openPhoto: { model.openPhoto(id: $0, index: $1) }, openEntry: model.profile.openCheckIn)
    }
}

private struct OwnProfileCheckInsContent: View {
    let state: IosOwnProfileState
    let strings: OwnProfileStrings
    var profile: IosOwnProfile? = nil
    var retry: () -> Void = {}
    var more: () -> Void = {}
    var clearDate: () -> Void = {}
    var openPhoto: (String, Int) -> Void = { _, _ in }
    var openEntry: (String) -> Void = { _ in }

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 16) {
            if let date = state.selectedDate {
                HStack {
                    Text(date).font(CoffeePeekNativeStyle.font(15, weight: "Semibold"))
                    Spacer()
                    Button(strings["profile_check_ins_clear_date"], action: clearDate).frame(minHeight: 44)
                }
            }
            feedback
            ForEach(state.checkIns, id: \.id) { checkIn in
                OwnProfileCheckInCard(checkIn: checkIn, strings: strings, profile: profile,
                    openShop: { profile?.openShop(slug: $0) }, openCoffee: { profile?.openCoffee(slug: $0) },
                    openPhoto: { openPhoto(checkIn.id, $0) }, openEntry: { openEntry(checkIn.id) }, timeline: true)
            }
            pagination
        }
    }

    @ViewBuilder private var feedback: some View {
        if state.checkInsFailed {
            OwnProfileRetry(message: strings["profile_check_ins_failed"], strings: strings, retry: retry)
        } else if state.checkInsLoading && state.checkIns.isEmpty {
            ProgressView().frame(maxWidth: .infinity, minHeight: 64).accessibilityLabel(strings["profile_loading"])
        } else if state.checkIns.isEmpty {
            Text(strings["profile_check_ins_empty"]).foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }

    @ViewBuilder private var pagination: some View {
        if state.loadingMoreCheckIns {
            ProgressView().frame(maxWidth: .infinity, minHeight: 44).accessibilityLabel(strings["profile_loading"])
        } else if state.moreCheckInsFailed {
            OwnProfileRetry(message: strings["profile_check_ins_failed"], strings: strings, retry: retry)
        } else if state.moreCheckIns && !state.checkInsLoading && !state.checkInsFailed {
            Color.clear.frame(height: 1).onAppear(perform: more).accessibilityHidden(true)
        }
    }
}

struct OwnProfileCheckInCard: View {
    let checkIn: IosOwnProfileCheckIn
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
    let openShop: (String) -> Void
    let openCoffee: (String) -> Void
    let openPhoto: (Int) -> Void
    var openEntry: (() -> Void)?
    var imageLoader: ProfileImageLoader?
    var openMenu: (() -> Void)?
    var report: (() -> Void)?
    var ratingDescription: String?
    var timeline = false

    private var loadImage: ProfileImageLoader { imageLoader ?? profileImageLoader(profile) }

    var body: some View {
        if timeline {
            content
        } else {
            content
                .padding(12)
                .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
                .overlay { RoundedRectangle(cornerRadius: 16).strokeBorder(CoffeePeekNativeStyle.border).allowsHitTesting(false) }
        }
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: timeline ? 8 : 10) {
            author
            heading
            if !checkIn.note.isEmpty {
                if timeline {
                    Text(checkIn.note).font(CoffeePeekNativeStyle.font(17))
                        .fixedSize(horizontal: false, vertical: true)
                } else {
                    Text(checkIn.note)
                }
            }
            photos
            footer
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var author: some View {
        HStack(spacing: timeline ? 12 : 8) {
            authorAvatar
                .frame(width: timeline ? 44 : 32, height: timeline ? 44 : 32).clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(checkIn.name).font(CoffeePeekNativeStyle.font(timeline ? 15 : 14, weight: "Semibold"))
                if !checkIn.date.isEmpty {
                    Group {
                        if timeline {
                            Text(profileTimelineDate(checkIn.date, language: strings.languageTag))
                                .font(CoffeePeekNativeStyle.font(12, weight: "Medium", relativeTo: .caption))
                        } else {
                            Text(checkIn.date)
                        }
                    }.foregroundStyle(CoffeePeekNativeStyle.secondary)
                }
            }
            Spacer(minLength: 0)
            if let openEntry {
                Button(action: openEntry) { Image(systemName: "chevron.right").frame(width: 44, height: 44) }
                    .buttonStyle(.plain).accessibilityLabel(strings["profile_check_in_open"])
            }
            if let openMenu, let report {
                Menu {
                    Button(strings["profile_check_in_report"], systemImage: "flag", action: report)
                    Button(strings["profile_block_author"], systemImage: "nosign", role: .destructive, action: openMenu)
                } label: {
                    Image(systemName: "ellipsis").frame(width: 44, height: 44)
                }
                .accessibilityLabel(strings["profile_check_in_actions"])
            }
        }
    }

    @ViewBuilder
    private var authorAvatar: some View {
        if let url = checkIn.avatar, !url.isEmpty {
            OwnProfileImage(url: url, symbol: "person.crop.circle", loadImage: loadImage)
        } else {
            CoffeePeekNativeStyle.color(0xF8F1DD).overlay {
                Text(String(checkIn.name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(1)).uppercased())
                    .font(CoffeePeekNativeStyle.font(17, weight: "Semibold"))
                    .foregroundStyle(CoffeePeekNativeStyle.color(0x1C1917))
            }
            .accessibilityHidden(true)
        }
    }

    @ViewBuilder
    private var heading: some View {
        if checkIn.home {
            if let brew = checkIn.brew {
                OwnProfileBrewPreview(brew: brew, strings: strings, loadImage: loadImage,
                                      openCoffee: openCoffee, timeline: timeline)
            } else {
                Text(strings["history_activity_at_home"]).font(CoffeePeekNativeStyle.font(17, weight: "Semibold"))
            }
        } else if let slug = checkIn.shopSlug, !slug.isEmpty {
            Button { openShop(slug) } label: {
                Text(shopName).frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
            }
            .buttonStyle(.plain)
            .font(CoffeePeekNativeStyle.font(22, weight: "Semibold", relativeTo: .title2))
        } else {
            Text(shopName).font(CoffeePeekNativeStyle.font(22, weight: "Semibold", relativeTo: .title2))
        }
        if let score = checkIn.score {
            rating(score)
        }
    }

    private func rating(_ score: String) -> some View {
        HStack(spacing: 8) {
            HStack(spacing: 4) {
                ForEach(0..<5) { index in
                    ratingStar(fill: min(1, max(0, (Double(score.replacingOccurrences(of: ",", with: ".")) ?? 0) - Double(index))))
                }
            }
            Text(score).font(CoffeePeekNativeStyle.font(15))
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(ratingDescription ?? String(format: strings["profile_native_rating"], score))
    }

    private func ratingStar(fill: Double) -> some View {
        Image(systemName: "star").font(.system(size: 20))
            .foregroundStyle(CoffeePeekNativeStyle.secondary)
            .overlay(alignment: .leading) {
                Image(systemName: "star.fill").font(.system(size: 20))
                    .foregroundStyle(CoffeePeekNativeStyle.accent)
                    .mask(alignment: .leading) { Rectangle().frame(width: 22 * fill) }
            }
            .frame(width: 22, height: 22)
    }

    private var shopName: String {
        checkIn.shop.isEmpty ? strings["profile_favorite_shop"] : checkIn.shop
    }

    @ViewBuilder
    private var photos: some View {
        if timeline {
            timelinePhotos
        } else if !checkIn.photos.isEmpty {
            ScrollView(.horizontal) {
                HStack(spacing: 8) {
                    ForEach(checkIn.photos.indices, id: \.self) { index in
                        photoTile(index, width: 92, height: 92)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var timelinePhotos: some View {
        let indices = checkIn.photos.indices.filter { timelinePhotoURL(checkIn.photos[$0]) != nil }
        if !indices.isEmpty {
            GeometryReader { geometry in
                photoGrid(indices, size: geometry.size)
            }
            .aspectRatio(indices.count == 1 ? 2 : 4.0 / 3, contentMode: .fit)
        } else if let index = checkIn.photos.firstIndex(where: { $0.canOpen }) {
            photoTile(index, width: 44, height: 44)
        }
    }

    @ViewBuilder
    private func photoGrid(_ indices: [Int], size: CGSize) -> some View {
        if indices.count == 1 {
            photoTile(indices[0], width: size.width, height: size.height)
        } else {
            let width = max(0, (size.width - 4) / 2)
            HStack(spacing: 4) {
                photoTile(indices[0], width: width, height: size.height)
                if indices.count == 2 {
                    photoTile(indices[1], width: width, height: size.height)
                } else {
                    let height = max(0, (size.height - 4) / 2)
                    VStack(spacing: 4) {
                        photoTile(indices[1], width: width, height: height)
                        photoTile(indices[2], width: width, height: height, more: indices.count - 3)
                    }
                }
            }
        }
    }

    private func photoTile(_ index: Int, width: CGFloat, height: CGFloat, more: Int = 0) -> some View {
        let photo = checkIn.photos[index]
        let url = timeline ? timelinePhotoURL(photo) : photo.url
        return Button { openPhoto(index) } label: {
            OwnProfileImage(url: url, symbol: "photo", fallbackUrl: timeline ? photo.fallbackUrl : nil, loadImage: loadImage)
                .frame(width: width, height: height)
                .overlay {
                    if more > 0 {
                        Color.black.opacity(0.35).overlay { Text("+\(more)").font(.headline).foregroundStyle(.white) }
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: timeline ? 12 : 8))
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!photo.canOpen)
        .accessibilityLabel(String(format: strings["profile_native_photo"], index + 1, checkIn.photos.count))
    }

    private func timelinePhotoURL(_ photo: IosOwnProfilePhoto) -> String? {
        if let url = photo.url, !url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return url }
        if let url = photo.fallbackUrl, !url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return url }
        return nil
    }

    @ViewBuilder
    private var footer: some View {
        if timeline {
            footerContent.font(CoffeePeekNativeStyle.font(15, relativeTo: .subheadline))
        } else {
            footerContent
        }
    }

    private var footerContent: some View {
        HStack {
            Label(checkIn.helpfulCount.formatted(), systemImage: checkIn.liked ? "heart.fill" : "heart")
                .frame(minWidth: 48, minHeight: 48, alignment: .leading)
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .accessibilityLabel(strings.count("profile_check_in_likes", Int(checkIn.helpfulCount)))
            Spacer()
            if let drink = checkIn.drink { Text(drink) }
        }
        .foregroundStyle(CoffeePeekNativeStyle.secondary)
    }
}

private func profileTimelineDate(_ value: String, language: String) -> String {
    guard let day = value.split(separator: " ", maxSplits: 1).first else { return value }
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.dateFormat = "dd.MM.yyyy"
    guard let date = formatter.date(from: String(day)) else { return value }
    formatter.locale = Locale(identifier: language)
    formatter.dateFormat = "d MMM yyyy"
    return formatter.string(from: date)
}

private struct OwnProfileBrewPreview: View {
    let brew: IosOwnProfileBrew
    let strings: OwnProfileStrings
    let loadImage: ProfileImageLoader
    let openCoffee: (String) -> Void
    var timeline = false

    private var cover: String? {
        guard timeline, let url = brew.cover else { return brew.cover }
        return url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : url
    }

    var body: some View {
        Group {
            if let coffee = brew.coffee, !coffee.isEmpty, cover != nil {
                Button { openCoffee(coffee) } label: { row(link: true) }
                    .buttonStyle(.plain)
                    .accessibilityHint(strings["profile_open_coffee"])
            } else {
                row(link: false)
            }
        }
        .accessibilityElement(children: .combine)
    }

    private func row(link: Bool) -> some View {
        HStack(spacing: timeline ? 12 : 10) {
            thumbnail
            VStack(alignment: .leading, spacing: timeline ? (cover == nil ? 8 : 2) : 3) {
                if timeline { timelineHeading } else { standardHeading }
                if !details.isEmpty {
                    recipeDetails
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if link {
                if timeline {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 18))
                        .frame(width: 48, height: 48)
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                        .accessibilityHidden(true)
                } else {
                    Image(systemName: "chevron.right")
                }
            }
        }
        .frame(minHeight: 44)
        .padding(.vertical, timeline && cover != nil ? 4 : 0)
        .contentShape(Rectangle())
    }

    @ViewBuilder
    private var thumbnail: some View {
        if let cover {
            OwnProfileImage(url: cover, symbol: "cup.and.saucer", fits: true, loadImage: loadImage)
                .frame(width: timeline ? 44 : 52, height: timeline ? 54 : 52)
                .clipShape(RoundedRectangle(cornerRadius: timeline ? 4 : 8))
        }
    }

    @ViewBuilder
    private var standardHeading: some View {
        Text(title).font(.headline)
        if let roaster = brew.roaster, !roaster.isEmpty {
            Text(roaster).foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }

    private var recipeDetails: some View {
        Group {
            if timeline {
                Text(details).font(CoffeePeekNativeStyle.font(15, relativeTo: .subheadline))
            } else {
                Text(details)
            }
        }.foregroundStyle(CoffeePeekNativeStyle.secondary)
    }

    private var timelineHeading: Text {
        let name = Text(title).font(CoffeePeekNativeStyle.font(17, weight: "Semibold", relativeTo: .headline))
        guard cover != nil, let roaster = brew.roaster, !roaster.isEmpty else { return name }
        let origin = Text(strings["profile_check_in_separator"] + roaster)
            .font(CoffeePeekNativeStyle.font(17, weight: "Medium", relativeTo: .headline))
            .foregroundColor(CoffeePeekNativeStyle.secondary)
        return Text("\(name)\(origin)")
    }

    private var title: String {
        if let bean = brew.bean, !bean.isEmpty { return bean }
        return strings[cover != nil ? "profile_shelf_bag" : "history_activity_at_home"]
    }

    private var details: String {
        var values: [String] = []
        if let method = brew.method, !method.isEmpty {
            let key = "brew_\(method)"
            let label = strings[key]
            values.append(label == key ? method : label)
        }
        if let dose = brew.dose { values.append(String(format: strings["profile_check_in_dose"], dose)) }
        if brew.seconds >= 0 { values.append(strings.count("profile_check_in_seconds", Int(brew.seconds))) }
        if brew.taste >= 0 { values.append(strings.count("profile_check_in_taste", Int(brew.taste))) }
        return values.joined(separator: strings["profile_check_in_separator"])
    }
}

struct OwnProfileFollowersSheet: View {
    @ObservedObject var model: OwnProfileModel

    var body: some View {
        let followers = model.state.followers
        VStack(alignment: .leading) {
            HStack {
                Text(model.strings["profile_stat_followers"]).font(.headline).accessibilityAddTraits(.isHeader)
                Spacer()
                Button(model.strings["profile_cancel"], action: model.profile.closeFollowers)
                    .frame(minHeight: 44)
            }
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 6) {
                    ForEach(followers.people, id: \.slug) { person in
                        Button { model.profile.openFollower(slug: person.slug) } label: {
                            HStack(spacing: 8) {
                                OwnProfileImage(url: person.avatar, profile: model.profile, symbol: "person.crop.circle")
                                    .frame(width: 40, height: 40).clipShape(Circle())
                                Text(person.name)
                                Spacer()
                                Image(systemName: "chevron.right")
                            }
                            .frame(minHeight: 44)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                    if followers.loading {
                        ProgressView().frame(maxWidth: .infinity, minHeight: 44)
                    } else if followers.failed {
                        OwnProfileRetry(message: model.strings["profile_followers_failed"], strings: model.strings,
                                        retry: model.profile.retryFollowers)
                    } else if followers.more {
                        Button(model.strings["profile_load_more"], action: model.profile.moreFollowers)
                            .frame(minHeight: 44)
                    } else if followers.people.isEmpty {
                        Text(model.strings["profile_followers_empty"]).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    }
                }
            }
        }
        .padding(16)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background)
    }
}

private func ownProfileCheckInPreview(language: String) -> some View {
    let brew = IosOwnProfileBrew(bean: "Ethiopia", roaster: "CoffeePeek", method: "pour_over",
                                dose: "15", seconds: 180, taste: 4, cover: nil, coffee: nil)
    let row = IosOwnProfileCheckIn(id: "preview", name: "CoffeePeek", avatar: nil, shop: "", shopSlug: nil, home: true,
                                  note: "", date: "17.11.2026 13:45", drink: "V60", helpfulCount: 2, liked: false,
                                  score: nil, photos: [], brew: brew)
    return OwnProfileCheckInCard(checkIn: row, strings: OwnProfileStrings(languageTag: language), profile: nil,
                                 openShop: { _ in }, openCoffee: { _ in }, openPhoto: { _ in }, timeline: true).padding()
        .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
}

#Preview("Own check-in light") {
    ownProfileCheckInPreview(language: "ru").preferredColorScheme(.light)
}

#Preview("Own check-in dark") {
    ownProfileCheckInPreview(language: "be").preferredColorScheme(.dark)
}

private func ownProfileHistoryPreview(selectedDate: String?) -> some View {
    let rows = [false, true].map { home in
        IosOwnProfileCheckIn(id: home ? "home:fixture" : "shop:fixture", name: "CoffeePeek", avatar: nil,
            shop: home ? "" : "CoffeePeek", shopSlug: nil, home: home, note: "", date: "17.11.2026 12:00",
            drink: nil, helpfulCount: 0, liked: false, score: nil, photos: [], brew: nil)
    }
    let state = IosOwnProfileState(active: true, identity: nil, loading: false, refreshing: false,
        failed: false, refreshFailed: false, checkInsLoading: false, checkInsFailed: false, checkIns: rows,
        followers: IosOwnProfileFollowers(open: false, loading: false, failed: false, more: false, people: []),
        moreCheckIns: false, loadingMoreCheckIns: false, moreCheckInsFailed: false, selectedDate: selectedDate)
    return ScrollView {
        OwnProfileCheckInsContent(state: state, strings: OwnProfileStrings(languageTag: "ru")).padding(16)
    }.foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
}

#Preview("Merged history light") {
    ownProfileHistoryPreview(selectedDate: nil).preferredColorScheme(.light)
}

#Preview("Merged history selected dark") {
    ownProfileHistoryPreview(selectedDate: "2026-11-17").preferredColorScheme(.dark)
}

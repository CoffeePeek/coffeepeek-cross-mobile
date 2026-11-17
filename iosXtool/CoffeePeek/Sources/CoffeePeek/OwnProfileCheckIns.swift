import ComposeApp
import SwiftUI

struct OwnProfileCheckIns: View {
    @ObservedObject var model: OwnProfileModel

    var body: some View {
        let state = model.state
        VStack(alignment: .leading, spacing: 10) {
            if state.checkInsFailed {
                OwnProfileRetry(message: model.strings["profile_check_ins_failed"], strings: model.strings,
                                retry: model.profile.retryCheckIns)
            } else if state.checkInsLoading && state.checkIns.isEmpty {
                ProgressView().frame(maxWidth: .infinity, minHeight: 64)
                    .accessibilityLabel(model.strings["profile_loading"])
            } else if state.checkIns.isEmpty {
                Text(model.strings["profile_check_ins_empty"]).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
            ForEach(state.checkIns, id: \.id) { checkIn in
                OwnProfileCheckInCard(checkIn: checkIn, strings: model.strings, profile: model.profile,
                    openShop: { model.profile.openShop(slug: $0) },
                    openCoffee: { model.profile.openCoffee(slug: $0) },
                    openPhoto: { model.openPhoto(id: checkIn.id, index: $0) })
            }
            Button(model.strings["profile_section_all_check_ins"], action: model.profile.openHistory)
                .frame(maxWidth: .infinity, minHeight: 44)
                .tint(CoffeePeekNativeStyle.foreground)
        }
    }
}

private struct OwnProfileCheckInCard: View {
    let checkIn: IosOwnProfileCheckIn
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
    let openShop: (String) -> Void
    let openCoffee: (String) -> Void
    let openPhoto: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            author
            heading
            if !checkIn.note.isEmpty { Text(checkIn.note) }
            photos
            footer
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
        .overlay { RoundedRectangle(cornerRadius: 16).strokeBorder(CoffeePeekNativeStyle.border).allowsHitTesting(false) }
    }

    private var author: some View {
        HStack(spacing: 8) {
            OwnProfileImage(url: checkIn.avatar, profile: profile, symbol: "person.crop.circle")
                .frame(width: 32, height: 32).clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(checkIn.name).font(CoffeePeekNativeStyle.font(14, weight: "Semibold"))
                if !checkIn.date.isEmpty {
                    Text(checkIn.date).foregroundStyle(CoffeePeekNativeStyle.secondary)
                }
            }
        }
    }

    @ViewBuilder
    private var heading: some View {
        if checkIn.home {
            if let brew = checkIn.brew {
                OwnProfileBrewPreview(brew: brew, strings: strings, profile: profile, openCoffee: openCoffee)
            } else {
                Text(strings["history_activity_at_home"]).font(.headline)
            }
        } else if let slug = checkIn.shopSlug, !slug.isEmpty {
            Button { openShop(slug) } label: {
                Label(shopName, systemImage: "arrow.up.right").frame(minHeight: 44)
            }
            .buttonStyle(.plain)
            .font(.headline)
        } else {
            Text(shopName).font(.headline)
        }
        if let score = checkIn.score {
            Label(score, systemImage: "star.fill").accessibilityLabel(String(format: strings["profile_native_rating"], score))
        }
    }

    private var shopName: String {
        checkIn.shop.isEmpty ? strings["profile_favorite_shop"] : checkIn.shop
    }

    @ViewBuilder
    private var photos: some View {
        if !checkIn.photos.isEmpty {
            ScrollView(.horizontal) {
                HStack(spacing: 8) {
                    ForEach(checkIn.photos.indices, id: \.self) { index in
                        let photo = checkIn.photos[index]
                        Button { openPhoto(index) } label: {
                            OwnProfileImage(url: photo.url, profile: profile, symbol: "photo")
                                .frame(width: 92, height: 92).clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                        .buttonStyle(.plain)
                        .disabled(!photo.canOpen)
                        .accessibilityLabel(String(format: strings["profile_native_photo"], index + 1, checkIn.photos.count))
                    }
                }
            }
        }
    }

    private var footer: some View {
        HStack {
            Label(checkIn.helpfulCount.formatted(), systemImage: checkIn.liked ? "heart.fill" : "heart")
                .accessibilityLabel(strings.count("profile_check_in_likes", Int(checkIn.helpfulCount)))
            Spacer()
            if let drink = checkIn.drink { Text(drink) }
        }
        .foregroundStyle(CoffeePeekNativeStyle.secondary)
    }
}

private struct OwnProfileBrewPreview: View {
    let brew: IosOwnProfileBrew
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
    let openCoffee: (String) -> Void

    var body: some View {
        Group {
            if let coffee = brew.coffee, !coffee.isEmpty, brew.cover != nil {
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
        HStack(spacing: 10) {
            if brew.cover != nil {
                OwnProfileImage(url: brew.cover, profile: profile, symbol: "cup.and.saucer", fits: true)
                    .frame(width: 52, height: 52).clipShape(RoundedRectangle(cornerRadius: 8))
            }
            VStack(alignment: .leading, spacing: 3) {
                if let bean = brew.bean, !bean.isEmpty { Text(bean).font(.headline) }
                else if brew.cover != nil { Text(strings["profile_shelf_bag"]).font(.headline) }
                else { Text(strings["history_activity_at_home"]).font(.headline) }
                if let roaster = brew.roaster, !roaster.isEmpty { Text(roaster).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                if !details.isEmpty { Text(details).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            }
            Spacer(minLength: 0)
            if link { Image(systemName: "chevron.right") }
        }
        .frame(minHeight: 44)
        .contentShape(Rectangle())
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
                                  note: "", date: "17.11.2026", drink: "V60", helpfulCount: 2, liked: false,
                                  score: nil, photos: [], brew: brew)
    return OwnProfileCheckInCard(checkIn: row, strings: OwnProfileStrings(languageTag: language), profile: nil,
                                 openShop: { _ in }, openCoffee: { _ in }, openPhoto: { _ in }).padding()
}

#Preview("Own check-in light") {
    ownProfileCheckInPreview(language: "ru").preferredColorScheme(.light)
}

#Preview("Own check-in dark") {
    ownProfileCheckInPreview(language: "be").preferredColorScheme(.dark)
}

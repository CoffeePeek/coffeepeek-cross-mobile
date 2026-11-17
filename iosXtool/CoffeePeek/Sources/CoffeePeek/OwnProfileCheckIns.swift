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
                .padding(.horizontal, 16)
            }
            feedback.padding(.horizontal, 16)
            ForEach(state.checkIns, id: \.id) { checkIn in
                OwnProfileCheckInCard(checkIn: checkIn, strings: strings, profile: profile,
                    openShop: { profile?.openShop(slug: $0) }, openCoffee: { profile?.openCoffee(slug: $0) },
                    openPhoto: { openPhoto(checkIn.id, $0) }, openEntry: { openEntry(checkIn.id) })
            }
            pagination.padding(.horizontal, 16)
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
                                 openShop: { _ in }, openCoffee: { _ in }, openPhoto: { _ in })
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

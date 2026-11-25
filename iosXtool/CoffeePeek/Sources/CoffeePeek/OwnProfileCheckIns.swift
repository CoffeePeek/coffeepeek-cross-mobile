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

struct OwnProfileConnectionsSheet: View {
    @ObservedObject var model: OwnProfileModel

    var body: some View {
        CoffeePeekProfileConnectionsSheet(state: model.state.connections, strings: model.strings,
            loadImage: profileImageLoader(model.profile), close: model.profile.closeConnections,
            select: { model.profile.openConnections(followers: $0) }, retry: model.profile.retryConnections,
            more: model.profile.moreConnections, openUser: { model.profile.openConnection(slug: $0) })
    }
}

struct CoffeePeekProfileConnectionsSheet: View {
    let state: IosPublicProfileConnections
    let strings: OwnProfileStrings
    let loadImage: ProfileImageLoader
    let close: () -> Void
    let select: (Bool) -> Void
    let retry: () -> Void
    let more: () -> Void
    let openUser: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            CoffeePeekProfileSheetHeader(title: strings[state.followers ? "profile_followers" : "profile_following_title"],
                closeLabel: strings["profile_close"], close: close)
            Picker(strings["profile_followers"], selection: Binding(get: { state.followers }, set: { select($0) })) {
                Text(strings["profile_followers"]).tag(true)
                Text(strings["profile_following_title"]).tag(false)
            }.pickerStyle(.segmented)
            people
        }.padding(16).foregroundStyle(CoffeePeekNativeStyle.foreground)
            .background(CoffeePeekNativeStyle.background)
            .presentationDetents([.medium, .large]).presentationDragIndicator(.visible)
    }

    private var people: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 6) {
                ForEach(state.people, id: \.slug) { person in
                    CoffeePeekProfilePersonRow(person: person, hint: strings["profile_open_user"], loadImage: loadImage) {
                        openUser(person.slug)
                    }
                }
                if state.loading { ProgressView().frame(maxWidth: .infinity, minHeight: 44) }
                else if state.failed {
                    OwnProfileRetry(message: strings[state.followers ? "profile_followers_failed" : "profile_following_failed"],
                        strings: strings, retry: retry)
                } else if state.people.isEmpty {
                    Text(strings[state.followers ? "profile_followers_empty" : "profile_following_empty"])
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                } else if state.more { Button(strings["profile_load_more"], action: more).frame(minHeight: 44) }
            }
        }.id(state.followers)
    }
}

@MainActor
private func profileConnectionsSheetPreview(_ followers: Bool) -> some View {
    CoffeePeekProfileConnectionsSheet(state: IosPublicProfileConnections(open: true, followers: followers,
        loading: false, failed: false, more: false, people: [IosOwnProfileFollower(slug: "preview", name: "CoffeePeek", avatar: nil)]),
        strings: OwnProfileStrings(languageTag: "ru"), loadImage: { _, _ in nil },
        close: {}, select: { _ in }, retry: {}, more: {}, openUser: { _ in })
}
#Preview("Followers sheet light") { profileConnectionsSheetPreview(true).preferredColorScheme(.light) }
#Preview("Following sheet dark") { profileConnectionsSheetPreview(false).preferredColorScheme(.dark) }

@MainActor
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

@MainActor
private func ownProfileHistoryPreview(selectedDate: String?) -> some View {
    let rows = [false, true].map { home in
        IosOwnProfileCheckIn(id: home ? "home:fixture" : "shop:fixture", name: "CoffeePeek", avatar: nil,
            shop: home ? "" : "CoffeePeek", shopSlug: nil, home: home, note: "", date: "17.11.2026 12:00",
            drink: nil, helpfulCount: 0, liked: false, score: nil, photos: [], brew: nil)
    }
    let state = IosOwnProfileState(active: true, identity: nil, loading: false, refreshing: false,
        failed: false, refreshFailed: false, checkInsLoading: false, checkInsFailed: false, checkIns: rows,
        connections: IosPublicProfileConnections(open: false, followers: true, loading: false, failed: false, more: false, people: []),
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

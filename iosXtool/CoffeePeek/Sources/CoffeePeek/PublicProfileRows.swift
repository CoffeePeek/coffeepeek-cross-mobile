import ComposeApp
import SwiftUI

struct PublicProfileRows: View {
    @ObservedObject var model: PublicProfileModel
    let full: Bool

    var body: some View {
        let state = model.state.checkIns
        let rows = full ? state.rows : Array(state.rows.prefix(2))
        LazyVStack(alignment: .leading, spacing: 10) {
            ForEach(rows, id: \.id, content: card)
            if state.loading {
                ProgressView().frame(maxWidth: .infinity, minHeight: 44)
                    .accessibilityLabel(model.strings["profile_loading"])
            } else if state.failed {
                OwnProfileRetry(message: model.strings["profile_check_ins_failed"], strings: model.strings) {
                    model.profile?.retryCheckIns()
                }
            } else if rows.isEmpty {
                Text(model.strings["profile_check_ins_empty"]).foregroundStyle(CoffeePeekNativeStyle.secondary)
            } else if full && state.more {
                Button(model.strings["profile_load_more"]) { model.profile?.moreCheckIns() }
                    .frame(maxWidth: .infinity, minHeight: 44)
            }
            if !full {
                Button(model.strings["profile_section_all_check_ins"]) { model.profile?.openCheckIns() }
                    .frame(maxWidth: .infinity, minHeight: 44)
            }
        }
    }

    private func card(_ row: IosOwnProfileCheckIn) -> some View {
        let canModerate = model.state.signedIn && !model.state.own
        return OwnProfileCheckInCard(checkIn: row, strings: model.strings, profile: nil,
            openShop: { model.profile?.openShop(slug: $0) },
            openCoffee: { model.profile?.openCoffee(slug: $0) },
            openPhoto: { model.openPhoto(id: row.id, index: $0) },
            imageLoader: model.loadImage,
            openMenu: canModerate ? { model.profile?.blockOrUnblock() } : nil,
            report: canModerate ? { model.profile?.reportCheckIn(id: row.id) } : nil,
            ratingDescription: ratingDescription(row)).padding(.horizontal, -16)
    }

    private func ratingDescription(_ row: IosOwnProfileCheckIn) -> String? {
        guard let rating = model.state.checkIns.ratings[row.id], let score = row.score else { return nil }
        return String(format: model.strings["profile_check_in_rating"], rating.coffee, rating.service, rating.place, score)
    }
}

struct PublicProfileWeek: View {
    @ObservedObject var model: PublicProfileModel

    var body: some View {
        let state = model.state.activity
        VStack(alignment: .leading, spacing: 8) {
            if state.loading {
                ProgressView().frame(maxWidth: .infinity, minHeight: 44)
                    .accessibilityLabel(model.strings["profile_week_loading"])
            }
            if !state.days.isEmpty {
                Button { model.profile?.openCheckIns() } label: {
                    HStack(spacing: 4) {
                        ForEach(state.days.indices, id: \.self) { index in
                            dayCell(state.days[index], index: index)
                        }
                    }
                    .frame(minHeight: 44)
                }
                .buttonStyle(.plain)
                .accessibilityElement(children: .contain)
                .accessibilityLabel(model.strings["profile_week_open"])
            }
            if state.failed {
                OwnProfileRetry(message: model.strings["profile_week_error"], strings: model.strings) {
                    model.profile?.retryActivity()
                }
            }
        }
    }

    private func dayCell(_ day: IosPublicProfileDay, index: Int) -> some View {
        VStack(spacing: 4) {
            Text(model.strings["history_weekday_\(index)"])
                .font(CoffeePeekNativeStyle.font(11, relativeTo: .caption))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
            OwnProfileImage(url: day.photo, symbol: day.photo == nil ? "minus" : nil,
                loadImage: model.loadImage)
                .frame(height: 32).clipShape(RoundedRectangle(cornerRadius: 6))
                .overlay {
                    if day.today {
                        RoundedRectangle(cornerRadius: 6).strokeBorder(CoffeePeekNativeStyle.accent, lineWidth: 2)
                    }
                }
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(String(format: model.strings["profile_week_day"], day.date,
            model.strings.count("history_profile_day_count", Int(day.count))))
    }
}

struct PublicProfileConnectionsSheet: View {
    @ObservedObject var model: PublicProfileModel

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            topBar
            picker
            people
        }
        .padding(16)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background)
        .presentationDetents([.medium, .large])
    }

    private var topBar: some View {
        HStack {
            Text(model.strings[label(followers: model.state.connections.followers)])
                .font(.headline).accessibilityAddTraits(.isHeader)
            Spacer()
            Button(model.strings["profile_close"]) { model.profile?.closeConnections() }
                .frame(minHeight: 44)
        }
    }

    private var picker: some View {
        Picker(model.strings["profile_followers"], selection: Binding(
            get: { model.state.connections.followers },
            set: { model.profile?.openConnections(followers: $0) })) {
                Text(model.strings[label(followers: true)]).tag(true)
                Text(model.strings[label(followers: false)]).tag(false)
            }
            .pickerStyle(.segmented)
    }

    private var people: some View {
        let state = model.state.connections
        return ScrollView {
            LazyVStack(alignment: .leading, spacing: 6) {
                ForEach(state.people, id: \.slug, content: person)
                if state.loading {
                    ProgressView().frame(maxWidth: .infinity, minHeight: 44)
                } else if state.failed {
                    OwnProfileRetry(message: model.strings[state.followers ? "profile_followers_failed" : "profile_following_failed"],
                        strings: model.strings) { model.profile?.retryConnections() }
                } else if state.people.isEmpty {
                    Text(model.strings[state.followers ? "profile_followers_empty" : "profile_following_empty"])
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                } else if state.more {
                    Button(model.strings["profile_load_more"]) { model.profile?.moreConnections() }
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
            }
        }
        .id(state.followers)
    }

    private func person(_ person: IosOwnProfileFollower) -> some View {
        Button { model.profile?.openUser(slug: person.slug) } label: {
            HStack(spacing: 8) {
                OwnProfileImage(url: person.avatar, symbol: "person.crop.circle", loadImage: model.loadImage)
                    .frame(width: 40, height: 40).clipShape(Circle())
                Text(person.name)
                Spacer()
                Image(systemName: "chevron.right").accessibilityHidden(true)
            }
            .frame(minHeight: 44).contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(person.slug.isEmpty)
        .accessibilityHint(model.strings["profile_open_user"])
    }

    private func label(followers: Bool) -> String {
        followers ? "profile_followers" : "profile_following_title"
    }
}

import ComposeApp
import SwiftUI

struct PublicProfileRows: View {
    @ObservedObject var model: PublicProfileModel
    let full: Bool

    var body: some View {
        let state = model.state.checkIns
        let rows = full ? state.rows : Array(state.rows.prefix(2))
        LazyVStack(alignment: .leading, spacing: 16) {
            if !full {
                HStack {
                    Text(model.strings["profile_section_check_ins"])
                        .font(CoffeePeekNativeStyle.font(22, weight: "SemiBold", relativeTo: .title2))
                        .accessibilityAddTraits(.isHeader)
                    Spacer()
                    CoffeePeekNativeChevronButton(label: model.strings["profile_section_all_check_ins"]) {
                        model.profile?.openCheckIns()
                    }
                }
            }
            ForEach(rows, id: \.id, content: card)
            if state.loading {
                CoffeePeekNativeLoader(size: 24).frame(maxWidth: .infinity, minHeight: 44)
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
        }
    }

    private func card(_ row: IosOwnProfileCheckIn) -> some View {
        let canModerate = model.state.signedIn && !model.state.own
        return VStack(alignment: .leading, spacing: 8) {
            checkInCard(row, canModerate: canModerate)
            if model.state.checkIns.likeErrorIds.contains(row.id) {
                OwnProfileRetry(message: model.strings["profile_check_in_like_failed"], strings: model.strings) {
                    model.profile?.toggleCheckInLike(id: row.id)
                }
            }
        }
    }

    private func checkInCard(_ row: IosOwnProfileCheckIn, canModerate: Bool) -> some View {
        OwnProfileCheckInCard(checkIn: row, strings: model.strings, profile: nil,
            openShop: { model.profile?.openShop(slug: $0) },
            openCoffee: { model.profile?.openCoffee(slug: $0) },
            openPhoto: { model.openPhoto(id: row.id, index: $0) },
            imageLoader: model.loadImage,
            openMenu: canModerate ? { model.profile?.blockOrUnblock() } : nil,
            report: canModerate ? { model.profile?.reportCheckIn(id: row.id) } : nil,
            ratingDescription: ratingDescription(row),
            onLike: { model.profile?.toggleCheckInLike(id: row.id) },
            likeEnabled: !model.state.checkIns.pendingLikeIds.contains(row.id)).padding(.horizontal, -16)
    }

    private func ratingDescription(_ row: IosOwnProfileCheckIn) -> String? {
        guard let rating = model.state.checkIns.ratings[row.id], let score = row.score else { return nil }
        return String(format: model.strings["profile_check_in_rating"], rating.coffee, rating.service, rating.place, score)
    }
}

struct PublicProfileWeek: View {
    @ObservedObject var model: PublicProfileModel
    @State private var width: CGFloat = 320

    var body: some View {
        CoffeePeekProfileActivityCard(state: activity, strings: model.strings, loadImage: model.loadImage,
            availableWidth: width, toggle: {}, changeMonth: { _ in },
            openDay: { _ in model.profile?.openCheckIns() }, retry: { model.profile?.retryActivity() }, canExpand: false,
            dayHint: model.strings["profile_week_open"])
            .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { width = $0 }
    }

    private var activity: IosOwnProfileActivity {
        let state = model.state.activity
        let date = state.days.first(where: \.today)?.date ?? state.days.last?.date ?? ""
        let parts = date.split(separator: "-").compactMap { Int32($0) }
        let calendar = Calendar(identifier: .gregorian)
        return IosOwnProfileActivity(expanded: false,
            year: parts.count == 3 ? parts[0] : Int32(calendar.component(.year, from: Date())),
            month: parts.count == 3 ? parts[1] : Int32(calendar.component(.month, from: Date())),
            loading: state.loading, failed: state.failed, selected: nil, canGoNext: false, canGoPrevious: false,
            days: state.days.map { IosOwnProfileDay(date: $0.date, photo: $0.photo, shopCount: Int64($0.count),
                homeCount: 0, selected: false, today: $0.today, enabled: true) })
    }
}

struct PublicProfileConnectionsSheet: View {
    @ObservedObject var model: PublicProfileModel

    var body: some View {
        CoffeePeekProfileConnectionsSheet(state: model.state.connections, strings: model.strings, loadImage: model.loadImage,
            close: { model.profile?.closeConnections() }, select: { model.profile?.openConnections(followers: $0) },
            retry: { model.profile?.retryConnections() }, more: { model.profile?.moreConnections() },
            openUser: { model.profile?.openUser(slug: $0) })
    }
}

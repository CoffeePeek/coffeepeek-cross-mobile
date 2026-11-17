import ComposeApp
import SwiftUI

struct OwnProfileActivityView: View {
    let state: IosOwnProfileActivity
    let model: OwnProfileModel
    let strings: OwnProfileStrings

    var body: some View {
        OwnProfileActivityCard(
            state: state, strings: strings, profile: model.profile, toggle: model.toggleActivity,
            changeMonth: { model.profile.changeMonth(offset: Int32($0)) },
            openDay: { date in
                if state.expanded { model.profile.selectDay(date: date) }
                else { model.profile.openWeek() }
            },
            openRow: { model.profile.openHistoryRow(key: $0) },
            retry: { model.profile.retryActivity(expanded: state.expanded) }
        )
    }
}

private struct OwnProfileActivityCard: View {
    let state: IosOwnProfileActivity
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
    let toggle: () -> Void
    let changeMonth: (Int) -> Void
    let openDay: (String) -> Void
    let openRow: (String) -> Void
    let retry: () -> Void
    private let columns = Array(repeating: GridItem(.flexible(minimum: 1), spacing: 2), count: 7)

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            header
            weekdays
            Divider()
            LazyVGrid(columns: columns, spacing: 6) {
                ForEach(state.days.indices, id: \.self) { index in
                    let day = state.days[index]
                    if let date = day.date {
                        OwnProfileActivityCell(day: day, date: date, strings: strings, profile: profile,
                                               isExpanded: state.expanded) { openDay(date) }
                    } else {
                        Color.clear.frame(minHeight: 44).accessibilityHidden(true)
                    }
                }
            }
            if state.loading {
                ProgressView().accessibilityLabel(strings["history_activity_loading"])
            } else if state.failed {
                OwnProfileRetry(message: strings["history_activity_failed"], strings: strings, retry: retry)
            } else if state.expanded, let selected = state.selected {
                selectedRows(date: selected)
            }
        }
        .padding(10)
        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 20))
        .overlay { RoundedRectangle(cornerRadius: 20).strokeBorder(CoffeePeekNativeStyle.border).allowsHitTesting(false) }
    }

    private var header: some View {
        HStack(spacing: 3) {
            if state.expanded {
                arrow(-1, enabled: state.canGoPrevious, key: "history_previous_month", symbol: "chevron.left")
            }
            Text(monthLabel)
                .font(CoffeePeekNativeStyle.font(16, weight: "Semibold", relativeTo: .headline))
                .accessibilityAddTraits(.isHeader)
            if state.expanded {
                arrow(1, enabled: state.canGoNext, key: "history_next_month", symbol: "chevron.right")
            }
            Spacer(minLength: 0)
            Button(action: toggle) {
                Image(systemName: state.expanded ? "chevron.up" : "chevron.down")
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(strings[state.expanded ? "history_activity_collapse" : "history_activity_expand"])
        }
    }

    private var weekdays: some View {
        LazyVGrid(columns: columns) {
            ForEach(0..<7) { index in
                Text(strings["history_weekday_\(index)"])
                    .font(CoffeePeekNativeStyle.font(11, relativeTo: .caption))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
        }
        .accessibilityHidden(true)
    }

    private var monthLabel: String {
        let month = calendar.date(from: DateComponents(year: Int(state.year), month: Int(state.month), day: 1))
        return month?.formatted(.dateTime.month(.wide).year().locale(Locale(identifier: strings.languageTag))) ?? ""
    }

    private var calendar: Calendar { Calendar(identifier: .gregorian) }

    private func arrow(_ offset: Int, enabled: Bool, key: String, symbol: String) -> some View {
        Button { changeMonth(offset) } label: { Image(systemName: symbol).frame(width: 44, height: 44) }
            .buttonStyle(.plain)
            .disabled(!enabled)
            .accessibilityLabel(strings[key])
    }

    private func selectedRows(date: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(ownProfileDayLabel(date, language: strings.languageTag))
                .font(CoffeePeekNativeStyle.font(17, weight: "Bold", relativeTo: .headline))
                .accessibilityAddTraits(.isHeader)
            if state.rows.isEmpty {
                Text(strings["history_activity_empty_day"]).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
            ForEach(state.rows, id: \.key) { row in
                Button { openRow(row.key) } label: {
                    HStack(spacing: 8) {
                        OwnProfileImage(url: row.photo, profile: profile, symbol: row.home ? "house" : "cup.and.saucer")
                            .frame(width: 44, height: 44).clipShape(RoundedRectangle(cornerRadius: 8))
                        VStack(alignment: .leading, spacing: 3) {
                            Text(row.home ? strings["history_activity_at_home"] : row.title ?? strings["history_shop"])
                            if let drink = row.drink { Text(drink).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                            if !row.time.isEmpty { Text(row.time).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                        }
                        Spacer()
                        Image(systemName: "chevron.right").accessibilityHidden(true)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityElement(children: .combine)
            }
        }
    }
}

private struct OwnProfileActivityCell: View {
    let day: IosOwnProfileDay
    let date: String
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
    let isExpanded: Bool
    let open: () -> Void

    var body: some View {
        Button(action: open) {
            VStack(spacing: 2) {
                Text(String(Int(date.suffix(2)) ?? 0)).font(CoffeePeekNativeStyle.font(12, weight: "Semibold", relativeTo: .caption))
                if day.shopCount > 0 || day.homeCount > 0 {
                    HStack(spacing: 2) {
                        if day.shopCount > 0 { dot("location.fill") }
                        if day.homeCount > 0 { dot("house.fill") }
                    }
                }
            }
            .frame(maxWidth: .infinity, minHeight: 44)
            .background {
                if let photo = day.photo {
                    OwnProfileImage(url: photo, profile: profile, symbol: day.homeCount > 0 ? "house" : "cup.and.saucer")
                    CoffeePeekNativeStyle.surface.opacity(0.72)
                }
            }
            .background(day.selected ? CoffeePeekNativeStyle.accent.opacity(0.28) : .clear)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay {
                if day.today || day.selected {
                    RoundedRectangle(cornerRadius: 8).strokeBorder(CoffeePeekNativeStyle.accent, lineWidth: 2)
                }
            }
        }
        .buttonStyle(.plain)
        .disabled(!day.enabled)
        .opacity(day.enabled ? 1 : 0.45)
        .accessibilityLabel(ownProfileDayLabel(date, language: strings.languageTag))
        .accessibilityValue(strings.count("history_profile_day_count", Int(day.shopCount) + Int(day.homeCount)))
        .accessibilityHint(strings[isExpanded ? "history_select_day" : "history_activity_open"])
        .accessibilityAddTraits(day.selected ? .isSelected : [])
    }

    private func dot(_ symbol: String) -> some View {
        Image(systemName: symbol)
            .font(.system(size: 8))
            .foregroundStyle(CoffeePeekNativeStyle.foreground)
            .accessibilityHidden(true)
    }
}

private func ownProfileDayLabel(_ value: String, language: String) -> String {
    let parts = value.split(separator: "-").compactMap { Int($0) }
    guard parts.count == 3,
          let date = Calendar(identifier: .gregorian).date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    else { return value }
    return date.formatted(.dateTime.weekday(.wide).day().month(.wide).locale(Locale(identifier: language)))
}

private func ownProfileActivityPreview(expanded: Bool, language: String) -> some View {
    let days = (11...17).map { day in
        IosOwnProfileDay(date: "2026-11-\(day)", photo: nil, shopCount: day == 14 ? 1 : 0, homeCount: day == 17 ? 1 : 0,
                         selected: expanded && day == 17, today: day == 17, enabled: true)
    }
    let state = IosOwnProfileActivity(expanded: expanded, year: 2026, month: 11, loading: false, failed: false,
                                      selected: expanded ? "2026-11-17" : nil, canGoNext: false, canGoPrevious: true,
                                      days: days, rows: [])
    return OwnProfileActivityCard(state: state, strings: OwnProfileStrings(languageTag: language), profile: nil,
                                  toggle: {}, changeMonth: { _ in }, openDay: { _ in }, openRow: { _ in }, retry: {})
        .padding()
}

#Preview("Current week light") {
    ownProfileActivityPreview(expanded: false, language: "ru").preferredColorScheme(.light)
}

#Preview("Selected date dark") {
    ownProfileActivityPreview(expanded: true, language: "be").preferredColorScheme(.dark)
}

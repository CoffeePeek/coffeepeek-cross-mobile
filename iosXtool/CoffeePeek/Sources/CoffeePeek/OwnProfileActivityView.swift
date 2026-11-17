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
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    private let columns = Array(repeating: GridItem(.flexible(minimum: 1), spacing: 2), count: 7)

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if state.expanded { monthCalendar }
            else { weekCalendar }
            feedback
        }
    }

    private var weekCalendar: some View {
        VStack(spacing: 0) {
            weekHeader
            dates
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 2)
        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 18))
        .overlay {
            RoundedRectangle(cornerRadius: 18)
                .strokeBorder(CoffeePeekNativeStyle.border)
                .allowsHitTesting(false)
        }
    }

    private var weekHeader: some View {
        Button(action: toggle) {
            weekdays(highlightToday: true)
                .frame(minHeight: 44)
                .overlay(alignment: .topTrailing) {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                        .padding(.top, 4)
                }
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(strings["history_activity_expand"])
    }

    private var monthCalendar: some View {
        VStack(spacing: 8) {
            monthHeader
            weekdays(highlightToday: false)
                .padding(.vertical, 3)
            Rectangle()
                .fill(CoffeePeekNativeStyle.border.opacity(0.5))
                .frame(height: 1)
                .accessibilityHidden(true)
            dates
        }
        .padding(.horizontal, dynamicTypeSize.isAccessibilitySize ? 0 : 28)
    }

    private var monthHeader: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 8) {
                monthTitle.fixedSize(horizontal: true, vertical: false)
                Spacer(minLength: 0)
                monthControls
            }
            VStack(alignment: .leading, spacing: 6) {
                monthTitle
                monthControls.frame(maxWidth: .infinity, alignment: .trailing)
            }
        }
    }

    private var monthTitle: some View {
        Text(monthLabel)
            .font(CoffeePeekNativeStyle.font(17, weight: "Medium", relativeTo: .headline))
            .foregroundStyle(CoffeePeekNativeStyle.foreground)
            .accessibilityAddTraits(.isHeader)
    }

    private var monthControls: some View {
        HStack(spacing: 3) {
            calendarButton("chevron.left", key: "history_previous_month", enabled: state.canGoPrevious) {
                changeMonth(-1)
            }
            calendarButton("chevron.right", key: "history_next_month", enabled: state.canGoNext) {
                changeMonth(1)
            }
            calendarButton("chevron.up", key: "history_activity_collapse", action: toggle)
        }
    }

    private func calendarButton(
        _ symbol: String, key: String, enabled: Bool = true, action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .frame(width: 44, height: 44)
                .background(CoffeePeekNativeStyle.surface, in: Circle())
                .shadow(color: .black.opacity(0.06), radius: 2, x: 0, y: 1)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.4)
        .accessibilityLabel(strings[key])
    }

    private func weekdays(highlightToday: Bool) -> some View {
        LazyVGrid(columns: columns) {
            ForEach(0..<7) { index in
                let today = highlightToday && state.days.indices.contains(index) && state.days[index].today
                Text(strings["history_weekday_\(index)"])
                    .font(CoffeePeekNativeStyle.font(12, weight: today ? "Semibold" : "Regular", relativeTo: .caption))
                    .foregroundStyle(today ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.secondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
        }
        .accessibilityHidden(!highlightToday)
    }

    private var dates: some View {
        LazyVGrid(columns: columns, spacing: 0) {
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
    }

    private var monthLabel: String {
        let locale = Locale(identifier: strings.languageTag)
        let calendar = Calendar(identifier: .gregorian)
        guard let month = calendar.date(from: DateComponents(year: Int(state.year), month: Int(state.month), day: 1))
        else { return "" }
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = calendar
        formatter.dateFormat = "LLLL yyyy"
        return formatter.string(from: month).capitalized(with: locale)
    }

    @ViewBuilder
    private var feedback: some View {
        if state.loading {
            ProgressView().accessibilityLabel(strings["history_activity_loading"])
        } else if state.failed {
            OwnProfileRetry(message: strings["history_activity_failed"], strings: strings, retry: retry)
        } else if state.expanded, let selected = state.selected {
            selectedRows(date: selected)
        }
    }

    private func selectedRows(date: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(ownProfileDayLabel(date, language: strings.languageTag))
                .font(CoffeePeekNativeStyle.font(17, weight: "Bold", relativeTo: .headline))
                .accessibilityAddTraits(.isHeader)
            if state.rows.isEmpty {
                Text(strings["history_activity_empty_day"]).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
            ForEach(state.rows, id: \.key, content: activityRow)
        }
    }

    private func activityRow(_ row: IosOwnProfileActivityRow) -> some View {
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

private struct OwnProfileActivityCell: View {
    let day: IosOwnProfileDay
    let date: String
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
    let isExpanded: Bool
    let open: () -> Void
    @ScaledMetric(relativeTo: .body) private var dateHeight: CGFloat = 44

    var body: some View {
        Button(action: open) {
            Group {
                if isExpanded { monthDate }
                else { weekMarker }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!day.enabled)
        .opacity(day.enabled || !isExpanded ? 1 : 0.45)
        .accessibilityLabel(ownProfileDayLabel(date, language: strings.languageTag))
        .accessibilityValue(strings.count("history_profile_day_count", Int(day.shopCount) + Int(day.homeCount)))
        .accessibilityHint(strings[isExpanded ? "history_select_day" : "history_activity_open"])
        .accessibilityAddTraits(day.selected ? .isSelected : [])
    }

    private var hasActivity: Bool { day.shopCount > 0 || day.homeCount > 0 }

    private var weekMarker: some View {
        Group {
            if let photo = day.photo {
                thumbnail(photo)
            } else {
                Circle()
                    .fill(hasActivity ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.secondary.opacity(0.32))
                    .frame(width: 8, height: 8)
            }
        }
        .frame(maxWidth: .infinity, minHeight: 44)
    }

    private var monthDate: some View {
        VStack(spacing: 4) {
            monthMarker
            if hasActivity {
                Circle().fill(CoffeePeekNativeStyle.accent).frame(width: 4, height: 4)
            }
        }
        .frame(maxWidth: .infinity, minHeight: dateHeight)
        .background(day.selected ? CoffeePeekNativeStyle.accent.opacity(0.14) : .clear, in: RoundedRectangle(cornerRadius: 10))
        .overlay {
            if day.today || day.selected {
                RoundedRectangle(cornerRadius: 10).strokeBorder(CoffeePeekNativeStyle.accent, lineWidth: 1)
                    .allowsHitTesting(false)
            }
        }
    }

    @ViewBuilder
    private var monthMarker: some View {
        if let photo = day.photo {
            thumbnail(photo)
        } else {
            Text(date.suffix(2).drop(while: { $0 == "0" }))
                .font(CoffeePeekNativeStyle.font(14, weight: day.today ? "Semibold" : "Regular", relativeTo: .body))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
    }

    private func thumbnail(_ url: String) -> some View {
        OwnProfileImage(url: url, profile: profile, symbol: day.homeCount > 0 ? "house" : "cup.and.saucer")
            .frame(width: 22, height: 22)
            .clipShape(RoundedRectangle(cornerRadius: 5))
    }
}

private func ownProfileDayLabel(_ value: String, language: String) -> String {
    let parts = value.split(separator: "-").compactMap { Int($0) }
    guard parts.count == 3,
          let date = Calendar(identifier: .gregorian).date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    else { return value }
    return date.formatted(.dateTime.weekday(.wide).day().month(.wide).locale(Locale(identifier: language)))
}

private func ownProfileActivityPreviewDays(expanded: Bool) -> [IosOwnProfileDay] {
    let dates = (expanded ? 1...30 : 16...22).map { day in
        IosOwnProfileDay(date: String(format: "2026-11-%02d", day), photo: nil,
                         shopCount: day == 10 ? 1 : 0, homeCount: day == 16 ? 1 : 0,
                         selected: expanded && day == 16, today: day == 16, enabled: day <= 16)
    }
    guard expanded else { return dates }
    let blank = IosOwnProfileDay(date: nil, photo: nil, shopCount: 0, homeCount: 0,
                                 selected: false, today: false, enabled: false)
    return Array(repeating: blank, count: 6) + dates
}

private func ownProfileActivityPreview(expanded: Bool, language: String) -> some View {
    let state = IosOwnProfileActivity(expanded: expanded, year: 2026, month: 11, loading: false, failed: false,
                                      selected: nil, canGoNext: false, canGoPrevious: true,
                                      days: ownProfileActivityPreviewDays(expanded: expanded), rows: [])
    return OwnProfileActivityCard(state: state, strings: OwnProfileStrings(languageTag: language), profile: nil,
                                  toggle: {}, changeMonth: { _ in }, openDay: { _ in }, openRow: { _ in }, retry: {})
        .padding()
}

#Preview("Current week light") {
    ownProfileActivityPreview(expanded: false, language: "ru").preferredColorScheme(.light)
}

#Preview("Current week dark") {
    ownProfileActivityPreview(expanded: false, language: "be").preferredColorScheme(.dark)
}

#Preview("Month light") {
    ownProfileActivityPreview(expanded: true, language: "ru").preferredColorScheme(.light)
}

#Preview("Month dark") {
    ownProfileActivityPreview(expanded: true, language: "be").preferredColorScheme(.dark)
}

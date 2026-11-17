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
    private let columns = Array(repeating: GridItem(.flexible(minimum: 1), spacing: 0), count: 7)

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            calendar
            feedback
        }
    }

    private var calendar: some View {
        VStack(spacing: 0) {
            monthHeader.padding(.bottom, 8)
            weekdays
            Rectangle().fill(CoffeePeekNativeStyle.border.opacity(0.4))
                .frame(height: 1).padding(.top, 10).padding(.bottom, 6)
                .accessibilityHidden(true)
            dates
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 10)
        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 28))
        .overlay {
            RoundedRectangle(cornerRadius: 28).strokeBorder(CoffeePeekNativeStyle.border)
                .allowsHitTesting(false)
        }
    }

    private var monthHeader: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 0) {
                if state.expanded { previousMonth }
                monthTitle.fixedSize(horizontal: true, vertical: false)
                if state.expanded { nextMonth }
                Spacer(minLength: 0)
                collapseButton
            }
            VStack(alignment: .leading, spacing: 4) {
                monthTitle
                HStack {
                    if state.expanded {
                        previousMonth
                        nextMonth
                    }
                    Spacer()
                    collapseButton
                }
            }
        }
    }

    private var monthTitle: some View {
        Text(monthLabel)
            .font(CoffeePeekNativeStyle.font(17, weight: "Semibold", relativeTo: .headline))
            .foregroundStyle(CoffeePeekNativeStyle.foreground)
            .padding(.leading, state.expanded ? 0 : 8)
            .accessibilityAddTraits(.isHeader)
    }

    private var previousMonth: some View {
        calendarButton("chevron.left", key: "history_previous_month", enabled: state.canGoPrevious) { changeMonth(-1) }
    }

    private var nextMonth: some View {
        calendarButton("chevron.right", key: "history_next_month", enabled: state.canGoNext) { changeMonth(1) }
    }

    private var collapseButton: some View {
        calendarButton(state.expanded ? "chevron.up" : "chevron.down",
                       key: state.expanded ? "history_activity_collapse" : "history_activity_expand", action: toggle)
    }

    private func calendarButton(
        _ symbol: String, key: String, enabled: Bool = true, action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: symbol).font(.system(size: 20, weight: .regular))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .frame(width: 48, height: 48).contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.35)
        .accessibilityLabel(strings[key])
    }

    private var weekdays: some View {
        LazyVGrid(columns: columns, spacing: 0) {
            ForEach(0..<7) { index in
                Text(strings["history_weekday_\(index)"])
                    .font(CoffeePeekNativeStyle.font(12, weight: "Medium", relativeTo: .caption))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
                    .lineLimit(1).minimumScaleFactor(0.7)
            }
        }
        .accessibilityHidden(true)
    }

    private var dates: some View {
        LazyVGrid(columns: columns, spacing: 0) {
            ForEach(state.days.indices, id: \.self) { index in
                let day = state.days[index]
                if let date = day.date {
                    OwnProfileActivityCell(day: day, date: date, strings: strings, profile: profile,
                                           isExpanded: state.expanded) { openDay(date) }
                } else {
                    Color.clear.aspectRatio(1, contentMode: .fit).accessibilityHidden(true)
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

    var body: some View {
        Button(action: open) {
            monthDate
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!day.enabled)
        .opacity(day.enabled ? 1 : 0.35)
        .accessibilityLabel(ownProfileDayLabel(date, language: strings.languageTag))
        .accessibilityValue(strings.count("history_profile_day_count", Int(day.shopCount) + Int(day.homeCount)))
        .accessibilityHint(strings[isExpanded ? "history_select_day" : "history_activity_open"])
        .accessibilityAddTraits(day.selected ? .isSelected : [])
    }

    private var hasActivity: Bool { day.shopCount > 0 || day.homeCount > 0 }

    private var monthDate: some View {
        ZStack {
            monthMarker.padding(hasActivity ? 8 : 4)
            if day.shopCount > 0 {
                visitMarkers.frame(maxHeight: .infinity, alignment: .bottom).padding(.bottom, 4)
            }
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(1, contentMode: .fit)
        .background(day.selected && day.photo == nil ? CoffeePeekNativeStyle.accent : .clear,
                    in: RoundedRectangle(cornerRadius: 12))
        .overlay {
            if (day.today && !day.selected) || (day.selected && day.photo != nil) {
                RoundedRectangle(cornerRadius: 12).strokeBorder(CoffeePeekNativeStyle.accent, lineWidth: 1)
                    .allowsHitTesting(false)
            }
        }
        .padding(2)
    }

    @ViewBuilder
    private var monthMarker: some View {
        if let photo = day.photo {
            OwnProfileImage(url: photo, profile: profile, symbol: day.homeCount > 0 ? "house" : "cup.and.saucer")
                .aspectRatio(1, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .padding(.bottom, day.shopCount > 0 ? 6 : 0)
        } else {
            Text(date.suffix(2).drop(while: { $0 == "0" }))
                .font(CoffeePeekNativeStyle.font(15, weight: day.today || day.selected ? "Semibold" : "Regular"))
                .foregroundStyle(day.selected ? CoffeePeekNativeStyle.color(0x1A1412) : CoffeePeekNativeStyle.foreground)
                .padding(.bottom, day.shopCount > 0 ? 6 : 0)
                .lineLimit(1).minimumScaleFactor(0.7)
        }
    }

    private var visitMarkers: some View {
        HStack(spacing: 2) {
            ForEach(0..<min(Int(day.shopCount), 3), id: \.self) { _ in
                Circle().fill(day.selected && day.photo == nil ? CoffeePeekNativeStyle.color(0x1A1412) : CoffeePeekNativeStyle.accent)
                    .frame(width: 4, height: 4)
            }
        }
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
        .background(CoffeePeekNativeStyle.background)
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

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
            openDay: model.selectDay,
            retry: { model.profile.retryActivity(expanded: state.expanded) }
        )
    }
}

private struct OwnProfileActivityCard: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var pagingDirection = 1
    let state: IosOwnProfileActivity
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
    let toggle: () -> Void
    let changeMonth: (Int) -> Void
    let openDay: (String) -> Void
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
            ZStack(alignment: .top) {
                dates.id(monthKey).transition(pageTransition)
            }
            .clipped()
            .simultaneousGesture(monthSwipe)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 10)
        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 28))
        .overlay {
            RoundedRectangle(cornerRadius: 28).strokeBorder(CoffeePeekNativeStyle.border)
                .allowsHitTesting(false)
        }
        .overlay(alignment: .bottomTrailing) {
            if state.loading {
                ProgressView().padding(8)
                    .background(CoffeePeekNativeStyle.surface, in: Circle())
                    .padding(8).allowsHitTesting(false)
                    .accessibilityLabel(strings["history_activity_loading"])
            }
        }
        .animation(calendarAnimation, value: state.expanded)
        .animation(calendarAnimation, value: monthKey)
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
        calendarButton("chevron.left", key: "history_previous_month", enabled: state.canGoPrevious) { page(-1) }
    }

    private var nextMonth: some View {
        calendarButton("chevron.right", key: "history_next_month", enabled: state.canGoNext) { page(1) }
    }

    private var collapseButton: some View {
        calendarButton("chevron.down",
                       key: state.expanded ? "history_activity_collapse" : "history_activity_expand",
                       rotation: state.expanded ? -180 : 0, action: toggle)
    }

    private func calendarButton(
        _ symbol: String, key: String, enabled: Bool = true, rotation: Double = 0,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: symbol).font(.system(size: 20, weight: .regular))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .rotationEffect(.degrees(rotation))
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
            ForEach(0..<slotCount, id: \.self) { index in
                if index < state.days.count, let date = state.days[index].date {
                    OwnProfileActivityCell(day: state.days[index], date: date, strings: strings,
                                           profile: profile) { openDay(date) }
                } else {
                    OwnProfileDaySlot { Color.clear }.accessibilityHidden(true)
                }
            }
        }
    }

    private var slotCount: Int {
        let fullWeeks = ((state.days.count + 6) / 7) * 7
        return max(state.expanded ? 42 : 7, fullWeeks)
    }

    private var monthKey: String { "\(state.year)-\(state.month)" }

    private var calendarAnimation: Animation? {
        reduceMotion ? nil : .easeInOut(duration: 0.28)
    }

    private var pageTransition: AnyTransition {
        guard !reduceMotion && state.expanded else { return .identity }
        return .asymmetric(
            insertion: .move(edge: pagingDirection > 0 ? .trailing : .leading).combined(with: .opacity),
            removal: .move(edge: pagingDirection > 0 ? .leading : .trailing).combined(with: .opacity)
        )
    }

    private var monthSwipe: some Gesture {
        DragGesture(minimumDistance: 30).onEnded { value in
            let translation = value.translation
            guard abs(translation.width) > abs(translation.height) * 1.5 else { return }
            page(translation.width < 0 ? 1 : -1)
        }
    }

    private func page(_ offset: Int) {
        guard state.expanded, offset < 0 ? state.canGoPrevious : state.canGoNext else { return }
        pagingDirection = offset
        changeMonth(offset)
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
        if state.failed && !state.loading {
            OwnProfileRetry(message: strings["history_activity_failed"], strings: strings, retry: retry)
        }
    }
}

private struct OwnProfileDaySlot<Content: View>: View {
    @ScaledMetric(relativeTo: .body) private var minimumHeight: CGFloat = 44
    @ViewBuilder let content: () -> Content

    var body: some View {
        Color.clear.aspectRatio(1, contentMode: .fit)
            .frame(minHeight: minimumHeight)
            .overlay { content() }
    }
}

private struct OwnProfileActivityCell: View {
    let day: IosOwnProfileDay
    let date: String
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
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
        .accessibilityHint(strings[day.selected ? "profile_check_ins_clear_date" : "history_select_day"])
        .accessibilityAddTraits(day.selected ? .isSelected : [])
    }

    private var visitCount: Int { Int(day.shopCount) + Int(day.homeCount) }
    private var hasActivity: Bool { visitCount > 0 }

    private var monthDate: some View {
        OwnProfileDaySlot {
            ZStack {
                monthMarker.padding(hasActivity ? 8 : 4)
                if visitCount > 0 {
                    visitMarkers.frame(maxHeight: .infinity, alignment: .bottom).padding(.bottom, 4)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
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
    }

    @ViewBuilder
    private var monthMarker: some View {
        if let photo = day.photo {
            OwnProfileImage(url: photo, profile: profile, symbol: day.homeCount > 0 ? "house" : "cup.and.saucer")
                .aspectRatio(1, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .padding(.bottom, visitCount > 0 ? 6 : 0)
        } else {
            Text(date.suffix(2).drop(while: { $0 == "0" }))
                .font(CoffeePeekNativeStyle.font(15, weight: day.today || day.selected ? "Semibold" : "Regular"))
                .foregroundStyle(day.selected ? CoffeePeekNativeStyle.color(0x1A1412) : CoffeePeekNativeStyle.foreground)
                .padding(.bottom, visitCount > 0 ? 6 : 0)
                .lineLimit(1).minimumScaleFactor(0.7)
        }
    }

    private var visitMarkers: some View {
        HStack(spacing: 2) {
            ForEach(0..<min(visitCount, 3), id: \.self) { _ in
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

private func ownProfileActivityPreviewDays(expanded: Bool, month: Int, selected: String?) -> [IosOwnProfileDay] {
    let calendar = Calendar(identifier: .gregorian)
    let first = calendar.date(from: DateComponents(year: 2026, month: month, day: 1))!
    let dates = ownProfileActivityPreviewDateValues(expanded: expanded, month: month, selected: selected).map { value in
        let day = calendar.component(.day, from: value)
        let valueMonth = calendar.component(.month, from: value)
        let date = String(format: "%04d-%02d-%02d", calendar.component(.year, from: value), valueMonth, day)
        return IosOwnProfileDay(date: date, photo: day == 16 ? "preview://coffee" : nil,
                         shopCount: day == 10 || day == 16 ? 2 : 0, homeCount: day == 16 ? 1 : 0,
                         selected: selected == date, today: date == "2026-11-16", enabled: date <= "2026-11-16")
    }
    guard expanded else { return dates }
    let blank = IosOwnProfileDay(date: nil, photo: nil, shopCount: 0, homeCount: 0,
                                 selected: false, today: false, enabled: false)
    return Array(repeating: blank, count: (calendar.component(.weekday, from: first) + 5) % 7) + dates
}

private func ownProfileActivityPreviewDateValues(expanded: Bool, month: Int, selected: String?) -> [Date] {
    var calendar = Calendar(identifier: .gregorian)
    calendar.firstWeekday = 2
    let first = calendar.date(from: DateComponents(year: 2026, month: month, day: 1))!
    if expanded {
        return calendar.range(of: .day, in: .month, for: first)!.map {
            calendar.date(byAdding: .day, value: $0 - 1, to: first)!
        }
    }
    let anchorDay = selected.flatMap { Int($0.suffix(2)) } ?? (month == 11 ? 16 : 1)
    let anchor = calendar.date(byAdding: .day, value: anchorDay - 1, to: first)!
    let start = calendar.dateInterval(of: .weekOfYear, for: anchor)!.start
    return (0..<7).map { calendar.date(byAdding: .day, value: $0, to: start)! }
}

private struct OwnProfileActivityPreview: View {
    @State var expanded: Bool
    let language: String
    @State private var month = 11
    @State private var selected: String?

    private var state: IosOwnProfileActivity {
        IosOwnProfileActivity(expanded: expanded, year: 2026, month: Int32(month), loading: false, failed: false,
                              selected: selected, canGoNext: month < 11, canGoPrevious: month > 1,
                              days: ownProfileActivityPreviewDays(expanded: expanded, month: month,
                                                                  selected: selected))
    }

    var body: some View {
        ScrollView {
            OwnProfileActivityCard(state: state, strings: OwnProfileStrings(languageTag: language), profile: nil,
                                   toggle: toggle, changeMonth: changeMonth,
                                   openDay: { selected = selected == $0 ? nil : $0 }, retry: {})
                .padding()
        }
        .background(CoffeePeekNativeStyle.background)
    }

    private func toggle() {
        expanded.toggle()
    }

    private func changeMonth(_ offset: Int) {
        month += offset
        selected = nil
    }
}

#Preview("Current week light") {
    OwnProfileActivityPreview(expanded: false, language: "ru").preferredColorScheme(.light)
}

#Preview("Current week dark") {
    OwnProfileActivityPreview(expanded: false, language: "be").preferredColorScheme(.dark)
}

#Preview("Month light") {
    OwnProfileActivityPreview(expanded: true, language: "ru").preferredColorScheme(.light)
}

#Preview("Month dark") {
    OwnProfileActivityPreview(expanded: true, language: "be").preferredColorScheme(.dark)
}

#Preview("Month narrow, large text") {
    OwnProfileActivityPreview(expanded: true, language: "ru")
        .frame(width: 320).dynamicTypeSize(.accessibility2).preferredColorScheme(.light)
}

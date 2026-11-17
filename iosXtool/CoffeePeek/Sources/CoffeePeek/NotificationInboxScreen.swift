import ComposeApp
import SwiftUI

struct NotificationInboxScreen: View {
    let state: IosNotificationInboxState
    let strings: NotificationInboxStrings
    let close: () -> Void
    let refresh: () -> Void
    let loadMore: () -> Void
    let markAll: () -> Void
    let select: (String) -> Void
    let seen: (String) -> Void
    let signIn: () -> Void
    @Environment(\.scenePhase) private var scenePhase
    @State private var rowBounds: [String: CGRect] = [:]
    private let viewport = "notification-inbox"

    var body: some View {
        VStack(spacing: 0) {
            header
            GeometryReader { geometry in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 14) {
                        if state.signedIn {
                            history(height: geometry.size.height)
                            status
                            if state.hasMore && state.errorKey == nil { more }
                        } else {
                            empty(signedIn: false)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 24)
                    .padding(.top, 8)
                }
                .coordinateSpace(name: viewport)
                .onPreferenceChange(InboxRowBounds.self) { rowBounds = $0 }
            }
        }
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
    }

    private var header: some View {
        HStack {
            Text(strings.text("notifications_title"))
                .font(CoffeePeekNativeStyle.font(23, weight: "Bold", relativeTo: .title2))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .accessibilityAddTraits(.isHeader)
            Spacer()
            Button(action: close) {
                Image(systemName: "xmark")
                    .font(.system(size: 16, weight: .semibold))
                    .frame(width: 44, height: 44)
                    .background(CoffeePeekNativeStyle.surface, in: Circle())
            }
            .buttonStyle(.plain)
            .foregroundStyle(CoffeePeekNativeStyle.foreground)
            .accessibilityLabel(Text(strings.text("notifications_close")))
            .keyboardShortcut(.cancelAction)
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
    }

    private func history(height: CGFloat) -> some View {
        ForEach(groups) { group in
            HStack {
                Text(strings.text(group.today ? "notifications_today" : "notifications_earlier"))
                    .font(CoffeePeekNativeStyle.font(12, weight: "Semibold", relativeTo: .caption))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                if groups.first?.id == group.id {
                    action("notifications_read_all", run: markAll)
                        .disabled(!canInteract || state.unreadCount == 0 || state.markingRead)
                }
            }
            ForEach(group.rows, id: \.id) { row in
                rowView(row, height: height)
            }
        }
    }

    private func rowView(_ row: IosNotificationInboxRow, height: CGFloat) -> some View {
        let shouldMarkSeen = !row.read && scenePhase == .active && mostlyVisible(row.id, height: height)
        return NotificationInboxRow(row: row, strings: strings, select: { select(row.id) })
            .disabled(!canInteract || (row.read && !row.canOpen))
            .background {
                GeometryReader { geometry in
                    Color.clear.preference(key: InboxRowBounds.self,
                                           value: [row.id: geometry.frame(in: .named(viewport))])
                }
            }
            .task(id: shouldMarkSeen) {
                guard shouldMarkSeen else { return }
                do { try await Task.sleep(for: .seconds(2)) } catch { return }
                guard !Task.isCancelled else { return }
                seen(row.id)
            }
    }

    @ViewBuilder
    private var status: some View {
        if state.loading && state.items.isEmpty {
            ProgressView().frame(maxWidth: .infinity).accessibilityLabel(Text(strings.text("notifications_loading")))
        } else if let error = state.errorKey {
            VStack(alignment: .leading, spacing: 6) {
                Text(strings.text(error)).foregroundStyle(.red)
                action("notifications_retry", run: refresh)
            }
        } else if state.items.isEmpty {
            empty(signedIn: true)
        }
    }

    private var more: some View {
        action(state.loadingMore ? "notifications_loading" : "notifications_load_more", run: loadMore)
            .frame(maxWidth: .infinity)
            .disabled(state.loading || state.loadingMore || state.markingRead)
    }

    private func empty(signedIn: Bool) -> some View {
        VStack(spacing: 10) {
            Image(systemName: "bell").font(.title2).accessibilityHidden(true)
            Text(strings.text(signedIn ? "notifications_empty_title" : "notifications_sign_in_title"))
                .font(CoffeePeekNativeStyle.font(18, weight: "Bold", relativeTo: .headline))
            Text(strings.text(signedIn ? "notifications_empty_description" : "notifications_sign_in_description"))
                .font(CoffeePeekNativeStyle.font(14))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
            if !signedIn { action("notifications_sign_in", run: signIn) }
        }
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .multilineTextAlignment(.center)
        .padding(.vertical, 24)
        .frame(maxWidth: .infinity)
    }

    private func action(_ key: String, run: @escaping () -> Void) -> some View {
        Button(action: run) {
            Text(strings.text(key))
                .font(CoffeePeekNativeStyle.font(14, weight: "Semibold"))
                .frame(minHeight: 44)
        }
        .tint(CoffeePeekNativeStyle.foreground)
    }

    private var canInteract: Bool { state.openingId == nil && !state.loading && !state.loadingMore }

    private var groups: [InboxGroup] {
        var result: [InboxGroup] = []
        for row in state.items {
            if let index = result.firstIndex(where: { $0.today == row.today }) {
                result[index].rows.append(row)
            } else {
                result.append(InboxGroup(today: row.today, rows: [row]))
            }
        }
        return result
    }

    private func mostlyVisible(_ id: String, height: CGFloat) -> Bool {
        guard let bounds = rowBounds[id], height > 0 else { return false }
        let visible = min(bounds.maxY, height) - max(bounds.minY, 0)
        return visible > 0 && visible >= min(bounds.height, height) / 2
    }
}

private struct InboxGroup: Identifiable {
    var id: Bool { today }
    let today: Bool
    var rows: [IosNotificationInboxRow]
}

private struct InboxRowBounds: PreferenceKey {
    static let defaultValue: [String: CGRect] = [:]

    static func reduce(value: inout [String: CGRect], nextValue: () -> [String: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { _, newer in newer })
    }
}

private struct NotificationInboxRow: View {
    let row: IosNotificationInboxRow
    let strings: NotificationInboxStrings
    let select: () -> Void

    var body: some View {
        Button(action: select) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: icon).foregroundStyle(CoffeePeekNativeStyle.foreground)
                    .frame(width: 34, height: 34)
                    .background(CoffeePeekNativeStyle.surface, in: Circle())
                    .accessibilityHidden(true)
                text
                if !row.read {
                    Circle().fill(CoffeePeekNativeStyle.accent)
                        .frame(width: 7, height: 7).padding(.top, 7)
                        .accessibilityHidden(true)
                }
            }
            .padding(.vertical, 8)
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityValue(Text(strings.text(row.read ? "notifications_read" : "notifications_unread")))
        .accessibilityHint(Text(strings.text(row.canOpen ? "notifications_open" : "notifications_mark_read")))
    }

    private var text: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(row.title).font(CoffeePeekNativeStyle.font(14, weight: row.read ? "Medium" : "Semibold"))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
            Text(row.date).font(CoffeePeekNativeStyle.font(12, relativeTo: .caption))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
            if !row.body.isEmpty {
                Text(row.body).font(CoffeePeekNativeStyle.font(14))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .multilineTextAlignment(.leading)
    }

    private var icon: String {
        if row.rejected { return "xmark.circle" }
        switch row.kind {
        case "ShopRejected", "RoasterRejected": return "xmark.circle"
        case "ShopApproved", "RoasterApproved": return "checkmark.circle"
        case "NewShop": return "location"
        case "CheckInModeration": return "text.bubble"
        case "CorrectionModeration": return "pencil"
        case "Inactivity": return "cup.and.saucer"
        default: return "sparkles"
        }
    }
}

private func previewInbox(signedIn: Bool, languageTag: String) -> some View {
    let item = IosNotificationInboxRow(
        id: "preview", kind: "NewShop", title: "CoffeePeek", body: "",
        date: "17.11.2026 10:45", today: true, read: false, rejected: false, canOpen: true
    )
    let state = IosNotificationInboxState(
        sheetVersion: 1, signedIn: signedIn, unreadCount: signedIn ? 1 : 0,
        items: signedIn ? [item] : [], loading: false, loadingMore: false,
        markingRead: false, hasMore: signedIn, openingId: nil, errorKey: nil
    )
    return NotificationInboxScreen(
        state: state, strings: NotificationInboxStrings(languageTag: languageTag),
        close: {}, refresh: {}, loadMore: {}, markAll: {}, select: { _ in }, seen: { _ in }, signIn: {}
    )
}

#Preview("Inbox — light") { previewInbox(signedIn: true, languageTag: "ru").preferredColorScheme(.light) }
#Preview("Inbox — dark") { previewInbox(signedIn: false, languageTag: "be").preferredColorScheme(.dark) }

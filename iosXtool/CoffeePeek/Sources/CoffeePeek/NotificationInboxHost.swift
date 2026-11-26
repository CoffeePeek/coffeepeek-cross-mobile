import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeNotificationInboxController(inbox: IosNotificationInbox, languageTag: String) -> UIViewController {
    let controller = UIHostingController(rootView: NotificationInboxHost(
        inbox: inbox, strings: NotificationInboxStrings(languageTag: languageTag)
    ))
    controller.view.backgroundColor = .clear
    return controller
}

@MainActor
private final class NotificationInboxModel: ObservableObject {
    let inbox: IosNotificationInbox
    @Published private(set) var state: IosNotificationInboxState
    private var observation: IosNotificationInboxObservation?

    init(inbox: IosNotificationInbox) {
        self.inbox = inbox
        state = inbox.current()
    }

    func start() {
        guard observation == nil else { return }
        state = inbox.current()
        observation = inbox.observe { [weak self] newState in self?.state = newState }
    }

    func stop() {
        observation?.cancel()
        observation = nil
        inbox.dismiss(version: inbox.current().sheetVersion)
    }

    isolated deinit {
        observation?.cancel()
        inbox.close()
    }
}

private struct NotificationInboxHost: View {
    @StateObject private var model: NotificationInboxModel
    let strings: NotificationInboxStrings

    init(inbox: IosNotificationInbox, strings: NotificationInboxStrings) {
        _model = StateObject(wrappedValue: NotificationInboxModel(inbox: inbox))
        self.strings = strings
    }

    var body: some View {
        let version = model.state.sheetVersion
        NotificationInboxBell(unreadCount: model.state.unreadCount, strings: strings, open: model.inbox.show)
            .onAppear(perform: model.start)
            .onDisappear(perform: model.stop)
            .sheet(item: presentation(version)) { route in
                ActiveNotificationInbox(model: model, version: route.id, strings: strings)
                    .id(route.id)
                    .presentationDetents([.large])
                    .presentationDragIndicator(.visible)
            }
    }

    private func presentation(_ version: Int64) -> Binding<NotificationInboxRoute?> {
        Binding(
            get: {
                let active = model.state.sheetVersion
                return active == 0 ? nil : NotificationInboxRoute(id: active)
            },
            set: { if $0 == nil { model.inbox.dismiss(version: version) } }
        )
    }
}

private struct NotificationInboxRoute: Identifiable {
    let id: Int64
}

private struct ActiveNotificationInbox: View {
    @ObservedObject var model: NotificationInboxModel
    let version: Int64
    let strings: NotificationInboxStrings

    var body: some View {
        if model.state.sheetVersion == version {
            NotificationInboxScreen(
                state: model.state,
                strings: strings,
                close: { model.inbox.dismiss(version: version) },
                refresh: { model.inbox.refresh(version: version) },
                loadMore: { model.inbox.loadMore(version: version) },
                markAll: { model.inbox.markAllRead(version: version) },
                select: { model.inbox.select(version: version, id: $0) },
                seen: { model.inbox.seen(version: version, id: $0) },
                signIn: { model.inbox.signIn(version: version) }
            )
            .onAppear { model.inbox.appeared(version: version) }
        }
    }
}

private struct NotificationInboxBell: View {
    let unreadCount: Int32
    let strings: NotificationInboxStrings
    let open: () -> Void

    var body: some View {
        Button(action: open) {
            Image(systemName: "bell")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .frame(width: 44, height: 44)
                .background(CoffeePeekNativeStyle.surface, in: Circle())
                .overlay { Circle().strokeBorder(CoffeePeekNativeStyle.border, lineWidth: 1) }
                .frame(width: 48, height: 48)
                .contentShape(Rectangle())
                .overlay(alignment: .topTrailing) {
                    if unreadCount > 0 {
                        Text(unreadCount > 99 ? strings.text("notifications_badge_max") : String(unreadCount))
                            .font(.system(size: 10, weight: .semibold))
                            .lineLimit(1)
                            .foregroundStyle(CoffeePeekNativeStyle.color(0x1C1917))
                            .padding(.horizontal, 4)
                            .frame(minWidth: 16, minHeight: 16)
                            .background(CoffeePeekNativeStyle.accent, in: Capsule())
                            .accessibilityHidden(true)
                            .allowsHitTesting(false)
                    }
                }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(strings.bellLabel(unreadCount)))
    }
}

struct NotificationInboxStrings {
    private let bundle: Bundle

    init(languageTag: String) {
        let code = languageTag.split(separator: "-").first.map(String.init) ?? "ru"
        let path = Bundle.main.path(forResource: code, ofType: "lproj")
            ?? Bundle.main.path(forResource: "ru", ofType: "lproj")
        bundle = path.flatMap { Bundle(path: $0) } ?? .main
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "NativeNotificationInbox")
    }

    func bellLabel(_ count: Int32) -> String {
        count == 0 ? text("notifications_title") : String(format: text("notifications_button_unread"), count)
    }
}

#Preview("Bell — light") {
    NotificationInboxBell(unreadCount: 12, strings: NotificationInboxStrings(languageTag: "ru"), open: {})
        .preferredColorScheme(.light)
}

#Preview("Bell — dark") {
    NotificationInboxBell(unreadCount: 105, strings: NotificationInboxStrings(languageTag: "be"), open: {})
        .preferredColorScheme(.dark)
}

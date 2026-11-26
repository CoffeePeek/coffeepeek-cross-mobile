import ComposeApp
import SwiftUI
import UIKit
import UniformTypeIdentifiers

struct ShelfNativeGridScreen: View {
    @ObservedObject var model: ShelfNativeModel
    @State private var settingsShown = false
    @State private var archive: IosShelfArchive?
    @State private var archivedBag: String?
    var body: some View {
        ShelfNativeGridContent(state: model.state, strings: model.strings, own: model.screen.kind == "shelf",
            cover: { AnyView(ShelfNativeCover(screen: model.screen, item: $0, sitsOnShelf: true)) },
            action: model.action, move: { model.screen.move(id: $0, position: Int32($1)) },
            onSettings: openSettings, messageExpired: { model.screen.messageExpired(key: $0) })
        .sheet(isPresented: $settingsShown, onDismiss: finishSettings) {
            ShelfNativeSettingsHost(model: model, archive: archive, close: { settingsShown = false })
        }
        .onChange(of: model.state.active) { _, active in if !active { archivedBag = nil; settingsShown = false } }
        .sheet(isPresented: Binding(get: { model.state.finishingId != nil }, set: { if !$0 { model.action("cancelFinish") } })) {
            if let item = model.state.items.first(where: { $0.id == model.state.finishingId }) {
                ShelfNativeFinishSheet(cover: AnyView(ShelfNativeCover(screen: model.screen, item: item)), item: item, strings: model.strings,
                    rating: Int(model.state.finishRating), again: model.state.finishBuyAgain?.boolValue,
                    rate: { model.screen.rate(value: Int32($0)) }, chooseAgain: { model.screen.chooseAgain(value: $0) },
                    finish: { model.screen.finish(id: item.id, skip: $0) },
                    cancel: { model.action("cancelFinish") })
            }
        }
        .id(model.state.revision)
    }

    private func openSettings() {
        model.settingsVisibility(true)
        archive = model.screen.makeArchive { id in
            archivedBag = id
            settingsShown = false
        }
        settingsShown = true
    }
    private func finishSettings() {
        if let id = archivedBag { model.action("archivedBag", id) }
        archivedBag = nil
        model.screen.closeArchive()
        archive = nil
        model.settingsVisibility(false)
    }
}

private struct ShelfNativeSettingsHost: View {
    @ObservedObject var model: ShelfNativeModel
    let archive: IosShelfArchive?
    let close: () -> Void

    var body: some View {
        let controls = ShelfNativeVisibilitySettings(state: model.state, strings: model.strings,
            setVisible: { model.screen.setVisible(value: $0) }, retry: { model.action("retryVisibility") },
            close: close)
        return Group {
            if model.state.active {
                if let archive { ShelfArchiveHost(archive: archive, languageTag: model.screen.languageTag, settings: AnyView(controls)) }
                else { controls }
            } else { Color.clear }
        }
        .animation(nil, value: model.state.active)
        .padding(.top, 16).presentationDetents([.large]).presentationDragIndicator(.visible)
        .background(CoffeePeekNativeStyle.background)
    }
}

private struct ShelfNativeGlassRail: View {
    var body: some View {
        Color.clear.discoveryGlass(in: RoundedRectangle(cornerRadius: 9))
            .overlay { RoundedRectangle(cornerRadius: 9).stroke(.white.opacity(0.6), lineWidth: 0.75) }
            .overlay {
                HStack {
                    fastener
                    Spacer()
                    fastener
                }.padding(.horizontal, 12)
            }
            .shadow(color: .black.opacity(0.16), radius: 10, x: 0, y: 8)
            .accessibilityHidden(true)
    }

    private var fastener: some View {
        Circle().fill(LinearGradient(colors: [.white.opacity(0.9), .gray.opacity(0.6)],
                                    startPoint: .topLeading, endPoint: .bottomTrailing))
            .overlay { Capsule().fill(.black.opacity(0.5)).frame(width: 5, height: 1).rotationEffect(.degrees(-45)) }
            .overlay { Circle().stroke(.white.opacity(0.5), lineWidth: 0.5) }
            .frame(width: 8, height: 8).shadow(color: .black.opacity(0.25), radius: 1, y: 1)
    }
}

private struct ShelfNativeGridContent: View {
    let state: IosShelfState
    let strings: ShelfNativeStrings
    let own: Bool
    let cover: (IosShelfItem) -> AnyView
    let action: (String, String?) -> Void
    let move: (String, Int) -> Void
    let onSettings: () -> Void
    let messageExpired: (String) -> Void
    @State private var dragged: String?
    @ScaledMetric(relativeTo: .body) private var bagHeight: CGFloat = 160
    @State private var dropRequest: ShelfNativeDrop?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if own { header }
                if state.loading { CoffeePeekNativeLoader(label: strings["shelf_loading"], size: 32, showsLabel: true).frame(maxWidth: .infinity) }
                else if state.failed { ShelfNativeStatus(message: strings["shelf_load_failed"], retry: strings["shelf_retry"]) { action("refresh", nil) } }
                else if state.visible { grid }
            }.padding(16).padding(.bottom, own ? 88 : 16)
                .coffeePeekRefreshable(isRefreshing: state.loading, label: strings["shelf_loading"]) { action("refresh", nil) }
        }
        .safeAreaInset(edge: .bottom) { if own && (state.undo || state.failure != nil) { message } }
        .onDisappear { dragged = nil; dropRequest = nil }
        .task(id: dropRequest?.id) {
            guard let request = dropRequest else { return }
            await request.load()
            if dropRequest?.id == request.id { dropRequest = nil }
        }
        .task(id: state.messageKey) {
            guard own, let key = state.messageKey else { return }
            let duration: UInt64 = UIAccessibility.isVoiceOverRunning ? 20 : (state.undo ? 5 : 4)
            try? await Task.sleep(nanoseconds: duration * 1_000_000_000)
            if !Task.isCancelled { messageExpired(key) }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(strings["shelf_profile_title"]).font(.title3.weight(.semibold))
                Spacer()
                Button(action: onSettings) {
                    Label(strings["shelf_settings"], systemImage: "gearshape").labelStyle(.iconOnly).frame(width: 44, height: 44)
                }.buttonStyle(.plain).shopDetailGlassCircle()
            }
            Text([strings.count("bags", state.items.count),
                  state.roasters > 0 ? strings.count("roasters", Int(state.roasters)) : nil].compactMap { $0 }.joined(separator: " · "))
                .font(.footnote).foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }

    private var grid: some View {
        VStack(alignment: .leading, spacing: 20) {
            if !own { Text(strings["shelf_profile_title"]).font(.title3.weight(.semibold)) }
            if state.items.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text(strings[own ? "shelf_empty_title" : "shelf_profile_empty"]).font(.headline)
                    if own { Text(strings["shelf_empty_body"]).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                }.padding(.vertical, 24)
            }
            LazyVStack(spacing: 40) {
                ForEach(Array(stride(from: 0, to: state.items.count, by: 3)), id: \.self) { start in shelfRow(start) }
            }
            if own && dragged != nil { finishZone }
        }
    }

    private func shelfRow(_ start: Int) -> some View {
        HStack(alignment: .top, spacing: 16) {
            ForEach(start..<min(start + 3, state.items.count), id: \.self) { index in
                tile(state.items[index], index: index).frame(maxWidth: .infinity).id(state.items[index].id)
            }
            ForEach(0..<max(0, start + 3 - state.items.count), id: \.self) { _ in
                Color.clear.frame(maxWidth: .infinity).frame(height: bagHeight).accessibilityHidden(true)
            }
        }
        .overlay(alignment: .top) {
            ShelfNativeGlassRail().frame(height: 28).offset(y: bagHeight - 12).allowsHitTesting(false)
        }
    }

    @ViewBuilder private func tile(_ item: IosShelfItem, index: Int) -> some View {
        if own {
            bagTile(item)
                .onDrag { dragged = item.id; return NSItemProvider(object: ("coffeepeek-shelf:" + item.id) as NSString) }
                .onDrop(of: [UTType.text], isTargeted: nil) { providers in
                    acceptDrop(providers) { move($0, index) }
                }
                .accessibilityActions {
                    Button(strings["shelf_move_up"]) { move(item.id, max(0, index - 1)) }
                    Button(strings["shelf_move_down"]) { move(item.id, min(state.items.count - 1, index + 1)) }
                    Button(strings["shelf_finish_action"]) { action("finish", item.id) }
                }
                .contextMenu { Button(strings["shelf_finish_action"]) { action("finish", item.id) } }
        } else { bagTile(item).disabled(!item.hasCoffee) }
    }

    private func bagTile(_ item: IosShelfItem) -> some View {
        Button { action("open", item.id) } label: {
            VStack(spacing: 28) {
                cover(item).frame(height: bagHeight)
                    .shadow(color: .black.opacity(0.16), radius: 4, x: 0, y: 4)
                Text(item.title ?? strings["shelf_bag_untitled"]).font(.caption).lineLimit(2)
            }
            .padding(.horizontal, 4)
            .background(item.id == state.arrivingId ? CoffeePeekNativeStyle.accent.opacity(0.15) : .clear, in: RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
        .onAppear { if own && item.id == state.arrivingId { action("arrived", item.id) } }
    }

    private var finishZone: some View {
        Label(strings["shelf_finish_zone"], systemImage: "archivebox")
            .frame(maxWidth: .infinity, minHeight: 64)
            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
            .onDrop(of: [UTType.text], isTargeted: nil) { providers in
                acceptDrop(providers) { action("finish", $0) }
            }
    }

    private func acceptDrop(_ providers: [NSItemProvider], perform: @escaping (String) -> Void) -> Bool {
        guard let expected = dragged, let provider = providers.first(where: { $0.canLoadObject(ofClass: NSString.self) }) else { return false }
        dragged = nil
        dropRequest = ShelfNativeDrop(provider: provider, expected: expected, perform: perform)
        return true
    }

    private var message: some View {
        HStack {
            Text(state.undo ? strings["shelf_finished_message"] : strings.issue(state.failure))
            Spacer()
            if state.undo { Button(strings["shelf_undo"]) { action("undo", nil) } }
            Button { action("dismiss", nil) } label: { Label(strings["shelf_sheet_close"], systemImage: "xmark").labelStyle(.iconOnly) }
        }.padding().background(CoffeePeekNativeStyle.surface)
    }
}

@MainActor
private struct ShelfNativeDrop: Identifiable {
    let id = UUID()
    let provider: NSItemProvider
    let expected: String
    let perform: (String) -> Void

    func load() async {
        let expected = self.expected
        let response = AsyncStream<Bool>.makeStream(bufferingPolicy: .bufferingNewest(1))
        let progress = provider.loadObject(ofClass: NSString.self) { object, _ in
            response.continuation.yield(object as? String == "coffeepeek-shelf:" + expected)
            response.continuation.finish()
        }
        defer { progress.cancel(); response.continuation.finish() }
        for await accepted in response.stream {
            if accepted && !Task.isCancelled { perform(expected) }
            return
        }
    }
}

private struct ShelfNativeVisibilitySettings: View {
    let state: IosShelfState
    let strings: ShelfNativeStrings
    let setVisible: (Bool) -> Void
    let retry: () -> Void
    let close: () -> Void
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(strings["shelf_settings"]).font(.headline)
                Spacer()
                Button(action: close) {
                    Label(strings["shelf_sheet_close"], systemImage: "xmark").labelStyle(.iconOnly).frame(width: 44, height: 44)
                }.buttonStyle(.plain).shopDetailGlassCircle()
            }
            Toggle(strings["shelf_profile_show"], isOn: Binding(get: { state.visibility }, set: { setVisible($0) }))
                .disabled(!state.visibilityEnabled)
            Text(strings["shelf_profile_show_hint"]).font(.footnote).foregroundStyle(CoffeePeekNativeStyle.secondary)
            if state.visibilityLoading { CoffeePeekNativeLoader(label: strings["shelf_profile_show"], size: 24) }
            if let error = state.visibilityError {
                Text(strings[error == "Load" ? "shelf_profile_visibility_load_failed" : "shelf_profile_visibility_save_failed"])
                    .font(.footnote).foregroundStyle(.red)
                if error == "Load" { Button(strings["shelf_retry"], action: retry) }
            }
        }.padding(.vertical, 8)
            .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
    }

}

@MainActor
private func shelfGridPreview() -> some View {
    let state = shelfNativePreviewState(items: (0..<7).map { shelfNativePreviewItem(id: "bag-\($0)") })
    return ShelfNativeGridContent(state: state, strings: ShelfNativeStrings("ru"), own: true,
        cover: { _ in AnyView(CoffeePeekPhotoPlaceholder()) }, action: { _, _ in }, move: { _, _ in }, onSettings: {}, messageExpired: { _ in })
        .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
}

#Preview("Shelf light") { shelfGridPreview().preferredColorScheme(.light) }
#Preview("Shelf dark") { shelfGridPreview().preferredColorScheme(.dark) }

#Preview("Glass rail light") {
    ShelfNativeGlassRail().frame(height: 28).padding(24)
        .background(CoffeePeekNativeStyle.background).preferredColorScheme(.light)
}
#Preview("Glass rail dark") {
    ShelfNativeGlassRail().frame(height: 28).padding(24)
        .background(CoffeePeekNativeStyle.background).preferredColorScheme(.dark)
}

#Preview("Public shelf light") {
    ShelfNativeGridContent(state: shelfNativePreviewState(), strings: ShelfNativeStrings("ru"), own: false,
        cover: { _ in AnyView(CoffeePeekPhotoPlaceholder()) }, action: { _, _ in }, move: { _, _ in }, onSettings: {}, messageExpired: { _ in })
        .preferredColorScheme(.light)
}
#Preview("Public shelf dark") {
    ShelfNativeGridContent(state: shelfNativePreviewState(), strings: ShelfNativeStrings("ru"), own: false,
        cover: { _ in AnyView(CoffeePeekPhotoPlaceholder()) }, action: { _, _ in }, move: { _, _ in }, onSettings: {}, messageExpired: { _ in })
        .preferredColorScheme(.dark)
}

#Preview("Shelf visibility light") {
    ShelfNativeVisibilitySettings(state: shelfNativePreviewState(), strings: ShelfNativeStrings("ru"),
        setVisible: { _ in }, retry: {}, close: {}).preferredColorScheme(.light)
}
#Preview("Shelf visibility dark") {
    ShelfNativeVisibilitySettings(state: shelfNativePreviewState(), strings: ShelfNativeStrings("ru"),
        setVisible: { _ in }, retry: {}, close: {}).preferredColorScheme(.dark)
}

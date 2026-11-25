import ComposeApp
import SwiftUI
import UIKit
import UniformTypeIdentifiers

struct ShelfNativeGridScreen: View {
    @ObservedObject var model: ShelfNativeModel
    var body: some View {
        ShelfNativeGridContent(state: model.state, strings: model.strings, own: model.screen.kind == "shelf",
            cover: { AnyView(ShelfNativeCover(screen: model.screen, item: $0)) },
            action: model.action, move: { model.screen.move(id: $0, position: Int32($1)) },
            setVisible: { model.screen.setVisible(value: $0) }, messageExpired: { model.screen.messageExpired(key: $0) })
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
}

private struct ShelfNativeGridContent: View {
    let state: IosShelfState
    let strings: ShelfNativeStrings
    let own: Bool
    let cover: (IosShelfItem) -> AnyView
    let action: (String, String?) -> Void
    let move: (String, Int) -> Void
    let setVisible: (Bool) -> Void
    let messageExpired: (String) -> Void
    @State private var dragged: String?
    @State private var settingsShown = false
    @State private var dropRequest: ShelfNativeDrop?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if own { header }
                if state.loading { CoffeePeekNativeLoader(label: strings["shelf_loading"], size: 32, showsLabel: true).frame(maxWidth: .infinity) }
                else if state.failed { ShelfNativeStatus(message: strings["shelf_load_failed"], retry: strings["shelf_retry"]) { action("refresh", nil) } }
                else if state.visible { grid }
            }.padding(16).padding(.bottom, own ? 88 : 16)
        }
        .refreshable { action("refresh", nil) }
        .safeAreaInset(edge: .bottom) { if own && (state.undo || state.failure != nil) { message } }
        .onDisappear { dragged = nil; dropRequest = nil }
        .task(id: dropRequest?.id) {
            guard let request = dropRequest else { return }
            await request.load()
            if dropRequest?.id == request.id { dropRequest = nil }
        }
         .sheet(isPresented: $settingsShown) {
            ShelfNativeVisibilitySettings(state: state, strings: strings, setVisible: setVisible,
                retry: { action("retryVisibility", nil) }, close: { settingsShown = false })
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
                Button { action("archive", nil) } label: {
                    Label(strings["shelf_archive"], systemImage: "archivebox").labelStyle(.iconOnly).frame(width: 44, height: 44)
                }
                Button { settingsShown = true } label: {
                    Label(strings["shelf_settings"], systemImage: "gearshape").labelStyle(.iconOnly).frame(width: 44, height: 44)
                }
            }
            Text([strings.count("bags", state.items.count), state.roasters > 0 ? strings.count("roasters", Int(state.roasters)) : nil,
                  state.grams.map { strings.weight(Int($0.intValue)) }].compactMap { $0 }.joined(separator: " · "))
                .font(.footnote).foregroundStyle(CoffeePeekNativeStyle.secondary)
            Button { action("add", nil) } label: { Label(strings["shelf_scan_bag"], systemImage: "plus") }
                .buttonStyle(.borderedProminent).foregroundStyle(.black)
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
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 3), alignment: .leading, spacing: 24) {
                ForEach(Array(state.items.enumerated()), id: \.element.id) { index, item in
                    tile(item, index: index)
                }
            }
            if own && dragged != nil { finishZone }
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
            VStack(spacing: 8) {
                cover(item).frame(height: 136)
                    .shadow(color: .black.opacity(0.16), radius: 4, x: 0, y: 4)
                RoundedRectangle(cornerRadius: 2).fill(CoffeePeekNativeStyle.secondary.opacity(0.18)).frame(height: 5)
                Text(item.title ?? strings["shelf_bag_untitled"]).font(.caption).lineLimit(2)
            }
            .padding(4)
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
                Button(strings["shelf_sheet_close"]) { close() }
            }
            Toggle(strings["shelf_profile_show"], isOn: Binding(get: { state.visibility }, set: { setVisible($0) }))
                .disabled(!state.visibilityEnabled)
            Text(strings["shelf_profile_show_hint"]).font(.footnote).foregroundStyle(CoffeePeekNativeStyle.secondary)
            if state.visibilityLoading { CoffeePeekNativeLoader(size: 24) }
            if let error = state.visibilityError {
                Text(strings[error == "Load" ? "shelf_profile_visibility_load_failed" : "shelf_profile_visibility_save_failed"])
                    .font(.footnote).foregroundStyle(.red)
                if error == "Load" { Button(strings["shelf_retry"], action: retry) }
            }
        }.padding(20).presentationDetents([.medium])
            .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
    }

}

@MainActor
private func shelfGridPreview() -> some View {
    let state = shelfNativePreviewState()
    return ShelfNativeGridContent(state: state, strings: ShelfNativeStrings("ru"), own: true,
        cover: { _ in AnyView(CoffeePeekPhotoPlaceholder()) }, action: { _, _ in }, move: { _, _ in }, setVisible: { _ in }, messageExpired: { _ in })
        .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
}

#Preview("Shelf light") { shelfGridPreview().preferredColorScheme(.light) }
#Preview("Shelf dark") { shelfGridPreview().preferredColorScheme(.dark) }

#Preview("Public shelf light") {
    ShelfNativeGridContent(state: shelfNativePreviewState(), strings: ShelfNativeStrings("ru"), own: false,
        cover: { _ in AnyView(CoffeePeekPhotoPlaceholder()) }, action: { _, _ in }, move: { _, _ in }, setVisible: { _ in }, messageExpired: { _ in })
        .preferredColorScheme(.light)
}
#Preview("Public shelf dark") {
    ShelfNativeGridContent(state: shelfNativePreviewState(), strings: ShelfNativeStrings("ru"), own: false,
        cover: { _ in AnyView(CoffeePeekPhotoPlaceholder()) }, action: { _, _ in }, move: { _, _ in }, setVisible: { _ in }, messageExpired: { _ in })
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

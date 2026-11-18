import ComposeApp
import SwiftUI
import UniformTypeIdentifiers

struct ShelfNativeGridScreen: View {
    @ObservedObject var model: ShelfNativeModel
    @State private var dragged: String?
    private var own: Bool { model.screen.kind == "shelf" }
    private var strings: ShelfNativeStrings { model.strings }
    private var state: IosShelfState { model.state }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if own { header }
                if state.loading { ProgressView(strings["shelf_loading"]).frame(maxWidth: .infinity) }
                else if state.failed { ShelfNativeStatus(message: strings["shelf_load_failed"], retry: strings["shelf_retry"]) { model.action("refresh") } }
                else if state.visible { grid }
                if own { visibility }
            }.padding(16).padding(.bottom, own ? 88 : 16)
        }
        .refreshable { model.action("refresh") }
        .sheet(isPresented: Binding(get: { state.finishingId != nil }, set: { if !$0 { model.action("cancelFinish") } })) {
            if let item = state.items.first(where: { $0.id == state.finishingId }) {
                ShelfNativeFinishSheet(screen: model.screen, item: item, strings: strings,
                    finish: { rating, again in model.screen.finish(id: item.id, rating: rating, buyAgain: again) },
                    cancel: { model.action("cancelFinish") })
            }
        }
        .safeAreaInset(edge: .bottom) { if own && (state.undo || state.failure != nil) { message } }
        .id(state.revision)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(strings["shelf_profile_title"]).font(.title3.weight(.semibold))
                Spacer()
                Button(strings["shelf_archive"]) { model.action("archive") }
            }
            Text([strings.count("bags", state.items.count), strings.count("roasters", Int(state.roasters)),
                  state.grams.map { String(format: strings["shelf_weight_g"], $0.intValue) }].compactMap { $0 }.joined(separator: " Â· "))
                .font(.footnote).foregroundStyle(CoffeePeekNativeStyle.secondary)
            Button { model.action("add") } label: { Label(strings["shelf_scan_bag"], systemImage: "plus") }
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

    private func tile(_ item: IosShelfItem, index: Int) -> some View {
        Button { model.action("open", item.id) } label: {
            VStack(spacing: 8) {
                ShelfNativeCover(screen: model.screen, item: item).frame(height: 136)
                    .shadow(color: .black.opacity(0.16), radius: 4, x: 0, y: 4)
                RoundedRectangle(cornerRadius: 2).fill(CoffeePeekNativeStyle.secondary.opacity(0.18)).frame(height: 5)
                Text(item.title ?? strings["shelf_bag_untitled"]).font(.caption).lineLimit(2)
            }
            .padding(4)
            .background(item.id == state.arrivingId ? CoffeePeekNativeStyle.accent.opacity(0.15) : .clear, in: RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
        .disabled(!own && !item.hasCoffee)
        .onAppear { if own && item.id == state.arrivingId { model.action("arrived", item.id) } }
        .onDrag { dragged = own ? item.id : nil; return NSItemProvider(object: item.id as NSString) }
        .onDrop(of: [UTType.text], isTargeted: nil) { providers in
            guard own, !providers.isEmpty, let id = dragged else { return false }
            model.screen.move(id: id, position: Int32(index)); dragged = nil; return true
        }
        .accessibilityAction(named: strings["shelf_move_up"]) { if own { model.screen.move(id: item.id, position: Int32(max(0, index - 1))) } }
        .accessibilityAction(named: strings["shelf_move_down"]) { if own { model.screen.move(id: item.id, position: Int32(min(state.items.count - 1, index + 1))) } }
        .accessibilityAction(named: strings["shelf_finish_action"]) { if own { model.action("finish", item.id) } }
        .contextMenu { if own { Button(strings["shelf_finish_action"]) { model.action("finish", item.id) } } }
    }

    private var finishZone: some View {
        Label(strings["shelf_finish_zone"], systemImage: "archivebox")
            .frame(maxWidth: .infinity, minHeight: 64)
            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
            .onDrop(of: [UTType.text], isTargeted: nil) { providers in
                guard !providers.isEmpty, let id = dragged else { return false }
                model.action("finish", id); dragged = nil; return true
            }
    }

    private var visibility: some View {
        VStack(alignment: .leading, spacing: 8) {
            Toggle(strings["shelf_profile_show"], isOn: Binding(get: { state.visibility }, set: { model.screen.setVisible(value: $0) }))
                .disabled(!state.visibilityEnabled)
            Text(strings["shelf_profile_show_hint"]).font(.footnote).foregroundStyle(CoffeePeekNativeStyle.secondary)
            if state.visibilityLoading { ProgressView() }
            if state.visibilityFailed {
                ShelfNativeStatus(message: strings["shelf_profile_visibility_load_failed"], retry: strings["shelf_retry"]) { model.action("retryVisibility") }
            }
        }
    }

    private var message: some View {
        HStack {
            Text(strings[state.undo ? "shelf_finished_message" : "shelf_error_generic"])
            Spacer()
            if state.undo { Button(strings["shelf_undo"]) { model.action("undo") } }
            Button { model.action("dismiss") } label: { Label(strings["shelf_sheet_close"], systemImage: "xmark").labelStyle(.iconOnly) }
        }.padding().background(CoffeePeekNativeStyle.surface)
    }
}

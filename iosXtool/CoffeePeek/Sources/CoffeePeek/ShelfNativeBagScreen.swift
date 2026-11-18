import ComposeApp
import SwiftUI

struct ShelfNativeBagScreen: View {
    @ObservedObject var model: ShelfNativeModel
    private var strings: ShelfNativeStrings { model.strings }
    private var state: IosShelfState { model.state }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if let item = state.item { details(item) }
                else if state.missing { Text(strings["shelf_bag_missing_title"]).font(.title2); Text(strings["shelf_bag_missing_body"]) }
                else if state.failed {
                    ShelfNativeStatus(message: strings["shelf_bag_offline_body"], retry: strings["shelf_retry"]) { model.action("retry") }
                } else { ProgressView(strings["shelf_loading"]).frame(maxWidth: .infinity) }
                if let failure = state.failure { ShelfNativeStatus(message: strings.issue(failure), retry: strings["shelf_sheet_close"]) { model.action("dismiss") } }
            }.padding(16)
        }
        .safeAreaInset(edge: .top) { header }
        .safeAreaInset(edge: .bottom) { if state.item != nil { mainAction } }
        .sheet(isPresented: Binding(get: { state.finishingId != nil }, set: { if !$0 { model.action("cancelFinish") } })) {
            if let item = state.item {
                ShelfNativeFinishSheet(screen: model.screen, item: item, strings: strings,
                    finish: { rating, again in model.screen.finish(id: item.id, rating: rating, buyAgain: again) },
                    cancel: { model.action("cancelFinish") })
            }
        }
        .alert(strings["shelf_delete_title"], isPresented: Binding(get: { state.deleting }, set: { if !$0 { model.action("cancelDelete") } })) {
            Button(strings["shelf_bag_delete"], role: .destructive) { model.action("confirmDelete") }
            Button(strings["shelf_cancel"], role: .cancel) { model.action("cancelDelete") }
        } message: { Text(strings["shelf_delete_message"]) }
        .fullScreenCover(isPresented: Binding(get: { state.brewing }, set: { if !$0 { model.screen.closeBrew() } })) {
            ShelfNativeBrewHost(model: model).interactiveDismissDisabled()
        }
    }

    private var header: some View {
        HStack {
            ShelfNativeHeader(title: "", back: strings["shelf_bag_back"]) { model.action("back") }
            if state.item != nil {
                Menu {
                    Button(strings[state.archived ? "shelf_bag_restore" : "shelf_finish_action"]) { model.action(state.archived ? "restore" : "finish") }
                    Button(strings["shelf_bag_delete"], role: .destructive) { model.action("delete") }
                } label: {
                    Label(strings["shelf_bag_more"], systemImage: "ellipsis").labelStyle(.iconOnly).frame(width: 44, height: 44)
                }.disabled(state.busy).padding(.trailing, 12)
            }
        }.background(CoffeePeekNativeStyle.background)
    }

    private func details(_ item: IosShelfItem) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            ShelfNativeCover(screen: model.screen, item: item).frame(height: 270).frame(maxWidth: .infinity)
            Text(item.title ?? strings["shelf_bag_untitled"]).font(.title2.weight(.semibold))
            if let roaster = item.roaster { Text(roaster).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            if item.hasCoffee { Button(strings["shelf_open_catalog"]) { model.action("coffee") } }
            Text(metadata(item)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            if let days = item.days { Text(String(format: strings["shelf_on_shelf_for"], strings.count("days", Int(days.intValue)))) }
            if let date = item.roastedOn { Text("\(strings["shelf_form_roasted"]) · \(date)") }
            if let date = item.finishedOn { Text(String(format: strings["shelf_finished_on"], date)) }
            if let rating = item.rating {
                Label("\(rating.intValue)/5", systemImage: "star.fill").foregroundStyle(CoffeePeekNativeStyle.accent)
            }
            if let again = item.buyAgain { Text(strings[again.boolValue ? "shelf_buy_again" : "shelf_not_again"]) }
            brews
        }
    }

    private var brews: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(strings["shelf_brews_title"]).font(.headline)
            if let left = state.leftGrams { Text(String(format: strings["shelf_bag_left"], String(format: strings["shelf_weight"], left.intValue))) }
            if state.brewsLoading { ProgressView(strings["shelf_brews_loading"]) }
            else if state.brewsFailed {
                ShelfNativeStatus(message: strings["shelf_brews_failed"], retry: strings["shelf_retry"]) { model.action("retryBrews") }
            } else if state.brews.isEmpty { Text(strings["shelf_brews_empty"]).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            ForEach(state.brews, id: \.id) { brew in
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(brew.method ?? strings["shelf_brew_untitled"])
                        if let date = brew.date { Text(date).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                    }
                    Spacer()
                    if let dose = brew.dose { Text(String(format: strings["shelf_brew_dose"], dose.stringValue)) }
                    if let score = brew.score { Label("\(score.intValue)", systemImage: "star.fill").foregroundStyle(CoffeePeekNativeStyle.accent) }
                }.padding(.vertical, 4)
            }
        }
    }

    private func metadata(_ item: IosShelfItem) -> String {
        [item.grams.map { String(format: strings["shelf_weight"], $0.intValue) },
         item.grind.map { strings[$0 == "Beans" ? "shelf_grind_beans" : "shelf_grind_ground"] },
         item.purpose.map { strings[$0 == "Espresso" ? "shelf_roast_espresso" : "shelf_roast_filter"] }]
            .compactMap { $0 }.joined(separator: " · ")
    }

    private var mainAction: some View {
        Button(strings[state.archived ? "shelf_bag_restore" : "shelf_brew_action"]) { model.action(state.archived ? "restore" : "brew") }
            .buttonStyle(.borderedProminent).foregroundStyle(.black)
            .disabled(state.busy).frame(maxWidth: .infinity).padding(16)
            .background(CoffeePeekNativeStyle.background)
    }
}

private struct ShelfNativeBrewHost: View {
    @ObservedObject var model: ShelfNativeModel
    @State private var controller: UIViewController?
    var body: some View {
        Group {
            if let controller { OwnProfileEmbedded(controller: controller).ignoresSafeArea() }
            else { ProgressView() }
        }.onAppear { if controller == nil { controller = model.screen.makeBrew() } }
    }
}

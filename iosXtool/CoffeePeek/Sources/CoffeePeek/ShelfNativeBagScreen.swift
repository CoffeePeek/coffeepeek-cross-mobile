import ComposeApp
import SwiftUI

struct ShelfNativeBagScreen: View {
    @ObservedObject var model: ShelfNativeModel
    private var strings: ShelfNativeStrings { model.strings }
    private var state: IosShelfState { model.state }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if let item = state.item { ShelfNativeBagDetails(state: state, item: item, strings: strings, cover: AnyView(ShelfNativeCover(screen: model.screen, item: item)), action: { model.action($0) }) }
                else if state.missing { Text(strings["shelf_bag_missing_title"]).font(.title2); Text(strings["shelf_bag_missing_body"]) }
                else if state.failed {
                    ShelfNativeStatus(message: strings["shelf_bag_offline_body"], retry: strings["shelf_retry"]) { model.action("retry") }
                } else { CoffeePeekNativeLoader(label: strings["shelf_loading"], size: 32, showsLabel: true).frame(maxWidth: .infinity) }
                if let failure = state.failure { ShelfNativeStatus(message: strings.issue(failure), retry: strings["shelf_sheet_close"]) { model.action("dismiss") } }
            }.padding(16)
        }
        .safeAreaInset(edge: .top) { header }
        .safeAreaInset(edge: .bottom) { if state.item != nil { mainAction } }
        .sheet(isPresented: Binding(get: { state.finishingId != nil }, set: { if !$0 { model.action("cancelFinish") } })) {
            if let item = state.item {
                ShelfNativeFinishSheet(cover: AnyView(ShelfNativeCover(screen: model.screen, item: item)), item: item, strings: strings,
                    rating: Int(state.finishRating), again: state.finishBuyAgain?.boolValue,
                    rate: { model.screen.rate(value: Int32($0)) }, chooseAgain: { model.screen.chooseAgain(value: $0) },
                    finish: { model.screen.finish(id: item.id, skip: $0) },
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
            else { CoffeePeekNativeLoader(size: 24) }
        }.onAppear { if controller == nil { controller = model.screen.makeBrew() } }
            .onDisappear { model.screen.releaseBrew(); controller = nil }
    }
}

private struct ShelfNativeBagDetails: View {
    let state: IosShelfState
    let item: IosShelfItem
    let strings: ShelfNativeStrings
    let cover: AnyView
    let action: (String) -> Void
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            cover.frame(height: 270).frame(maxWidth: .infinity)
            Text(item.title ?? strings["shelf_bag_untitled"]).font(.title2.weight(.semibold))
            if let roaster = item.roaster { Text(roaster).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            if item.hasCoffee { Button(strings["shelf_open_catalog"]) { action("coffee") } }
            Text(metadata(item)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            if let days = item.days { Text(age(Int(days.intValue))) }
            if let roastDays = item.roastDays {
                Text(roastDays.intValue == 0 ? strings["shelf_roasted_today"] : String(format: strings["shelf_roasted_ago"], strings.age(Int(roastDays.intValue))))
            }
            if let date = item.finishedOn { Text(String(format: strings["shelf_finished_on"], strings.date(date))) }
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
            if !state.archived, let left = state.leftGrams { Text(String(format: strings["shelf_bag_left"], String(format: strings["shelf_weight"], left.intValue))) }
            if state.brewsLoading { CoffeePeekNativeLoader(label: strings["shelf_brews_loading"], size: 32, showsLabel: true) }
            else if state.brewsFailed {
                ShelfNativeStatus(message: strings["shelf_brews_failed"], retry: strings["shelf_retry"]) { action("retryBrews") }
            } else if state.brews.isEmpty { Text(strings["shelf_brews_empty"]).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            ForEach(state.brews, id: \.id) { brew in
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(brew.method ?? strings["shelf_brew_untitled"])
                        if let date = brew.date { Text(strings.date(date)).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                    }
                    Spacer()
                    if let dose = brew.dose { Text(String(format: strings["shelf_brew_dose"], dose.stringValue)) }
                    if let score = brew.score { Label("\(score.intValue)", systemImage: "star.fill").foregroundStyle(CoffeePeekNativeStyle.accent) }
                }.padding(.vertical, 4)
            }
        }
    }

    private func age(_ days: Int) -> String {
        guard days > 0 else { return strings["shelf_added_today"] }
        let duration = strings.age(days)
        return String(format: strings["shelf_on_shelf_for"], duration)
    }

    private func metadata(_ item: IosShelfItem) -> String {
        [item.grams.map { String(format: strings["shelf_weight"], $0.intValue) },
         item.grind.map { strings[$0 == "Beans" ? "shelf_grind_beans" : "shelf_grind_ground"] },
         item.purpose.map { strings[$0 == "Espresso" ? "shelf_roast_espresso" : "shelf_roast_filter"] }]
            .compactMap { $0 }.joined(separator: " · ")
    }

 }

@MainActor
private func shelfBagPreview() -> some View {
    ScrollView {
        ShelfNativeBagDetails(state: shelfNativePreviewState(), item: shelfNativePreviewItem(), strings: ShelfNativeStrings("ru"),
            cover: AnyView(CoffeePeekPhotoPlaceholder()), action: { _ in }).padding()
    }.foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
}
#Preview("Bag detail light") { shelfBagPreview().preferredColorScheme(.light) }
#Preview("Bag detail dark") { shelfBagPreview().preferredColorScheme(.dark) }

import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShelfController(screen: IosShelfScreen) -> UIViewController {
    ShelfNativeHostingController(screen: screen)
}

private final class ShelfNativeHostingController: UIHostingController<ShelfNativeHost> {
    private let model: ShelfNativeModel
    init(screen: IosShelfScreen) {
        let model = ShelfNativeModel(screen)
        self.model = model
        super.init(rootView: ShelfNativeHost(model: model))
    }
    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    deinit { model.screen.close() }
}

final class ShelfNativeModel: ObservableObject {
    let screen: IosShelfScreen
    let strings: ShelfNativeStrings
    @Published var state: IosShelfState
    private var observation: IosShelfObservation?

    init(_ screen: IosShelfScreen) {
        self.screen = screen
        strings = ShelfNativeStrings(screen.languageTag)
        state = screen.current()
    }
    func appear() {
        guard observation == nil else { return }
        state = screen.current()
        observation = screen.observe { [weak self] in self?.state = $0 }
        if screen.kind == "shelf" || screen.kind == "public" { action("refresh") }
    }
    func disappear() {
        screen.stopCamera()
        // A full-screen native Home form completes through the bag model; keep that close/saved state observed.
        guard !state.brewing else { return }
        observation?.cancel()
        observation = nil
    }
    func action(_ value: String, _ id: String? = nil) { screen.action(value: value, id: id) }
    func send(_ intent: ShelfNativeAddIntent) {
        switch intent {
        case .command(let value): action(value)
        case .query(let value): screen.query(value: value)
        case .coffee(let slug): screen.coffee(slug: slug)
        case .roaster(let value): screen.roaster(value: value)
        case .weight(let grams): screen.weight(grams: grams.map { KotlinInt(int: Int32($0)) })
        case .grind(let value): screen.grind(value: value)
        case .purpose(let value): screen.purpose(value: value)
        case .date(let value): screen.date(value: value)
        case .save(let later): screen.save(later: later)
        }
    }
    deinit { observation?.cancel() }
}

private struct ShelfNativeHost: View {
    @ObservedObject var model: ShelfNativeModel

    var body: some View {
        Group {
            if !model.state.active { Color.clear }
            else if model.screen.kind == "add" { ShelfNativeAddScreen(model: model) }
            else if model.screen.kind == "bag" { ShelfNativeBagScreen(model: model) }
            else { ShelfNativeGridScreen(model: model) }
        }
        .font(CoffeePeekNativeStyle.font(15))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .background(CoffeePeekNativeStyle.background)
        .onAppear(perform: model.appear)
        .onDisappear(perform: model.disappear)
    }
}

struct ShelfNativeStrings {
    private let bundle: Bundle
    init(_ language: String) {
        // The existing shelf resources are Russian only; retain that fallback for other app languages.
        let url = Bundle.main.url(forResource: language, withExtension: "lproj")
        let translated = url.flatMap { Bundle(url: $0) }
        let ru = Bundle.main.url(forResource: "ru", withExtension: "lproj").flatMap { Bundle(url: $0) }
        bundle = translated?.url(forResource: "NativeShelf", withExtension: "strings") != nil ? translated! : (ru ?? .main)
    }
    subscript(_ key: String) -> String { bundle.localizedString(forKey: key, value: nil, table: "NativeShelf") }
    func count(_ stem: String, _ value: Int) -> String {
        let tail = value % 100
        let form = (11...14).contains(tail) ? "many" : value % 10 == 1 ? "one" : (2...4).contains(value % 10) ? "few" : "many"
        return String(format: self["shelf_\(stem)_\(form)"], value)
    }
    func age(_ days: Int) -> String { days < 14 ? count("days", days) : count("weeks", days / 7) }
    func weight(_ grams: Int) -> String {
        guard grams >= 1000 else { return String(format: self["shelf_weight_g"], grams) }
        let tenths = (grams + 50) / 100
        let value = tenths % 10 == 0 ? "\(tenths / 10)" : "\(tenths / 10),\(tenths % 10)"
        return String(format: self["shelf_weight_kg"], value)
    }
    func date(_ value: String) -> String {
        let parts = value.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return value }
        let month = self["shelf_month_of_\(parts[1])"]
        if parts[0] == Calendar.current.component(.year, from: Date()) { return String(format: self["shelf_date"], parts[2], month) }
        return String(format: self["shelf_date_year"], parts[2], month, parts[0])
    }
    func issue(_ issue: String?) -> String {
        let key = ["Offline": "offline", "Timeout": "offline", "Unavailable": "offline", "SignedOut": "signed_out",
                   "AccountChanged": "signed_out", "Unauthorized": "signed_out", "Stale": "stale", "NotFound": "not_found", "PhotoUpload": "photo_upload",
                   "ShelfFull": "shelf_full", "BeanUnavailable": "bean_unavailable"][issue ?? ""] ?? "generic"
        return self["shelf_error_\(key)"]
    }
    func reason(_ value: String) -> String {
        let key = ["EdgesNotFound": "edges", "NotWholeBag": "whole", "TooFar": "far", "OutOfFrame": "frame",
                   "ObjectsOnBag": "objects", "BagCovered": "covered", "Tilted": "tilted", "MoveCloser": "closer", "Glare": "glare"][value] ?? "edges"
        return self["shelf_reason_\(key)"]
    }
}

struct ShelfNativeCover: View {
    let screen: IosShelfScreen
    let item: IosShelfItem
    var body: some View {
        CoffeePeekCardImage(url: item.hasPhoto ? item.id : nil, symbol: nil, fits: true, loadImage: { id, receive in
            let request = screen.loadCover(id: id, receive: receive)
            return CoffeePeekCardImageRequest { request.cancel() }
        })
        .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}

struct ShelfNativeHeader: View {
    let title: String
    let back: String
    let onBack: () -> Void
    var body: some View {
        HStack {
            Button(action: onBack) { Label(back, systemImage: "chevron.backward").labelStyle(.iconOnly).frame(width: 44, height: 44) }
            Text(title).font(.headline).accessibilityAddTraits(.isHeader)
            Spacer()
        }
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .padding(.horizontal, 8)
        .background(CoffeePeekNativeStyle.background)
    }
}

struct ShelfNativeStatus: View {
    let message: String
    let retry: String
    let onRetry: () -> Void
    var body: some View {
        VStack(spacing: 12) {
            Text(message).multilineTextAlignment(.center)
            Button(retry, action: onRetry).buttonStyle(.bordered)
        }.frame(maxWidth: .infinity).padding(24)
    }
}

enum ShelfNativeAddIntent {
    case command(String), query(String), coffee(String), roaster(String), weight(Int?), grind(String?), purpose(String?), date(String?), save(Bool)
}

func shelfNativePreviewItem() -> IosShelfItem {
    IosShelfItem(id: "preview", title: "Santa Rosa", roaster: "Coffee Roaster", grams: KotlinInt(int: 250),
        grind: "Beans", purpose: "Filter", roastedOn: nil, addedOn: nil, finishedOn: nil, days: nil, roastDays: nil,
        rating: nil, buyAgain: nil, hasPhoto: false, hasCoffee: false)
}

func shelfNativePreviewState() -> IosShelfState {
    let item = shelfNativePreviewItem()
    return IosShelfState(active: true, items: [item], loading: false, failed: false, visible: true,
        finishingId: nil, arrivingId: nil, grams: KotlinInt(int: 250), roasters: 1, undo: false, failure: nil,
        item: item, missing: false, archived: false, deleting: false, busy: false, brewing: false,
        brewsLoading: false, brewsFailed: false, leftGrams: nil, brews: [], visibility: false,
        visibilityEnabled: true, visibilityLoading: false, visibilityFailed: false, revision: 0, add: nil,
        messageKey: nil, finishRating: 5, finishBuyAgain: nil, visibilityError: nil)
}

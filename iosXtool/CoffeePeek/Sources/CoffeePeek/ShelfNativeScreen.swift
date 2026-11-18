import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShelfController(screen: IosShelfScreen) -> UIViewController {
    UIHostingController(rootView: ShelfNativeHost(screen: screen))
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
    func disappear() { observation?.cancel(); observation = nil; screen.stopCamera() }
    func action(_ value: String, _ id: String? = nil) { screen.action(value: value, id: id) }
    deinit { observation?.cancel(); screen.close() }
}

private struct ShelfNativeHost: View {
    @StateObject private var model: ShelfNativeModel
    init(screen: IosShelfScreen) { _model = StateObject(wrappedValue: ShelfNativeModel(screen)) }

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
    func issue(_ issue: String?) -> String {
        let key = ["Offline": "offline", "SignedOut": "signed_out", "Unauthorized": "signed_out", "Stale": "stale", "NotFound": "not_found", "PhotoUpload": "photo_upload",
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

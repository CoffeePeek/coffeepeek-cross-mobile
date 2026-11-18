import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeRoasterController(screen: IosRoasterScreen) -> UIViewController {
    switch screen {
    case let source as IosRoasterDetail:
        return UIHostingController(rootView: RoasterDetailNativeHost(source: source))
    default:
        preconditionFailure("Unsupported native roaster screen")
    }
}

extension View {
    func roasterExposure(_ key: String, source: IosRoasterScreen?) -> some View {
        modifier(RoasterNativeExposure(key: key, source: source))
    }
}

private struct RoasterNativeExposure: ViewModifier {
    let key: String
    let source: IosRoasterScreen?
    @State private var fraction = 0.0
    func body(content: Content) -> some View {
        content.onGeometryChange(for: Double.self) { geometry in
            let rect = geometry.frame(in: .global)
            let visible = rect.intersection(UIScreen.main.bounds)
            let area = rect.width * rect.height
            return area > 0 && !visible.isNull ? Double(visible.width * visible.height / area) : 0
        } action: { next in
            fraction = next
            source?.visible(section: key, fraction: next)
        }
        .onChange(of: source) { previous, next in
            previous?.visible(section: key, fraction: 0)
            next?.visible(section: key, fraction: fraction)
        }
        .onAppear { source?.visible(section: key, fraction: fraction) }
        .onDisappear { source?.visible(section: key, fraction: 0) }
    }
}

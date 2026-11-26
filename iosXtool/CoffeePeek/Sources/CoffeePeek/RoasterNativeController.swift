import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeRoasterController(screen: IosRoasterScreen) -> UIViewController {
    switch screen {
    case let source as IosRoasterDetail:
        return RoasterDetailHostingController(source: source)
    default:
        preconditionFailure("Unsupported native roaster screen")
    }
}

private final class RoasterDetailHostingController: UIHostingController<RoasterDetailNativeHost> {
    private let source: IosRoasterDetail
    init(source: IosRoasterDetail) {
        self.source = source
        super.init(rootView: RoasterDetailNativeHost(source: source))
        safeAreaRegions = []
        view.backgroundColor = .clear
    }
    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        source.setVisible(visible: true)
    }
    override func viewWillDisappear(_ animated: Bool) {
        source.setVisible(visible: false)
        super.viewWillDisappear(animated)
    }
    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        rootView.topInset = view.safeAreaInsets.top
        rootView.bottomInset = view.safeAreaInsets.bottom
    }
    isolated deinit { source.close() }
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
        content.onGeometryChange(for: CGRect.self) { geometry in
            geometry.frame(in: .global)
        } action: { rect in
            let visible = rect.intersection(UIScreen.main.bounds)
            let area = rect.width * rect.height
            let next = area > 0 && !visible.isNull ? Double(visible.width * visible.height / area) : 0
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

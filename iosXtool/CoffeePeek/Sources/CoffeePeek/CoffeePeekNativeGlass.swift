import ComposeApp
import UIKit
import SwiftUI

@MainActor
func configureCoffeePeekNativeGlass() {
    MainViewControllerKt.configureNativeLoader { color, size in
        let side = CGFloat(truncating: size)
        let controller = UIHostingController(rootView: CoffeePeekNativeLoader(color: Color(uiColor: color), size: side)
            .accessibilityHidden(true))
        controller.view.backgroundColor = .clear
        return controller
    }
    // Swift weak-links UIKit classes introduced after the deployment target.
    if #available(iOS 26.0, *) {
        MainViewControllerKt.configureNativeGlass(makeView: {
            let view = UIVisualEffectView(effect: UIGlassEffect(style: .regular))
            view.cornerConfiguration = .capsule()
            return view
        })
    }
}

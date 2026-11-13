import ComposeApp
import UIKit

@MainActor
func configureCoffeePeekNativeGlass() {
    // Swift weak-links UIKit classes introduced after the deployment target.
    if #available(iOS 26.0, *) {
        MainViewControllerKt.configureNativeGlass(makeView: {
            let view = UIVisualEffectView(effect: UIGlassEffect(style: .regular))
            view.cornerConfiguration = .capsule()
            return view
        })
    }
}

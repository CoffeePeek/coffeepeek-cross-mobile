import ComposeApp
import SwiftUI
import UIKit

struct CoffeePeekLocalBackProtection: UIViewControllerRepresentable {
    let protected: Bool
    func makeUIViewController(context: Context) -> LocalBackProtectionController { LocalBackProtectionController() }
    func updateUIViewController(_ controller: LocalBackProtectionController, context: Context) {
        controller.protected = protected
    }
    static func dismantleUIViewController(_ controller: LocalBackProtectionController, coordinator: ()) {
        controller.reset()
    }
}

final class LocalBackProtectionController: UIViewController {
    var protected = false { didSet { apply() } }
    override func loadView() { view = UIView(); view.isUserInteractionEnabled = false }
    override func didMove(toParent parent: UIViewController?) {
        super.didMove(toParent: parent)
        apply()
    }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        apply()
    }
    override func viewDidDisappear(_ animated: Bool) {
        reset()
        super.viewDidDisappear(animated)
    }
    override func willMove(toParent parent: UIViewController?) {
        if parent == nil { reset() }
        super.willMove(toParent: parent)
    }
    func reset() { NativeLocalBackProtection.shared.update(controller: self, enabled: false) }
    private func apply() { NativeLocalBackProtection.shared.update(controller: self, enabled: protected) }
}


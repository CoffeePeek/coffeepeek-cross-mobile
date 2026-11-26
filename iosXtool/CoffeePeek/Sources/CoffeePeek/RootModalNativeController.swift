import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeRootModalController(model: IosRootModal) -> UIViewController {
    let controller: UIViewController
    switch model {
    case let onboarding as IosGuestOnboarding:
        let completion = CoffeePeekNativeModalDismissal()
        controller = UIHostingController(rootView: GuestOnboardingNativeHost(source: onboarding, finish: completion.finish))
        completion.controller = controller
        // Compose owns dismissal/navigation and must stay attached to process these actions.
        controller.modalPresentationStyle = .overFullScreen
    case let update as IosAppUpdate:
        controller = UIHostingController(rootView: AppUpdateNativeHost(source: update))
        controller.modalPresentationStyle = .overFullScreen
        controller.view.backgroundColor = .clear
    case let operation as IosAuthorRootPrompt:
        controller = makeAuthorRootPrompt(operation)
    default:
        preconditionFailure("Unsupported root modal")
    }
    controller.isModalInPresentation = true
    return controller
}

@MainActor
private func makeAuthorRootPrompt(_ model: IosAuthorRootPrompt) -> UIViewController {
    let strings = BlockedAuthorsStrings(languageTag: model.languageTag())
    if let prompt = model.state.prompt, prompt.working {
        let controller = UIHostingController(rootView: CoffeePeekNativeLoader(label: strings.text(prompt.blocking ? "author_blocking_blocking" : "author_blocking_unblocking")))
        controller.modalPresentationStyle = .overFullScreen
        controller.view.backgroundColor = UIColor.black.withAlphaComponent(0.15)
        return controller
    }
    return makeAuthorRootAlert(model, strings: strings)
}

@MainActor
private func makeAuthorRootAlert(_ model: IosAuthorRootPrompt, strings: BlockedAuthorsStrings) -> UIViewController {
    let prompt = model.state.prompt
    let login = model.state.needsLogin
    let operation = prompt?.blocking == true ? "block" : "unblock"
    let title = login ? strings.text("author_blocking_login_title") : prompt?.failed == true ? strings.text("author_blocking_save_failed") :
        strings.authorTitle("author_blocking_\(operation)_title", name: prompt?.name ?? "")
    let message = strings.text(login ? "author_blocking_login_message" : prompt?.failed == true ? "author_blocking_save_failed_message" : "author_blocking_\(operation)_message")
    let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
    let dismissal = CoffeePeekNativeModalDismissal()
    dismissal.controller = alert
    let label = strings.text(login ? "author_blocking_login" : prompt?.failed == true ? "author_blocking_retry" : "author_blocking_\(operation)")
    alert.addAction(UIAlertAction(title: label, style: !login && prompt?.blocking == true && prompt?.failed == false ? .destructive : .default) { _ in
        dismissal.finish { if login { model.signIn() } else { model.confirm() } }
    })
    alert.addAction(UIAlertAction(title: strings.text("author_blocking_cancel"), style: .cancel) { _ in dismissal.finish { model.cancel() } })
    return alert
}

/// UIKit finishes the modal transition before Kotlin changes the presentation state.
@MainActor
final class CoffeePeekNativeModalDismissal {
    weak var controller: UIViewController?
    private var finishing = false

    func finish(_ action: @escaping () -> Void) {
        guard !finishing else { return }
        finishing = true
        guard let controller, controller.presentingViewController != nil else { action(); return }
        controller.dismiss(animated: !UIAccessibility.isReduceMotionEnabled, completion: action)
    }
}

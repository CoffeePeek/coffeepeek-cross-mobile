import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeRootModalController(model: IosRootModal) -> UIViewController {
    let controller: UIViewController
    switch model {
    case let onboarding as IosGuestOnboarding:
        controller = UIHostingController(rootView: GuestOnboardingNativeHost(source: onboarding))
        controller.modalPresentationStyle = .fullScreen
    case let update as IosAppUpdate:
        controller = UIHostingController(rootView: AppUpdateNativeHost(source: update))
        controller.modalPresentationStyle = .overFullScreen
        controller.view.backgroundColor = .clear
    default:
        preconditionFailure("Unsupported root modal")
    }
    controller.isModalInPresentation = true
    return controller
}

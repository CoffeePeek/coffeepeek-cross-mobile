import ComposeApp
import UIKit

/// One serial UIKit presenter; its completion callbacks acknowledge actual presentation/dismissal.
@MainActor
final class CoffeePeekRootModalPresenter: NSObject, UIAdaptivePresentationControllerDelegate {
    private let source: IosNativeRoot
    private weak var host: UIViewController?
    private var desired: IosRootModalState
    private var observation: IosRootObservation?
    private var primary: PresentedRootModal?
    private var blocking: PresentedRootModal?
    private var busy = false
    private var waitingForTransition = false
    private weak var settledCoordinator: AnyObject?

    init(source: IosNativeRoot, host: UIViewController) {
        self.source = source
        self.host = host
        desired = source.currentModals()
    }
    isolated deinit { observation?.cancel() }
    func start() {
        guard observation == nil else { return }
        observation = source.observeModals { [weak self] in
            self?.desired = $0
            self?.flush()
        }
    }
    func flush() {
        guard !busy, let host else { return }
        if waitForTransition(host) { return }
        if let blocking, blocking.model !== desired.blocking || primary?.model !== desired.primary {
            dismiss(blocking, blocking: true); return
        }
        if let primary, primary.model !== desired.primary { dismiss(primary, blocking: false); return }
        if primary == nil, let model = desired.primary { present(model, blocking: false); return }
        if blocking == nil, let model = desired.blocking { present(model, blocking: true); return }
        primary?.controller.isModalInPresentation = !desired.dismissAllowed || primary?.model.fullScreen == true
    }
    private func waitForTransition(_ host: UIViewController) -> Bool {
        guard let coordinator = host.transitionCoordinator else { return false }
        if settledCoordinator === (coordinator as AnyObject) { return false }
        guard !waitingForTransition else { return true }
        waitingForTransition = true
        coordinator.animate(alongsideTransition: nil) { [weak self] _ in
            self?.settledCoordinator = coordinator as AnyObject
            self?.waitingForTransition = false
            self?.flush()
        }
        return true
    }
    private func present(_ model: IosRootPresentation, blocking: Bool) {
        guard let presenter = blocking ? primary?.controller ?? host : host,
              presenter.presentedViewController == nil, presenter.viewIfLoaded?.window != nil,
              !waitForTransition(presenter) else { return }
        guard let controller = source.modalController(presentation: model) else { return }
        if model.fullScreen, !(controller is UIAlertController), controller.modalPresentationStyle != .overFullScreen {
            controller.modalPresentationStyle = .fullScreen
        }
        controller.isModalInPresentation = blocking || !desired.dismissAllowed || model.fullScreen
        let presented = PresentedRootModal(model: model, controller: controller)
        if blocking { self.blocking = presented } else { primary = presented }
        attachObserver(to: controller)
        busy = true
        presenter.present(controller, animated: !UIAccessibility.isReduceMotionEnabled) { [weak self] in
            guard let self else { return }
            controller.presentationController?.delegate = self
            self.busy = false
            self.source.modalPresented(presentation: model)
            self.flush()
        }
    }
    private func dismiss(_ presented: PresentedRootModal, blocking: Bool) {
        guard presented.controller.presentingViewController != nil else {
            finishDismissal(presented, blocking: blocking); return
        }
        guard !waitForTransition(presented.controller) else { return }
        busy = true
        presented.controller.dismiss(animated: !UIAccessibility.isReduceMotionEnabled) { [weak self] in
            self?.busy = false
            self?.finishDismissal(presented, blocking: blocking)
        }
    }
    private func finishDismissal(_ presented: PresentedRootModal, blocking: Bool) {
        guard (blocking ? self.blocking : primary)?.controller === presented.controller else { return }
        if blocking { self.blocking = nil } else { primary = nil }
        source.modalDismissed(presentation: presented.model)
        flush()
    }
    func presentationControllerShouldDismiss(_ presentationController: UIPresentationController) -> Bool {
        guard let primary, presentationController.presentedViewController === primary.controller else { return false }
        return desired.dismissAllowed && !primary.model.fullScreen && source.canDismissModal(presentation: primary.model)
    }
    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        guard let primary, presentationController.presentedViewController === primary.controller else { return }
        self.primary = nil
        source.closeModal(presentation: primary.model)
        source.modalDismissed(presentation: primary.model)
        flush()
    }
    private func attachObserver(to controller: UIViewController) {
        let observer = RootModalAppearanceObserver { [weak self] in self?.flush() }
        controller.addChild(observer)
        observer.view.isUserInteractionEnabled = false
        observer.view.frame = .zero
        controller.view.addSubview(observer.view)
        observer.didMove(toParent: controller)
    }
}

@MainActor
private struct PresentedRootModal {
    let model: IosRootPresentation
    let controller: UIViewController
}

@MainActor
private final class RootModalAppearanceObserver: UIViewController {
    private let appeared: () -> Void
    init(appeared: @escaping () -> Void) { self.appeared = appeared; super.init(nibName: nil, bundle: nil) }
    required init?(coder: NSCoder) { nil }
    override func viewDidAppear(_ animated: Bool) { super.viewDidAppear(animated); appeared() }
}

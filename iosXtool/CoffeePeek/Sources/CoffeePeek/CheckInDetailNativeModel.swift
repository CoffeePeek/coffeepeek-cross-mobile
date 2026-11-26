import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCheckInDetailController(detail: IosCheckInDetail) -> UIViewController {
    CheckInDetailHostingController(detail: detail)
}

@MainActor
private final class CheckInDetailHostingController: UIHostingController<CheckInDetailNativeHost> {
    private let detail: IosCheckInDetail
    private let visibility: CoffeePeekNativeControllerVisibility
    private var navigationObservation: IosCheckInDetailObservation?
    init(detail: IosCheckInDetail) {
        self.detail = detail
        let visibility = CoffeePeekNativeControllerVisibility { detail.setVisible(visible: $0) }
        self.visibility = visibility
        super.init(rootView: CheckInDetailNativeHost(detail: detail, visibility: visibility))
        navigationObservation = detail.observe { [weak self] state in self?.updateTitle(state.title) }
        view.backgroundColor = .clear
    }
    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        updateTitle(detail.current().title)
        visibility.setShowing(true)
    }
    override func viewWillDisappear(_ animated: Bool) {
        visibility.setShowing(false)
        super.viewWillDisappear(animated)
    }
    private func updateTitle(_ title: String) {
        self.title = title
        if let parent { parent.navigationItem.title = title }
    }
    isolated deinit {
        navigationObservation?.cancel()
        detail.close()
    }
}

@MainActor
private struct CheckInDetailNativeHost: View {
    @StateObject private var model: CheckInDetailNativeModel
    let visibility: CoffeePeekNativeControllerVisibility
    @Environment(\.scenePhase) private var scenePhase

    init(detail: IosCheckInDetail, visibility: CoffeePeekNativeControllerVisibility) {
        _model = StateObject(wrappedValue: CheckInDetailNativeModel(detail: detail))
        self.visibility = visibility
    }

    var body: some View {
        CheckInDetailNativeContent(state: model.presentation, loadImage: model.loadImage,
            perform: { model.detail.perform(action: $0) },
            openPhoto: { model.detail.openPhoto(index: Int32($0)) })
            .onAppear {
                model.observe()
                visibility.setForeground(scenePhase == .active)
            }
            .onChange(of: scenePhase) { _, phase in visibility.setForeground(phase == .active) }
            .coffeePeekCommunityOverlays(model.detail.overlays)
    }
}

@MainActor
private final class CheckInDetailNativeModel: ObservableObject {
    let detail: IosCheckInDetail
    @Published private(set) var state: IosCheckInDetailState
    private var observation: IosCheckInDetailObservation?

    init(detail: IosCheckInDetail) {
        self.detail = detail
        state = detail.current()
    }

    isolated deinit { observation?.cancel() }

    func observe() {
        guard observation == nil else { return }
        state = detail.current()
        observation = detail.observe { [weak self] in self?.state = $0 }
    }

    var loadImage: CoffeePeekCardImageLoader {
        { [detail] url, complete in
            let observation = detail.loadImage(url: url, onImage: complete)
            return CoffeePeekCardImageRequest { observation.cancel() }
        }
    }

    var presentation: CheckInDetailNativePresentation {
        guard state.active else { return CheckInDetailNativePresentation(active: false) }
        var result = CheckInDetailNativePresentation(loadingLabel: state.loadingLabel, errorText: state.errorText,
            retryLabel: state.retryLabel, notice: state.notice, dismissAllowed: state.dismissAllowed)
        if let card = state.card, card.active { result.card = CoffeePeekCheckInCardData(state: card) }
        if let confirmation = state.confirmation {
            result.confirmation = CheckInDetailNativeConfirmation(title: confirmation.title, body: confirmation.body,
                confirmLabel: confirmation.confirmLabel, cancelLabel: confirmation.cancelLabel)
        }
        return result
    }
}

import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCheckInDetailController(detail: IosCheckInDetail) -> UIViewController {
    UIHostingController(rootView: CheckInDetailNativeHost(detail: detail))
}

private struct CheckInDetailNativeHost: View {
    @StateObject private var model: CheckInDetailNativeModel

    init(detail: IosCheckInDetail) {
        _model = StateObject(wrappedValue: CheckInDetailNativeModel(detail: detail))
    }

    var body: some View {
        CheckInDetailNativeContent(state: model.presentation, loadImage: model.loadImage,
            perform: { model.detail.perform(action: $0) },
            openPhoto: { model.detail.openPhoto(index: Int32($0)) })
            .onAppear(perform: model.observe)
    }
}

private final class CheckInDetailNativeModel: ObservableObject {
    let detail: IosCheckInDetail
    @Published private(set) var state: IosCheckInDetailState
    private var observation: IosCheckInDetailObservation?

    init(detail: IosCheckInDetail) {
        self.detail = detail
        state = detail.current()
    }

    deinit { observation?.cancel() }

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
        result.actions = state.actions.map {
            CheckInDetailNativeAction(id: $0.id, label: $0.label, enabled: $0.enabled, destructive: $0.destructive)
        }
        if let confirmation = state.confirmation {
            result.confirmation = CheckInDetailNativeConfirmation(title: confirmation.title, body: confirmation.body,
                confirmLabel: confirmation.confirmLabel, cancelLabel: confirmation.cancelLabel)
        }
        return result
    }
}

import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeOwnershipActionsController(actions: IosOwnershipActions) -> UIViewController {
    OwnershipActionsHostingController(actions: actions)
}

private final class OwnershipActionsHostingController: UIHostingController<OwnershipActionsHost> {
    private let model: OwnershipActionsModel
    init(actions: IosOwnershipActions) {
        let model = OwnershipActionsModel(actions)
        self.model = model
        super.init(rootView: OwnershipActionsHost(model: model))
        view.backgroundColor = .clear
        safeAreaRegions = []
    }
    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    deinit { model.close() }
}

private final class OwnershipActionsModel: ObservableObject {
    let actions: IosOwnershipActions
    @Published var owner: Bool
    @Published var active: Bool
    @Published var strings: OwnershipNativeStrings
    @Published var claim: UIViewController?
    private var observation: IosOwnershipObservation?
    init(_ actions: IosOwnershipActions) {
        self.actions = actions
        owner = actions.owner()
        active = actions.active()
        strings = OwnershipNativeStrings(actions.languageTag())
    }
    func appear() {
        guard observation == nil else { return }
        update()
        observation = actions.observe { [weak self] in self?.update() }
    }
    func beginClaim() { claim = actions.makeClaim { [weak self] in self?.claim = nil } }
    func close() { observation?.cancel(); observation = nil; claim = nil; actions.close() }
    private func update() { owner = actions.owner(); active = actions.active(); strings = OwnershipNativeStrings(actions.languageTag()) }
    deinit { observation?.cancel() }
}

private struct OwnershipActionsHost: View {
    @ObservedObject var model: OwnershipActionsModel
    var body: some View {
        Group {
            if model.active {
                OwnershipNativeActionsContent(owner: model.owner, roaster: model.actions.kind == "Roaster", strings: model.strings,
                    edit: model.actions.edit, suggest: model.actions.suggest, claim: model.beginClaim)
            }
        }
        .onAppear(perform: model.appear)
        .sheet(isPresented: Binding(get: { model.claim != nil }, set: { if !$0 { model.claim = nil } })) {
            if let controller = model.claim {
                OwnProfileEmbedded(controller: controller).ignoresSafeArea().interactiveDismissDisabled()
            }
        }
    }
}

private struct OwnershipNativeActionsContent: View {
    let owner: Bool
    let roaster: Bool
    let strings: OwnershipNativeStrings
    let edit: () -> Void
    let suggest: () -> Void
    let claim: () -> Void
    var body: some View {
        Group {
            if owner {
                Button(action: edit) { controlLabel(strings["ownership_edit"], symbol: "square.and.pencil") }
            } else {
                Menu {
                    if !roaster { Button(strings["ownership_edit"], action: suggest) }
                    Button(strings[roaster ? "ownership_representative_roaster" : "ownership_representative"], action: claim)
                } label: { controlLabel(strings["ownership_actions"], symbol: "ellipsis") }
            }
        }
        .buttonStyle(.plain).shopDetailGlassCircle()
        .foregroundStyle(CoffeePeekNativeStyle.foreground).tint(CoffeePeekNativeStyle.foreground)
    }

    private func controlLabel(_ title: String, symbol: String) -> some View {
        Label(title, systemImage: symbol).labelStyle(.iconOnly).font(.system(size: 20))
            .frame(width: 48, height: 48).contentShape(Circle())
    }
}

#Preview("Owner menu light") {
    OwnershipNativeActionsContent(owner: false, roaster: false, strings: OwnershipNativeStrings("ru"), edit: {}, suggest: {}, claim: {})
        .preferredColorScheme(.light)
}
#Preview("Owner menu dark") {
    OwnershipNativeActionsContent(owner: true, roaster: true, strings: OwnershipNativeStrings("ru"), edit: {}, suggest: {}, claim: {})
        .preferredColorScheme(.dark)
}

#Preview("Shop owner action dark") {
    OwnershipNativeActionsContent(owner: true, roaster: false, strings: OwnershipNativeStrings("ru"), edit: {}, suggest: {}, claim: {})
        .preferredColorScheme(.dark)
}
#Preview("Shop owner action light") {
    OwnershipNativeActionsContent(owner: true, roaster: false, strings: OwnershipNativeStrings("ru"), edit: {}, suggest: {}, claim: {})
        .preferredColorScheme(.light)
}

#Preview("Roaster menu light") {
    OwnershipNativeActionsContent(owner: false, roaster: true, strings: OwnershipNativeStrings("ru"), edit: {}, suggest: {}, claim: {})
        .preferredColorScheme(.light)
}
#Preview("Roaster menu dark") {
    OwnershipNativeActionsContent(owner: false, roaster: true, strings: OwnershipNativeStrings("be"), edit: {}, suggest: {}, claim: {})
        .preferredColorScheme(.dark)
}

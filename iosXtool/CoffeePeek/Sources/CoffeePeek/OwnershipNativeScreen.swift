import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeOwnershipController(screen: IosOwnershipScreen) -> UIViewController {
    OwnershipNativeHostingController(screen: screen)
}

private final class OwnershipNativeHostingController: UIHostingController<OwnershipNativeHost>, UIAdaptivePresentationControllerDelegate {
    private let model: OwnershipNativeModel
    init(screen: IosOwnershipScreen) {
        let model = OwnershipNativeModel(screen)
        self.model = model
        super.init(rootView: OwnershipNativeHost(model: model))
        screen.bindPhotoPresenter { [weak self] in self }
    }
    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        var ancestor: UIViewController? = self
        while let current = ancestor {
            if let presentation = current.presentationController, current.presentingViewController != nil {
                current.isModalInPresentation = true
                presentation.delegate = self
                break
            }
            ancestor = current.parent
        }
    }
    func presentationControllerShouldDismiss(_ presentationController: UIPresentationController) -> Bool { false }
    func presentationControllerDidAttemptToDismiss(_ presentationController: UIPresentationController) { model.send(.action("back")) }
    isolated deinit { model.screen.close() }
}

@MainActor
final class OwnershipNativeModel: ObservableObject {
    let screen: IosOwnershipScreen
    @Published var state: IosOwnershipState
    @Published var strings: OwnershipNativeStrings
    private var observation: IosOwnershipObservation?
    init(_ screen: IosOwnershipScreen) {
        self.screen = screen
        state = screen.current()
        strings = OwnershipNativeStrings(screen.languageTag())
    }
    func appear() {
        guard observation == nil else { return }
        state = screen.current()
        observation = screen.observe(receive: { [weak self] in self?.state = $0 }, onLanguage: { [weak self] in self?.strings = OwnershipNativeStrings($0) })
    }
    func disappear() { observation?.cancel(); observation = nil }
    func send(_ intent: OwnershipNativeIntent) {
        let generation = state.generation
        switch intent {
        case .action(let value): screen.action(value: value, generation: generation)
        case .claim(let id): screen.claim(id: id, generation: generation)
        case .business(let kind, let id): screen.business(kind: kind, id: id, generation: generation)
        case .claimsPage(let page): screen.claimsPage(page: Int32(page), generation: generation)
        case .roastersPage(let page): screen.roastersPage(page: Int32(page), generation: generation)
        case .claimText(let field, let value): screen.claimText(field: field, value: value, generation: generation)
        case .profileText(let field, let value): screen.profileText(field: field, value: value, generation: generation)
        case .addPhotos: screen.choosePhotos(generation: generation)
        case .removePhoto(let index, let evidence): screen.removePhoto(index: Int32(index), evidence: evidence, generation: generation)
        case .movePhoto(let from, let to, let evidence): screen.movePhoto(from: Int32(from), to: Int32(to), evidence: evidence, generation: generation)
        }
    }
    var imageLoader: CoffeePeekCardImageLoader {
        let generation = state.generation
        return { [screen] key, receive in
            let job = screen.loadImage(key: key, generation: generation, receive: receive)
            return CoffeePeekCardImageRequest { job.cancel() }
        }
    }
    isolated deinit { observation?.cancel() }
}

enum OwnershipNativeIntent {
    case action(String), claim(String), business(String, String), claimsPage(Int), roastersPage(Int)
    case claimText(String, String), profileText(String, String), addPhotos, removePhoto(Int, Bool), movePhoto(Int, Int, Bool)
}

private struct OwnershipNativeHost: View {
    @ObservedObject var model: OwnershipNativeModel
    var body: some View {
        OwnershipNativeContent(state: model.state, tab: model.screen.tab, strings: model.strings,
            send: model.send, imageLoader: model.imageLoader, qrLink: { model.screen.qrLink(id: $0, generation: model.state.generation) },
            shareQr: { image, name in model.screen.shareQr(image: image, name: name, generation: model.state.generation) })
            .onAppear(perform: model.appear).onDisappear(perform: model.disappear)
    }
}

struct OwnershipNativeContent: View {
    let state: IosOwnershipState
    let tab: Bool
    let strings: OwnershipNativeStrings
    let send: (OwnershipNativeIntent) -> Void
    let imageLoader: CoffeePeekCardImageLoader
    let qrLink: (String) -> String?
    let shareQr: (UIImage, String) -> Void

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                if !state.signedIn {
                    Text(strings["ownership_login_hint"])
                    CoffeePeekNativeActionButton(title: strings["ownership_login"]) { send(.action("login")) }
                } else {
                    feedback
                    if state.loading { CoffeePeekNativeLoader(label: strings["ownership_loading"], size: 32, showsLabel: true).frame(maxWidth: .infinity) }
                    else { destination }
                }
            }.padding(16).padding(.bottom, tab ? 100 : 16)
        }
        .refreshable { send(.action("refresh")) }
        .safeAreaInset(edge: .top) { header }
        .font(CoffeePeekNativeStyle.font(15))
        .foregroundStyle(CoffeePeekNativeStyle.foreground).tint(CoffeePeekNativeStyle.accent)
        .background(CoffeePeekNativeStyle.background)
        .id(state.generation)
        .alert(strings[state.confirmPublish ? "ownership_publish_title" : "ownership_discard_title"],
            isPresented: Binding(get: { state.confirmPublish || state.confirmDiscard }, set: { _ in })) {
                Button(strings[state.confirmPublish ? "ownership_save" : "ownership_discard"], role: state.confirmPublish ? nil : .destructive) {
                    send(.action(state.confirmPublish ? "save" : state.reloadAfterDiscard ? "reload" : "discard"))
                }
                Button(strings["ownership_cancel"], role: .cancel) { send(.action("cancel")) }
            } message: { Text(strings[state.confirmPublish ? "ownership_publish_hint" : "ownership_discard_hint"]) }
    }

    private var header: some View {
        Group {
            if tab {
                HStack {
                    Text(title).font(.largeTitle.weight(.semibold)).accessibilityAddTraits(.isHeader)
                    Spacer()
                }.padding(.horizontal, 16).padding(.vertical, 8)
            } else {
                CoffeePeekNativeNavigationHeader(title: title, backLabel: strings["ownership_back"],
                    back: { send(.action("back")) })
            }
        }.background(CoffeePeekNativeStyle.background)
    }
    private var title: String {
        if tab { return strings["ownership_business"] }
        return strings[state.destination == "Editor" ? "ownership_editor_title" : state.destination == "Overview" ? "ownership_title" : "ownership_claim_title"]
    }
    @ViewBuilder private var feedback: some View {
        if state.saved { Text(strings["ownership_saved"]).foregroundStyle(CoffeePeekNativeStyle.accent) }
        if let problem = state.problem {
            VStack(alignment: .leading, spacing: 12) {
                Text(strings.problem(problem))
                CoffeePeekNativeActionButton(title: strings["ownership_retry"], primary: false, enabled: !state.busy && !state.loading) { send(.action("retry")) }
            }.padding(12).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 12))
        }
        if state.busy { CoffeePeekNativeLoader(label: strings["ownership_loading"], size: 32, showsLabel: true) }
    }
    @ViewBuilder private var destination: some View {
        if tab || state.destination == "Overview" {
            OwnershipNativeOverview(state: state, tab: tab, strings: strings, send: send, imageLoader: imageLoader, qrLink: qrLink, shareQr: shareQr)
        } else if state.destination == "Submit" {
            OwnershipNativeClaimForm(state: state, strings: strings, send: send, imageLoader: imageLoader)
        } else if state.destination == "Claim", let claim = state.claim {
            OwnershipNativeClaimDetails(claim: claim, strings: strings, imageLoader: imageLoader)
        } else if state.destination == "Editor" {
            OwnershipNativeEditor(state: state, strings: strings, send: send, imageLoader: imageLoader)
        }
    }
}

struct OwnershipNativeStrings {
    private let bundle: Bundle
    init(_ language: String) {
        let url = Bundle.main.url(forResource: language, withExtension: "lproj") ?? Bundle.main.url(forResource: "ru", withExtension: "lproj")
        bundle = url.flatMap { Bundle(url: $0) } ?? .main
    }
    subscript(_ key: String) -> String { bundle.localizedString(forKey: key, value: nil, table: "NativeOwnership") }
    func status(_ value: String) -> String { self["ownership_\(value.lowercased())"] }
    func problem(_ value: String) -> String {
        let key = ["Unavailable": "unavailable", "SignedOut": "signedout", "NotFound": "notfound", "Conflict": "conflict", "Validation": "validation", "PartialSave": "partial"][value] ?? "unavailable"
        return self["ownership_error_\(key)"]
    }
    func field(_ name: String) -> String { self["ownership_\(name.lowercased())"] }
}

func ownershipPreviewClaim() -> IosOwnershipClaim {
    IosOwnershipClaim(id: "claim", title: "CoffeePeek Roasters", slug: "preview-roaster", status: "Approved", access: false,
        contact: "owner@example.test", evidence: "Representative", created: "18.11.2026, 12:30", reviewed: nil, decision: nil, photos: [])
}
func ownershipPreviewState(_ destination: String = "Overview") -> IosOwnershipState {
    IosOwnershipState(signedIn: true, generation: 1, destination: destination, loading: false, busy: false,
        problem: nil, saved: false, dirty: true, reloadRequired: false, confirmPublish: false, confirmDiscard: false,
        reloadAfterDiscard: false, claims: [ownershipPreviewClaim()], claimsPage: 1, claimsNext: false,
        businesses: [IosOwnershipBusiness(id: "shop", kind: "CoffeeShop", title: "CoffeePeek", address: "Example Street, 1", cover: nil),
            IosOwnershipBusiness(id: "roaster", kind: "Roaster", title: "CoffeePeek Roasters", address: nil, cover: nil)],
        roastersPage: 1, roastersNext: false, contact: "owner@example.test", evidence: "Representative", evidencePhotos: [],
        claim: ownershipPreviewClaim(), editorKind: "Roaster", editorPhotos: [], fields: [IosOwnershipField(name: "Name", value: "CoffeePeek Roasters"), IosOwnershipField(name: "About", value: "Coffee")])
}
@MainActor
private func ownershipTabPreview() -> some View {
    OwnershipNativeContent(state: ownershipPreviewState(), tab: true, strings: OwnershipNativeStrings("ru"),
        send: { _ in }, imageLoader: { _, _ in nil }, qrLink: { _ in nil }, shareQr: { _, _ in })
}
#Preview("Business tab light") { ownershipTabPreview().preferredColorScheme(.light) }
#Preview("Business tab dark") { ownershipTabPreview().preferredColorScheme(.dark) }

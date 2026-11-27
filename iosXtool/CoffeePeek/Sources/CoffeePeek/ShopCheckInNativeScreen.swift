import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShopCheckInRouteController(route: IosShopCheckInRoute) -> UIViewController {
    UIHostingController(rootView: ShopCheckInRouteHost(model: ShopCheckInRouteModel(route)))
}

@MainActor
private struct ShopCheckInRouteHost: View {
    @StateObject var model: ShopCheckInRouteModel
    var body: some View {
        Group {
            if let editor = model.state.editor {
                ShopCheckInRouteContent(editor: editor).id(ObjectIdentifier(editor)).ignoresSafeArea(.container)
            } else {
                CoffeePeekNativeRouteStatus(failed: model.state.failed, title: model.state.title, retry: model.state.retry,
                    back: model.state.back, onRetry: { model.route.retry() }, onBack: { model.route.back() })
            }
        }
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .onAppear(perform: model.observe)
        .background(CoffeePeekLocalBackProtection(protected: model.state.editor != nil))
    }
}

@MainActor
private final class ShopCheckInRouteModel: ObservableObject {
    let route: IosShopCheckInRoute
    @Published private(set) var state: IosShopCheckInRouteState
    private var observation: IosShopCheckInObservation?
    init(_ route: IosShopCheckInRoute) {
        self.route = route
        state = route.current()
    }
    isolated deinit {
        observation?.cancel()
        route.close()
    }
    func observe() {
        guard observation == nil else { return }
        observation = route.observe { [weak self] in self?.state = $0 }
    }
}

private struct ShopCheckInRouteContent: UIViewControllerRepresentable {
    let editor: IosShopCheckInEditor
    func makeUIViewController(context: Context) -> UIViewController { makeShopCheckInEditorController(editor: editor) }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ShopCheckInNativeActions {
    var noteChanged: (String) -> Void = { _ in }
    var ratingChanged: (Int32, Int32, Int32) -> Void = { _, _, _ in }
    var dateChanged: (String) -> Void = { _ in }
    var visibilityChanged: (Bool) -> Void = { _ in }
    var drinkChanged: (String?) -> Void = { _ in }
    var retryDrinks: () -> Void = {}
    var choosePhotos: (Bool) -> Void = { _ in }
    var removePhoto: (Int32) -> Void = { _ in }
    var movePhoto: (Int32, Int32) -> Void = { _, _ in }
    var submit: () -> Void = {}
    var dismiss: () -> Void = {}
    var askDiscard: () -> Void = {}
    var cancelDiscard: () -> Void = {}
    var discard: () -> Void = {}
    var historyChecked: () -> Void = {}
    var openHistory: () -> Void = {}
    var goToFeed: () -> Void = {}
    var changeShop: () -> Void = {}
}

@MainActor
func makeShopCheckInEditorController(editor: IosShopCheckInEditor) -> UIViewController {
    ShopCheckInHostingController(editor: editor)
}

@MainActor
private final class ShopCheckInHostingController: UIHostingController<ShopCheckInNativeHost> {
    private let editor: IosShopCheckInEditor
    init(editor: IosShopCheckInEditor) {
        self.editor = editor
        super.init(rootView: ShopCheckInNativeHost(editor: editor))
        editor.bindPresenter { [weak self] in self }
    }
    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    isolated deinit { editor.close() }
}

private struct ShopCheckInNativeHost: View {
    @StateObject private var model: ShopCheckInNativeModel

    init(editor: IosShopCheckInEditor) {
        _model = StateObject(wrappedValue: ShopCheckInNativeModel(editor: editor))
    }

    var body: some View {
        ShopCheckInNativeScreen(state: model.state, strings: model.strings, actions: model.actions)
            .onAppear(perform: model.observe)
            .background(CoffeePeekLocalBackProtection(protected: true))
    }
}

@MainActor
final class ShopCheckInNativeModel: ObservableObject {
    let editor: IosShopCheckInEditor
    @Published private(set) var state: IosShopCheckInState
    @Published private(set) var strings: CheckInNativeStrings
    private var observation: IosShopCheckInObservation?

    init(editor: IosShopCheckInEditor) {
        self.editor = editor
        state = editor.current()
        strings = CheckInNativeStrings(languageTag: editor.languageTag(), table: "NativeShopCheckIn")
    }

    isolated deinit { observation?.cancel() }

    var actions: ShopCheckInNativeActions {
        ShopCheckInNativeActions(noteChanged: editor.noteChanged, ratingChanged: editor.ratingChanged,
            dateChanged: editor.dateChanged, visibilityChanged: editor.visibilityChanged,
            drinkChanged: editor.drinkChanged,
            retryDrinks: editor.retryDrinks, choosePhotos: editor.choosePhotos,
            removePhoto: editor.removePhoto, movePhoto: editor.movePhoto,
            submit: editor.submit, dismiss: editor.dismiss, askDiscard: editor.askDiscard,
            cancelDiscard: editor.cancelDiscard, discard: editor.discard,
            historyChecked: editor.historyChecked, openHistory: editor.openHistory,
            goToFeed: editor.goToFeed, changeShop: editor.changeShop)
    }

    func observe() {
        guard observation == nil else { return }
        state = editor.current()
        observation = editor.observe(onState: { [weak self] in self?.state = $0 }, onLanguage: { [weak self] in
            self?.strings = CheckInNativeStrings(languageTag: $0, table: "NativeShopCheckIn")
        })
    }

}

struct ShopCheckInNativeScreen: View {
    let state: IosShopCheckInState
    let strings: CheckInNativeStrings
    let actions: ShopCheckInNativeActions
    @State private var keyboardVisible = false
    @State private var phase: ShopCheckInNativePhase

    init(state: IosShopCheckInState, strings: CheckInNativeStrings, actions: ShopCheckInNativeActions,
         initialPhase: ShopCheckInNativePhase? = nil) {
        self.state = state
        self.strings = strings
        self.actions = actions
        let details = state.deliveryUnconfirmed || ["shop_checkin_form_invalid_date", "shop_checkin_form_invalid_drink"].contains(state.validationKey ?? "")
        _phase = State(initialValue: initialPhase ?? (details ? .details : .ratings))
    }

    var body: some View {
        VStack(spacing: 0) {
            if state.active && state.submitted {
                ShopCheckInNativeOutcome(state: state, strings: strings, feed: actions.goToFeed, dismiss: actions.dismiss)
            } else {
                header
                if state.active { progress.padding(.horizontal, 16).padding(.bottom, 8) }
                scrollContent
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if state.active && !state.loading && !state.submitting && !state.submitted && !keyboardVisible { footer }
        }
        .font(CoffeePeekNativeStyle.font(16))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .tint(CoffeePeekNativeStyle.accent)
        .environment(\.locale, Locale(identifier: strings.languageTag))
        .onChange(of: state.validationKey) { _, key in routeValidation(key) }
        .onChange(of: state.deliveryUnconfirmed) { _, uncertain in if uncertain { phase = .details } }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in keyboardVisible = true }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in keyboardVisible = false }
        .confirmationDialog(strings["shop_checkin_discard_title"], isPresented: discardBinding, titleVisibility: .visible) {
            Button(strings["shop_checkin_discard"], role: .destructive, action: actions.discard)
            Button(strings["shop_checkin_discard_cancel"], role: .cancel, action: actions.cancelDiscard)
        } message: { Text(strings["shop_checkin_discard_message"]) }
    }

    private var scrollContent: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Color.clear.frame(height: 0).id("shop-checkin-top").accessibilityHidden(true)
                    formContent
                }.padding(.horizontal, 16).padding(.vertical, 16)
            }
            .scrollDismissesKeyboard(.interactively)
            .onChange(of: phase) { _, _ in proxy.scrollTo("shop-checkin-top", anchor: .top) }
        }
    }

    @ViewBuilder private var formContent: some View {
        if !state.active { Text(strings["shop_checkin_error_unauthorized"]) }
        else if state.loading || state.submitting {
            CoffeePeekNativeLoader(label: strings["shop_checkin_form_sending"], size: 32, showsLabel: true)
                .frame(maxWidth: .infinity).padding(.vertical, 32)
        } else {
            if hasWarnings { warnings }
            ShopCheckInNativeFields(state: state, strings: strings, actions: actions, phase: phase)
        }
    }

    private var discardBinding: Binding<Bool> {
        Binding(get: { state.active && state.confirmDiscard }, set: { if !$0 { actions.cancelDiscard() } })
    }

    private var header: some View {
        HStack(spacing: 8) {
            if state.active {
                CoffeePeekNativeChevronButton(label: strings["shop_checkin_step_back"], symbol: "chevron.backward", action: back)
                    .disabled(!canNavigate)
            } else {
                CoffeePeekNativeChevronButton(label: strings["shop_checkin_form_dismiss"], symbol: "xmark", action: actions.dismiss)
            }
            Text(stepTitle).font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
                .multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity).accessibilityAddTraits(.isHeader)
            if state.active && state.hasDraft && !state.loading && !state.submitting {
                CoffeePeekNativeChevronButton(label: strings["shop_checkin_discard"], symbol: "trash", action: actions.askDiscard)
                    .disabled(navigationBusy)
            } else { Color.clear.frame(width: 44, height: 44).accessibilityHidden(true) }
        }.padding(.horizontal, 16).padding(.vertical, 8)
    }

    private var stepTitle: String {
        if !state.active { return strings["shop_checkin_form_title"] }
        return strings[phase == .ratings ? "shop_checkin_step_impressions" : "shop_checkin_step_details"]
    }

    private var hasWarnings: Bool {
        state.deliveryUnconfirmed || state.draftFailed || state.serverMessage?.isEmpty == false ||
            state.failureKey != nil || state.validationKey != nil
    }

    private var warnings: some View {
        VStack(alignment: .leading, spacing: 12) {
            if state.deliveryUnconfirmed {
                Text(strings["shop_checkin_form_uncertain"])
                Button(strings["shop_checkin_form_history"], action: actions.openHistory)
                Button(strings["shop_checkin_form_history_checked"], action: actions.historyChecked)
            }
            if state.draftFailed { Text(strings["shop_checkin_form_draft_error"]).foregroundStyle(.red) }
            if let message = state.serverMessage, !message.isEmpty {
                Text(message).foregroundStyle(.red)
            } else if let key = state.failureKey {
                Text(strings[key]).foregroundStyle(.red)
            }
            if let key = state.validationKey { Text(strings[key]).foregroundStyle(.red) }
        }.accessibilityElement(children: .contain)
    }

    private var progress: some View {
        let total = state.shopSelectionIncluded ? 3 : 2
        let step = total - (phase == .ratings ? 1 : 0)
        return ShopCheckInNativeProgress(step: step, total: total,
            caption: strings.format("shop_checkin_step_progress", step, total))
    }

    private var navigationBusy: Bool {
        state.loading || state.submitting || state.discarding || state.photosPreparing || state.changingShop
    }

    private var canNavigate: Bool { state.active && !navigationBusy }

    private func back() {
        guard canNavigate else { return }
        if phase == .details { phase = .ratings }
        else if state.shopSelectionIncluded { actions.changeShop() }
        else { actions.dismiss() }
    }

    private func routeValidation(_ key: String?) {
        guard state.active, let key else { return }
        switch key {
        case "shop_checkin_form_invalid_note", "shop_checkin_form_invalid_rating",
             "shop_checkin_form_invalid_photos", "shop_checkin_form_invalid_photo": phase = .ratings
        case "shop_checkin_form_invalid_date", "shop_checkin_form_invalid_drink": phase = .details
        default: break
        }
    }

    private func submit() {
        guard state.editable && !state.changingShop else { return }
        routeValidation(state.validationKey)
        actions.submit()
    }

    private var footer: some View {
        VStack(spacing: 12) {
            if phase == .ratings {
                CheckInNativePrimaryButton(title: strings["shop_checkin_step_continue"], enabled: canNavigate) {
                    guard canNavigate else { return }
                    phase = .details
                }
            } else {
                CheckInNativePrimaryButton(title: strings["shop_checkin_form_submit"], busy: state.submitting,
                    enabled: state.editable && !state.changingShop, action: submit)
            }
        }.padding(16).background(CoffeePeekNativeStyle.background)
    }

}

struct ShopCheckInNativeOutcome: View {
    let state: IosShopCheckInState
    let strings: CheckInNativeStrings
    let feed: () -> Void
    var dismiss: () -> Void = {}

    var body: some View {
        CoffeePeekNativeSuccessScreen(title: strings["shop_checkin_form_success_published"],
            description: strings[state.isPublic ? "shop_checkin_form_success_public" : "shop_checkin_form_success_private"],
            buttonTitle: strings["shop_checkin_form_feed"], closeLabel: strings["shop_checkin_form_dismiss"],
            notice: state.draftFailed ? strings["shop_checkin_form_cleanup_error"] : nil, noticeIsError: true,
            onDismiss: dismiss, action: feed)
    }
}

func shopCheckInPreviewState(uncertain: Bool = false, submitted: Bool = false, shopSelectionIncluded: Bool = false) -> IosShopCheckInState {
    IosShopCheckInState(shopName: "CoffeePeek", shopAddress: "ул. Кофейная, 12", note: "Ягодный фильтр",
        placeRating: 4, serviceRating: 5, coffeeRating: 4, visitDate: "2026-11-17", today: "2026-11-17",
        dateLabel: "17 ноября", isPublic: true, drinkSlug: "other", customDrink: "Авторский фильтр",
        drinks: [IosShopCheckInDrink(slug: "filter", name: "Фильтр"), IosShopCheckInDrink(slug: "other", name: "Другое")],
        photos: [], active: true, loading: false, editable: !uncertain && !submitted, photosPreparing: false,
        draftFailed: false, hasDraft: true, confirmDiscard: false, discarding: false,
        drinksLoading: false, drinksFailed: false, submitting: false, submitted: submitted,
        publication: "Unknown", deliveryUnconfirmed: uncertain, failureKey: nil, serverMessage: nil, validationKey: nil,
        changingShop: false, shopSelectionIncluded: shopSelectionIncluded)
}

#Preview("Shop form light") {
    ShopCheckInNativeScreen(state: shopCheckInPreviewState(),
        strings: CheckInNativeStrings(languageTag: "ru", table: "NativeShopCheckIn"), actions: ShopCheckInNativeActions())
        .preferredColorScheme(.light)
}

#Preview("Shop form dark") {
    ShopCheckInNativeScreen(state: shopCheckInPreviewState(uncertain: true),
        strings: CheckInNativeStrings(languageTag: "be", table: "NativeShopCheckIn"), actions: ShopCheckInNativeActions())
        .preferredColorScheme(.dark)
}

#Preview("Shop success light") {
    ShopCheckInNativeOutcome(state: shopCheckInPreviewState(submitted: true),
        strings: CheckInNativeStrings(languageTag: "ru", table: "NativeShopCheckIn"), feed: {})
        .preferredColorScheme(.light)
}

#Preview("Shop success dark") {
    ShopCheckInNativeOutcome(state: shopCheckInPreviewState(submitted: true),
        strings: CheckInNativeStrings(languageTag: "be", table: "NativeShopCheckIn"), feed: {})
        .preferredColorScheme(.dark)
}

#Preview("Shop details step light") {
    ShopCheckInNativeScreen(state: shopCheckInPreviewState(shopSelectionIncluded: true),
        strings: CheckInNativeStrings(languageTag: "ru", table: "NativeShopCheckIn"),
        actions: ShopCheckInNativeActions(), initialPhase: .details).preferredColorScheme(.light)
}
#Preview("Shop details step dark") {
    ShopCheckInNativeScreen(state: shopCheckInPreviewState(shopSelectionIncluded: true),
        strings: CheckInNativeStrings(languageTag: "be", table: "NativeShopCheckIn"),
        actions: ShopCheckInNativeActions(), initialPhase: .details).preferredColorScheme(.dark)
}

#Preview("Global shop impressions light") {
    ShopCheckInNativeScreen(state: shopCheckInPreviewState(shopSelectionIncluded: true),
        strings: CheckInNativeStrings(languageTag: "ru", table: "NativeShopCheckIn"),
        actions: ShopCheckInNativeActions()).preferredColorScheme(.light)
}
#Preview("Global shop impressions dark") {
    ShopCheckInNativeScreen(state: shopCheckInPreviewState(shopSelectionIncluded: true),
        strings: CheckInNativeStrings(languageTag: "be", table: "NativeShopCheckIn"),
        actions: ShopCheckInNativeActions()).preferredColorScheme(.dark)
}

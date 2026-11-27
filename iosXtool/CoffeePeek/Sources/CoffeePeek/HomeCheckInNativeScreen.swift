import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeHomeCheckInRouteController(route: IosHomeCheckInRoute) -> UIViewController {
    UIHostingController(rootView: HomeCheckInRouteHost(model: HomeCheckInRouteModel(route)))
}

@MainActor
private struct HomeCheckInRouteHost: View {
    @StateObject var model: HomeCheckInRouteModel
    var body: some View {
        let photo = model.state.photo
        Group {
            if let detail = model.state.detail {
                VStack(spacing: 0) {
                    CoffeePeekNativeNavigationHeader(title: model.state.navigationTitle, backLabel: model.state.detailBack,
                        back: { model.route.back() }).disabled(!model.state.dismissAllowed)
                    HomeCheckInRouteDetail(detail: detail).id(ObjectIdentifier(detail))
                }
            } else {
                CoffeePeekNativeRouteStatus(failed: model.state.failed, title: model.state.title, retry: model.state.retry,
                    back: model.state.back, onRetry: { model.route.retry() }, onBack: { model.route.back() })
            }
        }
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .onAppear(perform: model.observe)
        .fullScreenCover(item: model.editorBinding) { item in
            HomeCheckInRouteEditor(editor: item.editor).interactiveDismissDisabled(!model.state.dismissAllowed)
        }
        .coffeePeekPhotoViewer(photo?.viewer, languageTag: photo?.languageTag ?? "ru") {
            if let photo { model.route.dismissPhoto(photo: photo) }
        }
        .background(CoffeePeekLocalBackProtection(protected: model.state.editor != nil || !model.state.dismissAllowed))
    }
}

@MainActor
private final class HomeCheckInRouteModel: ObservableObject {
    let route: IosHomeCheckInRoute
    @Published private(set) var state: IosHomeCheckInRouteState
    private var observation: IosHomeCheckInObservation?
    init(_ route: IosHomeCheckInRoute) {
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
    var editorBinding: Binding<HomeCheckInRouteEditorIdentity?> {
        let presented = state.editor
        return Binding(get: { self.state.editor.map(HomeCheckInRouteEditorIdentity.init) }, set: {
            if $0 == nil, let presented { self.route.requestEditorDismiss(editor: presented) }
        })
    }
}

private struct HomeCheckInRouteEditorIdentity: Identifiable {
    let editor: IosHomeCheckInEditor
    var id: ObjectIdentifier { ObjectIdentifier(editor) }
}
private struct HomeCheckInRouteEditor: UIViewControllerRepresentable {
    let editor: IosHomeCheckInEditor
    func makeUIViewController(context: Context) -> UIViewController { makeHomeCheckInEditorController(editor: editor) }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
private struct HomeCheckInRouteDetail: UIViewControllerRepresentable {
    let detail: IosCheckInDetail
    func makeUIViewController(context: Context) -> UIViewController { makeCheckInDetailController(detail: detail) }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@MainActor
func makeHomeCheckInEditorController(editor: IosHomeCheckInEditor) -> UIViewController {
    HomeCheckInHostingController(editor: editor)
}

@MainActor
private final class HomeCheckInHostingController: UIHostingController<HomeCheckInNativeHost> {
    private let editor: IosHomeCheckInEditor
    init(editor: IosHomeCheckInEditor) {
        self.editor = editor
        super.init(rootView: HomeCheckInNativeHost(editor: editor))
        editor.bindPresenter { [weak self] in self }
    }
    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    isolated deinit { editor.close() }
}

private struct HomeCheckInNativeHost: View {
    @StateObject private var model: HomeCheckInNativeModel
    init(editor: IosHomeCheckInEditor) {
        _model = StateObject(wrappedValue: HomeCheckInNativeModel(editor: editor))
    }
    var body: some View {
        HomeCheckInNativeContent(state: model.state, strings: model.strings, today: model.editor.today(), actions: model.actions)
            .environment(\.locale, Locale(identifier: model.strings.languageTag))
            .onAppear(perform: model.observe)
            .background(CoffeePeekLocalBackProtection(protected: true))
    }
}

@MainActor
final class HomeCheckInNativeModel: ObservableObject {
    @Published private(set) var state: IosHomeCheckInState
    @Published private(set) var strings: CheckInNativeStrings
    let editor: IosHomeCheckInEditor
    private var observation: IosHomeCheckInObservation?
    init(editor: IosHomeCheckInEditor) {
        self.editor = editor
        state = editor.current()
        strings = CheckInNativeStrings(languageTag: editor.languageTag(), table: "NativeHomeCheckIn")
    }
    isolated deinit { observation?.cancel() }
    func observe() {
        guard observation == nil else { return }
        state = editor.current()
        observation = editor.observe(onState: { [weak self] in self?.state = $0 }, onLanguage: { [weak self] in
            self?.strings = CheckInNativeStrings(languageTag: $0, table: "NativeHomeCheckIn")
        })
    }
    var actions: HomeCheckInNativeActions {
        let editor = self.editor
        return HomeCheckInNativeActions(close: editor.requestClose, retry: editor.retryDetail, save: editor.save,
            done: editor.finishSaved, date: { editor.dateChanged(civilDate: $0) },
            drink: { editor.drinkSelected(slug: $0) }, retryDrinks: editor.retryDrinks, note: { editor.noteChanged(value: $0) },
            taste: { editor.tasteTapped(score: Int32($0)) }, visibility: { editor.visibilityChanged(isPublic: $0) },
            method: { editor.methodChanged(value: $0) }, retryMethods: editor.retryMethods,
            dose: { editor.doseChanged(value: $0) }, duration: { editor.durationChanged(seconds: $0.map { KotlinInt(int: Int32($0)) }) },
            openBeans: editor.openBeanPicker, closeBeans: editor.closeBeanPicker,
            query: { editor.beanQueryChanged(value: $0) }, search: editor.searchBeans,
            bean: { editor.beanSelected(slug: $0) }, customBean: { editor.customBeanChosen(name: $0) },
            shelfBag: { editor.shelfBagSelected(id: $0) }, showBag: { editor.showShelfBagChanged(show: $0) },
            photos: { editor.choosePhotos(camera: $0) }, removePhoto: { editor.removePhoto(index: Int32($0)) },
            movePhoto: { editor.movePhoto(from: Int32($0), to: Int32($1)) },
            loadImage: { editor.loadImage(url: $0, onImage: $1) })
    }
}

struct HomeCheckInNativeActions {
    var close: () -> Void = {}
    var retry: () -> Void = {}
    var save: () -> Void = {}
    var done: () -> Void = {}
    var date: (String) -> Void = { _ in }
    var drink: (String?) -> Void = { _ in }
    var retryDrinks: () -> Void = {}
    var note: (String) -> Void = { _ in }
    var taste: (Int) -> Void = { _ in }
    var visibility: (Bool) -> Void = { _ in }
    var method: (String?) -> Void = { _ in }
    var retryMethods: () -> Void = {}
    var dose: (String) -> Void = { _ in }
    var duration: (Int?) -> Void = { _ in }
    var openBeans: () -> Void = {}
    var closeBeans: () -> Void = {}
    var query: (String) -> Void = { _ in }
    var search: () -> Void = {}
    var bean: (String) -> Void = { _ in }
    var customBean: (String) -> Void = { _ in }
    var shelfBag: (String) -> Void = { _ in }
    var showBag: (Bool) -> Void = { _ in }
    var photos: (Bool) -> Void = { _ in }
    var removePhoto: (Int) -> Void = { _ in }
    var movePhoto: (Int, Int) -> Void = { _, _ in }
    var loadImage: (String, @escaping (UIImage?) -> Void) -> IosHomeCheckInObservation? = { _, _ in nil }
}

enum HomeCheckInNativePhase: Equatable { case ratings, details }

struct HomeCheckInNativeContent: View {
    let state: IosHomeCheckInState
    let strings: CheckInNativeStrings
    let today: String
    let actions: HomeCheckInNativeActions
    @State private var keyboardVisible = false
    @State private var phase: HomeCheckInNativePhase

    init(state: IosHomeCheckInState, strings: CheckInNativeStrings, today: String,
         actions: HomeCheckInNativeActions, initialPhase: HomeCheckInNativePhase = .ratings) {
        self.state = state
        self.strings = strings
        self.today = today
        self.actions = actions
        _phase = State(initialValue: Self.errorPhase(state) ?? initialPhase)
    }

    var body: some View {
        Group {
            if state.status.closed || state.status.revoked { Color.clear }
            else if let saved = state.saved { HomeCheckInNativeSaved(saved: saved, strings: strings, done: actions.done) }
            else if state.catalog.beanPickerOpen {
                HomeCheckInNativeBeanPicker(state: state, strings: strings, actions: actions)
            } else { editor }
        }
        .font(CoffeePeekNativeStyle.font(15)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .interactiveDismissDisabled(!state.status.dismissAllowed)
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in keyboardVisible = true }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in keyboardVisible = false }
        .onChange(of: issueSignature) { _, _ in routeValidation() }
        .onChange(of: state.status.saveAttempt) { _, _ in routeValidation() }
        .onChange(of: summary) { _, message in
            if let message { UIAccessibility.post(notification: .announcement, argument: message) }
        }
    }

    private var editor: some View {
        VStack(spacing: 0) {
            header
            if !state.status.editing {
                ShopCheckInNativeProgress(step: phase == .ratings ? 1 : 2, total: 2,
                    caption: strings.format("home_check_in_step_progress", phase == .ratings ? 1 : 2, 2))
                    .padding(.horizontal, 16).padding(.bottom, 8)
            }
            if state.status.ready { scrollContent } else { loadStatus }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if state.status.ready && !keyboardVisible { footer }
        }
    }

    private var scrollContent: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    Color.clear.frame(height: 0).id("home-checkin-top").accessibilityHidden(true)
                    errorSummary
                    if state.status.editing {
                        dateAndDrink
                        impressions
                        HomeCheckInNativeRecipe(state: state, strings: strings, actions: actions)
                    } else if phase == .ratings { impressions }
                    else { details }
                }.padding(.horizontal, 16).padding(.vertical, 8)
            }.scrollDismissesKeyboard(.interactively)
                .onChange(of: phase) { _, _ in proxy.scrollTo("home-checkin-top", anchor: .top) }
        }
    }

    private var header: some View {
        HStack(spacing: 8) {
            if !state.status.editing {
                CoffeePeekNativeChevronButton(label: strings["home_check_in_step_back"], symbol: "chevron.backward", action: back)
                    .disabled(phase == .details ? !canNavigate : !state.status.dismissAllowed)
            }
            Text(headerTitle).font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
                .multilineTextAlignment(state.status.editing ? .leading : .center)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: state.status.editing ? .leading : .center)
                .accessibilityAddTraits(.isHeader)
            if state.status.editing {
                CoffeePeekNativeChevronButton(label: strings["home_check_in_editor_close"], symbol: "xmark", action: actions.close)
                    .disabled(!state.status.dismissAllowed)
            } else { Color.clear.frame(width: 44, height: 44).accessibilityHidden(true) }
        }.padding(.horizontal, 16).padding(.vertical, 8)
    }

    private var headerTitle: String {
        if state.status.editing { return strings["home_check_in_editor_title"] }
        return strings[phase == .ratings ? "home_check_in_step_impressions" : "home_check_in_step_details"]
    }

    private var impressions: some View {
        VStack(alignment: .leading, spacing: 20) {
            CheckInNativeField(title: "", error: error("Rating"), style: .home) {
                HomeCheckInNativeRating(strings: strings, value: state.draft.tasteScore?.intValue, select: actions.taste)
            }
            note
            HomeCheckInNativePhotos(state: state, strings: strings, actions: actions)
        }.disabled(!state.status.canEdit)
    }

    private var details: some View {
        VStack(alignment: .leading, spacing: 20) {
            dateAndDrink
            HomeCheckInNativeRecipe(state: state, strings: strings, actions: actions)
            visibility
        }
    }

    private var dateAndDrink: some View {
        VStack(alignment: .leading, spacing: 20) {
            CheckInNativeField(title: strings["home_check_in_prepared_date"], error: error("Date"), style: .home) {
                CheckInNativeDateField(title: strings["home_check_in_choose_date"], value: state.draft.preparedOn,
                    today: today, label: state.draft.dateLabel, done: strings["home_check_in_done"],
                    cancel: strings["home_check_in_cancel"], changed: actions.date, style: .home, error: error("Date"))
            }
            drink
        }.disabled(!state.status.canEdit)
    }

    private var drink: some View {
        CheckInNativeField(title: strings["home_check_in_drink"], error: error("Drink"), style: .home) {
            if state.catalog.drinksLoading { CoffeePeekNativeLoader(label: strings["home_check_in_drink"], size: 24) }
            if let key = state.catalog.drinksIssueKey {
                Text(strings[key]).foregroundStyle(CoffeePeekNativeStyle.error)
                Button(strings["home_check_in_retry"], action: actions.retryDrinks)
            } else if !state.catalog.drinksLoading && drinkOptions.isEmpty {
                Text(strings["home_check_in_drinks_empty"]).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
            CoffeePeekNativeDrinkChips(options: drinkOptions, selected: state.draft.drinkSlug,
                legacyTitle: legacyDrinkTitle, changed: actions.drink)
        }
    }

    private var drinkOptions: [CoffeePeekNativeDrinkOption] {
        state.catalog.drinks.filter { $0.id != "other" }.map { .init(id: $0.id, title: $0.name ?? $0.id) }
    }

    private var legacyDrinkTitle: String? {
        if !state.draft.drinkName.isEmpty { return state.draft.drinkName }
        return state.draft.drinkSlug.map { $0 == "other" ? strings["home_check_in_drink_other"] : $0 }
    }

    private var note: some View {
        let title = strings["home_check_in_editor_note"]
        let limit = 1000
        return CheckInNativeField(title: title, error: error("Note"), style: .home,
            counter: "\(state.draft.note.utf16.count)/\(limit)") {
            CheckInNativeInput(title: title, placeholder: strings["home_check_in_note_hint"],
                value: Binding(get: { state.draft.note }, set: { actions.note(String($0.prefix(limit))) }),
                multiline: true, style: .home, error: error("Note"))
        }
    }

    private var footer: some View {
        VStack(spacing: 8) {
            if state.status.editing { visibility }
            if !state.status.editing && phase == .ratings {
                CheckInNativePrimaryButton(title: strings["home_check_in_step_next"], enabled: canNavigate) {
                    dismissKeyboard()
                    phase = .details
                }
            } else {
                CheckInNativePrimaryButton(title: strings[state.status.saving ? "home_check_in_saving" : "home_check_in_save"],
                    busy: state.status.saving, enabled: canNavigate, action: save)
            }
        }.padding(16).background(CoffeePeekNativeStyle.background)
    }

    private var visibility: some View {
        CheckInNativeField(title: "", error: error("Visibility"), style: .home) {
            Toggle(isOn: Binding(get: { state.draft.isPublic?.boolValue == true }, set: actions.visibility)) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(strings["home_check_in_public_title"]).font(CoffeePeekNativeStyle.font(17, weight: "Semibold"))
                    Text(strings["home_check_in_public_hint"]).font(CoffeePeekNativeStyle.font(13))
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                }
            }.disabled(!state.status.canEdit)
        }
    }

    private var canNavigate: Bool {
        state.status.ready && state.status.canEdit && !state.status.loading && !state.status.saving && !state.status.photosPreparing
    }

    private func back() {
        if phase == .details {
            guard canNavigate else { return }
            dismissKeyboard()
            phase = .ratings
        } else if state.status.dismissAllowed { actions.close() }
    }

    private func save() {
        guard canNavigate else { return }
        dismissKeyboard()
        routeValidation()
        actions.save()
    }

    private func dismissKeyboard() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }

    private var issueSignature: [String] { state.issues.fields.map { $0.field + ":" + $0.messageKey } }

    private func routeValidation() {
        if !state.status.editing, let failedPhase = Self.errorPhase(state) { phase = failedPhase }
    }

    private static func errorPhase(_ state: IosHomeCheckInState) -> HomeCheckInNativePhase? {
        if state.issues.fields.contains(where: { ["Rating", "Note", "Photos"].contains($0.field) }) { return .ratings }
        return state.issues.fields.isEmpty ? nil : .details
    }

    private var loadStatus: some View {
        VStack(spacing: 16) {
            if state.status.loading { CoffeePeekNativeLoader(label: strings["home_check_in_loading"], size: 32, showsLabel: true) }
            if let key = state.issues.loadKey {
                Text(strings[key]).foregroundStyle(CoffeePeekNativeStyle.error)
                Button(strings["home_check_in_retry"], action: actions.retry).disabled(state.status.loading)
            }
        }.frame(maxWidth: .infinity, maxHeight: .infinity).padding(16)
    }

    @ViewBuilder private var errorSummary: some View {
        if let summary { Text(summary).foregroundStyle(CoffeePeekNativeStyle.error) }
    }
    private var summary: String? {
        if !state.issues.fields.isEmpty { return strings["home_check_in_error_input"] }
        return state.issues.saveKey.map { strings[$0] }
    }
    private func error(_ field: String) -> String? {
        state.issues.fields.first { $0.field == field }.map { strings[$0.messageKey] }
    }
}

private struct HomeCheckInNativeRating: View {
    let strings: CheckInNativeStrings
    let value: Int?
    let select: (Int) -> Void
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        Group {
            if dynamicTypeSize.isAccessibilitySize {
                stacked
            } else {
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: 12) {
                        artwork
                        rating
                    }
                    stacked
                }
            }
        }
        .padding(.horizontal, 8).padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CoffeePeekNativeStyle.foreground.opacity(0.04), in: RoundedRectangle(cornerRadius: 12))
        .overlay { RoundedRectangle(cornerRadius: 12).strokeBorder(CheckInNativeFormStyle.homeOutline).allowsHitTesting(false) }
    }

    private var stacked: some View {
        VStack(alignment: .leading, spacing: 8) {
            artwork
            rating
        }
    }

    private var rating: some View {
        CheckInNativeRating(title: strings["home_check_in_taste_question"], value: value,
            label: { strings.format("home_check_in_choose_rating", $0) }, select: select)
    }

    private var artwork: some View {
        Group {
            if let image = Self.coffeeArtwork {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                Image(systemName: "cup.and.saucer").font(.system(size: 28))
                    .foregroundStyle(CoffeePeekNativeStyle.secondaryAccent)
            }
        }
        .frame(width: 56, height: 56).clipShape(RoundedRectangle(cornerRadius: 8))
        .accessibilityHidden(true)
    }

    private static let coffeeArtwork: UIImage? = {
        let resource = "coffeepeek.composeapp.generated.resources/drawable"
        for prefix in ["compose-resources/composeResources", "compose-resources"] {
            if let url = Bundle.main.url(forResource: "checkin_rating_coffee", withExtension: "png",
                                         subdirectory: "\(prefix)/\(resource)"),
               let image = UIImage(contentsOfFile: url.path) {
                return image
            }
        }
        return nil
    }()
}

func homeCheckInNativePreviewStrings(_ language: String) -> CheckInNativeStrings {
    CheckInNativeStrings(languageTag: language, table: "NativeHomeCheckIn")
}

func homeCheckInNativePreviewState(editing: Bool = false) -> IosHomeCheckInState {
    let draft = IosHomeCheckInDraft(preparedOn: "2026-11-17", dateLabel: "17.11.2026", methodCode: "pour_over",
        dose: "18", durationSeconds: KotlinInt(int: 180), tasteScore: KotlinInt(int: 4), note: "", drinkName: "V60",
        isPublic: KotlinBoolean(bool: true), beanName: nil, shelfBagId: nil, showShelfBag: false,
        shelfBagCardUrl: nil, photos: [], existingPhotoUrls: [], customBeanName: nil, drinkSlug: nil)
    let status = IosHomeCheckInStatus(editing: editing, ready: true, loading: false, saving: false,
        closed: false, revoked: false, canEdit: true, canAddPhotos: !editing, dismissAllowed: true,
        photosPreparing: false, saveAttempt: 0)
    let catalog = IosHomeCheckInCatalog(beanPickerOpen: false, beanQuery: "", methods: [],
        beans: [], shelfBags: [], methodsLoading: false, beansLoading: false, methodsIssueKey: nil, beansIssueKey: nil,
        drinks: [.init(id: "espresso", name: "Эспрессо", detail: nil, imageUrl: nil),
                 .init(id: "filter", name: "Фильтр-кофе", detail: nil, imageUrl: nil)],
        drinksLoading: false, drinksIssueKey: nil)
    return IosHomeCheckInState(draft: draft, status: status, catalog: catalog,
        issues: IosHomeCheckInIssues(loadKey: nil, saveKey: nil, fields: []), saved: nil)
}

#Preview("Home editor light") {
    HomeCheckInNativeContent(state: homeCheckInNativePreviewState(), strings: homeCheckInNativePreviewStrings("ru"),
                            today: "2026-11-17", actions: .init()).preferredColorScheme(.light)
}
#Preview("Home editor dark") {
    HomeCheckInNativeContent(state: homeCheckInNativePreviewState(editing: true), strings: homeCheckInNativePreviewStrings("be"),
                            today: "2026-11-17", actions: .init()).preferredColorScheme(.dark)
}

#Preview("Home taste rating at 320 pt") {
    HomeCheckInNativeRating(strings: homeCheckInNativePreviewStrings("ru"), value: 3, select: { _ in })
        .padding(16).frame(width: 320)
        .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
        .preferredColorScheme(.light)
}

#Preview("Home taste rating with large text at 320 pt") {
    HomeCheckInNativeRating(strings: homeCheckInNativePreviewStrings("be"), value: 5, select: { _ in })
        .padding(16).frame(width: 320).dynamicTypeSize(.accessibility2)
        .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
        .preferredColorScheme(.dark)
}

#Preview("Home creation details light") {
    HomeCheckInNativeContent(state: homeCheckInNativePreviewState(), strings: homeCheckInNativePreviewStrings("ru"),
        today: "2026-11-17", actions: .init(), initialPhase: .details).preferredColorScheme(.light)
}
#Preview("Home creation details dark") {
    HomeCheckInNativeContent(state: homeCheckInNativePreviewState(), strings: homeCheckInNativePreviewStrings("be"),
        today: "2026-11-17", actions: .init(), initialPhase: .details).preferredColorScheme(.dark)
}
#Preview("Home creation ratings dark") {
    HomeCheckInNativeContent(state: homeCheckInNativePreviewState(), strings: homeCheckInNativePreviewStrings("be"),
        today: "2026-11-17", actions: .init()).preferredColorScheme(.dark)
}
#Preview("Home edit light") {
    HomeCheckInNativeContent(state: homeCheckInNativePreviewState(editing: true), strings: homeCheckInNativePreviewStrings("ru"),
        today: "2026-11-17", actions: .init()).preferredColorScheme(.light)
}
#Preview("Home details large text") {
    HomeCheckInNativeContent(state: homeCheckInNativePreviewState(), strings: homeCheckInNativePreviewStrings("ru"),
        today: "2026-11-17", actions: .init(), initialPhase: .details)
        .dynamicTypeSize(.accessibility2).preferredColorScheme(.light)
}

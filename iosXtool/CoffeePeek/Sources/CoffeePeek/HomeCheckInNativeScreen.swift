import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeHomeCheckInEditorController(editor: IosHomeCheckInEditor) -> UIViewController {
    UIHostingController(rootView: HomeCheckInNativeHost(editor: editor))
}

private struct HomeCheckInNativeHost: View {
    @StateObject private var model: HomeCheckInNativeModel
    init(editor: IosHomeCheckInEditor) {
        _model = StateObject(wrappedValue: HomeCheckInNativeModel(editor: editor))
    }
    var body: some View {
        HomeCheckInNativeContent(state: model.state, strings: model.strings, today: model.editor.today(), actions: model.actions)
            .environment(\.locale, Locale(identifier: model.strings.languageTag))
            .onAppear(perform: model.observe).onDisappear(perform: model.stopObserving)
    }
}

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
    deinit { observation?.cancel() }
    func observe() {
        guard observation == nil else { return }
        state = editor.current()
        observation = editor.observe(onState: { [weak self] in self?.state = $0 }, onLanguage: { [weak self] in
            self?.strings = CheckInNativeStrings(languageTag: $0, table: "NativeHomeCheckIn")
        })
    }
    func stopObserving() {
        observation?.cancel()
        observation = nil
    }
    var actions: HomeCheckInNativeActions {
        let editor = self.editor
        return HomeCheckInNativeActions(close: editor.requestClose, retry: editor.retryDetail, save: editor.save,
            done: editor.finishSaved, date: { editor.dateChanged(civilDate: $0) },
            drink: { editor.drinkChanged(value: $0) }, note: { editor.noteChanged(value: $0) },
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
    var drink: (String) -> Void = { _ in }
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

struct HomeCheckInNativeContent: View {
    let state: IosHomeCheckInState
    let strings: CheckInNativeStrings
    let today: String
    let actions: HomeCheckInNativeActions
    @State private var keyboardVisible = false

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
        .onChange(of: summary) { _, message in
            if let message { UIAccessibility.post(notification: .announcement, argument: message) }
        }
    }

    private var editor: some View {
        VStack(spacing: 0) {
            header
            if state.status.ready {
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        errorSummary
                        primaryFields
                        HomeCheckInNativeRecipe(state: state, strings: strings, actions: actions)
                    }.padding(.horizontal, 16).padding(.vertical, 8)
                }
                .scrollDismissesKeyboard(.interactively)
                if !keyboardVisible { footer }
            } else { loadStatus }
        }
    }

    private var header: some View {
        HStack {
            Text(strings[state.status.editing ? "home_check_in_editor_title" : "home_check_in_home"])
                .font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
                .accessibilityAddTraits(.isHeader)
            Spacer()
            Button(action: actions.close) { Image(systemName: "xmark").frame(width: 48, height: 48) }
                .accessibilityLabel(strings["home_check_in_editor_close"])
                .disabled(state.status.saving).tint(CoffeePeekNativeStyle.foreground)
        }.padding(.horizontal, 16).padding(.bottom, 16)
    }

    private var primaryFields: some View {
        VStack(alignment: .leading, spacing: 20) {
            CheckInNativeField(title: strings["home_check_in_prepared_date"], error: error("Date")) {
                CheckInNativeDateField(title: strings["home_check_in_choose_date"], value: state.draft.preparedOn,
                    today: today, label: state.draft.dateLabel, done: strings["home_check_in_done"],
                    cancel: strings["home_check_in_cancel"], changed: actions.date)
            }
            input("home_check_in_drink", hint: "home_check_in_drink_hint", value: state.draft.drinkName, field: "Drink", changed: actions.drink)
            CheckInNativeField(title: "", error: error("Rating")) {
                CheckInNativeRating(title: strings["home_check_in_taste_question"], value: state.draft.tasteScore?.intValue,
                    label: { strings.format("home_check_in_choose_rating", $0) }, select: actions.taste)
            }
            input("home_check_in_editor_note", hint: "home_check_in_note_hint", value: state.draft.note,
                  field: "Note", multiline: true, changed: actions.note)
            HomeCheckInNativePhotos(state: state, strings: strings, actions: actions)
        }.disabled(!state.status.canEdit)
    }

    private func input(_ title: String, hint: String, value: String, field: String, multiline: Bool = false,
                       changed: @escaping (String) -> Void) -> some View {
        CheckInNativeField(title: strings[title], error: error(field)) {
            CheckInNativeInput(title: strings[title], placeholder: strings[hint],
                value: Binding(get: { value }, set: { changed(String($0.prefix(multiline ? 1000 : 100))) }), multiline: multiline)
        }
    }

    private var footer: some View {
        VStack(spacing: 8) {
            CheckInNativeField(title: "", error: error("Visibility")) {
                Toggle(isOn: Binding(get: { state.draft.isPublic?.boolValue == true }, set: actions.visibility)) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(strings["home_check_in_public_title"]).font(CoffeePeekNativeStyle.font(17, weight: "Semibold"))
                        Text(strings["home_check_in_public_hint"]).font(CoffeePeekNativeStyle.font(13))
                            .foregroundStyle(CoffeePeekNativeStyle.secondary)
                    }
                }.disabled(!state.status.canEdit)
            }
            CheckInNativePrimaryButton(title: strings[state.status.saving ? "home_check_in_saving" : "home_check_in_save"],
                busy: state.status.saving, enabled: state.status.canEdit && !state.status.photosPreparing) {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    actions.save()
                }
        }.padding(16).background(CoffeePeekNativeStyle.background)
    }

    private var loadStatus: some View {
        VStack(spacing: 16) {
            if state.status.loading { ProgressView(strings["home_check_in_loading"]) }
            if let key = state.issues.loadKey {
                Text(strings[key]).foregroundStyle(.red)
                Button(strings["home_check_in_retry"], action: actions.retry).disabled(state.status.loading)
            }
        }.frame(maxWidth: .infinity, maxHeight: .infinity).padding(16)
    }

    @ViewBuilder private var errorSummary: some View {
        if let summary { Text(summary).foregroundStyle(.red) }
    }
    private var summary: String? {
        if !state.issues.fields.isEmpty { return strings["home_check_in_error_input"] }
        return state.issues.saveKey.map { strings[$0] }
    }
    private func error(_ field: String) -> String? {
        state.issues.fields.first { $0.field == field }.map { strings[$0.messageKey] }
    }
}

func homeCheckInNativePreviewStrings(_ language: String) -> CheckInNativeStrings {
    CheckInNativeStrings(languageTag: language, table: "NativeHomeCheckIn")
}

func homeCheckInNativePreviewState(editing: Bool = false) -> IosHomeCheckInState {
    let draft = IosHomeCheckInDraft(preparedOn: "2026-11-17", dateLabel: "17.11.2026", methodCode: "pour_over",
        dose: "18", durationSeconds: KotlinInt(int: 180), tasteScore: KotlinInt(int: 4), note: "", drinkName: "V60",
        isPublic: KotlinBoolean(bool: true), beanName: nil, shelfBagId: nil, showShelfBag: false,
        shelfBagCardUrl: nil, photos: [], existingPhotoUrls: [], customBeanName: nil)
    let status = IosHomeCheckInStatus(editing: editing, ready: true, loading: false, saving: false,
        closed: false, revoked: false, canEdit: true, canAddPhotos: !editing, dismissAllowed: true,
        photosPreparing: false, saveAttempt: 0)
    let catalog = IosHomeCheckInCatalog(beanPickerOpen: false, beanQuery: "", methods: [],
        beans: [], shelfBags: [], methodsLoading: false, beansLoading: false, methodsIssueKey: nil, beansIssueKey: nil)
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

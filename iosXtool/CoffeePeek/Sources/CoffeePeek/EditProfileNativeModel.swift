import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeEditProfileController(editor: IosEditProfile) -> UIViewController {
    EditProfileHostingController(editor: editor)
}

@MainActor
private final class EditProfileHostingController: UIHostingController<EditProfileNativeHost> {
    private let editor: IosEditProfile
    private let visibility: CoffeePeekNativeControllerVisibility
    init(editor: IosEditProfile) {
        self.editor = editor
        let visibility = CoffeePeekNativeControllerVisibility { editor.setVisible(visible: $0) }
        self.visibility = visibility
        super.init(rootView: EditProfileNativeHost(editor: editor, visibility: visibility))
        editor.bindPresenter { [weak self] in self }
        view.backgroundColor = .clear
    }
    @MainActor required dynamic init?(coder: NSCoder) { return nil }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        visibility.setShowing(true)
    }
    override func viewWillDisappear(_ animated: Bool) {
        visibility.setShowing(false)
        super.viewWillDisappear(animated)
    }
    isolated deinit { editor.close() }
}

@MainActor
private struct EditProfileNativeHost: View {
    @StateObject private var model: EditProfileNativeModel
    let visibility: CoffeePeekNativeControllerVisibility
    @Environment(\.scenePhase) private var scenePhase
    init(editor: IosEditProfile, visibility: CoffeePeekNativeControllerVisibility) {
        _model = StateObject(wrappedValue: EditProfileNativeModel(editor: editor))
        self.visibility = visibility
    }
    var body: some View {
        EditProfileNativeContent(state: model.state, strings: model.strings, actions: model.actions)
            .environment(\.locale, Locale(identifier: model.strings.languageTag))
            .background(CoffeePeekLocalBackProtection(protected: model.state.saving).frame(width: 0, height: 0))
            .onAppear {
                model.observe()
                visibility.setForeground(scenePhase == .active)
            }
            .onChange(of: scenePhase) { _, phase in visibility.setForeground(phase == .active) }
    }
}

@MainActor
private final class EditProfileNativeModel: ObservableObject {
    @Published private(set) var state: IosEditProfileState
    @Published private(set) var strings: EditProfileNativeStrings
    let editor: IosEditProfile
    private var observation: IosEditProfileObservation?
    init(editor: IosEditProfile) {
        self.editor = editor
        state = editor.current()
        strings = EditProfileNativeStrings(languageTag: editor.languageTag())
    }
    isolated deinit { observation?.cancel() }
    func observe() {
        guard observation == nil else { return }
        state = editor.current()
        observation = editor.observe(onState: { [weak self] in self?.state = $0 }, onLanguage: { [weak self] in
            self?.strings = EditProfileNativeStrings(languageTag: $0)
        })
    }
    var actions: EditProfileNativeActions {
        let editor = self.editor
        return EditProfileNativeActions(back: editor.back, save: editor.save, retry: editor.reload,
            username: { editor.usernameChanged(value: $0) }, about: { editor.aboutChanged(value: $0) },
            choosePhoto: { editor.choosePhoto(camera: $0) }, deleteAccount: editor.deleteAccount,
            confirmUsername: editor.confirmUsername, cancelUsername: editor.cancelUsername,
            dismissError: editor.dismissError, loadImage: { editor.loadImage(url: $0, onImage: $1) })
    }
}

struct EditProfileNativeActions {
    var back: () -> Void = {}
    var save: () -> Void = {}
    var retry: () -> Void = {}
    var username: (String) -> Void = { _ in }
    var about: (String) -> Void = { _ in }
    var choosePhoto: (Bool) -> Void = { _ in }
    var deleteAccount: () -> Void = {}
    var confirmUsername: () -> Void = {}
    var cancelUsername: () -> Void = {}
    var dismissError: () -> Void = {}
    var loadImage: (String, @escaping (UIImage?) -> Void) -> IosEditProfileObservation? = { _, _ in nil }
}

struct EditProfileNativeStrings {
    let languageTag: String
    subscript(_ key: String) -> String {
        let bundle = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
            .flatMap(Bundle.init(url:)) ?? .main
        return bundle.localizedString(forKey: key, value: nil, table: "NativeEditProfile")
    }
    func format(_ key: String, _ value: String) -> String {
        String(format: self[key].replacingOccurrences(of: "$s", with: "$@"), value)
    }
}

import ComposeApp
import SwiftUI
import UIKit

/// A covered controller stays hidden when its scene becomes active again.
@MainActor
final class CoffeePeekNativeControllerVisibility {
    private var showing = false
    private var foreground = UIApplication.shared.applicationState == .active
    private var published = false
    private let update: (Bool) -> Void

    init(update: @escaping (Bool) -> Void) { self.update = update }
    func setShowing(_ showing: Bool) {
        self.showing = showing
        publish()
    }
    func setForeground(_ foreground: Bool) {
        self.foreground = foreground
        publish()
    }
    private func publish() {
        let next = showing && foreground
        guard next != published else { return }
        published = next
        update(next)
    }
}

@MainActor
func makeCommunitySheetController(sheet: IosCommunitySheet) -> UIViewController {
    CommunitySheetHostingController(sheet: sheet)
}

@MainActor
private final class CommunitySheetHostingController: UIHostingController<CommunitySheetHost>, UIAdaptivePresentationControllerDelegate {
    private let model: CommunitySheetModel

    init(sheet: IosCommunitySheet) {
        let model = CommunitySheetModel(sheet: sheet)
        self.model = model
        super.init(rootView: CommunitySheetHost(model: model))
        modalPresentationStyle = .pageSheet
        sheetPresentationController?.detents = [.medium(), .large()]
        sheetPresentationController?.prefersGrabberVisible = true
        presentationController?.delegate = self
        view.backgroundColor = .clear
    }

    @MainActor required dynamic init?(coder: NSCoder) { return nil }

    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        model.perform("dismiss")
    }
}

private struct CommunitySheetHost: View {
    @ObservedObject var model: CommunitySheetModel

    var body: some View {
        Group {
            if model.state.active {
                ScrollView {
                    Group {
                        if let editor = model.state.editor {
                            CommunityEditorContent(state: model.state, editor: editor, perform: model.perform)
                        } else if let report = model.state.report {
                            CommunityReportContent(state: model.state, report: report, perform: model.perform)
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .scrollDismissesKeyboard(.interactively)
            } else { Color.clear }
        }
        .font(CoffeePeekNativeStyle.font(16))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .background(CoffeePeekNativeStyle.background)
        .interactiveDismissDisabled(model.state.busy)
        .onAppear(perform: model.observe)
    }
}

@MainActor
private final class CommunitySheetModel: ObservableObject {
    @Published private(set) var state: CommunitySheetSnapshot
    private let sheet: IosCommunitySheet
    private var observation: IosCommunitySheetObservation?

    init(sheet: IosCommunitySheet) {
        self.sheet = sheet
        state = CommunitySheetSnapshot.decode(sheet.current())
    }

    // The feature owner retains the draft across Swift graph recreation.
    isolated deinit { observation?.cancel() }

    func observe() {
        guard observation == nil else { return }
        state = CommunitySheetSnapshot.decode(sheet.current())
        observation = sheet.observe { [weak self] json in self?.state = CommunitySheetSnapshot.decode(json) }
    }

    func perform(_ action: String, _ value: String = "") { sheet.perform(action: action, value: value) }
}

extension View {
    @ViewBuilder
    func coffeePeekCommunityOverlays(_ source: IosCommunityOverlays?) -> some View {
        if let source { modifier(CommunityOverlaysModifier(source: source)) } else { self }
    }
}

@MainActor
private struct CommunityOverlaysModifier: ViewModifier {
    @StateObject private var model: CommunityOverlaysModel
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    init(source: IosCommunityOverlays) {
        _model = StateObject(wrappedValue: CommunityOverlaysModel(source: source))
    }
    func body(content: Content) -> some View {
        let photo = model.state.photo
        content
            .onAppear(perform: model.observe)
            .sheet(item: model.sheetBinding) { item in CommunitySheetContainer(sheet: item.source, model: model) }
            .coffeePeekPhotoViewer(photo?.viewer, languageTag: photo?.languageTag ?? "ru") {
                if let photo { model.source.dismissPhoto(photo: photo) }
            }
            .overlay(alignment: .bottom) {
                ZStack {
                    if let message = model.state.message {
                        CoffeePeekNativeMessage(text: message.text, actionLabel: message.actionLabel, dismissLabel: message.dismissLabel,
                            dismiss: { message.dismiss() }, performAction: { message.performAction() })
                            .padding(.horizontal, 16).padding(.bottom, max(0, model.state.bottomClearance) + 8)
                            .id(ObjectIdentifier(message)).transition(.opacity)
                            .onAppear { UIAccessibility.post(notification: .announcement, argument: message.text) }
                    }
                }
                .animation(reduceMotion ? nil : .easeInOut(duration: 0.2), value: model.state.message.map { ObjectIdentifier($0) })
            }
    }
}

@MainActor
private struct CommunitySheetContainer: View {
    let sheet: IosCommunitySheet
    @ObservedObject var model: CommunityOverlaysModel
    var body: some View {
        CommunitySheetControllerView(sheet: sheet)
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
            .presentationContentInteraction(.scrolls)
            .presentationBackground(CoffeePeekNativeStyle.background)
            .interactiveDismissDisabled(model.state.sheet?.isBusy() == true)
    }
}

@MainActor
private struct CommunitySheetControllerView: UIViewControllerRepresentable {
    let sheet: IosCommunitySheet
    func makeUIViewController(context: Context) -> UIViewController { makeCommunitySheetController(sheet: sheet) }
    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}

private struct CommunitySheetIdentity: Identifiable {
    let source: IosCommunitySheet
    var id: ObjectIdentifier { ObjectIdentifier(source) }
}

@MainActor
private final class CommunityOverlaysModel: ObservableObject {
    let source: IosCommunityOverlays
    @Published private(set) var state: IosCommunityOverlayState
    private var observation: IosCommunityOverlayObservation?
    init(source: IosCommunityOverlays) {
        self.source = source
        state = source.current()
    }
    isolated deinit { observation?.cancel() }
    func observe() {
        guard observation == nil else { return }
        state = source.current()
        observation = source.observe { [weak self] in self?.state = $0 }
    }
    var sheetBinding: Binding<CommunitySheetIdentity?> {
        let presented = state.sheet
        return Binding(get: { self.state.sheet.map(CommunitySheetIdentity.init) }, set: {
            if $0 == nil, let presented { self.source.dismissSheet(sheet: presented) }
        })
    }
}

struct CommunitySheetSnapshot: Decodable {
    let active: Bool
    var kind = ""
    var title = ""
    var busy = false
    var submitLabel = ""
    var cancelLabel = ""
    var error: String?
    var editor: CommunityEditorSnapshot?
    var report: CommunityReportSnapshot?

    static func decode(_ json: String) -> Self {
        (try? JSONDecoder().decode(Self.self, from: Data(json.utf8))) ?? Self(active: false)
    }

    // Revocation deliberately contains only `active`; avoid retaining the previous draft.
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        active = try values.decode(Bool.self, forKey: .active)
        guard active else { return }
        kind = try values.decode(String.self, forKey: .kind)
        title = try values.decode(String.self, forKey: .title)
        busy = try values.decode(Bool.self, forKey: .busy)
        submitLabel = try values.decode(String.self, forKey: .submitLabel)
        cancelLabel = try values.decode(String.self, forKey: .cancelLabel)
        error = try values.decodeIfPresent(String.self, forKey: .error)
        editor = try values.decodeIfPresent(CommunityEditorSnapshot.self, forKey: .editor)
        report = try values.decodeIfPresent(CommunityReportSnapshot.self, forKey: .report)
    }

    init(active: Bool, kind: String = "", title: String = "", busy: Bool = false,
         submitLabel: String = "", cancelLabel: String = "", error: String? = nil,
         editor: CommunityEditorSnapshot? = nil, report: CommunityReportSnapshot? = nil) {
        self.active = active
        self.kind = kind
        self.title = title
        self.busy = busy
        self.submitLabel = submitLabel
        self.cancelLabel = cancelLabel
        self.error = error
        self.editor = editor
        self.report = report
    }

    private enum CodingKeys: String, CodingKey {
        case active, kind, title, busy, submitLabel, cancelLabel, error, editor, report
    }
}

struct CommunityEditorSnapshot: Decodable {
    let shopName: String
    let keptText: String
    let ratingsTitle: String
    let ratingsHint: String
    let ratings: [CommunityRatingSnapshot]
    let noteLabel: String
    let notePlaceholder: String
    let text: String
    let limit: Int
    let drink: CommunityDrinkSnapshot
}

struct CommunityRatingSnapshot: Decodable, Identifiable {
    var id: String { key }
    let key: String
    let label: String
    let value: Int
    let imageUri: String
    let descriptions: [String]
}

struct CommunityDrinkSnapshot: Decodable {
    let label: String
    let selected: String
    let custom: String?
    let customLabel: String
    let customLimit: Int
    let catalogError: String?
    let retryLabel: String
    let options: [CommunityDrinkOption]
}

struct CommunityDrinkOption: Decodable, Identifiable {
    var id: String { key }
    let key: String
    let label: String
}

struct CommunityReportSnapshot: Decodable {
    let text: String
    let placeholder: String
    let limit: Int
}

import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCommunitySheetController(sheet: IosCommunitySheet) -> UIViewController {
    CommunitySheetHostingController(sheet: sheet)
}

private final class CommunitySheetHostingController: UIHostingController<CommunitySheetHost> {
    private let model: CommunitySheetModel

    init(sheet: IosCommunitySheet) {
        let model = CommunitySheetModel(sheet: sheet)
        self.model = model
        super.init(rootView: CommunitySheetHost(model: model))
        // The outer sheet owns container insets; SwiftUI still avoids the keyboard.
        safeAreaRegions = .keyboard
        view.backgroundColor = .clear
    }

    @MainActor required dynamic init?(coder: NSCoder) { return nil }
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
                    .background {
                        GeometryReader { geometry in
                            Color.clear.preference(key: CommunitySheetHeight.self, value: geometry.size.height)
                        }
                    }
                }
                .scrollDismissesKeyboard(.interactively)
            } else { Color.clear }
        }
        .font(CoffeePeekNativeStyle.font(16))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .onPreferenceChange(CommunitySheetHeight.self, perform: model.heightChanged)
        .onAppear(perform: model.observe)
    }
}

private struct CommunitySheetHeight: PreferenceKey {
    static var defaultValue: CGFloat { 0 }
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = max(value, nextValue()) }
}

private final class CommunitySheetModel: ObservableObject {
    @Published private(set) var state: CommunitySheetSnapshot
    private let sheet: IosCommunitySheet
    private var observation: IosCommunitySheetObservation?

    init(sheet: IosCommunitySheet) {
        self.sheet = sheet
        state = CommunitySheetSnapshot.decode(sheet.current())
    }

    // Kotlin composition owns the sheet. Swift graph recreation cannot revoke its source.
    deinit { observation?.cancel() }

    func observe() {
        guard observation == nil else { return }
        state = CommunitySheetSnapshot.decode(sheet.current())
        observation = sheet.observe { [weak self] json in self?.state = CommunitySheetSnapshot.decode(json) }
    }

    func perform(_ action: String, _ value: String = "") { sheet.perform(action: action, value: value) }
    func heightChanged(_ value: CGFloat) { sheet.heightChanged(value: Double(value)) }
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

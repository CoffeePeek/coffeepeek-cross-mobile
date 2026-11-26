import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeContentReportController(report: IosContentReport, languageTag: String) -> UIViewController {
    ContentReportHostingController(report: report, languageTag: languageTag)
}

@MainActor
private final class ContentReportHostingController: UIHostingController<ContentReportHost> {
    private let report: IosContentReport

    init(report: IosContentReport, languageTag: String) {
        self.report = report
        super.init(rootView: ContentReportHost(report: report, strings: ContentReportStrings(languageTag: languageTag)))
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("Use init(report:languageTag:)") }

    isolated deinit { report.close() }
}

private struct ContentReportHost: View {
    let report: IosContentReport
    let strings: ContentReportStrings
    @Environment(\.scenePhase) private var scenePhase
    @State private var state: ContentReportViewState
    @State private var observation: IosContentReportObservation?

    init(report: IosContentReport, strings: ContentReportStrings) {
        self.report = report
        self.strings = strings
        _state = State(initialValue: ContentReportViewState(report.currentState()))
    }

    var body: some View {
        ContentReportScreen(
            state: state, strings: strings, changeText: report.changeText, submit: report.submit,
            retryAccess: report.retryAccess, back: report.back
        )
        .onAppear(perform: observe)
        .onDisappear {
            observation?.cancel()
            observation = nil
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active && observation != nil { report.resume() }
        }
    }

    private func observe() {
        guard observation == nil else { return }
        report.resume()
        state = ContentReportViewState(report.currentState())
        observation = report.observe { [binding = $state] snapshot in
            binding.wrappedValue = ContentReportViewState(snapshot)
        }
    }
}

private struct ContentReportViewState {
    var text = ""
    var preview = false
    var accessAvailable = true
    var canEdit = true
    var canSubmit = false
    var submitting = false
    var submitted = false
    var deliveryUnconfirmed = false
    var errorKey: String?
    var serverMessage: String?

    init(
        text: String = "", preview: Bool = false, accessAvailable: Bool = true,
        canEdit: Bool = true, canSubmit: Bool = false, submitting: Bool = false, submitted: Bool = false,
        deliveryUnconfirmed: Bool = false, errorKey: String? = nil, serverMessage: String? = nil
    ) {
        self.text = text
        self.preview = preview
        self.accessAvailable = accessAvailable
        self.canEdit = canEdit
        self.canSubmit = canSubmit
        self.submitting = submitting
        self.submitted = submitted
        self.deliveryUnconfirmed = deliveryUnconfirmed
        self.errorKey = errorKey
        self.serverMessage = serverMessage
    }

    init(_ state: IosContentReportState) {
        self.init(text: state.text, preview: state.preview, accessAvailable: state.accessAvailable,
                  canEdit: state.canEdit, canSubmit: state.canSubmit, submitting: state.submitting,
                  submitted: state.submitted, deliveryUnconfirmed: state.deliveryUnconfirmed,
                  errorKey: state.errorKey, serverMessage: state.serverMessage)
    }
}

private struct ContentReportScreen: View {
    let state: ContentReportViewState
    let strings: ContentReportStrings
    let changeText: (String) -> Void
    let submit: () -> Void
    let retryAccess: () -> Void
    let back: () -> Void
    @FocusState private var editorFocused: Bool

    var body: some View {
        Form {
            if !state.accessAvailable {
                Section {
                    Text(strings.text("content_report_access_failed"))
                    Button(strings.text("content_report_retry"), action: retryAccess)
                }
            } else if state.deliveryUnconfirmed {
                Section {
                    Text(strings.text("content_report_unconfirmed")).foregroundStyle(.secondary)
                    Button(strings.text("content_report_back"), action: close)
                }
            } else if state.submitted {
                confirmation
            } else {
                form
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .safeAreaInset(edge: .top, spacing: 0) { toolbar }
    }

    private var toolbar: some View {
        CoffeePeekNativeNavigationHeader(title: strings.text("content_report_title"),
            backLabel: strings.text("content_report_back"), back: close)
    }

    private var form: some View {
        Section {
            if state.preview {
                Text(strings.text("content_report_preview_hint"))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            editor
            if let error = state.serverMessage ?? state.errorKey.map(strings.text) {
                Text(error).foregroundStyle(.red)
            }
            if state.submitting {
                CoffeePeekNativeLoader(label: strings.text("content_report_loading"), size: 32, showsLabel: true)
            } else {
                Button(strings.text(state.preview ? "content_report_preview_submit" : "content_report_submit")) {
                    editorFocused = false
                    submit()
                }
                .disabled(!state.canSubmit)
            }
        } header: {
            Text(strings.text("content_report_prompt"))
        }
    }

    private var editor: some View {
        ZStack(alignment: .topLeading) {
            TextEditor(text: Binding(get: { state.text }, set: { changeText($0) }))
                .frame(minHeight: 110)
                .focused($editorFocused)
                .disabled(!state.canEdit)
                .accessibilityLabel(strings.text("content_report_placeholder"))
            if state.text.isEmpty {
                Text(strings.text("content_report_placeholder"))
                    .foregroundStyle(.secondary)
                    .padding(.top, 8)
                    .padding(.leading, 5)
                    .allowsHitTesting(false)
                    .accessibilityHidden(true)
            }
        }
    }

    private var confirmation: some View {
        Section {
            Text(strings.text(state.preview ? "content_report_preview_title" : "content_report_success_title"))
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            Text(strings.text(state.preview ? "content_report_preview_success" : "content_report_success"))
                .foregroundStyle(.secondary)
            Button(strings.text("content_report_done"), action: close)
        }
    }

    private func close() {
        editorFocused = false
        back()
    }
}

private struct ContentReportStrings {
    private let bundle: Bundle

    init(languageTag: String) {
        let localization = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
            ?? Bundle.main.url(forResource: "ru", withExtension: "lproj")
        bundle = localization.flatMap { Bundle(url: $0) } ?? .main
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "ContentReport")
    }
}

#Preview("Content report light") {
    ContentReportScreen(
        state: ContentReportViewState(text: "Пример причины жалобы", canSubmit: true),
        strings: ContentReportStrings(languageTag: "ru"), changeText: { _ in }, submit: {}, retryAccess: {}, back: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Content report dark") {
    ContentReportScreen(
        state: ContentReportViewState(preview: true, canEdit: false, submitted: true),
        strings: ContentReportStrings(languageTag: "be"), changeText: { _ in }, submit: {}, retryAccess: {}, back: {}
    )
    .preferredColorScheme(.dark)
}

#Preview("Content report delivery unconfirmed") {
    ContentReportScreen(
        state: ContentReportViewState(canEdit: false, deliveryUnconfirmed: true),
        strings: ContentReportStrings(languageTag: "ru"), changeText: { _ in }, submit: {}, retryAccess: {}, back: {}
    )
}

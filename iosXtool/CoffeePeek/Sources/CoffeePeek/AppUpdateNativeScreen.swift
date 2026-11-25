import ComposeApp
import SwiftUI

struct AppUpdateNativeHost: View {
    let source: IosAppUpdate
    @StateObject private var model: AppUpdateNativeModel

    init(source: IosAppUpdate) {
        self.source = source
        _model = StateObject(wrappedValue: AppUpdateNativeModel(source))
    }

    var body: some View {
        AppUpdateNativeScreen(required: source.required, error: model.error, strings: model.strings,
            update: source.openStore, dismiss: source.dismiss)
            .onAppear(perform: model.start).onDisappear(perform: model.stop)
    }
}

@MainActor
private final class AppUpdateNativeModel: ObservableObject {
    @Published var error: String?
    @Published var strings: OwnProfileStrings
    private let source: IosAppUpdate
    private var observations: [IosRootModalObservation] = []

    init(_ source: IosAppUpdate) {
        self.source = source
        error = source.currentError()
        strings = OwnProfileStrings(languageTag: source.languageTag(), table: "NativeAppUpdate")
    }

    func start() {
        guard observations.isEmpty else { return }
        observations = [source.observeError { [weak self] in self?.error = $0 },
            source.observeLanguage { [weak self] tag in
                self?.strings = OwnProfileStrings(languageTag: tag, table: "NativeAppUpdate")
            }]
    }
    func stop() { observations.forEach { $0.cancel() }; observations = [] }
    isolated deinit { observations.forEach { $0.cancel() } }
}

struct AppUpdateNativeScreen: View {
    let required: Bool
    let error: String?
    let strings: OwnProfileStrings
    let update: () -> Void
    let dismiss: () -> Void

    var body: some View {
        ZStack(alignment: .top) {
            Color.black.opacity(0.3).ignoresSafeArea().onTapGesture { if !required { dismiss() } }
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Label(strings[required ? "appupdate_title_required" : "appupdate_title_available"], systemImage: "arrow.down.app")
                        .font(CoffeePeekNativeStyle.font(20, weight: "Semibold", relativeTo: .headline))
                        .accessibilityAddTraits(.isHeader)
                    Text(strings[required ? "appupdate_body_required" : "appupdate_body_available"])
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                    if let error { Text(error).foregroundStyle(.red) }
                    CoffeePeekNativeActionButton(title: strings[error == nil ? "appupdate_app_store" : "appupdate_retry"], action: update)
                    if !required { CoffeePeekNativeActionButton(title: strings["appupdate_later"], primary: false, action: dismiss) }
                }
                .padding(20).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 24))
                .overlay(RoundedRectangle(cornerRadius: 24).strokeBorder(CoffeePeekNativeStyle.border, lineWidth: 1))
                .padding(.horizontal, 16).padding(.top, 24).frame(maxWidth: 560).frame(maxWidth: .infinity)
            }.scrollBounceBehavior(.basedOnSize)
        }
        .font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .accessibilityAction(.escape) { if !required { dismiss() } }
        .simultaneousGesture(DragGesture().onEnded { if !required && $0.translation.height < -64 { dismiss() } })
    }
}

#Preview("Optional update light") {
    AppUpdateNativeScreen(required: false, error: nil, strings: OwnProfileStrings(languageTag: "ru", table: "NativeAppUpdate"),
        update: {}, dismiss: {}).preferredColorScheme(.light)
}
#Preview("Required update dark") {
    AppUpdateNativeScreen(required: true, error: nil, strings: OwnProfileStrings(languageTag: "be", table: "NativeAppUpdate"),
        update: {}, dismiss: {}).preferredColorScheme(.dark)
}

import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeSettingsController(screen: IosSettingsScreen) -> UIViewController {
    switch screen {
    case let model as IosSettingsTab:
        return hostSettingsTab(model)
    case let model as IosCitySettings:
        return SettingsHostingController(screen: model, rootView: SettingsStateHost(
            screen: model, current: model.currentState, observe: model.observeState
        ) { state, strings in
            CitySettingsScreen(state: state, strings: strings, select: model.select, retry: model.retry, back: model.back)
        })
    case let model as IosLanguageSettings:
        return SettingsHostingController(screen: model, rootView: SettingsStateHost(
            screen: model, current: model.currentState, observe: model.observeState
        ) { state, strings in
            LanguageSettingsScreen(state: state, strings: strings, select: model.select, retry: model.retry, back: model.back)
        })
    case let model as IosUsagePolicy:
        return SettingsHostingController(screen: model, rootView: SettingsStateHost(
            screen: model, current: model.currentState, observe: model.observeState
        ) { state, strings in
            UsagePolicyScreen(
                state: state, strings: strings, setConsent: model.setConsent,
                openTerms: model.openTerms, openPrivacy: model.openPrivacy, back: model.back
            )
        })
    default:
        preconditionFailure("Unsupported iOS settings destination")
    }
}

@MainActor
private func hostSettingsTab(_ model: IosSettingsTab) -> UIViewController {
    SettingsHostingController(screen: model, rootView: SettingsStateHost(
        screen: model,
        current: model.currentState,
        observe: model.observeState,
        start: {
            model.screenShown()
            return [model.observeNotificationRefresh(), model.observeUpdateMessage { key in
                model.showUpdateMessage(message: SettingsNativeStrings(languageTag: model.languageTag()).text(key))
            }]
        }
    ) { state, strings in
        SettingsNativeTab(
            state: state, strings: strings, open: model.open, checkForUpdates: model.checkForUpdates,
            setPushEnabled: model.setPushEnabled, refreshNotifications: model.refreshNotifications,
            switchAccount: model.switchAccount
        )
    })
}

private final class SettingsHostingController<Content: View>: UIHostingController<Content> {
    private let screen: IosSettingsScreen

    init(screen: IosSettingsScreen, rootView: Content) {
        self.screen = screen
        super.init(rootView: rootView)
    }

    @MainActor required dynamic init?(coder: NSCoder) {
        return nil
    }

    deinit {
        screen.close()
    }
}

private struct SettingsStateHost<Snapshot, Content: View>: View {
    let screen: IosSettingsScreen
    let current: () -> Snapshot
    let observe: (@escaping (Snapshot) -> Void) -> IosSettingsObservation
    let start: () -> [IosSettingsObservation]
    let content: (Snapshot, SettingsNativeStrings) -> Content

    @State private var snapshot: Snapshot
    @State private var languageTag: String
    @State private var observations: [IosSettingsObservation] = []

    init(
        screen: IosSettingsScreen,
        current: @escaping () -> Snapshot,
        observe: @escaping (@escaping (Snapshot) -> Void) -> IosSettingsObservation,
        start: @escaping () -> [IosSettingsObservation] = { [] },
        @ViewBuilder content: @escaping (Snapshot, SettingsNativeStrings) -> Content
    ) {
        self.screen = screen
        self.current = current
        self.observe = observe
        self.start = start
        self.content = content
        _snapshot = State(initialValue: current())
        _languageTag = State(initialValue: screen.languageTag())
    }

    var body: some View {
        content(snapshot, SettingsNativeStrings(languageTag: languageTag))
            .onAppear(perform: beginObservation)
            .onDisappear {
                observations.forEach { $0.cancel() }
                observations = []
            }
    }

    private func beginObservation() {
        guard observations.isEmpty else { return }
        snapshot = current()
        languageTag = screen.languageTag()
        observations = [
            observe { [snapshot = $snapshot] state in snapshot.wrappedValue = state },
            screen.observeLanguage { [language = $languageTag] tag in language.wrappedValue = tag }
        ] + start()
    }
}

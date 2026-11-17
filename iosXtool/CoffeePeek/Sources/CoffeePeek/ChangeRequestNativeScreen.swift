import ComposeApp
import SwiftUI

struct ChangeRequestHost: View {
    let route: IosChangeRequest
    let strings: ContributionsStrings
    @State private var state: IosChangeRequestState?
    @State private var observation: IosContributionObservation?

    init(route: IosChangeRequest, strings: ContributionsStrings) {
        self.route = route
        self.strings = strings
    }

    var body: some View {
        ChangeRequestNativeScreen(
            state: state, strings: strings, back: route.back, signIn: route.signIn,
            refresh: route.refresh, openShop: route.openShop
        )
        .onAppear(perform: observe)
        .onDisappear(perform: stop)
    }

    private func observe() {
        guard observation == nil else { return }
        observation = route.observe { [binding = $state] snapshot in binding.wrappedValue = snapshot }
    }

    private func stop() {
        observation?.cancel()
        observation = nil
        state = nil
    }
}

private struct ChangeRequestNativeScreen: View {
    let state: IosChangeRequestState?
    let strings: ContributionsStrings
    let back: () -> Void
    let signIn: () -> Void
    let refresh: () -> Void
    let openShop: () -> Void

    var body: some View {
        ContributionsScaffold(title: strings.text("request_title"), backLabel: strings.text("back"), onBack: back) {
            if let state, !state.loading {
                if let section = state.section {
                    details(state, section: section)
                } else {
                    let error = state.error ?? "load_failed"
                    let needsSignIn = error == "sign_in_prompt"
                    ContributionsMessage(
                        text: needsSignIn ? strings.text(error) : (state.serverMessage ?? strings.text(error)),
                        actionTitle: strings.text(needsSignIn ? "sign_in" : "retry"),
                        onAction: needsSignIn ? signIn : refresh
                    )
                }
            } else {
                ProgressView(strings.text("loading"))
            }
        }
    }

    private func details(_ state: IosChangeRequestState, section: String) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header(state, section: section)
                if let reason = state.reason, !reason.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(strings.text("request_rejection")).font(.headline)
                        Text(reason)
                    }
                    .contributionsCard()
                }
                fields(state)
                Button(action: openShop) {
                    Label(strings.text("request_open_shop"), systemImage: "cup.and.saucer")
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .buttonStyle(.bordered)
                .tint(CoffeePeekNativeStyle.foreground)
                .disabled(!state.canOpenShop)
            }
            .padding(16)
        }
    }

    private func header(_ state: IosChangeRequestState, section: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings.text("section_\(section)")).font(.title2.bold())
            Text(strings.text("hint_\(section)")).foregroundStyle(CoffeePeekNativeStyle.secondary)
            if let status = state.status { ContributionStatusLabel(status: status, strings: strings) }
            if let date = state.date { Label(date, systemImage: "calendar").font(.subheadline) }
        }
        .contributionsCard()
    }

    private func fields(_ state: IosChangeRequestState) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(strings.text("request_payload")).font(.headline)
            if state.fields.isEmpty {
                Text(strings.text("request_empty"))
            }
            ForEach(state.fields.indices, id: \.self) { index in
                let field = state.fields[index]
                Divider()
                if let label = field.label ?? field.labelKey.map(strings.text) { Text(label).font(.subheadline.bold()) }
                if let value = field.value ?? field.valueKey.map(strings.text) { Text(value) }
                if let price = field.price { Text(strings.format("request_price", price)).font(.subheadline) }
            }
            if let note = state.note {
                Text(strings.text(note)).font(.footnote).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
        }
        .contributionsCard()
    }
}

private func changeRequestPreview() -> some View {
    ChangeRequestNativeScreen(
        state: IosChangeRequestState(
            loading: false, error: nil, serverMessage: nil, section: "description", status: "pending",
            reason: nil, date: "21.10.2026 10:00",
            fields: [IosContributionField(labelKey: nil, label: nil, valueKey: "request_description_removed",
                                          value: nil, price: nil)],
            note: nil, canOpenShop: true
        ),
        strings: ContributionsStrings(languageTag: "ru"), back: {}, signIn: {}, refresh: {}, openShop: {}
    )
}

#Preview("Request light") { changeRequestPreview().preferredColorScheme(.light) }
#Preview("Request dark") { changeRequestPreview().preferredColorScheme(.dark) }

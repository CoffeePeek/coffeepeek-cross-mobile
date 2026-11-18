import ComposeApp
import SwiftUI
import UIKit

struct GuestOnboardingNativeHost: View {
    let source: IosGuestOnboarding
    @StateObject private var model: GuestOnboardingNativeModel

    init(source: IosGuestOnboarding) {
        self.source = source
        _model = StateObject(wrappedValue: GuestOnboardingNativeModel(source))
    }

    var body: some View {
        GuestOnboardingNativeScreen(posters: model.posters, strings: model.strings,
            guest: source.continueAsGuest, signIn: source.signIn, register: source.register)
            .onAppear(perform: model.start).onDisappear(perform: model.stop)
    }
}

private final class GuestOnboardingNativeModel: ObservableObject {
    @Published var posters: [Int: UIImage] = [:]
    @Published var strings: OwnProfileStrings
    private let source: IosGuestOnboarding
    private var observations: [IosRootModalObservation] = []

    init(_ source: IosGuestOnboarding) {
        self.source = source
        strings = OwnProfileStrings(languageTag: source.languageTag(), table: "NativeGuestOnboarding")
    }

    func start() {
        guard observations.isEmpty else { return }
        observations = [source.observeLanguage { [weak self] tag in
            self?.strings = OwnProfileStrings(languageTag: tag, table: "NativeGuestOnboarding")
        }]
        for index in 0..<5 {
            observations.append(source.loadPoster(index: Int32(index)) { [weak self] in self?.posters[index] = $0 })
        }
    }

    func stop() { observations.forEach { $0.cancel() }; observations = [] }
    deinit { observations.forEach { $0.cancel() } }
}

struct GuestOnboardingNativeScreen: View {
    let posters: [Int: UIImage]
    let strings: OwnProfileStrings
    let guest: () -> Void
    let signIn: () -> Void
    let register: () -> Void
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.accessibilityVoiceOverEnabled) private var voiceOver
    @State private var selected = 0
    @State private var held = false
    private let pages = ["cover", "discovery", "atmosphere", "check_ins", "favorites"]

    var body: some View {
        VStack(spacing: 16) {
            TabView(selection: $selected) {
                ForEach(pages.indices, id: \.self) { index in slide(index).tag(index) }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .simultaneousGesture(DragGesture(minimumDistance: 0).onChanged { _ in held = true }.onEnded { _ in held = false })
            .overlay(alignment: .topTrailing) { close }
            indicators
            VStack(spacing: 12) {
                CoffeePeekNativeActionButton(title: strings["home_check_in_onboarding_register"], action: register)
                CoffeePeekNativeActionButton(title: strings["home_check_in_onboarding_sign_in"], primary: false, action: signIn)
            }.padding(.horizontal, 24).padding(.bottom, 16).frame(maxWidth: 480)
        }
        .font(CoffeePeekNativeStyle.font(16))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .accessibilityAction(.escape, guest)
        .task(id: "\(selected)-\(held)-\(scenePhase == .active)-\(voiceOver)") { await advance() }
    }

    private var close: some View {
        Button(action: guest) { Image(systemName: "xmark").frame(width: 48, height: 48) }
            .buttonStyle(.plain).accessibilityLabel(strings["home_check_in_onboarding_guest"])
    }

    private var indicators: some View {
        HStack(spacing: 8) {
            ForEach(pages.indices, id: \.self) { index in
                Capsule().fill(index == selected ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.secondary.opacity(0.25))
                    .frame(width: index == selected ? 24 : 8, height: 8)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(String(format: strings["home_check_in_onboarding_step"], selected + 1, pages.count))
        .accessibilityAdjustableAction { direction in
            selected = (selected + (direction == .increment ? 1 : pages.count - 1)) % pages.count
        }
    }

    private func slide(_ index: Int) -> some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 12) {
                    Group {
                        if let poster = posters[index] { Image(uiImage: poster).resizable().scaledToFit() }
                        else { CoffeePeekPhotoPlaceholder() }
                    }.frame(height: geometry.size.height * 0.72).clipShape(RoundedRectangle(cornerRadius: 24))
                    Text(strings["home_check_in_onboarding_\(pages[index])_title"])
                        .font(CoffeePeekNativeStyle.font(24, weight: "Bold", relativeTo: .title2))
                        .accessibilityAddTraits(.isHeader)
                    Text(strings["home_check_in_onboarding_poster_\(pages[index])"])
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                }.padding(.horizontal, 24).padding(.vertical, 8).frame(maxWidth: 480).frame(maxWidth: .infinity)
            }
        }.multilineTextAlignment(.center)
    }

    private func advance() async {
        guard scenePhase == .active, !held, !voiceOver else { return }
        do { try await Task.sleep(for: .seconds(2)) } catch { return }
        guard !Task.isCancelled else { return }
        withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.42)) { selected = (selected + 1) % pages.count }
    }
}

#Preview("Guest onboarding light") {
    GuestOnboardingNativeScreen(posters: [:], strings: OwnProfileStrings(languageTag: "ru", table: "NativeGuestOnboarding"),
        guest: {}, signIn: {}, register: {}).preferredColorScheme(.light)
}
#Preview("Guest onboarding dark") {
    GuestOnboardingNativeScreen(posters: [:], strings: OwnProfileStrings(languageTag: "be", table: "NativeGuestOnboarding"),
        guest: {}, signIn: {}, register: {}).preferredColorScheme(.dark)
}

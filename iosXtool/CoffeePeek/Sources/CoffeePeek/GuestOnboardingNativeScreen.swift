import ComposeApp
import SwiftUI
import UIKit

struct GuestOnboardingNativeHost: View {
    let source: IosGuestOnboarding
    let finish: (@escaping () -> Void) -> Void
    @StateObject private var model: GuestOnboardingNativeModel

    init(source: IosGuestOnboarding, finish: @escaping (@escaping () -> Void) -> Void) {
        self.source = source
        self.finish = finish
        _model = StateObject(wrappedValue: GuestOnboardingNativeModel(source))
    }

    var body: some View {
        GuestOnboardingNativeScreen(posters: model.posters, strings: model.strings,
            guest: { finish(source.continueAsGuest) }, signIn: { finish(source.signIn) },
            register: { finish(source.register) })
            .onAppear(perform: model.start).onDisappear(perform: model.stop)
    }
}

@MainActor
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
    isolated deinit { observations.forEach { $0.cancel() } }
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
    @GestureState private var held = false
    private let pages = ["cover", "discovery", "atmosphere", "check_ins", "favorites"]

    var body: some View {
        VStack(spacing: 16) {
            TabView(selection: $selected) {
                ForEach(pages.indices, id: \.self) { index in slide(index).tag(index) }
            }
            .frame(maxWidth: 480)
            .tabViewStyle(.page(indexDisplayMode: .never))
            .simultaneousGesture(DragGesture(minimumDistance: 0).updating($held) { _, state, _ in state = true })
            .overlay(alignment: .topTrailing) { close.padding(.top, 16).padding(.trailing, 24) }
            indicators
            VStack(spacing: 12) {
                CoffeePeekNativeActionButton(title: strings["home_check_in_onboarding_register"], action: register)
                CoffeePeekNativeActionButton(title: strings["home_check_in_onboarding_sign_in"], primary: false, action: signIn)
            }.padding(.horizontal, 24).padding(.bottom, 16).frame(maxWidth: 480)
        }
        .frame(maxWidth: .infinity)
        .font(CoffeePeekNativeStyle.font(16))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .accessibilityAction(.escape, guest)
        .task(id: "\(selected)-\(held)-\(scenePhase == .active)-\(voiceOver)") { await advance() }
    }

    private var close: some View {
        Button(action: guest) { Image(systemName: "xmark").frame(width: 48, height: 48) }
            .buttonStyle(.plain).shopDetailGlassCircle()
            .accessibilityLabel(strings["home_check_in_onboarding_guest"])
    }

    private var indicators: some View {
        GuestOnboardingPageControl(count: pages.count,
            selected: Binding(get: { selected }, set: { selectPage($0) }),
            label: String(format: strings["home_check_in_onboarding_step"], selected + 1, pages.count))
            .frame(height: 44).fixedSize(horizontal: true, vertical: false)
    }

    private func selectPage(_ index: Int) {
        withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.42)) { selected = index }
    }

    private func slide(_ index: Int) -> some View {
        GuestOnboardingSlide(poster: posters[index],
            title: strings["home_check_in_onboarding_\(pages[index])_title"],
            caption: strings["home_check_in_onboarding_poster_\(pages[index])"])
    }

    private func advance() async {
        guard scenePhase == .active, !held, !voiceOver else { return }
        do { try await Task.sleep(for: .seconds(3)) } catch { return }
        guard !Task.isCancelled else { return }
        selectPage((selected + 1) % pages.count)
    }
}

private struct GuestOnboardingPageControl: UIViewRepresentable {
    let count: Int
    @Binding var selected: Int
    let label: String

    func makeUIView(context: Context) -> UIPageControl {
        let control = UIPageControl()
        control.currentPageIndicatorTintColor = UIColor(CoffeePeekNativeStyle.accent)
        control.pageIndicatorTintColor = UIColor(CoffeePeekNativeStyle.secondary.opacity(0.25))
        control.addTarget(context.coordinator, action: #selector(Coordinator.changed(_:)), for: .valueChanged)
        return control
    }

    func updateUIView(_ control: UIPageControl, context: Context) {
        context.coordinator.selection = $selected
        control.numberOfPages = count
        control.currentPage = selected
        control.accessibilityLabel = label
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UIPageControl, context: Context) -> CGSize? {
        CGSize(width: uiView.size(forNumberOfPages: count).width, height: 44)
    }

    func makeCoordinator() -> Coordinator { Coordinator(selection: $selected) }

    @MainActor
    final class Coordinator: NSObject {
        var selection: Binding<Int>
        init(selection: Binding<Int>) { self.selection = selection }
        @objc func changed(_ control: UIPageControl) { selection.wrappedValue = control.currentPage }
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
#Preview("Guest onboarding compact") {
    GuestOnboardingNativeScreen(posters: [:], strings: OwnProfileStrings(languageTag: "ru", table: "NativeGuestOnboarding"),
        guest: {}, signIn: {}, register: {}).frame(width: 320, height: 568).preferredColorScheme(.light)
}
#Preview("Guest onboarding large text") {
    GuestOnboardingNativeScreen(posters: [:], strings: OwnProfileStrings(languageTag: "be", table: "NativeGuestOnboarding"),
        guest: {}, signIn: {}, register: {}).dynamicTypeSize(.accessibility1).preferredColorScheme(.dark)
}

/// Reserve the complete caption before fitting the artwork. Large text can still scroll.
struct GuestOnboardingSlide: View {
    let poster: UIImage?
    let title: String
    let caption: String

    var body: some View {
        GeometryReader { viewport in
            ScrollView {
                GuestOnboardingSlideLayout(viewportHeight: max(0, viewport.size.height - 16), aspect: aspect) {
                    Group {
                        if let poster { Image(uiImage: poster).resizable().scaledToFit() }
                        else { CoffeePeekPhotoPlaceholder() }
                    }.clipShape(RoundedRectangle(cornerRadius: 24))
                    captions
                }
                .padding(.horizontal, 16).padding(.vertical, 8)
            }.scrollIndicators(.hidden)
        }.multilineTextAlignment(.center)
    }

    private var aspect: CGFloat {
        guard let poster, poster.size.width > 0, poster.size.height > 0 else { return 1200.0 / 1518.0 }
        return poster.size.width / poster.size.height
    }

    private var captions: some View {
        VStack(spacing: 12) {
            Text(title).font(CoffeePeekNativeStyle.font(24, weight: "Bold", relativeTo: .title2))
                .accessibilityAddTraits(.isHeader)
            Text(caption).font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.secondary)
        }.fixedSize(horizontal: false, vertical: true)
    }
}

private struct GuestOnboardingSlideLayout: Layout {
    let viewportHeight: CGFloat
    let aspect: CGFloat

    private func dimensions(width: CGFloat, subviews: Subviews) -> (poster: CGFloat, caption: CGSize) {
        let caption = subviews[1].sizeThatFits(ProposedViewSize(width: width, height: nil))
        let poster = min(width / aspect, max(120, viewportHeight - caption.height - 12))
        return (poster, caption)
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = max(0, proposal.width ?? 320)
        let size = dimensions(width: width, subviews: subviews)
        return CGSize(width: width, height: size.poster + 12 + size.caption.height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let size = dimensions(width: bounds.width, subviews: subviews)
        subviews[0].place(at: CGPoint(x: bounds.midX, y: bounds.minY), anchor: .top,
            proposal: ProposedViewSize(width: bounds.width, height: size.poster))
        subviews[1].place(at: CGPoint(x: bounds.minX, y: bounds.minY + size.poster + 12), anchor: .topLeading,
            proposal: ProposedViewSize(width: bounds.width, height: size.caption.height))
    }
}

@MainActor
private func guestSlidePreview() -> some View {
    GuestOnboardingSlide(poster: nil, title: "Сохраняй любимое",
        caption: "Собирай подборки кофеен и добавляй зерно на кофейную полку")
        .frame(width: 320, height: 400).background(CoffeePeekNativeStyle.background)
}

#Preview("Onboarding caption light") { guestSlidePreview().preferredColorScheme(.light) }
#Preview("Onboarding caption dark") { guestSlidePreview().preferredColorScheme(.dark) }
#Preview("Onboarding caption large text") { guestSlidePreview().dynamicTypeSize(.accessibility2).preferredColorScheme(.dark) }

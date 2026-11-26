import ComposeApp
import SwiftUI
import UIKit

struct CoffeePeekNativeLoader: View {
    var label: String = ""
    var animated = true
    var color: Color = CoffeePeekNativeStyle.accent
    var size: CGFloat = CGFloat(IosLoaderGeometry.shared.size)
    var showsLabel = false
    @Environment(\.accessibilityReduceMotion) private var reducedMotion
    @Environment(\.scenePhase) private var scenePhase
    @State private var started = Date()
    private let geometry = IosLoaderGeometry.shared
    private let contours = IosLoaderGeometry.shared.contours.map { shape in
        shape.map { CGPoint(x: CGFloat($0.x), y: CGFloat($0.y)) }
    }
    private var stroke: CGFloat { max(size / 16, 2) }

    var body: some View {
        VStack(spacing: 8) {
            contour
            if showsLabel && !label.isEmpty { Text(label).multilineTextAlignment(.center) }
        }
        .accessibilityElement(children: .ignore).accessibilityLabel(label)
        .accessibilityAddTraits(.updatesFrequently)
    }

    private var contour: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30, paused: !animated || reducedMotion || scenePhase != .active)) { context in
            let elapsed = max(0, context.date.timeIntervalSince(started))
            let moving = animated && !reducedMotion
            let position = moving ? elapsed.truncatingRemainder(dividingBy: geometry.morphSeconds) / geometry.morphSeconds * Double(contours.count) : 0
            let morph = floor(position) + eased(position - floor(position))
            let fill = moving ? elapsed.truncatingRemainder(dividingBy: geometry.fillSeconds) / geometry.fillSeconds * Double(geometry.maxFill) : 0.5
            let shape = CoffeePeekLoaderContour(contours: contours, morph: morph, stroke: stroke)
            ZStack {
                shape.stroke(color.opacity(0.18), style: StrokeStyle(lineWidth: stroke, lineCap: .round, lineJoin: .round))
                if fill > 0.01 {
                    shape.trim(from: 0, to: CGFloat(fill)).stroke(color,
                        style: StrokeStyle(lineWidth: stroke, lineCap: .butt, lineJoin: .round))
                }
            }.rotationEffect(.degrees(moving ? elapsed / geometry.rotationSeconds * 360 : 0))
        }
        .frame(width: size, height: size)
        .clipped()
    }

    private func eased(_ value: Double) -> Double {
        var low = 0.0
        var high = 1.0
        for _ in 0..<16 {
            let time = (low + high) / 2
            let x = 1.2 * time - 1.8 * time * time + 1.6 * time * time * time
            if x < value { low = time } else { high = time }
        }
        let time = (low + high) / 2
        return 3 * time * time - 2 * time * time * time
    }
}

private struct CoffeePeekLoaderContour: Shape {
    let contours: [[CGPoint]]
    let morph: Double
    let stroke: CGFloat

    func path(in rect: CGRect) -> Path {
        let points = points(in: rect)
        guard let first = points.first else { return Path() }
        var path = Path()
        path.move(to: first)
        for index in points.indices {
            let point = points[index]
            let previous = points[(index + points.count - 1) % points.count]
            let next = points[(index + 1) % points.count]
            let after = points[(index + 2) % points.count]
            let control1 = CGPoint(x: point.x + (next.x - previous.x) / 6, y: point.y + (next.y - previous.y) / 6)
            let control2 = CGPoint(x: next.x - (after.x - point.x) / 6, y: next.y - (after.y - point.y) / 6)
            path.addCurve(to: next, control1: control1, control2: control2)
        }
        path.closeSubpath()
        return path
    }

    private func points(in rect: CGRect) -> [CGPoint] {
        guard !contours.isEmpty else { return [] }
        let position = morph.truncatingRemainder(dividingBy: Double(contours.count))
        let index = Int(position)
        let from = contours[index]
        let to = contours[(index + 1) % contours.count]
        guard from.count == to.count else { return [] }
        let amount = CGFloat(position - Double(index))
        let radius = max(0, (min(rect.width, rect.height) - stroke) / 2)
        return from.indices.map { index in
            CGPoint(x: rect.midX + (from[index].x + (to[index].x - from[index].x) * amount) * radius,
                    y: rect.midY + (from[index].y + (to[index].y - from[index].y) * amount) * radius)
        }
    }
}

#Preview("Native loader light") { CoffeePeekNativeLoader(label: "Загрузка", animated: false).padding().preferredColorScheme(.light) }
#Preview("Native loader dark") { CoffeePeekNativeLoader(label: "Загрузка", animated: false).padding().preferredColorScheme(.dark) }
#Preview("Inline loader light") { CoffeePeekNativeLoader(label: "Загрузка", animated: false, color: .black, size: 20, showsLabel: true).padding().preferredColorScheme(.light) }
#Preview("Inline loader dark") { CoffeePeekNativeLoader(label: "Загрузка", animated: false, size: 20, showsLabel: true).padding().preferredColorScheme(.dark) }

@MainActor
extension View {
    func coffeePeekRefreshable(isRefreshing: Bool, label: String,
                              refresh: @escaping @MainActor () async -> Void) -> some View {
        modifier(CoffeePeekNativeRefreshModifier(isRefreshing: isRefreshing, label: label, refresh: refresh))
    }
}

@MainActor
private struct CoffeePeekNativeRefreshModifier: ViewModifier {
    let isRefreshing: Bool
    let label: String
    let refresh: @MainActor () async -> Void
    @State private var request: UUID?

    func body(content: Content) -> some View {
        content.background {
            CoffeePeekNativeRefreshBridge(refreshing: isRefreshing || request != nil, label: label) {
                if !isRefreshing && request == nil { request = UUID() }
            }
        }
        .task(id: request) {
            guard let current = request else { return }
            await refresh()
            if !Task.isCancelled && request == current { request = nil }
        }
        .onDisappear { request = nil }
    }
}

private struct CoffeePeekNativeRefreshBridge: UIViewControllerRepresentable {
    let refreshing: Bool
    let label: String
    let onPull: @MainActor () -> Void
    func makeUIViewController(context: Context) -> CoffeePeekNativeRefreshAttachment { CoffeePeekNativeRefreshAttachment() }
    func updateUIViewController(_ controller: CoffeePeekNativeRefreshAttachment, context: Context) {
        controller.update(refreshing: refreshing, label: label, onPull: onPull)
    }
    static func dismantleUIViewController(_ controller: CoffeePeekNativeRefreshAttachment, coordinator: ()) { controller.detach() }
}

/// Uses public UIKit scroll/refresh controls while SwiftUI retains its scroll identity and preferences.
@MainActor
private final class CoffeePeekNativeRefreshAttachment: UIViewController {
    private let control = UIRefreshControl()
    private let loader = UIHostingController(rootView: CoffeePeekNativeLoader(animated: false, size: 28))
    private weak var scroll: UIScrollView?
    private var onPull: @MainActor () -> Void = {}
    private var label = ""
    private var refreshing = false

    override func loadView() {
        view = UIView()
        view.isUserInteractionEnabled = false
        control.tintColor = .clear
        control.addTarget(self, action: #selector(pulled), for: .valueChanged)
        loader.view.backgroundColor = .clear
        loader.view.isUserInteractionEnabled = false
    }
    override func viewDidLayoutSubviews() { super.viewDidLayoutSubviews(); attach() }
    override func viewDidAppear(_ animated: Bool) { super.viewDidAppear(animated); attach() }
    override func willMove(toParent parent: UIViewController?) {
        if parent == nil { detach() }
        super.willMove(toParent: parent)
    }
    isolated deinit { detach() }

    func update(refreshing: Bool, label: String, onPull: @escaping @MainActor () -> Void) {
        self.label = label
        self.refreshing = refreshing
        self.onPull = onPull
        attach()
        updateRefreshing()
    }
    private func attach() {
        guard isViewLoaded, view.bounds.height > 0, let parent, let target = findScroll() else { return }
        if scroll === target && target.refreshControl === control { return }
        unbind()
        guard target.refreshControl == nil else { return }
        scroll = target
        parent.addChild(loader)
        loader.view.translatesAutoresizingMaskIntoConstraints = false
        control.addSubview(loader.view)
        NSLayoutConstraint.activate([
            loader.view.centerXAnchor.constraint(equalTo: control.centerXAnchor),
            loader.view.centerYAnchor.constraint(equalTo: control.centerYAnchor),
            loader.view.widthAnchor.constraint(equalToConstant: 28),
            loader.view.heightAnchor.constraint(equalToConstant: 28),
        ])
        loader.didMove(toParent: parent)
        target.refreshControl = control
        updateRefreshing()
    }
    func detach() {
        unbind()
        onPull = {}
    }
    private func unbind() {
        control.endRefreshing()
        updateLoader()
        if scroll?.refreshControl === control { scroll?.refreshControl = nil }
        scroll = nil
        if loader.parent != nil {
            loader.willMove(toParent: nil)
            loader.viewIfLoaded?.removeFromSuperview()
            loader.removeFromParent()
        }
    }
    private func updateRefreshing() {
        if refreshing && !control.isRefreshing { control.beginRefreshing() }
        else if !refreshing && control.isRefreshing { control.endRefreshing() }
        updateLoader()
    }
    @objc private func pulled() { onPull(); updateLoader() }
    private func updateLoader() {
        guard loader.rootView.label != label || loader.rootView.animated != control.isRefreshing else { return }
        loader.rootView = CoffeePeekNativeLoader(label: label, animated: control.isRefreshing, size: 28)
    }
    private func findScroll() -> UIScrollView? {
        var ancestor = view.superview
        while let candidate = ancestor {
            if let target = candidate as? UIScrollView { return target }
            ancestor = candidate.superview
        }
        return nil
    }
}

#Preview("Pull loader light") {
    ScrollView {
        Text("CoffeePeek").frame(maxWidth: .infinity, minHeight: 600)
            .coffeePeekRefreshable(isRefreshing: false, label: "Загрузка") {}
    }.preferredColorScheme(.light)
}
#Preview("Pull loader dark") {
    ScrollView {
        Text("CoffeePeek").frame(maxWidth: .infinity, minHeight: 600)
            .coffeePeekRefreshable(isRefreshing: false, label: "Загрузка") {}
    }.preferredColorScheme(.dark)
}

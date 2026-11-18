import ComposeApp
import SwiftUI
import UIKit

/// Uses the Kotlin image source while presenting an independent SwiftUI cover.
extension View {
    func coffeePeekPhotoViewer(count: Int, selection: Binding<Int?>, languageTag: String,
                              load: @escaping (Int, @escaping (UIImage?) -> Void) -> (() -> Void)) -> some View {
        fullScreenCover(item: Binding(
            get: { selection.wrappedValue.map(CoffeePeekLocalPhotoPresentation.init) },
            set: { if $0 == nil { selection.wrappedValue = nil } }
        )) { selected in
            CoffeePeekNativePhotoViewer(source: CoffeePeekPhotoSource(count: count, selected: selected.id, load: load),
                                        languageTag: languageTag, onDismiss: { selection.wrappedValue = nil })
        }
    }

    func coffeePeekPhotoViewer(images: [UIImage?], selection: Binding<Int?>, languageTag: String) -> some View {
        fullScreenCover(item: Binding(
            get: { selection.wrappedValue.map(CoffeePeekLocalPhotoPresentation.init) },
            set: { if $0 == nil { selection.wrappedValue = nil } }
        )) { selected in
            CoffeePeekNativePhotoViewer(source: CoffeePeekPhotoSource(images: images, selected: selected.id),
                                        languageTag: languageTag, onDismiss: { selection.wrappedValue = nil })
        }
    }

    func coffeePeekPhotoViewer(
        _ viewer: IosPhotoViewer?,
        languageTag: String,
        onDismiss: @escaping () -> Void
    ) -> some View {
        fullScreenCover(item: Binding(
            get: { viewer.map(CoffeePeekPhotoPresentation.init) },
            set: { if $0 == nil, viewer != nil { onDismiss() } }
        )) { presentation in
            CoffeePeekNativePhotoViewer(source: CoffeePeekPhotoSource(presentation.viewer),
                                        languageTag: languageTag, onDismiss: onDismiss)
        }
    }
}

private struct CoffeePeekLocalPhotoPresentation: Identifiable {
    let id: Int
}

private struct CoffeePeekPhotoPresentation: Identifiable {
    let viewer: IosPhotoViewer
    var id: ObjectIdentifier { ObjectIdentifier(viewer) }
}

private struct CoffeePeekPhotoSource {
    typealias Frame = (UIImage?) -> Void
    typealias Load = (Int, Int, @escaping Frame) -> (() -> Void)

    let count: Int
    let initialIndex: Int
    let loadPreview: Load
    let loadOriginal: Load
    let setVisible: (Bool) -> Void

    init(count: Int, selected: Int, load: @escaping (Int, @escaping Frame) -> (() -> Void)) {
        self.count = count
        initialIndex = min(max(0, selected), max(0, count - 1))
        loadPreview = { index, _, receive in load(index, receive) }
        loadOriginal = loadPreview
        setVisible = { _ in }
    }

    init(images: [UIImage?], selected: Int) {
        count = images.count
        initialIndex = min(max(0, selected), max(0, images.count - 1))
        loadPreview = { index, _, receive in
            receive(images.indices.contains(index) ? images[index] : nil)
            return {}
        }
        loadOriginal = loadPreview
        setVisible = { _ in }
    }

    init(_ viewer: IosPhotoViewer) {
        count = Int(viewer.photoCount)
        initialIndex = Int(viewer.initialIndex)
        loadPreview = { index, selected, receive in
            viewer.loadPreview(page: Int32(index), selected: Int32(selected), onFrame: receive).cancel
        }
        loadOriginal = { index, selected, receive in
            viewer.loadOriginal(page: Int32(index), selected: Int32(selected), onFrame: receive).cancel
        }
        setVisible = { viewer.setVisible(shown: $0) }
    }

    init(preview: UIImage?) {
        count = 3
        initialIndex = 1
        loadPreview = { _, _, receive in receive(preview); return {} }
        loadOriginal = { _, _, receive in receive(nil); return {} }
        setVisible = { _ in }
    }
}

private struct CoffeePeekNativePhotoViewer: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.accessibilityVoiceOverEnabled) private var voiceOver
    @Environment(\.scenePhase) private var scenePhase
    @State private var selected: Int
    @State private var zoom: CGFloat = 1
    @State private var pan = CGSize.zero
    @State private var pinchStart: ZoomSnapshot?
    @State private var dragStart: CGSize?
    @State private var dragDirection: DragDirection?
    @State private var displacement = CGSize.zero
    @State private var controlsVisible = true
    @State private var originalRequested = false
    private let source: CoffeePeekPhotoSource
    private let strings: CoffeePeekPhotoViewerStrings
    private let onDismiss: () -> Void

    init(source: CoffeePeekPhotoSource, languageTag: String, onDismiss: @escaping () -> Void) {
        self.source = source
        strings = CoffeePeekPhotoViewerStrings(languageTag: languageTag)
        self.onDismiss = onDismiss
        _selected = State(initialValue: source.initialIndex)
    }

    var body: some View {
        ZStack(alignment: .top) {
            Color.black.ignoresSafeArea()
            GeometryReader { proxy in
                if scenePhase == .active && source.count > 0 {
                    pages(viewport: proxy.size)
                }
            }
            if controlsVisible || voiceOver { toolbar }
        }
        .foregroundStyle(.white)
        .statusBarHidden(true)
        .accessibilityAction(.escape) { onDismiss() }
        .onAppear { source.setVisible(true) }
        .onDisappear { source.setVisible(false) }
    }

    private var toolbar: some View {
        ZStack(alignment: .topTrailing) {
            if source.count > 1 {
                Text(strings.position(selected, source.count))
                    .font(.subheadline.weight(.semibold).monospacedDigit())
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(.ultraThinMaterial, in: Capsule())
                    .frame(maxWidth: .infinity)
                    .accessibilityLabel(strings.description(selected, source.count))
                    .accessibilityAdjustableAction(adjustPage)
            }
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.body.weight(.semibold))
                    .frame(width: 44, height: 44)
                    .background(.ultraThinMaterial, in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(strings.text("close"))
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }

    private func pages(viewport: CGSize) -> some View {
        let first = max(0, selected - 1)
        let end = min(source.count, selected + 2)
        return ZStack {
            ForEach(first..<end, id: \.self) { index in
                let current = index == selected
                CoffeePeekPhotoPage(
                    source: source, strings: strings, index: index, selected: selected,
                    original: current && originalRequested, zoom: current ? zoom : 1,
                    pan: current ? pan : .zero
                )
                .frame(width: viewport.width, height: viewport.height)
                .offset(x: CGFloat(index - selected) * viewport.width + displacement.width,
                        y: displacement.height)
                .accessibilityHidden(!current)
            }
        }
        .frame(width: viewport.width, height: viewport.height)
        .clipped()
        .contentShape(Rectangle())
        .gesture(drag(in: viewport))
        .simultaneousGesture(magnify(in: viewport))
        .simultaneousGesture(taps(in: viewport))
    }

    private func taps(in viewport: CGSize) -> some Gesture {
        SpatialTapGesture(count: 2).exclusively(before: TapGesture()).onEnded { result in
            switch result {
            case .first(let tap): toggleZoom(at: tap.location, in: viewport)
            case .second: if !voiceOver { controlsVisible.toggle() }
            }
        }
    }

    private func magnify(in viewport: CGSize) -> some Gesture {
        MagnifyGesture().onChanged { value in
            let start = pinchStart ?? ZoomSnapshot(scale: zoom, pan: pan)
            pinchStart = start
            let anchor = CGPoint(x: value.startAnchor.x * viewport.width, y: value.startAnchor.y * viewport.height)
            applyZoom(start.scale * value.magnification, from: start, at: anchor, in: viewport)
        }
        .onEnded { _ in pinchStart = nil }
    }

    private func drag(in viewport: CGSize) -> some Gesture {
        DragGesture(minimumDistance: 12).onChanged { value in
            if pinchStart != nil { return }
            if zoom > 1 {
                let start = dragStart ?? pan
                dragStart = start
                pan = clamped(CGSize(width: start.width + value.translation.width,
                                     height: start.height + value.translation.height), scale: zoom, viewport: viewport)
                return
            }
            let direction = dragDirection ?? DragDirection(value.translation)
            dragDirection = direction
            switch direction {
            case .horizontal:
                let x = value.translation.width
                let edge = (selected == 0 && x > 0) || (selected == source.count - 1 && x < 0)
                displacement.width = edge ? x * 0.25 : x
            case .vertical:
                displacement.height = max(0, value.translation.height)
            }
        }
        .onEnded { value in
            defer {
                dragStart = nil
                dragDirection = nil
                animate { displacement = .zero }
            }
            guard zoom <= 1, pinchStart == nil else { return }
            switch dragDirection {
            case .horizontal:
                let distance = value.predictedEndTranslation.width
                if abs(distance) > min(120, viewport.width * 0.3) {
                    select(selected + (distance < 0 ? 1 : -1))
                }
            case .vertical:
                if value.translation.height >= 120 || value.predictedEndTranslation.height >= 180 { onDismiss() }
            case nil: break
            }
        }
    }

    private func adjustPage(_ direction: AccessibilityAdjustmentDirection) {
        switch direction {
        case .increment: select(selected + 1)
        case .decrement: select(selected - 1)
        @unknown default: break
        }
    }

    private func select(_ index: Int) {
        guard (0..<source.count).contains(index), index != selected else { return }
        animate {
            selected = index
            zoom = 1
            pan = .zero
            originalRequested = false
        }
    }

    private func toggleZoom(at point: CGPoint, in viewport: CGSize) {
        let start = ZoomSnapshot(scale: zoom, pan: pan)
        animate { applyZoom(start.scale > 1 ? 1 : 2.5, from: start, at: point, in: viewport) }
    }

    private func applyZoom(_ scale: CGFloat, from start: ZoomSnapshot, at anchor: CGPoint, in viewport: CGSize) {
        let next = min(max(scale, 1), 5)
        let x = anchor.x - viewport.width / 2
        let y = anchor.y - viewport.height / 2
        let ratio = next / start.scale
        pan = clamped(CGSize(width: (start.pan.width - x) * ratio + x,
                             height: (start.pan.height - y) * ratio + y), scale: next, viewport: viewport)
        zoom = next
        if next > 1 { originalRequested = true }
    }

    private func clamped(_ offset: CGSize, scale: CGFloat, viewport: CGSize) -> CGSize {
        let x = viewport.width * (scale - 1) / 2
        let y = viewport.height * (scale - 1) / 2
        return CGSize(width: min(max(offset.width, -x), x), height: min(max(offset.height, -y), y))
    }

    private func animate(_ update: () -> Void) {
        if reduceMotion { update() } else { withAnimation(.easeOut(duration: 0.2), update) }
    }
}

private struct ZoomSnapshot {
    let scale: CGFloat
    let pan: CGSize
}

private enum DragDirection {
    case horizontal, vertical

    init(_ offset: CGSize) { self = abs(offset.width) >= abs(offset.height) ? .horizontal : .vertical }
}

private struct CoffeePeekPhotoPage: View {
    let source: CoffeePeekPhotoSource
    let strings: CoffeePeekPhotoViewerStrings
    let index: Int
    let selected: Int
    let original: Bool
    let zoom: CGFloat
    let pan: CGSize
    @StateObject private var images = CoffeePeekPhotoPageImages()

    var body: some View {
        ZStack {
            Color.clear
            if let image = images.original ?? images.preview {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .scaleEffect(zoom)
                    .offset(pan)
                    .accessibilityLabel(strings.description(index, source.count))
            } else if images.isLoading {
                ProgressView().tint(.white).accessibilityLabel(strings.text("loading"))
            } else if index == selected {
                Button { reload() } label: {
                    Label(strings.text("retry"), systemImage: "arrow.clockwise")
                        .padding(12)
                }
                .buttonStyle(.plain)
            }
        }
        .onChange(of: selected, initial: true) { _, _ in reload() }
        .onChange(of: original) { _, _ in updateOriginal() }
        .onDisappear(perform: images.cancel)
    }

    private func reload() {
        images.loadPreview(source, page: index, selected: selected)
        updateOriginal()
    }

    private func updateOriginal() { images.loadOriginal(source, page: index, selected: selected, request: original) }
}

private final class CoffeePeekPhotoPageImages: ObservableObject {
    @Published private(set) var preview: UIImage?
    @Published private(set) var original: UIImage?
    @Published private(set) var isLoading = true
    private var previewTask: (() -> Void)?
    private var originalTask: (() -> Void)?

    deinit { previewTask?(); originalTask?() }

    func loadPreview(_ source: CoffeePeekPhotoSource, page: Int, selected: Int) {
        previewTask?()
        preview = nil
        isLoading = true
        previewTask = source.loadPreview(page, selected) { [weak self] image in
            self?.preview = image
            self?.isLoading = false
        }
    }

    func loadOriginal(_ source: CoffeePeekPhotoSource, page: Int, selected: Int, request: Bool) {
        originalTask?()
        original = nil
        originalTask = request ? source.loadOriginal(page, selected) { [weak self] in self?.original = $0 } : nil
    }

    func cancel() {
        previewTask?()
        originalTask?()
        previewTask = nil
        originalTask = nil
        preview = nil
        original = nil
    }
}

private struct CoffeePeekPhotoViewerStrings {
    private let bundle: Bundle
    private let locale: Locale

    init(languageTag: String) {
        bundle = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
            .flatMap { Bundle(url: $0) } ?? .main
        locale = Locale(identifier: languageTag)
    }

    func text(_ key: String) -> String { bundle.localizedString(forKey: key, value: nil, table: "NativePhotoViewer") }
    func description(_ index: Int, _ count: Int) -> String { format("description", index, count) }
    func position(_ index: Int, _ count: Int) -> String { format("position", index, count) }

    private func format(_ key: String, _ index: Int, _ count: Int) -> String {
        String(format: text(key), locale: locale, Int32(index + 1), Int32(count))
    }
}

#Preview("Light") {
    CoffeePeekNativePhotoViewer(source: CoffeePeekPhotoSource(preview: UIImage(systemName: "photo")),
                                languageTag: "ru", onDismiss: {})
        .preferredColorScheme(.light)
}

#Preview("Dark") {
    CoffeePeekNativePhotoViewer(source: CoffeePeekPhotoSource(preview: nil), languageTag: "be", onDismiss: {})
        .preferredColorScheme(.dark)
}

@MainActor
func makeCheckInPhotoController(presentation: IosCheckInPhotoPresentation) -> UIViewController {
    let controller = UIHostingController(rootView: CoffeePeekNativePhotoViewer(
        source: CoffeePeekPhotoSource(presentation.viewer), languageTag: presentation.languageTag,
        onDismiss: presentation.dismiss))
    controller.modalPresentationStyle = .fullScreen
    controller.isModalInPresentation = true
    return controller
}

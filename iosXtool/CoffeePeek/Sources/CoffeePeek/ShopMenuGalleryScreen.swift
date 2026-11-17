import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShopMenuGalleryController(gallery: IosShopMenuGallery, languageTag: String) -> UIViewController {
    UIHostingController(rootView: ShopMenuGalleryHost(
        gallery: gallery,
        strings: ShopMenuGalleryStrings(languageTag: languageTag)
    ))
}

private struct ShopMenuGalleryHost: View {
    @StateObject private var model: ShopMenuGalleryModel
    private let gallery: IosShopMenuGallery
    private let strings: ShopMenuGalleryStrings

    init(gallery: IosShopMenuGallery, strings: ShopMenuGalleryStrings) {
        self.gallery = gallery
        self.strings = strings
        _model = StateObject(wrappedValue: ShopMenuGalleryModel(gallery: gallery))
    }

    var body: some View {
        ShopMenuGalleryScreen(
            state: model.state,
            strings: strings,
            onBack: { gallery.back() },
            onRetry: { gallery.retry() },
            onPhoto: { gallery.openPhoto(index: Int32($0)) },
            photoContent: { index in
                ShopMenuGalleryPreviewPhoto(gallery: gallery, index: index, strings: strings)
            }
        )
        .accessibilityHidden(model.viewer != nil)
        .allowsHitTesting(model.viewer == nil)
        .coffeePeekPhotoViewer(model.viewer, languageTag: strings.languageTag, onDismiss: model.dismissViewer)
        .onAppear { model.observe() }
        .onDisappear { model.stopObserving() }
    }
}

private final class ShopMenuGalleryModel: ObservableObject {
    @Published private(set) var state: IosShopMenuGalleryState
    @Published private(set) var viewer: IosPhotoViewer?
    private let gallery: IosShopMenuGallery
    private var observation: IosShopMenuGalleryObservation?

    init(gallery: IosShopMenuGallery) {
        self.gallery = gallery
        state = gallery.currentState()
    }

    deinit {
        observation?.cancel()
        gallery.close()
    }

    func observe() {
        guard observation == nil else { return }
        state = gallery.currentState()
        observation = gallery.observe(
            onState: { [weak self] state in self?.state = state },
            onPhoto: { [weak self] photo in self?.openViewer(photo) }
        )
    }

    func stopObserving() {
        guard viewer == nil else { return }
        observation?.cancel()
        observation = nil
    }

    private func openViewer(_ photo: IosShopMenuGalleryPhoto) {
        guard viewer == nil else { return }
        viewer = gallery.photoViewer(photo: photo)
    }

    func dismissViewer() { viewer = nil }
}

private struct ShopMenuGalleryScreen<Photo: View>: View {
    let state: IosShopMenuGalleryState
    let strings: ShopMenuGalleryStrings
    let onBack: () -> Void
    let onRetry: () -> Void
    let onPhoto: (Int) -> Void
    let photoContent: (Int) -> Photo

    var body: some View {
        Group {
            if state.isLoading {
                ProgressView().accessibilityLabel(strings.text("gallery.loading"))
            } else if state.hasError || state.previewUrls.isEmpty {
                statusMessage
            } else {
                photos
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(uiColor: .systemBackground))
        .safeAreaInset(edge: .top, spacing: 0) { header }
    }

    private var header: some View {
        HStack(spacing: 8) {
            Button(action: onBack) {
                Label(strings.text("gallery.back"), systemImage: "chevron.backward")
                    .labelStyle(.iconOnly)
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(Rectangle())
            }
            Text(strings.text("gallery.title"))
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .background(.bar)
    }

    private var statusMessage: some View {
        VStack(spacing: 12) {
            Text(strings.text(state.hasError ? "gallery.error" : "gallery.empty"))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            if state.hasError {
                Button(strings.text("gallery.retry"), action: onRetry)
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding(16)
    }

    private var photos: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                if !state.shopTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(state.shopTitle)
                        .font(.title3)
                        .foregroundStyle(.secondary)
                        .accessibilityAddTraits(.isHeader)
                }
                ForEach(state.previewUrls.indices, id: \.self) { index in
                    photoCard(index)
                }
            }
            .padding(16)
        }
    }

    private func photoCard(_ index: Int) -> some View {
        Button { onPhoto(index) } label: {
            Color(uiColor: .secondarySystemBackground)
                .aspectRatio(0.72, contentMode: .fit)
                .overlay {
                    photoContent(index).id(state.previewUrls[index])
                }
                .overlay(alignment: .bottomTrailing) {
                    Text(strings.position("gallery.counter", index: index, count: state.previewUrls.count))
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .foregroundStyle(.white)
                        .background(.black.opacity(0.65), in: Capsule())
                        .padding(12)
                }
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(strings.position("gallery.photo", index: index, count: state.previewUrls.count))
    }
}

private struct ShopMenuGalleryPreviewPhoto: View {
    let gallery: IosShopMenuGallery
    let index: Int
    let strings: ShopMenuGalleryStrings
    @State private var observation: IosShopMenuGalleryObservation?
    @State private var image: UIImage?

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image).resizable().scaledToFit()
            } else {
                Text(strings.text("gallery.photoUnavailable"))
                    .foregroundStyle(.secondary)
                    .padding(12)
            }
        }
        .accessibilityHidden(true)
        .onAppear {
            observation = gallery.loadPreview(index: Int32(index), onImage: { [image = $image] in
                image.wrappedValue = $0
            })
        }
        .onDisappear {
            observation?.cancel()
            observation = nil
            image = nil
        }
    }
}

private struct ShopMenuGalleryStrings {
    let languageTag: String
    private let bundle: Bundle
    private let locale: Locale

    init(languageTag: String) {
        self.languageTag = languageTag
        let localization = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = localization.flatMap { Bundle(url: $0) } ?? .main
        locale = Locale(identifier: languageTag)
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "ShopMenuGallery")
    }

    func position(_ key: String, index: Int, count: Int) -> String {
        String(format: text(key), locale: locale, Int32(index + 1), Int32(count))
    }
}

#Preview("Light") {
    ShopMenuGalleryScreen(
        state: IosShopMenuGalleryState(shopTitle: "Кофейня", previewUrls: ["preview"], isLoading: false, hasError: false),
        strings: ShopMenuGalleryStrings(languageTag: "ru"),
        onBack: {}, onRetry: {}, onPhoto: { _ in },
        photoContent: { _ in Image(systemName: "photo").foregroundStyle(.secondary) }
    )
    .preferredColorScheme(.light)
}

#Preview("Dark") {
    ShopMenuGalleryScreen(
        state: IosShopMenuGalleryState(shopTitle: "", previewUrls: [], isLoading: false, hasError: true),
        strings: ShopMenuGalleryStrings(languageTag: "be"),
        onBack: {}, onRetry: {}, onPhoto: { _ in },
        photoContent: { _ in Color.clear }
    )
    .preferredColorScheme(.dark)
}

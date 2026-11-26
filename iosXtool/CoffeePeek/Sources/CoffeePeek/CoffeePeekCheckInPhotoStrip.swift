import SwiftUI
import UIKit

struct CoffeePeekCheckInPhotoStrip: View {
    let photos: [CoffeePeekCheckInPhoto]
    let loadImage: CoffeePeekCardImageLoader
    let openPhoto: (Int) -> Void
    var horizontalInset: CGFloat = 16
    @State private var ratios: [String: CGFloat] = [:]
    @State private var availableWidth: CGFloat = 0

    private var contentWidth: CGFloat { max(0, availableWidth - horizontalInset * 2) }
    private var maximumHeight: CGFloat { min(contentWidth * 0.72, 260) }
    private var stripHeight: CGFloat {
        guard photos.count == 1, let photo = photos.first else { return maximumHeight }
        return dimensions(photo, single: true).height
    }

    var body: some View {
        GeometryReader { geometry in
            Group {
                if photos.count == 1, let photo = photos.first {
                    tile(photo, index: 0, single: true).padding(.horizontal, horizontalInset)
                } else {
                    ScrollView(.horizontal) {
                        HStack(alignment: .top, spacing: 8) {
                            ForEach(Array(photos.enumerated()), id: \.element.id) { index, photo in
                                tile(photo, index: index, single: false)
                            }
                        }.padding(.horizontal, horizontalInset)
                    }.scrollIndicators(.hidden)
                }
            }
            .onAppear { availableWidth = geometry.size.width }
            .onChange(of: geometry.size.width) { _, width in availableWidth = width }
        }
        .frame(height: stripHeight)
        .onChange(of: photos.map(\.id)) { _, _ in ratios = [:] }
    }

    private func dimensions(_ photo: CoffeePeekCheckInPhoto, single: Bool) -> CGSize {
        let ratio = max(0.75, ratios[photo.id] ?? (single ? 2 : 0.75))
        let width = min(contentWidth * (single ? 1 : 0.88), maximumHeight * ratio)
        return CGSize(width: width, height: width / ratio)
    }

    private func tile(_ photo: CoffeePeekCheckInPhoto, index: Int, single: Bool) -> some View {
        let size = dimensions(photo, single: single)
        return Button { openPhoto(index) } label: {
            CoffeePeekCardImage(url: photo.preview, detail: photo.detail, symbol: nil,
                fits: (ratios[photo.id] ?? 1) >= 0.75, loadImage: loadImage) { decoded in
                    guard ratios[photo.id] == nil, decoded.width > 0, decoded.height > 0 else { return }
                    ratios[photo.id] = min(16, decoded.width / decoded.height)
                }
                .frame(width: size.width, height: size.height)
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain).disabled(!photo.canOpen).accessibilityLabel(photo.label)
    }
}

struct CoffeePeekCardImage: View {
    @Environment(\.scenePhase) private var scenePhase
    let url: String?
    var detail: String?
    let symbol: String?
    var fits = false
    var imageAlignment: Alignment = .center
    let loadImage: CoffeePeekCardImageLoader
    var decoded: (CGSize) -> Void = { _ in }
    @State private var image: UIImage?
    @State private var previewImage: UIImage?
    @State private var previewRequest: CoffeePeekCardImageRequest?
    @State private var detailRequest: CoffeePeekCardImageRequest?
    @State private var requestID: UUID?
    @State private var detailRequestID: UUID?
    @State private var hasShownPreview = false
    @State private var hasShownDetail = false
    @State private var usesDetailFallback = false
    @State private var visible = false

    var body: some View {
        GeometryReader { geometry in
            Group {
                if let image {
                    Image(uiImage: image).resizable().aspectRatio(contentMode: fits ? .fit : .fill)
                        .frame(width: geometry.size.width, height: geometry.size.height, alignment: imageAlignment)
                } else if let symbol {
                    CoffeePeekNativeStyle.surface.overlay {
                        Image(systemName: symbol).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    }
                } else {
                    CoffeePeekPhotoPlaceholder()
                }
            }.clipped()
        }
        .accessibilityHidden(true)
        .onAppear {
            visible = true
            load()
        }
        .onChange(of: [url, detail]) { _, _ in load() }
        .onChange(of: scenePhase) { _, _ in load() }
        .onDisappear {
            visible = false
            cancel()
        }
    }

    private func load() {
        cancel()
        guard visible, scenePhase == .active else { return }
        let source = url?.trimmingCharacters(in: .whitespacesAndNewlines)
        let target = source?.isEmpty == false ? source : detail
        guard let target, !target.isEmpty else { return }
        let id = UUID()
        requestID = id
        let handle = loadImage(target) { next in
            receivePreview(next, id: id, target: target)
        }
        if usesDetailFallback { handle?.cancel() } else { previewRequest = handle }
    }

    private func receivePreview(_ next: UIImage?, id: UUID, target: String) {
        guard requestID == id, !usesDetailFallback else { return }
        previewImage = next
        image = next
        cancelDetail()
        if let next {
            hasShownPreview = true
            decoded(next.size)
        } else if hasShownPreview {
            return
        }
        guard let detail, !detail.isEmpty, detail != target else { return }
        if next == nil {
            usesDetailFallback = true
            previewRequest?.cancel()
            previewRequest = nil
        }
        upgrade(detail, id: id)
    }

    private func upgrade(_ url: String, id: UUID) {
        let detailID = UUID()
        detailRequestID = detailID
        detailRequest = loadImage(url) { next in
            guard requestID == id, detailRequestID == detailID else { return }
            if let next {
                hasShownDetail = true
                image = next
                decoded(next.size)
            } else {
                image = hasShownDetail ? nil : previewImage
            }
        }
    }

    private func cancelDetail() {
        detailRequestID = nil
        detailRequest?.cancel()
        detailRequest = nil
        hasShownDetail = false
    }

    private func cancel() {
        requestID = nil
        previewRequest?.cancel()
        cancelDetail()
        previewRequest = nil
        image = nil
        previewImage = nil
        hasShownPreview = false
        usesDetailFallback = false
    }
}

#Preview("Photo strip light") {
    CoffeePeekCheckInPhotoStrip(photos: [0, 1, 2].map {
        CoffeePeekCheckInPhoto(id: "fixture-\($0)", preview: nil, detail: nil, label: "Photo \($0 + 1)")
    }, loadImage: { _, _ in nil }, openPhoto: { _ in }).preferredColorScheme(.light)
}

#Preview("Photo strip dark") {
    CoffeePeekCheckInPhotoStrip(photos: [CoffeePeekCheckInPhoto(id: "fixture", preview: nil, detail: nil, label: "Photo")],
        loadImage: { _, _ in nil }, openPhoto: { _ in }).preferredColorScheme(.dark)
}

@MainActor
private func cardImageAlignmentPreview() -> some View {
    HStack(alignment: .bottom, spacing: 16) {
        ForEach(0..<2) { index in
            CoffeePeekCardImage(url: "preview-bag", symbol: nil, fits: true,
                imageAlignment: index == 0 ? .center : .bottom, loadImage: { _, receive in
                    receive(UIImage(systemName: "bag.fill")?.withTintColor(.systemYellow, renderingMode: .alwaysOriginal))
                    return nil
                })
                .frame(width: 100, height: 180)
                .background(CoffeePeekNativeStyle.surfaceVariant)
                .overlay(alignment: .bottom) { Rectangle().fill(CoffeePeekNativeStyle.accent).frame(height: 2) }
        }
    }.padding().background(CoffeePeekNativeStyle.background)
}

#Preview("Image alignment light") { cardImageAlignmentPreview().preferredColorScheme(.light) }
#Preview("Image alignment dark") { cardImageAlignmentPreview().preferredColorScheme(.dark) }

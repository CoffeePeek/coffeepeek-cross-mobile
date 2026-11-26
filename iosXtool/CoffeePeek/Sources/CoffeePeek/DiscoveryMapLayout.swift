import CoreGraphics

enum DiscoveryMapLayout {
    static let previewHeight: CGFloat = 200
    static let previewSpacing: CGFloat = 16
    static let previewTravel = previewHeight + previewSpacing * 2
}

func discoveryMapLayoutFrame(viewport: CGSize, preview: CGRect, expanded: Bool,
                             previewVisible: Bool = true, resultsTop: CGFloat = 0) -> CGRect? {
    let source = expanded ? CGRect(origin: .zero, size: viewport) : preview
    let inset: CGFloat = expanded ? 0 : 16
    // SwiftUI may report zero or invalid bounds before the preview is measured.
    // Insetting a preview narrower than both margins produces CGRect.null.
    guard !source.isNull, !source.isInfinite,
          source.origin.x.isFinite, source.origin.y.isFinite,
          source.size.width.isFinite, source.size.height.isFinite,
          source.size.width > inset * 2, source.size.height > 0 else { return nil }
    var frame = source.insetBy(dx: inset, dy: 0)
    if !expanded && !previewVisible {
        guard resultsTop.isFinite else { return nil }
        frame.origin.y = min(frame.minY, resultsTop - frame.height - DiscoveryMapLayout.previewSpacing)
    }
    guard frame.maxX.isFinite, frame.maxY.isFinite else { return nil }
    return frame
}

func discoveryMapClipFrame(viewport: CGSize, results: CGRect, expanded: Bool) -> CGRect {
    guard let bounds = discoveryMapLayoutFrame(viewport: viewport, preview: .zero, expanded: true) else { return .zero }
    if expanded { return bounds }
    guard !results.isNull, !results.isInfinite, !results.isEmpty,
          results.minX.isFinite, results.minY.isFinite, results.maxX.isFinite, results.maxY.isFinite else { return .zero }
    let visible = bounds.intersection(results)
    return visible.isEmpty ? .zero : visible
}

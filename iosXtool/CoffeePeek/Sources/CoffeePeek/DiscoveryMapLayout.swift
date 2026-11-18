import CoreGraphics

func discoveryMapLayoutFrame(viewport: CGSize, preview: CGRect, expanded: Bool) -> CGRect? {
    let source = expanded ? CGRect(origin: .zero, size: viewport) : preview
    let inset: CGFloat = expanded ? 0 : 16
    // SwiftUI may report zero or invalid bounds before the preview is measured.
    // Insetting a preview narrower than both margins produces CGRect.null.
    guard !source.isNull, !source.isInfinite,
          source.origin.x.isFinite, source.origin.y.isFinite,
          source.size.width.isFinite, source.size.height.isFinite,
          source.size.width > inset * 2, source.size.height > 0 else { return nil }
    let frame = source.insetBy(dx: inset, dy: 0)
    guard frame.maxX.isFinite, frame.maxY.isFinite else { return nil }
    return frame
}

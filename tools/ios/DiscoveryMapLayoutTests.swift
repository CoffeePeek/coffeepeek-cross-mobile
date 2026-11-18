import CoreGraphics

@main
struct DiscoveryMapLayoutTests {
    static func main() {
        rejectsInvalidPreviewFrames()
        respectsPreviewMarginsAndFullScreenBounds()
    }

    private static func rejectsInvalidPreviewFrames() {
        let viewport = CGSize(width: 393, height: 852)
        assert(CGRect.zero.insetBy(dx: 16, dy: 0).isNull)
        let invalidFrames: [CGRect] = [
            .zero, .null, .infinite,
            CGRect(x: 0, y: 0, width: 31, height: 200),
            CGRect(x: 0, y: 0, width: 32, height: 200),
            CGRect(x: CGFloat.infinity, y: 0, width: 393, height: 200),
            CGRect(x: 0, y: CGFloat.nan, width: 393, height: 200),
        ]
        for frame in invalidFrames {
            assert(discoveryMapLayoutFrame(viewport: viewport, preview: frame, expanded: false) == nil)
        }
    }

    private static func respectsPreviewMarginsAndFullScreenBounds() {
        let viewport = CGSize(width: 393, height: 852)
        let preview = CGRect(x: 0, y: -25, width: 393, height: 200)
        let previewFrame = discoveryMapLayoutFrame(viewport: viewport, preview: preview, expanded: false)
        assert(previewFrame == CGRect(x: 16, y: -25, width: 361, height: 200))
        let expanded = discoveryMapLayoutFrame(viewport: viewport, preview: .null, expanded: true)
        assert(expanded == CGRect(origin: .zero, size: viewport))
        assert(discoveryMapLayoutFrame(viewport: .zero, preview: preview, expanded: true) == nil)
    }
}

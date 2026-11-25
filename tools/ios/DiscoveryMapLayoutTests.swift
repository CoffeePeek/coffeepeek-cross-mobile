import CoreGraphics

@main
struct DiscoveryMapLayoutTests {
    static func main() {
        rejectsInvalidPreviewFrames()
        respectsPreviewMarginsAndFullScreenBounds()
        clipsPreviewToMeasuredResultsAndLeavesExpandedMapFullSize()
        slidesHiddenPreviewAboveResultsWithoutResizing()
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

    private static func clipsPreviewToMeasuredResultsAndLeavesExpandedMapFullSize() {
        let viewport = CGSize(width: 393, height: 852)
        let results = CGRect(x: 0, y: 110, width: 393, height: 800)
        let clip = discoveryMapClipFrame(viewport: viewport, results: results, expanded: false)
        assert(clip == CGRect(x: 0, y: 110, width: 393, height: 742))
        assert(discoveryMapClipFrame(viewport: viewport, results: .zero, expanded: false) == .zero)
        assert(discoveryMapClipFrame(viewport: viewport, results: .infinite, expanded: false) == .zero)
        assert(discoveryMapClipFrame(viewport: viewport, results: results, expanded: true) == CGRect(origin: .zero, size: viewport))
    }

    private static func slidesHiddenPreviewAboveResultsWithoutResizing() {
        let viewport = CGSize(width: 393, height: 852)
        let preview = CGRect(x: 0, y: 126, width: 393, height: 200)
        let hidden = discoveryMapLayoutFrame(viewport: viewport, preview: preview, expanded: false,
            previewVisible: false, resultsTop: 110)
        assert(hidden == CGRect(x: 16, y: -90, width: 361, height: 200))
        assert(hidden?.maxY == 110)
        let offscreen = CGRect(x: 0, y: -400, width: 393, height: 200)
        assert(discoveryMapLayoutFrame(viewport: viewport, preview: offscreen, expanded: false,
            previewVisible: false, resultsTop: 110)?.minY == -400)
        assert(discoveryMapLayoutFrame(viewport: viewport, preview: preview, expanded: false,
            previewVisible: false, resultsTop: .nan) == nil)
        assert(discoveryMapLayoutFrame(viewport: viewport, preview: preview, expanded: true,
            previewVisible: false, resultsTop: .nan) == CGRect(origin: .zero, size: viewport))
    }
}

import ComposeApp
import SwiftUI

struct CoffeePeekNativeLoader: View {
    let label: String
    var animated = true
    @Environment(\.accessibilityReduceMotion) private var reducedMotion
    @Environment(\.scenePhase) private var scenePhase
    @State private var started = Date()
    private let contours = IosLoaderGeometry.shared.contours.map { shape in
        shape.map { CGPoint(x: CGFloat($0.x), y: CGFloat($0.y)) }
    }

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30, paused: !animated || reducedMotion || scenePhase != .active)) { context in
            let elapsed = max(0, context.date.timeIntervalSince(started))
            let moving = animated && !reducedMotion
            let position = moving ? elapsed.truncatingRemainder(dividingBy: 5) : 0
            let morph = floor(position) + eased(position - floor(position))
            let fill = moving ? elapsed.truncatingRemainder(dividingBy: 2.8) / 2.8 * 0.72 : 0.5
            let shape = CoffeePeekLoaderContour(contours: contours, morph: morph)
            ZStack {
                shape.stroke(CoffeePeekNativeStyle.accent.opacity(0.18), style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
                if fill > 0.01 {
                    shape.trim(from: 0, to: fill).stroke(CoffeePeekNativeStyle.accent,
                        style: StrokeStyle(lineWidth: 3, lineCap: .butt, lineJoin: .round))
                }
            }.rotationEffect(.degrees(moving ? elapsed / 1.6 * 360 : 0))
        }
        .frame(width: 48, height: 48)
        .accessibilityElement(children: .ignore).accessibilityLabel(label)
        .accessibilityAddTraits(.updatesFrequently)
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
        let radius = max(0, (min(rect.width, rect.height) - 3) / 2)
        return from.indices.map { index in
            CGPoint(x: rect.midX + (from[index].x + (to[index].x - from[index].x) * amount) * radius,
                    y: rect.midY + (from[index].y + (to[index].y - from[index].y) * amount) * radius)
        }
    }
}

#Preview("Native loader light") { CoffeePeekNativeLoader(label: "Загрузка", animated: false).padding().preferredColorScheme(.light) }
#Preview("Native loader dark") { CoffeePeekNativeLoader(label: "Загрузка", animated: false).padding().preferredColorScheme(.dark) }

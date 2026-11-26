import SwiftUI

/// One decorative heart; the owning button supplies its label, action and touch target.
struct CoffeePeekLikeIcon: View {
    let selected: Bool
    var unselectedColor: Color = CoffeePeekNativeStyle.foreground
    var isLoading = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var fill: Double
    @State private var scale: CGFloat = 1

    init(selected: Bool, unselectedColor: Color = CoffeePeekNativeStyle.foreground, isLoading: Bool = false) {
        self.selected = selected
        self.unselectedColor = unselectedColor
        self.isLoading = isLoading
        _fill = State(initialValue: selected ? 1 : 0)
    }

    var body: some View {
        ZStack {
            Image(systemName: "heart.fill").foregroundStyle(CoffeePeekNativeStyle.accent).opacity(fill)
            Image(systemName: "heart").foregroundStyle(fill > 0 ? CoffeePeekNativeStyle.accent : unselectedColor)
        }
        .font(.system(size: 24))
        .frame(width: 24, height: 24)
        .scaleEffect(scale)
        .accessibilityHidden(true)
        .task(id: AnimationTarget(selected: selected, reduced: reduceMotion, loading: isLoading)) {
            await animateSelection()
        }
    }

    @MainActor private func animateSelection() async {
        let target = selected ? 1.0 : 0.0
        if reduceMotion {
            fill = target
            scale = 1
            return
        }
        guard !isLoading, fill != target || scale != 1 else { return }
        withAnimation(.easeOut(duration: 0.095)) { scale = 1.24 }
        guard await wait(95) else { return }
        withAnimation(.easeInOut(duration: 0.08)) { fill = target }
        guard await wait(80) else { return }
        withAnimation(.easeOut(duration: 0.145)) { scale = 1 }
    }

    private func wait(_ milliseconds: UInt64) async -> Bool {
        do {
            try await Task.sleep(nanoseconds: milliseconds * 1_000_000)
            return !Task.isCancelled
        } catch { return false }
    }

    private struct AnimationTarget: Equatable {
        let selected: Bool
        let reduced: Bool
        let loading: Bool
    }
}

private struct CoffeePeekLikeIconPreview: View {
    @State private var selected = false
    var body: some View {
        HStack(spacing: 24) {
            Button { selected.toggle() } label: {
                CoffeePeekLikeIcon(selected: selected).frame(width: 48, height: 48)
            }.accessibilityLabel("Избранное")
            CoffeePeekLikeIcon(selected: selected, unselectedColor: .white)
                .frame(width: 48, height: 48).background(.black.opacity(0.68), in: Circle())
        }.buttonStyle(.plain).padding().background(CoffeePeekNativeStyle.background)
    }
}

#Preview("Hearts light") { CoffeePeekLikeIconPreview().preferredColorScheme(.light) }
#Preview("Hearts dark") { CoffeePeekLikeIconPreview().preferredColorScheme(.dark) }

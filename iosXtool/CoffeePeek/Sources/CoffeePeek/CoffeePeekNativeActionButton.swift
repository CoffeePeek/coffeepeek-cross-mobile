import SwiftUI

struct CoffeePeekNativeActionButton: View {
    let title: String
    var primary = true
    var symbol: String? = nil
    var busy = false
    var enabled = true
    var fontSize: CGFloat = 17
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let symbol { Image(systemName: symbol) }
                if busy { ProgressView().tint(primary ? .black : CoffeePeekNativeStyle.foreground) }
                Text(title).font(CoffeePeekNativeStyle.font(fontSize, weight: "Semibold"))
            }
            .multilineTextAlignment(.center)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, minHeight: 48)
        }
        .buttonStyle(.plain)
        .foregroundStyle(primary ? .black : CoffeePeekNativeStyle.foreground)
        .background(primary ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.surface, in: Capsule())
        .opacity(enabled ? 1 : 0.5)
        .disabled(!enabled || busy)
    }
}

#Preview("Native actions light") {
    VStack {
        CoffeePeekNativeActionButton(title: "CoffeePeek", symbol: "plus", action: {})
        CoffeePeekNativeActionButton(title: "CoffeePeek", primary: false, action: {})
    }.padding().preferredColorScheme(.light)
}

#Preview("Native actions dark") {
    VStack {
        CoffeePeekNativeActionButton(title: "CoffeePeek", symbol: "plus", action: {})
        CoffeePeekNativeActionButton(title: "CoffeePeek", primary: false, action: {})
    }.padding().preferredColorScheme(.dark)
}

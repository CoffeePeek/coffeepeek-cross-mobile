import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCommunityMessageController(message: IosCommunityMessage) -> UIViewController {
    let controller = UIHostingController(rootView: CoffeePeekNativeMessage(text: message.text,
        actionLabel: message.actionLabel, dismissLabel: message.dismissLabel,
        dismiss: message.dismiss, performAction: message.performAction)
        .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { message.heightChanged(height: Double($0)) })
    controller.view.backgroundColor = .clear
    controller.safeAreaRegions = []
    controller.sizingOptions = .intrinsicContentSize
    return controller
}

struct CoffeePeekNativeMessage: View {
    let text: String
    let actionLabel: String?
    let dismissLabel: String
    let dismiss: () -> Void
    let performAction: () -> Void
    @State private var contentHeight: CGFloat = 64

    var body: some View {
        ScrollView {
            content.onGeometryChange(for: CGFloat.self) { $0.size.height } action: { contentHeight = $0 }
        }
        .scrollBounceBehavior(.basedOnSize)
        .frame(maxWidth: .infinity)
        .frame(height: min(contentHeight, max(64, UIScreen.main.bounds.height * 0.5)))
        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 12))
        .overlay { RoundedRectangle(cornerRadius: 12).stroke(CoffeePeekNativeStyle.border, lineWidth: 1) }
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
    }

    private var content: some View {
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                Image(systemName: "info.circle.fill").foregroundStyle(CoffeePeekNativeStyle.accent)
                    .font(.system(size: 20)).accessibilityHidden(true)
                Text(text).font(CoffeePeekNativeStyle.font(14)).frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, 8)
                Button(action: dismiss) { Image(systemName: "xmark").font(.system(size: 20)).frame(width: 48, height: 48) }
                    .buttonStyle(.plain).foregroundStyle(CoffeePeekNativeStyle.secondary).accessibilityLabel(dismissLabel)
            }.padding(.leading, 16).padding(.trailing, 4)
            if let actionLabel {
                CoffeePeekNativeActionButton(title: actionLabel, fontSize: 14, cornerRadius: 12, action: performAction)
                    .padding(.horizontal, 16).padding(.bottom, 12)
            }
        }
    }
}

#Preview("Message light") {
    CoffeePeekNativeMessage(text: "Изменения сохранены", actionLabel: nil, dismissLabel: "Закрыть", dismiss: {}, performAction: {})
        .preferredColorScheme(.light)
}

#Preview("Message dark, large text") {
    CoffeePeekNativeMessage(text: "Не удалось сохранить изменения. Попробуйте ещё раз.", actionLabel: "Повторить", dismissLabel: "Закрыть", dismiss: {}, performAction: {})
        .preferredColorScheme(.dark).environment(\.dynamicTypeSize, .accessibility3)
}

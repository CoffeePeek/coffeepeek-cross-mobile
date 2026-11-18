import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeGlobalDialogController(dialog: IosGlobalDialog) -> UIViewController {
    let controller = UIHostingController(rootView: GlobalDialogNativeHost(dialog: dialog))
    controller.view.backgroundColor = .clear
    controller.modalPresentationStyle = .overFullScreen
    controller.isModalInPresentation = true
    return controller
}

private struct GlobalDialogNativeHost: View {
    let dialog: IosGlobalDialog
    @State private var illustration: UIImage?
    @State private var observation: IosGlobalDialogObservation?

    var body: some View {
        GlobalDialogNativeContent(error: dialog.error, title: dialog.title, message: dialog.message,
            closeLabel: dialog.closeLabel, illustration: illustration, dismiss: dialog.dismiss)
            .onAppear {
                if dialog.error && observation == nil {
                    observation = dialog.loadIllustration { illustration = $0 }
                }
            }
            .onDisappear { observation?.cancel(); observation = nil }
    }
}

struct GlobalDialogNativeContent: View {
    let error: Bool
    let title: String
    let message: String?
    let closeLabel: String
    let illustration: UIImage?
    let dismiss: () -> Void
    @State private var cardHeight: CGFloat = 0

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                Color.black.opacity(error ? 0.32 : 0.001).ignoresSafeArea()
                    .onTapGesture { if error { dismiss() } }
                if error { errorCard(maxHeight: max(48, geometry.size.height - 64)) }
                else {
                    VStack(spacing: 16) {
                        CoffeePeekNativeLoader(label: title)
                        if let message { Text(message).font(CoffeePeekNativeStyle.font(16, weight: "Medium")) }
                    }
                }
            }.frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .accessibilityAddTraits(.isModal)
        .accessibilityAction(.escape) { if error { dismiss() } }
    }

    private func errorCard(maxHeight: CGFloat) -> some View {
        ScrollView {
            errorMessage.padding(24).multilineTextAlignment(.center)
                .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 28))
                .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { cardHeight = $0 }
        }
        .scrollBounceBehavior(.basedOnSize)
        .frame(maxWidth: 480)
        .frame(height: min(cardHeight > 0 ? cardHeight : maxHeight, maxHeight))
        .padding(.horizontal, 32)
    }

    private var errorMessage: some View {
        VStack(spacing: 0) {
            if let illustration {
                Image(uiImage: illustration).resizable().scaledToFit().frame(width: 96, height: 96).accessibilityHidden(true)
            } else { Color.clear.frame(height: 96).accessibilityHidden(true) }
            Text(title).font(CoffeePeekNativeStyle.font(20, weight: "Bold", relativeTo: .headline))
                .accessibilityAddTraits(.isHeader).padding(.top, 16)
            Text(message ?? "").font(CoffeePeekNativeStyle.font(16))
                .foregroundStyle(CoffeePeekNativeStyle.secondary).padding(.top, 8)
            CoffeePeekNativeActionButton(title: closeLabel, fontSize: 16, action: dismiss).padding(.top, 24)
        }
    }

}

#Preview("Global error light") {
    GlobalDialogNativeContent(error: true, title: "Что-то пошло не так", message: "Повторите позже", closeLabel: "Понятно",
        illustration: nil, dismiss: {}).preferredColorScheme(.light)
}
#Preview("Global error dark") {
    GlobalDialogNativeContent(error: true, title: "Что-то пошло не так", message: "Повторите позже", closeLabel: "Понятно",
        illustration: nil, dismiss: {}).preferredColorScheme(.dark)
}

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

    var body: some View {
        ZStack {
            Color.black.opacity(error ? 0.32 : 0.001).ignoresSafeArea()
                .onTapGesture { if error { dismiss() } }
            if error { errorCard }
            else {
                VStack(spacing: 16) {
                    CoffeePeekNativeLoader(label: title)
                    if let message { Text(message).font(CoffeePeekNativeStyle.font(16, weight: "Medium")) }
                }
            }
        }
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .accessibilityAddTraits(.isModal)
        .accessibilityAction(.escape) { if error { dismiss() } }
    }

    private var errorCard: some View {
        ScrollView {
            VStack(spacing: 0) {
                if let illustration {
                    Image(uiImage: illustration).resizable().scaledToFit().frame(width: 96, height: 96).accessibilityHidden(true)
                } else { Color.clear.frame(height: 96).accessibilityHidden(true) }
                Text(title).font(CoffeePeekNativeStyle.font(20, weight: "Bold", relativeTo: .headline))
                    .accessibilityAddTraits(.isHeader).padding(.top, 16)
                Text(message ?? "").font(CoffeePeekNativeStyle.font(16))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary).padding(.top, 8)
                Button(action: dismiss) {
                    Text(closeLabel).font(CoffeePeekNativeStyle.font(16, weight: "Semibold"))
                        .padding(.horizontal, 16).padding(.vertical, 12).frame(maxWidth: .infinity, minHeight: 48)
                }
                .buttonStyle(.plain).foregroundStyle(.black)
                .background(CoffeePeekNativeStyle.accent, in: Capsule()).padding(.top, 24)
            }.padding(24).multilineTextAlignment(.center)
                .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 28))
        }
        .scrollBounceBehavior(.basedOnSize)
        .fixedSize(horizontal: false, vertical: true)
        .frame(maxWidth: 480).padding(32)
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

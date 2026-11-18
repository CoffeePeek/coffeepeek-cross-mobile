import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShopPhotoOverlayController(album: IosPhotoViewer, languageTag: String, dismiss: @escaping () -> Void) -> UIViewController {
    let controller = UIHostingController(rootView: Color.clear
        .coffeePeekPhotoViewer(album, languageTag: languageTag, onDismiss: dismiss))
    controller.view.backgroundColor = .clear
    return controller
}

@MainActor
func makeShopDetailNoticeController(notice: IosShopDetailNotice) -> UIViewController {
    let controller = UIHostingController(rootView: ShopDetailToastView(message: notice.message,
        dismissLabel: notice.dismissLabel, dismiss: { notice.dismiss() }))
    controller.view.backgroundColor = .clear
    return controller
}

@MainActor
func makeShopDetailErrorController(error: IosShopDetailSessionError) -> UIViewController {
    UIHostingController(rootView: ShopDetailSessionErrorView(title: error.title, retry: error.retry, back: error.back,
        onRetry: { error.onRetry() }, onBack: { error.onBack() }))
}

struct ShopDetailToastView: View {
    let message: String
    let dismissLabel: String
    let dismiss: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Text(message).font(CoffeePeekNativeStyle.font(14)).frame(maxWidth: .infinity, alignment: .leading)
            Button(action: dismiss) { Image(systemName: "xmark").frame(width: 44, height: 44) }
                .accessibilityLabel(dismissLabel)
        }.padding(12).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
            .overlay { RoundedRectangle(cornerRadius: 16).stroke(CoffeePeekNativeStyle.border, lineWidth: 1) }
            .foregroundStyle(CoffeePeekNativeStyle.foreground).padding(.horizontal, 16)
            .task(id: message) {
                do { try await Task.sleep(for: .seconds(4)) } catch { return }
                dismiss()
            }
    }
}

private struct ShopDetailSessionErrorView: View {
    let title: String
    let retry: String
    let back: String
    let onRetry: () -> Void
    let onBack: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Text(title).font(CoffeePeekNativeStyle.font(20, weight: "SemiBold", relativeTo: .title3))
            CoffeePeekNativeActionButton(title: retry, action: onRetry)
            Button(back, action: onBack)
        }.padding(24).frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(CoffeePeekNativeStyle.background).foregroundStyle(CoffeePeekNativeStyle.foreground)
    }
}

#Preview("Shop session error light") {
    ShopDetailSessionErrorView(title: "Не удалось загрузить кофейню", retry: "Повторить", back: "Назад", onRetry: {}, onBack: {})
        .preferredColorScheme(.light)
}
#Preview("Shop session error dark") {
    ShopDetailSessionErrorView(title: "Не удалось загрузить кофейню", retry: "Повторить", back: "Назад", onRetry: {}, onBack: {})
        .preferredColorScheme(.dark)
}

#Preview("Shop message light") {
    ShopDetailToastView(message: "Номер скопирован", dismissLabel: "Закрыть", dismiss: {}).preferredColorScheme(.light)
}
#Preview("Shop message dark") {
    ShopDetailToastView(message: "Номер скопирован", dismissLabel: "Закрыть", dismiss: {}).preferredColorScheme(.dark)
}

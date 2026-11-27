import SwiftUI
import UIKit

/// Shared native outcome presentation. Features supply status text and navigation actions.
struct CoffeePeekNativeSuccessScreen: View {
    let title: String
    let description: String
    let buttonTitle: String
    let closeLabel: String
    var notice: String? = nil
    var noticeIsError = false
    let onDismiss: () -> Void
    let action: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            header
            GeometryReader { geometry in
                ScrollView {
                    message.padding(.horizontal, 24).padding(.vertical, 20)
                        .frame(maxWidth: .infinity, minHeight: geometry.size.height)
                }.scrollBounceBehavior(.basedOnSize)
            }
            CoffeePeekNativeActionButton(title: buttonTitle, action: action).padding(16)
        }
        .font(CoffeePeekNativeStyle.font(16))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
    }

    private var header: some View {
        HStack(spacing: 8) {
            Button(action: onDismiss) {
                Image(systemName: "xmark").font(.system(size: 18, weight: .medium)).frame(width: 48, height: 48)
            }.buttonStyle(.plain).shopDetailGlassCircle().accessibilityLabel(closeLabel)
            Text(title).font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
                .multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity).accessibilityAddTraits(.isHeader)
            Color.clear.frame(width: 48, height: 48).accessibilityHidden(true)
        }.padding(.horizontal, 16).padding(.top, 8)
    }

    private var message: some View {
        VStack(spacing: 16) {
            if let image = Self.mascot {
                Image(uiImage: image).resizable().scaledToFit().frame(width: 180, height: 180)
                    .accessibilityHidden(true)
            }
            if !description.isEmpty {
                Text(description).foregroundStyle(CoffeePeekNativeStyle.secondary).lineSpacing(4)
            }
            if let notice, !notice.isEmpty {
                Text(notice).foregroundStyle(noticeIsError ? CoffeePeekNativeStyle.error : CoffeePeekNativeStyle.secondary)
                    .lineSpacing(4)
            }
        }.multilineTextAlignment(.center).frame(maxWidth: 480).fixedSize(horizontal: false, vertical: true)
    }

    private static let mascot: UIImage? = {
        Bundle.main.url(forResource: "shop_maskot_with_book", withExtension: "png")
            .flatMap { UIImage(contentsOfFile: $0.path) }
    }()
}

#Preview("Success screen light") {
    CoffeePeekNativeSuccessScreen(title: "Чекин добавлен", description: "Он сохранён в вашей истории.",
        buttonTitle: "Готово", closeLabel: "Закрыть", onDismiss: {}, action: {}).preferredColorScheme(.light)
}
#Preview("Success screen dark") {
    CoffeePeekNativeSuccessScreen(title: "Чэкін дададзены", description: "Ён захаваны ў вашай гісторыі.",
        buttonTitle: "Гатова", closeLabel: "Закрыць", onDismiss: {}, action: {}).preferredColorScheme(.dark)
}
#Preview("Success warning accessibility light") {
    CoffeePeekNativeSuccessScreen(title: "Чекин добавлен", description: "Он сохранён в вашей истории.",
        buttonTitle: "Готово", closeLabel: "Закрыть", notice: "Настройка аудитории не подтвердилась. Проверьте запись.",
        onDismiss: {}, action: {}).dynamicTypeSize(.accessibility2).preferredColorScheme(.light)
}
#Preview("Success warning accessibility dark") {
    CoffeePeekNativeSuccessScreen(title: "Чэкін дададзены", description: "Ён захаваны ў вашай гісторыі.",
        buttonTitle: "Гатова", closeLabel: "Закрыць", notice: "Не атрымалася пацвердзіць настройку аўдыторыі. Праверце запіс.",
        onDismiss: {}, action: {}).dynamicTypeSize(.accessibility2).preferredColorScheme(.dark)
}

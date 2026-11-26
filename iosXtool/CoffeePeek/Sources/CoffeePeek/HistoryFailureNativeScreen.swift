import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeHistoryFailureController(failure: IosHistoryFailure) -> UIViewController {
    UIHostingController(rootView: HistoryFailureNativeContent(message: failure.message,
        retryLabel: failure.retryLabel, backLabel: failure.backLabel, retry: failure.retry, back: failure.back))
}

struct HistoryFailureNativeContent: View {
    let message: String
    let retryLabel: String
    let backLabel: String
    let retry: () -> Void
    let back: () -> Void

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 12) {
                    Text(message).font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.error)
                        .fixedSize(horizontal: false, vertical: true).multilineTextAlignment(.center)
                    CoffeePeekNativeActionButton(title: retryLabel, action: retry)
                    CoffeePeekNativeActionButton(title: backLabel, action: back)
                }.frame(maxWidth: 480).padding(24)
                    .frame(maxWidth: .infinity, minHeight: geometry.size.height)
            }.scrollBounceBehavior(.basedOnSize)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
    }
}

#Preview("History failure light") {
    HistoryFailureNativeContent(message: "Не удалось загрузить данные", retryLabel: "Повторить", backLabel: "Назад", retry: {}, back: {})
        .preferredColorScheme(.light)
}

#Preview("History failure dark, large text") {
    HistoryFailureNativeContent(message: "Не удалось загрузить данные", retryLabel: "Повторить", backLabel: "Назад", retry: {}, back: {})
        .preferredColorScheme(.dark).environment(\.dynamicTypeSize, .accessibility3)
}

import AuthenticationServices
import SwiftUI
import UIKit

/// Uses Apple's own control while keeping authentication in the existing app flow.
struct CoffeePeekAppleSignInButton: View {
    let action: () -> Void
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        NativeAppleButton(style: colorScheme == .dark ? .white : .black, action: action)
            .id(colorScheme)
            .frame(maxWidth: .infinity)
            .frame(height: 48)
    }

    private struct NativeAppleButton: UIViewRepresentable {
        let style: ASAuthorizationAppleIDButton.Style
        let action: () -> Void

        func makeCoordinator() -> Coordinator { Coordinator(action: action) }

        func makeUIView(context: Context) -> ASAuthorizationAppleIDButton {
            let button = ASAuthorizationAppleIDButton(authorizationButtonType: .signIn, authorizationButtonStyle: style)
            button.cornerRadius = 24
            button.isEnabled = context.environment.isEnabled
            button.addTarget(context.coordinator, action: #selector(Coordinator.activate(_:)), for: .touchUpInside)
            return button
        }

        func updateUIView(_ button: ASAuthorizationAppleIDButton, context: Context) {
            context.coordinator.action = action
            button.isEnabled = context.environment.isEnabled
        }

        func sizeThatFits(_ proposal: ProposedViewSize, uiView: ASAuthorizationAppleIDButton, context: Context) -> CGSize? {
            CGSize(width: proposal.width ?? uiView.intrinsicContentSize.width, height: 48)
        }

        @MainActor
        final class Coordinator: NSObject {
            var action: () -> Void

            init(action: @escaping () -> Void) { self.action = action }

            @objc func activate(_ sender: ASAuthorizationAppleIDButton) {
                guard sender.isEnabled else { return }
                action()
            }
        }
    }
}

#Preview("Apple sign-in light") {
    CoffeePeekAppleSignInButton(action: {})
        .padding(24).background(CoffeePeekNativeStyle.background)
        .preferredColorScheme(.light)
}

#Preview("Apple sign-in dark") {
    CoffeePeekAppleSignInButton(action: {})
        .padding(24).background(CoffeePeekNativeStyle.background)
        .preferredColorScheme(.dark)
}

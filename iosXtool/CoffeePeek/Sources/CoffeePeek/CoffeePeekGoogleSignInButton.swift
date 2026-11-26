import SwiftUI

/// Keeps Google branding separate from the app tint and authentication flow.
struct CoffeePeekGoogleSignInButton: View {
    let title: String
    let action: () -> Void
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                logo
                Text(title)
                    .font(.body.weight(.medium))
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, minHeight: 48)
            .foregroundStyle(foregroundColor)
            .contentShape(Capsule())
        }
        .buttonStyle(CoffeePeekGoogleSignInButtonStyle(
            fillColor: fillColor, borderColor: borderColor, foregroundColor: foregroundColor
        ))
    }

    private var logo: some View {
        // Original Google asset: https://developers.google.com/identity/branding-guidelines
        Image("GoogleSignInLogo")
            .renderingMode(.original)
            .resizable()
            .scaledToFit()
            .frame(width: 20, height: 20)
            .background(.white, in: Circle())
            .accessibilityHidden(true)
    }

    private var fillColor: Color {
        CoffeePeekNativeStyle.color(colorScheme == .dark ? 0x131314 : 0xFFFFFF)
    }

    private var borderColor: Color {
        CoffeePeekNativeStyle.color(colorScheme == .dark ? 0x8E918F : 0x747775)
    }

    private var foregroundColor: Color {
        CoffeePeekNativeStyle.color(colorScheme == .dark ? 0xE3E3E3 : 0x1F1F1F)
    }
}

private struct CoffeePeekGoogleSignInButtonStyle: ButtonStyle {
    let fillColor: Color
    let borderColor: Color
    let foregroundColor: Color
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background {
                Capsule().fill(fillColor)
                    .overlay {
                        Capsule().fill(foregroundColor.opacity(configuration.isPressed && isEnabled ? 0.08 : 0))
                    }
            }
            .overlay { Capsule().strokeBorder(borderColor, lineWidth: 1).allowsHitTesting(false) }
            .opacity(isEnabled ? 1 : 0.55)
    }
}

@MainActor
private func googleSignInButtonPreview() -> some View {
    VStack(spacing: 16) {
        CoffeePeekGoogleSignInButton(title: "Войти через Google", action: {})
        CoffeePeekGoogleSignInButton(title: "Войти через Google", action: {}).disabled(true)
    }
    .padding(24)
    .background(CoffeePeekNativeStyle.background)
}

#Preview("Google sign-in light") {
    googleSignInButtonPreview().preferredColorScheme(.light)
}

#Preview("Google sign-in dark") {
    googleSignInButtonPreview().preferredColorScheme(.dark)
}

#Preview("Google sign-in large text") {
    googleSignInButtonPreview().dynamicTypeSize(.accessibility3).preferredColorScheme(.dark)
}

import SwiftUI
import CoreText
import UIKit

// Native rendering of CoffeePeekColors, CoffeePeekDimens and CoffeePeekIconBadgePalette.
// Keep these values aligned with core/design-system; no UIKit grouped-list defaults.
enum CoffeePeekNativeStyle {
    static let background = adaptive(0xFAFAF9, 0x1A1412)
    static let surface = adaptive(0xFFFFFF, 0x2D241F)
    static let foreground = adaptive(0x1C1917, 0xFFFFFF)
    static let secondary = adaptive(0x6B655F, 0xA39E93)
    static let border = adaptive(0xE5E1DA, 0x3D2F28)
    static let accent = color(0xFACC15)

    // Root-tab titles match design-system headlineMedium; pushed navigation stays compact.
    static var rootHeadingFont: Font { font(28, weight: "Bold", relativeTo: .title) }

    // The bundled static weights share one PostScript name. Loading the actual
    // font data avoids global registration collisions and preserves each weight.
    private static let graphicsFonts: [String: CGFont] = {
        Dictionary(uniqueKeysWithValues: ["regular", "medium", "semibold", "bold"].compactMap { weight in
            loadGraphicsFont(weight).map { (weight, $0) }
        })
    }()

    static func font(_ size: CGFloat, weight: String = "Regular", relativeTo: Font.TextStyle = .body) -> Font {
        let scaledSize = UIFontMetrics(forTextStyle: textStyle(relativeTo)).scaledValue(for: size)
        guard let graphicsFont = graphicsFonts[weight.lowercased()] else {
            return .system(size: scaledSize, weight: fallbackWeight(weight))
        }
        return Font(CTFontCreateWithGraphicsFont(graphicsFont, scaledSize, nil, nil))
    }

    private static func loadGraphicsFont(_ weight: String) -> CGFont? {
        let resource = "com.coffeepeek.core.designsystem.resources/font"
        for directory in ["compose-resources/composeResources/\(resource)", "compose-resources/\(resource)"] {
            if let url = Bundle.main.url(forResource: "manrope_\(weight)", withExtension: "ttf", subdirectory: directory),
               let provider = CGDataProvider(url: url as CFURL), let font = CGFont(provider) {
                return font
            }
        }
        return nil
    }

    private static func textStyle(_ style: Font.TextStyle) -> UIFont.TextStyle {
        switch style {
        case .largeTitle: .largeTitle
        case .title: .title1
        case .title2: .title2
        case .title3: .title3
        case .headline: .headline
        case .subheadline: .subheadline
        case .callout: .callout
        case .footnote: .footnote
        case .caption: .caption1
        case .caption2: .caption2
        default: .body
        }
    }

    private static func fallbackWeight(_ weight: String) -> Font.Weight {
        switch weight.lowercased() {
        case "bold": .bold
        case "semibold": .semibold
        case "medium": .medium
        default: .regular
        }
    }

    static func color(_ hex: UInt32) -> Color {
        Color(red: Double((hex >> 16) & 255) / 255, green: Double((hex >> 8) & 255) / 255, blue: Double(hex & 255) / 255)
    }

    private static func adaptive(_ light: UInt32, _ dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            let hex = traits.userInterfaceStyle == .dark ? dark : light
            return UIColor(red: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255,
                           blue: CGFloat(hex & 255) / 255, alpha: 1)
        })
    }

}

private func nativeRootHeadingPreview() -> some View {
    VStack(alignment: .leading, spacing: 16) {
        Text("Настройки")
        HStack {
            Text("Профиль")
            Spacer()
            Image(systemName: "ellipsis").font(.system(size: 20)).frame(width: 48, height: 48)
        }
    }
    .font(CoffeePeekNativeStyle.rootHeadingFont)
    .foregroundStyle(CoffeePeekNativeStyle.foreground)
    .padding(16)
    .background(CoffeePeekNativeStyle.background)
}

#Preview("Root headings light") {
    nativeRootHeadingPreview().preferredColorScheme(.light)
}

#Preview("Root headings dark") {
    nativeRootHeadingPreview().preferredColorScheme(.dark)
}

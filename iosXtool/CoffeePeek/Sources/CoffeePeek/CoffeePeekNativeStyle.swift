import SwiftUI
import CoreText
import UIKit

// Native rendering of CoffeePeekColors, CoffeePeekDimens and CoffeePeekIconBadgePalette.
// Keep these values aligned with core/design-system; no UIKit grouped-list defaults.
@MainActor
enum CoffeePeekNativeStyle {
    static let background = adaptive(0xFAFAF9, 0x1A1412)
    static let surface = adaptive(0xFFFFFF, 0x2D241F)
    static let surfaceVariant = adaptive(0xF3F4F6, 0x3D2F28)
    static let foreground = adaptive(0x1C1917, 0xFFFFFF)
    static let secondary = adaptive(0x6B655F, 0xA39E93)
    static let border = adaptive(0xE5E1DA, 0x3D2F28)
    static let accent = color(0xFACC15)
    static let secondaryAccent = adaptive(0x8C5A00, 0xFACC15)
    static let onAccent = color(0x1A1412)
    static let error = adaptive(0xB91C1C, 0xFCA5A5)

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

struct CoffeePeekNativeRootHeader<Trailing: View>: View {
    let title: String
    @ViewBuilder let trailing: Trailing

    var body: some View {
        HStack(spacing: 12) {
            Text(title).font(CoffeePeekNativeStyle.rootHeadingFont)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.isHeader)
            trailing
        }
        .frame(minHeight: 48)
        .padding(.vertical, 12)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
    }
}

struct CoffeePeekNativeNavigationHeader: View {
    let title: String
    let backLabel: String
    let back: (() -> Void)?

    var body: some View {
        HStack(spacing: 8) {
            if let back {
                Button(action: back) {
                    Image(systemName: "chevron.backward").font(.system(size: 22))
                        .frame(width: 44, height: 44).contentShape(Rectangle())
                }.buttonStyle(.plain).shopDetailGlassCircle().accessibilityLabel(backLabel)
            } else { Color.clear.frame(width: 44, height: 44).accessibilityHidden(true) }
            Text(title).font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
                .multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity).accessibilityAddTraits(.isHeader)
            Color.clear.frame(width: 44, height: 44).accessibilityHidden(true)
        }.padding(.horizontal, 12).padding(.vertical, 8)
            .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
    }
}

struct CoffeePeekNativeChevronButton: View {
    let label: String
    var symbol = "chevron.right"
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol).font(.system(size: 20))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .frame(width: 44, height: 44).contentShape(Circle())
        }.buttonStyle(.plain).shopDetailGlassCircle().accessibilityLabel(label)
    }
}

struct CoffeePeekNativeContactIcon: View {
    let kind: String

    var body: some View {
        Group {
            if kind == "instagram" {
                ZStack {
                    RoundedRectangle(cornerRadius: 6).stroke(lineWidth: 2).padding(3)
                    Circle().stroke(lineWidth: 2).padding(8)
                    Circle().frame(width: 3, height: 3).offset(x: 6, y: -6)
                }
            } else {
                Image(systemName: symbol).resizable().scaledToFit().padding(2)
            }
        }.frame(width: 28, height: 28).accessibilityHidden(true)
    }

    private var symbol: String {
        switch kind {
        case "phone": "phone"
        case "email": "envelope"
        default: "globe"
        }
    }
}

struct CoffeePeekNativeContactButton: View {
    let title: String
    let kind: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                CoffeePeekNativeContactIcon(kind: kind)
                Text(title).fixedSize(horizontal: false, vertical: true)
            }.padding(.horizontal, 16).frame(minHeight: 48).contentShape(Capsule())
        }.buttonStyle(.plain).foregroundStyle(CoffeePeekNativeStyle.foreground)
            .font(CoffeePeekNativeStyle.font(17, weight: "SemiBold", relativeTo: .headline))
            .overlay { Capsule().strokeBorder(CoffeePeekNativeStyle.border).allowsHitTesting(false) }
    }
}

@MainActor
private func nativeContactControlsPreview() -> some View {
    VStack(spacing: 16) {
        CoffeePeekNativeContactButton(title: "@coffeepeek", kind: "instagram", action: {})
        CoffeePeekNativeContactButton(title: "+375 00 000-00-00", kind: "phone", action: {})
        CoffeePeekNativeChevronButton(label: "Все чекины", action: {})
    }.padding().background(CoffeePeekNativeStyle.background)
}
#Preview("Contacts and disclosure light") { nativeContactControlsPreview().preferredColorScheme(.light) }
#Preview("Contacts and disclosure dark") { nativeContactControlsPreview().preferredColorScheme(.dark) }

#Preview("Native navigation light") {
    CoffeePeekNativeNavigationHeader(title: "Добавить кофейню", backLabel: "Назад", back: {}).preferredColorScheme(.light)
}
#Preview("Native navigation dark") {
    CoffeePeekNativeNavigationHeader(title: "Добавить обжарщика", backLabel: "Назад", back: {}).preferredColorScheme(.dark)
}

@MainActor
private func nativeRootHeadingPreview() -> some View {
    VStack(alignment: .leading, spacing: 0) {
        CoffeePeekNativeRootHeader(title: "Профиль") {
            Image(systemName: "ellipsis").font(.system(size: 20)).frame(width: 48, height: 48)
        }
        CoffeePeekNativeRootHeader(title: "Лента") { Color.clear.frame(width: 56, height: 48) }
        CoffeePeekNativeRootHeader(title: "Настройки") { EmptyView() }
    }
    .padding(.horizontal, 16)
    .background(CoffeePeekNativeStyle.background)
}

#Preview("Root headings light") {
    nativeRootHeadingPreview().preferredColorScheme(.light)
}

#Preview("Root headings dark") {
    nativeRootHeadingPreview().preferredColorScheme(.dark)
}

#Preview("Root headings large text") {
    nativeRootHeadingPreview().dynamicTypeSize(.accessibility2).preferredColorScheme(.dark)
}

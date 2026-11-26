import SwiftUI

@MainActor
enum SettingsNativeStyle {
    static func heading(_ title: String, description: String) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2)).accessibilityAddTraits(.isHeader)
            Text(description).font(CoffeePeekNativeStyle.font(17)).foregroundStyle(CoffeePeekNativeStyle.secondary).fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, 8)
    }

    static func section<Content: View>(_ title: String? = nil, description: String? = nil,
                                      @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            if let title {
                Text(title.uppercased()).font(CoffeePeekNativeStyle.font(13, relativeTo: .footnote))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary).accessibilityAddTraits(.isHeader)
            }
            if let description { Text(description).font(CoffeePeekNativeStyle.font(14)).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            VStack(spacing: 0, content: content).settingsCard()
        }
    }

    static var divider: some View {
        Rectangle().fill(CoffeePeekNativeStyle.border).frame(height: 0.5).accessibilityHidden(true)
    }
}

extension View {
    func settingsCard() -> some View {
        frame(maxWidth: .infinity, alignment: .leading)
            .background(CoffeePeekNativeStyle.surface)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(CoffeePeekNativeStyle.border, lineWidth: 1))
    }

    func settingsAppearance() -> some View {
        font(CoffeePeekNativeStyle.font(17))
            .foregroundStyle(CoffeePeekNativeStyle.foreground)
            .tint(CoffeePeekNativeStyle.accent)
            .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
    }
}

struct SettingsNativeStrings {
    private let bundle: Bundle
    private let locale: Locale

    init(languageTag: String) {
        let localization = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = localization.flatMap { Bundle(url: $0) } ?? .main
        locale = Locale(identifier: languageTag)
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "SettingsNative")
    }

    func version(_ value: String) -> String {
        String(format: text("settings_version"), locale: locale, value)
    }
}

struct SettingsNativePage<Content: View>: View {
    let title: String
    let backLabel: String
    let back: () -> Void
    @ViewBuilder let content: () -> Content

    init(title: String, strings: SettingsNativeStrings, back: @escaping () -> Void,
         @ViewBuilder content: @escaping () -> Content) {
        self.init(title: title, backLabel: strings.text("settings_back"), back: back, content: content)
    }

    init(title: String, backLabel: String, back: @escaping () -> Void,
         @ViewBuilder content: @escaping () -> Content) {
        self.title = title
        self.backLabel = backLabel
        self.back = back
        self.content = content
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16, content: content)
                .padding(16)
                .padding(.bottom, 24)
        }
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .settingsAppearance()
    }

    private var header: some View {
        CoffeePeekNativeNavigationHeader(title: title,
            backLabel: backLabel, back: back)
    }
}

struct SettingsNativeRow: View {
    let title: String
    var description: String? = nil
    var systemImage: String? = nil
    var value: String? = nil
    var navigates: Bool = false
    var busy: Bool = false
    let action: () -> Void
    @Environment(\.dynamicTypeSize) private var textSize

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                if let systemImage {
                    SettingsNativeLabel.icon(systemImage)
                }
                VStack(alignment: .leading, spacing: 4) {
                    SettingsNativeLabel(title: title, description: description)
                    if textSize.isAccessibilitySize, let value {
                        Text(value).font(CoffeePeekNativeStyle.font(15)).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if let value, !textSize.isAccessibilitySize {
                    Text(value)
                        .font(CoffeePeekNativeStyle.font(15))
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                        .multilineTextAlignment(.trailing)
                        .frame(maxWidth: 110, alignment: .trailing)
                        .fixedSize(horizontal: true, vertical: false)
                }
                if busy {
                    CoffeePeekNativeLoader(size: 24).accessibilityHidden(true)
                } else if navigates {
                    Image(systemName: "chevron.forward")
                        .font(.system(size: 17, weight: .regular))
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                        .accessibilityHidden(true)
                }
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .frame(minHeight: 64)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(busy)
    }
}

struct SettingsNativeLabel: View {
    let title: String
    var description: String? = nil
    var descriptionSize: CGFloat = 14

    var body: some View {
        VStack(alignment: .leading, spacing: descriptionSize > 14 ? 8 : 4) {
            Text(title).font(CoffeePeekNativeStyle.font(17)).foregroundStyle(CoffeePeekNativeStyle.foreground)
            if let description {
                Text(description).font(CoffeePeekNativeStyle.font(descriptionSize)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    static func icon(_ name: String) -> some View { Badge(name: name) }

    static func toggle(_ title: String, description: String, icon: String,
                       isOn: Binding<Bool>, longDescription: Bool = false) -> some View {
        Toggle(isOn: isOn) {
            HStack(alignment: longDescription ? .top : .center, spacing: 12) {
                Self.icon(icon)
                SettingsNativeLabel(title: title, description: description, descriptionSize: longDescription ? 15 : 14)
            }
        }
        .toggleStyle(.switch)
        .tint(CoffeePeekNativeStyle.accent)
        .padding(.horizontal, 16).padding(.vertical, longDescription ? 16 : 12)
    }

    private struct Badge: View {
        let name: String
        @Environment(\.colorScheme) private var scheme
        @ScaledMetric(relativeTo: .body) private var size = 40.0
        private var colors: (UInt32, UInt32) {
            switch name {
            case "location", "mappin": (0xDDFAF8, 0x257D78)
            case "globe", "bean": (0xECFAED, 0x287D3B)
            case "business", "desktopcomputer", "square.and.arrow.up": (0xD3EFFA, 0x247A8B)
            case "theme", "moon": (0xF5F3FF, 0x7C3AED)
            case "shield", "doc.text": (0xFDF9E3, 0xE8C52A)
            case "lock.shield": (0xECFDF5, 0x087F5B)
            default: (0xFEF3C7, 0x8A6300)
            }
        }
        var body: some View {
            symbol.font(.system(size: 22, weight: .regular))
                .foregroundStyle(CoffeePeekNativeStyle.color(scheme == .dark ? lightenedIcon : colors.1))
                .frame(width: size, height: size)
                .background(CoffeePeekNativeStyle.color(scheme == .dark ? colors.1 : colors.0)
                    .opacity(scheme == .dark ? 0.2 : 1), in: RoundedRectangle(cornerRadius: 12))
                .accessibilityHidden(true)
        }
        private var lightenedIcon: UInt32 {
            let channels = [16, 8, 0].map { UInt32(Double((colors.1 >> $0) & 255) * 0.65 + 255 * 0.35) }
            return (channels[0] << 16) | (channels[1] << 8) | channels[2]
        }
        @ViewBuilder private var symbol: some View {
            switch name {
            case "bean", "business":
                ZStack {
                    Ellipse().stroke(lineWidth: 1.5).frame(width: 14, height: 21).rotationEffect(.degrees(35))
                    Path { path in
                        path.move(to: CGPoint(x: 7, y: 0))
                        path.addCurve(to: CGPoint(x: 7, y: 21), control1: CGPoint(x: 18, y: 7), control2: CGPoint(x: -4, y: 14))
                    }.stroke(lineWidth: 1.3).frame(width: 14, height: 21).rotationEffect(.degrees(35))
                }.frame(width: 22, height: 22)
            case "location": Image(systemName: "mappin.and.ellipse")
            case "theme": Image(systemName: "sun.max")
            default: Image(systemName: name)
            }
        }
    }
}

struct SettingsSelectionRow: View {
    let title: String
    let selected: Bool
    var systemImage: String? = nil
    let select: () -> Void

    var body: some View {
        Button(action: select) {
            HStack(spacing: 12) {
                if let systemImage {
                    SettingsNativeLabel.icon(systemImage)
                }
                Text(title).font(CoffeePeekNativeStyle.font(17)).foregroundStyle(CoffeePeekNativeStyle.foreground)
                Spacer(minLength: 0)
                if selected {
                    Image(systemName: "checkmark")
                        .foregroundStyle(CoffeePeekNativeStyle.accent)
                        .accessibilityHidden(true)
                }
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .frame(minHeight: 64)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

struct SettingsRetryRow: View {
    let message: String
    let retry: String
    let action: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(message).foregroundStyle(.secondary)
            Button(action: action) {
                Text(retry)
                    .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                    .contentShape(Rectangle())
            }
        }
        .padding(16)
    }
}

@MainActor
private func settingsComponentsPreview(languageTag: String) -> some View {
    let strings = SettingsNativeStrings(languageTag: languageTag)
    return SettingsNativePage(title: strings.text("settings_tab_title"), strings: strings, back: {}) {
        settingsNavigationPreview(strings: strings, languageTag: languageTag)
        settingsLanguagePreview(strings: strings, languageTag: languageTag)
    }
}

@MainActor
private func settingsNavigationPreview(strings: SettingsNativeStrings, languageTag: String) -> some View {
    SettingsNativeStyle.section(strings.text("settings_section_settings")) {
        SettingsNativeRow(
            title: strings.text("settings_city_row"),
            description: strings.text("settings_city_row_description"),
            systemImage: "location", value: languageTag == "be" ? "Мінск" : "Минск", navigates: true
        ) {}
        SettingsNativeStyle.divider
        SettingsNativeRow(
            title: strings.text("settings_update_checking"), systemImage: "arrow.down.circle", busy: true
        ) {}
    }
}

@MainActor
private func settingsLanguagePreview(strings: SettingsNativeStrings, languageTag: String) -> some View {
    SettingsNativeStyle.section(strings.text("settings_language_title")) {
        SettingsSelectionRow(
            title: strings.text("settings_language_russian"), selected: languageTag == "ru"
        ) {}
        SettingsNativeStyle.divider
        SettingsSelectionRow(
            title: strings.text("settings_language_belarusian"), selected: languageTag == "be"
        ) {}
        SettingsRetryRow(
            message: strings.text("settings_language_save_error"), retry: strings.text("settings_retry"), action: {}
        )
    }
}

#Preview("Settings components light") {
    settingsComponentsPreview(languageTag: "ru").preferredColorScheme(.light)
}

#Preview("Settings components dark") {
    settingsComponentsPreview(languageTag: "be").preferredColorScheme(.dark)
}

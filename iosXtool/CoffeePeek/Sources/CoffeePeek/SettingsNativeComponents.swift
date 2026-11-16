import SwiftUI

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
    let strings: SettingsNativeStrings
    let back: () -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        List(content: content)
            .listStyle(.insetGrouped)
            .safeAreaInset(edge: .top, spacing: 0) {
                HStack(spacing: 8) {
                    Button(action: back) {
                        Label(strings.text("settings_back"), systemImage: "chevron.backward")
                            .labelStyle(.iconOnly)
                            .frame(minWidth: 44, minHeight: 44)
                            .contentShape(Rectangle())
                    }
                    Text(title)
                        .font(.headline)
                        .accessibilityAddTraits(.isHeader)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 12)
                .background(.bar)
            }
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

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                if let systemImage {
                    Image(systemName: systemImage)
                        .foregroundStyle(Color.accentColor)
                        .accessibilityHidden(true)
                }
                SettingsNativeLabel(title: title, description: description)
                Spacer(minLength: 0)
                if let value {
                    Text(value)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                if busy {
                    ProgressView().accessibilityHidden(true)
                } else if navigates {
                    Image(systemName: "chevron.forward")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tertiary)
                        .accessibilityHidden(true)
                }
            }
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(busy)
    }
}

struct SettingsNativeLabel: View {
    let title: String
    var description: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).foregroundStyle(.primary)
            if let description {
                Text(description).font(.footnote).foregroundStyle(.secondary)
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
            HStack {
                if let systemImage {
                    Image(systemName: systemImage).accessibilityHidden(true)
                }
                Text(title).foregroundStyle(.primary)
                Spacer(minLength: 0)
                if selected {
                    Image(systemName: "checkmark")
                        .foregroundStyle(Color.accentColor)
                        .accessibilityHidden(true)
                }
            }
            .frame(minHeight: 44)
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
            Button(retry, action: action)
        }
    }
}

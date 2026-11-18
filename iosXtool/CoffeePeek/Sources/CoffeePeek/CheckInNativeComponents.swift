import SwiftUI
import UIKit

struct CheckInNativeStrings {
    let languageTag: String
    let table: String

    subscript(_ key: String) -> String {
        let bundle = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
            .flatMap(Bundle.init(url:)) ?? .main
        return bundle.localizedString(forKey: key, value: nil, table: table)
    }

    func format(_ key: String, _ values: CVarArg...) -> String {
        String(format: self[key].replacingOccurrences(of: "$s", with: "$@"),
               locale: Locale(identifier: languageTag), arguments: values)
    }
}

struct CheckInNativeField<Content: View>: View {
    let title: String
    var error: String? = nil
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if !title.isEmpty { Text(title).font(CoffeePeekNativeStyle.font(15, weight: "Medium")) }
            content
            if let error { Text(error).font(CoffeePeekNativeStyle.font(12)).foregroundStyle(.red) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct CheckInNativeInput: View {
    let title: String
    let placeholder: String
    @Binding var value: String
    var multiline = false
    var keyboard: UIKeyboardType = .default

    var body: some View {
        TextField(placeholder, text: $value, axis: multiline ? .vertical : .horizontal)
            .lineLimit(multiline ? 4...8 : 1...1)
            .keyboardType(keyboard)
            .font(CoffeePeekNativeStyle.font(16))
            .padding(.horizontal, 16).padding(.vertical, 12)
            .frame(minHeight: 48, alignment: .topLeading)
            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: multiline ? 20 : 24))
            .overlay(RoundedRectangle(cornerRadius: multiline ? 20 : 24).stroke(CoffeePeekNativeStyle.border))
            .accessibilityLabel(title)
    }
}

struct CheckInNativeRating: View {
    let title: String
    let value: Int?
    let label: (Int) -> String
    let select: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(CoffeePeekNativeStyle.font(17, weight: "SemiBold"))
            HStack(spacing: 8) {
                ForEach(1...5, id: \.self) { score in
                    Button { select(score) } label: {
                        Image(systemName: score <= (value ?? 0) ? "star.fill" : "star")
                            .font(.system(size: 24)).frame(maxWidth: .infinity, minHeight: 44)
                    }
                    .buttonStyle(.plain).foregroundStyle(CoffeePeekNativeStyle.accent)
                    .accessibilityLabel(label(score))
                    .accessibilityAddTraits(value == score ? .isSelected : [])
                }
            }
        }
    }
}

struct CheckInNativePrimaryButton: View {
    let title: String
    var busy = false
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if busy { ProgressView().tint(CoffeePeekNativeStyle.foreground) }
                Text(title).font(CoffeePeekNativeStyle.font(17, weight: "SemiBold"))
            }
            .frame(maxWidth: .infinity, minHeight: 48)
        }
        .buttonStyle(.plain).foregroundStyle(Color.black.opacity(enabled ? 0.9 : 0.55))
        .background(CoffeePeekNativeStyle.accent.opacity(enabled ? 1 : 0.45), in: Capsule())
        .disabled(!enabled || busy)
    }
}

#Preview("Check-in fields light") {
    VStack(spacing: 24) {
        CheckInNativeField(title: "Напиток") {
            CheckInNativeInput(title: "Напиток", placeholder: "Название", value: .constant(""))
        }
        CheckInNativeRating(title: "Оценка", value: 3, label: { "\($0) / 5" }, select: { _ in })
        CheckInNativePrimaryButton(title: "Опубликовать", action: {})
    }.padding().background(CoffeePeekNativeStyle.background).preferredColorScheme(.light)
}

#Preview("Check-in fields dark") {
    VStack(spacing: 24) {
        CheckInNativeField(title: "Напиток") {
            CheckInNativeInput(title: "Напиток", placeholder: "Название", value: .constant(""))
        }
        CheckInNativeRating(title: "Оценка", value: nil, label: { "\($0) / 5" }, select: { _ in })
        CheckInNativePrimaryButton(title: "Опубликовать", enabled: false, action: {})
    }.padding().background(CoffeePeekNativeStyle.background).preferredColorScheme(.dark)
}

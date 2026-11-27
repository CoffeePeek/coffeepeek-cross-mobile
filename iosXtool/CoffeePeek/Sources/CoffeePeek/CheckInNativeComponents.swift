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

@MainActor
enum CheckInNativeFormStyle {
    case standard, home

    static let homeOutline = CoffeePeekNativeStyle.adaptive(0xE7E5E4, 0x3D2F28)
}

struct CheckInNativeField<Content: View>: View {
    let title: String
    var error: String? = nil
    var style: CheckInNativeFormStyle = .standard
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: style == .home ? 4 : 8) {
            if !title.isEmpty {
                if style == .home { label.foregroundStyle(CoffeePeekNativeStyle.secondary) }
                else { label }
            }
            content
            if let error { errorLabel(error) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var label: some View { Text(title).font(CoffeePeekNativeStyle.font(15, weight: "Medium")) }

    @ViewBuilder private func errorLabel(_ message: String) -> some View {
        if style == .home {
            HStack(alignment: .top, spacing: 6) {
                Image(systemName: "exclamationmark.circle").font(.system(size: 18)).accessibilityHidden(true)
                Text(message).font(CoffeePeekNativeStyle.font(13))
            }.foregroundStyle(CoffeePeekNativeStyle.error)
        } else { Text(message).font(CoffeePeekNativeStyle.font(12)).foregroundStyle(.red) }
    }
}

struct CheckInNativeInput: View {
    let title: String
    let placeholder: String
    @Binding var value: String
    var multiline = false
    var keyboard: UIKeyboardType = .default
    var style: CheckInNativeFormStyle = .standard
    var error: String? = nil
    @FocusState private var focused: Bool

    var body: some View {
        if style == .home && !multiline { input.submitLabel(.done).onSubmit { focused = false } }
        else { input }
    }

    private var input: some View {
        TextField(placeholder, text: $value,
            prompt: style == .home ? Text(placeholder).foregroundStyle(CoffeePeekNativeStyle.secondary) : nil,
            axis: multiline ? .vertical : .horizontal)
            .lineLimit(multiline ? (style == .home ? 1...8 : 4...8) : 1...1)
            .keyboardType(keyboard)
            .font(CoffeePeekNativeStyle.font(style == .home ? 15 : 16))
            .focused($focused)
            .padding(.horizontal, style == .home ? 14 : 16).padding(.vertical, verticalPadding)
            .frame(minHeight: minimumHeight, alignment: style == .home && !multiline ? .leading : .topLeading)
            .background(background, in: RoundedRectangle(cornerRadius: radius))
            .overlay(RoundedRectangle(cornerRadius: radius).stroke(outline))
            .accessibilityLabel(title)
    }

    private var verticalPadding: CGFloat { style == .home && !multiline ? 0 : 12 }
    private var minimumHeight: CGFloat { style == .home ? (multiline ? 80 : 44) : 48 }
    private var radius: CGFloat { style == .home ? (multiline ? 24 : 22) : (multiline ? 20 : 24) }
    private var background: Color {
        guard style == .home else { return CoffeePeekNativeStyle.surface }
        return error != nil && !multiline ? CoffeePeekNativeStyle.error.opacity(0.05) : CoffeePeekNativeStyle.foreground.opacity(0.04)
    }
    private var outline: Color {
        guard style == .home else { return CoffeePeekNativeStyle.border }
        return error == nil ? CheckInNativeFormStyle.homeOutline : CoffeePeekNativeStyle.error
    }
}

#Preview("Home inputs light") { homeInputsPreview().preferredColorScheme(.light) }
#Preview("Home inputs dark") { homeInputsPreview().preferredColorScheme(.dark) }

@MainActor
private func homeInputsPreview() -> some View {
    let strings = CheckInNativeStrings(languageTag: "ru", table: "NativeHomeCheckIn")
    return VStack(spacing: 20) {
        CheckInNativeField(title: strings["home_check_in_drink"], style: .home) {
            CheckInNativeInput(title: strings["home_check_in_drink"], placeholder: strings["home_check_in_drink_hint"],
                value: .constant(""), style: .home)
        }
        CheckInNativeField(title: strings["home_check_in_editor_note"], style: .home) {
            CheckInNativeInput(title: strings["home_check_in_editor_note"], placeholder: strings["home_check_in_note_hint"],
                value: .constant(""), multiline: true, style: .home)
        }
        CheckInNativeField(title: strings["home_check_in_dose_label"], error: strings["home_check_in_error_dose"], style: .home) {
            CheckInNativeInput(title: strings["home_check_in_dose_label"], placeholder: strings["home_check_in_dose_hint"],
                value: .constant("0"), keyboard: .decimalPad, style: .home, error: strings["home_check_in_error_dose"])
        }
    }.padding(16).background(CoffeePeekNativeStyle.background)
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
                if busy { CoffeePeekNativeLoader(color: .black, size: 24) }
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

struct CoffeePeekNativeRating: View {
    let value: Double
    let valueText: String
    let label: String

    private var roundedValue: Double { value.isFinite ? (min(5, max(0, value)) * 10).rounded() / 10 : 0 }

    var body: some View {
        HStack(spacing: 8) {
            HStack(spacing: 4) {
                ForEach(0..<5) { index in
                    CoffeePeekNativeRatingStar(fill: min(1, max(0, roundedValue - Double(index))))
                        .frame(width: 22, height: 22)
                }
            }
            Text(valueText).font(CoffeePeekNativeStyle.font(15))
        }.accessibilityElement(children: .ignore).accessibilityLabel(label)
    }
}

private struct CoffeePeekNativeRatingStar: View {
    let fill: Double

    var body: some View {
        Group {
            if fill >= 1 { symbol("star.fill").foregroundStyle(CoffeePeekNativeStyle.accent) }
            else {
                symbol("star").foregroundStyle(CoffeePeekNativeStyle.secondary)
                    .overlay {
                        if fill > 0 {
                            symbol("star.fill").foregroundStyle(CoffeePeekNativeStyle.accent)
                                .mask(alignment: .leading) {
                                    GeometryReader { geometry in
                                        Rectangle().frame(width: geometry.size.width * fill)
                                    }
                                }
                        }
                    }
            }
        }
    }

    private func symbol(_ name: String) -> some View {
        Image(systemName: name).resizable().scaledToFit().symbolRenderingMode(.monochrome)
    }
}

@MainActor
private func nativeRatingsPreview() -> some View {
    VStack(alignment: .leading, spacing: 16) {
        ForEach([0, 1, 3.7, 4.5, 4.96, 5], id: \.self) { value in
            CoffeePeekNativeRating(value: value, valueText: String(format: "%.1f", value), label: "Rating")
        }
    }.padding(16).foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
}

#Preview("Native ratings light") { nativeRatingsPreview().preferredColorScheme(.light) }
#Preview("Native ratings dark") { nativeRatingsPreview().preferredColorScheme(.dark) }

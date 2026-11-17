import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCheckInCreatePickerController(picker: IosCheckInCreatePicker) -> UIViewController {
    CheckInCreatePickerController(picker: picker)
}

private final class CheckInCreatePickerController: UIHostingController<CheckInCreatePickerView>,
    UIAdaptivePresentationControllerDelegate {
    private let picker: IosCheckInCreatePicker

    init(picker: IosCheckInCreatePicker) {
        self.picker = picker
        super.init(rootView: CheckInCreatePickerView(
            strings: CheckInCreatePickerStrings(languageTag: picker.languageTag),
            dismiss: { picker.dismiss() },
            shop: { picker.chooseShop() },
            home: { picker.chooseHome() },
            shelfBag: { picker.chooseShelfBag() }
        ))
        modalPresentationStyle = .pageSheet
        sheetPresentationController?.detents = [.medium(), .large()]
        sheetPresentationController?.prefersGrabberVisible = true
        sheetPresentationController?.prefersEdgeAttachedInCompactHeight = true
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { return nil }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        presentationController?.delegate = self
        picker.visibilityChanged(visible: true)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        picker.visibilityChanged(visible: false)
    }

    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        picker.dismiss()
    }
}

private struct CheckInCreatePickerView: View {
    let strings: CheckInCreatePickerStrings
    let dismiss: () -> Void
    let shop: () -> Void
    let home: () -> Void
    let shelfBag: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                heading
                choice("shop", icon: "location.fill", action: shop)
                choice("home", icon: "cup.and.saucer.fill", action: home)
                choice("shelf", icon: "camera.fill", action: shelfBag)
            }
            .padding(.horizontal, 16)
            .padding(.top, 24)
            .padding(.bottom, 24)
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
        }
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
    }

    private var heading: some View {
        HStack(spacing: 8) {
            Text(strings.text("title"))
                .font(CoffeePeekNativeStyle.font(24, weight: "Bold", relativeTo: .title2))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .accessibilityAddTraits(.isHeader)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: dismiss) {
                Image(systemName: "xmark")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(CoffeePeekNativeStyle.foreground)
                    .frame(width: 44, height: 44)
                    .background(CoffeePeekNativeStyle.surface, in: Circle())
                    .overlay(Circle().strokeBorder(CoffeePeekNativeStyle.border))
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(strings.text("close")))
        }
    }

    private func choice(_ key: String, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(.black)
                    .frame(width: 40, height: 40)
                    .background(CoffeePeekNativeStyle.accent, in: RoundedRectangle(cornerRadius: 12))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 3) {
                    Text(strings.text(key))
                        .font(CoffeePeekNativeStyle.font(16, weight: "Semibold"))
                        .foregroundStyle(CoffeePeekNativeStyle.foreground)
                    Text(strings.text("\(key).hint"))
                        .font(CoffeePeekNativeStyle.font(13, relativeTo: .footnote))
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(12)
            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18).strokeBorder(CoffeePeekNativeStyle.border))
            .contentShape(RoundedRectangle(cornerRadius: 18))
        }
        .buttonStyle(.plain)
    }
}

private struct CheckInCreatePickerStrings {
    private let bundle: Bundle

    init(languageTag: String) {
        let url = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = url.flatMap { Bundle(url: $0) } ?? .main
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "NativeCheckInCreate")
    }
}

#Preview("Light") {
    CheckInCreatePickerView(strings: CheckInCreatePickerStrings(languageTag: "ru"),
        dismiss: {}, shop: {}, home: {}, shelfBag: {})
        .preferredColorScheme(.light)
}

#Preview("Dark") {
    CheckInCreatePickerView(strings: CheckInCreatePickerStrings(languageTag: "be"),
        dismiss: {}, shop: {}, home: {}, shelfBag: {})
        .preferredColorScheme(.dark)
}

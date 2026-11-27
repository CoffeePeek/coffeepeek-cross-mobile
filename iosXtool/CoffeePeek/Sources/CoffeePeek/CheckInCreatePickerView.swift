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
    private var contentHeight: CGFloat?

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
        rootView.heightChanged = { [weak self] height in self?.updateHeight(height) }
        sheetPresentationController?.detents = [.custom(identifier: .init("check-in-create-content")) { [weak self] context in
            guard let self else { return nil }
            return min(self.contentHeight ?? self.initialContentHeight(), context.maximumDetentValue)
        }]
        sheetPresentationController?.prefersGrabberVisible = true
        sheetPresentationController?.prefersScrollingExpandsWhenScrolledToEdge = false
        sheetPresentationController?.prefersEdgeAttachedInCompactHeight = true
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { return nil }

    private func initialContentHeight() -> CGFloat {
        let width = view.bounds.width > 0 ? view.bounds.width : presentingViewController?.view.bounds.width ?? 320
        let measuring = UIHostingController(rootView: rootView.content)
        return ceil(measuring.sizeThatFits(in: CGSize(width: width, height: .greatestFiniteMagnitude)).height)
    }

    private func updateHeight(_ height: CGFloat) {
        let height = ceil(height)
        guard height > 0, contentHeight != height else { return }
        contentHeight = height
        sheetPresentationController?.invalidateDetents()
    }

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
    var heightChanged: (CGFloat) -> Void = { _ in }

    var body: some View {
        ScrollView {
            content.onGeometryChange(for: CGFloat.self) { $0.size.height } action: { heightChanged($0) }
        }
        .scrollBounceBehavior(.basedOnSize)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
    }

    var content: some View {
        VStack(alignment: .leading, spacing: 12) {
            heading
            choice("shop", icon: "location.fill", action: shop)
            choice("home", icon: "cup.and.saucer.fill", action: home)
            choice("shelf", icon: "camera.fill", action: shelfBag)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 24)
        .frame(maxWidth: 560)
        .frame(maxWidth: .infinity)
        .fixedSize(horizontal: false, vertical: true)
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

#Preview("Small viewport and large text") {
    CheckInCreatePickerView(strings: CheckInCreatePickerStrings(languageTag: "ru"),
        dismiss: {}, shop: {}, home: {}, shelfBag: {})
        .dynamicTypeSize(.accessibility3)
        .frame(width: 320, height: 480)
        .preferredColorScheme(.light)
}

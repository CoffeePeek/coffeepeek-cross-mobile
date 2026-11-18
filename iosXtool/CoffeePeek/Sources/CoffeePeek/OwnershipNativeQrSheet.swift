import CoreImage
import SwiftUI
import UIKit

struct OwnershipNativeQrSheet: View {
    let name: String
    let link: String
    let strings: OwnershipNativeStrings
    let share: (UIImage) -> Void
    @State private var image: UIImage?
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                Text(name).font(.title2.weight(.semibold))
                Text(strings["ownership_checkin_qr_hint"]).multilineTextAlignment(.center)
                if let image {
                    Image(uiImage: image).interpolation(.none).resizable().scaledToFit().frame(maxWidth: 280)
                        .accessibilityLabel(String(format: strings["ownership_checkin_qr_description"], name))
                    Button(strings["ownership_checkin_qr_share"]) { share(image) }.buttonStyle(.borderedProminent).foregroundStyle(.black)
                } else { Text(strings["ownership_checkin_qr_error"]) }
            }.padding(20)
        }
        .foregroundStyle(CoffeePeekNativeStyle.foreground).tint(CoffeePeekNativeStyle.accent)
        .background(CoffeePeekNativeStyle.background)
        .onAppear { image = printableCheckInQr(link) }
    }
}

private func printableCheckInQr(_ link: String) -> UIImage? {
    guard let filter = CIFilter(name: "CIQRCodeGenerator") else { return nil }
    filter.setValue(Data(link.utf8), forKey: "inputMessage")
    filter.setValue("M", forKey: "inputCorrectionLevel")
    guard let output = filter.outputImage, let bitmap = CIContext().createCGImage(output, from: output.extent) else { return nil }
    let code = UIImage(cgImage: bitmap)
    let format = UIGraphicsImageRendererFormat()
    format.scale = 1
    return UIGraphicsImageRenderer(size: CGSize(width: 1024, height: 1024), format: format).image { context in
        UIColor.white.setFill()
        context.fill(CGRect(x: 0, y: 0, width: 1024, height: 1024))
        context.cgContext.interpolationQuality = .none
        code.draw(in: CGRect(x: 164, y: 164, width: 696, height: 696))
    }
}

#Preview("Check-in QR light") {
    OwnershipNativeQrSheet(name: "CoffeePeek", link: "https://coffeepeek.by/coffee-shops/preview?checkin=1",
        strings: OwnershipNativeStrings("ru"), share: { _ in }).preferredColorScheme(.light)
}
#Preview("Check-in QR dark") {
    OwnershipNativeQrSheet(name: "CoffeePeek", link: "https://coffeepeek.by/coffee-shops/preview?checkin=1",
        strings: OwnershipNativeStrings("ru"), share: { _ in }).preferredColorScheme(.dark)
}

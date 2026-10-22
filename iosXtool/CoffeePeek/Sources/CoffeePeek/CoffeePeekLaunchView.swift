import SwiftUI
import UIKit

/// The same artwork stays visible until Compose's first frame is ready.
struct CoffeePeekLaunchView: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ZStack {
            Color(uiColor: UIColor(named: "LaunchBackground") ?? .systemBackground)
                .ignoresSafeArea()
            Image(uiImage: launchImage)
                .resizable()
                .scaledToFit()
                .frame(width: 144, height: 144)
                .accessibilityHidden(true)
        }
    }

    private var launchImage: UIImage {
        let name = colorScheme == .dark ? "LaunchLogo-dark" : "LaunchLogo"
        return UIImage(named: name) ?? UIImage(named: "LaunchLogo") ?? UIImage()
    }
}

#Preview("Light") { CoffeePeekLaunchView().preferredColorScheme(.light) }
#Preview("Dark") { CoffeePeekLaunchView().preferredColorScheme(.dark) }

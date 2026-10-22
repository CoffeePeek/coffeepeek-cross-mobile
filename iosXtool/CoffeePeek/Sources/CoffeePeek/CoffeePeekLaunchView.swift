import SwiftUI
import UIKit

/// The same artwork stays visible until Compose's first frame is ready.
struct CoffeePeekLaunchView: View {
    var body: some View {
        ZStack {
            Color(uiColor: UIColor(named: "LaunchBackground") ?? .systemBackground)
                .ignoresSafeArea()
            Image("LaunchLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 144, height: 144)
                .accessibilityHidden(true)
        }
    }
}

#Preview("Light") { CoffeePeekLaunchView().preferredColorScheme(.light) }
#Preview("Dark") { CoffeePeekLaunchView().preferredColorScheme(.dark) }

import ComposeApp
import SwiftUI

struct HomeCheckInNativeSaved: View {
    let saved: IosHomeCheckInSaved
    let strings: CheckInNativeStrings
    let done: () -> Void
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "checkmark.circle").font(.system(size: 48))
                .foregroundStyle(CoffeePeekNativeStyle.accent).accessibilityHidden(true)
            Text(strings[saved.isPublic?.boolValue == false ? "home_check_in_saved_private" : "home_check_in_saved_unknown"])
                .font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
                .multilineTextAlignment(.center).accessibilityAddTraits(.isHeader)
            if saved.visibilityUnconfirmed {
                Text(strings["home_check_in_saved_visibility_changed"])
                    .font(CoffeePeekNativeStyle.font(15)).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    .multilineTextAlignment(.center)
            }
            CheckInNativePrimaryButton(title: strings["home_check_in_done"], action: done)
        }.padding(24).frame(maxWidth: .infinity, maxHeight: .infinity)
            .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
    }
}

#Preview("Home saved light") {
    HomeCheckInNativeSaved(saved: IosHomeCheckInSaved(id: "preview", isPublic: KotlinBoolean(bool: true),
        moderation: "Pending", visibilityUnconfirmed: false), strings: homeCheckInNativePreviewStrings("ru"), done: {})
        .preferredColorScheme(.light)
}
#Preview("Home saved dark") {
    HomeCheckInNativeSaved(saved: IosHomeCheckInSaved(id: "preview", isPublic: KotlinBoolean(bool: false),
        moderation: "Pending", visibilityUnconfirmed: true), strings: homeCheckInNativePreviewStrings("be"), done: {})
        .preferredColorScheme(.dark)
}

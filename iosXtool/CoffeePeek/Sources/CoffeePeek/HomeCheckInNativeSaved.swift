import ComposeApp
import SwiftUI

struct HomeCheckInNativeSaved: View {
    let saved: IosHomeCheckInSaved
    let strings: CheckInNativeStrings
    let done: () -> Void
    var body: some View {
        CoffeePeekNativeSuccessScreen(title: strings["home_check_in_saved_unknown"],
            description: strings[saved.isPublic?.boolValue == false ? "home_check_in_saved_private" : "home_check_in_saved_description"],
            buttonTitle: strings["home_check_in_done"], closeLabel: strings["home_check_in_close"],
            notice: saved.visibilityUnconfirmed ? strings["home_check_in_saved_visibility_changed"] : nil,
            onDismiss: done, action: done)
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

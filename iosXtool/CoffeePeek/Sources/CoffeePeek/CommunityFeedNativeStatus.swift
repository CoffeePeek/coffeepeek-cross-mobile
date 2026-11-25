import SwiftUI
import UIKit

struct CommunityFeedNativeStatus: View {
    let state: CommunityFeedNativePresentation
    let perform: (String) -> Void

    var body: some View {
        VStack(spacing: 16) {
            if !state.labels.errorText.isEmpty { error }
            else if state.rows.isEmpty && !state.loading {
                Text(state.labels.empty).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if state.guest { signIn }
            }
            if state.loadingMore { ProgressView(state.labels.loading).tint(CoffeePeekNativeStyle.foreground) }
        }.padding(.horizontal, 16)
    }

    private var error: some View {
        VStack(spacing: 12) {
            if let mascot = Self.errorMascot {
                Image(uiImage: mascot).resizable().scaledToFit().frame(width: 120, height: 120).accessibilityHidden(true)
            }
            Text(state.labels.errorTitle).font(CoffeePeekNativeStyle.font(20, weight: "Semibold"))
            Text(state.labels.errorText).foregroundStyle(CoffeePeekNativeStyle.secondary)
            Button(state.labels.retry) { perform("retry") }
                .buttonStyle(.borderedProminent).buttonBorderShape(.capsule).controlSize(.large).foregroundStyle(.black)
        }.multilineTextAlignment(.center).frame(maxWidth: .infinity).padding(.vertical, 16)
    }

    private var signIn: some View {
        CoffeePeekNativeActionButton(title: state.labels.signIn, fontSize: 16) { perform("signIn") }
            .fixedSize(horizontal: false, vertical: true)
    }

    private static let errorMascot: UIImage? = {
        let resource = "com.coffeepeek.feature.community.impl.resources/drawable"
        for directory in ["compose-resources/composeResources/\(resource)", "compose-resources/\(resource)"] {
            if let url = Bundle.main.url(forResource: "community_mascot_error", withExtension: "png", subdirectory: directory),
               let image = UIImage(contentsOfFile: url.path) { return image }
        }
        return nil
    }()
}

struct CommunityFeedConfirmedCard: View {
    let id: String
    let labels: CommunityFeedNativeLabels
    let perform: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(labels.confirmedTitle).font(CoffeePeekNativeStyle.font(18, weight: "Semibold"))
            Text(labels.confirmedBody).foregroundStyle(CoffeePeekNativeStyle.secondary)
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 16) { buttons }
                VStack(alignment: .leading, spacing: 4) { buttons }
            }
        }.frame(maxWidth: .infinity, alignment: .leading).padding(16)
            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 20))
            .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(CoffeePeekNativeStyle.border))
    }

    @ViewBuilder private var buttons: some View {
        Button(labels.confirmedOpen) { perform("openConfirmed:" + id) }.frame(minHeight: 44)
        Button(labels.refresh) { perform("refresh") }.frame(minHeight: 44)
    }
}

#Preview("Feed error light") {
    CommunityFeedNativeStatus(state: communityFeedPreview(error: true), perform: { _ in })
        .tint(CoffeePeekNativeStyle.accent).preferredColorScheme(.light)
}
#Preview("Feed error dark") {
    CommunityFeedNativeStatus(state: communityFeedPreview(error: true), perform: { _ in })
        .tint(CoffeePeekNativeStyle.accent).preferredColorScheme(.dark)
}
#Preview("Confirmed post light") {
    CommunityFeedConfirmedCard(id: "fixture", labels: communityFeedPreview().labels, perform: { _ in }).padding()
        .tint(CoffeePeekNativeStyle.accent).preferredColorScheme(.light)
}
#Preview("Confirmed post dark") {
    CommunityFeedConfirmedCard(id: "fixture", labels: communityFeedPreview().labels, perform: { _ in }).padding()
        .tint(CoffeePeekNativeStyle.accent).preferredColorScheme(.dark)
}

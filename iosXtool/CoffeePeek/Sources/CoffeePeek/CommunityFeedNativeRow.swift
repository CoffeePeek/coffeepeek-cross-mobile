import SwiftUI

struct CommunityFeedNativeRowView: View {
    let row: CommunityFeedNativeRow
    let signInLabel: String
    let signIn: () -> Void

    var body: some View {
        Group {
            if row.teaser {
                card.allowsHitTesting(false).accessibilityHidden(true)
                    .frame(maxHeight: 320, alignment: .top).clipped()
                    .overlay { teaserOverlay }
            } else { card }
        }
    }

    @ViewBuilder private var card: some View {
        if let card = row.card { CoffeePeekCommunityCardView(card: card) }
        else if let preview = row.preview {
            CoffeePeekCheckInCard(data: preview, loadImage: { _, _ in nil }, perform: { _ in }, openPhoto: { _ in })
        }
    }

    private var teaserOverlay: some View {
        LinearGradient(stops: [
            .init(color: CoffeePeekNativeStyle.background.opacity(0.60), location: 0),
            .init(color: CoffeePeekNativeStyle.background.opacity(0.91), location: 0.55),
            .init(color: CoffeePeekNativeStyle.background, location: 1)
        ], startPoint: .top, endPoint: .bottom)
        .overlay {
            Button(action: signIn) { Color.clear.contentShape(Rectangle()) }
                .buttonStyle(.plain).accessibilityLabel(signInLabel)
        }
    }
}

private func communityFeedRowPreview(teaser: Bool) -> CommunityFeedNativeRow {
    var data = CoffeePeekCheckInCardData(id: "preview", name: "CoffeePeek", avatar: nil, date: "18 ноября")
    data.title = "CoffeePeek"
    data.note = "Кофе с ягодными нотами и сладким послевкусием."
    data.rating = 4.5
    data.ratingLabel = "4,5 из 5"
    data.drink = "Фильтр"
    data.likes = 12
    return CommunityFeedNativeRow(id: "preview", key: "preview", preview: data, teaser: teaser)
}

#Preview("Feed row light") {
    CommunityFeedNativeRowView(row: communityFeedRowPreview(teaser: false), signInLabel: "Войти", signIn: {})
        .preferredColorScheme(.light)
}
#Preview("Feed row dark") {
    CommunityFeedNativeRowView(row: communityFeedRowPreview(teaser: false), signInLabel: "Войти", signIn: {})
        .preferredColorScheme(.dark)
}
#Preview("Feed guest teaser light") {
    CommunityFeedNativeRowView(row: communityFeedRowPreview(teaser: true), signInLabel: "Войдите, чтобы читать ленту", signIn: {})
        .preferredColorScheme(.light)
}
#Preview("Feed guest teaser dark") {
    CommunityFeedNativeRowView(row: communityFeedRowPreview(teaser: true), signInLabel: "Войдите, чтобы читать ленту", signIn: {})
        .preferredColorScheme(.dark)
}

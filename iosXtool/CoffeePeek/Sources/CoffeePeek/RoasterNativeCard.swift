import ComposeApp
import SwiftUI

struct RoasterNativeCard: View {
    let card: IosRoasterCard
    let strings: OwnProfileStrings
    let source: IosRoasterDiscovery?
    let loader: CoffeePeekCardImageLoader
    var compact = false

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Button { if let slug = card.slug { source?.open(slug: slug) } } label: { content }
                .buttonStyle(.plain).disabled(card.slug == nil)
            favorite.padding(4)
        }
        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(CoffeePeekNativeStyle.border, lineWidth: 1))
        .onAppear { if let slug = card.slug { source?.appeared(slug: slug) } }
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: compact ? 8 : 16) {
            HStack(alignment: .top, spacing: 12) {
                CoffeePeekCardImage(url: card.photo, symbol: nil, fits: true, loadImage: loader)
                    .frame(width: compact ? 56 : 64, height: compact ? 56 : 64).background(Color.white).clipShape(RoundedRectangle(cornerRadius: 12))
                VStack(alignment: .leading, spacing: 4) {
                    Text(card.name).font(CoffeePeekNativeStyle.font(compact ? 16 : 17, weight: "Bold")).lineLimit(1).padding(.trailing, 36)
                    if let about = card.about, !about.isEmpty {
                        Text(about).font(CoffeePeekNativeStyle.font(compact ? 13 : 14)).foregroundStyle(CoffeePeekNativeStyle.secondary).lineLimit(compact ? 2 : 3)
                    }
                }.frame(maxWidth: .infinity, alignment: .leading)
            }
            HStack(spacing: compact ? 8 : 12) {
                counter(card.shops, label: strings["roaster_stat_shops"], symbol: "cup.and.saucer")
                Divider().frame(height: 20)
                counter(card.products, label: strings["roaster_stat_catalog"], symbol: "leaf")
            }
        }
        .padding(compact ? 12 : 16)

    }

    private var favorite: some View {
        Button { if let slug = card.slug { source?.favorite(slug: slug) } } label: {
            Image(systemName: card.favorite ? "heart.fill" : "heart").frame(width: 44, height: 44)
                .foregroundStyle(card.favorite ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.foreground)
        }
        .buttonStyle(.plain).disabled(card.slug == nil)
        .accessibilityLabel(strings[card.favorite ? "roaster_favorite_remove" : "roaster_favorite_add"])
        .roasterExposure("favorite:\(card.slug ?? card.name)", source: source)
    }

    private func counter(_ value: Int32, label: String, symbol: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: symbol).accessibilityHidden(true)
            Text(value > 0 ? "\(value)" : "—").fontWeight(.semibold)
            Text(label).font(CoffeePeekNativeStyle.font(12)).foregroundStyle(CoffeePeekNativeStyle.secondary)
        }.frame(maxWidth: .infinity, alignment: .leading).accessibilityElement(children: .combine)
    }
}

#Preview("Roaster card light") {
    RoasterNativeCard(card: IosRoasterCard(slug: "fixture", name: "CoffeePeek", photo: nil, about: "Кофе каждый день",
        shops: 7, products: 21, favorite: false), strings: OwnProfileStrings(languageTag: "ru", table: "NativeRoaster"),
        source: nil, loader: { _, receive in receive(nil); return nil }).padding().preferredColorScheme(.light)
}
#Preview("Roaster card dark") {
    RoasterNativeCard(card: IosRoasterCard(slug: "fixture", name: "CoffeePeek", photo: nil, about: nil,
        shops: 0, products: 0, favorite: true), strings: OwnProfileStrings(languageTag: "be", table: "NativeRoaster"),
        source: nil, loader: { _, receive in receive(nil); return nil }).padding().preferredColorScheme(.dark)
}

#Preview("Compact roaster card light") {
    RoasterNativeCard(card: IosRoasterCard(slug: "fixture", name: "CoffeePeek", photo: nil, about: "Кофе каждый день",
        shops: 7, products: 21, favorite: false), strings: OwnProfileStrings(languageTag: "ru", table: "NativeRoaster"),
        source: nil, loader: { _, receive in receive(nil); return nil }, compact: true).padding().preferredColorScheme(.light)
}
#Preview("Compact roaster card dark") {
    RoasterNativeCard(card: IosRoasterCard(slug: "fixture", name: "CoffeePeek", photo: nil, about: "Кава кожны дзень",
        shops: 7, products: 21, favorite: true), strings: OwnProfileStrings(languageTag: "be", table: "NativeRoaster"),
        source: nil, loader: { _, receive in receive(nil); return nil }, compact: true).padding().preferredColorScheme(.dark)
}

import ComposeApp
import SwiftUI

/// The Discovery owner supplies all display text and favorite state; images use shared authorized loading.
struct DiscoveryShopCard: View {
    let shop: IosDiscoveryShop
    let picker: Bool
    let compact: Bool
    let labels: [String: String]
    let actions: DiscoveryNativeActions
    var action = "open"
    var catalogDetails = true
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Group {
            if picker || compact { row } else { card }
        }
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
    }

    private var row: some View {
        HStack(spacing: 8) {
            Button { actions.call(action, shop.id) } label: {
                HStack(spacing: 12) {
                    photo.frame(width: picker ? 56 : 72, height: picker ? 56 : 72)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    VStack(alignment: .leading, spacing: 4) {
                        if !picker { rating }
                        Text(shop.title).font(CoffeePeekNativeStyle.font(16, weight: "SemiBold")).lineLimit(2)
                        details
                    }.frame(maxWidth: .infinity, alignment: .leading)
                    Image(systemName: "chevron.right").font(.system(size: 16))
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                }.contentShape(Rectangle())
            }.buttonStyle(.plain)
            if !picker && action == "open" { favorite }
        }.padding(12).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 20))
            .overlay { RoundedRectangle(cornerRadius: 20).stroke(CoffeePeekNativeStyle.border, lineWidth: 1) }
    }

    private var card: some View {
        VStack(alignment: .leading, spacing: 0) {
            cover
            Button { actions.call(action, shop.id) } label: {
                VStack(alignment: .leading, spacing: 8) {
                    ViewThatFits(in: .horizontal) {
                        HStack(spacing: 8) { title; Spacer(minLength: 0); status }
                        VStack(alignment: .leading, spacing: 8) { title; status }
                    }
                    if catalogDetails { brewMethods }
                    details
                    HStack(spacing: 6) {
                        Text(shop.kind)
                        if let decoration = shop.decoration, decoration.priceLevel > 0 {
                            Text("·")
                            Text(Array(repeating: "Br", count: min(3, Int(decoration.priceLevel))).joined(separator: " "))
                                .font(CoffeePeekNativeStyle.font(9, weight: "Bold"))
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                    }.font(CoffeePeekNativeStyle.font(12)).foregroundStyle(CoffeePeekNativeStyle.secondary)
                }.padding(16).frame(maxWidth: .infinity, alignment: .leading)
            }.buttonStyle(.plain)
        }
        .background(CoffeePeekNativeStyle.surface)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .overlay { RoundedRectangle(cornerRadius: 24).stroke(CoffeePeekNativeStyle.border, lineWidth: 1) }
    }

    private var cover: some View {
        Button { actions.call(action, shop.id) } label: { photo.aspectRatio(1.6, contentMode: .fit).clipped() }
            .buttonStyle(.plain)
            .overlay(alignment: .topLeading) {
                if shop.rating != nil {
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill").foregroundStyle(CoffeePeekNativeStyle.accent)
                        Text(shop.rating ?? "").foregroundStyle(.white)
                        if let decoration = shop.decoration, decoration.ratingCount > 0 {
                            Text("(\(decoration.ratingCount))").foregroundStyle(.white.opacity(0.9))
                        }
                    }.font(CoffeePeekNativeStyle.font(12, weight: "SemiBold"))
                        .padding(8).background(.black.opacity(0.68), in: RoundedRectangle(cornerRadius: 12)).padding(12)
                }
            }
            .overlay(alignment: .topTrailing) { favorite.padding(8) }
            .overlay(alignment: .bottomLeading) {
                if shop.isNew {
                    Label(labels["new"] ?? "", systemImage: "sparkles")
                        .font(CoffeePeekNativeStyle.font(11, weight: "SemiBold")).foregroundStyle(.white)
                        .padding(8).background(.black.opacity(0.68), in: RoundedRectangle(cornerRadius: 12)).padding(12)
                }
            }
            .overlay(alignment: .bottomTrailing) { if catalogDetails { roasterLogos.padding(12) } }
    }

    private var title: some View {
        Text(shop.title).font(CoffeePeekNativeStyle.font(20, weight: "Bold", relativeTo: .title3)).lineLimit(3)
    }

    private var status: some View {
        Text(labels[shop.open ? "open" : "closed"] ?? "")
            .font(CoffeePeekNativeStyle.font(10, weight: "SemiBold"))
            .foregroundStyle(shop.open ? CoffeePeekNativeStyle.color(colorScheme == .dark ? 0x22C55E : 0x166534) : CoffeePeekNativeStyle.color(colorScheme == .dark ? 0xFCA5A5 : 0xB91C1C))
            .padding(.horizontal, 8).padding(.vertical, 4)
            .background((shop.open ? Color.green : Color.red).opacity(0.12), in: Capsule())
    }

    private var brewMethods: some View {
        HStack(spacing: 4) {
            ForEach(Array((shop.decoration?.brewMethods ?? []).prefix(2).enumerated()), id: \.offset) { _, method in
                Text(method).lineLimit(1).padding(6)
                    .background(CoffeePeekNativeStyle.background, in: RoundedRectangle(cornerRadius: 12))
            }
            if let methods = shop.decoration?.brewMethods, methods.count > 2 { Text("+\(methods.count - 2)") }
        }.font(CoffeePeekNativeStyle.font(11)).foregroundStyle(CoffeePeekNativeStyle.secondary)
    }

    private var roasterLogos: some View {
        HStack(spacing: -14) {
            ForEach(Array((shop.decoration?.roasterLogos ?? []).prefix(3).enumerated()), id: \.offset) { _, url in
                CoffeePeekCardImage(url: url, symbol: nil, fits: true, loadImage: actions.image)
                    .frame(width: 36, height: 36).background(CoffeePeekNativeStyle.surface)
                    .clipShape(Circle()).overlay { Circle().stroke(CoffeePeekNativeStyle.border, lineWidth: 1) }
            }
        }
    }

    private var photo: some View {
        CoffeePeekCardImage(url: shop.photoUrl, symbol: nil, loadImage: actions.image)
    }

    @ViewBuilder private var rating: some View {
        if let rating = shop.rating {
            Label(rating, systemImage: "star.fill").font(CoffeePeekNativeStyle.font(13, weight: "SemiBold"))
                .foregroundStyle(CoffeePeekNativeStyle.accent)
        } else if let detail = shop.detail {
            Text(detail).font(CoffeePeekNativeStyle.font(12)).foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }

    @ViewBuilder private var details: some View {
        if let address = shop.address, !address.isEmpty {
            Text(address).font(CoffeePeekNativeStyle.font(13)).foregroundStyle(CoffeePeekNativeStyle.secondary).lineLimit(2)
        }
        if let distance = shop.distance {
            Text(distance).font(CoffeePeekNativeStyle.font(12)).foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
        if let hours = shop.hours {
            Text(hours).font(CoffeePeekNativeStyle.font(12)).foregroundStyle(CoffeePeekNativeStyle.secondary).lineLimit(1)
        }
        if !picker && !compact && catalogDetails && !shop.tags.isEmpty {
            Text(shop.tags.prefix(3).joined(separator: " · "))
                .font(CoffeePeekNativeStyle.font(12)).foregroundStyle(CoffeePeekNativeStyle.secondary).lineLimit(2)
        }
    }

    private var favorite: some View {
        Button { actions.call("favorite", shop.id) } label: {
            Image(systemName: shop.favorite ? "heart.fill" : "heart")
                .font(.system(size: 20)).foregroundStyle(shop.favorite ? CoffeePeekNativeStyle.accent : Color.white)
                .frame(width: 44, height: 44).background(.black.opacity(0.68), in: Circle())
        }
        .buttonStyle(.plain).disabled(shop.favoriteBusy).accessibilityLabel(shop.favoriteLabel)
        .discoveryExposure("favorite", id: "favorite-\(shop.id)-\(compact)")
    }
}

#Preview("Discovery shop light") {
    DiscoveryShopCard(shop: discoveryPreviewState().shops[0], picker: false, compact: false,
        labels: discoveryPreviewState().labels, actions: DiscoveryNativeActions()).padding().preferredColorScheme(.light)
}

#Preview("Discovery shop dark") {
    DiscoveryShopCard(shop: discoveryPreviewState().shops[0], picker: false, compact: false,
        labels: discoveryPreviewState().labels, actions: DiscoveryNativeActions()).padding().preferredColorScheme(.dark)
}

#Preview("Check-in shop choice light") {
    DiscoveryShopCard(shop: discoveryPreviewState().shops[0], picker: true, compact: true,
        labels: discoveryPreviewState().labels, actions: DiscoveryNativeActions()).padding().preferredColorScheme(.light)
}

#Preview("Check-in shop choice dark") {
    DiscoveryShopCard(shop: discoveryPreviewState().shops[0], picker: true, compact: true,
        labels: discoveryPreviewState().labels, actions: DiscoveryNativeActions()).padding().preferredColorScheme(.dark)
}

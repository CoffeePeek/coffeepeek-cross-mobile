import ComposeApp
import SwiftUI
import UIKit

struct OwnershipNativeOverview: View {
    let state: IosOwnershipState
    let tab: Bool
    let strings: OwnershipNativeStrings
    let send: (OwnershipNativeIntent) -> Void
    let imageLoader: CoffeePeekCardImageLoader
    let qrLink: (String) -> String?
    let shareQr: (UIImage, String) -> Void
    @State private var qr: IosOwnershipBusiness?

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if !tab {
                Text(strings["ownership_claims"]).font(.title3.weight(.semibold))
                if state.claims.isEmpty { Text(strings["ownership_empty"]) }
                ForEach(state.claims, id: \.id) { claim in
                    Button { send(.claim(claim.id)) } label: {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(claim.title).font(.headline)
                            Text(strings.status(claim.status))
                            Text(strings[claim.access ? "ownership_access_yes" : "ownership_access_no"]).font(.footnote)
                        }.frame(maxWidth: .infinity, alignment: .leading).padding(12)
                            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 12))
                    }.buttonStyle(.plain)
                }
                pager(page: Int(state.claimsPage), next: state.claimsNext) { send(.claimsPage($0)) }
            }
            Text(strings["ownership_shops"]).font(.title3.weight(.semibold))
            businesses("CoffeeShop")
            if state.businesses.contains(where: { $0.kind == "Roaster" }) {
                Text(strings["ownership_roasters"]).font(.title3.weight(.semibold))
                businesses("Roaster")
                pager(page: Int(state.roastersPage), next: state.roastersNext) { send(.roastersPage($0)) }
            }
        }
        .sheet(isPresented: Binding(get: { qr != nil }, set: { if !$0 { qr = nil } })) {
            if let shop = qr, let link = qrLink(shop.id) {
                OwnershipNativeQrSheet(name: shop.title, link: link, strings: strings, share: { shareQr($0, shop.title) })
            }
        }
        .onChange(of: state.generation) { _, _ in qr = nil }
    }
    @ViewBuilder private func businesses(_ kind: String) -> some View {
        let items = state.businesses.filter { $0.kind == kind }
        if items.isEmpty { Text(strings["ownership_empty"]).foregroundStyle(CoffeePeekNativeStyle.secondary) }
        ForEach(items, id: \.id) { business in
            if tab {
                Button { send(.business(kind, business.id)) } label: {
                    VStack(alignment: .leading, spacing: 8) {
                        CoffeePeekCardImage(url: business.cover, symbol: nil, loadImage: imageLoader)
                            .frame(height: kind == "CoffeeShop" ? 180 : 120).clipped()
                        Text(business.title).font(.headline)
                        if let address = business.address, !address.isEmpty { Text(address).font(.subheadline).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                    }.frame(maxWidth: .infinity, alignment: .leading).padding(12)
                        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
                }.buttonStyle(.plain)
                if kind == "CoffeeShop", qrLink(business.id) != nil {
                    Button(strings["ownership_checkin_qr_action"]) { qr = business }.buttonStyle(.bordered).frame(maxWidth: .infinity)
                }
            } else {
                Button(business.title) { send(.business(kind, business.id)) }.buttonStyle(.bordered).frame(maxWidth: .infinity)
            }
        }
    }
    @ViewBuilder private func pager(page: Int, next: Bool, change: @escaping (Int) -> Void) -> some View {
        if page > 1 || next {
            HStack {
                Button(strings["ownership_previous"]) { change(page - 1) }.disabled(page <= 1)
                Spacer()
                Button(strings["ownership_next"]) { change(page + 1) }.disabled(!next)
            }.buttonStyle(.bordered)
        }
    }
}

private func ownershipOverviewPreview() -> some View {
    ScrollView { OwnershipNativeOverview(state: ownershipPreviewState(), tab: false, strings: OwnershipNativeStrings("ru"),
        send: { _ in }, imageLoader: { _, _ in nil }, qrLink: { _ in nil }, shareQr: { _, _ in }).padding() }
        .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
}
#Preview("My business light") { ownershipOverviewPreview().preferredColorScheme(.light) }
#Preview("My business dark") { ownershipOverviewPreview().preferredColorScheme(.dark) }

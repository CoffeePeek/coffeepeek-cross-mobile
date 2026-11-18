import ComposeApp
import SwiftUI

struct ShopDetailNativeInformation: View {
    let details: IosShopDetails
    let scheduleExpanded: Bool
    let featuresExpanded: Bool
    let labels: [String: String]
    let actions: ShopDetailNativeActions

    private func text(_ key: String) -> String { labels[key] ?? "" }

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            if let description = details.summary, !description.isEmpty {
                section(text("description")) { Text(description).fixedSize(horizontal: false, vertical: true) }
                    .padding(16).shopDetailPanel()
            }
            if !details.roasters.isEmpty { roasters }
            catalog(text("beans"), values: details.beans)
            catalog(text("equipment"), values: details.equipment)
            if !details.schedule.isEmpty { schedule }
            if let menu = details.menu { menuSection(menu) }
            if !details.contacts.isEmpty { contacts }
            if !details.features.isEmpty { features }
        }.frame(maxWidth: .infinity, alignment: .leading)
    }

    private var roasters: some View {
        section(text("roasters")) {
            VStack(spacing: 0) {
                ForEach(Array(details.roasters.enumerated()), id: \.element.id) { index, roaster in
                    if index > 0 { Divider().padding(.horizontal, 16) }
                    Button { actions.call("roaster", roaster.id) } label: {
                        HStack(spacing: 12) {
                            CoffeePeekCardImage(url: roaster.photo, symbol: nil, fits: true, loadImage: actions.image)
                                .frame(width: 48, height: 48).clipShape(Circle())
                            Text(roaster.name).font(CoffeePeekNativeStyle.font(16, weight: "Medium"))
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Image(systemName: "chevron.right").foregroundStyle(CoffeePeekNativeStyle.secondary)
                        }.padding(16)
                    }.buttonStyle(.plain).accessibilityLabel(roaster.label)
                }
            }.shopDetailPanel()
        }.shopDetailExposure("Roasters", id: "roasters")
    }

    @ViewBuilder private func catalog(_ title: String, values: [String]) -> some View {
        if !values.isEmpty {
            section(title) {
                Text(values.joined(separator: " · ")).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading).padding(16).shopDetailPanel()
            }
        }
    }

    private var schedule: some View {
        section(text("hours")) {
            VStack(spacing: 12) {
                Button { actions.call("schedule") } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "clock")
                        Text(details.scheduleStatus).frame(maxWidth: .infinity, alignment: .leading)
                        Image(systemName: "chevron.right").rotationEffect(.degrees(scheduleExpanded ? 90 : 0))
                    }.frame(minHeight: 44)
                }.buttonStyle(.plain).accessibilityLabel(text(scheduleExpanded ? "hours-collapse" : "hours-expand"))
                if scheduleExpanded {
                    Divider()
                    ForEach(Array(details.schedule.enumerated()), id: \.offset) { _, day in
                        HStack(alignment: .top, spacing: 12) {
                            Text(day.day).frame(maxWidth: .infinity, alignment: .leading)
                            Text(day.hours).multilineTextAlignment(.trailing)
                        }.font(CoffeePeekNativeStyle.font(14)).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    }
                }
            }.padding(16).shopDetailPanel()
        }.shopDetailExposure("Hours", id: "hours")
    }

    private func menuSection(_ menu: IosShopDetailMenu) -> some View {
        section(text("menu")) {
            VStack(alignment: .leading, spacing: 16) {
                menuItems(menu.groups)
                if !menu.photos.isEmpty { menuPhotos(menu.photos) }
                ForEach(Array(menu.freshness.enumerated()), id: \.offset) { _, line in
                    Text(line).font(CoffeePeekNativeStyle.font(11)).foregroundStyle(CoffeePeekNativeStyle.secondary)
                }
            }.padding(16).shopDetailPanel()
        }.shopDetailExposure("Menu", id: "menu")
    }

    private func menuItems(_ groups: [[IosShopDetailMenuItem]]) -> some View {
        ForEach(Array(groups.enumerated()), id: \.offset) { groupIndex, items in
            if groupIndex > 0 { Divider() }
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                HStack(alignment: .firstTextBaseline, spacing: 12) {
                    Text(item.title).frame(maxWidth: .infinity, alignment: .leading)
                    if let amount = item.amount {
                        Text(amount + " " + item.currency).font(CoffeePeekNativeStyle.font(15, weight: "SemiBold"))
                            .accessibilityLabel(item.priceLabel ?? amount)
                    }
                }.padding(.vertical, 4)
            }
        }
    }

    private func menuPhotos(_ photos: [IosShopDetailPhoto]) -> some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                ForEach(Array(photos.enumerated()), id: \.element.id) { index, photo in
                    Button { actions.call("menu-photo", "", index) } label: {
                        CoffeePeekCardImage(url: photo.url, symbol: nil, loadImage: actions.image)
                            .frame(width: 120, height: 154).clipShape(RoundedRectangle(cornerRadius: 12))
                    }.buttonStyle(.plain).accessibilityLabel(photo.label)
                }
            }
        }.scrollIndicators(.hidden)
    }

    private var contacts: some View {
        section(text("contacts")) {
            VStack(spacing: 0) {
                ForEach(Array(details.contacts.enumerated()), id: \.element.kind) { index, contact in
                    if index > 0 { Divider().padding(.horizontal, 16) }
                    HStack(spacing: 12) {
                        Button { actions.call("link", contact.target) } label: {
                            Label(contact.title, systemImage: contactSymbol(contact.kind))
                                .frame(maxWidth: .infinity, alignment: .leading).frame(minHeight: 44)
                        }.buttonStyle(.plain).accessibilityLabel(contact.label)
                        if contact.copyable {
                            Button { actions.call("copy", contact.title) } label: {
                                Image(systemName: "doc.on.doc").frame(width: 44, height: 44)
                            }.buttonStyle(.plain).accessibilityLabel(text("copy"))
                        }
                    }.padding(.horizontal, 16).padding(.vertical, 4)
                }
            }.shopDetailPanel()
        }.shopDetailExposure("Contacts", id: "contacts")
    }

    private var features: some View {
        section(text("features")) {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(details.features.prefix(featuresExpanded ? details.features.count : 5).enumerated()), id: \.offset) { _, feature in
                    featureRow(feature)
                }
                if details.features.count > 5 {
                    Button(details.featureToggle) { actions.call("features") }.frame(maxWidth: .infinity, minHeight: 44)
                }
            }.padding(16).shopDetailPanel()
        }
    }

    private func featureRow(_ feature: IosShopDetailFeature) -> some View {
        HStack(spacing: 12) {
            if let icon = feature.icon {
                Image(uiImage: icon).renderingMode(.template).resizable().scaledToFit()
                    .frame(width: 28, height: 28).accessibilityHidden(true)
            } else { Color.clear.frame(width: 28, height: 28).accessibilityHidden(true) }
            Text(feature.name).frame(maxWidth: .infinity, alignment: .leading)
        }.frame(minHeight: 44).accessibilityElement(children: .combine)
    }

    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(CoffeePeekNativeStyle.font(22, weight: "SemiBold", relativeTo: .title2))
                .accessibilityAddTraits(.isHeader)
            content()
        }
    }

    private func contactSymbol(_ kind: String) -> String {
        switch kind {
        case "phone": "phone"
        case "email": "envelope"
        case "instagram": "camera"
        default: "globe"
        }
    }
}

private extension View {
    func shopDetailPanel() -> some View {
        background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 24))
            .overlay { RoundedRectangle(cornerRadius: 24).stroke(CoffeePeekNativeStyle.border, lineWidth: 1) }
    }
}


#Preview("Shop information light") {
    ScrollView {
        ShopDetailNativeInformation(details: shopDetailPreviewData().details!, scheduleExpanded: true,
            featuresExpanded: true, labels: shopDetailPreviewData().labels, actions: ShopDetailNativeActions()).padding()
    }.background(CoffeePeekNativeStyle.background).preferredColorScheme(.light)
}
#Preview("Shop information dark") {
    ScrollView {
        ShopDetailNativeInformation(details: shopDetailPreviewData().details!, scheduleExpanded: true,
            featuresExpanded: true, labels: shopDetailPreviewData().labels, actions: ShopDetailNativeActions()).padding()
    }.background(CoffeePeekNativeStyle.background).preferredColorScheme(.dark)
}

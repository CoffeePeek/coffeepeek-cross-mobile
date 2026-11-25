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
            VStack(spacing: 0) {
                Button { actions.call("schedule") } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "clock")
                        Text(details.scheduleStatus).frame(maxWidth: .infinity, alignment: .leading)
                        Image(systemName: "chevron.right").rotationEffect(.degrees(scheduleExpanded ? 90 : 0))
                    }.frame(maxWidth: .infinity, minHeight: 44).padding(16).contentShape(Rectangle())
                }.buttonStyle(.plain).accessibilityLabel(text(scheduleExpanded ? "hours-collapse" : "hours-expand"))
                if scheduleExpanded {
                    scheduleDays.padding(.horizontal, 16).padding(.bottom, 16).transition(.opacity)
                }
            }.shopDetailPanel()
        }.shopDetailExposure("Hours", id: "hours")
    }

    private var scheduleDays: some View {
        VStack(spacing: 12) {
            Divider()
            ForEach(Array(details.schedule.enumerated()), id: \.offset) { _, day in
                HStack(alignment: .top, spacing: 12) {
                    Text(day.day).frame(maxWidth: .infinity, alignment: .leading)
                    Text(day.hours).multilineTextAlignment(.trailing)
                }.font(CoffeePeekNativeStyle.font(14)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
        }
    }

    private func menuSection(_ menu: IosShopDetailMenu) -> some View {
        section(text("menu")) {
            ShopMenuNativeContent(menu: menu, actions: actions).padding(16).shopDetailPanel()
        }.shopDetailExposure("Menu", id: "menu")
    }

    private var contacts: some View {
        section(text("contacts")) {
            VStack(spacing: 0) {
                ForEach(Array(details.contacts.enumerated()), id: \.element.kind) { index, contact in
                    if index > 0 { Divider().padding(.horizontal, 16) }
                    HStack(spacing: 12) {
                        Button { actions.call("link", contact.target) } label: {
                            HStack(spacing: 12) {
                                CoffeePeekNativeContactIcon(kind: contact.kind)
                                Text(contact.title).frame(maxWidth: .infinity, alignment: .leading)
                            }.frame(maxWidth: .infinity, minHeight: 48).contentShape(Rectangle())
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
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(text("features")).font(CoffeePeekNativeStyle.font(22, weight: "SemiBold", relativeTo: .title2))
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                if details.features.count > 5 {
                    CoffeePeekNativeChevronButton(label: details.featureToggle,
                        symbol: featuresExpanded ? "chevron.up" : "chevron.down") { actions.call("features") }
                }
            }
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(details.features.prefix(featuresExpanded ? details.features.count : 5).enumerated()), id: \.offset) { _, feature in
                    featureRow(feature)
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

}

private struct ShopMenuNativeContent: View {
    let menu: IosShopDetailMenu
    let actions: ShopDetailNativeActions
    @ScaledMetric(relativeTo: .body) private var fontScale: CGFloat = 1
    @State private var availableWidth: CGFloat = 0

    private var layout: AnyLayout {
        availableWidth >= 280 && fontScale <= 1.3 && !menu.groups.isEmpty && !menu.photos.isEmpty
            ? AnyLayout(HStackLayout(alignment: .top, spacing: 12))
            : AnyLayout(VStackLayout(alignment: .leading, spacing: 12))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            layout {
                if !menu.groups.isEmpty { items.frame(maxWidth: .infinity, alignment: .leading) }
                if !menu.photos.isEmpty {
                    ShopMenuNativePhotoStack(photos: menu.photos, freshness: menu.freshness, actions: actions)
                }
            }
            if menu.photos.isEmpty { ShopMenuNativeFreshness(lines: menu.freshness) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { availableWidth = $0 }
    }

    private var items: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(menu.groups.enumerated()), id: \.offset) { groupIndex, group in
                if groupIndex > 0 { Divider() }
                ForEach(Array(group.enumerated()), id: \.offset) { _, item in
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Text(item.title).frame(maxWidth: .infinity, alignment: .leading)
                        if let amount = item.amount {
                            Text(amount + " " + item.currency).font(CoffeePeekNativeStyle.font(15, weight: "SemiBold"))
                                .accessibilityLabel(item.priceLabel ?? amount)
                        }
                    }.padding(.vertical, 4)
                }
            }
        }
    }
}

private struct ShopMenuNativePhotoStack: View {
    let photos: [IosShopDetailPhoto]
    let freshness: [String]
    let actions: ShopDetailNativeActions
    @State private var selected = 0

    var body: some View {
        ZStack(alignment: .leading) {
            ForEach([2, 1], id: \.self) { depth in
                if photos.indices.contains(selected + depth) {
                    artwork(photos[selected + depth])
                        .scaleEffect(1 - CGFloat(depth) * 0.08, anchor: .trailing)
                        .offset(x: CGFloat(depth) * 12).zIndex(-Double(depth))
                        .allowsHitTesting(false).accessibilityHidden(true)
                }
            }
            pager.frame(width: 124, height: 248)
        }
        .frame(width: 124 + CGFloat(min(2, max(0, photos.count - 1))) * 12, height: 248, alignment: .leading)
        .onChange(of: photos.map(\.id)) { _, _ in selected = min(selected, max(0, photos.count - 1)) }
    }

    private var pager: some View {
        TabView(selection: $selected) {
            ForEach(Array(photos.enumerated()), id: \.element.id) { index, photo in
                Button { actions.call("menu-photo", "", index) } label: {
                    artwork(photo).overlay(alignment: .bottomLeading) {
                        if !freshness.isEmpty {
                            ShopMenuNativeFreshness(lines: freshness, onPhoto: true)
                                .padding(8).background(.black.opacity(0.68), in: RoundedRectangle(cornerRadius: 8)).padding(8)
                        }
                    }
                }.buttonStyle(.plain).accessibilityLabel(photo.label)
                    .accessibilityValue(freshness.joined(separator: ", ")).tag(index)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.18), radius: 4, y: 2)
    }

    private func artwork(_ photo: IosShopDetailPhoto) -> some View {
        Group {
            if photo.url.isEmpty { CoffeePeekPhotoPlaceholder() }
            else { CoffeePeekCardImage(url: photo.url, symbol: nil, loadImage: actions.image) }
        }
        .frame(width: 124, height: 248)
        .overlay {
            LinearGradient(colors: [.clear, .clear, .black.opacity(0.68)], startPoint: .top, endPoint: .bottom)
                .allowsHitTesting(false)
        }
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct ShopMenuNativeFreshness: View {
    let lines: [String]
    var onPhoto = false

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            ForEach(Array(lines.enumerated()), id: \.offset) { _, line in Text(line) }
        }
        .font(CoffeePeekNativeStyle.font(11, relativeTo: .caption2))
        .foregroundStyle(onPhoto ? .white : CoffeePeekNativeStyle.secondary)
        .fixedSize(horizontal: false, vertical: true)
    }
}

@MainActor
private func shopMenuNativePreview(photos: Bool = true) -> some View {
    let menu = IosShopDetailMenu(groups: [
        [IosShopDetailMenuItem(id: "espresso", title: "Эспрессо", amount: "4,00", currency: "BYN", priceLabel: nil),
         IosShopDetailMenuItem(id: "flat-white", title: "Флэт уайт", amount: "5,50", currency: "BYN", priceLabel: nil)],
        [IosShopDetailMenuItem(id: "filter", title: "Фильтр-кофе", amount: nil, currency: "BYN", priceLabel: nil)]
    ], photos: photos ? (0..<3).map { IosShopDetailPhoto(id: "photo-\($0)", url: "", label: "Фото \($0 + 1)") } : [],
        freshness: ["Снято 18.11.2026", "Обновлено 19.11.2026"])
    return ShopMenuNativeContent(menu: menu, actions: ShopDetailNativeActions())
        .padding(16).shopDetailPanel().padding(16).background(CoffeePeekNativeStyle.background)
        .font(CoffeePeekNativeStyle.font(15)).foregroundStyle(CoffeePeekNativeStyle.foreground)
}

#Preview("Shop menu photos light") { shopMenuNativePreview().preferredColorScheme(.light) }
#Preview("Shop menu photos dark") { shopMenuNativePreview().preferredColorScheme(.dark) }
#Preview("Shop menu accessibility light") { shopMenuNativePreview().dynamicTypeSize(.accessibility2).preferredColorScheme(.light) }
#Preview("Shop menu accessibility dark") { shopMenuNativePreview().dynamicTypeSize(.accessibility2).preferredColorScheme(.dark) }
#Preview("Shop menu prices light") { shopMenuNativePreview(photos: false).preferredColorScheme(.light) }
#Preview("Shop menu prices dark") { shopMenuNativePreview(photos: false).preferredColorScheme(.dark) }

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

private struct ShopScheduleInteractionPreview: View {
    @State private var expanded = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ScrollView {
            if let details = shopDetailPreviewData().details {
                ShopDetailNativeInformation(details: details, scheduleExpanded: expanded,
                    featuresExpanded: false, labels: shopDetailPreviewData().labels,
                    actions: ShopDetailNativeActions(perform: { name, _, _ in
                        if name == "schedule" { expanded.toggle() }
                    }))
                    .padding(16)
                    .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: expanded)
            }
        }.background(CoffeePeekNativeStyle.background)
    }
}

#Preview("Shop hours interaction light") { ShopScheduleInteractionPreview().preferredColorScheme(.light) }
#Preview("Shop hours interaction dark") { ShopScheduleInteractionPreview().preferredColorScheme(.dark) }

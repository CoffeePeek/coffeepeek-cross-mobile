import SwiftUI

struct CoffeePeekCheckInCard: View {
    let data: CoffeePeekCheckInCardData
    let loadImage: CoffeePeekCardImageLoader
    let perform: (String) -> Void
    let openPhoto: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            header.padding(.horizontal, data.horizontalInset)
            heading.padding(.horizontal, data.horizontalInset)
            if !data.note.isEmpty {
                Text(data.note).font(CoffeePeekNativeStyle.font(17))
                    .fixedSize(horizontal: false, vertical: true).padding(.horizontal, data.horizontalInset)
            }
            if !data.photos.isEmpty {
                CoffeePeekCheckInPhotoStrip(photos: data.photos, loadImage: loadImage, openPhoto: openPhoto,
                                           horizontalInset: data.horizontalInset)
            }
            publication.padding(.horizontal, data.horizontalInset)
            footer.padding(.horizontal, data.horizontalInset)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .contentShape(Rectangle())
        .modifier(CoffeePeekCheckInOpenAction(label: data.openLabel, enabled: data.actions.contains("open")) { perform("open") })
    }

    private var header: some View {
        HStack(spacing: 12) {
            actionable("author", label: data.authorLabel) {
                HStack(spacing: 12) {
                    avatar.frame(width: 44, height: 44).clipShape(Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text(data.name).font(CoffeePeekNativeStyle.font(15, weight: "Semibold"))
                        if !data.date.isEmpty {
                            Text(data.date).font(CoffeePeekNativeStyle.font(12, weight: "Medium", relativeTo: .caption))
                                .foregroundStyle(CoffeePeekNativeStyle.secondary)
                        }
                    }
                }
            }
            Spacer(minLength: 0)
            if !data.menu.isEmpty { menu }
        }
    }

    @ViewBuilder private var avatar: some View {
        if let url = data.avatar, !url.isEmpty {
            CoffeePeekCardImage(url: url, symbol: "person.crop.circle", loadImage: loadImage)
        } else {
            CoffeePeekNativeStyle.color(0xF8F1DD).overlay {
                Text(String(data.name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(1)).uppercased())
                    .font(CoffeePeekNativeStyle.font(17, weight: "Semibold"))
                    .foregroundStyle(CoffeePeekNativeStyle.color(0x1C1917))
            }.accessibilityHidden(true)
        }
    }

    private var menu: some View {
        Menu {
            ForEach(data.menu) { item in
                Button(role: item.destructive ? .destructive : nil) { perform(item.id) } label: {
                    Label(item.label, systemImage: menuSymbol(item.id))
                }.disabled(!item.enabled)
            }
        } label: {
            Image(systemName: "ellipsis").frame(width: 44, height: 44)
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
        .accessibilityLabel(data.menuLabel)
    }

    @ViewBuilder private var heading: some View {
        if data.home {
            if data.bag != nil || !data.bean.isEmpty || !data.recipe.isEmpty || !data.title.isEmpty {
                actionable("coffee", label: data.coffeeLabel) { brew }
            }
        } else {
            actionable("shop") {
                Text(data.title).font(CoffeePeekNativeStyle.font(22, weight: "Semibold", relativeTo: .title2))
                    .lineLimit(2).frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        if let rating = data.rating { stars(rating) }
    }

    private var brew: some View {
        HStack(spacing: 12) {
            if let bag = data.bag, !bag.isEmpty {
                CoffeePeekCardImage(url: bag, symbol: nil, fits: true, loadImage: loadImage)
                    .frame(width: 44, height: 54).clipShape(RoundedRectangle(cornerRadius: 4))
            }
            VStack(alignment: .leading, spacing: data.bag == nil ? 8 : 2) {
                if !data.bean.isEmpty { beanTitle }
                else if !data.title.isEmpty { Text(data.title).font(CoffeePeekNativeStyle.font(17, weight: "Semibold")) }
                if !data.recipe.isEmpty {
                    Text(data.recipe).font(CoffeePeekNativeStyle.font(15))
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                }
            }.frame(maxWidth: .infinity, alignment: .leading)
            if data.actions.contains("coffee") {
                Image(systemName: "chevron.right").font(.system(size: 18))
                    .frame(width: 48, height: 48).foregroundStyle(CoffeePeekNativeStyle.secondary).accessibilityHidden(true)
            }
        }.frame(minHeight: data.bag == nil ? 0 : 44)
    }

    private var beanTitle: Text {
        let bean = Text(data.bean).font(CoffeePeekNativeStyle.font(17, weight: "Semibold"))
        guard data.bag != nil, let roaster = data.roaster, !roaster.isEmpty else { return bean }
        let origin = Text(data.separator + roaster).font(CoffeePeekNativeStyle.font(17, weight: "Medium"))
            .foregroundColor(CoffeePeekNativeStyle.secondary)
        return Text("\(bean)\(origin)")
    }

    private func stars(_ value: Double) -> some View {
        HStack(spacing: 8) {
            HStack(spacing: 4) {
                ForEach(0..<5) { index in
                    Image(systemName: "star").font(.system(size: 20))
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                        .overlay(alignment: .leading) {
                            Image(systemName: "star.fill").font(.system(size: 20)).foregroundStyle(CoffeePeekNativeStyle.accent)
                                .mask(alignment: .leading) { Rectangle().frame(width: 22 * min(1, max(0, value - Double(index)))) }
                        }.frame(width: 22, height: 22)
                }
            }
            Text(String(format: "%.1f", value)).font(CoffeePeekNativeStyle.font(15))
        }.accessibilityElement(children: .ignore).accessibilityLabel(data.ratingLabel)
    }

    @ViewBuilder private var publication: some View {
        if let title = data.rejectedTitle {
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(CoffeePeekNativeStyle.font(15, weight: "Semibold"))
                if let reason = data.rejectedReason { Text(reason).font(CoffeePeekNativeStyle.font(13)) }
                if let edit = data.editLabel {
                    Button(edit) { perform("edit") }.disabled(!data.actions.contains("edit")).frame(minHeight: 44)
                }
            }.foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
        if let publication = data.publication {
            Text(publication).font(CoffeePeekNativeStyle.font(12, relativeTo: .caption))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }

    @ViewBuilder private var footer: some View {
        if data.likes != nil || data.drink != nil || data.publishLabel != nil { footerContent }
    }

    private var footerContent: some View {
        HStack(spacing: 8) {
            if let count = data.likes {
                actionable("like", label: data.likesLabel) {
                    HStack(spacing: 4) {
                        Image(systemName: data.liked ? "heart.fill" : "heart").font(.system(size: 24))
                            .foregroundStyle(data.liked ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.foreground)
                        Text(count.formatted()).font(CoffeePeekNativeStyle.font(15)).foregroundStyle(CoffeePeekNativeStyle.foreground)
                    }.frame(minWidth: 48, minHeight: 48, alignment: .leading)
                }.accessibilityLabel(data.likesLabel).accessibilityAddTraits(data.liked ? .isSelected : [])
            }
            Spacer(minLength: 0)
            if let label = data.publishLabel {
                Button(label) { perform("publish") }.disabled(!data.actions.contains("publish")).frame(minHeight: 44)
            }
            if let drink = data.drink {
                Text(drink).font(CoffeePeekNativeStyle.font(15)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
        }
    }

    @ViewBuilder private func actionable<Content: View>(_ action: String, label: String = "",
        @ViewBuilder content: () -> Content) -> some View {
        if data.actions.contains(action) {
            Button { perform(action) } label: { content().contentShape(Rectangle()) }
                .buttonStyle(.plain).accessibilityHint(label)
        } else { content() }
    }

    private func menuSymbol(_ action: String) -> String {
        switch action {
        case "share": "square.and.arrow.up"
        case "edit": "square.and.pencil"
        case "delete": "trash"
        case "hide": "eye.slash"
        case "report": "flag"
        case "block": "nosign"
        default: "ellipsis"
        }
    }
}

private struct CoffeePeekCheckInOpenAction: ViewModifier {
    let label: String
    let enabled: Bool
    let action: () -> Void
    @ViewBuilder func body(content: Content) -> some View {
        if enabled { content.onTapGesture(perform: action).accessibilityAction(named: Text(label)) { action() } }
        else { content }
    }
}

private func coffeePeekCardPreview() -> CoffeePeekCheckInCardData {
    var data = CoffeePeekCheckInCardData(id: "fixture", name: "CoffeePeek", avatar: nil, date: "17 ноября")
    data.title = "CoffeePeek"
    data.note = "Кофе с ягодными нотами."
    data.rating = 4.5
    data.ratingLabel = "4,5 из 5"
    data.likes = 3
    data.liked = true
    data.likesLabel = "Лайки: 3"
    data.drink = "Капучино"
    return data
}

#Preview("Shared check-in light") {
    CoffeePeekCheckInCard(data: coffeePeekCardPreview(), loadImage: { _, _ in nil }, perform: { _ in }, openPhoto: { _ in })
        .preferredColorScheme(.light)
}

#Preview("Shared check-in dark") {
    CoffeePeekCheckInCard(data: coffeePeekCardPreview(), loadImage: { _, _ in nil }, perform: { _ in }, openPhoto: { _ in })
        .preferredColorScheme(.dark)
}

private func coffeePeekRejectedCardPreview() -> CoffeePeekCheckInCardData {
    var data = coffeePeekCardPreview()
    data.home = true
    data.publication = "Виден только вам"
    data.rejectedTitle = "Публикация отклонена"
    data.rejectedReason = "Проверьте описание перед повторной отправкой."
    return data
}

#Preview("Rejected check-in light") {
    CoffeePeekCheckInCard(data: coffeePeekRejectedCardPreview(), loadImage: { _, _ in nil }, perform: { _ in }, openPhoto: { _ in })
        .preferredColorScheme(.light)
}
#Preview("Rejected check-in dark") {
    CoffeePeekCheckInCard(data: coffeePeekRejectedCardPreview(), loadImage: { _, _ in nil }, perform: { _ in }, openPhoto: { _ in })
        .preferredColorScheme(.dark)
}

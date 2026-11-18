import ComposeApp
import SwiftUI
import UIKit

struct ShopCheckInRatingPresentation {
    let value: Double
    let label: String
}

struct ShopCheckInListRatings: View {
    let overall: String
    let count: Int
    let isSample: Bool
    let breakdown: [ShopCheckInRatingPresentation]
    let strings: ShopCheckInListStrings

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ShopCheckInRatingLabel(title: strings.format("shop_checkins_rating_summary", overall, count))
                .font(.headline)
            if isSample { Text(strings.text("shop_checkins_rating_sample")).font(.footnote).foregroundStyle(.secondary) }
            ForEach(Array(breakdown.enumerated()), id: \.offset) { index, rating in
                let name = strings.text(["shop_review_coffee", "shop_review_service", "shop_checkins_aura"][index])
                HStack(spacing: 12) {
                    Text(name)
                    ProgressView(value: min(5, max(0, rating.value)), total: 5).tint(CoffeePeekNativeStyle.accent)
                    Text(rating.label).monospacedDigit()
                }
                .font(.subheadline)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(strings.format("shop_checkins_rating_category", name, rating.label))
            }
        }
    }
}

struct ShopCheckInListRow: Identifiable {
    private let value: IosShopCheckInListRow

    init(_ value: IosShopCheckInListRow) { self.value = value }

    var id: String { value.id }
    var authorName: String { value.authorName }
    var hasAvatar: Bool { value.hasAvatar }
    var date: String { value.date }
    var note: String { value.note }
    var drink: String? { value.drink }
    var ratingLabel: String? { value.ratingLabel }
    var coffeeRating: Int { Int(value.coffeeRating) }
    var serviceRating: Int { Int(value.serviceRating) }
    var placeRating: Int { Int(value.placeRating) }
    var helpfulCount: Int { Int(value.helpfulCount) }
    var markedHelpful: Bool { value.markedHelpful }
    var isOwn: Bool { value.isOwn }
    var isVoting: Bool { value.isVoting }
    var canVote: Bool { value.canVote }
    var canReport: Bool { value.canReport }
    var canBlock: Bool { value.canBlock }
    var canOpenAuthor: Bool { value.canOpenAuthor }
    var showHelpful: Bool { value.showHelpful }
    var photoCount: Int { Int(value.photoCount) }
    func canOpenPhoto(_ index: Int) -> Bool { value.canOpenPhoto(index: Int32(index)) }
}

struct ShopCheckInCardActions {
    let openAuthor: () -> Void
    let vote: () -> Void
    let report: () -> Void
    let block: () -> Void
    let photo: (Int) -> Void
}

struct ShopCheckInListCard: View {
    let row: ShopCheckInListRow
    let date: String
    let strings: ShopCheckInListStrings
    let actions: ShopCheckInCardActions
    let loadArtwork: (Int?, @escaping (UIImage?) -> Void) -> IosShopCheckInObservation?
    @State private var noteExpanded = false
    @State private var noteClipped = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            header
            if let rating = row.ratingLabel {
                ShopCheckInRatingLabel(title: rating)
                    .accessibilityLabel(strings.format("shop_checkins_rating_description", row.coffeeRating,
                                                       row.serviceRating, row.placeRating, rating))
            }
            if !row.note.isEmpty { note }
            if row.photoCount > 0 { photos }
            if row.showHelpful || row.drink != nil { footer }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
        .overlay { RoundedRectangle(cornerRadius: 16).strokeBorder(CoffeePeekNativeStyle.border) }
    }

    private var authorName: String {
        row.authorName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? strings.text(row.isOwn ? "shop_checkins_author_self" : "shop_checkins_author_unknown")
            : row.authorName
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 8) {
            if row.canOpenAuthor {
                Button(action: actions.openAuthor) { author }
                    .buttonStyle(.plain)
                    .accessibilityLabel(strings.format("shop_checkins_open_author", authorName))
                    .accessibilityValue(date)
            } else {
                author.accessibilityElement(children: .combine)
            }
            Spacer(minLength: 0)
            if row.canReport || row.canBlock {
                Menu {
                    if row.canReport { Button(strings.text("shop_checkins_report"), action: actions.report) }
                    if row.canBlock {
                        Button(strings.text("shop_checkins_block_author"), role: .destructive, action: actions.block)
                    }
                } label: {
                    Label(strings.text("shop_checkins_actions"), systemImage: "ellipsis")
                        .labelStyle(.iconOnly)
                        .frame(width: 44, height: 44)
                }
            }
        }
    }

    private var author: some View {
        HStack(spacing: 8) {
            Group {
                if row.hasAvatar {
                    ShopCheckInArtwork(load: { loadArtwork(nil, $0) }, placeholder: "person.crop.circle")
                } else {
                    Image(systemName: "person.crop.circle").resizable().scaledToFit()
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                }
            }
            .frame(width: 36, height: 36)
            .clipShape(Circle())
            .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(authorName).font(.subheadline.bold())
                Text(date).font(.caption).foregroundStyle(.secondary)
            }
        }
        .frame(minHeight: 44)
        .contentShape(Rectangle())
    }

    private var note: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(row.note).font(.subheadline)
                .lineLimit(noteExpanded ? nil : 4)
                .background {
                    GeometryReader { shown in
                        Text(row.note).font(.subheadline)
                            .fixedSize(horizontal: false, vertical: true)
                            .background {
                                GeometryReader { full in
                                    Color.clear.preference(key: ShopCheckInNoteClipped.self,
                                                           value: full.size.height > shown.size.height + 1)
                                }
                            }
                            .hidden()
                    }
                }
                .onPreferenceChange(ShopCheckInNoteClipped.self) { clipped in
                    if !noteExpanded { noteClipped = clipped }
                }
            if noteClipped {
                Button(strings.text(noteExpanded ? "shop_checkins_collapse_note" : "shop_checkins_read_more")) {
                    noteExpanded.toggle()
                }
                .font(.subheadline.bold())
                .frame(minHeight: 44)
            }
        }
    }

    private var photos: some View {
        Color.clear.aspectRatio(4.0 / 3.0, contentMode: .fit).overlay {
            if row.photoCount == 1 { photo(0) }
            else {
                HStack(spacing: 4) {
                    photo(0)
                    if row.photoCount == 2 { photo(1) }
                    else { VStack(spacing: 4) { photo(1); photo(2) } }
                }
            }
        }
    }

    private func photo(_ index: Int) -> some View {
        Button { actions.photo(index) } label: {
            ShopCheckInArtwork(load: { loadArtwork(index, $0) }, placeholder: nil)
                .overlay {
                    if index == 2 && row.photoCount > 3 {
                        Color.black.opacity(0.45).overlay {
                            Text(strings.format("shop_checkins_more_photos", row.photoCount - 3))
                                .font(.title2.bold()).foregroundStyle(.white)
                        }
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .disabled(!row.canOpenPhoto(index))
        .accessibilityLabel(strings.format("shop_checkins_photo_description", index + 1, row.photoCount))
    }

    private var footer: some View {
        HStack {
            if row.showHelpful {
                Button(action: actions.vote) {
                    ShopCheckInHelpfulLabel(selected: row.markedHelpful, count: row.helpfulCount)
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .disabled(!row.canVote)
                .accessibilityLabel(strings.format(helpfulLabel, row.helpfulCount))
            }
            Spacer()
            if let drink = row.drink {
                Text(drink).font(.caption)
                    .padding(.horizontal, 8).padding(.vertical, 4)
                    .background(CoffeePeekNativeStyle.background, in: Capsule())
            }
        }
    }

    private var helpfulLabel: String {
        if row.isVoting { return "shop_checkins_like_updating" }
        if !row.canVote { return "shop_checkins_like_count" }
        return row.markedHelpful ? "shop_checkins_like_remove" : "shop_checkins_like_add"
    }
}

private struct ShopCheckInNoteClipped: PreferenceKey {
    static let defaultValue = false
    static func reduce(value: inout Bool, nextValue: () -> Bool) { value = value || nextValue() }
}

private struct ShopCheckInArtwork: View {
    let load: (@escaping (UIImage?) -> Void) -> IosShopCheckInObservation?
    let placeholder: String?
    @State private var observation: IosShopCheckInObservation?
    @State private var image: UIImage?

    var body: some View {
        GeometryReader { bounds in
            CoffeePeekNativeStyle.background.overlay {
                if let image {
                    Image(uiImage: image).resizable().scaledToFill()
                        .frame(width: bounds.size.width, height: bounds.size.height)
                } else if let placeholder {
                    Image(systemName: placeholder).foregroundStyle(CoffeePeekNativeStyle.secondary)
                } else { CoffeePeekPhotoPlaceholder() }
            }
            .clipped()
        }
        .accessibilityHidden(true)
        .onAppear { if image == nil { observation = load { image = $0 } } }
        .onDisappear { observation?.cancel(); observation = nil }
    }
}

private struct ShopCheckInRatingLabel: View {
    let title: String

    var body: some View {
        Label {
            Text(title).foregroundStyle(CoffeePeekNativeStyle.foreground)
        } icon: {
            Image(systemName: "star.fill").foregroundStyle(CoffeePeekNativeStyle.accent)
        }
    }
}

private struct ShopCheckInHelpfulLabel: View {
    let selected: Bool
    let count: Int

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: selected ? "heart.fill" : "heart")
                .foregroundStyle(selected ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.foreground)
            Text(count.formatted()).foregroundStyle(CoffeePeekNativeStyle.foreground)
        }
    }
}

private func shopCheckInCardComponentsPreview() -> some View {
    VStack(alignment: .leading, spacing: 16) {
        ShopCheckInListRatings(overall: "4.8", count: 24, isSample: false,
            breakdown: [ShopCheckInRatingPresentation(value: 4.8, label: "4.8"),
                        ShopCheckInRatingPresentation(value: 4.5, label: "4.5"),
                        ShopCheckInRatingPresentation(value: 5, label: "5.0")],
            strings: ShopCheckInListStrings(languageTag: "ru"))
        VStack(alignment: .leading, spacing: 12) {
            ShopCheckInRatingLabel(title: "5.0")
            ShopCheckInArtwork(load: { _ in nil }, placeholder: nil)
                .aspectRatio(4.0 / 3.0, contentMode: .fit).clipShape(RoundedRectangle(cornerRadius: 10))
            HStack(spacing: 24) {
                ShopCheckInHelpfulLabel(selected: true, count: 6)
                ShopCheckInHelpfulLabel(selected: false, count: 2)
            }
        }.padding(16).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
    }.padding(16).background(CoffeePeekNativeStyle.background).foregroundStyle(CoffeePeekNativeStyle.foreground)
}

#Preview("Shop check-in components light") {
    shopCheckInCardComponentsPreview().preferredColorScheme(.light)
}
#Preview("Shop check-in components dark") {
    shopCheckInCardComponentsPreview().preferredColorScheme(.dark)
}

import ComposeApp
import SwiftUI

struct ShelfNativeFinishSheet: View {
    let cover: AnyView
    let item: IosShelfItem
    let strings: ShelfNativeStrings
    let rating: Int
    let again: Bool?
    let rate: (Int) -> Void
    let chooseAgain: (Bool) -> Void
    let finish: (Bool) -> Void
    let cancel: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                HStack {
                    Text(strings["shelf_finish_title"]).font(.title3.weight(.semibold))
                    Spacer()
                    Button(strings["shelf_sheet_close"], action: cancel)
                }
                cover.frame(width: 100, height: 132)
                Text([item.roaster, item.title ?? strings["shelf_bag_untitled"]].compactMap { $0 }.joined(separator: " · ")).font(.headline)
                if let days = item.days {
                    Text(days.intValue == 0 ? strings["shelf_added_today"] : String(format: strings["shelf_on_shelf_for"], strings.age(Int(days.intValue))))
                        .foregroundStyle(CoffeePeekNativeStyle.secondary)
                }
                Text(strings["shelf_finish_question"])
                HStack(spacing: 16) {
                    ForEach(1...5, id: \.self) { score in
                        Button { rate(score) } label: {
                            Image(systemName: score <= rating ? "star.fill" : "star").font(.title)
                                .foregroundStyle(CoffeePeekNativeStyle.accent)
                        }
                        .accessibilityLabel("\(strings["shelf_finish_rating"]) \(score)")
                        .accessibilityAddTraits(rating == score ? .isSelected : [])
                    }
                }
                ViewThatFits {
                    HStack { choice(true); choice(false) }
                    VStack { choice(true); choice(false) }
                }
                Button(strings["shelf_finish_confirm"]) { finish(false) }.buttonStyle(.borderedProminent).foregroundStyle(.black)
                Button(strings["shelf_finish_skip"]) { finish(true) }
            }.padding(20)
        }
        .background(CoffeePeekNativeStyle.background)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .presentationDetents([.medium, .large])
    }
    private func choice(_ value: Bool) -> some View {
        Button { chooseAgain(value) } label: {
            Label(strings[value ? "shelf_buy_again" : "shelf_not_again"], systemImage: again == value ? "checkmark.circle.fill" : "circle")
        }.buttonStyle(.bordered)
    }
}

#Preview("Finish bag light") {
    ShelfNativeFinishSheet(cover: AnyView(CoffeePeekPhotoPlaceholder()), item: shelfNativePreviewItem(),
        strings: ShelfNativeStrings("ru"), rating: 5, again: nil, rate: { _ in }, chooseAgain: { _ in }, finish: { _ in }, cancel: {}).preferredColorScheme(.light)
}
#Preview("Finish bag dark") {
    ShelfNativeFinishSheet(cover: AnyView(CoffeePeekPhotoPlaceholder()), item: shelfNativePreviewItem(),
        strings: ShelfNativeStrings("ru"), rating: 5, again: nil, rate: { _ in }, chooseAgain: { _ in }, finish: { _ in }, cancel: {}).preferredColorScheme(.dark)
}

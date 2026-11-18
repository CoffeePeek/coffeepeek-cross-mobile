import ComposeApp
import SwiftUI

struct ShelfNativeFinishSheet: View {
    let screen: IosShelfScreen
    let item: IosShelfItem
    let strings: ShelfNativeStrings
    let finish: (KotlinInt?, KotlinBoolean?) -> Void
    let cancel: () -> Void
    @State private var rating: Int?
    @State private var again: Bool?

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                HStack {
                    Text(strings["shelf_finish_title"]).font(.title3.weight(.semibold))
                    Spacer()
                    Button(strings["shelf_sheet_close"], action: cancel)
                }
                ShelfNativeCover(screen: screen, item: item).frame(width: 100, height: 132)
                Text(item.title ?? strings["shelf_bag_untitled"]).font(.headline)
                Text(strings["shelf_finish_question"])
                HStack(spacing: 16) {
                    ForEach(1...5, id: \.self) { score in
                        Button { rating = rating == score ? nil : score } label: {
                            Image(systemName: score <= (rating ?? 0) ? "star.fill" : "star").font(.title)
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
                Button(strings["shelf_finish_confirm"]) {
                    finish(rating.map { KotlinInt(int: Int32($0)) }, again.map { KotlinBoolean(bool: $0) })
                }.buttonStyle(.borderedProminent).foregroundStyle(.black)
                Button(strings["shelf_finish_skip"]) { finish(nil, nil) }
            }.padding(20)
        }
        .background(CoffeePeekNativeStyle.background)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.accent)
        .presentationDetents([.medium, .large])
    }
    private func choice(_ value: Bool) -> some View {
        Button { again = again == value ? nil : value } label: {
            Label(strings[value ? "shelf_buy_again" : "shelf_not_again"], systemImage: again == value ? "checkmark.circle.fill" : "circle")
        }.buttonStyle(.bordered)
    }
}

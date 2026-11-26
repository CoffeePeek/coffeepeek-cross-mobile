import ComposeApp
import SwiftUI
import UIKit

struct CoffeeDetailStrings {
    let languageTag: String
    private let bundle: Bundle

    init(languageTag: String) {
        self.languageTag = languageTag
        bundle = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
            .flatMap { Bundle(url: $0) } ?? .main
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "CoffeeDetail")
    }

    func optional(_ prefix: String, _ value: String?) -> String? {
        guard let value else { return nil }
        let key = prefix + value
        let translated = text(key)
        return translated == key ? nil : translated
    }

    func format(_ key: String, _ value: String) -> String {
        String(format: text(key), value)
    }
}

struct CoffeeDetailImage: View {
    let url: String?
    let images: IosCoffeeDetail?
    @State private var observation: IosCoffeeDetailObservation?
    @State private var image: UIImage?

    var body: some View {
        Color(uiColor: .secondarySystemBackground)
            .overlay {
                if let image {
                    Image(uiImage: image).resizable().scaledToFit()
                } else {
                    CoffeePeekPhotoPlaceholder()
                }
            }
            .accessibilityHidden(true)
            .onAppear(perform: load)
            .onChange(of: url) { _, _ in load() }
            .onDisappear(perform: clear)
    }

    private func load() {
        clear()
        guard let url, !url.isEmpty else { return }
        observation = images?.loadImage(url: url, onImage: { [image = $image] in image.wrappedValue = $0 })
    }

    private func clear() {
        observation?.cancel()
        observation = nil
        image = nil
    }
}

struct CoffeeDetailDescription: View {
    let text: String
    let expanded: Bool
    let strings: CoffeeDetailStrings
    let toggle: () -> Void
    @State private var displayedHeight: CGFloat = 0
    @State private var fullHeight: CGFloat = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(text)
                .lineLimit(expanded ? nil : 4)
                .background(alignment: .topLeading) { measurement }
                .background {
                    GeometryReader { geometry in
                        Color.clear.onChange(of: geometry.size.height, initial: true) { _, height in
                            displayedHeight = height
                        }
                    }
                }
            if expanded || fullHeight > displayedHeight + 1 {
                Button(strings.text(expanded ? "coffee_collapse" : "coffee_read_more"), action: toggle)
            }
        }
        .foregroundStyle(.secondary)
    }

    private var measurement: some View {
        Text(text)
            .fixedSize(horizontal: false, vertical: true)
            .hidden()
            .accessibilityHidden(true)
            .background {
                GeometryReader { geometry in
                    Color.clear.onChange(of: geometry.size.height, initial: true) { _, height in
                        fullHeight = height
                    }
                }
            }
    }
}

struct CoffeeDetailField: View {
    let title: String
    let value: String

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(title).foregroundStyle(.secondary)
            Spacer(minLength: 8)
            Text(value).multilineTextAlignment(.trailing)
        }
        .font(.subheadline)
        .accessibilityElement(children: .combine)
    }
}

struct CoffeeDetailOfferCard: View {
    let offer: IosCoffeeOffer
    let strings: CoffeeDetailStrings
    let open: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text(coffeeWeight(offer.weight, strings)).font(.headline)
                Spacer()
                Text(strings.text("coffee_availability_" + offer.availability))
                    .font(.subheadline)
                    .foregroundStyle(availabilityColor)
            }
            Text(coffeePrice(offer, strings)).font(.title3.bold())
            Button(strings.text("coffee_open_seller"), action: open)
                .buttonStyle(.borderedProminent)
                .disabled(!offer.canOpenSeller)
            fields
        }
        .padding(14)
        .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
    }

    private var fields: some View {
        Group {
            field("coffee_seller", value: offer.seller)
            field("coffee_grind", value: strings.optional("coffee_form_", offer.grind))
            field("coffee_brew", value: offer.brew ?? strings.text("coffee_unconfirmed"))
            field("coffee_source", value: sourceLabel)
            if let date = offer.checkedAt { field("coffee_checked", value: date) }
        }
    }

    private var availabilityColor: Color {
        switch offer.availability {
        case "InStock": .green
        case "OutOfStock": .red
        case "PreOrder": .accentColor
        default: .secondary
        }
    }

    private var sourceLabel: String? {
        guard let source = offer.source, ["online", "offline", "both"].contains(source) else { return nil }
        return strings.text("coffee_" + source)
    }

    private func field(_ key: String, value: String?) -> some View {
        CoffeeDetailField(title: strings.text(key), value: value ?? strings.text("coffee_unspecified"))
    }
}

struct CoffeeSimilarCard: View {
    let coffee: IosSimilarCoffee
    let images: IosCoffeeDetail?
    let strings: CoffeeDetailStrings
    let open: () -> Void
    @State private var selectedWeight: String?

    init(coffee: IosSimilarCoffee, images: IosCoffeeDetail?, strings: CoffeeDetailStrings, open: @escaping () -> Void) {
        self.coffee = coffee
        self.images = images
        self.strings = strings
        self.open = open
        _selectedWeight = State(initialValue: coffee.initialWeight)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Button(action: open) { summary }.buttonStyle(.plain)
            HStack {
                Text(coffeePrice(selectedOffer, strings)).font(.headline)
                Spacer()
                weights
            }
        }
        .padding(12)
        .frame(width: 246)
        .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
    }

    private var selectedOffer: IosCoffeeOffer? {
        coffee.offers.first { $0.weight == selectedWeight } ?? coffee.offers.first
    }

    private var summary: some View {
        VStack(alignment: .leading, spacing: 6) {
            CoffeeDetailImage(url: coffee.photo, images: images)
                .aspectRatio(1.4, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(alignment: .bottomTrailing) { roasterBadge.padding(8) }
            Text(coffee.name).font(.headline).lineLimit(1)
            if !coffee.countries.isEmpty { Text(coffee.countries).font(.footnote).foregroundStyle(.secondary) }
            if !coffee.tastes.isEmpty {
                Text(coffee.tastes.joined(separator: " · ")).font(.footnote).foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
    }

    private var roasterBadge: some View {
        Group {
            if let logo = coffee.roasterPhoto, !logo.isEmpty {
                CoffeeDetailImage(url: logo, images: images)
            } else {
                Text(String(coffee.roasterName.prefix(2)).uppercased()).font(.caption.bold())
            }
        }
        .frame(width: 40, height: 40)
        .background(Color(uiColor: .systemBackground), in: Circle())
        .clipShape(Circle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(coffee.roasterName)
    }

    @ViewBuilder
    private var weights: some View {
        if coffee.offers.count > 1 {
            Menu(coffeeWeight(selectedOffer?.weight, strings)) {
                ForEach(coffee.offers.indices, id: \.self) { index in
                    let weight = coffee.offers[index].weight
                    Button(coffeeWeight(weight, strings)) { selectedWeight = weight }
                }
            }
        } else {
            Text(coffeeWeight(selectedOffer?.weight, strings)).font(.caption)
        }
    }
}

private func coffeeWeight(_ weight: String?, _ strings: CoffeeDetailStrings) -> String {
    weight.map { strings.format("coffee_weight", $0) } ?? strings.text("coffee_weight_missing")
}

private func coffeePrice(_ offer: IosCoffeeOffer?, _ strings: CoffeeDetailStrings) -> String {
    guard let offer, let price = offer.price else { return strings.text("coffee_price_missing") }
    return [price, offer.currency ?? ""].filter { !$0.isEmpty }.joined(separator: " ")
}

enum CoffeeDetailSection: String, CaseIterable {
    case photos = "Photos", roaster = "Roaster", offers = "Offers", similarCoffees = "SimilarCoffees"
}

let coffeeDetailScrollSpace = "coffee-detail-scroll"

struct CoffeeDetailFrames: PreferenceKey {
    static let defaultValue: [CoffeeDetailSection: CGRect] = [:]

    static func reduce(value: inout [CoffeeDetailSection: CGRect], nextValue: () -> [CoffeeDetailSection: CGRect]) {
        value.merge(nextValue()) { _, next in next }
    }
}

extension View {
    @ViewBuilder
    func coffeeDetailSection(_ section: CoffeeDetailSection?) -> some View {
        if let section {
            background {
                GeometryReader { geometry in
                    Color.clear.preference(key: CoffeeDetailFrames.self,
                                           value: [section: geometry.frame(in: .named(coffeeDetailScrollSpace))])
                }
            }
        } else {
            self
        }
    }
}

func coffeeDetailFraction(_ frame: CGRect?, in viewport: CGSize) -> Float {
    guard let frame, frame.width > 0, frame.height > 0 else { return 0 }
    let clipped = frame.intersection(CGRect(origin: .zero, size: viewport))
    guard !clipped.isNull else { return 0 }
    return Float((clipped.width * clipped.height) / (frame.width * frame.height))
}

@MainActor
private func offerPreview(_ languageTag: String) -> some View {
    CoffeeDetailOfferCard(
        offer: IosCoffeeOffer(price: "24.5", currency: "BYN", weight: "250", availability: "InStock", seller: "CoffeePeek",
                              grind: "whole_beans", brew: nil, source: "online", checkedAt: nil, canOpenSeller: true),
        strings: CoffeeDetailStrings(languageTag: languageTag),
        open: {}
    )
    .padding()
}

#Preview("Offer light") { offerPreview("ru").preferredColorScheme(.light) }
#Preview("Offer dark") { offerPreview("be").preferredColorScheme(.dark) }

#Preview("Coffee image placeholder light") {
    CoffeeDetailImage(url: nil, images: nil).frame(width: 160, height: 200).preferredColorScheme(.light)
}

#Preview("Coffee image placeholder dark") {
    CoffeeDetailImage(url: nil, images: nil).frame(width: 160, height: 200).preferredColorScheme(.dark)
}

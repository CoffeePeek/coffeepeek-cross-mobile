import ComposeApp
import SwiftUI

// Feature DTOs end here; feed and profile use the same renderer below this adapter.
struct OwnProfileCheckInCard: View {
    let checkIn: IosOwnProfileCheckIn
    let strings: OwnProfileStrings
    let profile: IosOwnProfile?
    let openShop: (String) -> Void
    let openCoffee: (String) -> Void
    let openPhoto: (Int) -> Void
    var openEntry: (() -> Void)?
    var imageLoader: ProfileImageLoader?
    var openMenu: (() -> Void)?
    var report: (() -> Void)?
    var ratingDescription: String?
    var onLike: (() -> Void)?
    var likeEnabled = true

    var body: some View {
        CoffeePeekCheckInCard(data: presentation, loadImage: loadImage, perform: perform, openPhoto: openPhoto)
    }

    private var loadImage: CoffeePeekCardImageLoader {
        let loader = imageLoader ?? profileImageLoader(profile)
        return { url, complete in
            guard let observation = loader(url, complete) else { return nil }
            return CoffeePeekCardImageRequest { observation.cancel() }
        }
    }

    private var presentation: CoffeePeekCheckInCardData {
        var data = CoffeePeekCheckInCardData(id: checkIn.id, name: checkIn.name, avatar: checkIn.avatar,
                                           date: profileCardDate(checkIn.date, language: strings.languageTag))
        data.menuLabel = strings["profile_check_in_actions"]
        data.openLabel = strings["profile_check_in_open"]
        data.coffeeLabel = strings["profile_open_coffee"]
        data.home = checkIn.home
        data.title = checkIn.home ? strings["history_activity_at_home"] : checkIn.shop.isEmpty ? strings["profile_favorite_shop"] : checkIn.shop
        data.note = checkIn.note
        data.rating = checkIn.score.flatMap { Double($0.replacingOccurrences(of: ",", with: ".")) }
        data.ratingLabel = ratingDescription ?? String(format: strings["profile_native_rating"], checkIn.score ?? "")
        data.likes = Int(checkIn.helpfulCount)
        data.liked = checkIn.liked
        data.likesLabel = strings.count(onLike == nil ? "profile_check_in_likes" :
            checkIn.liked ? "profile_check_in_unlike" : "profile_check_in_like", Int(checkIn.helpfulCount))
        data.drink = checkIn.drink
        data.photos = checkIn.photos.enumerated().map { index, photo in
            CoffeePeekCheckInPhoto(id: "\(checkIn.id):\(index):\(photo.url ?? photo.fallbackUrl ?? "")", preview: photo.url, detail: photo.fallbackUrl,
                label: String(format: strings["profile_native_photo"], index + 1, checkIn.photos.count), canOpen: photo.canOpen)
        }
        appendBrew(to: &data)
        appendActions(to: &data)
        return data
    }

    private func appendBrew(to data: inout CoffeePeekCheckInCardData) {
        guard let brew = checkIn.brew else { return }
        data.bean = brew.bean?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        data.roaster = brew.roaster
        data.bag = brew.cover?.trimmingCharacters(in: .whitespacesAndNewlines)
        if data.bag?.isEmpty == true { data.bag = nil }
        if data.bean.isEmpty, data.bag != nil { data.bean = strings["profile_shelf_bag"] }
        data.recipe = profileCardRecipe(brew, strings: strings)
        data.separator = strings["profile_check_in_separator"]
    }

    private func appendActions(to data: inout CoffeePeekCheckInCardData) {
        if onLike != nil && likeEnabled { data.actions.insert("like") }
        if openEntry != nil { data.actions.insert("open") }
        if let slug = checkIn.shopSlug, !slug.isEmpty { data.actions.insert("shop") }
        if let coffee = checkIn.brew?.coffee, !coffee.isEmpty, data.bag != nil { data.actions.insert("coffee") }
        if report != nil { data.menu.append(CoffeePeekCheckInMenuItem(id: "report", label: strings["profile_check_in_report"])) }
        if openMenu != nil {
            data.menu.append(CoffeePeekCheckInMenuItem(id: "block", label: strings["profile_block_author"], destructive: true))
        }
    }

    private func perform(_ action: String) {
        switch action {
        case "like": if likeEnabled { onLike?() }
        case "open": openEntry?()
        case "shop": if let slug = checkIn.shopSlug { openShop(slug) }
        case "coffee": if let slug = checkIn.brew?.coffee { openCoffee(slug) }
        case "report": report?()
        case "block": openMenu?()
        default: break
        }
    }
}

private func profileCardRecipe(_ brew: IosOwnProfileBrew, strings: OwnProfileStrings) -> String {
    var values: [String] = []
    if let method = brew.method, !method.isEmpty {
        let key = "brew_\(method)"
        let label = strings[key]
        values.append(label == key ? method : label)
    }
    if let dose = brew.dose { values.append(String(format: strings["profile_check_in_dose"], dose)) }
    if brew.seconds >= 0 { values.append(strings.count("profile_check_in_seconds", Int(brew.seconds))) }
    if brew.taste >= 0 { values.append(strings.count("profile_check_in_taste", Int(brew.taste))) }
    return values.joined(separator: strings["profile_check_in_separator"])
}

private func profileCardDate(_ value: String, language: String) -> String {
    guard let day = value.split(separator: " ", maxSplits: 1).first else { return value }
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.dateFormat = "dd.MM.yyyy"
    guard let date = formatter.date(from: String(day)) else { return value }
    formatter.locale = Locale(identifier: language)
    formatter.dateFormat = "d MMM yyyy"
    return formatter.string(from: date)
}

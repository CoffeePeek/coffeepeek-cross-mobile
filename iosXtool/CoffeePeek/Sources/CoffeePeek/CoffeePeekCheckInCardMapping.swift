import ComposeApp
import SwiftUI

extension CoffeePeekCheckInCardData {
    init(state: IosCommunityCardState) {
        self.init(id: state.key, name: state.header.name, avatar: state.header.avatar, date: state.header.dateLabel)
        authorLabel = state.header.openAuthorLabel
        menuLabel = state.menuLabel
        openLabel = state.openLabel
        horizontalInset = CGFloat(state.horizontalInset)
        actions = Set(state.availableActions)
        menu = state.menu.map { CoffeePeekCheckInMenuItem(id: $0.id, label: $0.label,
                                                          enabled: $0.enabled, destructive: $0.destructive) }
        applyBody(state)
        applyFooter(state)
    }

    private mutating func applyBody(_ state: IosCommunityCardState) {
        let body = state.body
        home = body.isHome
        title = body.isHome ? "" : body.title
        note = body.note
        rating = body.rating?.doubleValue
        ratingLabel = body.ratingLabel
        bean = body.beanName ?? ""
        roaster = body.roasterName
        bag = body.bagUrl
        recipe = body.recipe
        separator = body.recipeSeparator
        coffeeLabel = body.openCoffeeLabel
        photos = body.photos.enumerated().map { index, photo in
            CoffeePeekCheckInPhoto(id: "\(state.key):\(index):\(photo.previewUrl)",
                                  preview: photo.previewUrl, detail: photo.detailUrl, label: photo.label)
        }
    }

    private mutating func applyFooter(_ state: IosCommunityCardState) {
        likes = state.footer.likes.map { Int($0.intValue) }
        liked = state.footer.liked
        likesLabel = state.footer.likesLabel
        drink = state.footer.drink
        publishLabel = state.footer.publishLabel
        publication = state.status.publication
        rejectedTitle = state.status.rejectedTitle
        rejectedReason = state.status.rejectedReason
        editLabel = state.status.editLabel
    }
}

import SwiftUI
import UIKit

struct CoffeePeekCheckInPhoto: Identifiable, Equatable {
    let id: String
    let preview: String?
    let detail: String?
    let label: String
    var canOpen = true
}

struct CoffeePeekCheckInMenuItem: Identifiable {
    let id: String
    let label: String
    var enabled = true
    var destructive = false
}

// Display values and allowed actions come from the feature boundary, never from this renderer.
struct CoffeePeekCheckInCardData {
    var id: String
    var name: String
    var avatar: String?
    var date: String
    var authorLabel = ""
    var menuLabel = ""
    var openLabel = ""
    var coffeeLabel = ""
    var horizontalInset: CGFloat = 16
    var home = false
    var title = ""
    var note = ""
    var rating: Double?
    var ratingLabel = ""
    var bean = ""
    var roaster: String?
    var bag: String?
    var recipe = ""
    var separator = ""
    var photos: [CoffeePeekCheckInPhoto] = []
    var likes: Int?
    var liked = false
    var likesLabel = ""
    var drink: String?
    var publication: String?
    var rejectedTitle: String?
    var rejectedReason: String?
    var editLabel: String?
    var publishLabel: String?
    var menu: [CoffeePeekCheckInMenuItem] = []
    var actions: Set<String> = []
}

final class CoffeePeekCardImageRequest {
    private var cancelAction: (() -> Void)?
    init(cancel: @escaping () -> Void) { cancelAction = cancel }
    func cancel() {
        let action = cancelAction
        cancelAction = nil
        action?()
    }
    deinit { cancelAction?() }
}

typealias CoffeePeekCardImageLoader = (String, @escaping (UIImage?) -> Void) -> CoffeePeekCardImageRequest?

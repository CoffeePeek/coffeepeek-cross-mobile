import ComposeApp
import SwiftUI

struct CommunityFeedNativePresentation {
    var active = true
    var revision: Int64 = 0
    var sessionGeneration: Int64 = 0
    var title = ""
    var loading = false
    var refreshing = false
    var loadingMore = false
    var canLoadMore = false
    var guest = false
    var interactive = true
    var labels = CommunityFeedNativeLabels()
    var confirmedIds: [String] = []
    var rows: [CommunityFeedNativeRow] = []
    var bottomClearance: CGFloat = 0
}

struct CommunityFeedNativeLabels {
    var loading = ""
    var errorTitle = ""
    var errorText = ""
    var retry = ""
    var empty = ""
    var signIn = ""
    var refresh = ""
    var confirmedTitle = ""
    var confirmedBody = ""
    var confirmedOpen = ""
}

struct CommunityFeedNativeRow: Identifiable {
    let id: AnyHashable
    let key: String
    var card: IosCommunityCard? = nil
    var preview: CoffeePeekCheckInCardData? = nil
    var teaser = false
}

struct CommunityFeedNativeActions {
    var perform: (String) -> Void = { _ in }
    var refresh: () async -> Void = {}
    var visible: (String, Double) -> Void = { _, _ in }
}

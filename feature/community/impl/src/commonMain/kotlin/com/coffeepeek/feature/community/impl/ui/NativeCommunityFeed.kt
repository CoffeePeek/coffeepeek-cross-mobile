package com.coffeepeek.feature.community.impl.ui

import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.impl.timeline.*

internal data class NativeCommunityFeedTexts(
    val title: String, val loading: String, val errorTitle: String, val errorBody: String,
    val restart: String, val retry: String, val empty: String, val signIn: String,
    val refresh: String, val savedTitle: String, val savedBody: String, val savedOpen: String,
)

internal fun nativeFeedPresentation(state: CommunityState, cards: List<CommunityCardPresentation>,
    bottomClearance: Double, interactive: Boolean, texts: NativeCommunityFeedTexts): CommunityFeedPresentation {
    return CommunityFeedPresentation(state.sessionGeneration, texts.title, cards,
        cards.getOrNull(1)?.key.takeIf { state.isLoggedIn == false }, state.visibleConfirmedSaves,
        nativeFeedLoading(state), state.isLoading, state.isLoadingMore, nativeFeedCanLoadMore(state, interactive),
        state.isLoggedIn == false, interactive, texts.loading,
        texts.errorTitle.takeIf { state.error != null }, texts.errorBody.takeIf { state.error != null },
        if (state.restartPagination) texts.restart else texts.retry, texts.empty, texts.signIn,
        texts.refresh, texts.savedTitle, texts.savedBody, texts.savedOpen, bottomClearance, state.accountEpoch)
}

internal fun nativeFeedLoading(state: CommunityState): Boolean =
    (state.isLoggedIn == null || state.isLoading && state.items.isEmpty()) && state.visibleConfirmedSaves.isEmpty()

internal fun nativeFeedCanLoadMore(state: CommunityState, interactive: Boolean): Boolean =
    interactive && state.isLoggedIn == true && state.hasMore && !state.isLoading && !state.isLoadingMore &&
        !state.isMutating && state.error == null

/** Keep every rendered key while an overlay owns interaction; the bridge may retain the card's media. */
internal fun nativeFeedCardActions(cards: Map<String, CommunityCardActions>, interactive: Boolean): Map<String, CommunityCardActions> =
    cards.mapValues { (_, actions) -> if (interactive) actions else CommunityCardActions(emptyMap()) {} }

internal fun nativeFeedCommands(state: CommunityState, interactive: Boolean, send: (CommunityAction) -> Unit,
    onSignIn: () -> Unit, onOpenPost: (String) -> Unit, onRefreshed: () -> Unit): Map<String, () -> Unit> {
    if (!interactive) return emptyMap()
    return buildMap {
        if (!state.isMutating) put("refresh") {
            send(CommunityAction.Refresh(FeedTrigger.Gesture))
            onRefreshed()
        }
        if (state.error != null) put("retry") { send(CommunityAction.Retry) }
        if (nativeFeedCanLoadMore(state, interactive)) put("loadMore") { send(CommunityAction.LoadMore()) }
        if (state.isLoggedIn == false) put("signIn", onSignIn)
        state.visibleConfirmedSaves.forEach { id -> put("openConfirmed:$id") { onOpenPost(id) } }
    }
}

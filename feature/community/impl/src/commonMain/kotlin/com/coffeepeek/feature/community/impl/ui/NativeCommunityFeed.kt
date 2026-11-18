package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.impl.resources.*
import com.coffeepeek.feature.community.impl.timeline.*

@Composable
internal fun NativeCommunityFeed(state: CommunityState, posts: List<CommunityPost>, onAction: (CommunityAction) -> Unit,
    host: CommunityHost, block: AuthorBlockActions, callbacks: CommunityCallbacks, cardCallbacks: CommunityFeedCardCallbacks,
    snackbar: SnackbarHostState, bottomClearance: Dp, onRefreshed: () -> Unit,
    headerAction: @Composable () -> Unit, interactive: Boolean) {
    val sharer = postSharer(host)
    val cardActions = linkedMapOf<String, CommunityCardActions>()
    val cards = posts.mapIndexed { index, post ->
        val teaser = state.isLoggedIn == false && index > 0
        val actions = if (teaser) PostCardActions(null, null, null, null, { callbacks.onSignIn() })
            else postActions(post, state, onAction, callbacks, block, sharer, cardCallbacks.onHomeEditorOpened, cardCallbacks.onPhoto)
        val card = nativePostPresentation(post, state.owns(post), state.timeline == CommunityTimeline.Public,
            state.timeline == CommunityTimeline.Public || post.helpfulCount > 0, !state.isMutating && !teaser,
            state.changingVisibilityId == post.id, actions, host, 16.0)
        cardActions[card.key] = nativePostActions(actions, !state.isMutating && !teaser, state.changingVisibilityId == post.id)
        card
    }
    host.nativeFeed?.invoke(nativeFeedPresentation(state, cards, bottomClearance, interactive),
        CommunityFeedActions(nativeFeedCommands(state, interactive, onAction, callbacks.onSignIn, callbacks.onOpenPost, onRefreshed),
            nativeFeedCardActions(cardActions, interactive)),
        Modifier.fillMaxSize(), headerAction,
        { host.snackbar(snackbar, bottomClearance) })
}

@Composable
private fun nativeFeedPresentation(state: CommunityState, cards: List<CommunityCardPresentation>,
    bottomClearance: Dp, interactive: Boolean): CommunityFeedPresentation {
    return CommunityFeedPresentation(state.sessionGeneration, stringResource(Res.string.community_title), cards,
        cards.getOrNull(1)?.key.takeIf { state.isLoggedIn == false }, state.visibleConfirmedSaves,
        nativeFeedLoading(state), state.isLoading, state.isLoadingMore, nativeFeedCanLoadMore(state, interactive),
        state.isLoggedIn == false, interactive, stringResource(Res.string.community_loading),
        stringResource(Res.string.community_error_title).takeIf { state.error != null },
        stringResource(Res.string.community_error_body).takeIf { state.error != null },
        stringResource(if (state.restartPagination) Res.string.community_restart else Res.string.community_retry),
        stringResource(Res.string.community_empty), stringResource(Res.string.community_sign_in_to_read),
        stringResource(Res.string.community_refresh), stringResource(Res.string.community_saved_title),
        stringResource(Res.string.community_saved_body), stringResource(Res.string.community_saved_open),
        bottomClearance.value.toDouble(), state.accountEpoch)
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

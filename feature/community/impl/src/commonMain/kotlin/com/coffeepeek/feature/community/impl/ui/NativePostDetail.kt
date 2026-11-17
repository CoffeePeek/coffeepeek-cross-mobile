package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.impl.post.*
import com.coffeepeek.feature.community.impl.resources.*

@Composable
internal fun NativePostDetail(state: PostState, onAction: (PostAction) -> Unit, host: CommunityHost,
    block: AuthorBlockActions, callbacks: PostCallbacks, snackbar: SnackbarHostState, modifier: Modifier,
    onPhoto: (PostPhotoPreview) -> Unit): Boolean {
    val fallback = stringResource(Res.string.community_user)
    val sharer = postSharer(host)
    val actions = state.post?.let { postActions(it, state, onAction, callbacks, block, fallback, sharer, onPhoto) }
    val card = state.post?.let { nativePostPresentation(it, state.isOwn, !state.isOwn,
        state.isPublished || it.helpfulCount > 0, !state.isMutating, false, requireNotNull(actions), host, 16.0) }
    val native = actions?.let { nativePostActions(it, !state.isMutating, false) }
    val commands = native?.actions.orEmpty() + mapOf("back" to callbacks.onBack, "retry" to { onAction(PostAction.Retry) })
    return host.nativeDetail?.invoke(nativePostDetailState(state, card),
        CommunityCardActions(commands) { native?.onPhoto?.invoke(it) }, modifier,
        { host.snackbar(snackbar, 0.dp) }) == true
}

@Composable
private fun nativePostDetailState(state: PostState, card: CommunityCardPresentation?): CommunityPostPresentation {
    val unavailable = state.error == PostError.Unavailable
    val failed = state.error == PostError.Failed
    return CommunityPostPresentation(card, stringResource(Res.string.community_post_title), stringResource(Res.string.community_back),
        loadingLabel = if (card == null && !unavailable && !failed) stringResource(Res.string.community_loading) else null,
        errorTitle = if (card != null || state.error == null) null else stringResource(
            if (unavailable) Res.string.community_post_unavailable_title else Res.string.community_error_title),
        errorText = if (card != null || state.error == null) null else stringResource(
            if (unavailable) Res.string.community_post_unavailable_body else Res.string.community_error_body),
        retryLabel = if (card == null && failed) stringResource(Res.string.community_retry) else null)
}

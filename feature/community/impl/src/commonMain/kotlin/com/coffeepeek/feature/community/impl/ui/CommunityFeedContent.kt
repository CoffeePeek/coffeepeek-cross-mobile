package com.coffeepeek.feature.community.impl.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.reducedMotionDuration
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.community.api.CommunityFloating
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.api.ExposureKind
import com.coffeepeek.feature.community.api.FeedItemAction
import com.coffeepeek.feature.community.api.FeedTrigger
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.impl.resources.*
import com.coffeepeek.feature.community.impl.timeline.CommunityAction
import com.coffeepeek.feature.community.impl.timeline.CommunityState
import com.coffeepeek.feature.community.impl.timeline.CommunityTimeline

private class PhotoPreview(val generation: Long, val thumbnails: List<String>, val originals: List<String>, val index: Int)

@Composable
internal fun CommunityFeedContent(state: CommunityState, onAction: (CommunityAction) -> Unit,
    host: CommunityHost, blockActions: AuthorBlockActions, callbacks: CommunityCallbacks,
    snackbar: SnackbarHostState, bottomClearance: Dp, listState: LazyListState, onRefreshed: () -> Unit,
    floatingAction: @Composable BoxScope.(CommunityFloating) -> Unit) {
    val guest = state.isLoggedIn == false
    val posts = if (guest) state.items.take(2) else state.items
    var preview by remember { mutableStateOf<PhotoPreview?>(null) }
    var homeEditorOpened by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (homeEditorOpened) {
            homeEditorOpened = false
            onAction(CommunityAction.Refresh())
        }
    }
    ResetFeedOnSessionChange(state.sessionGeneration, listState) { preview = null }
    host.feedViewed(state.firstPageCount, state.timeline == CommunityTimeline.Public)
    host.prefetchPhotos(listState, posts.map { it.id to it.photos.thumbnails.take(3) })
    if (!guest) CommunityPagination(state, listState, onAction)
    preview?.takeIf { it.generation == state.sessionGeneration }?.let {
        host.photoViewer(it.thumbnails, it.originals, it.index) { preview = null }
    }
    if (state.editing != null) key(state.sessionGeneration, state.editing.source.id) { EditSheet(state, onAction) }
    if (state.reportingId != null) ReportSheet(state.reportForm(), reportCallbacks(onAction))
    val sharer = postSharer(host)
    Box(Modifier.fillMaxSize()) {
        CommunityFeedList(state, posts, listState, onAction, host, snackbar, bottomClearance, onRefreshed, guest, callbacks.onSignIn) { index, post ->
            if (guest && index > 0) {
                CommunityGuestTeaser(post, host, callbacks.onSignIn)
            } else {
                CommunityFeedPost(index, post, state, host, postActions(post, state, onAction, callbacks, blockActions,
                    sharer, { homeEditorOpened = true }) { thumbnails, originals, photoIndex ->
                    preview = PhotoPreview(state.sessionGeneration, thumbnails, originals, photoIndex)
                })
            }
        }
        if (guest && posts.isNotEmpty()) {
            GuestFeedSignInButton(bottomClearance, callbacks.onSignIn)
        } else {
            floatingAction(CommunityFloating(state.isLoggedIn, state.sessionGeneration) { onAction(CommunityAction.Refresh()) })
        }
    }
}

@Composable
private fun BoxScope.GuestFeedSignInButton(bottomClearance: Dp, onSignIn: () -> Unit) {
    CoffeePeekButton(
        text = stringResource(Res.string.community_sign_in_to_read),
        onClick = onSignIn,
        modifier = Modifier.align(Alignment.BottomCenter).padding(
            start = CoffeePeekDimens.spacing4,
            end = CoffeePeekDimens.spacing4,
            bottom = bottomClearance + CoffeePeekDimens.spacing3,
        ),
    )
}

@Composable
private fun ResetFeedOnSessionChange(generation: Long, listState: LazyListState, closePreview: () -> Unit) {
    var displayedGeneration by rememberSaveable { mutableStateOf(generation) }
    LaunchedEffect(generation) {
        if (displayedGeneration != generation) {
            closePreview()
            listState.scrollToItem(0)
            displayedGeneration = generation
        }
    }
}

@Composable
private fun CommunityPagination(state: CommunityState, listState: LazyListState,
    onAction: (CommunityAction) -> Unit) {
    val loadMore by remember(state.items.size, state.hasMore, state.isLoading, state.isLoadingMore, state.error, state.isMutating) {
        derivedStateOf {
            state.hasMore && !state.isLoading && !state.isLoadingMore && !state.isMutating && state.error == null &&
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.items.size - 3
        }
    }
    LaunchedEffect(loadMore) { if (loadMore) onAction(CommunityAction.LoadMore()) }
}

@Composable
private fun CommunityFeedList(state: CommunityState, posts: List<CommunityPost>, listState: LazyListState,
    onAction: (CommunityAction) -> Unit, host: CommunityHost, snackbar: SnackbarHostState, bottomClearance: Dp,
    onRefreshed: () -> Unit, guest: Boolean, onSignIn: () -> Unit, post: @Composable (Int, CommunityPost) -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0),
        snackbarHost = { host.snackbar(snackbar, bottomClearance) }) { padding ->
        Crossfade(targetState = state.isLoggedIn == null || (state.isLoading && state.items.isEmpty()),
            animationSpec = tween(reducedMotionDuration(220)), label = "feed-load-status") { loading ->
            if (loading) {
                Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) { CenteredLoader() }
            } else CoffeePeekPullToRefresh(listState, isRefreshing = state.isLoading && state.items.isNotEmpty(),
                onRefresh = {
                    onAction(CommunityAction.Refresh(FeedTrigger.Gesture))
                    onRefreshed()
                },
                refreshDescription = stringResource(Res.string.community_refresh),
                loadingDescription = stringResource(Res.string.community_loading),
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) { scrollModifier ->
                LazyColumn(state = listState, modifier = host.exposure(scrollModifier.fillMaxSize(), ExposureKind.Feed),
                    contentPadding = PaddingValues(start = CoffeePeekDimens.spacing4, end = CoffeePeekDimens.spacing4,
                        top = CoffeePeekDimens.spacing2, bottom = bottomClearance + 88.dp),
                    verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                    itemsIndexed(posts, key = { _, item -> item.id },
                        contentType = { _, item -> if (item.isHome) "home-post" else "shop-post" }) { index, item -> post(index, item) }
                    communityFeedStatus(state, guest, onSignIn, onAction)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.communityFeedStatus(state: CommunityState,
    guest: Boolean, onSignIn: () -> Unit, onAction: (CommunityAction) -> Unit) {
    when {
        state.error != null -> item(key = "error") {
            ErrorBlock({ onAction(CommunityAction.Retry) }, stringResource(
                if (state.restartPagination) Res.string.community_restart else Res.string.community_retry))
        }
        state.items.isEmpty() && !state.isLoading -> item(key = "empty") {
            Text(stringResource(Res.string.community_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (guest) CoffeePeekButton(stringResource(Res.string.community_sign_in_to_read), onSignIn)
        }
    }
    if (state.isLoadingMore) item(key = "loading-more") { CenteredLoader() }
}

@Composable
private fun CommunityFeedPost(index: Int, post: CommunityPost, state: CommunityState,
    host: CommunityHost, actions: PostCardActions) {
    Column(host.exposure(Modifier, if (post.isHome) ExposureKind.HomePost else ExposureKind.ShopPost),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PostCard(post = post, isOwn = state.owns(post), publicTimeline = state.timeline == CommunityTimeline.Public,
            changingVisibility = state.changingVisibilityId == post.id,
            showHelpfulCount = state.timeline == CommunityTimeline.Public || post.helpfulCount > 0,
            enabled = !state.isMutating, actions = actions, host = host)
    }
}

@Composable
private fun postActions(
    post: CommunityPost,
    state: CommunityState,
    onAction: (CommunityAction) -> Unit,
    callbacks: CommunityCallbacks,
    blockActions: AuthorBlockActions,
    sharer: PostSharer,
    onHomeEditorOpened: () -> Unit,
    onPhoto: (List<String>, List<String>, Int) -> Unit,
): PostCardActions {
    val isOwn = state.owns(post)
    val public = state.timeline == CommunityTimeline.Public
    val author = post.author.slug?.takeIf(String::isNotBlank)
    val authorName = post.author.name.ifBlank { stringResource(Res.string.community_author) }
    return PostCardActions(
        onAuthor = author?.let { slug -> { callbacks.onOpenAuthor(slug, post.author.name, post.author.avatarUrl) } },
        onShop = { onAction(CommunityAction.ItemInteracted(FeedItemAction.Shop, post.id)); callbacks.onOpenShop(post.shopSlug) }
            .takeIf { post.shopSlug.isNotBlank() },
        onHelpful = { onAction(CommunityAction.ToggleHelpful(post.id)) }.takeIf { state.canLike },
        onBlock = {
            blockActions.requestBlock(AuthorReference(post.author.id, post.author.slug, authorName, post.author.avatarUrl))
        }.takeIf { public && !isOwn && state.canBlockAuthor(post) && (!post.author.id.isNullOrBlank() || !post.author.slug.isNullOrBlank()) },
        onPhoto = { index ->
            onAction(CommunityAction.ItemInteracted(FeedItemAction.Photo, post.id))
            onPhoto(post.photos.thumbnails, post.photos.originals, index)
        },
        onEdit = {
            if (post.isHome) { onHomeEditorOpened(); callbacks.onEditHome(post.id) } else onAction(CommunityAction.Edit(post.id))
        }.takeIf { isOwn },
        onHide = { onAction(CommunityAction.ToggleVisibility(post.id)) }.takeIf { isOwn && post.visibility == PostVisibility.Public },
        onReport = { onAction(CommunityAction.OpenReport(post.id)) }.takeIf { public && !isOwn },
        onPublish = { onAction(CommunityAction.ToggleVisibility(post.id)) }.takeIf { isOwn && post.visibility == PostVisibility.Private },
        onShare = sharer.action(post) { onAction(CommunityAction.ItemInteracted(FeedItemAction.Share, post.id)) },
        onOpen = { onAction(CommunityAction.ItemInteracted(FeedItemAction.Open, post.id)); callbacks.onOpenPost(post.id) }.takeIf { post.id.isNotBlank() },
        onCoffee = post.home?.coffeeProductSlug?.let { slug ->
            { onAction(CommunityAction.ItemInteracted(FeedItemAction.Coffee, post.id)); callbacks.onOpenCoffee(slug) }
        },
    )
}

@Composable
private fun CommunityFeedPreview(dark: Boolean, guest: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val posts = listOf(previewPost(), previewPost().copy(id = "second"))
    CommunityFeedContent(CommunityState(isLoggedIn = !guest, items = posts), {}, CommunityHost(),
        object : AuthorBlockActions {
            override fun requestBlock(author: AuthorReference) = Unit
            override fun requestUnblock(author: AuthorReference) = Unit
        }, CommunityCallbacks({}, { _, _, _ -> }, {}, {}, {}, {}), SnackbarHostState(), 0.dp, rememberLazyListState(), {}, {})
}
@Preview @Composable private fun CommunityFeedLightPreview() = CommunityFeedPreview(false)
@Preview @Composable private fun CommunityFeedDarkPreview() = CommunityFeedPreview(true)
@Preview @Composable private fun CommunityGuestFeedLightPreview() = CommunityFeedPreview(false, guest = true)
@Preview @Composable private fun CommunityGuestFeedDarkPreview() = CommunityFeedPreview(true, guest = true)

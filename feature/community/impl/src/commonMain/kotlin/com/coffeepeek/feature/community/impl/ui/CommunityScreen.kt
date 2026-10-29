package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.Image
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.community.impl.resources.Res
import com.coffeepeek.feature.community.impl.resources.community_mascot_error
import com.coffeepeek.feature.community.impl.resources.community_edit_saved
import com.coffeepeek.feature.community.impl.resources.community_edit_sent_for_review
import com.coffeepeek.feature.community.impl.resources.community_empty
import com.coffeepeek.feature.community.impl.resources.community_error_body
import com.coffeepeek.feature.community.impl.resources.community_error_title
import com.coffeepeek.feature.community.impl.resources.community_loading
import com.coffeepeek.feature.community.impl.resources.community_refresh
import com.coffeepeek.feature.community.impl.resources.community_report_sent
import com.coffeepeek.feature.community.impl.resources.community_restart
import com.coffeepeek.feature.community.impl.resources.community_retry
import com.coffeepeek.feature.community.impl.resources.community_title
import com.coffeepeek.feature.community.impl.resources.community_visibility_failed
import com.coffeepeek.feature.community.impl.resources.community_vote_failed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
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
import com.coffeepeek.feature.community.impl.timeline.ActionMessage
import com.coffeepeek.feature.community.impl.timeline.CommunityAction
import com.coffeepeek.feature.community.impl.timeline.CommunityEvent
import com.coffeepeek.feature.community.impl.timeline.CommunityState
import com.coffeepeek.feature.community.impl.timeline.CommunityTimeline
import com.coffeepeek.feature.community.impl.timeline.CommunityViewModel
import com.coffeepeek.feature.community.impl.timeline.MessageKind

private class PhotoPreview(val generation: Long, val thumbnails: List<String>, val originals: List<String>, val index: Int)

/** What the screen asks the caller to do; navigation and the creation flow are not decided here. */
internal class CommunityCallbacks(
    val onSignIn: () -> Unit,
    val onOpenAuthor: (slug: String, name: String, avatarUrl: String?) -> Unit,
    val onOpenShop: (slug: String) -> Unit,
    val onEditHome: (id: String) -> Unit,
    val onOpenPost: (id: String) -> Unit,
    val onOpenCoffee: (slug: String) -> Unit,
)

@Composable
internal fun CommunityScreen(
    vm: CommunityViewModel,
    host: CommunityHost,
    blockActions: AuthorBlockActions,
    callbacks: CommunityCallbacks,
    modifier: Modifier = Modifier,
    bottomClearance: Dp = 0.dp,
    headerAction: @Composable () -> Unit = {},
    floatingAction: @Composable BoxScope.(CommunityFloating) -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentCallbacks by rememberUpdatedState(callbacks)
    val currentMessages by rememberUpdatedState(messageTexts())
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.events.collect { event ->
                when (event) {
                    CommunityEvent.SignInRequested -> currentCallbacks.onSignIn()
                    is CommunityEvent.Message -> snackbar.showSnackbar(currentMessages.text(event.message))
                }
            }
        }
    }
    CommunityContent(state, vm::onAction, host, blockActions, callbacks, snackbar, modifier, bottomClearance, headerAction, floatingAction)
}

@Composable
internal fun messageTexts() = MessageTexts(
    vote = stringResource(Res.string.community_vote_failed),
    visibility = stringResource(Res.string.community_visibility_failed),
    reported = stringResource(Res.string.community_report_sent),
    saved = stringResource(Res.string.community_edit_saved),
    review = stringResource(Res.string.community_edit_sent_for_review),
)

internal class MessageTexts(val vote: String, val visibility: String, val reported: String, val saved: String, val review: String) {
    fun text(message: ActionMessage): String = when (message.kind) {
        MessageKind.VoteFailed -> message.serverMessage ?: vote
        MessageKind.VisibilityFailed -> message.serverMessage ?: visibility
        MessageKind.ReportSent -> reported
        MessageKind.EditSaved -> saved
        MessageKind.EditSentForReview -> review
    }
}

@Composable
internal fun CommunityContent(
    state: CommunityState,
    onAction: (CommunityAction) -> Unit,
    host: CommunityHost,
    blockActions: AuthorBlockActions,
    callbacks: CommunityCallbacks,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
    bottomClearance: Dp = 0.dp,
    headerAction: @Composable () -> Unit = {},
    floatingAction: @Composable BoxScope.(CommunityFloating) -> Unit = {},
) {
    val listState = rememberLazyListState()
    // A home editor opened from a post: the list reloads when the user comes back.
    var homeEditorOpened by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (homeEditorOpened) { homeEditorOpened = false; onAction(CommunityAction.Refresh()) }
    }
    var preview by remember { mutableStateOf<PhotoPreview?>(null) }
    var displayedGeneration by rememberSaveable { mutableStateOf(state.sessionGeneration) }
    host.feedViewed(state.firstPageCount, state.timeline == CommunityTimeline.Public)
    host.prefetchPhotos(listState, state.items.map { it.id to it.photos.thumbnails.take(3) })
    LaunchedEffect(state.sessionGeneration) {
        if (displayedGeneration != state.sessionGeneration) {
            preview = null
            listState.scrollToItem(0)
            displayedGeneration = state.sessionGeneration
        }
    }
    val shouldLoadMore by remember(state.items.size, state.hasMore, state.isLoading, state.isLoadingMore, state.error, state.isMutating) {
        derivedStateOf {
            state.hasMore && !state.isLoading && !state.isLoadingMore && !state.isMutating && state.error == null &&
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.items.size - 3
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onAction(CommunityAction.LoadMore()) }
    preview?.takeIf { it.generation == state.sessionGeneration }?.let {
        host.photoViewer(it.thumbnails, it.originals, it.index) { preview = null }
    }
    if (state.editing != null) key(state.sessionGeneration, state.editing.source.id) { EditSheet(state, onAction) }
    if (state.reportingId != null) ReportSheet(state.reportForm(), reportCallbacks(onAction))
    val sharer = postSharer(host)
    Box(modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { host.snackbar(snackbar, bottomClearance) },
        ) { padding ->
            CoffeePeekPullToRefresh(
                listState,
                isRefreshing = state.isLoading && state.items.isNotEmpty(),
                onRefresh = { onAction(CommunityAction.Refresh(FeedTrigger.Gesture)) },
                refreshDescription = stringResource(Res.string.community_refresh),
                loadingDescription = stringResource(Res.string.community_loading),
                modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = padding.calculateTopPadding()),
            ) { scrollModifier ->
                LazyColumn(
                    state = listState,
                    modifier = host.exposure(scrollModifier.fillMaxSize(), ExposureKind.Feed),
                    contentPadding = PaddingValues(start = CoffeePeekDimens.spacing4, end = CoffeePeekDimens.spacing4,
                        top = CoffeePeekDimens.spacing4, bottom = bottomClearance + 88.dp),
                    verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
                ) {
                    item(key = "heading", contentType = "heading") {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.community_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            headerAction()
                        }
                    }
                    if (state.isLoggedIn == null || (state.isLoading && state.items.isEmpty())) {
                        item(key = "loading", contentType = "loading") { CenteredLoader() }
                    } else {
                        itemsIndexed(state.items, key = { _, post -> post.id }, contentType = { _, post -> if (post.isHome) "home-post" else "shop-post" }) { index, post ->
                            Column(
                                host.exposure(Modifier, if (post.isHome) ExposureKind.HomePost else ExposureKind.ShopPost),
                                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
                            ) {
                                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                PostCard(
                                    post = post,
                                    isOwn = state.owns(post),
                                    publicTimeline = state.timeline == CommunityTimeline.Public,
                                    changingVisibility = state.changingVisibilityId == post.id,
                                    showHelpfulCount = state.timeline == CommunityTimeline.Public || post.helpfulCount > 0,
                                    enabled = !state.isMutating,
                                    actions = postActions(post, state, onAction, callbacks, blockActions, sharer, { homeEditorOpened = true }) { thumbs, originals, photoIndex ->
                                        preview = PhotoPreview(state.sessionGeneration, thumbs, originals, photoIndex)
                                    },
                                    host = host,
                                )
                            }
                        }
                        when {
                            state.error != null -> item(key = "error", contentType = "error") {
                                ErrorBlock(
                                    onRetry = { onAction(CommunityAction.Retry) },
                                    retryLabel = stringResource(if (state.restartPagination) Res.string.community_restart else Res.string.community_retry),
                                )
                            }
                            state.items.isEmpty() && !state.isLoading -> item(key = "empty", contentType = "empty") {
                                Text(stringResource(Res.string.community_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (state.isLoadingMore) item(key = "loading-more", contentType = "loading") { CenteredLoader() }
                    }
                }
            }
        }
        floatingAction(CommunityFloating(state.isLoggedIn, state.sessionGeneration) { onAction(CommunityAction.Refresh()) })
    }
}

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
    return PostCardActions(
        onAuthor = author?.let { slug -> { callbacks.onOpenAuthor(slug, post.author.name, post.author.avatarUrl) } },
        onShop = { onAction(CommunityAction.ItemInteracted(FeedItemAction.Shop, post.id)); callbacks.onOpenShop(post.shopSlug) }
            .takeIf { post.shopSlug.isNotBlank() },
        onHelpful = { onAction(CommunityAction.ToggleHelpful(post.id)) }.takeIf { !state.isMutating && public },
        onBlock = {
            blockActions.requestBlock(AuthorReference(post.author.id, post.author.slug, post.author.name.ifBlank { "Автор" }, post.author.avatarUrl))
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
internal fun CenteredLoader() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) { CoffeePeekLoader(stringResource(Res.string.community_loading)) }
}

/** The shared "something went wrong" block: the same look and words the legacy list used for every load failure. */
@Composable
internal fun ErrorBlock(onRetry: () -> Unit, retryLabel: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(Res.drawable.community_mascot_error), null, contentScale = ContentScale.Fit, modifier = Modifier.size(120.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.community_error_title), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(Res.string.community_error_body), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(retryLabel) }
    }
}

@Composable private fun CommunityPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CommunityContent(
        CommunityState(isLoggedIn = true, items = listOf(previewPost(), previewPost().copy(id = "second", note = "Вторая запись", photos = com.coffeepeek.feature.community.domain.model.PostPhotos.None))),
        {}, CommunityHost(), object : AuthorBlockActions { override fun requestBlock(author: AuthorReference) {}; override fun requestUnblock(author: AuthorReference) {} },
        CommunityCallbacks({}, { _, _, _ -> }, {}, {}, {}, {}), SnackbarHostState(),
    )
}
@Preview @Composable private fun CommunityLightPreview() = CommunityPreview(false)
@Preview @Composable private fun CommunityDarkPreview() = CommunityPreview(true)

@Composable private fun CommunityErrorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CommunityContent(
        CommunityState(isLoggedIn = true, error = com.coffeepeek.feature.community.impl.timeline.TimelineError.LoadFailed),
        {}, CommunityHost(), object : AuthorBlockActions { override fun requestBlock(author: AuthorReference) {}; override fun requestUnblock(author: AuthorReference) {} },
        CommunityCallbacks({}, { _, _, _ -> }, {}, {}, {}, {}), SnackbarHostState(),
    )
}

@Preview @Composable private fun CommunityErrorLightPreview() = CommunityErrorPreview(false)
@Preview @Composable private fun CommunityErrorDarkPreview() = CommunityErrorPreview(true)

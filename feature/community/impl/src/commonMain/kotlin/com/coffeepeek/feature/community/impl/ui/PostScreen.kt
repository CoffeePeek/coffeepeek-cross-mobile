package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.impl.resources.*
import com.coffeepeek.feature.community.impl.post.PostAction
import com.coffeepeek.feature.community.impl.post.PostError
import com.coffeepeek.feature.community.impl.post.PostEvent
import com.coffeepeek.feature.community.impl.post.PostState
import com.coffeepeek.feature.community.impl.post.PostViewModel

/** What the post screen asks the caller to do; navigation is not decided here. */
internal class PostCallbacks(
    val onBack: () -> Unit,
    val onSignIn: () -> Unit,
    val onOpenAuthor: (slug: String, name: String, avatarUrl: String?) -> Unit,
    val onOpenShop: (slug: String) -> Unit,
    val onOpenCoffee: (slug: String) -> Unit,
)

private class PostPhotoPreview(val thumbnails: List<String>, val originals: List<String>, val index: Int)

@Composable
internal fun PostScreen(vm: PostViewModel, host: CommunityHost, blockActions: AuthorBlockActions, callbacks: PostCallbacks, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentCallbacks by rememberUpdatedState(callbacks)
    val currentMessages by rememberUpdatedState(messageTexts())
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.events.collect { event ->
                when (event) {
                    PostEvent.SignInRequested -> currentCallbacks.onSignIn()
                    is PostEvent.Message -> snackbar.showSnackbar(currentMessages.text(event.message))
                }
            }
        }
    }
    PostScreenContent(state, vm::onAction, host, blockActions, callbacks, snackbar, modifier)
}

@Composable
internal fun PostScreenContent(
    state: PostState,
    onAction: (PostAction) -> Unit,
    host: CommunityHost,
    blockActions: AuthorBlockActions,
    callbacks: PostCallbacks,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var preview by remember { mutableStateOf<PostPhotoPreview?>(null) }
    preview?.let { host.photoViewer(it.thumbnails, it.originals, it.index) { preview = null } }
    if (state.isReportOpen) ReportSheet(state.reportForm(), postReportCallbacks(onAction))
    val authorFallback = stringResource(Res.string.community_user)
    val sharer = postSharer(host)
    CoffeePeekScaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(stringResource(Res.string.community_post_title), stringResource(Res.string.community_back), onBack = callbacks.onBack) },
        snackbarHost = { host.snackbar(snackbar, 0.dp) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val post = state.post
            when {
                post != null -> PostBody(post, state, host, postActions(post, state, onAction, callbacks, blockActions, authorFallback, sharer) { preview = it })
                state.error == PostError.Unavailable -> PostUnavailable()
                state.error == PostError.Failed -> ErrorBlock(onRetry = { onAction(PostAction.Retry) }, retryLabel = stringResource(Res.string.community_retry))
                else -> CenteredLoader()
            }
        }
    }
}

/** A lazy list, so that sections under the post (comments) can be paged items of the same scroll. */
@Composable
private fun PostBody(post: CommunityPost, state: PostState, host: CommunityHost, actions: PostCardActions) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(CoffeePeekDimens.spacing4)) {
        item(key = "post", contentType = "post") {
            PostCard(
                post = post,
                isOwn = state.isOwn,
                // The owner of a private or unapproved post sees its publication state, as in their own timeline.
                publicTimeline = !state.isOwn || state.isPublished,
                showHelpfulCount = state.isPublished || post.helpfulCount > 0,
                enabled = !state.isMutating,
                actions = actions,
                host = host,
            )
        }
    }
}

/** The feed's card actions for one post; likes, reports and blocking need a published post of someone else, sharing a published one. */
private fun postActions(
    post: CommunityPost,
    state: PostState,
    onAction: (PostAction) -> Unit,
    callbacks: PostCallbacks,
    blockActions: AuthorBlockActions,
    authorFallback: String,
    sharer: PostSharer,
    onPhoto: (PostPhotoPreview) -> Unit,
): PostCardActions {
    val interactive = state.isPublished && !state.isOwn
    val author = post.author
    return PostCardActions(
        onAuthor = author.slug?.takeIf(String::isNotBlank)?.let { slug -> { callbacks.onOpenAuthor(slug, author.name, author.avatarUrl) } },
        onShop = { callbacks.onOpenShop(post.shopSlug) }.takeIf { post.shopSlug.isNotBlank() },
        onHelpful = { onAction(PostAction.ToggleHelpful) }.takeIf { interactive && !state.isMutating },
        onBlock = { blockActions.requestBlock(AuthorReference(author.id, author.slug, author.name.ifBlank { authorFallback }, author.avatarUrl)) }
            .takeIf { state.canBlockAuthor && (!author.id.isNullOrBlank() || !author.slug.isNullOrBlank()) },
        onPhoto = { index -> onPhoto(PostPhotoPreview(post.photos.thumbnails, post.photos.originals, index)) },
        onReport = { onAction(PostAction.OpenReport) }.takeIf { interactive },
        onShare = sharer.action(post),
        onCoffee = post.home?.coffeeProductSlug?.let { slug -> { callbacks.onOpenCoffee(slug) } },
    )
}

private fun PostState.reportForm() = ReportForm(reportText, reportIssue, reportFailed, reportFailure, isReporting)

private fun postReportCallbacks(onAction: (PostAction) -> Unit) = ReportCallbacks(
    onText = { onAction(PostAction.ReportText(it)) },
    onSubmit = { onAction(PostAction.SubmitReport) },
    onDismiss = { onAction(PostAction.DismissReport) },
)

@Composable
private fun PostUnavailable(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
    ) {
        Image(painterResource(Res.drawable.community_mascot_error), null, contentScale = ContentScale.Fit, modifier = Modifier.size(120.dp))
        Text(stringResource(Res.string.community_post_unavailable_title), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text(stringResource(Res.string.community_post_unavailable_body), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

private val previewBlockActions = object : AuthorBlockActions {
    override fun requestBlock(author: AuthorReference) {}
    override fun requestUnblock(author: AuthorReference) {}
}

private val previewCallbacks = PostCallbacks({}, {}, { _, _, _ -> }, {}, {})

@Composable private fun PostPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PostScreenContent(PostState(isLoggedIn = true, post = previewPost()), {}, CommunityHost(), previewBlockActions, previewCallbacks, SnackbarHostState())
}
@Preview @Composable private fun PostLightPreview() = PostPreview(false)
@Preview @Composable private fun PostDarkPreview() = PostPreview(true)

@Composable private fun PostUnavailablePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PostScreenContent(PostState(isLoggedIn = false, error = PostError.Unavailable), {}, CommunityHost(), previewBlockActions, previewCallbacks, SnackbarHostState())
}
@Preview @Composable private fun PostUnavailableLightPreview() = PostUnavailablePreview(false)
@Preview @Composable private fun PostUnavailableDarkPreview() = PostUnavailablePreview(true)

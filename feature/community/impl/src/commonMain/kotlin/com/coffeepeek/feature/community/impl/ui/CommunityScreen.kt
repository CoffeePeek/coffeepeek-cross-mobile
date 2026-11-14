package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.Image
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.community.impl.resources.Res
import com.coffeepeek.feature.community.impl.resources.community_mascot_error
import com.coffeepeek.feature.community.impl.resources.community_edit_saved
import com.coffeepeek.feature.community.impl.resources.community_error_body
import com.coffeepeek.feature.community.impl.resources.community_error_title
import com.coffeepeek.feature.community.impl.resources.community_loading
import com.coffeepeek.feature.community.impl.resources.community_report_sent
import com.coffeepeek.feature.community.impl.resources.community_visibility_failed
import com.coffeepeek.feature.community.impl.resources.community_vote_failed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import com.coffeepeek.feature.community.impl.ui.compose.component.CommunitySections
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.community.api.CommunityFloating
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.impl.resources.*
import com.coffeepeek.feature.community.impl.timeline.ActionMessage
import com.coffeepeek.feature.community.impl.timeline.CommunityAction
import com.coffeepeek.feature.community.impl.timeline.CommunityEvent
import com.coffeepeek.feature.community.impl.timeline.CommunityState
import com.coffeepeek.feature.community.impl.timeline.CommunityViewModel
import com.coffeepeek.feature.community.impl.timeline.MessageKind

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
    var leadersSelected by rememberSaveable { mutableStateOf(false) }
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
    androidx.lifecycle.compose.LifecycleResumeEffect(vm) {
        vm.onAction(CommunityAction.Resume)
        onPauseOrDispose { }
    }
    CommunityContent(state, vm::onAction, host, blockActions, callbacks, snackbar, modifier, bottomClearance,
        headerAction, floatingAction, leadersSelected) { leadersSelected = it }
}

@Composable
internal fun messageTexts() = MessageTexts(
    vote = stringResource(Res.string.community_vote_failed),
    visibility = stringResource(Res.string.community_visibility_failed),
    reported = stringResource(Res.string.community_report_sent),
    saved = stringResource(Res.string.community_edit_saved),
)

internal class MessageTexts(val vote: String, val visibility: String, val reported: String, val saved: String) {
    fun text(message: ActionMessage): String = when (message.kind) {
        MessageKind.VoteFailed -> message.serverMessage ?: vote
        MessageKind.VisibilityFailed -> message.serverMessage ?: visibility
        MessageKind.ReportSent -> reported
        MessageKind.EditSaved -> saved
    }
}

@Composable
internal fun CommunityContent(
    state: CommunityState, onAction: (CommunityAction) -> Unit, host: CommunityHost,
    blockActions: AuthorBlockActions, callbacks: CommunityCallbacks, snackbar: SnackbarHostState,
    modifier: Modifier = Modifier, bottomClearance: Dp = 0.dp, headerAction: @Composable () -> Unit = {},
    floatingAction: @Composable BoxScope.(CommunityFloating) -> Unit = {},
    leadersSelected: Boolean = false, onSelectLeaders: (Boolean) -> Unit = {},
) {
    val savedContent = rememberSaveableStateHolder()
    var refreshKey by rememberSaveable { mutableLongStateOf(0L) }
    val leaders = host.leaders
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        if (leadersSelected && leaders != null) savedContent.SaveableStateProvider("leaders") {
            CommunitySections(true, onSelectLeaders, true, headerAction)
            leaders.invoke(refreshKey, bottomClearance, Modifier.weight(1f).fillMaxSize())
        } else savedContent.SaveableStateProvider("feed") {
            val listState = rememberLazyListState()
            // The feed header is pinned: it must stay put while the list scrolls.
            CommunitySections(false, onSelectLeaders, leaders != null, headerAction)
            Box(Modifier.weight(1f)) {
                CommunityFeedContent(state, onAction, host, blockActions, callbacks, snackbar,
                    bottomClearance, listState, { refreshKey++ }, floatingAction)
            }
        }
    }
}

@Composable
internal fun CenteredLoader() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) { CoffeePeekLoader(stringResource(Res.string.community_loading)) }
}

/** The shared "something went wrong" block: the same look and words the previous list used for every load failure. */
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

@Composable private fun CommunityPreview(dark: Boolean, pendingLike: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val first = previewPost().let { if (pendingLike) it.copy(isHelpfulByCurrentUser = true, helpfulCount = it.helpfulCount + 1) else it }
    CommunityContent(
        CommunityState(isLoggedIn = true, helpfulIds = if (pendingLike) setOf(first.id) else emptySet(),
            items = listOf(first, previewPost().copy(id = "second", note = "Вторая запись", photos = com.coffeepeek.feature.community.domain.model.PostPhotos.None))),
        {}, CommunityHost(leaders = { _, _, _ -> }), object : AuthorBlockActions { override fun requestBlock(author: AuthorReference) {}; override fun requestUnblock(author: AuthorReference) {} },
        CommunityCallbacks({}, { _, _, _ -> }, {}, {}, {}, {}), SnackbarHostState(),
    )
}
@Preview @Composable private fun CommunityLightPreview() = CommunityPreview(false)
@Preview @Composable private fun CommunityDarkPreview() = CommunityPreview(true)
@Preview @Composable private fun CommunityPendingLikeLightPreview() = CommunityPreview(false, true)
@Preview @Composable private fun CommunityPendingLikeDarkPreview() = CommunityPreview(true, true)

@Composable private fun CommunityErrorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CommunityContent(
        CommunityState(isLoggedIn = true, error = com.coffeepeek.feature.community.impl.timeline.TimelineError.LoadFailed),
        {}, CommunityHost(leaders = { _, _, _ -> }), object : AuthorBlockActions { override fun requestBlock(author: AuthorReference) {}; override fun requestUnblock(author: AuthorReference) {} },
        CommunityCallbacks({}, { _, _, _ -> }, {}, {}, {}, {}), SnackbarHostState(),
    )
}

@Preview @Composable private fun CommunityErrorLightPreview() = CommunityErrorPreview(false)
@Preview @Composable private fun CommunityErrorDarkPreview() = CommunityErrorPreview(true)

@Composable private fun CommunityLeadersPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CommunityContent(CommunityState(isLoggedIn = true), {}, CommunityHost(leaders = { _, _, modifier ->
        Text(stringResource(Res.string.community_leaders), modifier.padding(16.dp))
    }), object : AuthorBlockActions {
        override fun requestBlock(author: AuthorReference) = Unit
        override fun requestUnblock(author: AuthorReference) = Unit
    }, CommunityCallbacks({}, { _, _, _ -> }, {}, {}, {}, {}), SnackbarHostState(), leadersSelected = true)
}
@Preview @Composable private fun CommunityLeadersLightPreview() = CommunityLeadersPreview(false)
@Preview @Composable private fun CommunityLeadersDarkPreview() = CommunityLeadersPreview(true)

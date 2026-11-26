package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.impl.resources.*
import com.coffeepeek.feature.community.impl.timeline.*
import com.coffeepeek.feature.community.impl.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private data class FeedOverlay(val generation: Long = -1, val photo: NativeCommunityPhoto? = null, val homeEditor: Boolean = false)
private class FeedInput(val state: CommunityState, val resources: NativeCommunityResources, val runtime: NativeCommunityRuntime,
    val overlay: FeedOverlay)

/** Plain owner selected by native iOS. Existing feature ViewModels remain the only business writers. */
class NativeCommunityFeed(repository: CommunityRepository, account: CommunityAccount, analytics: CommunityAnalytics,
    drinks: ConsumedDrinks, authors: AuthorVisibility, saved: CommunitySavedPosts,
    private val blocks: AuthorBlockActions, private val host: CommunityNativeHost, private val navigation: CommunityNavigation,
) {
    private val owner = NativeCommunityFeedSession(repository, account, analytics, drinks, authors, saved)
    private val lifetime = NativeCommunityLifetime(account, host)
    private val overlay = MutableStateFlow(FeedOverlay())
    private val mutableState = MutableStateFlow<NativeCommunityFeedSnapshot?>(null)
    val state = mutableState.asStateFlow()
    private val mutableActive = MutableStateFlow(true)
    val active = mutableActive.asStateFlow()
    private var photoKey = 0L

    init {
        lifetime.start(::close)
        observePresentation()
        observeEvents()
    }

    fun presentation(started: Boolean, bottomClearance: Double) = lifetime.presentation(started, bottomClearance)
    fun resumed() {
        if (!lifetime.isCurrent()) return
        overlay.value = overlay.value.copy(homeEditor = false)
        owner.resumed()
    }

    fun refresh() {
        if (lifetime.isCurrent()) owner.perform(CommunityAction.Refresh())
    }

    private fun observePresentation() {
        lifetime.scope.launch {
            combine(owner.model.state, lifetime.resources.filterNotNull(), lifetime.runtime, overlay, lifetime.clock) { state, resources, runtime, overlay, _ ->
                FeedInput(state, resources, runtime, overlay)
            }.collectLatest { input ->
                val snapshot = snapshot(input)
                currentCoroutineContext().ensureActive()
                if (lifetime.isCurrent()) mutableState.value = snapshot
            }
        }
    }

    private suspend fun snapshot(input: FeedInput): NativeCommunityFeedSnapshot {
        val state = input.state
        val activeOverlay = input.overlay.takeIf { it.generation == state.sessionGeneration }
        val interactive = input.runtime.started && state.editing == null && state.reportingId == null &&
            activeOverlay?.photo == null && activeOverlay?.homeEditor != true
        val sharer = input.resources.sharer(host.share)
        val texts = input.resources.postTexts()
        val posts = if (state.isLoggedIn == false) state.items.take(2) else state.items
        val cards = posts.mapIndexed { index, post -> card(input, post, state.isLoggedIn == false && index > 0, texts, sharer) }
        val commands = nativeFeedCommands(state, interactive, owner::perform, navigation.onSignIn, navigation.onOpenPost, {})
        val actions = CommunityFeedActions(guardCommands(state, commands), nativeFeedCardActions(cards.associate { it.first.key to it.second }, interactive))
        return NativeCommunityFeedSnapshot(nativeFeedPresentation(state, cards.map { it.first }, input.runtime.bottomClearance,
            interactive, input.resources.feedTexts()), actions, sheet(state, input.resources), activeOverlay?.photo,
            state.firstPageCount, state.timeline == CommunityTimeline.Public)
    }

    private suspend fun card(input: FeedInput, post: CommunityPost, teaser: Boolean, texts: NativePostTexts,
        sharer: PostSharer): Pair<CommunityCardPresentation, CommunityCardActions> {
        val state = input.state
        val callbacks = if (teaser) PostCardActions(null, null, null, null, { navigation.onSignIn() }) else
            feedPostActions(post, state, owner::perform, navigation, blocks, sharer, { homeEditorOpened(state) },
                { thumbnails, originals, index -> openPhoto(state, thumbnails, originals, index) }, input.resources.text(Res.string.community_author))
        val formatted = input.resources.postFormatted(post, state.owns(post), callbacks.onHelpful != null, texts)
        val card = nativePostPresentation(post, state.owns(post), state.timeline == CommunityTimeline.Public,
            state.timeline == CommunityTimeline.Public || post.helpfulCount > 0, !state.isMutating && !teaser,
            state.changingVisibilityId == post.id, callbacks, texts, formatted,
            post.createdAt.takeIf(String::isNotBlank)?.let(host.displayDate).orEmpty(), input.resources.language, host.brewMethodLabel, 16.0)
        return card to guardCard(state, nativePostActions(callbacks, !state.isMutating && !teaser, state.changingVisibilityId == post.id))
    }

    private suspend fun sheet(state: CommunityState, resources: NativeCommunityResources): NativeCommunitySheet? {
        state.editing?.let { edit ->
            val data = nativeEditorPresentation(edit, state.drinks, state.drinksFailed, state.isSaving, resources.language,
                resources.editorTexts(), resources.editorRatings(edit), resources.editError(edit.error))
            val send: (CommunityAction) -> Unit = { if (current(state) && owner.model.state.value.editing?.source?.id == edit.source.id) owner.perform(it) }
            return NativeCommunitySheet.Editor("${state.accountEpoch}:${state.sessionGeneration}:edit:${edit.source.id}", data,
                nativeEditorActions(edit, state.drinks, timelineEditorCallbacks(send)))
        }
        val id = state.reportingId ?: return null
        val send: (CommunityAction) -> Unit = { if (current(state) && owner.model.state.value.reportingId == id) owner.perform(it) }
        return NativeCommunitySheet.Report("${state.accountEpoch}:${state.sessionGeneration}:report:$id",
            nativeReportPresentation(state.reportForm(), resources.reportTexts()), nativeReportActions(reportCallbacks(send)))
    }

    private fun guardCommands(state: CommunityState, commands: Map<String, () -> Unit>) = commands.mapValues { (_, action) ->
        { if (interactive(state)) action() }
    }

    private fun guardCard(state: CommunityState, actions: CommunityCardActions) = CommunityCardActions(guardCommands(state, actions.actions)) {
        if (interactive(state)) actions.onPhoto(it)
    }

    private fun current(state: CommunityState) = lifetime.isCurrent() && owner.model.state.value.let {
        it.accountEpoch == state.accountEpoch && it.sessionGeneration == state.sessionGeneration
    }

    private fun interactive(state: CommunityState): Boolean {
        val latest = owner.model.state.value
        val shown = overlay.value.takeIf { it.generation == latest.sessionGeneration }
        return current(state) && lifetime.runtime.value.started && latest.editing == null && latest.reportingId == null &&
            shown?.photo == null && shown?.homeEditor != true
    }

    private fun homeEditorOpened(state: CommunityState) {
        if (!current(state)) return
        owner.homeEditorOpened()
        overlay.value = FeedOverlay(state.sessionGeneration, homeEditor = true)
    }

    private fun openPhoto(state: CommunityState, thumbnails: List<String>, originals: List<String>, index: Int) {
        if (!interactive(state) || index !in originals.indices) return
        val key = ++photoKey
        overlay.value = FeedOverlay(state.sessionGeneration, NativeCommunityPhoto(key, thumbnails, originals, index) {
            if (lifetime.isCurrent() && overlay.value.photo?.key == key) overlay.value = FeedOverlay()
        })
    }

    private fun observeEvents() {
        lifetime.scope.launch { lifetime.runtime.map { it.started }.distinctUntilChanged().collectLatest { started ->
            if (started) owner.model.events.collect { event ->
                if (!lifetime.isCurrent()) return@collect
                when (event) {
                    CommunityEvent.SignInRequested -> navigation.onSignIn()
                    is CommunityEvent.Message -> lifetime.resources.filterNotNull().first().messageTexts().let {
                        if (lifetime.isCurrent()) host.message(it.text(event.message))
                    }
                }
            }
        } }
    }

    fun close() {
        mutableActive.value = false
        lifetime.close()
        owner.close()
        overlay.value = FeedOverlay()
        mutableState.value = null
    }
}

internal suspend fun NativeCommunityResources.sharer(share: (String) -> Unit) = PostSharer(
    text(Res.string.community_share_shop), text(Res.string.community_share_home),
    text(Res.string.community_user), text(Res.string.community_shop_default), share,
)
